package com.example.domain

import androidx.compose.ui.geometry.Offset

/**
 * Center and scale of a target photo, stored relative to the photo itself so that they still hold
 * when the photo is shown again on a canvas of another size.
 *
 * @property centerNormalized target center on the photo ([0..1] on each axis)
 * @property mmToPhoto size of one millimeter as a fraction of the photo width
 * @property imageAspectRatio width / height of the photo
 */
data class PhotoCalibration(
    val centerNormalized: Offset,
    val mmToPhoto: Float,
    val imageAspectRatio: Float
) {
    fun centerOnCanvas(canvasWidth: Float, canvasHeight: Float): Offset =
        ImageFit.toCanvas(centerNormalized, canvasWidth, canvasHeight, imageAspectRatio)

    fun pxPerMmOnCanvas(canvasWidth: Float, canvasHeight: Float): Float =
        mmToPhoto * ImageFit.displayedWidth(canvasWidth, canvasHeight, imageAspectRatio)

    /** Canvas position of a point given in mm from the target center (x right, y up). */
    fun toCanvas(impact: Impact, canvasWidth: Float, canvasHeight: Float): Offset {
        val center = centerOnCanvas(canvasWidth, canvasHeight)
        val pxPerMm = pxPerMmOnCanvas(canvasWidth, canvasHeight)
        return Offset(center.x + impact.xMm * pxPerMm, center.y - impact.yMm * pxPerMm)
    }

    /** Inverse of [toCanvas]. */
    fun toMm(canvasPoint: Offset, canvasWidth: Float, canvasHeight: Float): Impact {
        val center = centerOnCanvas(canvasWidth, canvasHeight)
        val pxPerMm = pxPerMmOnCanvas(canvasWidth, canvasHeight)
        return Impact((canvasPoint.x - center.x) / pxPerMm, (center.y - canvasPoint.y) / pxPerMm)
    }

    companion object {
        /** Calibration of what is on the canvas: center in canvas pixels, scale in pixels per mm. */
        fun fromCanvas(
            center: Offset,
            pxPerMm: Float,
            canvasWidth: Float,
            canvasHeight: Float,
            imageAspectRatio: Float
        ): PhotoCalibration? {
            if (pxPerMm <= 0f || canvasWidth <= 0f || canvasHeight <= 0f || imageAspectRatio <= 0f) return null
            val shownWidth = ImageFit.displayedWidth(canvasWidth, canvasHeight, imageAspectRatio)
            return PhotoCalibration(
                ImageFit.toImage(center, canvasWidth, canvasHeight, imageAspectRatio),
                pxPerMm / shownWidth,
                imageAspectRatio
            )
        }
    }
}
