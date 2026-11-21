package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyScrollLogEntity::class,
        BlockRuleEntity::class,
        BattleRivalEntity::class,
        PermissionEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UwesToDatabase : RoomDatabase() {
    abstract fun dailyScrollDao(): DailyScrollDao
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun battleRivalDao(): BattleRivalDao
    abstract fun permissionEventDao(): PermissionEventDao

    companion object {
        @Volatile
        private var INSTANCE: UwesToDatabase? = null

        fun getInstance(context: Context): UwesToDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UwesToDatabase::class.java,
                    "uwesto_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
