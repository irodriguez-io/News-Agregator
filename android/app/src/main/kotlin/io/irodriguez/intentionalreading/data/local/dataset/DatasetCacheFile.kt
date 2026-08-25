package io.irodriguez.intentionalreading.data.local.dataset

import java.io.File
import java.io.FileOutputStream

internal sealed interface DatasetCacheFileRead {
    data object Absent : DatasetCacheFileRead

    data class Present(val bytes: ByteArray) : DatasetCacheFileRead

    data object Failed : DatasetCacheFileRead
}

internal class DatasetCacheFile(
    private val directory: File,
    private val fileName: String,
    private val beforeRename: () -> Unit = {},
) {
    private val target = File(directory, fileName)
    private val temporary = File(directory, "$fileName.tmp")

    fun read(): DatasetCacheFileRead = try {
        if (!target.exists()) {
            DatasetCacheFileRead.Absent
        } else {
            DatasetCacheFileRead.Present(target.readBytes())
        }
    } catch (failure: Exception) {
        DatasetCacheFileRead.Failed
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

    fun remove(): Boolean = try {
        val targetRemoved = !target.exists() || target.delete()
        val temporaryRemoved = !temporary.exists() || temporary.delete()
        targetRemoved && temporaryRemoved
    } catch (failure: Exception) {
        false
    }
}
