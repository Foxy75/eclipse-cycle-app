package io.github.foxy75.eclipse.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Persists the app state to a single AES-256-GCM encrypted file `data.enc`.
 * Writes are atomic: temp -> fsync -> rename, keeping a `.bak` of the last good state.
 */
class EncryptedStore(context: Context) {

    private val dir: File = context.filesDir
    private val file = File(dir, "data.enc")
    private val tmp = File(dir, "data.tmp")
    private val bak = File(dir, "data.bak")
    private val mutex = Mutex()

    suspend fun load(): AppState = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = readCandidates()
            if (bytes == null) AppState() else decode(bytes) ?: AppState()
        }
    }

    suspend fun save(state: AppState) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bytes = Crypto.encryptWithMaster(Serialization.toJson(state).toByteArray(Charsets.UTF_8))
            writeAtomic(bytes)
        }
    }

    private fun readCandidates(): ByteArray? {
        if (file.exists()) file.readBytes().let { return it }
        if (bak.exists()) bak.readBytes().let { return it }
        return null
    }

    private fun decode(bytes: ByteArray): AppState? = try {
        Serialization.fromJson(String(Crypto.decryptWithMaster(bytes), Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }

    private fun writeAtomic(bytes: ByteArray) {
        FileOutputStream(tmp).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (file.exists()) {
            bak.delete()
            file.renameTo(bak)
        }
        tmp.renameTo(file)
    }
}