package io.github.foxy75.eclipse.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM backed by a non-exportable key in AndroidKeyStore.
 * Also hosts the shared PBKDF2-HmacSHA256 primitive (manual, since
 * minSdk 23 has no PBKDF2WithHmacSHA256 in the platform).
 */
object Crypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "eclipse.master"
    private const val IV_LEN = 12
    private const val TAG_LEN = 128
    const val SALT_LEN = 16
    const val KEY_LEN = 32
    const val PBKDF2_ITERATIONS = 120_000

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /** Returns (creating on demand) the 32-byte master key. */
    fun masterKey(): SecretKey {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encryptWithMaster(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext)
        return iv + ct
    }

    fun decryptWithMaster(payload: ByteArray): ByteArray {
        val iv = payload.copyOfRange(0, IV_LEN)
        val ct = payload.copyOfRange(IV_LEN, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(TAG_LEN, iv))
        return cipher.doFinal(ct)
    }

    fun encryptWithKeyBytes(key: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        val iv = cipher.iv
        return iv + cipher.doFinal(plaintext)
    }

    fun decryptWithKeyBytes(key: ByteArray, payload: ByteArray): ByteArray {
        val iv = payload.copyOfRange(0, IV_LEN)
        val ct = payload.copyOfRange(IV_LEN, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LEN, iv))
        return cipher.doFinal(ct)
    }

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandomHolder.random.nextBytes(it) }

    fun pbkdf2Sha256(password: String, salt: ByteArray, iterations: Int, dkLen: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val pass = password.toByteArray(Charsets.UTF_8)
        mac.init(SecretKeySpec(pass, "HmacSHA256"))
        val hLen = mac.macLength
        val blocks = (dkLen + hLen - 1) / hLen
        val out = ByteArray(blocks * hLen)
        val block = ByteArray(salt.size + 4)
        System.arraycopy(salt, 0, block, 0, salt.size)
        for (i in 1..blocks) {
            block[salt.size] = (i ushr 24).toByte()
            block[salt.size + 1] = (i ushr 16).toByte()
            block[salt.size + 2] = (i ushr 8).toByte()
            block[salt.size + 3] = i.toByte()
            var u = mac.doFinal(block)
            val t = u.copyOf()
            for (j in 2..iterations) {
                u = mac.doFinal(u)
                for (k in t.indices) t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
            }
            System.arraycopy(t, 0, out, (i - 1) * hLen, hLen)
        }
        return out.copyOf(dkLen)
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    private object SecureRandomHolder {
        val random = java.security.SecureRandom()
    }
}