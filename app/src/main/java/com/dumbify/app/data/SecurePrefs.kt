package com.dumbify.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SecurePrefs {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getBytes(key: String): ByteArray?
    fun putBytes(key: String, value: ByteArray?)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun contains(key: String): Boolean
    fun remove(key: String)
}

@Singleton
class EncryptedSecurePrefs @Inject constructor(
    @ApplicationContext context: Context,
) : SecurePrefs {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "dumbify_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getBytes(key: String): ByteArray? =
        prefs.getString(key, null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }

    override fun putBytes(key: String, value: ByteArray?) {
        prefs.edit().apply {
            if (value == null) remove(key)
            else putString(key, android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP))
        }.apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

object SecurePrefsKeys {
    const val REMOVAL_PIN_HASH = "removal_pin_hash"
    const val REMOVAL_PIN_SALT = "removal_pin_salt"
    const val BYPASS_PIN_HASH = "bypass_pin_hash"
    const val BYPASS_PIN_SALT = "bypass_pin_salt"
    const val PIN_FAIL_COUNT = "pin_fail_count"
    const val PIN_COOLDOWN_UNTIL = "pin_cooldown_until"
    const val WIZARD_STEP = "wizard_step"
}
