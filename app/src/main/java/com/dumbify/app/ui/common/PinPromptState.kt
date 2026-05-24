package com.dumbify.app.ui.common

import com.dumbify.app.policy.PinManager

data class PinPromptState(
    val title: String,
    val subtitle: String,
    val attemptsRemaining: Int = PinManager.MAX_ATTEMPTS,
    val lockedForMinutes: Long = 0,
)
