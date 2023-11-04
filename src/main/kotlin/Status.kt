package org.openredstone.velocityutils

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.scheduler.ScheduledTask
import com.velocitypowered.api.scheduler.Scheduler
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.util.logging.ExceptionLogger
import java.awt.Color
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


val serversOnline = ConcurrentHashMap<RegisteredServer, Boolean>()

fun createStatusFeature(plugin: VelocityUtils, config: StatusConfig): Feature {
    val scheduler = plugin.proxy.scheduler
    val api = DiscordApiBuilder()
        .setToken(config.discordApiKey)
        .setAllIntents()
        .login().join()
    val statusChannel = api.getServerTextChannelById(config.statusChannelId).toNullable() ?:
        throw Exception("Unable to load status channel ${config.statusChannelId}")
    createServersOnlineTask(plugin)
    return Feature(
        listeners = listOf(JoinAndLeaveListener(plugin, scheduler, statusChannel))
    )
}

data class StatusConfig(
    val discordApiKey: String = "check Stack's website",
    val statusChannelId: Long = 0L
)

fun createServersOnlineTask(plugin: VelocityUtils) {
    plugin.proxy.scheduler
        .buildTask(plugin, Runnable {
            serversOnline.clear()
            plugin.proxy.allServers.forEach {
                it.ping().whenComplete { _, error ->
                    serversOnline[it] = error == null
                }
            }
        })
        .repeat(60L, TimeUnit.SECONDS)
        .schedule()
}

fun updateDiscordStatus(server: ProxyServer, statusChannel: ServerTextChannel) {
    val embedBuilder = EmbedBuilder()
        .setColor(Color.decode("#cd2d0a"))
        .setTitle("Status")
        .addField("**Players Online**", "${server.allPlayers.size}")
        .setThumbnail("https://openredstone.org/wp-content/uploads/2018/07/icon-mini.png")
        .setTimestamp(Instant.now())
    val servers = server.allServers.associateWith { (it.playersConnected.map { player -> player.username }.sorted()) }
    servers.forEach { (currentServer, players) ->
        if (serversOnline.contains(currentServer) && (serversOnline[currentServer] == true)) {
            embedBuilder.addField("${currentServer.serverInfo.name} is **offline**",  ":skull_crossbones:")
        } else {
            if (players.isEmpty()) {
                embedBuilder.addField("${currentServer.serverInfo.name} (**0**)", ":frowning:")
            } else {
                val playerString = players.joinToString(", ") { "`${it}`" }
                embedBuilder.addField("${currentServer.serverInfo.name} (**${players.size}**)", playerString)
            }
        }
    }
    val messages = statusChannel.getMessages(1).get()
    if (messages.isEmpty()) {
        statusChannel.sendMessage(embedBuilder).exceptionally(ExceptionLogger.get())
    } else {
        messages.newestMessage.ifPresent {
            it.edit("")
            it.edit(embedBuilder)
        }
    }
}

private class JoinAndLeaveListener(
    val plugin: VelocityUtils,
    val scheduler: Scheduler,
    val statusChannel: ServerTextChannel
) {
    var updateTask: ScheduledTask? = null
    @Subscribe
    fun onJoinEvent(event: PostLoginEvent) {
        // player joins
        plugin.logger.info("PostLoginEvent!")
        queueDiscordUpdate()
    }
    @Subscribe
    fun onDisconnectEvent(event: DisconnectEvent) {
        // player leaves
        plugin.logger.info("DisconnectEvent!")
        queueDiscordUpdate()
    }
    @Subscribe
    fun onServerConnect(event: ServerConnectedEvent) {
        // player changed server (or logged in)
        plugin.logger.info("ServerConnectedEvent!")
        queueDiscordUpdate()
    }
    fun queueDiscordUpdate() {
        updateTask?.cancel()
        updateTask = scheduler
            .buildTask(plugin, Runnable { updateDiscordStatus(plugin.proxy, statusChannel) })
            .delay(1L, TimeUnit.SECONDS)
            .schedule()
    }
}
