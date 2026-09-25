package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class TargetDetectionResult(
    val targetType: String,
    val confidencePercent: Int,
    val suggestedDistanceMeters: Int,
    val visualDiameterMm: Float,
    val totalDiameterMm: Float,
    val detectedCenterNormalized: Offset, // Normalized center [0..1]
    val detectedBlackRadiusNormalized: Float, // Normalized radius [0..1]
    val detectedRatio: Float,
    val description: String,
    val details: String
)

object TargetRecognitionEngine {

    suspend fun analyzeImage(context: Context, imageUri: Uri): TargetDetectionResult = withContext(Dispatchers.Default) {
        try {
            val bitmap = decodeSampledBitmap(context, imageUri, reqWidth = 400, reqHeight = 400)
                ?: return@withContext fallbackDetection("Impossible de décoder l'image")

            val width = bitmap.width
            val height = bitmap.height
            if (width < 50 || height < 50) {
                return@withContext fallbackDetection("Image trop petite pour analyse")
            }

            // 1. Grayscale luminance buffer
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

            val dynamicRange = maxLuma - minLuma
            if (dynamicRange < 30f) {
                return@withContext fallbackDetection("Contraste insuffisant sur la photo")
            }

            // Adaptive threshold for the dark center visual
            val darkThreshold = minLuma + dynamicRange * 0.35f

            // 2. Find center of mass of dark pixels (bullseye / visuel noir)
            var sumX = 0.0
            var sumY = 0.0
            var darkCount = 0

            // Focus on inner 80% to avoid dark borders
            val marginX = (width * 0.10f).toInt()
            val marginY = (height * 0.10f).toInt()

            for (y in marginY until height - marginY) {
                val rowOffset = y * width
                for (x in marginX until width - marginX) {
                    if (luma[rowOffset + x] < darkThreshold) {
                        sumX += x
                        sumY += y
                        darkCount++
                    }
                }
            }

            val totalEligible = (width - 2 * marginX) * (height - 2 * marginY)
            val darkRatioTotal = darkCount.toFloat() / max(1, totalEligible)

            if (darkCount < 100 || darkRatioTotal < 0.02f) {
                return@withContext TargetDetectionResult(
                    targetType = "IPSC Classic / Métallique",
                    confidencePercent = 78,
                    suggestedDistanceMeters = 25,
                    visualDiameterMm = 300f,
                    totalDiameterMm = 450f,
                    detectedCenterNormalized = Offset(0.5f, 0.5f),
                    detectedBlackRadiusNormalized = 0.25f,
                    detectedRatio = 0.25f,
                    description = "Aucun visuel noir circulaire prononcé. Profil compatible Silhouette / Cible IPSC.",
                    details = "Zones A/C/D ou silhouette métallique sans centre noir ISSF."
                )
            }

            val centerX = (sumX / darkCount).toFloat()
            val centerY = (sumY / darkCount).toFloat()

            // 3. Radial profile scan from center to estimate circular bullseye radius
            val numRays = 16
            val maxRayLen = min(width, height) * 0.45f
            val step = 2f
            var totalRadiusAcc = 0f
            var validRays = 0

            for (ray in 0 until numRays) {
                val angle = (ray.toDouble() / numRays) * 2.0 * Math.PI
                val cosA = Math.cos(angle)
                val sinA = Math.sin(angle)

                var rayRadius = 0f
                var r = step
                while (r < maxRayLen) {
                    val px = (centerX + r * cosA).roundToInt()
                    val py = (centerY + r * sinA).roundToInt()

                    if (px !in 0 until width || py !in 0 until height) break

                    val valAtPoint = luma[py * width + px]
                    if (valAtPoint > darkThreshold) {
                        rayRadius = r.toFloat()
                        break
                    }
                    r += step
                }

                if (rayRadius > 10f) {
                    totalRadiusAcc += rayRadius
                    validRays++
                }
            }

            val estimatedRadiusPx = if (validRays > 0) totalRadiusAcc / validRays else (min(width, height) * 0.18f)
            val cardRadiusEstimatePx = min(width, height) * 0.45f
            val radiusRatio = (estimatedRadiusPx / cardRadiusEstimatePx).coerceIn(0.1f, 0.95f)

            // 4. Classify target type based on ratio and geometry
            // C50 standard: black visual (7-10) is 200mm on 500mm => ratio ~ 0.40
            // Carabine 10m ISSF: black visual (4-10) is 30.5mm on 45.5mm => ratio ~ 0.67
            // Pistolet 10m ISSF: black visual (7-10) is 59.5mm on 170mm => ratio ~ 0.35
            // C200 Carabine: black visual is 400mm on 800mm => ratio ~ 0.50
            return@withContext classifyTarget(
                radiusRatio = radiusRatio,
                centerX = centerX / width,
                centerY = centerY / height,
                detectedRadiusNormalized = estimatedRadiusPx / width
            )
        } catch (e: Exception) {
            fallbackDetection("Erreur d'analyse : ${e.message}")
        }
    }

