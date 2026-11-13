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
            val rejected = ArrayList<Triple<Float, Float, String>>()
            val circle = BlackVisualFinder.find(image)
            val spec = TargetSpecs.forTargetType("C50 (25m/50m)")!!
            val impacts = if (circle == null) emptyList() else {
                val mmToPhoto = circle.radius / (spec.blackVisualDiameterMm / 2f) / image.width
                ImpactDetector.detect(
                    image = image,
                    trace = { x, y, verdict ->
                        if (x < 0f) println("INFO ${file.name}: $verdict") else if (verdict != "ok") rejected.add(Triple(x, y, verdict))
                    },
                    params = ImpactDetectionParams(
                        centerNormalized = Offset(circle.centerX / image.width, circle.centerY / image.height),
                        blackRadiusNormalized = circle.radius / image.width,
                        mmToPhoto = mmToPhoto,
                        caliberMm = caliberMm,
                        cardHalfSizeNormalized = spec.cardSizeMm!! * 0.49f * mmToPhoto,
                        digitRadiiMm = spec.digitRadiiMm
                    )
                )
            }
            val rings = circle?.let {
                RingModel.build(image, it.centerX, it.centerY, it.radius, spec.blackVisualDiameterMm / 2f, TargetScoring.printedRingRadiiMm("C50 (25m/50m)"))
            }
            val table = TargetScoring.tableFor("C50 (25m/50m)")
            val scores = impacts.map { p ->
                val x = p.x * image.width
                val y = p.y * image.height
                val byRings = rings?.let { table.score(it.distanceMm(x, y)).first }
                val byCircle = circle?.let {
                    table.score(kotlin.math.hypot(x - it.centerX, y - it.centerY) / (it.radius / (spec.blackVisualDiameterMm / 2f))).first
                }
                Pair(byRings, byCircle)
            }
            val changed = scores.count { it.first != it.second }
            println("SCORES ${file.name}: ${scores.size} impacts, $changed scored differently with the rings")
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            println("PHOTO ${file.name}: ${image.width}x${image.height} radius=${circle?.radius?.toInt()} impacts=${impacts.size} ${elapsedMs}ms")
            annotate(photo, circle, impacts, rejected, File(outputDir, file.nameWithoutExtension + ".jpg"), rings, scores)
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

    private fun annotate(
        photo: BufferedImage,
        circle: VisualCircle?,
        impacts: List<Offset>,
        rejected: List<Triple<Float, Float, String>>,
        out: File,
        rings: RingModel? = null,
        scores: List<Pair<Int?, Int?>> = emptyList()
    ) {
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
        // Rings as located on the photo, ray by ray (green), with the score of each impact:
        // "a/b" when the rings (a) and a single circle (b) disagree.
        if (rings != null) {
            g.color = Color(0, 255, 120)
            g.stroke = BasicStroke(2f)
            val n = 360
            val pts = (0..n).map { i ->
                val a = 2 * Math.PI * i / n
                Pair(a, rings.circlesAt(a))
            }
            val count = pts[0].second.size
            for (k in 0 until count) {
                for (i in 0 until n) {
                    val (a0, r0) = pts[i]
                    val (a1, r1) = pts[i + 1]
                    g.drawLine(
                        (rings.centerX + Math.cos(a0) * r0[k]).toInt(), (rings.centerY + Math.sin(a0) * r0[k]).toInt(),
                        (rings.centerX + Math.cos(a1) * r1[k]).toInt(), (rings.centerY + Math.sin(a1) * r1[k]).toInt()
                    )
                }
            }
            g.stroke = BasicStroke(3f)
        }
        g.font = g.font.deriveFont(java.awt.Font.BOLD, 20f)
        impacts.forEachIndexed { i, p ->
            val (a, b) = scores.getOrNull(i) ?: return@forEachIndexed
            val label = if (a == b) "$a" else "$a/$b"
            g.color = if (a == b) Color.WHITE else Color.YELLOW
            g.drawString(label, p.x * photo.width + 14, p.y * photo.height - 10)
        }
        g.stroke = BasicStroke(3f)
        // Blobs that were considered and dropped, with the reason.
        g.color = Color(255, 140, 0)
        g.font = g.font.deriveFont(15f)
        rejected.forEach { (x, y, verdict) ->
            g.drawRect((x - 6).toInt(), (y - 6).toInt(), 12, 12)
            g.drawString(verdict, x + 9, y + 5)
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
