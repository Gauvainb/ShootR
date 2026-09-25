package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WeaponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeaponDao {
    @Query("SELECT * FROM weapons ORDER BY name ASC")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Query("SELECT * FROM weapons WHERE id = :id")
    suspend fun getWeaponById(id: Long): WeaponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeapon(weapon: WeaponEntity): Long

    @Update
    suspend fun updateWeapon(weapon: WeaponEntity)

    @Delete
    suspend fun deleteWeapon(weapon: WeaponEntity)

    @Query("UPDATE weapons SET totalRoundsFired = totalRoundsFired + :rounds WHERE id = :weaponId")
    suspend fun incrementRoundsFired(weaponId: Long, rounds: Int)
}
