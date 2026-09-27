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
    onFinishCalibration: () -> Unit,
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header with Reference Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Étalonnage par visuel cible",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }

                // Reference selection menu
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = !menuExpanded }
                ) {
                    OutlinedButton(
                        onClick = { menuExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text(
                            text = selectedReference.shortName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                    }
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        TargetCalibrationDefaults.references.forEach { ref ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(ref.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(ref.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }

            // Step Indicators & Guided Text
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = currentStep == CalibrationStep.SET_CENTER,
                            onClick = { onStepChange(CalibrationStep.SET_CENTER) },
                            label = { Text("1. Centre (vert)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = null,
                                    tint = if (isCenterPlaced) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = currentStep == CalibrationStep.CLICK_EDGE || currentStep == CalibrationStep.CALIBRATED,
                            onClick = { onStepChange(CalibrationStep.CLICK_EDGE) },
                            label = { Text("2. Bord (${selectedReference.diameterMm.toInt()} mm)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (pixelPerMm > 0f) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    // Instruction banner based on current step
                    when (currentStep) {
                        CalibrationStep.SET_CENTER -> {
                            Text(
                                text = "🎯 Touchez ou maintenez le doigt appuyé pour positionner précisément la croix verte au centre. (Défilement photo à 2 doigts)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        CalibrationStep.CLICK_EDGE -> {
                            Text(
                                text = "📏 Touchez ou glissez le doigt pour ajuster le diamètre du visuel (${selectedReference.diameterMm.toInt()} mm). Défilement photo à 2 doigts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        CalibrationStep.CALIBRATED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "✅ Étalonné : visuel ${selectedReference.diameterMm.toInt()} mm (${String.format(Locale.US, "%.2f", pixelPerMm)} px/mm)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Glissez le doigt sur l'anneau pour réajuster ou validez.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = onFinishCalibration,
                                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Passer aux impacts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
