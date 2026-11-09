package com.example.data

import android.content.Context
import com.example.Weapon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * JSON file persistence for the session history and the weapons library.
 *
 * All disk access runs on a single background thread: reads never block the UI thread, and writes
 * are applied in the order they were requested. Writes outlive the screen that requested them.
 */
object LocalJsonStore {
    private const val SESSIONS_FILE = "sessions_history.json"
    private const val WEAPONS_FILE = "weapons_library.json"

    private val diskDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val writeScope = CoroutineScope(SupervisorJob() + diskDispatcher)

    /** Saved sessions, newest first. Seeds sample sessions on first launch. */
    suspend fun loadSessions(context: Context): List<JSONObject> {
        val filesDir = context.applicationContext.filesDir
        return withContext(diskDispatcher) {
            try {
                val file = File(filesDir, SESSIONS_FILE)
                val sessions = if (file.exists()) {
                    val array = JSONArray(file.readText())
                    List(array.length()) { array.getJSONObject(it) }
                } else {
                    sampleSessions().also { writeAtomically(file, toJson(it)) }
                }
                sessions.sortedByDescending { it.optLong("id", 0L) }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun saveSessions(context: Context, sessions: List<JSONObject>) {
        val filesDir = context.applicationContext.filesDir
        val snapshot = sessions.toList()
        writeScope.launch {
            try {
                writeAtomically(File(filesDir, SESSIONS_FILE), toJson(snapshot))
            } catch (_: Exception) {
            }
        }
    }

    /** Weapons library. Seeds default weapons on first launch. */
    suspend fun loadWeapons(context: Context): List<Weapon> {
        val filesDir = context.applicationContext.filesDir
        return withContext(diskDispatcher) {
            try {
                val file = File(filesDir, WEAPONS_FILE)
                if (file.exists()) {
                    val array = JSONArray(file.readText())
                    List(array.length()) { i ->
                        val obj = array.getJSONObject(i)
                        Weapon(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            name = obj.optString("name", "Arme"),
                            type = obj.optString("type", "Pistolet"),
                            caliber = obj.optString("caliber", "9x19 mm"),
                            optics = obj.optString("optics", ""),
                            notes = obj.optString("notes", "")
                        )
                    }
                } else {
                    defaultWeapons().also { writeAtomically(file, weaponsToJson(it)) }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun saveWeapons(context: Context, weapons: List<Weapon>) {
        val filesDir = context.applicationContext.filesDir
        val snapshot = weapons.toList()
        writeScope.launch {
            try {
                writeAtomically(File(filesDir, WEAPONS_FILE), weaponsToJson(snapshot))
            } catch (_: Exception) {
            }
        }
    }

    private fun toJson(sessions: List<JSONObject>): String {
        val array = JSONArray()
        sessions.forEach { array.put(it) }
        return array.toString()
    }

    private fun weaponsToJson(weapons: List<Weapon>): String {
        val array = JSONArray()
        weapons.forEach { w ->
            array.put(
                JSONObject()
                    .put("id", w.id)
                    .put("name", w.name)
                    .put("type", w.type)
                    .put("caliber", w.caliber)
                    .put("optics", w.optics)
                    .put("notes", w.notes)
            )
        }
        return array.toString()
    }

    /** Writes to a temp file then renames it, so a crash mid-write cannot corrupt the store. */
    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(target)) {
            target.writeText(content)
            tmp.delete()
        }
    }

    private fun defaultWeapons(): List<Weapon> = listOf(
        Weapon(1L, "CZ 457 Varmint", "Carabine", ".22 LR", "Lunette Vortex 4-12x40"),
        Weapon(2L, "Glock 17 Gen 5", "Pistolet", "9x19 mm", "Visée d'origine 3 points"),
        Weapon(3L, "AR-15 DDM4 V7", "Fusil d'assaut", "5.56x45 mm / .223 Rem", "Point rouge EOTech EXPS2"),
        Weapon(4L, "Tikka T3x TAC A1", "Fusil de précision", ".308 Win", "Lunette Schmidt & Bender 5-25x56"),
        Weapon(5L, "Smith & Wesson 686", "Revolver", ".357 Magnum", "Hausse réglable")
    )

    private fun sampleSessions(): List<JSONObject> {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
        val samples = listOf(
            Triple(now - 86400000L * 4, 1.45f, 21.1f),
            Triple(now - 86400000L * 3, 1.20f, 17.5f),
            Triple(now - 86400000L * 2, 0.95f, 13.8f),
            Triple(now - 86400000L * 1, 0.82f, 11.9f)
        )
        val offsets = listOf(
            Pair(5.0, 4.0),
            Pair(-4.0, 6.0),
            Pair(2.0, -3.0),
            Pair(8.0, 10.0),
            Pair(-6.0, -5.0)
        )
        return samples.map { (time, moaVal, esVal) ->
            val obj = JSONObject()
            obj.put("id", time)
            obj.put("title", "Entraînement Précision 50m")
            obj.put("date", dateFormat.format(Date(time)))
            obj.put("weapon", "CZ 457 Varmint")
            obj.put("caliber", ".22 LR")
            obj.put("targetType", "C50 (25m/50m)")
            obj.put("distanceMeters", 50.0)
            obj.put("ammo", "SK Rifle Match 40gr")
            obj.put("notes", "Bon groupement serré, météo calme.")
            obj.put("shotsCount", 5)
            obj.put("totalScore", 48)
            obj.put("innerTens", 2)
            obj.put("esMm", esVal.toDouble())
            obj.put("hMm", (esVal * 0.8).toDouble())
            obj.put("lMm", (esVal * 0.7).toDouble())
            obj.put("hPlusL", (esVal * 1.5).toDouble())
            obj.put("moa", moaVal.toDouble())
            obj.put("mpiDeltaXMm", 1.2)
            obj.put("mpiDeltaYMm", -0.8)

            val imps = JSONArray()
            offsets.forEachIndexed { i, (x, y) ->
                val impObj = JSONObject()
                impObj.put("index", i + 1)
                impObj.put("xMm", x)
                impObj.put("yMm", y)
                impObj.put("score", if (i < 3) 10 else 9)
                impObj.put("isInnerTen", i == 0 || i == 2)
                imps.put(impObj)
            }
            obj.put("impacts", imps)
            obj
        }
    }
}
