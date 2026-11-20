package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Result of looking for the black visual on a photo.
 *
 * @property visualFound false when no black visual could be located; the geometry is then a
 *   placeholder (center of the photo)
 * @property confidencePercent share of the visual's outline that was actually seen on the photo
 * @property visualDiameterMm real diameter of the black visual for [targetType]: together with
 *   [detectedBlackRadiusNormalized] it gives the scale of the photo
 * @property detectedBlackRadiusNormalized radius of the visual as a fraction of the photo width
 */
data class TargetDetectionResult(
    val targetType: String,
    val visualFound: Boolean,
    val confidencePercent: Int,
    val visualDiameterMm: Float,
    val detectedCenterNormalized: Offset, // Normalized center [0..1]
    val detectedBlackRadiusNormalized: Float,
    val description: String,
    val details: String,
    val imageAspectRatio: Float = 1f // width / height of the analyzed (upright) image
)

/**
 * Decoded photo: luminance (one float per pixel, row-major) with its dynamic range, and the
 * original colors as packed ARGB when available.
 */
class LumaImage(
    val width: Int,
    val height: Int,
    val luma: FloatArray,
    val minLuma: Float,
    val maxLuma: Float,
    val argb: IntArray? = null
)

/**
 * Maps coordinates normalized to the photo ([0..1] on each axis) onto a canvas that displays the
 * photo with `ContentScale.Fit` (centered, letterboxed when aspect ratios differ).
 */
object ImageFit {
    fun displayedWidth(canvasWidth: Float, canvasHeight: Float, imageAspectRatio: Float): Float {
        if (canvasWidth <= 0f || canvasHeight <= 0f || imageAspectRatio <= 0f) return canvasWidth
        return min(canvasWidth, canvasHeight * imageAspectRatio)
    }

    fun toCanvas(normalized: Offset, canvasWidth: Float, canvasHeight: Float, imageAspectRatio: Float): Offset {
        if (canvasWidth <= 0f || canvasHeight <= 0f || imageAspectRatio <= 0f) {
            return Offset(normalized.x * canvasWidth, normalized.y * canvasHeight)
        }
        val shownWidth = displayedWidth(canvasWidth, canvasHeight, imageAspectRatio)
        val shownHeight = shownWidth / imageAspectRatio
        return Offset(
            (canvasWidth - shownWidth) / 2f + normalized.x * shownWidth,
            (canvasHeight - shownHeight) / 2f + normalized.y * shownHeight
        )
    }

    /** Inverse of [toCanvas]: canvas point to photo-normalized coordinates. */
    fun toImage(canvasPoint: Offset, canvasWidth: Float, canvasHeight: Float, imageAspectRatio: Float): Offset {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return Offset(0.5f, 0.5f)
        if (imageAspectRatio <= 0f) return Offset(canvasPoint.x / canvasWidth, canvasPoint.y / canvasHeight)
        val shownWidth = displayedWidth(canvasWidth, canvasHeight, imageAspectRatio)
        val shownHeight = shownWidth / imageAspectRatio
        return Offset(
            (canvasPoint.x - (canvasWidth - shownWidth) / 2f) / shownWidth,
            (canvasPoint.y - (canvasHeight - shownHeight) / 2f) / shownHeight
        )
    }
}

object TargetRecognitionEngine {

