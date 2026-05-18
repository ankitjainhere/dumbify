package com.dumbify.app.data

class FakeSecurePrefs : SecurePrefs {
    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String): String? = store[key] as? String
    override fun putString(key: String, value: String?) {
        if (value == null) store.remove(key) else store[key] = value
    }
    override fun getBytes(key: String): ByteArray? = store[key] as? ByteArray
    override fun putBytes(key: String, value: ByteArray?) {
        if (value == null) store.remove(key) else store[key] = value
    }
    override fun getLong(key: String, default: Long): Long = (store[key] as? Long) ?: default
    override fun putLong(key: String, value: Long) { store[key] = value }
    override fun getInt(key: String, default: Int): Int = (store[key] as? Int) ?: default
    override fun putInt(key: String, value: Int) { store[key] = value }
    override fun contains(key: String): Boolean = store.containsKey(key)
    override fun remove(key: String) { store.remove(key) }
}
