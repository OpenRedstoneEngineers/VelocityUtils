package org.openredstone.velocityutils

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.nio.file.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val defaultMaintenance = "<bold>This game server is currently down for maintenance.</bold>"

data class MaintenanceConfig(
    val enabled: Boolean = false
)

fun createMaintenanceFeature(plugin: VelocityUtils, config: MaintenanceConfig): Feature {
    val mm = MiniMessage.miniMessage()
    val message = mm.deserialize(
        loadMessage(plugin.dataFolder.toPath().resolve("maintenance.txt"))
    )
    if (!config.enabled) return Feature()
    return Feature(
        listeners = listOf(LoginListener(message))
    )
}

private fun loadMessage(messageFile: Path): String =
    if (messageFile.notExists()) {
        messageFile.writeText(defaultMaintenance)
        defaultMaintenance
    } else {
        messageFile.readText().trim { it <= ' ' }
    }

private class LoginListener(message: Component) {
    val denied: ResultedEvent.ComponentResult = ResultedEvent.ComponentResult.denied(message)

    @Subscribe
    fun onLogin(event: LoginEvent) {
        if (event.player.hasPermission("velocityutils.manage")) return
        event.result = denied
    }
}
