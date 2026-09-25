package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.BallisticsCalculator
import com.example.domain.GroupStats
import com.example.domain.Impact
import java.util.Locale
import kotlin.math.hypot

enum class CanvasInteractionMode {
    CALIBRATION,
    TARGET_CENTER,
    IMPACTS,
    PAN_ZOOM
}

data class AnnotatedPoint(
    val index: Int,
    val canvasOffset: Offset,
    val realMm: Impact
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetCanvas(
    modifier: Modifier = Modifier,
    imageUri: Uri?,
    distanceMeters: Float = 25f,
    onStatsUpdated: (GroupStats) -> Unit = {}
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // Pan & Zoom transformations
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Active interaction mode
    var activeMode by remember { mutableStateOf(CanvasInteractionMode.IMPACTS) }

    // Calibration state
    var calibrationPoints = remember { mutableStateListOf<Offset>() }
    var pixelPerMm by remember { mutableFloatStateOf(0f) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var inputDistanceMm by remember { mutableStateOf("50") }

    // Target center in unscaled canvas pixels
    var targetCenterPx by remember { mutableStateOf<Offset?>(null) }

    // Annotated impacts
    val annotatedImpacts = remember { mutableStateListOf<AnnotatedPoint>() }

    // Grouping stats computed via BallisticsCalculator
    var currentStats by remember {
        mutableStateOf(
            GroupStats(
                hMm = 0f,
                lMm = 0f,
                esMm = 0f,
                hPlusL = 0f,
                moa = 0f,
                mpiDeltaXMm = 0f,
                mpiDeltaYMm = 0f
            )
        )
    }

    // Helper to recalculate ballistics stats
    fun refreshStats() {
        val impactsList = annotatedImpacts.map { it.realMm }
        val stats = BallisticsCalculator.calculateStats(
            impacts = impactsList,
            targetCenter = Impact(0f, 0f),
            distanceMeters = distanceMeters
        )
        currentStats = stats
        onStatsUpdated(stats)
    }

    // Handle single tap in unscaled canvas space
    fun handleCanvasTap(canvasPoint: Offset) {
        when (activeMode) {
            CanvasInteractionMode.CALIBRATION -> {
                if (calibrationPoints.size < 2) {
                    calibrationPoints.add(canvasPoint)
                    if (calibrationPoints.size == 2) {
                        showCalibrationDialog = true
                    }
                } else {
                    // Reset calibration points on new tap
                    calibrationPoints.clear()
                    calibrationPoints.add(canvasPoint)
                }
            }

            CanvasInteractionMode.TARGET_CENTER -> {
                targetCenterPx = canvasPoint
                // Recalculate real mm coordinates of all existing impacts relative to new center
                if (pixelPerMm > 0f) {
                    val updated = annotatedImpacts.map { pt ->
                        val xMm = (pt.canvasOffset.x - canvasPoint.x) / pixelPerMm
                        val yMm = (canvasPoint.y - pt.canvasOffset.y) / pixelPerMm
                        pt.copy(realMm = Impact(xMm, yMm))
                    }
                    annotatedImpacts.clear()
                    annotatedImpacts.addAll(updated)
                    refreshStats()
                }
            }

            CanvasInteractionMode.IMPACTS -> {
                val center = targetCenterPx ?: Offset(500f, 500f) // Fallback default center
                val pxPerMm = if (pixelPerMm > 0f) pixelPerMm else 4f // Fallback scale: ~4px = 1mm

                val xMm = (canvasPoint.x - center.x) / pxPerMm
                // Standard Cartesian: Y positive upwards
                val yMm = (center.y - canvasPoint.y) / pxPerMm

                val newImpact = AnnotatedPoint(
                    index = annotatedImpacts.size + 1,
                    canvasOffset = canvasPoint,
                    realMm = Impact(xMm, yMm)
                )
                annotatedImpacts.add(newImpact)
                refreshStats()
            }

            CanvasInteractionMode.PAN_ZOOM -> {
                // No action on single tap in pan/zoom mode
            }
        }
    }

    // Calibration distance input dialog
    if (showCalibrationDialog && calibrationPoints.size == 2) {
        val p1 = calibrationPoints[0]
        val p2 = calibrationPoints[1]
        val distancePx = hypot(p1.x - p2.x, p1.y - p2.y)

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = {
                Text("Étalonnage de la cible", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Distance mesurée à l'écran : ${String.format(Locale.US, "%.1f", distancePx)} pixels.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Indiquez la distance réelle correspondante en millimètres (ex: diamètre du visuel noir C50 = 200 mm, ou quadrillage = 50 mm) :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputDistanceMm,
                        onValueChange = { inputDistanceMm = it },
                        label = { Text("Distance réelle (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    // Preset quick buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("25", "50", "100", "200").forEach { preset ->
                            TextButton(onClick = { inputDistanceMm = preset }) {
                                Text("${preset}mm")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val realMmVal = inputDistanceMm.toFloatOrNull() ?: 50f
                        if (realMmVal > 0f && distancePx > 0f) {
                            pixelPerMm = distancePx / realMmVal
                            // Re-calculate real mm for all impacts
                            val center = targetCenterPx ?: Offset(500f, 500f)
                            val updated = annotatedImpacts.map { pt ->
                                val xMm = (pt.canvasOffset.x - center.x) / pixelPerMm
                                val yMm = (center.y - pt.canvasOffset.y) / pixelPerMm
                                pt.copy(realMm = Impact(xMm, yMm))
                            }
                            annotatedImpacts.clear()
                            annotatedImpacts.addAll(updated)
                            refreshStats()
                            activeMode = CanvasInteractionMode.IMPACTS
                        }
                        showCalibrationDialog = false
                    }
                ) {
                    Text("Valider l'échelle")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    calibrationPoints.clear()
                    showCalibrationDialog = false
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("target_canvas_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector Bar
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = activeMode == CanvasInteractionMode.IMPACTS,
                onClick = { activeMode = CanvasInteractionMode.IMPACTS },
                label = { Text("Impacts (${annotatedImpacts.size})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AdsClick,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            FilterChip(
                selected = activeMode == CanvasInteractionMode.TARGET_CENTER,
                onClick = { activeMode = CanvasInteractionMode.TARGET_CENTER },
                label = { Text(if (targetCenterPx != null) "Centre visé ✓" else "Centre visé") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = null,
                        tint = Color(0xFF10B981)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            FilterChip(
                selected = activeMode == CanvasInteractionMode.CALIBRATION,
                onClick = { activeMode = CanvasInteractionMode.CALIBRATION },
                label = {
                    Text(
                        if (pixelPerMm > 0f) "Étalonné (${String.format(Locale.US, "%.1f", pixelPerMm)} px/mm)"
                        else "Étalonnage"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            FilterChip(
                selected = activeMode == CanvasInteractionMode.PAN_ZOOM,
                onClick = { activeMode = CanvasInteractionMode.PAN_ZOOM },
                label = { Text("Zoom/Pan") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null
                    )
                }
            )
        }

        // Action controls (Reset zoom, Undo impact)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (activeMode) {
                    CanvasInteractionMode.CALIBRATION -> "Touchez 2 points connus pour calibrer l'échelle en mm."
                    CanvasInteractionMode.TARGET_CENTER -> "Touchez le centre exact de la cible (mouche / 10)."
                    CanvasInteractionMode.IMPACTS -> "Touchez chaque impact pour le marquer en rouge."
                    CanvasInteractionMode.PAN_ZOOM -> "Glissez pour vous déplacer, pincez pour zoomer."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Réinitialiser le zoom",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (annotatedImpacts.isNotEmpty()) {
                            annotatedImpacts.removeAt(annotatedImpacts.lastIndex)
                            refreshStats()
                        }
                    },
                    enabled = annotatedImpacts.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Annuler dernier impact",
                        tint = if (annotatedImpacts.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Target Photo & Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E232B))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .clipToBounds()
                .pointerInput(activeMode) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                        offset += pan
                    }
                }
                .pointerInput(activeMode) {
                    detectTapGestures { tapPos ->
                        val unscaledX = (tapPos.x - offset.x) / scale
                        val unscaledY = (tapPos.y - offset.y) / scale
                        handleCanvasTap(Offset(unscaledX, unscaledY))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Photo de la cible annotée",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            } else {
                // Placeholder when no photo is attached
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune photo de cible chargée",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Le canevas fonctionne également en mode virtuel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Annotation Overlay Canvas (scaled and panned with the photo)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                // Initialize default center if not set
                if (targetCenterPx == null) {
                    targetCenterPx = Offset(size.width / 2f, size.height / 2f)
                }

                // 1. Draw Target Center Reference (Green Reticle)
                targetCenterPx?.let { center ->
                    val reticleRadius = 14f
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = reticleRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 2.5f,
                        center = center
                    )
                    // Crosshair arms
                    val armLength = 24f
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(center.x - armLength, center.y),
                        end = Offset(center.x + armLength, center.y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(center.x, center.y - armLength),
                        end = Offset(center.x, center.y + armLength),
                        strokeWidth = 2f
                    )
                }

                // 2. Draw Calibration Points and Scale Line
                if (calibrationPoints.isNotEmpty()) {
                    for (cp in calibrationPoints) {
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 6f,
                            center = cp
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = cp,
                            style = Stroke(width = 1.5f)
                        )
                    }
                    if (calibrationPoints.size == 2) {
                        val p1 = calibrationPoints[0]
                        val p2 = calibrationPoints[1]
                        drawLine(
                            color = Color(0xFFF59E0B),
                            start = p1,
                            end = p2,
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }
                }

                // 3. Draw Grouping Bounding Circle & Extreme Spread Line (if >= 2 impacts)
                if (annotatedImpacts.size >= 2) {
                    var sumX = 0f
                    var sumY = 0f
                    for (impact in annotatedImpacts) {
                        sumX += impact.canvasOffset.x
                        sumY += impact.canvasOffset.y
                    }
                    val avgX = sumX / annotatedImpacts.size
                    val avgY = sumY / annotatedImpacts.size
                    val mpiOffset = Offset(avgX, avgY)

                    // Find max distance from MPI for grouping circle
                    var maxDist = 0f
                    for (impact in annotatedImpacts) {
                        val d = hypot(impact.canvasOffset.x - mpiOffset.x, impact.canvasOffset.y - mpiOffset.y)
                        if (d > maxDist) maxDist = d
                    }

                    // Dashed Grouping circle
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                        radius = maxDist + 8f,
                        center = mpiOffset,
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )

                    // Barycenter / MPI Cyan Marker
                    val mpiArm = 16f
                    drawCircle(
                        color = Color(0xFF0284C7),
                        radius = 4f,
                        center = mpiOffset
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(mpiOffset.x - mpiArm, mpiOffset.y),
                        end = Offset(mpiOffset.x + mpiArm, mpiOffset.y),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(mpiOffset.x, mpiOffset.y - mpiArm),
                        end = Offset(mpiOffset.x, mpiOffset.y + mpiArm),
                        strokeWidth = 2.5f
                    )
                }

                // 4. Draw Numbered Red Impacts
                val impactCircleRadius = 12f
                annotatedImpacts.forEachIndexed { idx, point ->
                    val pos = point.canvasOffset
                    val isLatest = idx == annotatedImpacts.lastIndex

                    // Drop shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = impactCircleRadius + 2f,
                        center = Offset(pos.x + 1f, pos.y + 1f)
                    )

                    // Red impact circle
                    drawCircle(
                        color = if (isLatest) Color(0xFFFF3333) else Color(0xFFDC2626),
                        radius = impactCircleRadius,
                        center = pos
                    )

                    // White rim
                    drawCircle(
                        color = Color.White,
                        radius = impactCircleRadius,
                        center = pos,
                        style = Stroke(width = 2f)
                    )

                    // Impact index text
                    val textLayout = textMeasurer.measure(
                        point.index.toString(),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
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

        Spacer(modifier = Modifier.height(10.dp))

        // Live Ballistics Statistics Card (Computed via BallisticsCalculator)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calculs balistiques en temps réel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", currentStats.moa)} MOA",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Écart Extrême (ES)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", currentStats.esMm)} mm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column {
                        Text("H × L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", currentStats.hMm)} × ${String.format(Locale.US, "%.1f", currentStats.lMm)} mm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("H + L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(Locale.US, "%.1f", currentStats.hPlusL)} mm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Barycentre (MPI) : ΔX = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaXMm)} mm, ΔY = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaYMm)} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
