package com.example.domain

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Where and what to look for on the photo. Distances are fractions of the photo width.
 *
 * @property centerNormalized target center, normalized to the photo ([0..1] on each axis)
 * @property blackRadiusNormalized radius of the black visual; 0 when the target has none
 * @property mmToPhoto size of one millimeter on the photo, or null when the scale is unknown
 * @property caliberMm bullet diameter, or null when unknown
 * @property cardHalfSizeNormalized half the side of the (square, upright) target card: impacts
 *   outside of it are ignored; null to search the whole photo
 * @property digitRadiiMm distances from the center at which ring numbers are printed along the
 *   horizontal and vertical axes; lets the detector tell a printed digit from a hole
 */
data class ImpactDetectionParams(
    val centerNormalized: Offset,
    val blackRadiusNormalized: Float,
    val mmToPhoto: Float? = null,
    val caliberMm: Float? = null,
    val cardHalfSizeNormalized: Float? = null,
    val digitRadiiMm: List<Float> = emptyList()
)

/**
 * Finds bullet holes on a target photo.
 *
 * A hole is a compact mark that differs from what surrounds it: lighter than the ink inside the
 * black visual, or off the paper's color outside of it. On reactive ("splatter") targets the mark
 * is the bright yellow halo around the hole. Marks are grouped into blobs; the typical size of a
 * single hole is measured on the photo itself (from the isolated, round blobs), and each blob is
 * then counted as one hole, split into several overlapping holes, or rejected: printed rings and
 * digits, stickers, shadows, hands and objects are too thin, too hollow, too round or too large.
 */
object ImpactDetector {
    private const val MAX_HOLES_PER_BLOB = 8
    private const val PAPER_TINT_TOLERANCE = 28f

    // Marks left by the smallest and largest usual calibers, torn rim included.
    private const val MIN_HOLE_MM = 3.5f
    private const val MAX_HOLE_MM = 15f

    private class Blob(
        val start: Int,
        val area: Int,
        val x: Float,
        val y: Float,
        val solidity: Float,
        val elongation: Float,
        val length: Float,
        val thickness: Float,
        val inVisual: Boolean,
        val inkShare: Float,
        val darkShare: Float,
        val whiteShare: Float,
        val minCenterDistance: Float
    )

    private class Blobs(val list: List<Blob>, val pixels: IntArray)

    fun detect(image: LumaImage, params: ImpactDetectionParams): List<Offset> = detect(image, params, null)

