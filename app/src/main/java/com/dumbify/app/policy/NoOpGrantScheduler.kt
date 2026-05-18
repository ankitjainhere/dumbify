package com.dumbify.app.policy

import javax.inject.Inject

/** Stub implementation — real WorkManager-based scheduling comes in a later milestone. */
class NoOpGrantScheduler @Inject constructor() : GrantScheduler {
    override fun scheduleResuspend(pkg: String, until: Long) = Unit
}
