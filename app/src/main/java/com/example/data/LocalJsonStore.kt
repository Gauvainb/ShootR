package com.example.data

import android.content.Context
import android.net.Uri
import com.example.Ammo
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
 * JSON file persistence for the session history and the weapons and ammunition libraries.
 *
 * All disk access runs on a single background thread: reads never block the UI thread, and writes
 * are applied in the order they were requested. Writes outlive the screen that requested them.
 */
object LocalJsonStore {
    private const val SESSIONS_FILE = "sessions_history.json"
    private const val WEAPONS_FILE = "weapons_library.json"
    private const val AMMO_FILE = "ammo_library.json"
    private const val PHOTOS_DIR = "session_photos"

    private val diskDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val writeScope = CoroutineScope(SupervisorJob() + diskDispatcher)

    /** Saved sessions, newest first. Empty until the user saves one: no sample data is ever added. */
    suspend fun loadSessions(context: Context): List<JSONObject> {
        val filesDir = context.applicationContext.filesDir
        return withContext(diskDispatcher) {
            try {
                val file = File(filesDir, SESSIONS_FILE)
                val sessions = if (file.exists()) {
                    val array = JSONArray(file.readText())
                    List(array.length()) { array.getJSONObject(it) }
                } else {
                    emptyList()
                }
                sessions.sortedByDescending { it.optLong("id", 0L) }
            } catch (_: Exception) {
                // Unreadable file: keep a copy before the next save replaces it with the new history.
                keepUnreadableCopy(File(filesDir, SESSIONS_FILE))
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


    /** Folder of the session photos, inside the app's private storage. */
    private fun photosDir(context: Context): File =
        File(context.applicationContext.filesDir, PHOTOS_DIR)

    /** True when [uri] already points to a photo kept in private storage. */
    fun isPrivatePhoto(context: Context, uri: Uri): Boolean {
        if (uri.scheme != "file") return false
        val path = uri.path ?: return false
        return try {
            File(path).canonicalPath.startsWith(photosDir(context).canonicalPath + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Copies a session photo into private storage and returns its file URI, or null when the source
     * cannot be read. The copy is named after the session, so saving again replaces it.
     */
    suspend fun copyPhoto(context: Context, source: Uri, sessionId: Long): Uri? = withContext(diskDispatcher) {
        try {
            val dir = photosDir(context).apply { mkdirs() }
            val target = File(dir, "session-$sessionId.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            Uri.fromFile(target)
        } catch (_: Exception) {
            null
        }
    }

    /** Removes every session photo, e.g. when the history is cleared. */
    suspend fun deletePhotos(context: Context) = withContext(diskDispatcher) {
        photosDir(context).deleteRecursively()
        Unit
    }
    /** Ammunition library. Seeds a few common loads on first launch. */
    suspend fun loadAmmo(context: Context): List<Ammo> {
        val filesDir = context.applicationContext.filesDir
        return withContext(diskDispatcher) {
            try {
                val file = File(filesDir, AMMO_FILE)
                if (file.exists()) {
                    val array = JSONArray(file.readText())
                    List(array.length()) { i ->
                        val obj = array.getJSONObject(i)
                        Ammo(
                            id = obj.optLong("id", System.currentTimeMillis() + i),
                            name = obj.optString("name", "Munition"),
                            caliber = obj.optString("caliber", ""),
                            notes = obj.optString("notes", "")
                        )
                    }
                } else {
                    defaultAmmo().also { writeAtomically(file, ammoToJson(it)) }
                }
            } catch (_: Exception) {
                keepUnreadableCopy(File(filesDir, AMMO_FILE))
                emptyList()
            }
        }
    }

    fun saveAmmo(context: Context, ammo: List<Ammo>) {
        val filesDir = context.applicationContext.filesDir
        val snapshot = ammo.toList()
        writeScope.launch {
            try {
                writeAtomically(File(filesDir, AMMO_FILE), ammoToJson(snapshot))
            } catch (_: Exception) {
            }
        }
    }

    private fun ammoToJson(ammo: List<Ammo>): String {
        val array = JSONArray()
        ammo.forEach { a ->
            array.put(JSONObject().put("id", a.id).put("name", a.name).put("caliber", a.caliber).put("notes", a.notes))
        }
        return array.toString()
    }

    private fun defaultAmmo(): List<Ammo> = listOf(
        Ammo(1L, "SK Rifle Match 40gr", ".22 LR", "Subsonique"),
        Ammo(2L, "Lapua Center-X", ".22 LR", ""),
        Ammo(3L, "Geco FMJ 124gr", "9x19 mm", ""),
        Ammo(4L, "Sellier & Bellot HPBT 168gr", ".308 Win", ""),
        Ammo(5L, "RWS R10 Match", "4.5 mm", "Plombs")
    )

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
                keepUnreadableCopy(File(filesDir, WEAPONS_FILE))
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

    /**
     * Copies an unreadable store file to `<name>.corrupt-<timestamp>.json` so its content can be
     * recovered by hand. Never throws: this runs while a read is already failing.
     */
    private fun keepUnreadableCopy(file: File) {
        if (!file.exists()) return
        runCatching {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date())
            file.copyTo(File(file.parentFile, "${file.nameWithoutExtension}.corrupt-$stamp.json"))
        }
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
}
