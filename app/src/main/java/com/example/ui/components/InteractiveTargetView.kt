package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShotEntity
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun InteractiveTargetView(
    modifier: Modifier = Modifier,
    shots: List<ShotEntity>,
    targetType: String = "C50 (25m/50m)",
    isReadOnly: Boolean = false,
    showBarycenter: Boolean = true,
    showGroupingCircle: Boolean = true,
    onTargetTapped: (xRatio: Float, yRatio: Float) -> Unit = { _, _ -> }
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFE8D8)) // Realistic paper target color
            .testTag("interactive_target_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(isReadOnly) {
                    if (!isReadOnly) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = min(size.width, size.height) / 2f
                            // Normalized coordinates from -1.0 to 1.0 relative to target radius
                            val xRatio = (offset.x - center.x) / radius
                            val yRatio = (offset.y - center.y) / radius
                            onTargetTapped(xRatio, yRatio)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = min(size.width, size.height) / 2f

            // Target Rings configuration:
            // C50 / ISSF: rings 1 to 10
            // Rings 7 to 10 are inside the black central visual zone (Le Visuel)
            val ringCount = 10
            val blackZoneRingStart = 7 // rings >= 7 are black

            // 1. Draw black zone circle (from ring 7 to center)
            // Ring 7 boundary is at radius: maxRadius * (11 - 7) / 10 = maxRadius * 0.4f
            val blackRadius = maxRadius * ((11f - blackZoneRingStart) / ringCount)
            drawCircle(
                color = Color(0xFF18181B), // Target deep black
                radius = blackRadius,
                center = center
            )

            // 2. Draw all concentric ring lines & numbers
            val textStyleBlack = TextStyle(
                color = Color(0xFF27272A),
                fontSize = (maxRadius * 0.055f).sp,
                fontWeight = FontWeight.Bold
            )
            val textStyleWhite = TextStyle(
                color = Color(0xFFF4F4F5),
                fontSize = (maxRadius * 0.055f).sp,
                fontWeight = FontWeight.Bold
            )

            for (ring in 1..ringCount) {
                // Ring 1 is outermost (ratio 1.0), ring 10 is innermost (ratio 0.1)
                val ringRadius = maxRadius * ((11f - ring) / ringCount)
                val isInsideBlack = ring >= blackZoneRingStart
                val lineColor = if (isInsideBlack) Color(0xFF52525B) else Color(0xFFA1A1AA)

                drawCircle(
                    color = lineColor,
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = if (ring == blackZoneRingStart) 2f else 1.2f)
                )

                // Draw ring numbers (e.g. 1, 2, ..., 8, 9)
                if (ring in 1..9) {
                    val label = ring.toString()
                    val textLayout = textMeasurer.measure(
                        label,
                        style = if (isInsideBlack) textStyleWhite else textStyleBlack
                    )
                    val labelDist = ringRadius - (maxRadius / ringCount) * 0.5f

                    // Draw at 4 positions (top, bottom, left, right) for outer rings, or top/bottom for inner
                    val drawPositions = if (ring < 7) {
                        listOf(
                            Offset(center.x, center.y - labelDist),
                            Offset(center.x, center.y + labelDist),
                            Offset(center.x - labelDist, center.y),
                            Offset(center.x + labelDist, center.y)
                        )
                    } else {
                        listOf(
                            Offset(center.x, center.y - labelDist),
                            Offset(center.x, center.y + labelDist)
                        )
                    }

                    for (pos in drawPositions) {
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                pos.x - textLayout.size.width / 2f,
                                pos.y - textLayout.size.height / 2f
                            )
                        )
                    }
                }
            }

            // 3. Mouche / Inner 10 center cross and ring
            val moucheRadius = maxRadius * (0.05f)
            drawCircle(
                color = Color(0xFF71717A),
                radius = moucheRadius,
                center = center,
                style = Stroke(width = 1f)
            )
            // Center small cross
            val crossSize = maxRadius * 0.035f
            drawLine(
                color = Color(0xFFE4E4E7),
                start = Offset(center.x - crossSize, center.y),
                end = Offset(center.x + crossSize, center.y),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color(0xFFE4E4E7),
                start = Offset(center.x, center.y - crossSize),
                end = Offset(center.x, center.y + crossSize),
                strokeWidth = 1.2f
            )

            // 4. Draw shots grouping circle (if 2+ shots and enabled)
            if (showGroupingCircle && shots.size >= 2) {
                var sumX = 0f
                var sumY = 0f
                for (s in shots) {
                    sumX += s.xRatio
                    sumY += s.yRatio
                }
                val avgX = sumX / shots.size
                val avgY = sumY / shots.size
                val barycenterOffset = Offset(
                    center.x + avgX * maxRadius,
                    center.y + avgY * maxRadius
                )

                // Max distance to barycenter for grouping circle radius
                var maxDistFromCenter = 0f
                for (s in shots) {
                    val sx = center.x + s.xRatio * maxRadius
                    val sy = center.y + s.yRatio * maxRadius
                    val d = hypot(sx - barycenterOffset.x, sy - barycenterOffset.y)
                    if (d > maxDistFromCenter) maxDistFromCenter = d
                }

                // Draw dashed circle encompassing grouping
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.6f),
                    radius = maxDistFromCenter + (maxRadius * 0.03f),
                    center = barycenterOffset,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )

                // Draw barycenter crosshair
                if (showBarycenter) {
                    val bSize = maxRadius * 0.05f
                    // Cyan tactical barycenter marker
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 4f,
                        center = barycenterOffset
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(barycenterOffset.x - bSize, barycenterOffset.y),
                        end = Offset(barycenterOffset.x + bSize, barycenterOffset.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(barycenterOffset.x, barycenterOffset.y - bSize),
                        end = Offset(barycenterOffset.x, barycenterOffset.y + bSize),
                        strokeWidth = 2f
                    )
                }
            }

            // 5. Draw bullet impacts
            val impactRadius = maxRadius * 0.035f // Caliber size relative to target

            shots.forEachIndexed { index, shot ->
                val shotPos = Offset(
                    center.x + shot.xRatio * maxRadius,
                    center.y + shot.yRatio * maxRadius
                )
                val isLatest = index == shots.lastIndex

                // Shadow / hole depth effect
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = impactRadius + 2f,
                    center = Offset(shotPos.x + 1f, shotPos.y + 1f)
                )

                // Bullet hole lead/punch color
                val impactColor = when {
                    isLatest -> Color(0xFFEF4444) // Bright red highlight for latest shot
                    shot.isInnerTen -> Color(0xFFEAB308) // Amber/Gold for Mouche
                    else -> Color(0xFF3B82F6) // Electric blue for logged impacts
                }

                drawCircle(
                    color = impactColor,
                    radius = impactRadius,
                    center = shotPos
                )
                drawCircle(
                    color = Color.White,
                    radius = impactRadius,
                    center = shotPos,
                    style = Stroke(width = 1.5f)
                )

                // Impact number text
                val shotNumLayout = textMeasurer.measure(
                    shot.shotIndex.toString(),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (impactRadius * 1.1f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                drawText(
                    textLayoutResult = shotNumLayout,
                    topLeft = Offset(
                        shotPos.x - shotNumLayout.size.width / 2f,
                        shotPos.y - shotNumLayout.size.height / 2f
                    )
                )
            }
        }
    }
}
