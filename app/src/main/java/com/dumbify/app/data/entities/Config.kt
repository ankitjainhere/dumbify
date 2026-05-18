package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockMode { ALLOWLIST, DENYLIST }
enum class UserRole { SELF, MANAGED }

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val id: Int = 0,
    val mode: BlockMode,
    val userRole: UserRole,
    val customMessage: String,
    val launcherEnabled: Boolean,
    val onboardingComplete: Boolean,
)
