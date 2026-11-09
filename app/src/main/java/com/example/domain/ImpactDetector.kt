package com.example.domain

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Where and what to look for on the photo. Distances are fractions of the photo width.
 *
 * @property centerNormalized target center, normalized to the photo ([0..1] on each axis)
 * @property blackRadiusNormalized radius of the black visual; 0 when the target has none
 * @property holeDiameterNormalized expected bullet hole diameter (caliber at the photo scale), or
 *   null when the scale is unknown
 * @property searchRadiusNormalized impacts farther than this from the center are ignored (edge of
 *   the target card), or null to search the whole photo
 */
data class ImpactDetectionParams(
    val centerNormalized: Offset,
    val blackRadiusNormalized: Float,
    val holeDiameterNormalized: Float? = null,
    val searchRadiusNormalized: Float? = null
)

/**
 * Finds bullet holes on a target photo.
 *
 * A hole shows up as a compact spot that contrasts with what surrounds it: darker than the paper
 * outside the black visual, lighter than the ink inside it. Candidate pixels are grouped into
 * connected blobs, then each blob is kept only if its size and shape are those of a hole (or of a
 * few overlapping holes, which are split). Printed rings, digits, shadows and the visual itself are
 * rejected because they are too thin, hollow, or large.
 */
object ImpactDetector {
    private const val NOT_CANDIDATE: Byte = 0
    private const val ON_PAPER: Byte = 1
    private const val IN_VISUAL: Byte = 2

    private const val MIN_CONTRAST = 25f
    private const val MAX_HOLES_PER_BLOB = 6

