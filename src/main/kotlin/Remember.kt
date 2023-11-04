package org.openredstone.velocityutils

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Single
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

const val velocityUtilsCommandBase = "<dark_gray>[<gray>VelocityUtils<dark_gray>] <gray><message>"

object PlayerServerMapping : Table("mapping") {
    val uuid = varchar("uuid", 36).index()
    val server = varchar("server", 32)
    override val primaryKey = PrimaryKey(uuid)
}

fun initializeDatabase(database: Database) = transaction(database) {
    SchemaUtils.create(PlayerServerMapping)
}

fun setLastServer(database: Database, uuid: UUID, server: String) = transaction(database) {
    if (PlayerServerMapping.select { PlayerServerMapping.uuid eq uuid.toString() }.count() == 0L) {
        PlayerServerMapping.insert {
            it[this.uuid] = uuid.toString()
            it[this.server] = server
        }
    } else {
        PlayerServerMapping.update({ PlayerServerMapping.uuid eq uuid.toString() }) {
            it[this.server] = server
        }
    }
}

fun getLastServer(database: Database, uuid: UUID) : String? = transaction(database) {
    PlayerServerMapping.select { PlayerServerMapping.uuid eq uuid.toString() }
        .firstOrNull()?.let { it[PlayerServerMapping.server] }
}

fun createRememberFeature(plugin: VelocityUtils): Feature {
    val dbFile = plugin.dataFolder.resolve("remember.db").toString()
    val database = Database.connect("jdbc:sqlite:${dbFile}", "org.sqlite.JDBC")
    initializeDatabase(database)
    return Feature(
        listeners = listOf(RememberListeners(plugin, database)),
        commands = listOf(SendCommand(plugin), SendAllCommand(plugin))
    )
}

private class RememberListeners(
    val plugin: VelocityUtils,
    val database: Database
) {
    @Subscribe
    fun onServerConnect(event: ServerPostConnectEvent) {
        event.player.currentServer.ifPresent {
            setLastServer(database, event.player.uniqueId, it.serverInfo.name)
        }
    }
    @Subscribe
    fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) {
        val lastServerName = getLastServer(database, event.player.uniqueId) ?: return
        val lastServer = plugin.proxy.getServer(lastServerName).toNullable() ?: return
        event.setInitialServer(lastServer)
    }
}

@CommandAlias("send")
@CommandPermission("velocityutils.send")
private class SendCommand(
    private val plugin: VelocityUtils
) : BaseCommand() {
    val mm = MiniMessage.miniMessage()
    @Default
    @CommandCompletion("@players @servers")
    fun default(player: Player, @Single user: String, @Single to: String) {
        val target = plugin.proxy.getPlayer(user).toNullable() ?: run {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Player not available").resolveTo("message")
            ))
            return
        }
        val targetServer = plugin.proxy.getServer(to).toNullable() ?: run {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Target server does not exist").resolveTo("message")
            ))
            return
        }
        if (target.currentServer.toNullable()?.serverInfo?.name == to) {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Player is already connected to that server").resolveTo("message")
            ))
            return
        }
        target.createConnectionRequest(targetServer).connect().whenComplete { result, error ->
            if (result == null) {
                player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                    Component.text("Failed to send $user to '$to'").resolveTo("message")
                ))
                player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                    Component.text(error.localizedMessage).resolveTo("message")
                ))
            } else {
                target.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                    Component.text("You have been sent to '$to'").resolveTo("message")
                ))
                player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                    Component.text("Sent $user to '$to'").resolveTo("message")
                ))
            }
        }
    }
}

@CommandAlias("sendall")
@CommandPermission("velocityutils.send")
class SendAllCommand(
    private val plugin: VelocityUtils
) : BaseCommand() {
    private val mm = MiniMessage.miniMessage()

    @Default
    @CommandCompletion("@servers @servers")
    fun default(player: Player, @Single from: String, @Single to: String) {
        if (from == to) {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Source and destination are the same").resolveTo("message")
            ))
            return
        }
        val source = plugin.proxy.getServer(from).toNullable() ?: run {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Source server does not exist").resolveTo("message")
            ))
            return
        }
        val destination = plugin.proxy.getServer(to).toNullable() ?: run {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Destination server does not exist").resolveTo("message")
            ))
            return
        }
        player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
            Component.text("Creating connection requests for ${source.playersConnected.size} players from '$from' to '$to'").resolveTo("message")
        ))
        source.playersConnected.forEach {
            it.createConnectionRequest(destination).connect().whenComplete { result, _ ->
                if (result != null) {
                    it.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                        Component.text("You have been sent to '$to'").resolveTo("message")
                    ))
                }
            }
        }
    }
}
