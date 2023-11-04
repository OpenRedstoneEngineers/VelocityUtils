package org.openredstone.velocityutils

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

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
        listeners = listOf(RememberListeners(plugin, database))
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
        val lastServer = plugin.proxy.allServers.first { it.serverInfo.name == lastServerName }
        event.setInitialServer(lastServer)
    }
}
