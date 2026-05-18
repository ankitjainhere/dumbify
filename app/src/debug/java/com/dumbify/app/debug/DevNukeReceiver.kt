package com.dumbify.app.debug

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.dumbify.app.admin.DumbifyDeviceAdminReceiver

class DevNukeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.w(TAG, "DEV_NUKE invoked — clearing all DO restrictions and stepping down")
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DumbifyDeviceAdminReceiver::class.java)

        val allRestrictions = listOf(
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_APPS_CONTROL,
        )
        allRestrictions.forEach {
            runCatching { dpm.clearUserRestriction(admin, it) }
                .onFailure { e -> Log.w(TAG, "clearUserRestriction($it) failed", e) }
        }
        runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
            .onFailure { e -> Log.w(TAG, "setUninstallBlocked(false) failed", e) }
        runCatching { dpm.clearDeviceOwnerApp(context.packageName) }
            .onFailure { e -> Log.w(TAG, "clearDeviceOwnerApp failed", e) }

        Log.w(TAG, "DEV_NUKE complete — app should now be uninstallable normally")
    }

    companion object {
        const val TAG = "DumbifyDevNuke"
        const val ACTION = "com.dumbify.app.debug.DEV_NUKE"
    }
}
