package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shooting_sessions")
data class ShootingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val targetType: String = "C50 (25m/50m)",
    val distanceMeters: Int = 25,
    val weaponId: Long? = null,
    val weaponName: String = "",
    val ammoBrand: String = "",
    val ammoCaliber: String = "",
    val position: String = "Debout",
    val totalScore: Int = 0,
    val innerTens: Int = 0,
    val shotsCount: Int = 0,
    val groupingDiameterMm: Float = 0f,
    val photoUri: String? = null,
    val notes: String = ""
)
