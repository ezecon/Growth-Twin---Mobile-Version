package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plots")
data class PlotEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val cropId: String,
    val name: String,
    val status: String,
    val day: Int,
    val health: Int,
    val stateJson: String,
    val updatedAtMs: Long
)

@Entity(tableName = "plot_logs", primaryKeys = ["plotId", "day"])
data class PlotLogEntity(
    val plotId: String,
    val day: Int,
    val date: String,
    val stageId: String,
    val stageBn: String,
    val stageEn: String,
    val status: String,
    val aw: Double,
    val ph: Double,
    val ec: Double,
    val n: Double,
    val p: Double,
    val k: Double,
    val dis: Int,
    val health: Double,
    val pumpOn: Boolean,
    val irrigationMm: Double,
    val estYieldKg: Int,
    val tmax: Double,
    val tmin: Double,
    val rainMm: Double,
    val rh: Int
)

@Entity(tableName = "plot_events")
data class PlotEventEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val plotId: String,
    val day: Int,
    val kind: String,
    val textBn: String,
    val textEn: String,
    val eventKey: String?,
    val createdAtMs: Long
)

@Entity(tableName = "harvest_records")
data class HarvestEntity(
    @PrimaryKey val plotId: String,
    val userId: String,
    val cropId: String,
    val soil: String,
    val area: Double,
    val yieldKg: Int,
    val yieldKgPerDecimal: Double,
    val revenue: Int,
    val cost: Int,
    val profit: Int,
    val days: Int,
    val health: Int,
    val startDate: String,
    val outcome: String,
    val source: String
)

@Entity(tableName = "advisor_sessions")
data class AdvisorSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val prompt: String,
    val slotsJson: String,
    val askedSlotsCsv: String,
    val reviewStatus: String, // none | requested | reviewed
    val expertVerdict: String?,
    val expertNote: String?,
    val updatedAtMs: Long
)

@Entity(tableName = "telemetry_readings")
data class TelemetryReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plotId: String,
    val source: String, // real | simulation
    val deviceId: String,
    val sequence: Int,
    val timestampIso: String,
    val rawMoistureAdc: Int,
    val calibratedMoisturePct: Double,
    val predictedMoisturePct: Double,
    val correctedMoisturePct: Double,
    val residualPct: Double?,
    val measurementUsed: Boolean,
    val quality: String,
    val temperatureC: Double?,
    val humidityPct: Double?
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val userId: String,
    val name: String,
    val mobile: String,
    val role: String, // farmer | expert | admin
    val passwordHash: String,
    val approved: Boolean,
    val createdAtMs: Long
)

