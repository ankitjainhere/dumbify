package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dumbify.app.data.entities.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules")
    fun observeAll(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun all(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun byPkg(pkg: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("UPDATE app_rules SET grantedUntil = :until WHERE packageName = :pkg")
    suspend fun setGrantedUntil(pkg: String, until: Long?)
}
