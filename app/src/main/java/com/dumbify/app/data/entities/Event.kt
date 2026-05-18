package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    BLOCK_HIT,
    UNBLOCK_REQUEST,
    UNBLOCK_GRANTED,
    UNBLOCK_DENIED,
    RULE_EDIT,
    PIN_FAIL,
    MODE_CHANGE,
    REMOVAL_ATTEMPT,
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: EventType,
    val packageName: String?,
    val detail: String?,
)