    /**
     * Same as [detect]; [trace] is told about every blob that was considered and why it was
     * dropped (developer tooling).
     */
    internal fun detect(
        image: LumaImage,
        params: ImpactDetectionParams,
        trace: ((x: Float, y: Float, verdict: String) -> Unit)?
    ): List<Offset> {
        val width = image.width
        val height = image.height
        if (width < 100 || height < 100) return emptyList()
        val n = width * height
        val luma = image.luma
        val argb = image.argb

        val centerX = params.centerNormalized.x * width
        val centerY = params.centerNormalized.y * height
        val visualRadius = params.blackRadiusNormalized * width
        val hasVisual = visualRadius > 4f
        // Without a known scale, assume the usual 200 mm visual, or a photo about 300 mm wide.
        val pxPerMm = params.mmToPhoto?.let { it * width }
            ?: if (hasVisual) visualRadius / 100f else width / 300f
        val cardHalfSize = params.cardHalfSizeNormalized?.let { it * width } ?: Float.MAX_VALUE

        // 1. Brightness of the ink and of the paper.
        val inVisual = BooleanArray(n)
        val inkHistogram = IntArray(256)
        val paperHistogram = IntArray(256)
        // Tint of the paper (red minus green, green minus blue), to tell it from skin or objects
        // that are just as bright.
        val warmHistogram = IntArray(511)
        val coolHistogram = IntArray(511)
        val visualSq = visualRadius * visualRadius
        for (y in 0 until height) {
            val dy = y - centerY
            val row = y * width
            for (x in 0 until width) {
                val dx = x - centerX
                val distSq = dx * dx + dy * dy
                val bin = luma[row + x].toInt().coerceIn(0, 255)
                if (hasVisual && distSq < visualSq) {
                    inVisual[row + x] = true
                    if (distSq < visualSq * 0.94f) inkHistogram[bin]++
                } else if (!hasVisual || (distSq > visualSq * 1.1f && distSq < visualSq * 4.84f)) {
                    paperHistogram[bin]++
                    val i = row + x
                    warmHistogram[(red(argb, luma, i) - green(argb, luma, i)).toInt() + 255]++
                    coolHistogram[(green(argb, luma, i) - blue(argb, luma, i)).toInt() + 255]++
                }
            }
        }
        val paperLevel = percentile(paperHistogram, 0.5f) ?: return emptyList()
        val inkLevel = (if (hasVisual) percentile(inkHistogram, 0.5f) else null)
            ?: percentile(paperHistogram, 0.02f)
            ?: return emptyList()
        val contrast = paperLevel - inkLevel
        if (contrast < 25f) return emptyList()

        // 2. Local color of the paper, on a coarse grid, so that shadows and uneven lighting do
        //    not count as marks.
        val cell = max(2, (8f * pxPerMm).roundToInt())
        val gridW = (width + cell - 1) / cell
        val gridH = (height + cell - 1) / cell
        val cellR = FloatArray(gridW * gridH)
        val cellG = FloatArray(gridW * gridH)
        val cellB = FloatArray(gridW * gridH)
        val cellPaper = FloatArray(gridW * gridH)
        val cellTotal = FloatArray(gridW * gridH)
        val paperFloor = paperLevel - contrast * 0.25f
        val paperWarm = (percentile(warmHistogram, 0.5f) ?: 255f) - 255f
        val paperCool = (percentile(coolHistogram, 0.5f) ?: 255f) - 255f
        for (y in 0 until height) {
            val gy = y / cell
            for (x in 0 until width) {
                val i = y * width + x
                val g = gy * gridW + x / cell
                cellTotal[g] += 1f
                if (!inVisual[i] && luma[i] > paperFloor) {
                    val r = red(argb, luma, i)
                    val gr = green(argb, luma, i)
                    val b = blue(argb, luma, i)
                    if (abs(r - gr - paperWarm) < PAPER_TINT_TOLERANCE && abs(gr - b - paperCool) < PAPER_TINT_TOLERANCE) {
                        cellPaper[g] += 1f
                        cellR[g] += r
                        cellG[g] += gr
                        cellB[g] += b
                    }
                }
            }
        }
        val localR = FloatArray(gridW * gridH)
        val localG = FloatArray(gridW * gridH)
        val localB = FloatArray(gridW * gridH)
        val paperShare = FloatArray(gridW * gridH)
        for (gy in 0 until gridH) {
            for (gx in 0 until gridW) {
                var r = 0f
                var g = 0f
                var b = 0f
                var count = 0f
                var total = 0f
                for (ny in max(0, gy - 2)..min(gridH - 1, gy + 2)) {
                    for (nx in max(0, gx - 2)..min(gridW - 1, gx + 2)) {
                        val c = ny * gridW + nx
                        r += cellR[c]
                        g += cellG[c]
                        b += cellB[c]
                        count += cellPaper[c]
                        total += cellTotal[c]
                    }
                }
                val c = gy * gridW + gx
                paperShare[c] = if (total > 0f) count / total else 0f
                if (count > 0f) {
                    localR[c] = r / count
                    localG[c] = g / count
                    localB[c] = b / count
                }
            }
        }

        // 3. Candidate pixels.
        val brightInVisual = inkLevel + contrast * 0.18f
        val paperDeviation = max(12f, contrast * 0.065f)
        val inkLike = inkLevel + contrast * 0.35f
        val whiteLike = paperLevel - contrast * 0.15f
        val candidate = BooleanArray(n)
        val yellow = BooleanArray(n)
        var yellowInVisual = 0
        var visualPixels = 0
        for (y in 0 until height) {
            val gy = y / cell
            for (x in 0 until width) {
                val i = y * width + x
                val r = red(argb, luma, i)
                val g = green(argb, luma, i)
                val b = blue(argb, luma, i)
                // Vivid yellow-green: the halo of a reactive target.
                val isYellow = g > 150f && r > 140f && b < 0.45f * min(r, g)
                yellow[i] = isYellow
                if (inVisual[i]) {
                    visualPixels++
                    if (isYellow) yellowInVisual++
                    // The red center of reactive targets is not a mark.
                    val pureRed = r > 150f && g < 80f && b < 120f
                    candidate[i] = luma[i] > brightInVisual && !pureRed
                } else {
                    val c = gy * gridW + x / cell
                    candidate[i] = if (paperShare[c] <= 0.15f) {
                        true // not paper at all (hand, object, background): one solid region
                    } else {
                        val dr = r - localR[c]
                        val dg = g - localG[c]
                        val db = b - localB[c]
                        dr * dr + dg * dg + db * db > paperDeviation * paperDeviation
                    }
                }
            }
        }

        // Objects in front of the target (gun, hand): large regions that are neither ink nor
        // paper. Marks on them or right next to them are reflections and edges, not holes. On a
        // reactive target the yellow halos are left out: merged halos can be large too.
        val reactiveLook = hasVisual && visualPixels > 0 && yellowInVisual > visualPixels * 0.004f
        val yellowZone = if (reactiveLook) dilate(yellow, width, height, max(1, (5f * pxPerMm).roundToInt())) else null
        val notInkNorPaper = BooleanArray(n) {
            if (yellowZone != null && yellowZone[it]) false
            else if (inVisual[it]) luma[it] > inkLevel + contrast * 0.1f else candidate[it]
        }
        val objects = dilate(
            largeRegions(
                notInkNorPaper, width, height, max(1, (0.75f * pxPerMm).roundToInt()),
                minArea = areaOf(60f * pxPerMm).toInt()
            ),
            width, height, max(1, (6f * pxPerMm).roundToInt())
        )

        val impacts = ArrayList<Offset>()
        // A hole must stand out from its immediate surroundings (see surroundContrast).
        // On the paper the threshold is high: the edge of the card, transparent pasters and shadows
        // differ only a little from their surroundings. In the visual, printed rings and digits are
        // already handled by the opening and the digit test, and a hole may be dim (dark backing).
        val minContrastOnPaper = max(25f, contrast * 0.17f)
        val minContrastInVisual = 15f
        fun standsOut(blob: Blob, pixels: IntArray): Boolean {
            val c = surroundContrast(blob, pixels, width, height, luma, argb)
            return c >= if (blob.inVisual) minContrastInVisual else minContrastOnPaper
        }
        fun accept(blob: Blob, pixels: IntArray, singleArea: Float) {
            if (abs(blob.x - centerX) > cardHalfSize || abs(blob.y - centerY) > cardHalfSize) return
            if (objects[blob.y.roundToInt().coerceIn(0, height - 1) * width + blob.x.roundToInt().coerceIn(0, width - 1)]) {
                trace?.invoke(blob.x, blob.y, "object")
                return
            }
            if (!standsOut(blob, pixels)) {
                trace?.invoke(blob.x, blob.y, "flat")
                return
            }
            val verdict = classify(blob, pixels, singleArea, pxPerMm, width, height, impacts)
            trace?.invoke(blob.x, blob.y, verdict)
        }

        // 4. Reactive target: each impact is a yellow halo, much larger than the hole itself.
        val reactive = hasVisual && visualPixels > 0 && yellowInVisual > visualPixels * 0.004f
        var bare = candidate
        var measuredHaloArea: Float? = null
        if (reactive) {
            val reach = max(1, (5f * pxPerMm).roundToInt())
            val nearYellow = dilate(yellow, width, height, reach)
            val halo = BooleanArray(n) { yellow[it] || (nearYellow[it] && inVisual[it] && candidate[it]) }
            val haloBlobs = findBlobs(
                halo, width, height, max(1, (1.5f * pxPerMm).roundToInt()), luma, inVisual,
                inkLike, paperFloor, whiteLike, centerX, centerY, minArea = (PI * square(2.5f * pxPerMm)).toInt()
            )
            val haloArea = typicalArea(haloBlobs.list.filter { standsOut(it, haloBlobs.pixels) }, areaOf(5f * pxPerMm), areaOf(30f * pxPerMm))
            if (haloArea != null) haloBlobs.list.forEach { accept(it, haloBlobs.pixels, haloArea) }
            measuredHaloArea = haloArea
            bare = BooleanArray(n) { candidate[it] && !nearYellow[it] }
        }

        // 5. Plain holes. A first pass with a small opening measures the size of a hole; the
        //    second pass uses an opening sized on the holes, which erases digits and thick lines.
        val minDiameterMm = params.caliberMm?.let { it * 0.6f } ?: 3.5f
        val maxDiameterMm = params.caliberMm?.let { it * 1.6f } ?: 14f
        val smallest = areaOf(minDiameterMm * pxPerMm)
        val firstPass = findBlobs(
            bare, width, height, max(1, (0.75f * pxPerMm).roundToInt()), luma, inVisual,
            inkLike, paperFloor, whiteLike, centerX, centerY, minArea = (smallest * 0.3f).toInt()
        )
        // Only marks that stand out from their surroundings tell the size of a hole.
        val measurable = firstPass.list.filter { standsOut(it, firstPass.pixels) }
        val inside = measurable.filter { it.inVisual }
        val outside = measurable.filter { !it.inVisual }
        // On a reactive target the plain marks left inside the visual are mostly halo leftovers:
        // measure the holes on the paper instead.
        var areaInVisual = if (reactive) null else typicalArea(inside, smallest, areaOf(maxDiameterMm * pxPerMm), minCount = 3)
        // On paper the torn rim and its shadow are part of the mark, so it is somewhat larger.
        var areaOnPaper = if (areaInVisual != null) {
            typicalArea(outside, areaInVisual * 0.8f, areaInVisual * 2.6f) ?: areaInVisual * 1.5f
        } else {
            typicalArea(outside, smallest, areaOf(maxDiameterMm * 1.5f * pxPerMm), minCount = 3)
        }
        if (areaInVisual == null && areaOnPaper == null) {
            // Too few isolated holes to measure: fall back on any round blob, then on the caliber.
            areaInVisual = typicalArea(inside, smallest, areaOf(maxDiameterMm * pxPerMm))
            areaOnPaper = typicalArea(outside, smallest, areaOf(maxDiameterMm * 1.5f * pxPerMm))
            if (areaInVisual == null && areaOnPaper == null) {
                // A halo is roughly twice as wide as the hole it surrounds.
                areaInVisual = params.caliberMm?.let { areaOf(it * 0.9f * pxPerMm) } ?: measuredHaloArea?.div(4.5f)
            }
        }
        val holeAreaInVisual = areaInVisual ?: areaOnPaper?.div(1.5f) ?: return impacts
        val holeAreaOnPaper = areaOnPaper ?: holeAreaInVisual * 1.5f

        val holeDiameter = 2f * sqrt(holeAreaInVisual / PI.toFloat())
        trace?.invoke(
            -1f, -1f,
            "px/mm=%.2f hole in visual=%.1fmm on paper=%.1fmm reactive=%b".format(
                pxPerMm, holeDiameter / pxPerMm, 2f * sqrt(holeAreaOnPaper / PI.toFloat()) / pxPerMm, reactive
            )
        )
        val secondPass = findBlobs(
            bare, width, height, max(1, (holeDiameter * 0.225f).roundToInt()), luma, inVisual,
            inkLike, paperFloor, whiteLike, centerX, centerY, minArea = (holeAreaInVisual * 0.15f).toInt()
        )
        // The opening of the second pass trims the outline of every mark a little: measure a
        // single hole again on this pass, so that a cluster is compared with a hole of the same pass.
        val secondMeasurable = secondPass.list.filter { standsOut(it, secondPass.pixels) }
        val singleInVisual = typicalArea(
            secondMeasurable.filter { it.inVisual }, holeAreaInVisual * 0.6f, holeAreaInVisual * 1.4f
        ) ?: holeAreaInVisual
        val singleOnPaper = typicalArea(
            secondMeasurable.filter { !it.inVisual }, holeAreaOnPaper * 0.6f, holeAreaOnPaper * 1.4f
        ) ?: holeAreaOnPaper
        // Strokes of printed digits are about 1.5 mm wide; the core of a hole is wider than this.
        val minCore = max(4f, 2.6f * pxPerMm)
        for (blob in secondPass.list) {
            if (blob.inVisual) {
                // White print where a ring number is expected, unless a hole is merged with it:
                // a digit is made of thin strokes, a hole has a solid core that is not white.
                if (blob.whiteShare > 0.3f && onDigit(blob, params, centerX, centerY, pxPerMm) &&
                    solidCore(blob, secondPass.pixels, width) { !isWhitePrint(argb, luma, it, whiteLike) } < minCore
                ) {
                    trace?.invoke(blob.x, blob.y, "digit")
                    continue
                }
                accept(blob, secondPass.pixels, singleInVisual)
            } else {
                // Dark print where a ring number is expected, unless it has the solid dark core
                // of a hole.
                if (blob.darkShare > 0.5f && onDigit(blob, params, centerX, centerY, pxPerMm) &&
                    solidCore(blob, secondPass.pixels, width) { luma[it] < paperFloor } < minCore
                ) {
                    trace?.invoke(blob.x, blob.y, "digit")
                    continue
                }
                // Black that sticks out of the visual: a sticker on its edge.
                if (blob.inkShare > 0.6f && hasVisual && blob.minCenterDistance - visualRadius < 1.5f * pxPerMm) {
                    trace?.invoke(blob.x, blob.y, "edge")
                    continue
                }
                accept(blob, secondPass.pixels, singleOnPaper)
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

    /**
     * Counts the holes of a blob from the size of a single one, and adds their centers.
     * @return "ok", or the reason why the blob is not a hole
     */
    private fun classify(
        blob: Blob,
        pixels: IntArray,
        singleArea: Float,
        pxPerMm: Float,
        width: Int,
        height: Int,
        out: MutableList<Offset>
    ): String {
        val ratio = "%.1f".format(blob.area / singleArea)
        val diameter = 2f * sqrt(singleArea / PI.toFloat())
        val blobDiameterMm = 2f * sqrt(blob.area / PI.toFloat()) / pxPerMm
        val compact = blob.solidity > 0.85f && blob.elongation < 1.3f

        if (blob.area < singleArea * 0.35f) {
            // A second, smaller caliber on the same target: accepted only when clean and round.
            if (compact && blob.area > singleArea * 0.15f && blobDiameterMm >= MIN_HOLE_MM && roundness(blob, pixels, width) > 0.8f) {
                out.add(Offset(blob.x / width, blob.y / height))
                return "ok"
            }
            return "small$ratio"
        }
        if (blob.thickness < diameter * 0.5f) return "thin"

        var count = max(1, (blob.area / singleArea).roundToInt())
        // Two overlapping holes cover less than twice the area of one, but the blob is elongated.
        if (count == 1 && blob.elongation > 1.5f && blob.length > diameter * 1.5f) count = 2
        if (count > MAX_HOLES_PER_BLOB) return "big$ratio"

        // Larger than one hole yet a clean disc: overlapping holes have a scalloped outline, so
        // this is either a hole of a larger caliber or, beyond any caliber, a sticker.
        if (count >= 2 && compact && roundness(blob, pixels, width) > 0.8f) {
            if (blobDiameterMm > MAX_HOLE_MM) return "sticker"
            out.add(Offset(blob.x / width, blob.y / height))
            return "ok"
        }
        if (blob.solidity < (if (count == 1) 0.7f else 0.45f)) return "hollow%.2f".format(blob.solidity)
        if (blob.length > (1.3f * count + 0.3f) * diameter) return "long"

        if (count == 1) {
            out.add(Offset(blob.x / width, blob.y / height))
        } else {
            splitCluster(pixels, blob.start, blob.area, width, height, count, diameter, out)
        }
        return "ok"
    }

    /**
     * How much of its enclosing circle the blob fills: close to 1 for a disc, clearly lower for
     * several discs side by side. The circle is the one holding 90 % of the pixels, so a few
     * spikes of torn paper do not count.
     */
    private fun roundness(blob: Blob, pixels: IntArray, width: Int): Float {
        val end = blob.start + blob.area
        var maxDist = 0f
        for (i in blob.start until end) {
            val d = sqrt(square(pixels[i] % width - blob.x) + square(pixels[i] / width - blob.y))
            if (d > maxDist) maxDist = d
        }
        val histogram = IntArray(maxDist.toInt() + 2)
        for (i in blob.start until end) {
            histogram[sqrt(square(pixels[i] % width - blob.x) + square(pixels[i] / width - blob.y)).toInt()]++
        }
        var seen = 0
        var radius = histogram.size.toFloat()
        for (r in histogram.indices) {
            seen += histogram[r]
            if (seen >= blob.area * 0.9f) {
                radius = r + 1f
                break
            }
        }
        // In a disc of radius R, 90 % of the pixels lie within 0.95 R.
        return blob.area / (PI.toFloat() * square(radius / 0.95f))
    }

    /**
     * Side of the largest square that fits in the pixels of the blob for which [isCore] holds:
     * small for thin strokes however long they are, large for a filled spot.
     */
    private inline fun solidCore(blob: Blob, pixels: IntArray, width: Int, isCore: (Int) -> Boolean): Float {
        val end = blob.start + blob.area
        var minX = Int.MAX_VALUE
        var maxX = 0
        var minY = Int.MAX_VALUE
        var maxY = 0
        for (i in blob.start until end) {
            val x = pixels[i] % width
            val y = pixels[i] / width
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val w = maxX - minX + 1
        val h = maxY - minY + 1
        // Dynamic programming on the bounding box: side of the largest square ending at each cell.
        val side = IntArray(w * h)
        for (i in blob.start until end) {
            if (isCore(pixels[i])) side[(pixels[i] / width - minY) * w + (pixels[i] % width - minX)] = 1
        }
        var best = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = y * w + x
                if (side[c] == 0) continue
                if (x > 0 && y > 0) side[c] = 1 + min(side[c - 1], min(side[c - w], side[c - w - 1]))
                if (side[c] > best) best = side[c]
            }
        }
        return best.toFloat()
    }

    /** Whether the blob sits where a ring number is printed. */
    private fun onDigit(blob: Blob, params: ImpactDetectionParams, centerX: Float, centerY: Float, pxPerMm: Float): Boolean {
        if (params.digitRadiiMm.isEmpty()) return false
        val dx = abs(blob.x - centerX) / pxPerMm
        val dy = abs(blob.y - centerY) / pxPerMm
        // Digits are printed along the two axes through the center.
        val along = max(dx, dy)
        val across = min(dx, dy)
        if (across > 12f) return false
        return params.digitRadiiMm.any { abs(along - it) < 12f }
    }

    /**
     * Color distance between a blob and the ring of pixels just around it.
     *
     * A hole stands out from what surrounds it. A piece of the edge between the card and the
     * background, of a printed ring, of a transparent paster or of an object in front has about the
     * color of its own surroundings: its mean color is close to the mean of the ring around it.
     */
    private fun surroundContrast(blob: Blob, pixels: IntArray, width: Int, height: Int, luma: FloatArray, argb: IntArray?): Float {
        val end = blob.start + blob.area
        var r = 0f
        var g = 0f
        var b = 0f
        var reach = 0f
        for (k in blob.start until end) {
            val i = pixels[k]
            r += red(argb, luma, i)
            g += green(argb, luma, i)
            b += blue(argb, luma, i)
            val d = square(i % width - blob.x) + square(i / width - blob.y)
            if (d > reach) reach = d
        }
        r /= blob.area
        g /= blob.area
        b /= blob.area
        // Ring starting a little outside the blob, about half a blob wide.
        val radius = sqrt(reach)
        val inner = radius + max(2f, radius * 0.3f)
        val outer = inner + max(3f, radius * 0.6f)
        var rr = 0f
        var rg = 0f
        var rb = 0f
        var count = 0
        val steps = 48
        for (s in 0 until steps) {
            val angle = 2.0 * PI * s / steps
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            for (t in 0..2) {
                val dist = inner + (outer - inner) * t / 2f
                val x = (blob.x + cos * dist).roundToInt()
                val y = (blob.y + sin * dist).roundToInt()
                if (x < 0 || y < 0 || x >= width || y >= height) continue
                val i = y * width + x
                rr += red(argb, luma, i)
                rg += green(argb, luma, i)
                rb += blue(argb, luma, i)
                count++
            }
        }
        if (count == 0) return 0f
        return sqrt(square(r - rr / count) + square(g - rg / count) + square(b - rb / count))
    }

    /**
     * Median area of the blobs that look like one isolated hole, or null if there are too few.
     * A plain median: stickers and merged holes are larger but rarer than single holes.
     */
    private fun typicalArea(blobs: List<Blob>, minArea: Float, maxArea: Float, minCount: Int = 1): Float? {
        val areas = blobs
            .filter { it.solidity > 0.85f && it.elongation < 1.35f && it.area >= minArea && it.area <= maxArea }
            .map { it.area.toFloat() }
            .sorted()
        if (areas.size < minCount) return null
        return areas[areas.size / 2]
    }

    /**
     * Opens the mask (erasing anything thinner than `2 * opening + 1` pixels), then groups what is
     * left into 8-connected blobs and measures them.
     */
    private fun findBlobs(
        source: BooleanArray,
        width: Int,
        height: Int,
        opening: Int,
        luma: FloatArray,
        inVisual: BooleanArray,
        inkLike: Float,
        darkLike: Float,
        whiteLike: Float,
        centerX: Float,
        centerY: Float,
        minArea: Int
    ): Blobs {
        val n = width * height
        val mask = source.copyOf()
        val scratch = BooleanArray(n)
        val window = 2 * opening + 1
        boxPass(mask, scratch, width, height, opening, horizontal = true, required = window)
        boxPass(scratch, mask, width, height, opening, horizontal = false, required = window)
        boxPass(mask, scratch, width, height, opening, horizontal = true, required = 1)
        boxPass(scratch, mask, width, height, opening, horizontal = false, required = 1)

        // Blob pixels are stored back to back in one buffer: a pixel belongs to at most one blob.
        val pixels = IntArray(n)
        val blobs = ArrayList<Blob>()
        var stored = 0
        // Blobs covering a large part of the photo (background, hand, the paper's surroundings)
        // are measured but never kept.
        val maxArea = n / 8

        for (seed in 0 until n) {
            if (!mask[seed]) continue
            val start = stored
            var head = start
            var tail = start
            pixels[tail++] = seed
            mask[seed] = false
            var sumX = 0.0
            var sumY = 0.0
            var sumXX = 0.0
            var sumYY = 0.0
            var sumXY = 0.0
            var inkCount = 0
            var darkCount = 0
            var whiteCount = 0
            var visualCount = 0
            var minDistSq = Float.MAX_VALUE

            while (head < tail) {
                val p = pixels[head++]
                val px = p % width
                val py = p / width
                sumX += px
                sumY += py
                sumXX += px.toDouble() * px
                sumYY += py.toDouble() * py
                sumXY += px.toDouble() * py
                val l = luma[p]
                if (l < inkLike) inkCount++
                if (l < darkLike) darkCount++
                if (l > whiteLike) whiteCount++
                if (inVisual[p]) visualCount++
                val distSq = square(px - centerX) + square(py - centerY)
                if (distSq < minDistSq) minDistSq = distSq

                val yStart = if (py > 0) py - 1 else 0
                val yEnd = if (py < height - 1) py + 1 else py
                val xStart = if (px > 0) px - 1 else 0
                val xEnd = if (px < width - 1) px + 1 else px
                for (ny in yStart..yEnd) {
                    val nRow = ny * width
                    for (nx in xStart..xEnd) {
                        val q = nRow + nx
                        if (mask[q]) {
                            mask[q] = false
                            pixels[tail++] = q
                        }
                    }
                }
            }

            val area = tail - start
            if (area < minArea || area > maxArea) continue // buffer space is reused

            val meanX = sumX / area
            val meanY = sumY / area
            val varX = sumXX / area - meanX * meanX + 1.0 / 12.0
            val varY = sumYY / area - meanY * meanY + 1.0 / 12.0
            val covXY = sumXY / area - meanX * meanY
            val halfTrace = (varX + varY) / 2.0
            val spread = sqrt(square((varX - varY) / 2.0) + covXY * covXY)
            val sigmaMajor = sqrt(halfTrace + spread)
            val sigmaMinor = sqrt(max(halfTrace - spread, 1.0 / 12.0))
            blobs.add(
                Blob(
                    start = start,
                    area = area,
                    x = meanX.toFloat(),
                    y = meanY.toFloat(),
                    // 1.0 for a filled disc or ellipse, much lower for outlines and ragged shapes
                    solidity = (area / (4.0 * PI * sigmaMajor * sigmaMinor)).toFloat(),
                    elongation = (sigmaMajor / sigmaMinor).toFloat(),
                    length = (4.0 * sigmaMajor).toFloat(),
                    thickness = (4.0 * sigmaMinor).toFloat(),
                    inVisual = visualCount * 2 > area,
                    inkShare = inkCount.toFloat() / area,
                    darkShare = darkCount.toFloat() / area,
                    whiteShare = whiteCount.toFloat() / area,
                    minCenterDistance = sqrt(minDistSq)
                )
            )
            stored = tail
        }
        return Blobs(blobs, pixels)
    }

    /** Splits a blob made of overlapping holes into [count] centers with k-means. */
    private fun splitCluster(
        pixels: IntArray,
        start: Int,
        pixelCount: Int,
        width: Int,
        height: Int,
        count: Int,
        holeDiameter: Float,
        out: MutableList<Offset>
    ) {
        val cx = DoubleArray(count)
        val cy = DoubleArray(count)
        val end = start + pixelCount

        // Deterministic farthest-point initialization.
        var meanX = 0.0
        var meanY = 0.0
        for (i in start until end) {
            meanX += pixels[i] % width
            meanY += pixels[i] / width
        }
        meanX /= pixelCount
        meanY /= pixelCount
        for (k in 0 until count) {
            var best = -1.0
            for (i in start until end) {
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
            for (i in start until end) {
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
     * Pixels of the connected regions of [mask] that are at least [minArea] pixels large and reach
     * the border of the photo, after an
     * opening of [opening] pixels that erases thin strokes (printed rings, digits).
     */
    private fun largeRegions(mask: BooleanArray, width: Int, height: Int, opening: Int, minArea: Int): BooleanArray {
        val n = width * height
        val window = 2 * opening + 1
        val opened = BooleanArray(n)
        val scratch = BooleanArray(n)
        boxPass(mask, scratch, width, height, opening, horizontal = true, required = window)
        boxPass(scratch, opened, width, height, opening, horizontal = false, required = window)
        boxPass(opened, scratch, width, height, opening, horizontal = true, required = 1)
        boxPass(scratch, opened, width, height, opening, horizontal = false, required = 1)

        val out = BooleanArray(n)
        val queue = IntArray(n)
        val seen = BooleanArray(n)
        for (seed in 0 until n) {
            if (!opened[seed] || seen[seed]) continue
            var head = 0
            var tail = 0
            queue[tail++] = seed
            seen[seed] = true
            while (head < tail) {
                val p = queue[head++]
                val px = p % width
                val py = p / width
                for (ny in max(0, py - 1)..min(height - 1, py + 1)) {
                    for (nx in max(0, px - 1)..min(width - 1, px + 1)) {
                        val q = ny * width + nx
                        if (opened[q] && !seen[q]) {
                            seen[q] = true
                            queue[tail++] = q
                        }
                    }
                }
            }
            if (tail < minArea) continue
            // An object held in front of the target (gun, hand) enters the photo from its border;
            // a large region that does not reach the border is part of the target itself (glare on
            // the ink, a sticker), not an object.
            var touchesBorder = false
            for (k in 0 until tail) {
                val q = queue[k]
                val qx = q % width
                val qy = q / width
                if (qx == 0 || qy == 0 || qx == width - 1 || qy == height - 1) {
                    touchesBorder = true
                    break
                }
            }
            if (touchesBorder) for (k in 0 until tail) out[queue[k]] = true
        }
        return out
    }

    private fun dilate(source: BooleanArray, width: Int, height: Int, radius: Int): BooleanArray {
        val scratch = BooleanArray(source.size)
        val out = BooleanArray(source.size)
        boxPass(source, scratch, width, height, radius, horizontal = true, required = 1)
        boxPass(scratch, out, width, height, radius, horizontal = false, required = 1)
        return out
    }

    /**
     * One pass of a box filter on a mask: a pixel is set when at least [required] pixels of the
     * window centered on it are set (the whole window for an erosion, one pixel for a dilation).
     */
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

    /** Bright and colorless, like the white print inside the visual (a hole's backing is tinted or darker). */
    private fun isWhitePrint(argb: IntArray?, luma: FloatArray, i: Int, whiteLike: Float): Boolean {
        if (luma[i] <= whiteLike) return false
        val r = red(argb, luma, i)
        val g = green(argb, luma, i)
        val b = blue(argb, luma, i)
        return max(r, max(g, b)) - min(r, min(g, b)) < 40f
    }

    // A photo without color information is treated as gray.
    private fun red(argb: IntArray?, luma: FloatArray, i: Int) = if (argb != null) ((argb[i] shr 16) and 0xFF).toFloat() else luma[i]
    private fun green(argb: IntArray?, luma: FloatArray, i: Int) = if (argb != null) ((argb[i] shr 8) and 0xFF).toFloat() else luma[i]
    private fun blue(argb: IntArray?, luma: FloatArray, i: Int) = if (argb != null) (argb[i] and 0xFF).toFloat() else luma[i]

    private fun areaOf(diameter: Float) = (PI * diameter * diameter / 4.0).toFloat()
    private fun square(v: Float) = v * v
    private fun square(v: Double) = v * v
}
