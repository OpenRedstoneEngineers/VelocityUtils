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
import java.util.concurrent.TimeUnit

data class ApplyConfig(
    val studentGroup: String = "student",
    val destinationServer: String = "school",
    val prefix: String = "<dark_gray>[<gray>Application<dark_gray>]<reset>",
    val ineligible: String = "<prefix> <yellow>You are not eligible to apply</yellow>",
    val alreadyApplying: String = "<prefix> <yellow>You are currently in the application process.</yellow>",
    val notApplying: String = "<prefix> <yellow>You are currently not in the application process.</yellow>",
    val accepted: String = "<prefix> <yellow>Congratulations! You have been sent to the <server> server. You can " +
        "claim a plot by typing <hover:show_text:'Run command'><click:run_command:'/p auto'><gold>/p auto</gold>" +
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
        listOf("<yellow>Have you read and agree to <hover:show_text:'View rules'>" +
            "<click:open_url:'https://openredstone.org/rules'><gold>the rules</gold></click></hover>?", "yes")
    )
)

fun createApplyFeature(plugin: VelocityUtils, luckPerms: LuckPerms, config: ApplyConfig): Feature {
    val mm = MiniMessage.miniMessage()
    return Feature(
        commands = listOf(ApplyCommand(plugin, luckPerms, mm, config)),
    )
}

@CommandAlias("apply")
@CommandPermission("velocityutils.apply")
class ApplyCommand(
    private val plugin: VelocityUtils,
    private val luckPerms: LuckPerms,
    private val mm: MiniMessage,
    private val config: ApplyConfig
) : BaseCommand() {
    private val applications = hashMapOf<UUID, Int>()
    private val prefixReplacement = Placeholder.component("prefix", mm.deserialize(config.prefix))

    private fun sendQuestion(player: Player) {
        val applicant = applications[player.uniqueId]!!
        config.questions[applicant].let {
            val allReplacements = listOf(
                Placeholder.component("question", mm.deserialize(it.first())),
                Placeholder.component("response", mm.deserialize(config.responseFormat)),
                prefixReplacement
            ).toTypedArray()
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
                    val oldNode = InheritanceNode.builder(it.primaryGroup).value(true).build()
                    it.data().remove(oldNode)
                    val newNode = InheritanceNode.builder(config.studentGroup).value(true).build()
                    it.data().add(newNode)
                    luckPerms.userManager.saveUser(it).thenRun {
                        luckPerms.messagingService.toNullable()?.pushUserUpdate(it)
                    }
                }
                processAccepted(source)
            } else {
                sendQuestion(source)
            }
        } else {
            source.sendMessage(mm.deserialize(config.incorrect, prefixReplacement))
            applications.remove(source.uniqueId)
        }
    }

    private fun processAccepted(source: Player) {
        val serverTo = plugin.proxy.getServer(config.destinationServer).toNullable() ?: run {
            source.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Destination server does not exist").resolveTo("message")
            ))
            return
        }
        source.createConnectionRequest(serverTo).connect()
        applications.remove(source.uniqueId)
        plugin.proxy.scheduler
            .buildTask(plugin) { _ ->
                val allReplacements = listOf(
                    Placeholder.component("server", mm.deserialize(config.destinationServer.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    })),
                    prefixReplacement
                ).toTypedArray()
                source.sendMessage(mm.deserialize(config.accepted, *allReplacements))
            }
            .delay(2, TimeUnit.SECONDS)
            .schedule()
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
