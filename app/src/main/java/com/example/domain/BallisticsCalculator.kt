package com.example.domain

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/**
 * Représente un impact de tir sur la cible en millimètres (repère cartésien).
 */
data class Impact(
    val xMm: Float,
    val yMm: Float
)

/**
 * Représente une séance de tir avec ses impacts balistiques et son centre visé.
 */
data class ShotSession(
    val weapon: String,
    val caliber: String,
    val distanceMeters: Float,
    val impacts: List<Impact>,
    val targetCenter: Impact = Impact(0f, 0f)
)

/**
 * Statistiques balistiques complètes du groupement :
 * @property hMm Hauteur du groupement (Y max - Y min en mm)
 * @property lMm Largeur du groupement (X max - X min en mm)
 * @property esMm Écart extrême (Extreme Spread / diamètre max entre deux impacts en mm)
 * @property hPlusL Somme de la hauteur et largeur (H + L en mm, standard FFTir / TAR)
 * @property moa Valeur angulaire du groupement en Minutes of Angle (MOA exacte)
 * @property mpiDeltaXMm Décalage horizontal du Point Moyen d'Impact (MPI) par rapport au centre visé
 * @property mpiDeltaYMm Décalage vertical du Point Moyen d'Impact (MPI) par rapport au centre visé
 */
data class GroupStats(
    val hMm: Float,
    val lMm: Float,
    val esMm: Float,
    val hPlusL: Float,
    val moa: Float,
    val mpiDeltaXMm: Float,
    val mpiDeltaYMm: Float
)

/**
 * Moteur de calculs balistiques purs pour le tir sportif.
 */
object BallisticsCalculator {

    /**
     * Calcule l'intégralité des statistiques balistiques d'un groupe d'impacts.
     *
     * @param impacts Liste des impacts mesurés en millimètres.
     * @param targetCenter Coordonnées du centre visé (par défaut (0,0)).
     * @param distanceMeters Distance de tir en mètres (ex: 10m, 25m, 50m, 100m, 200m, 300m).
     * @return GroupStats contenant H, L, ES, H+L, MOA, et les coordonnées du MPI.
     */
    fun calculateStats(
        impacts: List<Impact>,
        targetCenter: Impact = Impact(0f, 0f),
        distanceMeters: Float
    ): GroupStats {
        if (impacts.isEmpty()) {
            return GroupStats(
                hMm = 0f,
                lMm = 0f,
                esMm = 0f,
                hPlusL = 0f,
                moa = 0f,
                mpiDeltaXMm = 0f,
                mpiDeltaYMm = 0f
            )
        }

        if (impacts.size == 1) {
            val single = impacts.first()
            val deltaX = single.xMm - targetCenter.xMm
            val deltaY = single.yMm - targetCenter.yMm
            return GroupStats(
                hMm = 0f,
                lMm = 0f,
                esMm = 0f,
                hPlusL = 0f,
                moa = 0f,
                mpiDeltaXMm = deltaX,
                mpiDeltaYMm = deltaY
            )
        }

        // 1. Calcul de H (hauteur max - min) et L (largeur max - min)
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        var sumX = 0.0
        var sumY = 0.0

        for (impact in impacts) {
            if (impact.xMm < minX) minX = impact.xMm
            if (impact.xMm > maxX) maxX = impact.xMm
            if (impact.yMm < minY) minY = impact.yMm
            if (impact.yMm > maxY) maxY = impact.yMm

            sumX += impact.xMm
            sumY += impact.yMm
        }

        val lMm = maxX - minX
        val hMm = maxY - minY
        val hPlusL = hMm + lMm

        // 2. Calcul de l'écart extrême (ES / Extreme Spread) : distance euclidienne maximale entre 2 impacts
        var maxDistance = 0.0f
        val count = impacts.size
        for (i in 0 until count) {
            val p1 = impacts[i]
            for (j in i + 1 until count) {
                val p2 = impacts[j]
                val dx = p1.xMm - p2.xMm
                val dy = p1.yMm - p2.yMm
                val dist = hypot(dx, dy)
                if (dist > maxDistance) {
                    maxDistance = dist
                }
            }
        }
        val esMm = maxDistance

        // 3. Calcul du barycentre (EMPI / Mean Point of Impact) et son écart au centre visé
        val meanX = (sumX / count).toFloat()
        val meanY = (sumY / count).toFloat()
        val mpiDeltaXMm = meanX - targetCenter.xMm
        val mpiDeltaYMm = meanY - targetCenter.yMm

        // 4. Formule MOA exacte : (ES en mm / (distance en m * 1000)) * (180 / PI) * 60
        val moa = if (distanceMeters > 0f) {
            val distanceMm = distanceMeters * 1000.0
            val radians = esMm.toDouble() / distanceMm
            val degrees = radians * (180.0 / Math.PI)
            (degrees * 60.0).toFloat()
        } else {
            0f
        }

        return GroupStats(
            hMm = hMm,
            lMm = lMm,
            esMm = esMm,
            hPlusL = hPlusL,
            moa = moa,
            mpiDeltaXMm = mpiDeltaXMm,
            mpiDeltaYMm = mpiDeltaYMm
        )
    }

    /**
     * Calcule la correction en clics de visée requise pour ramener le MPI au centre de la cible.
     * @param mpiDeltaMm Écart mesuré en mm
     * @param clickValueMoa Valeur d'un clic en MOA (ex: 1/4 MOA = 0.25f, 1/8 MOA = 0.125f, ou 0.1 MIL = 10mm à 100m)
     * @param distanceMeters Distance de tir en mètres
     */
    fun calculateClicksCorrection(
        mpiDeltaMm: Float,
        distanceMeters: Float,
        clickValueMoa: Float = 0.25f
    ): Int {
        if (distanceMeters <= 0f || clickValueMoa <= 0f) return 0
        val moaValue = ((mpiDeltaMm / (distanceMeters * 1000.0)) * (180.0 / Math.PI) * 60.0).toFloat()
        return kotlin.math.round(-moaValue / clickValueMoa).toInt()
    }
}
