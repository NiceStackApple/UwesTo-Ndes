package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyScrollLogEntity::class,
        BlockRuleEntity::class,
        BattleRivalEntity::class,
        PermissionEventEntity::class
    ],
    version = 2,
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

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE daily_scroll_logs ADD COLUMN tiktokCount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE block_rules SET targetAppsCsv = 'instagram,tiktok,youtube,facebook,snapchat' WHERE targetAppsCsv = 'instagram,youtube,facebook,snapchat'"
                )
            }
        }

        fun getInstance(context: Context): UwesToDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UwesToDatabase::class.java,
                    "uwesto_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
