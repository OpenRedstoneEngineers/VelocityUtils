package org.openredstone.velocityutils

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.luckperms.api.LuckPerms
import net.luckperms.api.node.types.InheritanceNode
import java.util.*

data class ApplyConfig(
    val prefix: String = "<dark_gray>[<gray>Application<dark_gray>]<reset>",
    val ineligible: String = "<prefix> <yellow>You are not eligible to apply</yellow>",
    val builderGroup: String = "builder",
    val alreadyApplying: String = "<prefix> <yellow>You are currently in the application process.</yellow>",
    val notApplying: String = "<prefix> <yellow>You are currently not in the application process.</yellow>",
    val accepted: String = "<prefix> <yellow>Congratulations! You can claim your plot by " +
        "<hover:show_text:'Go to School'><click:run_command:'/server school'><gold>going to the School server</gold>" +
        "</click></hover> executing <hover:show_text:'Run command'><click:run_command:'/p auto'><gold>/p auto</gold>" +
        "</click></hover>",
    val questionFormat: String = "<prefix> <yellow><question> <response>",
    val responseFormat: String = "<gold><hover:show_text:'Answer \"Yes\"'><click:run_command:'/apply reply yes'>[Yes]" +
        "</click></hover> <hover:show_text:'Answer \"No\"'><click:run_command:'/apply reply no'>[No]</click>" +
        "</hover></gold>",
    val incorrect: String = "<prefix> <yellow>That was not an expected answer. Please restart your application by " +
        "executing <gold><click:run_command:'/apply'><hover:show_text:'Run /apply'>/apply</hover></click></gold>." +
        "</yellow>",
    val questions: List<List<String>> = listOf(
        listOf("<yellow>Are you interested in learning about computational redstone?", "yes"),
        listOf("<yellow>Do you have prior experience in redstone?", "either"),
        listOf("<yellow>Have you read <hover:show_text:'View rules'><click:open_url:'https://openredstone.org/rules'>" +
            "<gold>the rules</gold></click></hover>?", "yes")
    )
)

fun createApplyFeature(luckPerms: LuckPerms, config: ApplyConfig): Feature {
    val mm = MiniMessage.miniMessage()
    return Feature(
        commands = listOf(ApplyCommand(luckPerms, mm, config)),
    )
}

@CommandAlias("apply")
@CommandPermission("velocityutils.apply")
class ApplyCommand(
    private val luckPerms: LuckPerms,
    private val mm: MiniMessage,
    private val config: ApplyConfig
) : BaseCommand() {
    private val applications = hashMapOf<UUID, Int>()
    private val prefixReplacement = Placeholder.component("prefix", mm.deserialize(config.prefix))

    private fun sendQuestion(player: Player) {
        val applicant = applications[player.uniqueId]!!
        config.questions[applicant].let {
            val replacements: Map<String, Component> = mapOf(
                "question" to mm.deserialize(it.first()),
                "response" to mm.deserialize(config.responseFormat)
            )
            val allReplacements = replacements.map { rep ->
                Placeholder.component(rep.key, rep.value)
            }.toMutableList().apply {
                this.add(prefixReplacement)
            }.toTypedArray()
            player.sendMessage(mm.deserialize(config.questionFormat, *allReplacements))
        }
    }

    private fun processReply(source: Player, yes: Boolean) {
        val progress = applications[source.uniqueId]!!
        val expected = config.questions[progress].last()
        val correct = (expected == "yes" && yes) || (expected == "no" && !yes) || (expected == "either")
        if (correct) {
            applications[source.uniqueId] = progress + 1
            if (applications[source.uniqueId] == config.questions.size) {
                luckPerms.userManager.getUser(source.uniqueId)?.let {
                    InheritanceNode.builder(config.builderGroup).value(true).build().let { node -> it.data().add(node)}
                    InheritanceNode.builder(it.primaryGroup).value(true).build().let { node -> it.data().remove(node)}
                    luckPerms.userManager.saveUser(it)
                }
                source.sendMessage(mm.deserialize(config.accepted, prefixReplacement))
                applications.remove(source.uniqueId)
            } else {
                sendQuestion(source)
            }
        } else {
            source.sendMessage(mm.deserialize(config.incorrect, prefixReplacement))
            applications.remove(source.uniqueId)
        }
    }

    @Default
    fun default(source: Player) {
        val primaryGroup = luckPerms.userManager.getUser(source.uniqueId)?.primaryGroup ?: return
        if (primaryGroup != "default") {
            source.sendMessage(mm.deserialize(config.ineligible, prefixReplacement))
            return
        }
        if (source.uniqueId in applications) {
            source.sendMessage(mm.deserialize(config.alreadyApplying, prefixReplacement))
            sendQuestion(source)
            return
        }
        applications[source.uniqueId] = 0
        sendQuestion(source)
    }

    @Subcommand("reply")
    inner class Reply : BaseCommand () {
        @Subcommand("yes")
        fun yes(source: Player) {
            if (source.uniqueId !in applications) {
                source.sendMessage(mm.deserialize(config.notApplying, prefixReplacement))
                return
            }
            processReply(source, true)
        }
        @Subcommand("no")
        fun no(source: Player) {
            if (source.uniqueId !in applications) {
                source.sendMessage(mm.deserialize(config.notApplying, prefixReplacement))
                return
            }
            processReply(source, false)
        }
    }
}
