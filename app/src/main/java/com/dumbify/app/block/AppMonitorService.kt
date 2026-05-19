package com.dumbify.app.block

import android.app.AppOpsManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dumbify.app.R
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.policy.RuleStore
import com.dumbify.app.util.Clock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject lateinit var ruleStore: RuleStore
    @Inject lateinit var policyEnforcer: PolicyEnforcer
    @Inject lateinit var clock: Clock
    @Inject lateinit var eventDao: EventDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var lastSuspendedPkg: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (!hasUsageStatsPermission()) {
            postUsageStatsNotification()
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch { monitorLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun monitorLoop() {
        while (true) {
            val fgPkg = queryForegroundPackage()
            if (fgPkg == null) {
                lastSuspendedPkg = null
            } else if (fgPkg != packageName) {
                val now = clock.nowMillis()
                if (ruleStore.isBlocked(fgPkg, now) && fgPkg != lastSuspendedPkg) {
                    runCatching {
                        policyEnforcer.setPackagesSuspended(listOf(fgPkg), true)
                        lastSuspendedPkg = fgPkg
                    }.onSuccess {
                        startActivity(BlockScreenActivity.intent(this@AppMonitorService, fgPkg))
                    }.onFailure { e ->
                        android.util.Log.w("AppMonitorService", "setPackagesSuspended failed for $fgPkg", e)
                    }
                    scope.launch {
                        eventDao.insert(
                            Event(
                                timestamp = now,
                                type = EventType.BLOCK_HIT,
                                packageName = fgPkg,
                                detail = null,
                            )
                        )
                    }
                } else if (fgPkg != lastSuspendedPkg) {
                    lastSuspendedPkg = null  // clear when a different (non-blocked) pkg is in foreground
                }
            }
            delay(500L)
        }
    }

    private fun queryForegroundPackage(): String? {
        val usm = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 3_000L, now)
        val event = UsageEvents.Event()
        var lastPkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPkg = event.packageName
            }
        }
        return lastPkg
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun postUsageStatsNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Dumbify needs Usage Access")
            .setContentText("Tap to grant Usage Access so Dumbify can monitor apps.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(USAGE_PERM_NOTIF_ID, notif)
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.notif_foreground_service))
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dumbify Protection",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "dumbify_protection"
        const val NOTIF_ID = 1
        const val USAGE_PERM_NOTIF_ID = 2

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AppMonitorService::class.java),
            )
        }
    }
}
