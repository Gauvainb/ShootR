package com.example.domain

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class CalibrationReference(
    val id: String,
    val targetType: String,
    val label: String,
    val shortName: String,
    val diameterMm: Float,
    val radiusMm: Float = diameterMm / 2f,
    val description: String
)

object TargetCalibrationDefaults {
    val references = listOf(
        CalibrationReference(
            id = "c50_black",
            targetType = "C50 (25m/50m)",
            label = "C50 • Visuel noir (200 mm)",
            shortName = "Noir 200 mm",
            diameterMm = 200f,
            description = "Bord extérieur de la zone 7 sur cible C50 (200 mm de diamètre)"
        ),
        CalibrationReference(
            id = "carabine_10m_black",
            targetType = "Carabine 10m (ISSF)",
            label = "Carabine 10m • Visuel noir (30.5 mm)",
            shortName = "Noir 30.5 mm",
            diameterMm = 30.5f,
            description = "Bord extérieur de la zone 4 sur cible Carabine 10m ISSF (30.5 mm)"
        ),
        CalibrationReference(
            id = "pistolet_10m_black",
            targetType = "Pistolet 10m (ISSF)",
            label = "Pistolet 10m • Visuel noir (59.5 mm)",
            shortName = "Noir 59.5 mm",
            diameterMm = 59.5f,
            description = "Bord extérieur de la zone 7 sur cible Pistolet 10m ISSF (59.5 mm)"
        ),
        CalibrationReference(
            id = "c200_black",
            targetType = "C200 (200m)",
            label = "C200 • Visuel noir (400 mm)",
            shortName = "Noir 400 mm",
            diameterMm = 400f,
            description = "Bord extérieur de la zone 6 sur cible C200 (400 mm)"
        ),
        CalibrationReference(
            id = "c50_ring10",
            targetType = "C50 (25m/50m)",
            label = "C50 • Anneau du 10 (50 mm)",
            shortName = "Anneau 10 (50 mm)",
            diameterMm = 50f,
            description = "Cercle officiel de la zone 10 (50 mm de diamètre)"
        ),
        CalibrationReference(
            id = "hunter_50",
            targetType = "Cible Hunter / Précision",
            label = "Hunter • Visuel (50 mm)",
            shortName = "Visuel 50 mm",
            diameterMm = 50f,
            description = "Visuel de précision Hunter / Benchrest 50 mm"
        ),
        CalibrationReference(
            id = "custom",
            targetType = "Personnalisé",
            label = "Diamètre personnalisé...",
            shortName = "Personnalisé",
            diameterMm = 100f,
            description = "Saisie manuelle d'un diamètre en millimètres"
        )
    )

    fun getReferenceForTargetType(targetType: String): CalibrationReference {
        return references.find { it.targetType.equals(targetType, ignoreCase = true) }
            ?: references.first()
    }

    fun calculateScale(center: Offset, edge: Offset, referenceRadiusMm: Float): Float {
        val distPx = hypot(edge.x - center.x, edge.y - center.y)
        return if (referenceRadiusMm > 0f) distPx / referenceRadiusMm else 1f
    }
}
