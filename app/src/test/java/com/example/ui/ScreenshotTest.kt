package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import android.app.Activity
import android.content.Intent
import android.net.Uri
import org.robolectric.Shadows
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun step1Session() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        save("1_seance")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun step2TargetWithoutPhoto() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithTag("next_to_target_button").performClick()
        save("2_cible_sans_photo")
    }

    /** Needs SCREENSHOTS_PHOTO: a real target photo, loaded through the photo picker. */
    @Test
    @Config(qualifiers = "w393dp-h1700dp-xxhdpi")
    fun step2TargetWithPhoto() {
        val photo = System.getenv("SCREENSHOTS_PHOTO")
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null && photo != null)
        rule.onNodeWithTag("next_to_target_button").performClick()
        rule.onNodeWithTag("load_photo_button").performClick()
        rule.waitForIdle()
        val activity = Shadows.shadowOf(rule.activity)
        val request = activity.nextStartedActivityForResult
        activity.receiveResult(request.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(File(photo!!))))
        rule.waitUntil(30_000) {
            rule.onAllNodesWithText("Cible repérée").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Cible non repérée").fetchSemanticsNodes().isNotEmpty()
        }
        save("2_cible_photo")

        rule.onNodeWithTag("next_to_impacts_button").performClick()
        rule.waitUntil(60_000) { rule.onAllNodesWithText("Détection…").fetchSemanticsNodes().isEmpty() }
        save("3_impacts_photo")
    }

    @Test
    @Config(qualifiers = "w393dp-h1600dp-xxhdpi")
    fun step3Impacts() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithTag("next_to_target_button").performClick()
        rule.onNodeWithTag("next_to_impacts_button").performClick()
        val canvas = rule.onNodeWithTag("target_photo_canvas")
        // 14 shots spread over the target, enough to fill several rows of the shot table.
        (0 until 14).map { i -> Pair(0.2f + 0.6f * ((i * 37) % 14) / 14f, 0.2f + 0.6f * ((i * 53) % 14) / 14f) }.forEach { (x, y) ->
            canvas.performTouchInput { click(Offset(width * x, height * y)) }
        }
        save("3_impacts")
        rule.onNodeWithTag("shot_chip_2").performClick()
        save("3_coup")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun history() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithText("Historique").performClick()
        save("historique")
    }

}
