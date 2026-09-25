package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shots",
    foreignKeys = [
        ForeignKey(
            entity = ShootingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ShotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val shotIndex: Int,
    val score: Int,
    val isInnerTen: Boolean = false,
    val xRatio: Float = 0f, // -1.0 to 1.0 relative to target radius
    val yRatio: Float = 0f  // -1.0 to 1.0 relative to target radius
)
