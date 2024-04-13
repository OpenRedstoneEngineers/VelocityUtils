package org.openredstone.velocityutils

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.minimessage.MiniMessage

fun createServerListFeature(plugin: VelocityUtils, config: ListConfig): Feature {
    val mm = MiniMessage.miniMessage()
    val createList = { plugin.proxy.createServerList(config, mm) }
    return Feature(
        commands = listOf(ListCommand(createList)),
    )
}

data class ListConfig(
    val format: String = """
        <server_list>
        <red><bold><total_player_count></bold></red> <yellow>player(s) online</yellow>
    """.trimIndent(),
    val serverFormat: String =
        "<yellow><click:run_command:/%server_name%><hover:show_text:Click to join><server_name></hover></click></yellow> <gray>-</gray> <red><server_player_count></red><gray>:</gray> <player_list>",
    val serverSeparator: String = "\n",
    val playerFormat: String = "<gray><player_name></gray>",
    val playerSeparator: String = "<gray>, </gray>",
)

private fun ProxyServer.createServerList(config: ListConfig, mm: MiniMessage): Component = mm.deserialize(
    config.format,
    Component.join(
        JoinConfiguration.separator(mm.deserialize(config.serverSeparator)),
        allServers
            .map { server ->
                val playerList = Component.join(
                    JoinConfiguration.separator(mm.deserialize(config.playerSeparator)),
                    server.playersConnected
                        .map { mm.deserialize(config.playerFormat, Component.text(it.username).resolveTo("player_name")) }
                )
                mm.deserialize(
                    config.serverFormat.replace("%server_name%", server.serverInfo.name), // This is a hack
                    playerList.resolveTo("player_list"),
                    Component.text("${server.playersConnected.size}").resolveTo("server_player_count"),
                    Component.text(server.serverInfo.name).resolveTo("server_name")
                )
            }
    ).resolveTo("server_list"),
    Component.text("${this.playerCount}").resolveTo("total_player_count")
)

@CommandAlias("list|ls")
@CommandPermission("velocityutils.list")
private class ListCommand(
    private val createList: () -> Component,
) : BaseCommand() {
    @Default
    fun default(player: Player) {
        player.sendMessage(createList())
    }
}
