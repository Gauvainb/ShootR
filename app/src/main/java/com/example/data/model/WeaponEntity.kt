package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weapons")
data class WeaponEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // Pistolet, Carabine, Revolver, Fusil, Air Comprimé
    val caliber: String, // 9x19 mm, .22 LR, .308 Win, 4.5 mm (.177), .38 Special, etc.
    val sights: String = "Visée ouverte", // Organes d'origine, Point rouge, Lunette, Dioptre
    val serialNumber: String = "",
    val totalRoundsFired: Int = 0,
    val notes: String = ""
)
