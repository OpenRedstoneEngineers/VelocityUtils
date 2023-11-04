package org.openredstone.velocityutils

import co.aikar.commands.BaseCommand
import co.aikar.commands.VelocityCommandManager
import co.aikar.commands.annotation.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.minimessage.MiniMessage
import net.luckperms.api.LuckPermsProvider
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path

data class Feature(
    val commands: List<BaseCommand> = emptyList(),
    val listeners: List<Any> = emptyList(),
    val unload: () -> Unit = {},
)

private const val VERSION = "0.1.0-SNAPSHOT"

@Plugin(
    id = "velocityutils",
    name = "VelocityUtils",
    version = VERSION,
    url = "https://openredstone.org",
    description = "For various proxy features.",
    authors = ["Nickster258", "PaukkuPalikka"],
    dependencies = [Dependency(id = "luckperms")]
)
class VelocityUtils @Inject constructor(val proxy: ProxyServer, val logger: Logger, @DataDirectory dataFolder: Path) {
    private lateinit var config: JsonNode
    private lateinit var features: List<Feature>
    private lateinit var commandManager: VelocityCommandManager
    private val mapper = ObjectMapper(YAMLFactory())
    val dataFolder = dataFolder.toFile()

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        config = loadConfig()
        commandManager = VelocityCommandManager(proxy, this).apply {
            registerCommand(VelocityUtilsCommand())
        }
        commandManager.commandCompletions.registerCompletion("servers") {
            return@registerCompletion proxy.allServers.map { it.serverInfo.name }
        }
        loadFeatures()
    }

    private fun loadFeatures() {
        features = listOf(
            createMotdFeature(this),
            createRememberFeature(this),
            createServerListFeature(this, mapper.treeToValue(config["list"], ListConfig::class.java)),
            createStatusFeature(this, mapper.treeToValue(config["status"], StatusConfig::class.java)),
            createPlayersListFeature(
                this,
                LuckPermsProvider.get(),
                mapper.treeToValue(config["players"], PlayersConfig::class.java)
            ),
        )
        features.forEach { (commands, listeners) ->
            commands.forEach(commandManager::registerCommand)
            listeners.forEach { proxy.eventManager.register(this, it) }
        }
    }

    fun reload() {
        unloadFeatures()
        config = loadConfig()
        loadFeatures()
    }

    private fun unloadFeatures() {
        proxy.eventManager.unregisterListeners(this)
        features.forEach { (commands, _, unload) ->
            commands.forEach(commandManager::unregisterCommand)
            unload()
        }
    }

    private fun loadConfig(): JsonNode {
        if (!dataFolder.exists()) {
            logger.info("No resource directory found, creating directory")
            dataFolder.mkdir()
        }
        val configFile = File(dataFolder, "config.yml")
        // does not overwrite or throw
        configFile.createNewFile()
        val config = mapper.readTree(configFile)
        logger.info("Loaded config.yml")
        return config
    }

    @CommandAlias("velocityutils")
    @CommandPermission("velocityutils.manage")
    private inner class VelocityUtilsCommand : BaseCommand() {
        private val mm = MiniMessage.miniMessage()
        private fun velocityUtilsMessage(msg: String) =
            mm.deserialize("<dark_gray>[</dark_gray><gray>VelocityUtils</gray><dark_gray>]</dark_gray> <gray>$msg</gray>")

        @Default
        @CatchUnknown
        @Subcommand("version")
        fun version(player: Player) {
            player.sendMessage(velocityUtilsMessage("Version $VERSION"))
        }

        @Subcommand("reload")
        fun reload(player: Player) {
            reload()
            player.sendMessage(velocityUtilsMessage("Reloaded"))
        }
    }
}
