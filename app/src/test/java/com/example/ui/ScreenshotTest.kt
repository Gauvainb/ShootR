package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
 * Skipped when SCREENSHOTS_OUT is not set.
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
    fun phone() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        save("phone")
    }

    @Test
    @Config(qualifiers = "w393dp-h2600dp-xxhdpi")
    fun withImpacts() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        val canvas = rule.onNodeWithTag("target_photo_canvas")
        listOf(0.5f to 0.5f, 0.53f to 0.46f, 0.45f to 0.55f, 0.6f to 0.42f, 0.3f to 0.7f).forEach { (x, y) ->
            canvas.performTouchInput { click(Offset(width * x, height * y)) }
        }
        save("impacts")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun sessionSheet() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithTag("session_characterization_card").performClick()
        rule.waitForIdle()
        saveDialog("sheet")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun history() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        rule.onNodeWithText("Historique").performClick()
        save("history")
    }

    /** Tall window: the whole scrolling content at once. */
    @Test
    @Config(qualifiers = "w393dp-h2600dp-xxhdpi")
    fun fullPage() {
        assumeTrue(System.getenv("SCREENSHOTS_OUT") != null)
        save("full")
    }
}
