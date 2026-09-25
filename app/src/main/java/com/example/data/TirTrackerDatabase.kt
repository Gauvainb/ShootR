package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.SessionDao
import com.example.data.dao.WeaponDao
import com.example.data.model.ShootingSessionEntity
import com.example.data.model.ShotEntity
import com.example.data.model.WeaponEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WeaponEntity::class,
        ShootingSessionEntity::class,
        ShotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TirTrackerDatabase : RoomDatabase() {

    abstract fun weaponDao(): WeaponDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: TirTrackerDatabase? = null

        fun getDatabase(context: Context): TirTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TirTrackerDatabase::class.java,
                    "tir_tracker_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial default weapons and sample session
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            seedInitialData(database)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: TirTrackerDatabase) {
            val weaponDao = db.weaponDao()
            val sessionDao = db.sessionDao()

            val w1 = weaponDao.insertWeapon(
                WeaponEntity(
                    name = "CZ 457 Varmint",
                    type = "Carabine",
                    caliber = ".22 LR",
                    sights = "Lunette Vortex 4-12x40",
                    totalRoundsFired = 30,
                    notes = "Excellente précision à 50m avec munitions subsoniques."
                )
            )

            val w2 = weaponDao.insertWeapon(
                WeaponEntity(
                    name = "Glock 17 Gen 5",
                    type = "Pistolet",
                    caliber = "9x19 mm",
                    sights = "Organes d'origine 3-points",
                    totalRoundsFired = 50,
                    notes = "Tir de précision et vitesse à 25m."
                )
            )

            val w3 = weaponDao.insertWeapon(
                WeaponEntity(
                    name = "Feinwerkbau P8X",
                    type = "Air Comprimé",
                    caliber = "4.5 mm (.177)",
                    sights = "Hausse micrométrique",
                    totalRoundsFired = 60,
                    notes = "Arme de match 10m olympique."
                )
            )

            // Seed a sample session with shots for immediate visualization
            val sampleSessionId = sessionDao.insertSession(
                ShootingSessionEntity(
                    title = "Entraînement Précision 50m",
                    dateMillis = System.currentTimeMillis() - 86400000L,
                    targetType = "C50 (25m/50m)",
                    distanceMeters = 50,
                    weaponId = w1,
                    weaponName = "CZ 457 Varmint",
                    ammoBrand = "SK Rifle Match",
                    ammoCaliber = ".22 LR",
                    position = "Couché",
                    totalScore = 96,
                    innerTens = 4,
                    shotsCount = 10,
                    groupingDiameterMm = 24.5f,
                    notes = "Conditions calmes, vent nul. Très beau groupement centré."
                )
            )

            val sampleShots = listOf(
                ShotEntity(sessionId = sampleSessionId, shotIndex = 1, score = 10, isInnerTen = true, xRatio = 0.02f, yRatio = -0.03f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 2, score = 10, isInnerTen = true, xRatio = -0.01f, yRatio = 0.04f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 3, score = 10, isInnerTen = false, xRatio = 0.08f, yRatio = 0.05f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 4, score = 10, isInnerTen = true, xRatio = -0.04f, yRatio = -0.02f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 5, score = 9, isInnerTen = false, xRatio = 0.16f, yRatio = 0.08f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 6, score = 10, isInnerTen = false, xRatio = -0.06f, yRatio = 0.09f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 7, score = 10, isInnerTen = true, xRatio = 0.01f, yRatio = 0.02f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 8, score = 9, isInnerTen = false, xRatio = -0.15f, yRatio = -0.10f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 9, score = 9, isInnerTen = false, xRatio = 0.12f, yRatio = -0.12f),
                ShotEntity(sessionId = sampleSessionId, shotIndex = 10, score = 9, isInnerTen = false, xRatio = -0.10f, yRatio = 0.15f)
            )
            sessionDao.insertShots(sampleShots)
        }
    }
}