    fun detect(image: LumaImage, params: ImpactDetectionParams): List<Offset> {
        val width = image.width
        val height = image.height
        if (width < 100 || height < 100) return emptyList()
        val luma = image.luma
        val pixelCount = width * height

        val centerX = params.centerNormalized.x * width
        val centerY = params.centerNormalized.y * height
        val blackRadius = params.blackRadiusNormalized * width
        // Pixels on the edge of the visual are ambiguous (the radius is never exact): skip them.
        val edgeBand = if (blackRadius > 0f) max(1.5f, blackRadius * 0.03f) else 0f
        val innerSq = square(max(0f, blackRadius - edgeBand))
        val outerSq = square(blackRadius + edgeBand)
        val hasVisual = blackRadius > edgeBand
        val searchSq = params.searchRadiusNormalized?.let { square(it * width) } ?: Float.MAX_VALUE

        // 1. Split the photo into "paper" and "black visual" regions and measure their brightness.
        val region = ByteArray(pixelCount)
        val paperHistogram = IntArray(256)
        val visualHistogram = IntArray(256)
        for (y in 0 until height) {
            val dy = y - centerY
            val row = y * width
            for (x in 0 until width) {
                val dx = x - centerX
                val distSq = dx * dx + dy * dy
                val bin = luma[row + x].toInt().coerceIn(0, 255)
                if (hasVisual && distSq < innerSq) {
                    region[row + x] = IN_VISUAL
                    visualHistogram[bin]++
                } else if ((!hasVisual || distSq > outerSq) && distSq < searchSq) {
                    region[row + x] = ON_PAPER
                    paperHistogram[bin]++
                }
            }
        }

        val paperLevel = percentile(paperHistogram, 0.5f) ?: return emptyList()
        val inkLevel = (if (hasVisual) percentile(visualHistogram, 0.5f) else null)
            ?: percentile(paperHistogram, 0.02f)
            ?: return emptyList()
        val contrast = paperLevel - inkLevel
        if (contrast < MIN_CONTRAST) return emptyList()

        val darkThreshold = paperLevel - contrast * 0.5f
        val brightThreshold = inkLevel + contrast * 0.6f

        // 2. Keep only pixels that contrast with their region.
        for (i in 0 until pixelCount) {
            when (region[i]) {
                ON_PAPER -> if (luma[i] >= darkThreshold) region[i] = NOT_CANDIDATE
                IN_VISUAL -> if (luma[i] <= brightThreshold) region[i] = NOT_CANDIDATE
            }
        }

        val scaleKnown = params.holeDiameterNormalized != null
        val holeDiameter = max(3f, (params.holeDiameterNormalized ?: 0.03f) * width)
        val holeArea = (PI * holeDiameter * holeDiameter / 4.0).toFloat()

        // 3. Erase structures much thinner than a hole (printed rings, digits, creases). This also
        //    detaches a hole from a ring line it touches.
        val thinness = when {
            !scaleKnown -> 1
            holeDiameter >= 6f -> max(1, (holeDiameter * 0.12f).toInt())
            else -> 0
        }
        if (thinness > 0) removeThinStructures(region, width, height, thinness)

        // 4. Group candidates into blobs and keep the hole-shaped ones.

        // Every pixel is enqueued at most once over the whole scan, so one buffer serves all blobs.
        val queue = IntArray(pixelCount)
        val impacts = ArrayList<Offset>()

        for (seed in 0 until pixelCount) {
            val kind = region[seed]
            if (kind == NOT_CANDIDATE) continue

            var head = 0
            var tail = 0
            queue[tail++] = seed
            region[seed] = NOT_CANDIDATE
            var sumX = 0.0
            var sumY = 0.0
            var sumXX = 0.0
            var sumYY = 0.0
            var sumXY = 0.0

            while (head < tail) {
                val p = queue[head++]
                val px = p % width
                val py = p / width
                sumX += px
                sumY += py
                sumXX += px.toDouble() * px
                sumYY += py.toDouble() * py
                sumXY += px.toDouble() * py

                val yStart = if (py > 0) py - 1 else 0
                val yEnd = if (py < height - 1) py + 1 else py
                val xStart = if (px > 0) px - 1 else 0
                val xEnd = if (px < width - 1) px + 1 else px
                for (ny in yStart..yEnd) {
                    val nRow = ny * width
                    for (nx in xStart..xEnd) {
                        val n = nRow + nx
                        if (region[n] == kind) {
                            region[n] = NOT_CANDIDATE
                            queue[tail++] = n
                        }
                    }
                }
            }

            val area = tail
            val minArea = holeArea * (if (scaleKnown) 0.25f else 0.16f)
            val maxArea = holeArea * (if (scaleKnown) MAX_HOLES_PER_BLOB + 0.5f else 6.25f)
            if (area < minArea || area > maxArea) continue

            // Shape: equivalent ellipse from second-order moments.
            val meanX = sumX / area
            val meanY = sumY / area
            val varX = sumXX / area - meanX * meanX + 1.0 / 12.0
            val varY = sumYY / area - meanY * meanY + 1.0 / 12.0
            val covXY = sumXY / area - meanX * meanY
            val halfTrace = (varX + varY) / 2.0
            val spread = sqrt(square((varX - varY) / 2.0) + covXY * covXY)
            val sigmaMajor = sqrt(halfTrace + spread)
            val sigmaMinor = sqrt(max(halfTrace - spread, 1.0 / 12.0))
            // 1.0 for a filled disc or ellipse, much lower for outlines (digits, ring fragments).
            val solidity = area / (4.0 * PI * sigmaMajor * sigmaMinor)
            val length = 4.0 * sigmaMajor
            val thickness = 4.0 * sigmaMinor

            if (!scaleKnown) {
                if (solidity >= 0.6) impacts.add(Offset((meanX / width).toFloat(), (meanY / height).toFloat()))
                continue
            }

            if (solidity < 0.65) continue
            if (thickness < 0.5 * holeDiameter) continue

            var holeCount = max(1, (area / holeArea).roundToInt())
            // Two overlapping holes cover less than twice the area of one, but the blob is elongated.
            if (holeCount == 1 && length > 1.5 * thickness && length > 1.6 * holeDiameter) holeCount = 2
            if (holeCount > MAX_HOLES_PER_BLOB) continue
            if (length > (1.3 * holeCount + 0.3) * holeDiameter) continue

            if (holeCount == 1) {
                impacts.add(Offset((meanX / width).toFloat(), (meanY / height).toFloat()))
            } else {
                splitCluster(queue, area, width, height, holeCount, holeDiameter, impacts)
            }
        }
        return impacts
    }

    /**
     * Drops detected points that coincide with an impact that is already placed, so running the
     * detection again never duplicates impacts.
     */
    fun excludeKnown(known: List<Offset>, detected: List<Offset>, minDistance: Float): List<Offset> {
        val minSq = minDistance * minDistance
        return detected.filter { candidate ->
            known.none { (it - candidate).getDistanceSquared() < minSq }
        }
    }

