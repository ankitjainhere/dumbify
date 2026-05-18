package com.dumbify.app.util

import javax.inject.Inject
import javax.inject.Singleton

interface Clock {
    fun nowMillis(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
