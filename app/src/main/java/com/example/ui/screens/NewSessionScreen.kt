package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WeaponEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(
    weapons: List<WeaponEntity>,
    onBack: () -> Unit,
    onStartSession: (
        title: String,
        targetType: String,
        distance: Int,
        weapon: WeaponEntity?,
        ammoBrand: String,
        ammoCaliber: String,
        position: String,
        notes: String
    ) -> Unit
) {
    val targetTypes = listOf(
        "C50 (25m/50m)",
        "Pistolet 10m (ISSF)",
        "Carabine 10m (ISSF)",
        "C200 (200m)",
        "Cible Hunter",
        "IPSC Classic"
    )
    val distancePresets = listOf(10, 25, 50, 100, 200, 300)
    val positions = listOf("Debout", "Couché", "À genoux", "Sur appui")

    var selectedTargetType by remember { mutableStateOf(targetTypes[0]) }
    var selectedDistance by remember { mutableIntStateOf(25) }
    var selectedPosition by remember { mutableStateOf(positions[0]) }

    var selectedWeapon by remember { mutableStateOf<WeaponEntity?>(weapons.firstOrNull()) }
    var weaponDropdownExpanded by remember { mutableStateOf(false) }

    var ammoBrand by remember { mutableStateOf("") }
    var ammoCaliber by remember { mutableStateOf(selectedWeapon?.caliber ?: "") }
    var sessionTitle by remember { mutableStateOf("Séance ${targetTypes[0]} - 25m") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle séance de tir", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("new_session_form"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = sessionTitle,
                onValueChange = { sessionTitle = it },
                label = { Text("Titre de la séance") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("session_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Target Type selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Type de cible",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targetTypes.take(3).forEach { target ->
                            FilterChip(
                                selected = selectedTargetType == target,
                                onClick = {
                                    selectedTargetType = target
                                    if (target.contains("10m")) selectedDistance = 10
                                    else if (target.contains("200m")) selectedDistance = 200
                                },
                                label = { Text(target, maxLines = 1) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targetTypes.drop(3).forEach { target ->
                            FilterChip(
                                selected = selectedTargetType == target,
                                onClick = { selectedTargetType = target },
                                label = { Text(target, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // Distance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Distance du pas de tir : $selectedDistance m",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distancePresets.forEach { dist ->
                            FilterChip(
                                selected = selectedDistance == dist,
                                onClick = { selectedDistance = dist },
                                label = { Text("${dist}m") }
                            )
                        }
                    }
                }
            }

            // Firearm choice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Arme utilisée",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (weapons.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = weaponDropdownExpanded,
                            onExpandedChange = { weaponDropdownExpanded = !weaponDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedWeapon?.name ?: "Sélectionner une arme",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weaponDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = weaponDropdownExpanded,
                                onDismissRequest = { weaponDropdownExpanded = false }
                            ) {
                                weapons.forEach { weapon ->
                                    DropdownMenuItem(
                                        text = { Text("${weapon.name} (${weapon.caliber})") },
                                        onClick = {
                                            selectedWeapon = weapon
                                            ammoCaliber = weapon.caliber
                                            weaponDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Aucune arme enregistrée dans le coffre. Vous pourrez en ajouter une dans l'onglet Armes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ammoBrand,
                            onValueChange = { ammoBrand = it },
                            label = { Text("Munitions / Marque") },
                            placeholder = { Text("Ex: SK, Geco, Sellier") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = ammoCaliber,
                            onValueChange = { ammoCaliber = it },
                            label = { Text("Calibre") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Shooting Position
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Position de tir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        positions.forEach { pos ->
                            FilterChip(
                                selected = selectedPosition == pos,
                                onClick = { selectedPosition = pos },
                                label = { Text(pos) }
                            )
                        }
                    }
                }
            }

            // Notes / Environment
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & météo (vent, lumière, réglages...)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Start Session Button
            Button(
                onClick = {
                    onStartSession(
                        sessionTitle,
                        selectedTargetType,
                        selectedDistance,
                        selectedWeapon,
                        ammoBrand,
                        ammoCaliber,
                        selectedPosition,
                        notes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_create_session_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Passer au pas de tir (Cible)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
