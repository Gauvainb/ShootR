package com.example.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The black visual of a target, as found on a photo (pixels of the analyzed image).
 *
 * @property support fraction of the circle's outline that was actually seen on the photo (0..1);
 *   low when the visual is cropped by the frame or hidden behind something
 */
data class VisualCircle(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val support: Float
)

/**
 * Locates the black visual: the large dark disc on light paper.
 *
 * Works on the outline of the disc rather than on its dark area, so that bullet holes, stickers,
 * printed digits inside the visual, or a hand and a pistol in front of it do not move the result:
 * 1. every strong dark-to-light edge votes for the centers it could belong to (Hough transform),
 *    and concentric rings all agree on the same center;
 * 2. the radius is the distance at which most outward-facing edges are found around that center;
 * 3. center and radius are then refined on the full-resolution outline with a least-squares fit.
 */
object BlackVisualFinder {
    private const val WORK_SIZE = 256
    private const val OUTER_CIRCLE_SHARE = 0.4f

    fun find(image: LumaImage): VisualCircle? {
        val factor = max(1, (max(image.width, image.height) + WORK_SIZE - 1) / WORK_SIZE)
        val w = image.width / factor
        val h = image.height / factor
        if (w < 40 || h < 40) return null
        val small = downsample(image, factor, w, h)

        // Sobel gradients; keep only edges that are strong compared to the photo's contrast.
        val contrast = contrastOf(small)
        if (contrast < 30f) return null
        val edgeThreshold = contrast * 0.2f
        val edgeX = ArrayList<Int>()
        val edgeY = ArrayList<Int>()
        val dirX = ArrayList<Float>()
        val dirY = ArrayList<Float>()
        val strength = ArrayList<Float>()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val gx = (small[i - w + 1] + 2f * small[i + 1] + small[i + w + 1] -
                    small[i - w - 1] - 2f * small[i - 1] - small[i + w - 1]) / 4f
                val gy = (small[i + w - 1] + 2f * small[i + w] + small[i + w + 1] -
                    small[i - w - 1] - 2f * small[i - w] - small[i - w + 1]) / 4f
                val magnitude = hypot(gx, gy)
                if (magnitude > edgeThreshold) {
                    edgeX.add(x)
                    edgeY.add(y)
                    dirX.add(gx / magnitude) // points from dark to light
                    dirY.add(gy / magnitude)
                    strength.add(magnitude)
                }
            }
        }
        if (edgeX.size < 30) return null

        // 1. Center: each edge votes along the line going into its dark side.
        val margin = max(w, h) / 4
        val accW = w + 2 * margin
        val accH = h + 2 * margin
        val votes = FloatArray(accW * accH)
        val minRadius = min(w, h) * 0.08f
        val maxRadius = max(w, h) * 0.75f
        for (e in edgeX.indices) {
            var r = minRadius
            while (r <= maxRadius) {
                val cx = (edgeX[e] - dirX[e] * r).roundToInt() + margin
                val cy = (edgeY[e] - dirY[e] * r).roundToInt() + margin
                if (cx in 0 until accW && cy in 0 until accH) votes[cy * accW + cx] += 1f
                r += 1f
            }
        }
        var bestVotes = 0f
        var bestCx = 0
        var bestCy = 0
        for (y in 2 until accH - 2) {
            for (x in 2 until accW - 2) {
                var sum = 0f
                for (dy in -2..2) for (dx in -2..2) sum += votes[(y + dy) * accW + x + dx]
                if (sum > bestVotes) {
                    bestVotes = sum
                    bestCx = x
                    bestCy = y
                }
            }
        }
        val coarseCx = (bestCx - margin).toFloat()
        val coarseCy = (bestCy - margin).toFloat()

        // 2. Radius: where outward-facing (dark inside, light outside) edges pile up.
        val histogram = FloatArray(maxRadius.toInt() + 3)
        for (e in edgeX.indices) {
            val dx = edgeX[e] - coarseCx
            val dy = edgeY[e] - coarseCy
            val d = hypot(dx, dy)
            if (d < minRadius || d > maxRadius) continue
            val alignment = (dx * dirX[e] + dy * dirY[e]) / d
            if (alignment > 0.9f) histogram[d.roundToInt()] += strength[e]
        }
        var bestWeight = 0f
        for (r in 1 until histogram.size - 1) {
            bestWeight = max(bestWeight, histogram[r - 1] + histogram[r] + histogram[r + 1])
        }
        if (bestWeight <= 0f) return null
        // The visual is the outermost strong circle: printed rings and stickers inside it can be
        // complete circles while the visual itself is partly out of the frame on a close-up.
        var coarseRadius = 0
        for (r in histogram.size - 2 downTo 1) {
            if (histogram[r - 1] + histogram[r] + histogram[r + 1] >= bestWeight * OUTER_CIRCLE_SHARE) {
                coarseRadius = r
                break
            }
        }
        if (coarseRadius == 0) return null

        // 3. Refine on the full-resolution outline.
        return refine(
            image,
            (coarseCx + 0.5f) * factor,
            (coarseCy + 0.5f) * factor,
            coarseRadius.toFloat() * factor,
            contrast
        )
    }

    private fun refine(image: LumaImage, cx0: Float, cy0: Float, r0: Float, contrast: Float): VisualCircle? {
        var cx = cx0.toDouble()
        var cy = cy0.toDouble()
        var radius = r0.toDouble()
        val angles = 360
        var support = 0f

        // Two passes: the second one looks closer to the first fit.
        for (pass in 0 until 2) {
            val window = radius * (if (pass == 0) 0.10 else 0.03)
            val px = ArrayList<Double>()
            val py = ArrayList<Double>()
            for (a in 0 until angles) {
                val angle = 2.0 * Math.PI * a / angles
                val ux = cos(angle)
                val uy = sin(angle)
                // Strongest dark-to-light step along the ray, near the expected radius.
                var bestStep = contrast * 0.25
                var bestR = -1.0
                var r = radius - window
                while (r <= radius + window) {
                    val inner = sample(image, cx + ux * (r - 2.0), cy + uy * (r - 2.0))
                    val outer = sample(image, cx + ux * (r + 2.0), cy + uy * (r + 2.0))
                    if (inner >= 0f && outer >= 0f && outer - inner > bestStep) {
                        bestStep = (outer - inner).toDouble()
                        bestR = r
                    }
                    r += 1.0
                }
                if (bestR > 0) {
                    px.add(cx + ux * bestR)
                    py.add(cy + uy * bestR)
                }
            }
            if (px.size < angles / 8) return if (pass == 0) null else VisualCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), support)

            // Least-squares circle, refitted once without the points that are off the first fit
            // (holes or objects on the outline).
            var fit = fitCircle(px, py) ?: return null
            val keptX = ArrayList<Double>()
            val keptY = ArrayList<Double>()
            for (i in px.indices) {
                if (abs(hypot(px[i] - fit[0], py[i] - fit[1]) - fit[2]) < fit[2] * 0.02) {
                    keptX.add(px[i])
                    keptY.add(py[i])
                }
            }
            if (keptX.size >= angles / 8) fit = fitCircle(keptX, keptY) ?: fit
            // Reject fits that wander away from the coarse estimate: they latched onto something else.
            if (hypot(fit[0] - cx0, fit[1] - cy0) > r0 * 0.25 || abs(fit[2] - r0) > r0 * 0.25) {
                return if (pass == 0) null else VisualCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), support)
            }
            cx = fit[0]
            cy = fit[1]
            radius = fit[2]
            support = keptX.size.toFloat() / angles
        }
        return VisualCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), support)
    }

    /** Algebraic least-squares circle fit. Returns [centerX, centerY, radius]. */
    private fun fitCircle(xs: List<Double>, ys: List<Double>): DoubleArray? {
        val n = xs.size
        if (n < 3) return null
        val meanX = xs.sum() / n
        val meanY = ys.sum() / n
        var suu = 0.0
        var svv = 0.0
        var suv = 0.0
        var suuu = 0.0
        var svvv = 0.0
        var suvv = 0.0
        var svuu = 0.0
        for (i in 0 until n) {
            val u = xs[i] - meanX
            val v = ys[i] - meanY
            suu += u * u
            svv += v * v
            suv += u * v
            suuu += u * u * u
            svvv += v * v * v
            suvv += u * v * v
            svuu += v * u * u
        }
        val det = suu * svv - suv * suv
        if (abs(det) < 1e-9) return null
        val bu = (suuu + suvv) / 2.0
        val bv = (svvv + svuu) / 2.0
        val uc = (bu * svv - bv * suv) / det
        val vc = (bv * suu - bu * suv) / det
        val radius = sqrt(uc * uc + vc * vc + (suu + svv) / n)
        return doubleArrayOf(uc + meanX, vc + meanY, radius)
    }

    private fun sample(image: LumaImage, x: Double, y: Double): Float {
        val xi = x.roundToInt()
        val yi = y.roundToInt()
        if (xi !in 0 until image.width || yi !in 0 until image.height) return -1f
        return image.luma[yi * image.width + xi]
    }

    private fun downsample(image: LumaImage, factor: Int, w: Int, h: Int): FloatArray {
        val out = FloatArray(w * h)
        val area = (factor * factor).toFloat()
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                for (dy in 0 until factor) {
                    val row = (y * factor + dy) * image.width + x * factor
                    for (dx in 0 until factor) sum += image.luma[row + dx]
                }
                out[y * w + x] = sum / area
            }
        }
        return out
    }

    /** Spread between the dark and light ends of the photo, ignoring outliers. */
    private fun contrastOf(luma: FloatArray): Float {
        val histogram = IntArray(256)
        for (v in luma) histogram[v.toInt().coerceIn(0, 255)]++
        fun percentile(fraction: Float): Int {
            val rank = (luma.size * fraction).toInt()
            var seen = 0
            for (bin in 0..255) {
                seen += histogram[bin]
                if (seen > rank) return bin
            }
            return 255
        }
        return (percentile(0.95f) - percentile(0.05f)).toFloat()
    }
}
