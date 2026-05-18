package com.dumbify.app.block

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dumbify.app.admin.PolicyEnforcer
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.policy.RuleStore
import com.dumbify.app.util.Clock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReSuspendWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val policyEnforcer: PolicyEnforcer,
    private val ruleStore: RuleStore,
    private val eventDao: EventDao,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pkg = inputData.getString(KEY_PKG) ?: return Result.failure()

        // Only re-suspend if grant hasn't been extended
        val rule = ruleStore.byPkg(pkg)
        val grantedUntil = rule?.grantedUntil ?: 0L
        if (grantedUntil > clock.nowMillis()) return Result.success() // grant extended; skip

        if (policyEnforcer.isDeviceOwner()) {
            runCatching { policyEnforcer.setPackagesSuspended(listOf(pkg), true) }
                .onFailure { e -> android.util.Log.w("ReSuspendWorker", "setPackagesSuspended failed for $pkg", e) }
        }
        ruleStore.setGrantedUntil(pkg, null)
        eventDao.insert(
            Event(
                timestamp = clock.nowMillis(),
                type = EventType.BLOCK_HIT,
                packageName = pkg,
                detail = "grant_expired",
            )
        )
        return Result.success()
    }

    companion object {
        const val KEY_PKG = "pkg"
        const val KEY_UNTIL = "until"

        fun buildRequest(pkg: String, until: Long): OneTimeWorkRequest {
            val delay = (until - System.currentTimeMillis()).coerceAtLeast(0L)
            return OneTimeWorkRequestBuilder<ReSuspendWorker>()
                .setInputData(workDataOf(KEY_PKG to pkg, KEY_UNTIL to until))
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("resuspend_$pkg")
                .build()
        }
    }
}
