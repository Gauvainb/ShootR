package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShootingSessionEntity
import com.example.data.model.ShotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM shooting_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<ShootingSessionEntity>>

    @Query("SELECT * FROM shooting_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<ShootingSessionEntity?>

    @Query("SELECT * FROM shooting_sessions WHERE id = :id")
    suspend fun getSessionByIdSync(id: Long): ShootingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ShootingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ShootingSessionEntity)

    @Delete
    suspend fun deleteSession(session: ShootingSessionEntity)

    @Query("SELECT * FROM shots WHERE sessionId = :sessionId ORDER BY shotIndex ASC")
    fun getShotsForSession(sessionId: Long): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE sessionId = :sessionId ORDER BY shotIndex ASC")
    suspend fun getShotsForSessionSync(sessionId: Long): List<ShotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: ShotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShots(shots: List<ShotEntity>)

    @Query("DELETE FROM shots WHERE sessionId = :sessionId AND shotIndex = (SELECT MAX(shotIndex) FROM shots WHERE sessionId = :sessionId)")
    suspend fun deleteLastShot(sessionId: Long)

    @Query("DELETE FROM shots WHERE sessionId = :sessionId")
    suspend fun clearShotsForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM shooting_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT SUM(shotsCount) FROM shooting_sessions")
    fun getTotalShotsCount(): Flow<Int?>
}
