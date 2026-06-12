package com.focusguard.data.dao

import androidx.room.*
import com.focusguard.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Update
    suspend fun update(session: Session)

    @Query("SELECT * FROM sessions ORDER BY startTime DESC LIMIT 50")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE startTime >= :from ORDER BY startTime DESC")
    fun getSessionsSince(from: Long): Flow<List<Session>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: Long): Session?
}

@Dao
interface AppRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AppRule)

    @Delete
    suspend fun delete(rule: AppRule)

    @Query("SELECT * FROM app_rules WHERE isAllowed = 1")
    fun getAllowedApps(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    fun getAll(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllSync(): List<AppRule>

    @Query("SELECT isAllowed FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun isAllowed(pkg: String): Boolean?
}

@Dao
interface VipContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: VipContact)

    @Delete
    suspend fun delete(contact: VipContact)

    @Query("SELECT * FROM vip_contacts")
    fun getAll(): Flow<List<VipContact>>

    @Query("SELECT * FROM vip_contacts")
    suspend fun getAllSync(): List<VipContact>
}

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: DailyStats)

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT 30")
    fun getLast30Days(): Flow<List<DailyStats>>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyStats?
}
