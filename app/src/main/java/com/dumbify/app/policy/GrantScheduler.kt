package com.dumbify.app.policy

interface GrantScheduler {
    /** Schedule a re-suspend for [pkg] at epoch millis [until]. */
    fun scheduleResuspend(pkg: String, until: Long)
}
