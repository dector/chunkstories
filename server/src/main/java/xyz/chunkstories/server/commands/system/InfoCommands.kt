//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands.system

import xyz.chunkstories.api.plugin.commands.Command
import xyz.chunkstories.api.plugin.commands.CommandEmitter
import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.ServerCommandBasic
import xyz.chunkstories.util.Colors.brightTurquoise
import xyz.chunkstories.util.Colors.red
import xyz.chunkstories.util.VersionInfo

/** Handles /uptime, /info commands  */
class InfoCommands(serverConsole: Server) : ServerCommandBasic(serverConsole) {

    init {
        server.pluginManager.registerCommand("uptime", this)
        server.pluginManager.registerCommand("info", this)
        server.pluginManager.registerCommand("help", this)
        server.pluginManager.registerCommand("plugins", this)
        server.pluginManager.registerCommand("mods", this)
    }

    override fun handleCommand(emitter: CommandEmitter, command: Command, arguments: Array<String>) = when (command.name) {
        "uptime" -> {
            emitter.sendMessage(brightTurquoise("The server has been running for " + server.uptime + " seconds."))
            true
        }
        "info" -> {
            emitter.sendMessage(brightTurquoise("The server's ip is " + server.publicIp))
            emitter.sendMessage(brightTurquoise("It's running version " + VersionInfo.versionJson.verboseVersion + " of the server software."))
            emitter.sendMessage(brightTurquoise("" + server.world))
            emitter.sendMessage(brightTurquoise("" + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "Mb used out of " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "Mb allocated"))
            true
        }
        "help" -> {
            emitter.sendMessage(brightTurquoise("Avaible commands :"))
            emitter.sendMessage(brightTurquoise(" /plugins"))
            emitter.sendMessage(brightTurquoise(" /mods"))
            emitter.sendMessage(brightTurquoise(" /list"))
            emitter.sendMessage(brightTurquoise(" /info"))
            emitter.sendMessage(brightTurquoise(" /uptime"))
            for (availableCommand in server.pluginManager.commands()) {
                emitter.sendMessage(brightTurquoise(" /" + availableCommand.name))
            }
            true
        }
        "plugins" -> {
            val list = server.pluginManager.activePlugins().joinToString { it.name }
            emitter.sendMessage(brightTurquoise("${list.length} active server plugins : $list"))
            true
        }
        "mods" -> {
            val list = server.content.modsManager.currentlyLoadedMods.joinToString { it.modInfo.name }
            emitter.sendMessage(red("${list.length} active server mods : $list"))
            true
        }
        else -> false
    }

}
