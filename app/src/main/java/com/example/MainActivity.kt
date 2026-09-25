package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.domain.Impact
import com.example.ui.components.ChartDisplayType
import com.example.ui.components.MoaProgressionChart
import com.example.ui.components.SessionChartData
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

// Tactical Dark Theme
private val TacticalDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF4D4D),           // Safety Red
    onPrimary = Color(0xFF380004),
    primaryContainer = Color(0xFF68000C),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFBBF24),         // Tactical Gold
    onSecondary = Color(0xFF261900),
    background = Color(0xFF0F141C),        // Charcoal Black
    surface = Color(0xFF161E29),           // Anthracite Card
    surfaceVariant = Color(0xFF212B3B),    // Elevated Panel
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

enum class AnnotationMode {
    CALIBRATE,
    TARGET_CENTER,
    IMPACT
}

data class ScreenImpact(
    val index: Int,
    val canvasOffset: Offset,
    val realMm: Impact
)

data class Weapon(
    val id: Long,
    val name: String,
    val type: String,
    val caliber: String,
    val optics: String = "",
    val notes: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = TacticalDarkColorScheme) {
                TirTrackerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TirTrackerApp() {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // 1. Photo selection state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // 2. Caractérisation de la séance en champs libres
    var weaponNameInput by remember { mutableStateOf("CZ 457 Varmint") }
    var weaponCaliberInput by remember { mutableStateOf(".22 LR") }
    var distanceInput by remember { mutableStateOf("50") }
    var ammoInput by remember { mutableStateOf("SK Rifle Match 40gr") }
    var notesInput by remember { mutableStateOf("") }

    val distanceMeters = remember(distanceInput) {
        distanceInput.toFloatOrNull() ?: 50f
    }

    // 3. Bibliothèque d'armes
    val weaponsList = remember { mutableStateListOf<Weapon>() }
    var showWeaponsDialog by remember { mutableStateOf(false) }
    var showAddWeaponDialog by remember { mutableStateOf(false) }

    fun loadWeapons(): List<Weapon> {
        return try {
            val file = File(context.filesDir, "weapons_library.json")
            if (!file.exists()) {
                listOf(
                    Weapon(1L, "CZ 457 Varmint", "Carabine", ".22 LR", "Lunette Vortex 4-12x40"),
                    Weapon(2L, "Glock 17 Gen 5", "Pistolet", "9x19 mm", "Visée d'origine 3 points"),
                    Weapon(3L, "AR-15 DDM4 V7", "Fusil d'assaut", "5.56x45 mm / .223 Rem", "Point rouge EOTech EXPS2"),
                    Weapon(4L, "Tikka T3x TAC A1", "Fusil de précision", ".308 Win", "Lunette Schmidt & Bender 5-25x56"),
                    Weapon(5L, "Smith & Wesson 686", "Revolver", ".357 Magnum", "Hausse réglable")
                )
            } else {
                val array = JSONArray(file.readText())
                val list = mutableListOf<Weapon>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Weapon(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            name = obj.optString("name", "Arme"),
                            type = obj.optString("type", "Pistolet"),
                            caliber = obj.optString("caliber", "9x19 mm"),
                            optics = obj.optString("optics", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveWeapons(list: List<Weapon>) {
        try {
            val file = File(context.filesDir, "weapons_library.json")
            val array = JSONArray()
            list.forEach { w ->
                val obj = JSONObject()
                obj.put("id", w.id)
                obj.put("name", w.name)
                obj.put("type", w.type)
                obj.put("caliber", w.caliber)
                obj.put("optics", w.optics)
                obj.put("notes", w.notes)
                array.put(obj)
            }
            file.writeText(array.toString(2))
        } catch (_: Exception) {}
    }

    remember {
        val loaded = loadWeapons()
        weaponsList.addAll(loaded)
        if (!File(context.filesDir, "weapons_library.json").exists()) {
            saveWeapons(loaded)
        }
        true
    }

    // 4. Canvas interaction state
    var activeMode by remember { mutableStateOf(AnnotationMode.IMPACT) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val calibrationPoints = remember { mutableStateListOf<Offset>() }
    var pixelPerMm by remember { mutableFloatStateOf(0f) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var calibrationDistanceMmInput by remember { mutableStateOf("50") }

    var targetCenterPx by remember { mutableStateOf<Offset?>(null) }
    val impactsList = remember { mutableStateListOf<ScreenImpact>() }

    // 5. History & Stats Sheet state
    var showHistorySheet by remember { mutableStateOf(false) }
    var showChartDialog by remember { mutableStateOf(false) }
    var savedSessionsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var chartWeaponFilter by remember { mutableStateOf("Toutes") }
    var chartDisplayType by remember { mutableStateOf(ChartDisplayType.LINE) }

    val currentStats by remember(impactsList.size, distanceMeters, pixelPerMm, targetCenterPx) {
        derivedStateOf {
            val domainImpacts = impactsList.map { it.realMm }
            BallisticsCalculator.calculateStats(
                impacts = domainImpacts,
                targetCenter = Impact(0f, 0f),
                distanceMeters = distanceMeters
            )
        }
    }

    fun loadHistory(): List<JSONObject> {
        return try {
            val file = File(context.filesDir, "sessions_history.json")
            if (!file.exists()) {
                // Seed initial historical sessions if none exist for instant MOA chart experience
                val sampleArray = JSONArray()
                val now = System.currentTimeMillis()
                val samples = listOf(
                    Triple(now - 86400000L * 4, 1.45f, 21.1f),
                    Triple(now - 86400000L * 3, 1.20f, 17.5f),
                    Triple(now - 86400000L * 2, 0.95f, 13.8f),
                    Triple(now - 86400000L * 1, 0.82f, 11.9f)
                )
                samples.forEachIndexed { idx, (time, moaVal, esVal) ->
                    val obj = JSONObject()
                    obj.put("id", time)
                    obj.put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(time)))
                    obj.put("weapon", "CZ 457 Varmint")
                    obj.put("caliber", ".22 LR")
                    obj.put("distanceMeters", 50.0)
                    obj.put("ammo", "SK Rifle Match 40gr")
                    obj.put("shotsCount", 5)
                    obj.put("esMm", esVal.toDouble())
                    obj.put("hMm", (esVal * 0.8).toDouble())
                    obj.put("lMm", (esVal * 0.7).toDouble())
                    obj.put("hPlusL", (esVal * 1.5).toDouble())
                    obj.put("moa", moaVal.toDouble())
                    sampleArray.put(obj)
                }
                file.writeText(sampleArray.toString(2))
                val list = mutableListOf<JSONObject>()
                for (i in 0 until sampleArray.length()) list.add(sampleArray.getJSONObject(i))
                list.reversed()
            } else {
                val array = JSONArray(file.readText())
                val list = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i))
                }
                list.reversed()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSessionToJson() {
        if (impactsList.isEmpty()) {
            Toast.makeText(context, "Ajoutez au moins un impact avant d'enregistrer.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = File(context.filesDir, "sessions_history.json")
            val array = if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) JSONArray(content) else JSONArray()
            } else {
                JSONArray()
            }

            val sessionObj = JSONObject()
            val timestamp = System.currentTimeMillis()
            sessionObj.put("id", timestamp)
            sessionObj.put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(timestamp)))
            sessionObj.put("weapon", weaponNameInput.ifBlank { "Arme libre" })
            sessionObj.put("caliber", weaponCaliberInput.ifBlank { "N/A" })
            sessionObj.put("distanceMeters", distanceMeters)
            sessionObj.put("ammo", ammoInput.ifBlank { "Munition libre" })
            sessionObj.put("notes", notesInput)
            sessionObj.put("imageUri", selectedImageUri?.toString() ?: "")
            sessionObj.put("shotsCount", impactsList.size)
            sessionObj.put("esMm", currentStats.esMm)
            sessionObj.put("hMm", currentStats.hMm)
            sessionObj.put("lMm", currentStats.lMm)
            sessionObj.put("hPlusL", currentStats.hPlusL)
            sessionObj.put("moa", currentStats.moa)
            sessionObj.put("mpiDeltaXMm", currentStats.mpiDeltaXMm)
            sessionObj.put("mpiDeltaYMm", currentStats.mpiDeltaYMm)

            val impactsArr = JSONArray()
            impactsList.forEach { imp ->
                val impObj = JSONObject()
                impObj.put("index", imp.index)
                impObj.put("xMm", imp.realMm.xMm)
                impObj.put("yMm", imp.realMm.yMm)
                impactsArr.put(impObj)
            }
            sessionObj.put("impacts", impactsArr)

            array.put(sessionObj)
            file.writeText(array.toString(2))
            Toast.makeText(context, "Séance enregistrée avec succès !", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Dialog: Calibration distance
    if (showCalibrationDialog && calibrationPoints.size == 2) {
        val p1 = calibrationPoints[0]
        val p2 = calibrationPoints[1]
        val pixelDist = hypot(p1.x - p2.x, p1.y - p2.y)

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Étalonnage de l'échelle", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Distance mesurée : ${String.format(Locale.US, "%.1f", pixelDist)} px")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Indiquez la distance réelle connue entre ces deux points en millimètres (ex: 200 mm pour visuel C50, 50 mm, etc.) :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = calibrationDistanceMmInput,
                        onValueChange = { calibrationDistanceMmInput = it },
                        label = { Text("Distance réelle (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("25", "50", "100", "200").forEach { dist ->
                            OutlinedButton(
                                onClick = { calibrationDistanceMmInput = dist },
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text("${dist}mm", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val realMm = calibrationDistanceMmInput.toFloatOrNull() ?: 50f
                        if (realMm > 0f && pixelDist > 0f) {
                            pixelPerMm = pixelDist / realMm
                            val center = targetCenterPx ?: Offset(500f, 500f)
                            val updated = impactsList.map { p ->
                                val xMm = (p.canvasOffset.x - center.x) / pixelPerMm
                                val yMm = (center.y - p.canvasOffset.y) / pixelPerMm
                                p.copy(realMm = Impact(xMm, yMm))
                            }
                            impactsList.clear()
                            impactsList.addAll(updated)
                            activeMode = AnnotationMode.IMPACT
                            Toast.makeText(context, "Échelle définie : ${String.format(Locale.US, "%.2f", pixelPerMm)} px/mm", Toast.LENGTH_SHORT).show()
                        }
                        showCalibrationDialog = false
                    }
                ) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = {
                    calibrationPoints.clear()
                    showCalibrationDialog = false
                }) { Text("Annuler") }
            }
        )
    }

    // Dialog: Bibliothèque d'armes (Armory Dialog)
    if (showWeaponsDialog) {
        AlertDialog(
            onDismissRequest = { showWeaponsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bibliothèque d'armes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = { showAddWeaponDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Ajouter arme", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    Text(
                        text = "Sélectionnez une arme pour pré-remplir la séance ou gérez votre coffre :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (weaponsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucune arme enregistrée.", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(weaponsList) { weapon ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            weaponNameInput = weapon.name
                                            weaponCaliberInput = weapon.caliber
                                            showWeaponsDialog = false
                                            Toast.makeText(context, "Arme sélectionnée : ${weapon.name}", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(weapon.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        weapon.type,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surface
                                                ) {
                                                    Text(
                                                        weapon.caliber,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                weaponsList.remove(weapon)
                                                saveWeapons(weaponsList.toList())
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showWeaponsDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Dialog: Ajouter une arme à la bibliothèque
    if (showAddWeaponDialog) {
        val weaponTypes = listOf("Pistolet", "Carabine", "Fusil d'assaut", "Fusil de précision", "Revolver", "Fusil à pompe", "Air comprimé")
        val caliberSuggestions = listOf("9x19 mm", ".22 LR", "5.56x45 mm / .223 Rem", ".308 Win", "7.62x39 mm", ".38 Special", ".357 Magnum", ".45 ACP", "Calibre 12", "6.5 Creedmoor", "4.5 mm")

        var newName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(weaponTypes[0]) }
        var newCaliber by remember { mutableStateOf("9x19 mm") }
        var newOptics by remember { mutableStateOf("") }
        var typeDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddWeaponDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Ajouter une arme au coffre", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nom / Modèle (ex: Glock 17, AR-15...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type d'arme") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            weaponTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newCaliber,
                        onValueChange = { newCaliber = it },
                        label = { Text("Calibre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Suggestions de calibres :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(caliberSuggestions) { cal ->
                            FilterChip(
                                selected = newCaliber == cal,
                                onClick = { newCaliber = cal },
                                label = { Text(cal, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newOptics,
                        onValueChange = { newOptics = it },
                        label = { Text("Visée / Optique (facultatif)") },
                        placeholder = { Text("Ex: Lunette 6-24x50, Point rouge") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val newWeapon = Weapon(
                                id = System.currentTimeMillis(),
                                name = newName.trim(),
                                type = selectedType,
                                caliber = newCaliber.trim(),
                                optics = newOptics.trim()
                            )
                            weaponsList.add(newWeapon)
                            saveWeapons(weaponsList.toList())
                            showAddWeaponDialog = false
                            Toast.makeText(context, "Arme ajoutée au coffre !", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Veuillez entrer un nom d'arme.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeaponDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog: Graphique d'évolution MOA (MOA Progression Chart Modal)
    if (showChartDialog) {
        val historyList = remember { loadHistory() }
        val chartDataList = remember(historyList) {
            historyList.map { obj ->
                SessionChartData(
                    id = obj.optLong("id", 0L),
                    date = obj.optString("date", ""),
                    weapon = obj.optString("weapon", "Arme"),
                    caliber = obj.optString("caliber", ""),
                    ammo = obj.optString("ammo", ""),
                    distanceMeters = obj.optDouble("distanceMeters", 25.0).toFloat(),
                    moa = obj.optDouble("moa", 0.0).toFloat(),
                    esMm = obj.optDouble("esMm", 0.0).toFloat(),
                    shotsCount = obj.optInt("shotsCount", 0)
                )
            }
        }

        // Available weapons from history
        val availableWeapons = remember(chartDataList) {
            listOf("Toutes") + chartDataList.map { it.weapon }.distinct()
        }

        AlertDialog(
            onDismissRequest = { showChartDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Évolution de la précision (MOA)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Type of Chart Toggle (Line vs Bar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type de vue :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = chartDisplayType == ChartDisplayType.LINE,
                                onClick = { chartDisplayType = ChartDisplayType.LINE },
                                label = { Text("Courbe", fontSize = 12.sp) },
                                leadingIcon = { Icon(imageVector = Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = chartDisplayType == ChartDisplayType.HISTOGRAM,
                                onClick = { chartDisplayType = ChartDisplayType.HISTOGRAM },
                                label = { Text("Histogramme", fontSize = 12.sp) },
                                leadingIcon = { Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    // Weapon Filter Chips
                    Text("Filtrer par arme :", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableWeapons) { w ->
                            FilterChip(
                                selected = chartWeaponFilter == w,
                                onClick = { chartWeaponFilter = w },
                                label = { Text(w, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // The Custom Native MoaProgressionChart
                    MoaProgressionChart(
                        sessions = chartDataList,
                        selectedWeapon = chartWeaponFilter,
                        chartType = chartDisplayType
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showChartDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Modal Bottom Sheet: Sessions History
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Historique des séances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { showChartDialog = true }) {
                            Icon(imageVector = Icons.Default.AutoGraph, contentDescription = "Graphique MOA", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(
                            onClick = {
                                val file = File(context.filesDir, "sessions_history.json")
                                if (file.exists()) file.delete()
                                savedSessionsList = emptyList()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (savedSessionsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune séance enregistrée.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(savedSessionsList) { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = s.optString("date", "Séance"),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.2f", s.optDouble("moa", 0.0))} MOA",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${s.optString("weapon", "Arme")} (${s.optString("caliber", "")}) • ${s.optDouble("distanceMeters", 25.0)} m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (s.optString("ammo").isNotBlank()) {
                                        Text(
                                            text = "Munitions : ${s.optString("ammo")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ES : ${String.format(Locale.US, "%.1f", s.optDouble("esMm", 0.0))} mm | H+L : ${String.format(Locale.US, "%.1f", s.optDouble("hPlusL", 0.0))} mm (${s.optInt("shotsCount", 0)} coups)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CrisisAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tir Tracker", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    // Graphique MOA
                    IconButton(
                        onClick = { showChartDialog = true }
                    ) {
                        Icon(imageVector = Icons.Default.AutoGraph, contentDescription = "Graphique MOA", tint = MaterialTheme.colorScheme.secondary)
                    }
                    // Bibliothèque d'armes
                    IconButton(
                        onClick = { showWeaponsDialog = true }
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Bibliothèque d'armes", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Historique des séances
                    IconButton(
                        onClick = {
                            savedSessionsList = loadHistory()
                            showHistorySheet = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Historique", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("main_screen_container"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. CARACTÉRISATION DE LA SÉANCE (Champs libres : Arme, Calibre, Distance, Munitions)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("session_characterization_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paramètres de la séance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showWeaponsDialog = true },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bibliothèque (${weaponsList.size})", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weaponNameInput,
                            onValueChange = { weaponNameInput = it },
                            label = { Text("Arme (champ libre)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        OutlinedTextField(
                            value = weaponCaliberInput,
                            onValueChange = { weaponCaliberInput = it },
                            label = { Text("Calibre") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = { distanceInput = it },
                            label = { Text("Distance (m)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        OutlinedTextField(
                            value = ammoInput,
                            onValueChange = { ammoInput = it },
                            label = { Text("Munitions (marque, grain...)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("10", "25", "50", "100", "200", "300").forEach { dist ->
                            FilterChip(
                                selected = distanceInput == dist,
                                onClick = { distanceInput = dist },
                                label = { Text("${dist}m", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            // 2. MODES DE POINTAGE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeMode == AnnotationMode.CALIBRATE,
                        onClick = { activeMode = AnnotationMode.CALIBRATE },
                        label = { Text("Calibrer", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Straighten, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f), selectedLabelColor = Color(0xFFFBBF24))
                    )
                    FilterChip(
                        selected = activeMode == AnnotationMode.TARGET_CENTER,
                        onClick = { activeMode = AnnotationMode.TARGET_CENTER },
                        label = { Text("Centre", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f), selectedLabelColor = Color(0xFF34D399))
                    )
                    FilterChip(
                        selected = activeMode == AnnotationMode.IMPACT,
                        onClick = { activeMode = AnnotationMode.IMPACT },
                        label = { Text("Impact", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.AdsClick, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.25f), selectedLabelColor = Color(0xFFFF6B6B))
                    )
                }
            }

            // 3. PHOTO LOADER & CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.height(44.dp).testTag("load_photo_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (selectedImageUri != null) "Changer photo" else "Charger photo cible", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset zoom", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { if (impactsList.isNotEmpty()) impactsList.removeAt(impactsList.lastIndex) },
                        enabled = impactsList.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (impactsList.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 4. CANEVAS AVEC PHOTO ET OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131821))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                            offset += pan
                        }
                    }
                    .pointerInput(activeMode, pixelPerMm, targetCenterPx) {
                        detectTapGestures { tapPos ->
                            val unscaledX = (tapPos.x - offset.x) / scale
                            val unscaledY = (tapPos.y - offset.y) / scale
                            val point = Offset(unscaledX, unscaledY)

                            when (activeMode) {
                                AnnotationMode.CALIBRATE -> {
                                    if (calibrationPoints.size < 2) {
                                        calibrationPoints.add(point)
                                        if (calibrationPoints.size == 2) showCalibrationDialog = true
                                    } else {
                                        calibrationPoints.clear()
                                        calibrationPoints.add(point)
                                    }
                                }
                                AnnotationMode.TARGET_CENTER -> {
                                    targetCenterPx = point
                                    val pxPerMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                    val updated = impactsList.map { imp ->
                                        val xMm = (imp.canvasOffset.x - point.x) / pxPerMm
                                        val yMm = (point.y - imp.canvasOffset.y) / pxPerMm
                                        imp.copy(realMm = Impact(xMm, yMm))
                                    }
                                    impactsList.clear()
                                    impactsList.addAll(updated)
                                }
                                AnnotationMode.IMPACT -> {
                                    val center = targetCenterPx ?: Offset(500f, 500f)
                                    val pxPerMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                    val xMm = (point.x - center.x) / pxPerMm
                                    val yMm = (center.y - point.y) / pxPerMm

                                    impactsList.add(
                                        ScreenImpact(
                                            index = impactsList.size + 1,
                                            canvasOffset = point,
                                            realMm = Impact(xMm, yMm)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    .testTag("target_photo_canvas"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(selectedImageUri).crossfade(true).build(),
                        contentDescription = "Cible de tir",
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Touchez « Charger photo cible » pour annoter une cible", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Ou touchez l'écran directement pour enregistrer les impacts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

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
                    if (targetCenterPx == null) {
                        targetCenterPx = Offset(size.width / 2f, size.height / 2f)
                    }

                    targetCenterPx?.let { c ->
                        val r = 16f
                        drawCircle(color = Color(0xFF10B981), radius = r, center = c, style = Stroke(width = 2.5f))
                        drawCircle(color = Color(0xFF10B981), radius = 3f, center = c)
                        val arm = 26f
                        drawLine(color = Color(0xFF10B981), start = Offset(c.x - arm, c.y), end = Offset(c.x + arm, c.y), strokeWidth = 2f)
                        drawLine(color = Color(0xFF10B981), start = Offset(c.x, c.y - arm), end = Offset(c.x, c.y + arm), strokeWidth = 2f)
                    }

                    if (calibrationPoints.isNotEmpty()) {
                        for (cp in calibrationPoints) {
                            drawCircle(color = Color(0xFFF59E0B), radius = 7f, center = cp)
                            drawCircle(color = Color.White, radius = 7f, center = cp, style = Stroke(width = 2f))
                        }
                        if (calibrationPoints.size == 2) {
                            drawLine(
                                color = Color(0xFFF59E0B),
                                start = calibrationPoints[0],
                                end = calibrationPoints[1],
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }
                    }

                    if (impactsList.size >= 2) {
                        var sumX = 0f
                        var sumY = 0f
                        for (imp in impactsList) {
                            sumX += imp.canvasOffset.x
                            sumY += imp.canvasOffset.y
                        }
                        val mpiPos = Offset(sumX / impactsList.size, sumY / impactsList.size)

                        var maxRadius = 0f
                        for (imp in impactsList) {
                            val d = hypot(imp.canvasOffset.x - mpiPos.x, imp.canvasOffset.y - mpiPos.y)
                            if (d > maxRadius) maxRadius = d
                        }

                        drawCircle(
                            color = Color(0xFF38BDF8).copy(alpha = 0.65f),
                            radius = maxRadius + 8f,
                            center = mpiPos,
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                        )

                        val mpiArm = 18f
                        drawCircle(color = Color(0xFF0284C7), radius = 4f, center = mpiPos)
                        drawLine(color = Color(0xFF38BDF8), start = Offset(mpiPos.x - mpiArm, mpiPos.y), end = Offset(mpiPos.x + mpiArm, mpiPos.y), strokeWidth = 2.5f)
                        drawLine(color = Color(0xFF38BDF8), start = Offset(mpiPos.x, mpiPos.y - mpiArm), end = Offset(mpiPos.x, mpiPos.y + mpiArm), strokeWidth = 2.5f)
                    }

                    val radius = 13f
                    impactsList.forEachIndexed { idx, imp ->
                        val pos = imp.canvasOffset
                        val isLatest = idx == impactsList.lastIndex

                        drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = radius + 2f, center = Offset(pos.x + 1f, pos.y + 1f))
                        drawCircle(color = if (isLatest) Color(0xFFFF3B30) else Color(0xFFDC2626), radius = radius, center = pos)
                        drawCircle(color = Color.White, radius = radius, center = pos, style = Stroke(width = 2f))

                        val numLayout = textMeasurer.measure(
                            imp.index.toString(),
                            style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        )
                        drawText(
                            textLayoutResult = numLayout,
                            topLeft = Offset(pos.x - numLayout.size.width / 2f, pos.y - numLayout.size.height / 2f)
                        )
                    }
                }
            }

            // 5. PANNEAU DE TÉLÉMÉTRIE BALISTIQUE
            Card(
                modifier = Modifier.fillMaxWidth().testTag("live_metrics_panel"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Télémétrie balistique",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${weaponNameInput.ifBlank { "Tir" }} • ${impactsList.size} coup${if (impactsList.size > 1) "s" else ""} à ${distanceMeters.toInt()}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", currentStats.moa)} MOA",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Écart Extrême (ES)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", currentStats.esMm)} mm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column {
                            Text("H + L", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", currentStats.hPlusL)} mm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("H (Hauteur)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", currentStats.hMm)} mm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("L (Largeur)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", currentStats.lMm)} mm", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Dérive EMPI (MPI)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("ΔX = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaXMm)} mm  |  ΔY = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaYMm)} mm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            // 6. BOUTON ENREGISTRER LA SÉANCE
            Button(
                onClick = { saveSessionToJson() },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_session_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer la séance", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