    private fun classifyTarget(
        radiusRatio: Float,
        centerX: Float,
        centerY: Float,
        detectedRadiusNormalized: Float
    ): TargetDetectionResult {
        return when {
            // Carabine 10m ISSF : Very large black bullseye relative to target card (~67%)
            radiusRatio >= 0.58f -> {
                val conf = (95 - abs(radiusRatio - 0.67f) * 100).toInt().coerceIn(75, 98)
                TargetDetectionResult(
                    targetType = "Carabine 10m (ISSF)",
                    confidencePercent = conf,
                    suggestedDistanceMeters = 10,
                    visualDiameterMm = 30.5f,
                    totalDiameterMm = 45.5f,
                    detectedCenterNormalized = Offset(centerX, centerY),
                    detectedBlackRadiusNormalized = detectedRadiusNormalized,
                    detectedRatio = radiusRatio,
                    description = "Cible Carabine 10m ISSF reconnue (visuel noir 30.5 mm couvrant les zones 4 à 10).",
                    details = "Visuel central dominant (${(radiusRatio * 100).roundToInt()}% du carton). Échelle micro-cible calibrée."
                )
            }
            // C200 (Carabine 200m / 300m) : Black visual covers ~50%
            radiusRatio in 0.46f..0.57f -> {
                val conf = (94 - abs(radiusRatio - 0.50f) * 80).toInt().coerceIn(75, 96)
                TargetDetectionResult(
                    targetType = "C200 (200m)",
                    confidencePercent = conf,
                    suggestedDistanceMeters = 200,
                    visualDiameterMm = 400f,
                    totalDiameterMm = 800f,
                    detectedCenterNormalized = Offset(centerX, centerY),
                    detectedBlackRadiusNormalized = detectedRadiusNormalized,
                    detectedRatio = radiusRatio,
                    description = "Cible C200 Longue Distance reconnue (visuel noir 400 mm).",
                    details = "Format grand carton 80x80 cm, centre noir du 6 au 10. Distance conseillée : 200m ou 300m."
                )
            }
            // C50 (Pistolet 25m / Carabine 50m) : Golden standard French/European target, ratio ~0.40
            radiusRatio in 0.36f..0.459f -> {
                val conf = (96 - abs(radiusRatio - 0.40f) * 100).toInt().coerceIn(80, 98)
                TargetDetectionResult(
                    targetType = "C50 (25m/50m)",
                    confidencePercent = conf,
                    suggestedDistanceMeters = 25,
                    visualDiameterMm = 200f,
                    totalDiameterMm = 500f,
                    detectedCenterNormalized = Offset(centerX, centerY),
                    detectedBlackRadiusNormalized = detectedRadiusNormalized,
                    detectedRatio = radiusRatio,
                    description = "Cible C50 standard reconnue (visuel noir de 200 mm du 7 au 10).",
                    details = "Format officiel FFTir 50x50 cm pour pistolet 25m et carabine 50m. Ratio noir/blanc conforme à 40%."
                )
            }
            // Pistolet 10m (ISSF) : Black visual ratio ~0.35 (59.5mm / 170mm)
            radiusRatio in 0.26f..0.359f -> {
                val conf = (92 - abs(radiusRatio - 0.35f) * 90).toInt().coerceIn(75, 95)
                TargetDetectionResult(
                    targetType = "Pistolet 10m (ISSF)",
                    confidencePercent = conf,
                    suggestedDistanceMeters = 10,
                    visualDiameterMm = 59.5f,
                    totalDiameterMm = 170f,
                    detectedCenterNormalized = Offset(centerX, centerY),
                    detectedBlackRadiusNormalized = detectedRadiusNormalized,
                    detectedRatio = radiusRatio,
                    description = "Cible Pistolet 10m Air Comprimé ISSF reconnue (visuel noir 59.5 mm).",
                    details = "Carton standard 17x17 cm, visuel noir du 7 au 10. Distance 10 mètres."
                )
            }
            // Small visual or multiple spots => Cible Hunter ou Benchrest
            else -> {
                TargetDetectionResult(
                    targetType = "Cible Hunter / Précision",
                    confidencePercent = 75,
                    suggestedDistanceMeters = 50,
                    visualDiameterMm = 100f,
                    totalDiameterMm = 500f,
                    detectedCenterNormalized = Offset(centerX, centerY),
                    detectedBlackRadiusNormalized = detectedRadiusNormalized,
                    detectedRatio = radiusRatio,
                    description = "Cible à petit visuel de précision / Benchrest ou Hunter.",
                    details = "Visuel compact (${(radiusRatio * 100).roundToInt()}% du champ). Centrage automatique calculé."
                )
            }
        }
    }

    private fun fallbackDetection(reason: String): TargetDetectionResult {
        return TargetDetectionResult(
            targetType = "C50 (25m/50m)",
            confidencePercent = 70,
            suggestedDistanceMeters = 25,
            visualDiameterMm = 200f,
            totalDiameterMm = 500f,
            detectedCenterNormalized = Offset(0.5f, 0.5f),
            detectedBlackRadiusNormalized = 0.20f,
            detectedRatio = 0.40f,
            description = "Détection par défaut : Cible standard C50 (25m / 50m).",
            details = reason
        )
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
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }
}
