package com.dumbify.app.policy

import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.Config
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RuleStore @Inject constructor(
    private val configDao: ConfigDao,
    private val appRuleDao: AppRuleDao,
    @Named("ownPackage") private val ownPackage: String,
) {
    suspend fun isBlocked(pkg: String, now: Long = System.currentTimeMillis()): Boolean {
        if (pkg in ALWAYS_ALLOWED || pkg == ownPackage) return false
        val rule = appRuleDao.byPkg(pkg)
        val grantedUntil = rule?.grantedUntil
        if (grantedUntil != null && grantedUntil > now) return false
        val config = configDao.get() ?: return false // no config = not onboarded; allow everything
        return evaluate(config, rule)
    }

    private fun evaluate(config: Config, rule: AppRule?): Boolean = when (config.mode) {
        BlockMode.ALLOWLIST -> rule?.isAllowed != true
        BlockMode.DENYLIST -> rule?.isAllowed == false
    }

    suspend fun upsert(rule: AppRule) = appRuleDao.upsert(rule)
    suspend fun delete(pkg: String) = appRuleDao.delete(pkg)
    suspend fun byPkg(pkg: String): AppRule? = appRuleDao.byPkg(pkg)
    suspend fun all(): List<AppRule> = appRuleDao.all()
    suspend fun setGrantedUntil(pkg: String, until: Long?) = appRuleDao.setGrantedUntil(pkg, until)

    suspend fun getConfig(): Config? = configDao.get()
    suspend fun upsertConfig(config: Config) = configDao.upsert(config)

    companion object {
        // Always-allowed packages — prevents user from locking themselves out
        // of essentials. AOSP packages; OEMs may use different names — refine later.
        val ALWAYS_ALLOWED = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.camera",
            "com.android.camera2",
            "com.android.settings",
            "com.android.systemui",
            "android",
        )
    }
}
