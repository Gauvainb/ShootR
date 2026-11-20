package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import android.app.Activity
import android.content.Intent
import android.net.Uri
import org.robolectric.Shadows
import androidx.compose.ui.test.performClick
import com.example.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Developer tool, not a regression test: renders the app's screens to PNG files to review the
 * interface without a device.
 *
 *   SCREENSHOTS_OUT=/path/to/dir ./gradlew testDebugUnitTest --tests '*ScreenshotTest*'
 *
 * Skipped when SCREENSHOTS_OUT is not set. SCREENSHOTS_PHOTO adds the target step with a real photo.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /** Captures the topmost window (a dialog or bottom sheet) on top of the activity. */
    private fun saveDialog(name: String) {
        val dir = File(System.getenv("SCREENSHOTS_OUT")!!).apply { mkdirs() }
        rule.waitForIdle()
        val decor = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        decor.draw(canvas)
        // All window roots, through the framework's own (hidden) registry.
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        @Suppress("UNCHECKED_CAST")
        val roots = global.getDeclaredField("mViews").apply { isAccessible = true }.get(instance) as List<android.view.View>
        roots.filter { it !== decor }.forEach { root ->
            val location = IntArray(2)
            root.getLocationOnScreen(location)
            canvas.save()
            canvas.translate(location[0].toFloat(), location[1].toFloat())
            root.draw(canvas)
            canvas.restore()
        }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun save(name: String) {
        val dir = File(System.getenv("SCREENSHOTS_OUT")!!).apply { mkdirs() }
        rule.waitForIdle()
        // Software draw of the whole window (PixelCopy-based capture does not run under Robolectric).
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** From the home screen to step 1 of a new session. */
    private fun openNewSession() {
        rule.onNodeWithTag("home_new_session").performClick()
        rule.waitForIdle()
    }

    /**
     * A C50 target photographed at an angle (squeezed vertically), with black ink, white rings in
     * the visual, grey rings on the paper and a few holes: lighter in the visual, dark on paper.
     */
    private fun writeTargetPhoto(file: File, holesMm: List<Pair<Float, Float>>) {
        val size = 1000
        val squeeze = 0.8f
        val pxPerMm = 2f
        val rings = listOf(12.5f, 25f, 50f, 75f, 100f, 125f, 150f, 175f, 200f, 225f, 250f)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val mm = Math.hypot(((x - size / 2f) / pxPerMm).toDouble(), ((y - size / 2f) / (pxPerMm * squeeze)).toDouble()).toFloat()
                val onRing = rings.any { Math.abs(mm - it) < 0.6f }
                val v = when {
                    mm <= 100f -> if (onRing && mm < 99f) 220 else 25
                    onRing -> 120
                    else -> 230
                }
                pixels[y * size + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        for ((hx, hy) in holesMm) {
            val inVisual = Math.hypot(hx.toDouble(), hy.toDouble()) <= 100.0
            val v = if (inVisual) 205 else 40
            val cx = size / 2f + hx * pxPerMm
            val cy = size / 2f - hy * pxPerMm * squeeze
            val r = 2.8f * pxPerMm
            for (y in (cy - r - 1).toInt()..(cy + r + 1).toInt()) {
                for (x in (cx - r - 1).toInt()..(cx + r + 1).toInt()) {
                    if (Math.hypot((x - cx).toDouble(), ((y - cy) / squeeze).toDouble()) <= r) {
                        pixels[y * size + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
                    }
                }
            }
        }
        val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun loadPhoto(photo: File) {
        rule.onNodeWithTag("load_photo_button").performClick()
        rule.waitForIdle()
        val activity = Shadows.shadowOf(rule.activity)
        val request = activity.nextStartedActivityForResult
        activity.receiveResult(request.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(photo)))
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun home() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.waitForIdle()
        save("0_accueil")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun armoryAndAnalysis() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithTag("nav_armory").performClick()
        save("ratelier_armes")
        rule.onNodeWithText("Munitions (5)").performClick()
        save("ratelier_munitions")
        rule.onNodeWithTag("nav_analysis").performClick()
        save("analyses")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun step1Session() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        openNewSession()
        save("1_arme_seance")
        rule.onNodeWithTag("pick_ammo_button").performClick()
        save("1_choix_munition")
    }

    /**
     * The whole path on a photographed target, shot by shot: SCREENSHOTS_PHOTO if given, otherwise
     * a synthetic photo of a C50 taken at an angle.
     */
    @Test
    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    fun fullSession() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        val photo = System.getenv("SCREENSHOTS_PHOTO")?.let { File(it) }
            ?: File.createTempFile("target", ".png").also {
                writeTargetPhoto(it, listOf(3f to 2f, -6f to 5f, 10f to -4f, -22f to -18f, 40f to 30f, -60f to 10f, 90f to -20f))
            }
        openNewSession()
        rule.onNodeWithTag("step_next_button").performClick()
        loadPhoto(photo)
        rule.waitUntil(60_000) { rule.onAllNodesWithText("Repérage de la cible").fetchSemanticsNodes().isNotEmpty() }
        save("2_reperage")
        rule.onNodeWithText("Appliquer & Calibrer").performClick()
        rule.onNodeWithTag("step_next_button").performClick()
        save("3_calibrage")
        rule.onNodeWithText("Valider le calibrage").performClick()
        rule.waitUntil(90_000) { rule.onAllNodesWithText("Détection…").fetchSemanticsNodes().isEmpty() }
        save("4_impacts")
        rule.onNodeWithTag("shot_chip_1").performClick()
        save("5_correction")
        rule.onNodeWithText("Terminé").performClick()
        rule.onNodeWithTag("step_next_button").performClick()
        save("6_score")
        rule.onNodeWithTag("save_session_button").performClick()
        rule.waitForIdle()
        save("7_apres_enregistrement")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun history() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithTag("nav_history").performClick()
        save("historique")
    }

}
