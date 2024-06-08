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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.LuckPerms

fun createPlayersListFeature(plugin: VelocityUtils, luckPerms: LuckPerms, config: PlayersConfig): Feature {
    val mm = MiniMessage.miniMessage()
    val legacyDeserializer = LegacyComponentSerializer.legacy('&')
    val createList = { createPlayersList(config, plugin.proxy, luckPerms, mm, legacyDeserializer) }
    return Feature(
        commands = listOf(PlayersCommand(createList)),
    )
}

data class PlayersConfig(
    val format: String = """
        <yellow>Player(s) online:</yellow>
        <ranks>
        """.trimIndent(),
    val rankFormat: String = "<rank_name> <gray>-</gray> <player_list>",
    val rankSeparator: String = "\n",
    val playerFormat: String = "<yellow><player_name></yellow>",
    val playerSeparator: String = "<red>, </red>",
)

private fun createPlayersList(
    config: PlayersConfig,
    proxy: ProxyServer,
    luckPerms: LuckPerms,
    mm: MiniMessage,
    legacyDeserializer: LegacyComponentSerializer
): Component {
    fun Player.groupDisplayName() =
        luckPerms.userManager.getUser(this.uniqueId)
            ?.primaryGroup
            ?.let(luckPerms.groupManager::getGroup)
            ?.let { luckGroup -> luckGroup.cachedData.metaData.prefix ?: luckGroup.name }
            ?: "Unavailable"

    val playerMap = proxy.allPlayers.groupBy({ it.groupDisplayName() }, { it.username })
    return mm.deserialize(
        config.format,
        Component.join(
            JoinConfiguration.separator(mm.deserialize(config.rankSeparator)),
            playerMap.map { (rank, players) ->
                val playerList = Component.join(
                    JoinConfiguration.separator(mm.deserialize(config.playerSeparator)),
                    players
                        .map { mm.deserialize(config.playerFormat, Component.text(it).resolveTo("player_name")) }
                )
                mm.deserialize(
                    config.rankFormat,
                    legacyDeserializer.deserialize(rank).resolveTo("rank_name"),
                    playerList.resolveTo("player_list")
                )
            }
        ).resolveTo("ranks")
    )
}

@CommandAlias("players|online")
@CommandPermission("velocityutils.players")
private class PlayersCommand(
    private val createList: () -> Component
) : BaseCommand() {
    @Default
    fun default(player: Player) {
        player.sendMessage(createList())
    }
}
