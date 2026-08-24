package io.irodriguez.intentionalreading.data.local.state

import java.io.File
import java.io.FileOutputStream

internal sealed interface LocalStateFileRead {
    data object Absent : LocalStateFileRead

    data class Present(val bytes: ByteArray) : LocalStateFileRead

    data object Failed : LocalStateFileRead
}

internal class LocalStateFile(
    directory: File,
    private val beforeRename: () -> Unit,
) {
    constructor(directory: File) : this(directory, {})

    private val directory = directory
    private val target = File(directory, FILE_NAME)
    private val temporary = File(directory, "$FILE_NAME.tmp")

    fun read(): LocalStateFileRead = try {
        if (!target.exists()) {
            LocalStateFileRead.Absent
        } else {
            LocalStateFileRead.Present(target.readBytes())
        }
    } catch (failure: Exception) {
        LocalStateFileRead.Failed
    }

    fun write(bytes: ByteArray): Boolean = try {
        if (!directory.exists() && !directory.mkdirs()) return false
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        beforeRename()
        if (!temporary.renameTo(target)) {
            temporary.delete()
            false
        } else {
            true
        }
    } catch (failure: Exception) {
        temporary.delete()
        false
    }

    fun reset(): Boolean = try {
        val targetRemoved = !target.exists() || target.delete()
        val temporaryRemoved = !temporary.exists() || temporary.delete()
        targetRemoved && temporaryRemoved
    } catch (failure: Exception) {
        false
    }

    private companion object {
        const val FILE_NAME = "intentional-reading-v1.json"
    }
}
