package com.example.domain

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoCalibrationTest {

    private val tolerance = 0.01f

    // A portrait photo (3:4) shown on a 1080 x 1300 canvas, calibrated at 2.68 px/mm.
    private val aspect = 0.75f
    private val canvasW = 1080f
    private val canvasH = 1300f
    private val center = Offset(540f, 640f)
    private val pxPerMm = 2.68f

    @Test
    fun `saving then restoring the calibration on the same canvas gives it back`() {
        val saved = PhotoCalibration.fromCanvas(center, pxPerMm, canvasW, canvasH, aspect)!!
        val restoredCenter = saved.centerOnCanvas(canvasW, canvasH)
        assertEquals(center.x, restoredCenter.x, tolerance)
        assertEquals(center.y, restoredCenter.y, tolerance)
        assertEquals(pxPerMm, saved.pxPerMmOnCanvas(canvasW, canvasH), tolerance)
    }

    @Test
    fun `an impact placed by hand comes back at the same place after reopening`() {
        // The QA case: an impact 150 px left of the center ended 225 px away after reopening.
        val tapped = Offset(center.x - 150f, center.y + 40f)
        val impactMm = Impact((tapped.x - center.x) / pxPerMm, (center.y - tapped.y) / pxPerMm)

        val saved = PhotoCalibration.fromCanvas(center, pxPerMm, canvasW, canvasH, aspect)!!
        val reopened = saved.toCanvas(impactMm, canvasW, canvasH)

        assertEquals(tapped.x, reopened.x, tolerance)
        assertEquals(tapped.y, reopened.y, tolerance)
    }

    @Test
    fun `an impact stays on the same point of the photo when the canvas size changes`() {
        val saved = PhotoCalibration.fromCanvas(center, pxPerMm, canvasW, canvasH, aspect)!!
        val impactMm = Impact(-56f, 15f)

        val onFirst = saved.toCanvas(impactMm, canvasW, canvasH)
        val photoPoint = ImageFit.toImage(onFirst, canvasW, canvasH, aspect)

        val otherW = 720f
        val otherH = 1100f
        val onOther = saved.toCanvas(impactMm, otherW, otherH)
        val samePhotoPoint = ImageFit.toCanvas(photoPoint, otherW, otherH, aspect)
        assertEquals(samePhotoPoint.x, onOther.x, tolerance)
        assertEquals(samePhotoPoint.y, onOther.y, tolerance)

        // And back to mm.
        val back = saved.toMm(onOther, otherW, otherH)
        assertEquals(impactMm.xMm, back.xMm, tolerance)
        assertEquals(impactMm.yMm, back.yMm, tolerance)
    }

    @Test
    fun `no calibration without a scale or a photo shape`() {
        assertNull(PhotoCalibration.fromCanvas(center, 0f, canvasW, canvasH, aspect))
        assertNull(PhotoCalibration.fromCanvas(center, pxPerMm, canvasW, canvasH, 0f))
    }
}
