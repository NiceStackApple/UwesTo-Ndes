package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyScrollDao {
    @Query("SELECT * FROM daily_scroll_logs WHERE dateKey = :dateKey LIMIT 1")
    fun observeDay(dateKey: String): Flow<DailyScrollLogEntity?>

    @Query("SELECT * FROM daily_scroll_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDay(dateKey: String): DailyScrollLogEntity?

    @Query("SELECT * FROM daily_scroll_logs ORDER BY dateKey DESC LIMIT :limit")
    fun observeRecentDays(limit: Int = 14): Flow<List<DailyScrollLogEntity>>

    @Query("SELECT * FROM daily_scroll_logs ORDER BY dateKey DESC LIMIT :limit")
    suspend fun getRecentDays(limit: Int = 30): List<DailyScrollLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyScrollLogEntity)
}

@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rules ORDER BY id ASC")
    fun observeAllRules(): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules")
    suspend fun getAllRules(): List<BlockRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRuleEntity): Long

    @Update
    suspend fun update(rule: BlockRuleEntity)

    @Query("DELETE FROM block_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface BattleRivalDao {
    @Query("SELECT * FROM battle_rivals ORDER BY id ASC")
    fun observeAllRivals(): Flow<List<BattleRivalEntity>>

    @Query("SELECT * FROM battle_rivals")
    suspend fun getAllRivals(): List<BattleRivalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rival: BattleRivalEntity): Long

    @Update
    suspend fun update(rival: BattleRivalEntity)

    @Query("DELETE FROM battle_rivals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PermissionEventDao {
    @Query("SELECT * FROM permission_events ORDER BY timestamp DESC LIMIT 12")
    fun observeRecentEvents(): Flow<List<PermissionEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PermissionEventEntity)
}
