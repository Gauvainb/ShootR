package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CalibrationReference
import com.example.domain.TargetCalibrationDefaults
import java.util.Locale

enum class CalibrationStep {
    SET_CENTER,
    CLICK_EDGE,
    CALIBRATED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationAssistantPanel(
    selectedReference: CalibrationReference,
    onSelectReference: (CalibrationReference) -> Unit,
    currentStep: CalibrationStep,
    onStepChange: (CalibrationStep) -> Unit,
    pixelPerMm: Float,
    isCenterPlaced: Boolean,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customDiameterInput by remember { mutableStateOf("100") }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Diamètre de référence personnalisé") },
            text = {
                Column {
                    Text(
                        "Indiquez le diamètre réel connu (en mm) de l'anneau ou du visuel sur lequel vous allez cliquer :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customDiameterInput,
                        onValueChange = { customDiameterInput = it },
                        label = { Text("Diamètre (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val diam = customDiameterInput.toFloatOrNull() ?: 100f
                    if (diam > 0f) {
                        val customRef = CalibrationReference(
                            id = "custom_${diam.toInt()}",
                            targetType = "Personnalisé",
                            label = "Personnalisé • ${diam.toInt()} mm",
                            shortName = "${diam.toInt()} mm",
                            diameterMm = diam,
                            description = "Repère personnalisé de $diam mm"
                        )
                        onSelectReference(customRef)
                    }
                    showCustomDialog = false
                }) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Réglage manuel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // What the edge of the ring is measured against
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = !menuExpanded }
            ) {
                OutlinedButton(
                    onClick = { menuExpanded = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).menuAnchor()
                ) {
                    Icon(imageVector = Icons.Default.Straighten, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Repère : ${selectedReference.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                }
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    TargetCalibrationDefaults.references.forEach { ref ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(ref.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                    Text(ref.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                if (ref.id == "custom") {
                                    showCustomDialog = true
                                } else {
                                    onSelectReference(ref)
                                }
                            }
                        )
                    }
                }
            }

            // Which handle the finger moves
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalibrationStepButton(
                    label = "Centre",
                    icon = Icons.Default.CenterFocusStrong,
                    color = Color(0xFF10B981),
                    selected = currentStep == CalibrationStep.SET_CENTER,
                    done = isCenterPlaced,
                    onClick = { onStepChange(CalibrationStep.SET_CENTER) },
                    modifier = Modifier.weight(1f)
                )
                CalibrationStepButton(
                    label = "Bord",
                    icon = Icons.Default.RadioButtonUnchecked,
                    color = Color(0xFFFBBF24),
                    selected = currentStep != CalibrationStep.SET_CENTER,
                    done = pixelPerMm > 0f,
                    onClick = { onStepChange(CalibrationStep.CLICK_EDGE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = when (currentStep) {
                    CalibrationStep.SET_CENTER -> "Touchez le centre de la cible ou faites glisser la croix verte."
                    CalibrationStep.CLICK_EDGE -> "Faites glisser jusqu'au bord du ${selectedReference.shortName.lowercase()} (${formatMm(selectedReference.diameterMm)} mm)."
                    CalibrationStep.CALIBRATED -> "Échelle réglée. Faites glisser la poignée jaune si le cercle ne suit pas le bord du visuel."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Deux doigts pour zoomer et déplacer la photo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMm(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.FRANCE, "%.1f", value)

@Composable
private fun CalibrationStepButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    selected: Boolean,
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) color.copy(alpha = 0.18f) else Color.Transparent
        )
    ) {
        Icon(
            imageVector = if (done && !selected) Icons.Default.Check else icon,
            contentDescription = null,
            tint = if (selected || done) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
