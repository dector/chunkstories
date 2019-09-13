//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server


import java.io.*
import kotlin.reflect.KProperty

class FileBasedUsersPrivileges {
    private val reloadme = mutableListOf<FileBackedList>()

    val admins: MutableSet<String> by FileBackedList(File("config/server-admins.txt"))
    val whitelist: MutableSet<String> by FileBackedList(File("config/server-whitelist.txt"))
    val bannedUsers: MutableSet<String> by FileBackedList(File("config/banned-users.txt"))
    val bannedIps: MutableSet<String> by FileBackedList(File("config/banned-ips.txt"))

    private inner class FileBackedList(val backingFile: File) {
        private val set = mutableSetOf<String>()

        init {
            load()
            reloadme += this
        }

        fun load() {
            if(backingFile.exists()) {
                set.clear()
                set.addAll(backingFile.readLines())
            }
        }

        fun save() {
            backingFile.parentFile.mkdirs()
            backingFile.writeText(set.joinToString(separator = "\n"))
        }

        operator fun getValue(fileBasedUsersPrivileges: FileBasedUsersPrivileges, property: KProperty<*>): MutableSet<String> {
            return set
        }
    }

    fun load() {
        reloadme.forEach(FileBackedList::load)
    }

    fun save() {
        reloadme.forEach(FileBackedList::save)
    }
}
