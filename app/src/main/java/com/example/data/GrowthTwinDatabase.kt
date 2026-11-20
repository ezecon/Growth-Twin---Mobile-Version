package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlotEntity::class,
        PlotLogEntity::class,
        PlotEventEntity::class,
        HarvestEntity::class,
        AdvisorSessionEntity::class,
        TelemetryReadingEntity::class,
        UserAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GrowthTwinDatabase : RoomDatabase() {
    abstract fun plotDao(): PlotDao
    abstract fun plotLogDao(): PlotLogDao
    abstract fun plotEventDao(): PlotEventDao
    abstract fun harvestDao(): HarvestDao
    abstract fun advisorDao(): AdvisorDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun userAccountDao(): UserAccountDao

    companion object {
        @Volatile
        private var INSTANCE: GrowthTwinDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `email` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `mobile` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `approved` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`email`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): GrowthTwinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GrowthTwinDatabase::class.java,
                    "growthtwin.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
