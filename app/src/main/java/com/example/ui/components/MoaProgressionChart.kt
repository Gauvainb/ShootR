package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import java.util.Locale
import kotlin.math.max

enum class ChartDisplayType {
    LINE,
    HISTOGRAM
}

data class SessionChartData(
    val id: Long,
    val date: String,
    val weapon: String,
    val caliber: String,
    val ammo: String,
    val distanceMeters: Float,
    val moa: Float,
    val esMm: Float,
    val shotsCount: Int
)

@Composable
fun MoaProgressionChart(
    modifier: Modifier = Modifier,
    sessions: List<SessionChartData>,
    selectedWeapon: String,
    chartType: ChartDisplayType = ChartDisplayType.LINE
) {
    val textMeasurer = rememberTextMeasurer()

    // Filter sessions by selected weapon if not "Toutes"
    val filteredSessions = remember(sessions, selectedWeapon) {
        val list = if (selectedWeapon == "Toutes" || selectedWeapon.isBlank()) {
            sessions
        } else {
            sessions.filter { it.weapon.equals(selectedWeapon, ignoreCase = true) }
        }
        // Chronological order (oldest to newest for evolution)
        list.sortedBy { it.id }
    }

    var selectedSessionIndex by remember(filteredSessions) {
        mutableIntStateOf(if (filteredSessions.isNotEmpty()) filteredSessions.lastIndex else -1)
    }

    if (filteredSessions.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucune séance pour « $selectedWeapon »",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Enregistrez plusieurs tirs pour visualiser l'évolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        return
    }

    val bestMoa = remember(filteredSessions) { filteredSessions.minOf { it.moa } }
    val avgMoa = remember(filteredSessions) { filteredSessions.map { it.moa }.average().toFloat() }
    val latestMoa = filteredSessions.last().moa

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("moa_progression_chart_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Stat Pills Header (Best MOA, Moyenne, Dernier tir)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Meilleur", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.2f", bestMoa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                        Text(" MOA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Moyenne", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.2f", avgMoa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(" MOA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Dernier tir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.2f", latestMoa),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (latestMoa <= 1.0f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                        Text(" MOA", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Main Chart Canvas Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(filteredSessions, chartType) {
                            detectTapGestures { tapOffset ->
                                val leftPadding = 40f
                                val rightPadding = 20f
                                val availableWidth = size.width - leftPadding - rightPadding

                                if (filteredSessions.size == 1) {
                                    selectedSessionIndex = 0
                                } else {
                                    val stepX = availableWidth / (filteredSessions.size - 1)
                                    val tappedIndex = ((tapOffset.x - leftPadding + stepX / 2f) / stepX)
                                        .toInt()
                                        .coerceIn(0, filteredSessions.lastIndex)
                                    selectedSessionIndex = tappedIndex
                                }
                            }
                        }
                ) {
                    val leftPadding = 45f
                    val rightPadding = 25f
                    val topPadding = 25f
                    val bottomPadding = 35f

                    val chartWidth = size.width - leftPadding - rightPadding
                    val chartHeight = size.height - topPadding - bottomPadding

                    // Determine max MOA for scaling (at least 2.0 MOA for benchmark scale)
                    val rawMaxMoa = filteredSessions.maxOfOrNull { it.moa } ?: 1.5f
                    val maxMoaScale = max(2.0f, rawMaxMoa * 1.25f)

                    // 1. Draw Grid Lines and Y-Axis Labels
                    val yGridSteps = 4
                    val labelStyle = TextStyle(
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    for (i in 0..yGridSteps) {
                        val moaVal = (maxMoaScale / yGridSteps) * i
                        val yPos = topPadding + chartHeight * (1f - (moaVal / maxMoaScale))

                        // Grid line
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.5f),
                            start = Offset(leftPadding, yPos),
                            end = Offset(size.width - rightPadding, yPos),
                            strokeWidth = 1f
                        )

                        // Y-axis label
                        val text = String.format(Locale.US, "%.1f", moaVal)
                        val layout = textMeasurer.measure(text, style = labelStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(leftPadding - layout.size.width - 6f, yPos - layout.size.height / 2f)
                        )
                    }

                    // 2. Sub-MOA Reference Line (1.0 MOA Standard)
                    if (maxMoaScale >= 1.0f) {
                        val subMoaY = topPadding + chartHeight * (1f - (1.0f / maxMoaScale))
                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.8f),
                            start = Offset(leftPadding, subMoaY),
                            end = Offset(size.width - rightPadding, subMoaY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        val subMoaBadge = textMeasurer.measure(
                            "1 MOA",
                            style = TextStyle(color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                        drawText(
                            textLayoutResult = subMoaBadge,
                            topLeft = Offset(size.width - rightPadding - subMoaBadge.size.width, subMoaY - subMoaBadge.size.height - 2f)
                        )
                    }

                    // 3. Render Chart according to ChartDisplayType
                    if (chartType == ChartDisplayType.LINE) {
                        // LINE CHART
                        val points = mutableListOf<Offset>()

                        filteredSessions.forEachIndexed { index, session ->
                            val xPos = if (filteredSessions.size == 1) {
                                leftPadding + chartWidth / 2f
                            } else {
                                leftPadding + (chartWidth / (filteredSessions.size - 1)) * index
                            }
                            val yPos = topPadding + chartHeight * (1f - (session.moa / maxMoaScale).coerceIn(0f, 1f))
                            points.add(Offset(xPos, yPos))
                        }

                        // Gradient fill path under curve
                        if (points.size > 1) {
                            val fillPath = Path().apply {
                                moveTo(points.first().x, topPadding + chartHeight)
                                points.forEach { lineTo(it.x, it.y) }
                                lineTo(points.last().x, topPadding + chartHeight)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFF4D4D).copy(alpha = 0.35f),
                                        Color(0xFFFF4D4D).copy(alpha = 0.02f)
                                    ),
                                    startY = topPadding,
                                    endY = topPadding + chartHeight
                                )
                            )

                            // Stroke line path
                            val strokePath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }

                            drawPath(
                                path = strokePath,
                                color = Color(0xFFFF4D4D),
                                style = Stroke(width = 3f)
                            )
                        }

                        // Draw points and values
                        points.forEachIndexed { index, pt ->
                            val isSelected = index == selectedSessionIndex
                            val session = filteredSessions[index]
                            val isSubMoa = session.moa <= 1.0f

                            // Outer glow if selected
                            if (isSelected) {
                                drawCircle(
                                    color = Color(0xFFFF4D4D).copy(alpha = 0.3f),
                                    radius = 12f,
                                    center = pt
                                )
                            }

                            // Point circle
                            val pointColor = if (isSubMoa) Color(0xFF10B981) else Color(0xFFFF4D4D)
                            drawCircle(color = pointColor, radius = if (isSelected) 6f else 4.5f, center = pt)
                            drawCircle(color = Color.White, radius = if (isSelected) 6f else 4.5f, center = pt, style = Stroke(width = 1.5f))

                            // X-axis session label (#1, #2...)
                            val xLabel = "#${index + 1}"
                            val xLayout = textMeasurer.measure(
                                xLabel,
                                style = TextStyle(
                                    color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            drawText(
                                textLayoutResult = xLayout,
                                topLeft = Offset(pt.x - xLayout.size.width / 2f, topPadding + chartHeight + 8f)
                            )
                        }
                    } else {
                        // HISTOGRAM / BAR CHART
                        val barCount = filteredSessions.size
                        val slotWidth = chartWidth / barCount
                        val barWidth = (slotWidth * 0.55f).coerceIn(12f, 40f)

                        filteredSessions.forEachIndexed { index, session ->
                            val isSelected = index == selectedSessionIndex
                            val centerX = leftPadding + slotWidth * index + slotWidth / 2f
                            val barHeight = chartHeight * (session.moa / maxMoaScale).coerceIn(0f, 1f)
                            val topY = topPadding + chartHeight - barHeight

                            val barColor = when {
                                session.moa <= 1.0f -> Color(0xFF10B981)
                                session.moa <= 2.0f -> Color(0xFFFBBF24)
                                else -> Color(0xFFFF4D4D)
                            }

                            // Draw rounded bar
                            drawRoundRect(
                                color = if (isSelected) barColor else barColor.copy(alpha = 0.75f),
                                topLeft = Offset(centerX - barWidth / 2f, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            if (isSelected) {
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(centerX - barWidth / 2f, topY),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(6f, 6f),
                                    style = Stroke(width = 2f)
                                )
                            }

                            // Value on top of bar
                            val valText = String.format(Locale.US, "%.1f", session.moa)
                            val valLayout = textMeasurer.measure(
                                valText,
                                style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = valLayout,
                                topLeft = Offset(centerX - valLayout.size.width / 2f, topY - valLayout.size.height - 2f)
                            )

                            // X label below bar
                            val xLabel = "#${index + 1}"
                            val xLayout = textMeasurer.measure(
                                xLabel,
                                style = TextStyle(
                                    color = if (isSelected) Color(0xFFF1F5F9) else Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            drawText(
                                textLayoutResult = xLayout,
                                topLeft = Offset(centerX - xLayout.size.width / 2f, topPadding + chartHeight + 8f)
                            )
                        }
                    }
                }
            }
        }

        // Interactive Selected Session Card (Tooltip detail on tap)
        if (selectedSessionIndex in filteredSessions.indices) {
            val selected = filteredSessions[selectedSessionIndex]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "Séance #${selectedSessionIndex + 1}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selected.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${selected.weapon} (${selected.caliber}) • ${selected.distanceMeters.toInt()}m",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (selected.ammo.isNotBlank()) {
                            Text(
                                text = "Munition : ${selected.ammo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", selected.moa)} MOA",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected.moa <= 1.0f) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ES: ${String.format(Locale.US, "%.1f", selected.esMm)} mm",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
