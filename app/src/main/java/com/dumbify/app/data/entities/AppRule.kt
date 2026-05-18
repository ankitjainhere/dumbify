package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BypassMode { DELAY, PIN, DELAY_AND_PIN }

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val isAllowed: Boolean,
    val bypassMode: BypassMode,
    val delaySeconds: Int,
    val grantedUntil: Long?,
)
