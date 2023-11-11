package org.openredstone.velocityutils

import com.velocitypowered.api.command.SimpleCommand

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
    override fun execute(invocation: SimpleCommand.Invocation?) {
        val server = invocation?.alias() ?: return
        plugin.proxy.commandManager.executeAsync(
            invocation.source(),
            "server $server"
        )
    }
}
