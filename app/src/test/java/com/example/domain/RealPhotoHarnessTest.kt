package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.min
import javax.imageio.ImageIO

/**
 * Developer tool, not a regression test: runs the recognition and impact detection on real target
 * photos and writes annotated copies, to judge the result by eye.
 *
 *   TARGET_PHOTOS=/path/to/photos TARGET_PHOTOS_OUT=/path/to/out ./gradlew testDebugUnitTest --tests '*RealPhotoHarnessTest*'
 *
 * Skipped when TARGET_PHOTOS is not set.
 */
class RealPhotoHarnessTest {

    @Test
    fun annotatePhotos() {
        val inputDir = System.getenv("TARGET_PHOTOS")?.let(::File)
        assumeTrue("TARGET_PHOTOS not set", inputDir != null && inputDir.isDirectory)
        val outputDir = File(System.getenv("TARGET_PHOTOS_OUT") ?: "${inputDir!!.path}/annotated").apply { mkdirs() }
        val caliberMm = System.getenv("TARGET_CALIBER_MM")?.toFloatOrNull()

        inputDir!!.listFiles { f -> f.extension.lowercase() in setOf("jpg", "jpeg", "png") }!!.sorted().forEach { file ->
            val photo = decodeLikeTheApp(ImageIO.read(file))
            val image = toLuma(photo)
            val started = System.nanoTime()
            val circle = BlackVisualFinder.find(image)
            val spec = TargetSpecs.forTargetType("C50 (25m/50m)")!!
            val impacts = if (circle == null) emptyList() else {
                val mmToPhoto = circle.radius / (spec.blackVisualDiameterMm / 2f) / image.width
                ImpactDetector.detect(
                    image,
                    ImpactDetectionParams(
                        centerNormalized = Offset(circle.centerX / image.width, circle.centerY / image.height),
                        blackRadiusNormalized = circle.radius / image.width,
                        mmToPhoto = mmToPhoto,
                        caliberMm = caliberMm,
                        cardHalfSizeNormalized = spec.cardSizeMm!! * 0.49f * mmToPhoto,
                        digitRadiiMm = spec.digitRadiiMm
                    )
                )
            }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            println("PHOTO ${file.name}: ${image.width}x${image.height} radius=${circle?.radius?.toInt()} impacts=${impacts.size} ${elapsedMs}ms")
            annotate(photo, circle, impacts, File(outputDir, file.nameWithoutExtension + ".jpg"))
        }
        montage(outputDir)
    }

    /** Same power-of-two subsampling as the app's bitmap decoding (both sides stay >= 800 px). */
    private fun decodeLikeTheApp(photo: BufferedImage): BufferedImage {
        var sample = 1
        while (photo.width / 2 / sample >= 800 && photo.height / 2 / sample >= 800) sample *= 2
        if (sample == 1) return photo
        val out = BufferedImage(photo.width / sample, photo.height / sample, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(photo.getScaledInstance(out.width, out.height, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null)
        g.dispose()
        return out
    }

    private fun montage(dir: File) {
        val files = dir.listFiles { f -> f.extension == "jpg" && !f.name.startsWith("montage") }!!.sorted()
        val tileW = 480
        val tileH = 640
        val columns = 4
        val rows = (files.size + columns - 1) / columns
        val sheet = BufferedImage(columns * tileW, rows * tileH, BufferedImage.TYPE_INT_RGB)
        val g = sheet.createGraphics()
        files.forEachIndexed { i, f ->
            val tile = ImageIO.read(f)
            val x = (i % columns) * tileW
            val y = (i / columns) * tileH
            g.drawImage(tile.getScaledInstance(tileW, tileH, java.awt.Image.SCALE_SMOOTH), x, y, null)
            g.color = Color.RED
            g.font = g.font.deriveFont(28f)
            g.drawString(f.nameWithoutExtension, x + 8, y + 30)
        }
        g.dispose()
        ImageIO.write(sheet, "jpg", File(dir, "montage.jpg"))
        // Three half-height sheets are easier to read than one tall one.
        val rowsPerSheet = 2
        for (first in 0 until rows step rowsPerSheet) {
            val h = min(rowsPerSheet, rows - first) * tileH
            ImageIO.write(sheet.getSubimage(0, first * tileH, sheet.width, h), "jpg", File(dir, "montage${first / rowsPerSheet}.jpg"))
        }
    }

    private fun toLuma(photo: BufferedImage): LumaImage {
        val width = photo.width
        val height = photo.height
        val pixels = photo.getRGB(0, 0, width, height, null, 0, width)
        val luma = FloatArray(width * height)
        var min = 255f
        var max = 0f
        for (i in pixels.indices) {
            val c = pixels[i]
            val v = 0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)
            luma[i] = v
            if (v < min) min = v
            if (v > max) max = v
        }
        return LumaImage(width, height, luma, min, max, pixels)
    }

    private fun annotate(photo: BufferedImage, circle: VisualCircle?, impacts: List<Offset>, out: File) {
        val canvas = BufferedImage(photo.width, photo.height, BufferedImage.TYPE_INT_RGB)
        val g = canvas.createGraphics()
        g.drawImage(photo, 0, 0, null)
        g.stroke = BasicStroke(3f)
        if (circle != null) {
            val cx = circle.centerX
            val cy = circle.centerY
            val r = circle.radius
            g.color = Color(0, 255, 255)
            g.drawOval((cx - r).toInt(), (cy - r).toInt(), (2 * r).toInt(), (2 * r).toInt())
            g.drawLine((cx - 30).toInt(), cy.toInt(), (cx + 30).toInt(), cy.toInt())
            g.drawLine(cx.toInt(), (cy - 30).toInt(), cx.toInt(), (cy + 30).toInt())
        }
        g.color = Color(255, 0, 255)
        impacts.forEach {
            val x = it.x * photo.width
            val y = it.y * photo.height
            g.drawOval((x - 11).toInt(), (y - 11).toInt(), 22, 22)
            g.fillOval((x - 3).toInt(), (y - 3).toInt(), 6, 6)
        }
        g.dispose()
        ImageIO.write(canvas, "jpg", out)
    }
}