    /** Splits a blob made of overlapping holes into [count] centers with k-means. */
    private fun splitCluster(
        pixels: IntArray,
        pixelCount: Int,
        width: Int,
        height: Int,
        count: Int,
        holeDiameter: Float,
        out: MutableList<Offset>
    ) {
        val cx = DoubleArray(count)
        val cy = DoubleArray(count)

        // Deterministic farthest-point initialization.
        var meanX = 0.0
        var meanY = 0.0
        for (i in 0 until pixelCount) {
            meanX += pixels[i] % width
            meanY += pixels[i] / width
        }
        meanX /= pixelCount
        meanY /= pixelCount
        for (k in 0 until count) {
            var best = -1.0
            for (i in 0 until pixelCount) {
                val x = (pixels[i] % width).toDouble()
                val y = (pixels[i] / width).toDouble()
                var d = if (k == 0) square(x - meanX) + square(y - meanY) else Double.MAX_VALUE
                for (j in 0 until k) d = minOf(d, square(x - cx[j]) + square(y - cy[j]))
                if (d > best) {
                    best = d
                    cx[k] = x
                    cy[k] = y
                }
            }
        }

        val sumX = DoubleArray(count)
        val sumY = DoubleArray(count)
        val members = IntArray(count)
        repeat(12) {
            sumX.fill(0.0)
            sumY.fill(0.0)
            members.fill(0)
            for (i in 0 until pixelCount) {
                val x = (pixels[i] % width).toDouble()
                val y = (pixels[i] / width).toDouble()
                var nearest = 0
                var nearestDist = Double.MAX_VALUE
                for (k in 0 until count) {
                    val d = square(x - cx[k]) + square(y - cy[k])
                    if (d < nearestDist) {
                        nearestDist = d
                        nearest = k
                    }
                }
                sumX[nearest] += x
                sumY[nearest] += y
                members[nearest]++
            }
            for (k in 0 until count) {
                if (members[k] > 0) {
                    cx[k] = sumX[k] / members[k]
                    cy[k] = sumY[k] / members[k]
                }
            }
        }

        // Centers that ended up almost on top of each other are one hole, not two.
        val mergeSq = square(holeDiameter * 0.35)
        val kept = ArrayList<Int>(count)
        for (k in 0 until count) {
            if (members[k] == 0) continue
            if (kept.none { square(cx[it] - cx[k]) + square(cy[it] - cy[k]) < mergeSq }) kept.add(k)
        }
        kept.forEach { out.add(Offset((cx[it] / width).toFloat(), (cy[it] / height).toFloat())) }
    }

    /**
     * Morphological opening of the candidate mask with a square of side `2 * halfSize + 1`:
     * anything that cannot contain that square disappears, larger shapes are left unchanged.
     */
    private fun removeThinStructures(region: ByteArray, width: Int, height: Int, halfSize: Int) {
        val size = width * height
        val mask = BooleanArray(size) { region[it] != NOT_CANDIDATE }
        val scratch = BooleanArray(size)
        val window = 2 * halfSize + 1
        // Erosion keeps a pixel when the whole window is set; dilation when any pixel of it is.
        boxPass(mask, scratch, width, height, halfSize, horizontal = true, required = window)
        boxPass(scratch, mask, width, height, halfSize, horizontal = false, required = window)
        boxPass(mask, scratch, width, height, halfSize, horizontal = true, required = 1)
        boxPass(scratch, mask, width, height, halfSize, horizontal = false, required = 1)
        for (i in 0 until size) if (!mask[i]) region[i] = NOT_CANDIDATE
    }

    private fun boxPass(
        src: BooleanArray,
        dst: BooleanArray,
        width: Int,
        height: Int,
        halfSize: Int,
        horizontal: Boolean,
        required: Int
    ) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val stride = if (horizontal) 1 else width
        for (line in 0 until lines) {
            val start = if (horizontal) line * width else line
            var count = 0
            for (i in 0 until minOf(halfSize, length)) if (src[start + i * stride]) count++
            for (i in 0 until length) {
                val entering = i + halfSize
                if (entering < length && src[start + entering * stride]) count++
                dst[start + i * stride] = count >= required
                val leaving = i - halfSize
                if (leaving >= 0 && src[start + leaving * stride]) count--
            }
        }
    }

    private fun percentile(histogram: IntArray, fraction: Float): Float? {
        val total = histogram.sum()
        if (total == 0) return null
        val rank = (total * fraction).toInt()
        var seen = 0
        for (bin in histogram.indices) {
            seen += histogram[bin]
            if (seen > rank) return bin.toFloat()
        }
        return 255f
    }

    private fun square(v: Float) = v * v
    private fun square(v: Double) = v * v
}
