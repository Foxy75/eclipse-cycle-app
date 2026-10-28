package io.github.foxy75.eclipse.data

import java.nio.charset.Charset

/**
 * Encrypted backup format (`.ecl`):
 * magic(4) "ECL1" || salt(16) || AES-GCM( iv(12) || ciphertext || tag )
 * Key = PBKDF2-HmacSHA256(passphrase, salt, 120k, 256). The passphrase is never stored.
 */
object Backup {
    private val MAGIC = byteArrayOf('E'.code.toByte(), 'C'.code.toByte(), 'L'.code.toByte(), '1'.code.toByte())

    fun export(state: AppState, passphrase: String): ByteArray {
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val key = Crypto.pbkdf2Sha256(passphrase, salt, Crypto.PBKDF2_ITERATIONS, Crypto.KEY_LEN)
        val plain = Serialization.toJson(state).toByteArray(Charset.forName("UTF-8"))
        val cipher = Crypto.encryptWithKeyBytes(key, plain)
        val out = ByteArray(MAGIC.size + salt.size + cipher.size)
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.size)
        System.arraycopy(salt, 0, out, MAGIC.size, salt.size)
        System.arraycopy(cipher, 0, out, MAGIC.size + salt.size, cipher.size)
        return out
    }

    /** Returns the decoded state, or null if the passphrase is wrong or the blob is corrupt. */
    fun `import`(blob: ByteArray, passphrase: String): AppState? {
        if (blob.size < MAGIC.size + Crypto.SALT_LEN) return null
        for (i in MAGIC.indices) if (blob[i] != MAGIC[i]) return null
        return try {
            val salt = blob.copyOfRange(MAGIC.size, MAGIC.size + Crypto.SALT_LEN)
            val cipher = blob.copyOfRange(MAGIC.size + Crypto.SALT_LEN, blob.size)
            val key = Crypto.pbkdf2Sha256(passphrase, salt, Crypto.PBKDF2_ITERATIONS, Crypto.KEY_LEN)
            val plain = Crypto.decryptWithKeyBytes(key, cipher)
            Serialization.fromJson(String(plain, Charset.forName("UTF-8")))
        } catch (_: Exception) {
            null
        }
    }
}