package com.example.domain

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where the target's circles actually are on the photo, direction by direction.
 *
 * A photo taken at an angle, or of a curved sheet, does not show circles: the rings are closer
 * together on one side than on the other, and more so far from the center. A single scale taken
 * from the black visual then misplaces hits on the outer rings by a whole zone. This model measures
 * the edge of the visual and, when the target's printed rings are known, follows them outwards and
 * inwards along many rays from the center. A point of the photo is then converted to a distance
 * in millimeters from the rings around it.
 *
 * Coordinates are pixels of the analyzed image.
 */
class RingModel internal constructor(
    val centerX: Float,
    val centerY: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    /** Radii in mm of the circles that were located, ascending. */
    private val circlesMm: FloatArray,
    /** [ray][circle]: radius in pixels of each circle along each ray. */
    private val radiiPx: Array<FloatArray>
) {
    private val rayCount = radiiPx.size

    /** Radius in pixels of every circle along the direction [angle] (radians), interpolated. */
    fun circlesAt(angle: Double): FloatArray {
        val pos = ((angle / (2 * PI)) * rayCount).let { ((it % rayCount) + rayCount) % rayCount }
        val i0 = pos.toInt() % rayCount
        val i1 = (i0 + 1) % rayCount
        val t = (pos - pos.toInt()).toFloat()
        return FloatArray(circlesMm.size) { k -> radiiPx[i0][k] * (1 - t) + radiiPx[i1][k] * t }
    }

    /** Distance in mm from the target's center of a point given in pixels of the analyzed image. */
    fun distanceMm(x: Float, y: Float): Float {
        val dx = x - centerX
        val dy = y - centerY
        val r = sqrt(dx * dx + dy * dy)
        val radii = circlesAt(atan2(dy.toDouble(), dx.toDouble()))
        // Piecewise linear between the located circles; the center is at 0.
        var prevPx = 0f
        var prevMm = 0f
        for (k in circlesMm.indices) {
            val px = radii[k]
            if (r <= px) {
                return if (px > prevPx) prevMm + (r - prevPx) / (px - prevPx) * (circlesMm[k] - prevMm) else circlesMm[k]
            }
            prevPx = px
            prevMm = circlesMm[k]
        }
        // Beyond the outermost circle: keep the scale of the last ring.
        val n = circlesMm.size
        val lastScale = if (n >= 2) (radii[n - 1] - radii[n - 2]) / (circlesMm[n - 1] - circlesMm[n - 2]) else radii[n - 1] / circlesMm[n - 1]
        return circlesMm[n - 1] + (r - radii[n - 1]) / max(lastScale, 1e-3f)
    }

    /**
     * Position in mm on the target (x to the right, y up) of a point given normalized to the photo.
     * The direction is the one seen on the photo; the distance follows the rings.
     */
    fun toTargetMm(normalized: Offset): Offset {
        val x = normalized.x * imageWidth
        val y = normalized.y * imageHeight
        val dx = x - centerX
        val dy = y - centerY
        val r = sqrt(dx * dx + dy * dy)
        if (r < 1e-3f) return Offset.Zero
        val mm = distanceMm(x, y)
        return Offset(dx / r * mm, -dy / r * mm)
    }

    /** Pixels per mm along a direction, from the visual's edge (for sizes such as a hole). */
    fun localScale(angle: Double, visualRadiusMm: Float): Float {
        val k = circlesMm.indexOfFirst { abs(it - visualRadiusMm) < 0.01f }
        val radii = circlesAt(angle)
        return if (k >= 0) radii[k] / visualRadiusMm else radii.last() / circlesMm.last()
    }

    companion object {
        private const val RAYS = 180

        /**
         * Measures the circles of a target on a photo.
         *
         * @param centerX center of the target, pixels of [image]
         * @param visualRadiusPx radius of the black visual found on the photo
         * @param visualRadiusMm real radius of the black visual
         * @param printedRingsMm radii of the printed rings to follow, or null to use the visual only
         */
        fun build(
            image: LumaImage,
            centerX: Float,
            centerY: Float,
            visualRadiusPx: Float,
            visualRadiusMm: Float,
            printedRingsMm: List<Float>?
        ): RingModel? {
            if (visualRadiusPx < 8f || visualRadiusMm <= 0f) return null
            val circles = (printedRingsMm.orEmpty() + visualRadiusMm).distinct().sorted().toFloatArray()
            val edgeIndex = circles.indexOfFirst { abs(it - visualRadiusMm) < 0.01f }
            val levels = levels(image, centerX, centerY, visualRadiusPx) ?: return null
            val (ink, paper) = levels
            val contrast = paper - ink
            if (contrast < 25f) return null

            val ux = FloatArray(RAYS) { cos(2 * PI * it / RAYS).toFloat() }
            val uy = FloatArray(RAYS) { sin(2 * PI * it / RAYS).toFloat() }
            val angles = DoubleArray(RAYS) { 2 * PI * it / RAYS }
            val fitted = arrayOfNulls<FloatArray>(circles.size)

            // The visual's edge: the strongest dark-to-light step near the expected radius, ray by
            // ray, then one smooth curve through them.
            val edgeRaw = FloatArray(RAYS) { findEdge(image, centerX, centerY, ux[it], uy[it], visualRadiusPx, contrast) ?: Float.NaN }
            val firstEdge = smoothCurve(angles, edgeRaw) ?: FloatArray(RAYS) { visualRadiusPx }
            // Second, closer look around that first curve, which follows an elliptical visual.
            val edgeRefined = FloatArray(RAYS) { findEdge(image, centerX, centerY, ux[it], uy[it], firstEdge[it], contrast, spread = 0.06f) ?: Float.NaN }
            fitted[edgeIndex] = smoothCurve(angles, edgeRefined) ?: firstEdge

            if (printedRingsMm == null) {
                // Only the visual is known: the other circles scale with its edge, direction by direction.
                val edge = fitted[edgeIndex]!!
                for (k in circles.indices) fitted[k] = FloatArray(RAYS) { edge[it] * circles[k] / visualRadiusMm }
            } else {
                // Outwards (dark lines on the paper) then inwards (light lines in the visual). Each
                // ring is looked for where the rings already found say it should be, which follows
                // a gradual change of scale; it is then smoothed around the target like the edge.
                for (direction in intArrayOf(1, -1)) {
                    var k = edgeIndex + direction
                    while (k in circles.indices) {
                        val prev = fitted[k - direction]!!
                        val prevMm = circles[k - direction]
                        val before = fitted.getOrNull(k - 2 * direction)
                        val beforeMm = circles.getOrNull(k - 2 * direction)
                        val expected = FloatArray(RAYS)
                        val measured = FloatArray(RAYS) { ray ->
                            // Local scale from the last ring step along this ray, or from the visual.
                            val scale = if (before != null && beforeMm != null) {
                                (prev[ray] - before[ray]) / (prevMm - beforeMm)
                            } else {
                                fitted[edgeIndex]!![ray] / visualRadiusMm
                            }
                            val step = (circles[k] - prevMm) * scale
                            expected[ray] = prev[ray] + step
                            val window = abs(step) * 0.3f
                            findLine(
                                image, centerX, centerY, ux[ray], uy[ray],
                                from = expected[ray] - window, to = expected[ray] + window, expected = expected[ray],
                                darkLine = direction > 0,
                                minStrength = if (direction > 0) max(6f, contrast * 0.05f) else max(15f, contrast * 0.15f)
                            ) ?: Float.NaN
                        }
                        fitted[k] = smoothCurve(angles, measured) ?: expected
                        k += direction
                    }
                }
            }

            val smoothed = Array(RAYS) { ray -> FloatArray(circles.size) { k -> fitted[k]!![ray] } }
            // Keep the circles in order along every ray.
            for (radii in smoothed) for (k in 1 until radii.size) if (radii[k] <= radii[k - 1]) radii[k] = radii[k - 1] + 0.5f

            return RingModel(centerX, centerY, image.width, image.height, circles, smoothed)
        }

        private const val HARMONICS = 3

        /**
         * A smooth closed curve r(θ) through the measured radii (NaN where nothing was found):
         * a Fourier series of low order, refitted without the points that stray from it (holes,
         * digits, stickers). Null when too few radii were measured to trust the curve.
         */
        internal fun smoothCurve(angles: DoubleArray, radii: FloatArray): FloatArray? {
            val terms = 1 + 2 * HARMONICS
            fun basis(a: Double) = DoubleArray(terms) { j ->
                when {
                    j == 0 -> 1.0
                    j % 2 == 1 -> cos(((j + 1) / 2) * a)
                    else -> sin((j / 2) * a)
                }
            }
            var use = BooleanArray(radii.size) { !radii[it].isNaN() }
            if (use.count { it } < radii.size / 4) return null
            var coefficients: DoubleArray? = null
            repeat(4) {
                val ata = Array(terms) { DoubleArray(terms) }
                val atb = DoubleArray(terms)
                for (i in radii.indices) {
                    if (!use[i]) continue
                    val b = basis(angles[i])
                    for (p in 0 until terms) {
                        atb[p] += b[p] * radii[i]
                        for (q in 0 until terms) ata[p][q] += b[p] * b[q]
                    }
                }
                coefficients = solve(ata, atb) ?: return null
                val residuals = radii.indices.filter { use[it] }.map { abs(radii[it] - evaluate(coefficients!!, basis(angles[it]))) }.sorted()
                val mad = residuals[residuals.size / 2].coerceAtLeast(0.5)
                val next = BooleanArray(radii.size) { i -> !radii[i].isNaN() && abs(radii[i] - evaluate(coefficients!!, basis(angles[i]))) <= 3.0 * mad }
                if (next.count { it } < radii.size / 4) return@repeat
                use = next
            }
            val c = coefficients ?: return null
            return FloatArray(radii.size) { evaluate(c, basis(angles[it])).toFloat() }
        }

        private fun evaluate(c: DoubleArray, b: DoubleArray): Double {
            var v = 0.0
            for (j in c.indices) v += c[j] * b[j]
            return v
        }

        /** Gaussian elimination with partial pivoting. */
        private fun solve(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = b.size
            val m = Array(n) { i -> DoubleArray(n + 1) { j -> if (j < n) a[i][j] else b[i] } }
            for (col in 0 until n) {
                var pivot = col
                for (r in col + 1 until n) if (abs(m[r][col]) > abs(m[pivot][col])) pivot = r
                if (abs(m[pivot][col]) < 1e-9) return null
                val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
                for (r in 0 until n) {
                    if (r == col) continue
                    val f = m[r][col] / m[col][col]
                    for (c in col..n) m[r][c] -= f * m[col][c]
                }
            }
            return DoubleArray(n) { m[it][n] / m[it][it] }
        }

        /** Median brightness of the ink (inside the visual) and of the paper (around it). */
        private fun levels(image: LumaImage, cx: Float, cy: Float, radius: Float): Pair<Float, Float>? {
            val ink = ArrayList<Float>()
            val paper = ArrayList<Float>()
            for (i in 0 until 720) {
                val a = 2 * PI * i / 720
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                sample(image, cx + c * radius * 0.85f, cy + s * radius * 0.85f)?.let { ink.add(it) }
                sample(image, cx + c * radius * 1.15f, cy + s * radius * 1.15f)?.let { paper.add(it) }
            }
            if (ink.size < 50 || paper.size < 50) return null
            ink.sort()
            paper.sort()
            return Pair(ink[ink.size / 2], paper[paper.size / 2])
        }

        /** The visual's edge along a ray, looked for within [spread] of [radius]. */
        private fun findEdge(image: LumaImage, cx: Float, cy: Float, ux: Float, uy: Float, radius: Float, contrast: Float, spread: Float = 0.15f): Float? {
            var best = contrast * 0.3f
            var bestR: Float? = null
            var r = radius * (1f - spread)
            while (r <= radius * (1f + spread)) {
                val inner = profile(image, cx, cy, ux, uy, r - 2f)
                val outer = profile(image, cx, cy, ux, uy, r + 2f)
                if (inner != null && outer != null && outer - inner > best) {
                    best = outer - inner
                    bestR = r
                }
                r += 0.5f
            }
            return bestR
        }

        /**
         * A thin line across the ray between [from] and [to]: dark on the paper or light in the
         * visual, standing out from both sides. The candidate nearest to [expected] wins.
         */
        private fun findLine(
            image: LumaImage,
            cx: Float,
            cy: Float,
            ux: Float,
            uy: Float,
            from: Float,
            to: Float,
            expected: Float,
            darkLine: Boolean,
            minStrength: Float
        ): Float? {
            val start = max(1f, min(from, to))
            val end = max(from, to)
            val values = ArrayList<Float>()
            val radii = ArrayList<Float>()
            var r = start - 4f
            while (r <= end + 4f) {
                val v = profile(image, cx, cy, ux, uy, r) ?: return null
                values.add(v)
                radii.add(r)
                r += 0.5f
            }
            var bestR: Float? = null
            var bestScore = Float.MAX_VALUE
            for (i in 8 until values.size - 8) {
                if (radii[i] < start || radii[i] > end) continue
                val sides = (values[i - 6] + values[i - 8] + values[i + 6] + values[i + 8]) / 4f
                val strength = if (darkLine) sides - values[i] else values[i] - sides
                if (strength < minStrength) continue
                val isExtremum = (i - 2..i + 2).all { j -> if (darkLine) values[j] >= values[i] else values[j] <= values[i] }
                if (!isExtremum) continue
                val score = abs(radii[i] - expected) / max(strength, 1f)
                if (score < bestScore) {
                    bestScore = score
                    bestR = radii[i]
                }
            }
            return bestR
        }

        /** Brightness across the ray at radius [r], averaged over 3 px perpendicular to it. */
        private fun profile(image: LumaImage, cx: Float, cy: Float, ux: Float, uy: Float, r: Float): Float? {
            val x = cx + ux * r
            val y = cy + uy * r
            var sum = 0f
            var n = 0
            for (d in -1..1) {
                val v = sample(image, x - uy * d, y + ux * d) ?: continue
                sum += v
                n++
            }
            return if (n == 0) null else sum / n
        }

        private fun sample(image: LumaImage, x: Float, y: Float): Float? {
            val xi = x.roundToInt()
            val yi = y.roundToInt()
            if (xi !in 0 until image.width || yi !in 0 until image.height) return null
            return image.luma[yi * image.width + xi]
        }
    }
}
