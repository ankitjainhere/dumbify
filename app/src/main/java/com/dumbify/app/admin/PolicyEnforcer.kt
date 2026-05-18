package com.dumbify.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.dumbify.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyEnforcer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin: ComponentName =
        ComponentName(context, DumbifyDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun applyRestrictions() {
        check(isDeviceOwner()) { "Cannot apply restrictions: not Device Owner" }
        RELEASE_ONLY_RESTRICTIONS.forEach { restriction ->
            if (!BuildConfig.DEBUG) {
                dpm.addUserRestriction(admin, restriction)
            }
        }
        ALWAYS_APPLIED_RESTRICTIONS.forEach { restriction ->
            dpm.addUserRestriction(admin, restriction)
        }
        if (!BuildConfig.DEBUG) {
            dpm.setUninstallBlocked(admin, context.packageName, true)
        }
    }

    fun clearAllRestrictionsForRemoval() {
        check(isDeviceOwner()) { "Cannot clear restrictions: not Device Owner" }
        (RELEASE_ONLY_RESTRICTIONS + ALWAYS_APPLIED_RESTRICTIONS).forEach { restriction ->
            runCatching { dpm.clearUserRestriction(admin, restriction) }
        }
        runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
    }

    fun clearDeviceOwner() {
        runCatching { dpm.clearDeviceOwnerApp(context.packageName) }
    }

    fun setPackagesSuspended(packages: List<String>, suspended: Boolean): List<String> {
        check(isDeviceOwner()) { "Cannot suspend packages: not Device Owner" }
        return dpm.setPackagesSuspended(admin, packages.toTypedArray(), suspended).toList()
    }

    companion object {
        private val RELEASE_ONLY_RESTRICTIONS = listOf(
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        )
        private val ALWAYS_APPLIED_RESTRICTIONS = listOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_APPS_CONTROL,
        )
    }
}
