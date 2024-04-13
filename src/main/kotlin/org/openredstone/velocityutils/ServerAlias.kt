package org.openredstone.velocityutils

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

fun createServerAliasFeature(plugin: VelocityUtils): Feature {
    val serverCommand = ServerAliasCommand(plugin)
    plugin.proxy.allServers.forEach {
        plugin.proxy.commandManager.register(it.serverInfo.name, serverCommand)
    }
    return Feature()
}

private class ServerAliasCommand(
    private val plugin: VelocityUtils
) : SimpleCommand {
    private val mm = MiniMessage.miniMessage()

    override fun execute(invocation: SimpleCommand.Invocation?) {
        val server = invocation?.alias() ?: return
        val player = try {
            invocation.source() as Player
        } catch (e: Exception) {
            invocation.source().sendMessage(Component.text("You're not a user...?"))
            return
        }
        val permissionNode = "velocityutils.server.${server}"
        if (!player.hasPermission(permissionNode)) {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("You do not have permission to go to this server!").resolveTo("message"))
            )
            return
        }
        val targetServer = plugin.proxy.getServer(server).toNullable() ?: run {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("Destination server not found.").resolveTo("message")
            ))
            return
        }
        val currentServer = player.currentServer.toNullable() ?: return  // This shouldn't happen
        if (currentServer.serverInfo.name == targetServer.serverInfo.name) {
            player.sendMessage(mm.deserialize(velocityUtilsCommandBase,
                Component.text("You are already connected to this server!").resolveTo("message")
            ))
            return
        }
        player.createConnectionRequest(targetServer).connect()
    }
}
