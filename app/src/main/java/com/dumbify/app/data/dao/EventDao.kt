package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dumbify.app.data.entities.Event

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event): Long

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<Event>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun all(): List<Event>
}
