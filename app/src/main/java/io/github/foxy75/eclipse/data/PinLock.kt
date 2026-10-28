package io.github.foxy75.eclipse.data

import android.content.Context
import android.util.Base64

/**
 * 6-digit PIN gate (PBKDF2-HmacSHA256, 120k). State lives in SharedPreferences;
 * the lockout counter is not a secret. 5 wrong attempts lock the app for 7 days,
 * after which the user gets a single attempt per 7 days.
 */
class PinLock(context: Context) {

    private val prefs = context.getSharedPreferences("eclipse_pin", Context.MODE_PRIVATE)

    data class PinCheck(
        val unlocked: Boolean,
        val attemptsLeft: Int,
        val lockedUntilMillis: Long
    ) {
        val locked: Boolean get() = lockedUntilMillis > System.currentTimeMillis()
    }

    companion object {
        const val PIN_LEN = 6
        const val MAX_ATTEMPTS = 5
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }

    fun isSet(): Boolean = prefs.contains("salt")

    fun set(pin: String) {
        val salt = Crypto.randomBytes(Crypto.SALT_LEN)
        val hash = Crypto.pbkdf2Sha256(pin, salt, Crypto.PBKDF2_ITERATIONS, Crypto.KEY_LEN)
        prefs.edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("fails", 0)
            .putLong("lockUntil", 0L)
            .apply()
    }

    fun check(pin: String): PinCheck {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.getLong("lockUntil", 0L)
        if (lockUntil != 0L && now < lockUntil) {
            return PinCheck(false, 0, lockUntil)
        }

        val salt = Base64.decode(prefs.getString("salt", null), Base64.NO_WRAP)
        val stored = Base64.decode(prefs.getString("hash", null), Base64.NO_WRAP)
        val candidate = Crypto.pbkdf2Sha256(pin, salt, Crypto.PBKDF2_ITERATIONS, Crypto.KEY_LEN)

        if (Crypto.constantTimeEquals(candidate, stored)) {
            prefs.edit().putInt("fails", 0).putLong("lockUntil", 0L).apply()
            return PinCheck(true, MAX_ATTEMPTS, 0L)
        }

        val wasWeekly = lockUntil != 0L
        var fails = prefs.getInt("fails", 0) + 1
        if (wasWeekly || fails >= MAX_ATTEMPTS) {
            val until = now + WEEK_MS
            prefs.edit().putLong("lockUntil", until).putInt("fails", 0).apply()
            return PinCheck(false, 0, until)
        }
        prefs.edit().putInt("fails", fails).apply()
        return PinCheck(false, MAX_ATTEMPTS - fails, 0L)
    }

    fun remainingLockedMillis(): Long {
        val until = prefs.getLong("lockUntil", 0L)
        val rem = until - System.currentTimeMillis()
        return if (rem > 0) rem else 0L
    }
}