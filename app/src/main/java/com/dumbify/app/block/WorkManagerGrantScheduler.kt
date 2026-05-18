package com.dumbify.app.block

import android.content.Context
import androidx.work.WorkManager
import com.dumbify.app.policy.GrantScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGrantScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : GrantScheduler {
    override fun scheduleResuspend(pkg: String, until: Long) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag("resuspend_$pkg")
        wm.enqueue(ReSuspendWorker.buildRequest(pkg, until))
    }
}
