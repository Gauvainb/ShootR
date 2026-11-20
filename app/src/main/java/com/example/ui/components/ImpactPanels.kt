package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ScreenImpact

@Composable
private fun NudgeButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(26.dp))
    }
}

/**
 * Correction of one impact, under the zoomed target: the photo stays visible while it moves.
 * The points follow from where the impact is; they are shown only on a target that is scored.
 */
@Composable
fun ImpactEditPanel(
    shot: ScreenImpact,
    scored: Boolean,
    onNudge: (dxMm: Float, dyMm: Float) -> Unit,
    onFlyerChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val scoreColor = when {
        shot.isInnerTen -> Color(0xFF10B981)
        shot.score >= 9 -> Color(0xFFFBBF24)
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("impact_edit_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Coup n° ${shot.index}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = if (scored) "Points selon la zone touchée" else "Cible sans points : seul le groupement est calculé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (scored) {
                    Text(if (shot.isInnerTen) "10X" else "${shot.score}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = scoreColor)
                }
            }

            // Fine nudge, 1 mm per press
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                NudgeButton(Icons.AutoMirrored.Filled.ArrowBack, "Gauche de 1 mm") { onNudge(-1f, 0f) }
                NudgeButton(Icons.Default.ArrowUpward, "Haut de 1 mm") { onNudge(0f, 1f) }
                NudgeButton(Icons.Default.ArrowDownward, "Bas de 1 mm") { onNudge(0f, -1f) }
                NudgeButton(Icons.AutoMirrored.Filled.ArrowForward, "Droite de 1 mm") { onNudge(1f, 0f) }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Coup de doigt (hors groupement)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = shot.isFlyer, onCheckedChange = onFlyerChange)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.size(ActionHeight),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer cet impact", modifier = Modifier.size(24.dp))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(ActionHeight),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Annuler", fontSize = 15.sp)
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f).height(ActionHeight).testTag("impact_edit_done"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Terminé", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Every shot at a glance, without scrolling sideways: a compact table of "n° | points" pairs,
 * [columns] per row. A cell opens the correction of its shot. On a target without points the
 * second column of a cell is a dash.
 */
@Composable
fun ShotTable(shots: List<ScreenImpact>, scored: Boolean, onSelect: (ScreenImpact) -> Unit, columns: Int = 3) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val cellText = MaterialTheme.typography.bodyMedium
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, line),
        modifier = Modifier.fillMaxWidth().testTag("shot_table")
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(columns) { c ->
                    if (c > 0) Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(line))
                    Row(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("N°", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                        Text("Points", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            shots.chunked(columns).forEach { row ->
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(line))
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    repeat(columns) { c ->
                        if (c > 0) Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(line))
                        val shot = row.getOrNull(c)
                        if (shot == null) {
                            Spacer(modifier = Modifier.weight(1f).height(40.dp))
                        } else {
                            val scoreColor = when {
                                shot.isFlyer -> MaterialTheme.colorScheme.outline
                                shot.isInnerTen -> Color(0xFF10B981)
                                shot.score >= 9 -> Color(0xFFFBBF24)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { onSelect(shot) }
                                    .padding(horizontal = 10.dp)
                                    .testTag("shot_chip_${shot.index}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${shot.index}", style = cellText, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                                Text(
                                    text = when {
                                        !scored -> "—"
                                        shot.isInnerTen -> "10X"
                                        else -> "${shot.score} pts"
                                    },
                                    style = cellText,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scoreColor,
                                    textDecoration = if (shot.isFlyer) TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                if (shot.isAuto) {
                                    // Detected automatically, not checked yet (circled in blue on the target).
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
