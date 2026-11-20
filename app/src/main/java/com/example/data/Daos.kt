package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlotDao {
    @Query("SELECT * FROM plots ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<PlotEntity>>

    @Query("SELECT * FROM plots ORDER BY updatedAtMs DESC")
    suspend fun getAll(): List<PlotEntity>

    @Query("SELECT * FROM plots WHERE id = :id")
    suspend fun getById(id: String): PlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plot: PlotEntity)

    @Query("DELETE FROM plots WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlotLogDao {
    @Query("SELECT * FROM plot_logs WHERE plotId = :plotId ORDER BY day ASC")
    fun observeForPlot(plotId: String): Flow<List<PlotLogEntity>>

    @Query("SELECT * FROM plot_logs WHERE plotId = :plotId ORDER BY day ASC")
    suspend fun getForPlot(plotId: String): List<PlotLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<PlotLogEntity>)

    @Query("DELETE FROM plot_logs WHERE plotId = :plotId")
    suspend fun deleteForPlot(plotId: String)
}

@Dao
interface PlotEventDao {
    @Query("SELECT * FROM plot_events WHERE plotId = :plotId ORDER BY rowId DESC LIMIT 80")
    fun observeForPlot(plotId: String): Flow<List<PlotEventEntity>>

    @Insert
    suspend fun insertAll(events: List<PlotEventEntity>)

    @Query("DELETE FROM plot_events WHERE plotId = :plotId")
    suspend fun deleteForPlot(plotId: String)
}

@Dao
interface HarvestDao {
    @Query("SELECT * FROM harvest_records ORDER BY startDate DESC")
    fun observeAll(): Flow<List<HarvestEntity>>

    @Query("SELECT * FROM harvest_records")
    suspend fun getAll(): List<HarvestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(harvest: HarvestEntity)
}

@Dao
interface AdvisorDao {
    @Query("SELECT * FROM advisor_sessions ORDER BY updatedAtMs DESC LIMIT 20")
    fun observeAll(): Flow<List<AdvisorSessionEntity>>

    @Query("SELECT * FROM advisor_sessions WHERE reviewStatus = 'requested' ORDER BY updatedAtMs DESC")
    fun observeReviewQueue(): Flow<List<AdvisorSessionEntity>>

    @Query("SELECT * FROM advisor_sessions WHERE sessionId = :id")
    suspend fun getById(id: String): AdvisorSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: AdvisorSessionEntity)
}

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM telemetry_readings WHERE plotId = :plotId ORDER BY id DESC LIMIT 100")
    fun observeForPlot(plotId: String): Flow<List<TelemetryReadingEntity>>

    @Query("SELECT * FROM telemetry_readings WHERE plotId = :plotId ORDER BY id DESC LIMIT 100")
    suspend fun getForPlot(plotId: String): List<TelemetryReadingEntity>

    @Insert
    suspend fun insertAll(items: List<TelemetryReadingEntity>)

    @Query("DELETE FROM telemetry_readings WHERE plotId = :plotId")
    suspend fun deleteForPlot(plotId: String)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserAccountEntity)
}

