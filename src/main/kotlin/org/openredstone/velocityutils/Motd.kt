package org.openredstone.velocityutils

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val defaultMotd = "<gray>hey, <dark_red><bold><player_name><reset><gray>, you nerd."

fun createMotdFeature(plugin: VelocityUtils): Feature {
    val mm = MiniMessage.miniMessage()
    val motd = loadMotd(plugin.dataFolder.toPath().resolve("motd.txt"))
    val doMotd = { player: Player ->
        player.sendMessage(mm.deserialize(motd, placeholdersFor(plugin.proxy, player)))
    }
    return Feature(
        commands = listOf(MotdCommand(doMotd)),
        listeners = listOf(JoinListener(doMotd)),
    )
}

private fun placeholdersFor(proxyServer: ProxyServer, player: Player): TagResolver = TagResolver.resolver(
    Component.text(player.username).resolveTo("player_name"),
    Component.text("${proxyServer.playerCount}").resolveTo("online_count")
)

private fun loadMotd(motdFile: Path): String =
    if (motdFile.notExists()) {
        motdFile.writeText(defaultMotd)
        defaultMotd
    } else {
        motdFile.readText().trim { it <= ' ' }
    }

private class JoinListener(private val doMotd: (Player) -> Unit) {
    @Subscribe
    fun onJoinEvent(event: PostLoginEvent) {
        doMotd(event.player)
    }
}

@CommandAlias("motd")
@CommandPermission("velocityutils.motd")
private class MotdCommand(private val doMotd: (Player) -> Unit) : BaseCommand() {
    @Default
    fun default(player: Player) {
        doMotd(player)
    }
}
