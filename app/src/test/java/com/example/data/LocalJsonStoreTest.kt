package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.Weapon
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import android.net.Uri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalJsonStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun ids(sessions: List<JSONObject>) = sessions.map { it.getLong("id") }

    @Test
    fun `a first launch has no session and adds none by itself`() = runBlocking {
        repeat(2) { assertTrue(LocalJsonStore.loadSessions(context).isEmpty()) }
        assertFalse(File(context.filesDir, "sessions_history.json").exists())
    }

    @Test
    fun `sessions are listed newest first and stay the same across reloads`() = runBlocking {
        val older = JSONObject().put("id", 100L).put("title", "Ancienne")
        val newest = JSONObject().put("id", 200L).put("title", "Nouvelle")
        LocalJsonStore.saveSessions(context, listOf(older, newest))

        // Order must be stable across reloads (it used to flip on every load).
        repeat(2) {
            val reloaded = LocalJsonStore.loadSessions(context)
            assertEquals(listOf("Nouvelle", "Ancienne"), reloaded.map { it.getString("title") })
        }
    }

    @Test
    fun `a cleared history stays empty`() = runBlocking {
        LocalJsonStore.loadSessions(context)
        LocalJsonStore.saveSessions(context, emptyList())
        assertTrue(LocalJsonStore.loadSessions(context).isEmpty())
    }

    @Test
    fun `an unreadable history is kept as a copy instead of being silently lost`() = runBlocking {
        val file = File(context.filesDir, "sessions_history.json")
        val garbage = "{ this is not json"
        file.writeText(garbage)

        assertTrue(LocalJsonStore.loadSessions(context).isEmpty())

        val copies = context.filesDir.listFiles { f -> f.name.startsWith("sessions_history.corrupt-") }.orEmpty()
        assertEquals(1, copies.size)
        assertEquals(garbage, copies.single().readText())
    }

    @Test
    fun `a session photo is copied into private storage and removed with the history`() = runBlocking {
        val source = File(context.cacheDir, "target.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val copy = LocalJsonStore.copyPhoto(context, Uri.fromFile(source), 42L)
        assertNotNull(copy)
        assertTrue(LocalJsonStore.isPrivatePhoto(context, copy!!))
        assertFalse(LocalJsonStore.isPrivatePhoto(context, Uri.fromFile(source)))
        assertArrayEquals(byteArrayOf(1, 2, 3), File(copy.path!!).readBytes())

        LocalJsonStore.deletePhotos(context)
        assertFalse(File(copy.path!!).exists())
    }

    @Test
    fun `a photo that cannot be read is not copied`() = runBlocking {
        val missing = Uri.fromFile(File(context.cacheDir, "missing.jpg"))
        assertNull(LocalJsonStore.copyPhoto(context, missing, 7L))
    }

    fun `weapons round trip`() = runBlocking {
        val defaults = LocalJsonStore.loadWeapons(context)
        assertEquals(5, defaults.size)

        val custom = Weapon(42L, "Anschütz 1907", "Carabine", ".22 LR", "Dioptre")
        LocalJsonStore.saveWeapons(context, defaults + custom)

        assertEquals(defaults + custom, LocalJsonStore.loadWeapons(context))
    }
}
