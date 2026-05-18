package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dumbify.app.data.entities.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 0 LIMIT 1")
    suspend fun get(): Config?

    @Query("SELECT * FROM config WHERE id = 0 LIMIT 1")
    fun observe(): Flow<Config?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: Config)
}
