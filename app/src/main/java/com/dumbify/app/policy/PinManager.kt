package com.dumbify.app.policy

import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.SecurePrefsKeys
import com.dumbify.app.util.Clock
import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val prefs: SecurePrefs,
    private val clock: Clock,
) {
    enum class Scope { REMOVAL, BYPASS }
    enum class VerifyResult { SUCCESS, WRONG, NOT_SET, COOLDOWN }

    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun setPin(scope: Scope, pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val saltedPin = saltedInput(pin, salt)
        val hash = argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY_KB, ARGON2_PARALLELISM, saltedPin.toCharArray())
        prefs.putString(hashKey(scope), hash)
        prefs.putBytes(saltKey(scope), salt)
    }

    fun hasPin(scope: Scope): Boolean = prefs.contains(hashKey(scope))

    fun clearPin(scope: Scope) {
        prefs.remove(hashKey(scope))
        prefs.remove(saltKey(scope))
    }

    fun verify(scope: Scope, pin: String): VerifyResult {
        val cooldownUntil = prefs.getLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL)
        if (clock.nowMillis() < cooldownUntil) return VerifyResult.COOLDOWN

        val hash = prefs.getString(hashKey(scope)) ?: return VerifyResult.NOT_SET
        val salt = prefs.getBytes(saltKey(scope)) ?: return VerifyResult.NOT_SET

        val saltedPin = saltedInput(pin, salt)
        val matches = argon2.verify(hash, saltedPin.toCharArray())

        return if (matches) {
            prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, 0)
            prefs.putLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL, 0)
            VerifyResult.SUCCESS
        } else {
            val fails = prefs.getInt(SecurePrefsKeys.PIN_FAIL_COUNT) + 1
            prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, fails)
            if (fails >= MAX_ATTEMPTS) {
                prefs.putLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL, clock.nowMillis() + COOLDOWN_MS)
                prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, 0)
            }
            VerifyResult.WRONG
        }
    }

    /** Returns minutes until PIN cooldown expires, rounded up. 0 if no cooldown active. */
    fun cooldownRemainingMinutes(): Long {
        val cooldownUntil = prefs.getLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL)
        val remaining = cooldownUntil - clock.nowMillis()
        if (remaining <= 0) return 0L
        return (remaining + 59_999L) / 60_000L // ceil division
    }

    private fun saltedInput(pin: String, salt: ByteArray): String {
        // Argon2-jvm's verify(hash, chars) handles its own embedded salt;
        // we additionally pepper with our SecurePrefs salt by prefixing,
        // so changing the salt invalidates the hash even for the same PIN.
        return java.util.Base64.getEncoder().withoutPadding().encodeToString(salt) + ":" + pin
    }

    private fun hashKey(scope: Scope): String = when (scope) {
        Scope.REMOVAL -> SecurePrefsKeys.REMOVAL_PIN_HASH
        Scope.BYPASS -> SecurePrefsKeys.BYPASS_PIN_HASH
    }

    private fun saltKey(scope: Scope): String = when (scope) {
        Scope.REMOVAL -> SecurePrefsKeys.REMOVAL_PIN_SALT
        Scope.BYPASS -> SecurePrefsKeys.BYPASS_PIN_SALT
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        const val COOLDOWN_MS = 5L * 60L * 1000L
        const val SALT_BYTES = 16
        // Argon2id parameters — tuned conservatively for low-end devices.
        const val ARGON2_ITERATIONS = 3
        const val ARGON2_MEMORY_KB = 65_536 // 64 MB
        const val ARGON2_PARALLELISM = 2
    }
}
