package com.example.data

import com.example.data.dao.SessionDao
import com.example.data.dao.WeaponDao
import com.example.data.model.ShootingSessionEntity
import com.example.data.model.ShotEntity
import com.example.data.model.WeaponEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.hypot
import kotlin.math.max

class TirTrackerRepository(
    private val sessionDao: SessionDao,
    private val weaponDao: WeaponDao
) {
    val allSessions: Flow<List<ShootingSessionEntity>> = sessionDao.getAllSessions()
    val allWeapons: Flow<List<WeaponEntity>> = weaponDao.getAllWeapons()
    val totalSessionsCount: Flow<Int> = sessionDao.getTotalSessionsCount()
    val totalShotsCount: Flow<Int?> = sessionDao.getTotalShotsCount()

    fun getSession(sessionId: Long): Flow<ShootingSessionEntity?> = sessionDao.getSessionById(sessionId)

    fun getShotsForSession(sessionId: Long): Flow<List<ShotEntity>> = sessionDao.getShotsForSession(sessionId)

    suspend fun saveSession(session: ShootingSessionEntity): Long {
        return if (session.id == 0L) {
            sessionDao.insertSession(session)
        } else {
            sessionDao.updateSession(session)
            session.id
        }
    }

    suspend fun deleteSession(session: ShootingSessionEntity) {
        sessionDao.deleteSession(session)
    }

    suspend fun addShot(
        sessionId: Long,
        xRatio: Float,
        yRatio: Float,
        targetDiameterMm: Float = 500f
    ) {
        val currentShots = sessionDao.getShotsForSessionSync(sessionId)
        val shotIndex = currentShots.size + 1

        val dist = hypot(xRatio, yRatio)
        // Score calculation:
        // dist <= 0.05 -> 10 + InnerTen (mouche)
        // dist <= 0.10 -> 10
        // dist <= 0.20 -> 9
        // dist <= 0.30 -> 8
        // dist <= 0.40 -> 7
        // dist <= 0.50 -> 6
        // dist <= 0.60 -> 5
        // dist <= 0.70 -> 4
        // dist <= 0.80 -> 3
        // dist <= 0.90 -> 2
        // dist <= 1.00 -> 1
        // > 1.0 -> 0 (paille / hors cible)
        val (score, isInner) = calculateScore(dist)

        val newShot = ShotEntity(
            sessionId = sessionId,
            shotIndex = shotIndex,
            score = score,
            isInnerTen = isInner,
            xRatio = xRatio,
            yRatio = yRatio
        )
        sessionDao.insertShot(newShot)

        // Recompute session stats
        val updatedShots = currentShots + newShot
        val totalScore = updatedShots.sumOf { it.score }
        val innerTens = updatedShots.count { it.isInnerTen }
        val groupingMm = calculateGroupingMm(updatedShots, targetDiameterMm)

        val session = sessionDao.getSessionByIdSync(sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    totalScore = totalScore,
                    innerTens = innerTens,
                    shotsCount = updatedShots.size,
                    groupingDiameterMm = groupingMm
                )
            )
            // Also increment rounds fired for weapon if linked
            if (session.weaponId != null) {
                weaponDao.incrementRoundsFired(session.weaponId, 1)
            }
        }
    }

    suspend fun removeLastShot(sessionId: Long, targetDiameterMm: Float = 500f) {
        sessionDao.deleteLastShot(sessionId)
        val remainingShots = sessionDao.getShotsForSessionSync(sessionId)
        val totalScore = remainingShots.sumOf { it.score }
        val innerTens = remainingShots.count { it.isInnerTen }
        val groupingMm = calculateGroupingMm(remainingShots, targetDiameterMm)

        val session = sessionDao.getSessionByIdSync(sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    totalScore = totalScore,
                    innerTens = innerTens,
                    shotsCount = remainingShots.size,
                    groupingDiameterMm = groupingMm
                )
            )
            if (session.weaponId != null && session.shotsCount > 0) {
                weaponDao.incrementRoundsFired(session.weaponId, -1)
            }
        }
    }

    suspend fun clearSessionShots(sessionId: Long) {
        val count = sessionDao.getShotsForSessionSync(sessionId).size
        sessionDao.clearShotsForSession(sessionId)
        val session = sessionDao.getSessionByIdSync(sessionId)
        if (session != null) {
            sessionDao.updateSession(
                session.copy(
                    totalScore = 0,
                    innerTens = 0,
                    shotsCount = 0,
                    groupingDiameterMm = 0f
                )
            )
            if (session.weaponId != null && count > 0) {
                weaponDao.incrementRoundsFired(session.weaponId, -count)
            }
        }
    }

    suspend fun saveWeapon(weapon: WeaponEntity): Long {
        return if (weapon.id == 0L) {
            weaponDao.insertWeapon(weapon)
        } else {
            weaponDao.updateWeapon(weapon)
            weapon.id
        }
    }

    suspend fun deleteWeapon(weapon: WeaponEntity) {
        weaponDao.deleteWeapon(weapon)
    }

    companion object {
        fun calculateScore(distRatio: Float): Pair<Int, Boolean> {
            return when {
                distRatio <= 0.05f -> Pair(10, true) // Mouche
                distRatio <= 0.10f -> Pair(10, false)
                distRatio <= 0.20f -> Pair(9, false)
                distRatio <= 0.30f -> Pair(8, false)
                distRatio <= 0.40f -> Pair(7, false)
                distRatio <= 0.50f -> Pair(6, false)
                distRatio <= 0.60f -> Pair(5, false)
                distRatio <= 0.70f -> Pair(4, false)
                distRatio <= 0.80f -> Pair(3, false)
                distRatio <= 0.90f -> Pair(2, false)
                distRatio <= 1.00f -> Pair(1, false)
                else -> Pair(0, false)
            }
        }

        fun calculateGroupingMm(shots: List<ShotEntity>, targetDiameterMm: Float): Float {
            if (shots.size < 2) return 0f
            var maxDistRatio = 0f
            for (i in shots.indices) {
                for (j in i + 1 until shots.size) {
                    val dx = shots[i].xRatio - shots[j].xRatio
                    val dy = shots[i].yRatio - shots[j].yRatio
                    val d = hypot(dx, dy)
                    if (d > maxDistRatio) {
                        maxDistRatio = d
                    }
                }
            }
            // Target radius is targetDiameterMm / 2f
            val targetRadiusMm = targetDiameterMm / 2f
            return maxDistRatio * targetRadiusMm
        }
    }
}
