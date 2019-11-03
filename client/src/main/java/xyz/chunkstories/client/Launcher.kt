package xyz.chunkstories.client

import xyz.chunkstories.graphics.GraphicsBackendsEnum
import xyz.chunkstories.util.VersionInfo
import kotlin.system.exitProcess

fun main(launchArguments: Array<String>) {
    val argumentsMap = mutableMapOf<String, String>()
    for (launchArgument in launchArguments) {
        if (launchArgument.startsWith("--")) {
            val stripped = launchArgument.removePrefix("--")

            if (launchArgument.contains('=')) {
                val firstIndex = stripped.indexOf('=')
                val argName = stripped.substring(0, firstIndex)
                val argValue = stripped.substring(firstIndex + 1, stripped.length).removeSurrounding("\"")

                argumentsMap[argName] = argValue
            } else {
                argumentsMap[stripped] = "true"
            }
        } else {
            println("Unrecognized launch argument: $launchArgument")
        }
    }

    if (argumentsMap["help"] != null) {
        printHelp()
        exitProcess(0)
    }

    ClientImplementation(argumentsMap)
}

private fun printHelp() {
    println("""
                Chunk Stories Client version: ${VersionInfo.versionJson.verboseVersion}

                Available commandline options:
                --core=... Specifies the folder/file to use as the base content
                --mods=... Specifies some mods to load
                --backend=[${GraphicsBackendsEnum.values()}] Forces a specific backend to be used.

                Backend-specific options:

                Vulkan-specific options:
                --enableValidation Enables the validation layers

                OpenGL-specific options:
            """.trimIndent())
}
