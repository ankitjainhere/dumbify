package com.dumbify.app.policy

import com.dumbify.app.util.Clock

class FakeClock(var current: Long = 0L) : Clock {
    override fun nowMillis(): Long = current
    fun advance(millis: Long) { current += millis }
}