    /**
     * Detects bullet holes on the photo.
     * @return hole centers normalized to the photo ([0..1], origin top-left, Y pointing down).
     */
    suspend fun detectImpacts(
        context: Context,
        imageUri: Uri,
        params: ImpactDetectionParams
    ): List<Offset> = withContext(Dispatchers.Default) {
        try {
            val image = loadLuma(context, imageUri, reqSize = 800) ?: return@withContext emptyList()
            ImpactDetector.detect(image, params)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Locates the black visual on the photo.
     *
     * The kind of target cannot be told from a photo (it depends on how it was framed), so the
     * caller says which target it is and how wide its visual really is; null for a target that
     * has no black visual (nothing is searched then).
     */
    suspend fun analyzeImage(
        context: Context,
        imageUri: Uri,
        targetType: String,
        visualDiameterMm: Float?
    ): TargetDetectionResult = withContext(Dispatchers.Default) {
        if (visualDiameterMm == null || visualDiameterMm <= 0f) {
            return@withContext noVisual(targetType)
        }
        try {
            val image = loadLuma(context, imageUri, reqSize = 800)
                ?: return@withContext notFound(targetType, visualDiameterMm, 1f, "Impossible de décoder l'image.")
            analyzeLuma(image, targetType, visualDiameterMm)
        } catch (e: Exception) {
            notFound(targetType, visualDiameterMm, 1f, "Erreur d'analyse : ${e.message}")
        }
    }

    /**
     * Locates the target's circles on the photo, so that hits are scored against the rings that
     * are really there (perspective, curved paper).
     *
     * @param centerNormalized calibrated center, normalized to the photo
     * @param visualRadiusNormalized calibrated radius of the black visual, as a fraction of the photo width
     */
    suspend fun buildRingModel(
        context: Context,
        imageUri: Uri,
        centerNormalized: Offset,
        visualRadiusNormalized: Float,
        visualRadiusMm: Float,
        targetType: String
    ): RingModel? = withContext(Dispatchers.Default) {
        try {
            val image = loadLuma(context, imageUri, reqSize = 800) ?: return@withContext null
            RingModel.build(
                image,
                centerX = centerNormalized.x * image.width,
                centerY = centerNormalized.y * image.height,
                visualRadiusPx = visualRadiusNormalized * image.width,
                visualRadiusMm = visualRadiusMm,
                printedRingsMm = TargetScoring.printedRingRadiiMm(targetType)
            )
        } catch (e: Exception) {
            null
        }
    }

    internal fun analyzeLuma(image: LumaImage, targetType: String, visualDiameterMm: Float?): TargetDetectionResult {
        if (visualDiameterMm == null || visualDiameterMm <= 0f) return noVisual(targetType)
        val aspectRatio = image.width.toFloat() / image.height
        val circle = BlackVisualFinder.find(image)
            ?: return notFound(targetType, visualDiameterMm, aspectRatio, "Aucun visuel noir circulaire n'a été repéré sur la photo.")

        val seen = (circle.support * 100).roundToInt()
        return TargetDetectionResult(
            targetType = targetType,
            visualFound = true,
            confidencePercent = seen,
            visualDiameterMm = visualDiameterMm,
            detectedCenterNormalized = Offset(circle.centerX / image.width, circle.centerY / image.height),
            detectedBlackRadiusNormalized = circle.radius / image.width,
            description = "Visuel noir repéré : centre et échelle calculés sur son contour.",
            details = if (seen >= 70) {
                "Contour visible à $seen %."
            } else {
                "Contour visible à $seen % seulement (visuel coupé par le cadre, masqué ou photo de biais) : vérifiez le centrage."
            },
            imageAspectRatio = aspectRatio
        )
    }

    private fun noVisual(targetType: String): TargetDetectionResult = TargetDetectionResult(
        targetType = targetType,
        visualFound = false,
        confidencePercent = 0,
        visualDiameterMm = 0f,
        detectedCenterNormalized = Offset(0.5f, 0.5f),
        detectedBlackRadiusNormalized = 0f,
        description = "Ce type de cible n'a pas de visuel noir : le centre et l'échelle ne peuvent pas être repérés automatiquement.",
        details = "Calibrez la cible à la main avec « Calibrer visuel » sur un repère de taille connue."
    )

    private fun notFound(targetType: String, visualDiameterMm: Float, aspectRatio: Float, reason: String): TargetDetectionResult {
        return TargetDetectionResult(
            targetType = targetType,
            visualFound = false,
            confidencePercent = 0,
            visualDiameterMm = visualDiameterMm,
            detectedCenterNormalized = Offset(0.5f, 0.5f),
            detectedBlackRadiusNormalized = 0f,
            description = reason,
            details = "Calibrez la cible à la main avec « Calibrer visuel ».",
            imageAspectRatio = aspectRatio
        )
    }

    /** Decodes a downsampled, upright copy of the photo and converts it to a luminance buffer. */
    private fun loadLuma(context: Context, uri: Uri, reqSize: Int): LumaImage? {
        val bitmap = decodeSampledBitmap(context, uri, reqSize, reqSize) ?: return null
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val luma = FloatArray(width * height)
            var minLuma = 255f
            var maxLuma = 0f
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                luma[i] = lum
                if (lum < minLuma) minLuma = lum
                if (lum > maxLuma) maxLuma = lum
            }
            return LumaImage(width, height, luma, minLuma, maxLuma, pixels)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null

            applyExifOrientation(context, uri, decoded)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The photo is displayed upright (the image loader honors EXIF), so the analysis must run on
     * the same orientation or detected coordinates would not match what the user sees.
     */
    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
