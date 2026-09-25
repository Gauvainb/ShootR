package com.example

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.BallisticsCalculator
import com.example.domain.Impact
import com.example.domain.TargetDetectionResult
import com.example.domain.TargetRecognitionEngine
import com.example.ui.components.ChartDisplayType
import com.example.ui.components.MoaProgressionChart
import com.example.ui.components.SessionChartData
import kotlinx.coroutines.launch
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

enum class AppTab {
    SHOOTING_RANGE,
    HISTORY,
    ARMORY
}

data class ScreenImpact(
    val index: Int,
    val canvasOffset: Offset,
    val realMm: Impact,
    val score: Int = 10,
    val isInnerTen: Boolean = false,
    val isFlyer: Boolean = false
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
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    var currentTab by remember { mutableStateOf(AppTab.SHOOTING_RANGE) }

    // 1. Photo selection & Recognition state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedTargetResult by remember { mutableStateOf<TargetDetectionResult?>(null) }
    var showTargetDetectionDialog by remember { mutableStateOf(false) }
    var isAnalyzingTarget by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isAnalyzingTarget = true
            coroutineScope.launch {
                val result = TargetRecognitionEngine.analyzeImage(context, uri)
                detectedTargetResult = result
                showTargetDetectionDialog = true
                isAnalyzingTarget = false
            }
        }
    }

    // 2. Caractérisation de la séance
    var sessionTitleInput by remember { mutableStateOf("Séance de tir") }
    var weaponNameInput by remember { mutableStateOf("CZ 457 Varmint") }
    var weaponCaliberInput by remember { mutableStateOf(".22 LR") }
    var targetTypeInput by remember { mutableStateOf("C50 (25m/50m)") }
    var distanceInput by remember { mutableStateOf("50") }
    var ammoInput by remember { mutableStateOf("SK Rifle Match 40gr") }
    var notesInput by remember { mutableStateOf("") }
    var editingSessionId by remember { mutableStateOf<Long?>(null) }

    val distanceMeters = remember(distanceInput) {
        distanceInput.toFloatOrNull() ?: 50f
    }

    // Target presets
    val targetTypeOptions = listOf(
        "C50 (25m/50m)",
        "Pistolet 10m (ISSF)",
        "Carabine 10m (ISSF)",
        "C200 (200m)",
        "Cible Hunter / Précision",
        "IPSC Classic / Métallique"
    )

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

    // 5. Shot Score Adjustment (Ajustement des points a posteriori)
    var impactUnderEdit by remember { mutableStateOf<ScreenImpact?>(null) }
    var showImpactEditDialog by remember { mutableStateOf(false) }

    // Calculate score automatically based on distance from target center
    fun computeScoreForImpact(realMm: Impact, targetType: String): Pair<Int, Boolean> {
        val distMm = hypot(realMm.xMm, realMm.yMm)
        return when {
            targetType.contains("Carabine 10m") -> {
                when {
                    distMm <= 1.0f -> Pair(10, true) // Mouche
                    distMm <= 2.5f -> Pair(10, false)
                    distMm <= 5.0f -> Pair(9, false)
                    distMm <= 7.5f -> Pair(8, false)
                    distMm <= 10.0f -> Pair(7, false)
                    distMm <= 13.0f -> Pair(6, false)
                    distMm <= 16.0f -> Pair(5, false)
                    distMm <= 19.0f -> Pair(4, false)
                    distMm <= 22.0f -> Pair(3, false)
                    distMm <= 25.0f -> Pair(2, false)
                    distMm <= 30.0f -> Pair(1, false)
                    else -> Pair(0, false)
                }
            }
            targetType.contains("Pistolet 10m") -> {
                when {
                    distMm <= 2.5f -> Pair(10, true)
                    distMm <= 5.75f -> Pair(10, false)
                    distMm <= 13.5f -> Pair(9, false)
                    distMm <= 21.5f -> Pair(8, false)
                    distMm <= 29.75f -> Pair(7, false)
                    distMm <= 38.0f -> Pair(6, false)
                    distMm <= 46.0f -> Pair(5, false)
                    distMm <= 54.0f -> Pair(4, false)
                    distMm <= 62.0f -> Pair(3, false)
                    distMm <= 70.0f -> Pair(2, false)
                    distMm <= 85.0f -> Pair(1, false)
                    else -> Pair(0, false)
                }
            }
            targetType.contains("C200") -> {
                when {
                    distMm <= 25f -> Pair(10, true)
                    distMm <= 50f -> Pair(10, false)
                    distMm <= 100f -> Pair(9, false)
                    distMm <= 150f -> Pair(8, false)
                    distMm <= 200f -> Pair(7, false)
                    distMm <= 250f -> Pair(6, false)
                    distMm <= 300f -> Pair(5, false)
                    distMm <= 350f -> Pair(4, false)
                    distMm <= 400f -> Pair(1, false)
                    else -> Pair(0, false)
                }
            }
            else -> { // C50 standard (Visuel 200mm, carton 500mm)
                when {
                    distMm <= 12.5f -> Pair(10, true) // Mouche 25mm diam
                    distMm <= 25.0f -> Pair(10, false) // 10: 50mm diam
                    distMm <= 50.0f -> Pair(9, false)  // 9: 100mm diam
                    distMm <= 75.0f -> Pair(8, false)  // 8: 150mm diam
                    distMm <= 100.0f -> Pair(7, false) // 7: 200mm diam (limite noir)
                    distMm <= 125.0f -> Pair(6, false)
                    distMm <= 150.0f -> Pair(5, false)
                    distMm <= 175.0f -> Pair(4, false)
                    distMm <= 200.0f -> Pair(3, false)
                    distMm <= 225.0f -> Pair(2, false)
                    distMm <= 250.0f -> Pair(1, false)
                    else -> Pair(0, false)
                }
            }
        }
    }

    // 6. History & Stats Sheet state
    var savedSessionsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var historySearchQuery by remember { mutableStateOf("") }
    var historyDistanceFilter by remember { mutableStateOf("Toutes") }
    var historyWeaponFilter by remember { mutableStateOf("Toutes") }

    var showChartDialog by remember { mutableStateOf(false) }
    var chartWeaponFilter by remember { mutableStateOf("Toutes") }
    var chartDisplayType by remember { mutableStateOf(ChartDisplayType.LINE) }

    fun loadHistory(): List<JSONObject> {
        return try {
            val file = File(context.filesDir, "sessions_history.json")
            if (!file.exists()) {
                val sampleArray = JSONArray()
                val now = System.currentTimeMillis()
                val samples = listOf(
                    Triple(now - 86400000L * 4, 1.45f, 21.1f),
                    Triple(now - 86400000L * 3, 1.20f, 17.5f),
                    Triple(now - 86400000L * 2, 0.95f, 13.8f),
                    Triple(now - 86400000L * 1, 0.82f, 11.9f)
                )
                samples.forEachIndexed { _, (time, moaVal, esVal) ->
                    val obj = JSONObject()
                    obj.put("id", time)
                    obj.put("title", "Entraînement Précision 50m")
                    obj.put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(time)))
                    obj.put("weapon", "CZ 457 Varmint")
                    obj.put("caliber", ".22 LR")
                    obj.put("targetType", "C50 (25m/50m)")
                    obj.put("distanceMeters", 50.0)
                    obj.put("ammo", "SK Rifle Match 40gr")
                    obj.put("notes", "Bon groupement serré, météo calme.")
                    obj.put("shotsCount", 5)
                    obj.put("totalScore", 48)
                    obj.put("innerTens", 2)
                    obj.put("esMm", esVal.toDouble())
                    obj.put("hMm", (esVal * 0.8).toDouble())
                    obj.put("lMm", (esVal * 0.7).toDouble())
                    obj.put("hPlusL", (esVal * 1.5).toDouble())
                    obj.put("moa", moaVal.toDouble())
                    obj.put("mpiDeltaXMm", 1.2)
                    obj.put("mpiDeltaYMm", -0.8)

                    val imps = JSONArray()
                    val offsets = listOf(
                        Pair(5.0, 4.0),
                        Pair(-4.0, 6.0),
                        Pair(2.0, -3.0),
                        Pair(8.0, 10.0),
                        Pair(-6.0, -5.0)
                    )
                    offsets.forEachIndexed { i, (x, y) ->
                        val impObj = JSONObject()
                        impObj.put("index", i + 1)
                        impObj.put("xMm", x)
                        impObj.put("yMm", y)
                        impObj.put("score", if (i < 3) 10 else 9)
                        impObj.put("isInnerTen", i == 0 || i == 2)
                        imps.put(impObj)
                    }
                    obj.put("impacts", imps)
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

    fun saveHistory(list: List<JSONObject>) {
        try {
            val file = File(context.filesDir, "sessions_history.json")
            val array = JSONArray()
            list.forEach { array.put(it) }
            file.writeText(array.toString(2))
            savedSessionsList = list
        } catch (_: Exception) {}
    }

    remember {
        savedSessionsList = loadHistory()
        true
    }

    // Ballistics stats based on impacts (ignoring flyers)
    val currentStats by remember(impactsList.size, distanceMeters, pixelPerMm, targetCenterPx) {
        derivedStateOf {
            val activeImpacts = impactsList.filter { !it.isFlyer }.map { it.realMm }
            BallisticsCalculator.calculateStats(
                impacts = activeImpacts,
                targetCenter = Impact(0f, 0f),
                distanceMeters = distanceMeters
            )
        }
    }

    val totalScore = remember(impactsList.size) {
        derivedStateOf { impactsList.sumOf { it.score } }
    }
    val innerTensCount = remember(impactsList.size) {
        derivedStateOf { impactsList.count { it.isInnerTen } }
    }

    // Function to load a past session into the active editor
    fun loadSessionForRetroactiveAdjustment(sessionObj: JSONObject) {
        try {
            editingSessionId = sessionObj.optLong("id", 0L)
            sessionTitleInput = sessionObj.optString("title", "Séance")
            weaponNameInput = sessionObj.optString("weapon", "CZ 457 Varmint")
            weaponCaliberInput = sessionObj.optString("caliber", ".22 LR")
            targetTypeInput = sessionObj.optString("targetType", "C50 (25m/50m)")
            distanceInput = sessionObj.optDouble("distanceMeters", 50.0).toInt().toString()
            ammoInput = sessionObj.optString("ammo", "")
            notesInput = sessionObj.optString("notes", "")

            val imgUriStr = sessionObj.optString("imageUri", "")
            selectedImageUri = if (imgUriStr.isNotBlank()) Uri.parse(imgUriStr) else null

            // Reconstruct impacts
            impactsList.clear()
            val imps = sessionObj.optJSONArray("impacts")
            val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4.0f
            val center = targetCenterPx ?: Offset(500f, 500f)

            if (imps != null) {
                for (i in 0 until imps.length()) {
                    val imp = imps.getJSONObject(i)
                    val xMm = imp.optDouble("xMm", 0.0).toFloat()
                    val yMm = imp.optDouble("yMm", 0.0).toFloat()
                    val sc = imp.optInt("score", 10)
                    val isInner = imp.optBoolean("isInnerTen", false)
                    val isFlyer = imp.optBoolean("isFlyer", false)

                    // Reconstruct canvas offset from realMm and center
                    val canvasX = center.x + xMm * pxMm
                    val canvasY = center.y - yMm * pxMm

                    impactsList.add(
                        ScreenImpact(
                            index = imp.optInt("index", i + 1),
                            canvasOffset = Offset(canvasX, canvasY),
                            realMm = Impact(xMm, yMm),
                            score = sc,
                            isInnerTen = isInner,
                            isFlyer = isFlyer
                        )
                    )
                }
            }

            currentTab = AppTab.SHOOTING_RANGE
            Toast.makeText(context, "Séance chargée : vous pouvez ajuster les points et impacts a posteriori.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur chargement séance : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Save or Update Session in JSON
    fun saveSession() {
        if (impactsList.isEmpty()) {
            Toast.makeText(context, "Ajoutez au moins un impact avant d'enregistrer.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = File(context.filesDir, "sessions_history.json")
            val currentList = loadHistory().toMutableList()

            val timestamp = editingSessionId ?: System.currentTimeMillis()
            val sessionObj = JSONObject()
            sessionObj.put("id", timestamp)
            sessionObj.put("title", sessionTitleInput.ifBlank { "Séance $targetTypeInput" })
            sessionObj.put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(timestamp)))
            sessionObj.put("weapon", weaponNameInput.ifBlank { "Arme libre" })
            sessionObj.put("caliber", weaponCaliberInput.ifBlank { "N/A" })
            sessionObj.put("targetType", targetTypeInput)
            sessionObj.put("distanceMeters", distanceMeters)
            sessionObj.put("ammo", ammoInput.ifBlank { "Munition libre" })
            sessionObj.put("notes", notesInput)
            sessionObj.put("imageUri", selectedImageUri?.toString() ?: "")
            sessionObj.put("shotsCount", impactsList.size)
            sessionObj.put("totalScore", totalScore.value)
            sessionObj.put("innerTens", innerTensCount.value)
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
                impObj.put("score", imp.score)
                impObj.put("isInnerTen", imp.isInnerTen)
                impObj.put("isFlyer", imp.isFlyer)
                impactsArr.put(impObj)
            }
            sessionObj.put("impacts", impactsArr)

            // Replace existing or add new
            val existingIndex = currentList.indexOfFirst { it.optLong("id") == timestamp }
            if (existingIndex >= 0) {
                currentList[existingIndex] = sessionObj
                Toast.makeText(context, "Séance mise à jour a posteriori !", Toast.LENGTH_SHORT).show()
            } else {
                currentList.add(0, sessionObj)
                Toast.makeText(context, "Nouvelle séance enregistrée !", Toast.LENGTH_SHORT).show()
            }

            saveHistory(currentList)
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // DIALOG: Reconnaissance automatique de cible
    if (showTargetDetectionDialog && detectedTargetResult != null) {
        val result = detectedTargetResult!!
        AlertDialog(
            onDismissRequest = { showTargetDetectionDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reconnaissance de cible IA", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎯 Cible détectée : ${result.targetType}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Niveau de confiance : ${result.confidencePercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = result.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = result.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Paramètres automatiques proposés :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("• Distance préconisée : ${result.suggestedDistanceMeters} m", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Diamètre visuel : ${result.visualDiameterMm} mm / Total : ${result.totalDiameterMm} mm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Centrage automatique calculé sur le visuel noir", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        targetTypeInput = result.targetType
                        distanceInput = result.suggestedDistanceMeters.toString()

                        // Automatically center the reticle on the detected center
                        val normCenter = result.detectedCenterNormalized
                        targetCenterPx = Offset(500f * (normCenter.x / 0.5f), 500f * (normCenter.y / 0.5f))

                        // Estimate pixelPerMm based on detected visual diameter
                        if (result.detectedBlackRadiusNormalized > 0.05f && result.visualDiameterMm > 0f) {
                            val estimatedVisualPixels = (result.detectedBlackRadiusNormalized * 2f) * 1000f
                            pixelPerMm = estimatedVisualPixels / result.visualDiameterMm
                        }

                        showTargetDetectionDialog = false
                        Toast.makeText(context, "Cible configurée : ${result.targetType} (Étalonnage auto)", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Appliquer & Calibrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDetectionDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // DIALOG: Ajustement des points a posteriori pour un coup spécifique
    if (showImpactEditDialog && impactUnderEdit != null) {
        val shot = impactUnderEdit!!
        var currentScore by remember(shot) { mutableIntStateOf(shot.score) }
        var isInnerTen by remember(shot) { mutableStateOf(shot.isInnerTen) }
        var isFlyer by remember(shot) { mutableStateOf(shot.isFlyer) }
        var deltaXMm by remember(shot) { mutableFloatStateOf(shot.realMm.xMm) }
        var deltaYMm by remember(shot) { mutableFloatStateOf(shot.realMm.yMm) }

        AlertDialog(
            onDismissRequest = { showImpactEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajuster le coup #${shot.index}", fontWeight = FontWeight.Bold)
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isInnerTen) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (isInnerTen) "10X" else "$currentScore pts",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ajustez rétroactivement les points attribués, la zone de cordon ou la position de l'impact :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Score Buttons Grid
                    Text("Attribution des points :", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Mouche 10X
                        FilterChip(
                            selected = currentScore == 10 && isInnerTen,
                            onClick = { currentScore = 10; isInnerTen = true },
                            label = { Text("10X (Mouche)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.3f), selectedLabelColor = Color(0xFF10B981))
                        )
                        // Standard numbers 10 to 0
                        (10 downTo 0).forEach { sc ->
                            FilterChip(
                                selected = currentScore == sc && !isInnerTen,
                                onClick = { currentScore = sc; isInnerTen = false },
                                label = { Text(if (sc == 0) "0 (Paille)" else "$sc") }
                            )
                        }
                    }

                    // 2. Cordon touché Quick Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentScore < 10) currentScore += 1
                                Toast.makeText(context, "Cordon favorable (+1 pt)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+1 Cordon", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (currentScore > 0) currentScore -= 1
                                Toast.makeText(context, "Cordon défavorable (-1 pt)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-1 Cordon", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 3. Nudge fine position (Recalage millimétrique)
                    Text(
                        text = "Recalage fin de la position (ΔX=${String.format(Locale.US, "%.1f", deltaXMm)} mm, ΔY=${String.format(Locale.US, "%.1f", deltaYMm)} mm) :",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { deltaXMm -= 1f }) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = "Gauche", modifier = Modifier.graphicsLayer { rotationZ = -90f })
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { deltaYMm += 1f }) {
                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Haut")
                            }
                            Text("1 mm", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { deltaYMm -= 1f }) {
                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Bas")
                            }
                        }
                        IconButton(onClick = { deltaXMm += 1f }) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = "Droite", modifier = Modifier.graphicsLayer { rotationZ = 90f })
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // 4. Flyer / Coup de doigt exclusion
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Coup de doigt (Flyer)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Exclure de la dispersion MOA du groupement", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isFlyer,
                            onCheckedChange = { isFlyer = it }
                        )
                    }

                    // 5. Delete specific shot
                    OutlinedButton(
                        onClick = {
                            val idx = impactsList.indexOfFirst { it.index == shot.index }
                            if (idx >= 0) {
                                impactsList.removeAt(idx)
                                // re-index
                                val reindexed = impactsList.mapIndexed { i, s -> s.copy(index = i + 1) }
                                impactsList.clear()
                                impactsList.addAll(reindexed)
                                Toast.makeText(context, "Coup #${shot.index} supprimé.", Toast.LENGTH_SHORT).show()
                            }
                            showImpactEditDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supprimer cet impact")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = impactsList.indexOfFirst { it.index == shot.index }
                        if (idx >= 0) {
                            val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                            val center = targetCenterPx ?: Offset(500f, 500f)
                            val newCanvasOffset = Offset(center.x + deltaXMm * pxMm, center.y - deltaYMm * pxMm)

                            impactsList[idx] = shot.copy(
                                score = currentScore,
                                isInnerTen = isInnerTen,
                                isFlyer = isFlyer,
                                realMm = Impact(deltaXMm, deltaYMm),
                                canvasOffset = newCanvasOffset
                            )
                            Toast.makeText(context, "Coup #${shot.index} ajusté ($currentScore pts) !", Toast.LENGTH_SHORT).show()
                        }
                        showImpactEditDialog = false
                    }
                ) {
                    Text("Valider l'ajustement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImpactEditDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // DIALOG: Étalonnage manuel de l'échelle
    if (showCalibrationDialog && calibrationPoints.size == 2) {
        val p1 = calibrationPoints[0]
        val p2 = calibrationPoints[1]
        val pixelDist = hypot(p1.x - p2.x, p1.y - p2.y)

        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text("Étalonnage manuel de l'échelle", fontWeight = FontWeight.Bold) },
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

    // DIALOG: Graphique d'évolution MOA
    if (showChartDialog) {
        val historyList = remember(savedSessionsList) { savedSessionsList }
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

    // MAIN SCAFFOLD WITH BOTTOM NAVIGATION
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CrisisAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingSessionId != null) "Tir Tracker • Ajustement" else "Tir Tracker",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showChartDialog = true }) {
                        Icon(imageVector = Icons.Default.AutoGraph, contentDescription = "Graphique MOA", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = { showWeaponsDialog = true }) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Coffre d'armes", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentTab == AppTab.SHOOTING_RANGE,
                    onClick = { currentTab = AppTab.SHOOTING_RANGE },
                    icon = { Icon(imageVector = Icons.Default.AdsClick, contentDescription = "Pas de tir") },
                    label = { Text("Pas de tir") }
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = {
                        savedSessionsList = loadHistory()
                        currentTab = AppTab.HISTORY
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (savedSessionsList.isNotEmpty()) {
                                    Badge { Text("${savedSessionsList.size}") }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.History, contentDescription = "Historique")
                        }
                    },
                    label = { Text("Historique") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (currentTab) {
            AppTab.SHOOTING_RANGE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("shooting_range_container"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Editing Mode Banner
                    if (editingSessionId != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ajustement a posteriori de la séance",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editingSessionId = null
                                        impactsList.clear()
                                        sessionTitleInput = "Séance de tir"
                                        Toast.makeText(context, "Nouvelle séance vierge réinitialisée.", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = ButtonDefaults.TextButtonContentPadding
                                ) {
                                    Text("Nouveau tir", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // 1. CARACTÉRISATION DE LA SÉANCE & TYPE DE CIBLE
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
                                    Text("Armes (${weaponsList.size})", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            // Titre de séance
                            OutlinedTextField(
                                value = sessionTitleInput,
                                onValueChange = { sessionTitleInput = it },
                                label = { Text("Titre de la séance") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Type de cible avec Reconnaissance Auto Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Type de cible :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (detectedTargetResult != null) {
                                    Text(
                                        text = "🎯 IA : ${detectedTargetResult?.targetType}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(targetTypeOptions) { target ->
                                    FilterChip(
                                        selected = targetTypeInput == target,
                                        onClick = {
                                            targetTypeInput = target
                                            if (target.contains("10m")) distanceInput = "10"
                                            else if (target.contains("200m")) distanceInput = "200"
                                        },
                                        label = { Text(target, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = weaponNameInput,
                                    onValueChange = { weaponNameInput = it },
                                    label = { Text("Arme") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = weaponCaliberInput,
                                    onValueChange = { weaponCaliberInput = it },
                                    label = { Text("Calibre") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
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
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = ammoInput,
                                    onValueChange = { ammoInput = it },
                                    label = { Text("Munitions (marque/grain)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(10.dp)
                                )
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

                    // 3. PHOTO LOADER & RECONNAISSANCE AUTOMATIQUE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.height(44.dp).testTag("load_photo_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedImageUri != null) "Changer photo" else "Charger photo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Bouton Scanner / Reconnaissance Cible
                            if (selectedImageUri != null) {
                                OutlinedButton(
                                    onClick = {
                                        isAnalyzingTarget = true
                                        coroutineScope.launch {
                                            val result = TargetRecognitionEngine.analyzeImage(context, selectedImageUri!!)
                                            detectedTargetResult = result
                                            showTargetDetectionDialog = true
                                            isAnalyzingTarget = false
                                        }
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanner cible", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset zoom", modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { if (impactsList.isNotEmpty()) impactsList.removeAt(impactsList.lastIndex) },
                                enabled = impactsList.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (impactsList.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
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
                            .pointerInput(activeMode, pixelPerMm, targetCenterPx, targetTypeInput) {
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
                                                val (sc, inner) = computeScoreForImpact(Impact(xMm, yMm), targetTypeInput)
                                                imp.copy(
                                                    realMm = Impact(xMm, yMm),
                                                    score = sc,
                                                    isInnerTen = inner
                                                )
                                            }
                                            impactsList.clear()
                                            impactsList.addAll(updated)
                                        }
                                        AnnotationMode.IMPACT -> {
                                            // Check if user tapped an existing impact to edit it a posteriori!
                                            val hitRadius = 25f
                                            val clickedImpact = impactsList.find { imp ->
                                                hypot(imp.canvasOffset.x - point.x, imp.canvasOffset.y - point.y) <= hitRadius
                                            }

                                            if (clickedImpact != null) {
                                                impactUnderEdit = clickedImpact
                                                showImpactEditDialog = true
                                            } else {
                                                // Add new impact
                                                val center = targetCenterPx ?: Offset(500f, 500f)
                                                val pxPerMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                                val xMm = (point.x - center.x) / pxPerMm
                                                val yMm = (center.y - point.y) / pxPerMm
                                                val realImpact = Impact(xMm, yMm)
                                                val (scoreVal, isInner) = computeScoreForImpact(realImpact, targetTypeInput)

                                                impactsList.add(
                                                    ScreenImpact(
                                                        index = impactsList.size + 1,
                                                        canvasOffset = point,
                                                        realMm = realImpact,
                                                        score = scoreVal,
                                                        isInnerTen = isInner
                                                    )
                                                )
                                            }
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
                                Text("Touchez « Charger photo » ou touchez directement la cible pour placer des impacts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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

                            // Grouping circle (ignoring flyers)
                            val validImpacts = impactsList.filter { !it.isFlyer }
                            if (validImpacts.size >= 2) {
                                var sumX = 0f
                                var sumY = 0f
                                for (imp in validImpacts) {
                                    sumX += imp.canvasOffset.x
                                    sumY += imp.canvasOffset.y
                                }
                                val mpiPos = Offset(sumX / validImpacts.size, sumY / validImpacts.size)

                                var maxRadius = 0f
                                for (imp in validImpacts) {
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

                            // Draw each impact
                            val radius = 13f
                            impactsList.forEachIndexed { idx, imp ->
                                val pos = imp.canvasOffset
                                val isLatest = idx == impactsList.lastIndex

                                drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = radius + 2f, center = Offset(pos.x + 1f, pos.y + 1f))
                                val impactColor = when {
                                    imp.isFlyer -> Color(0xFF6B7280) // Grayed out flyer
                                    imp.isInnerTen -> Color(0xFF10B981) // Green Mouche
                                    imp.score == 10 -> Color(0xFFEAB308) // Gold 10
                                    isLatest -> Color(0xFFFF3B30)
                                    else -> Color(0xFFDC2626)
                                }
                                drawCircle(color = impactColor, radius = radius, center = pos)
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

                    // 5. TABLEAU D'AJUSTEMENT RÉTROACTIF DES IMPACTS (A POSTERIORI)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                        text = "Impacts & Points (Ajustement)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Touchez un coup pour ajuster ses points ou son cordon",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${totalScore.value} pts (${innerTensCount.value}X)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (impactsList.isEmpty()) {
                                Text(
                                    text = "Aucun impact enregistré. Touchez la cible pour ajouter des impacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(impactsList) { imp ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when {
                                                imp.isFlyer -> MaterialTheme.colorScheme.surface
                                                imp.isInnerTen -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                imp.score >= 9 -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (imp.isInnerTen) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    impactUnderEdit = imp
                                                    showImpactEditDialog = true
                                                }
                                                .testTag("shot_chip_${imp.index}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("#${imp.index}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = if (imp.isInnerTen) "10X" else "${imp.score}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 14.sp,
                                                        color = if (imp.isInnerTen) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Ajuster", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. PANNEAU DE TÉLÉMÉTRIE BALISTIQUE
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
                                        text = "Télémétrie balistique (Groupement)",
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
                                    Text("Dérive MPI", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Text("ΔX = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaXMm)} mm  |  ΔY = ${String.format(Locale.US, "%+.1f", currentStats.mpiDeltaYMm)} mm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                }
                            }
                        }
                    }

                    // 7. BOUTON ENREGISTRER LA SÉANCE
                    Button(
                        onClick = { saveSession() },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_session_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingSessionId != null) "Mettre à jour la séance" else "Enregistrer la séance",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            AppTab.HISTORY -> {
                // ÉCRAN COMPLET D'HISTORIQUE DES SÉANCES
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("history_screen_container")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Historique des séances",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${savedSessionsList.size} séance${if (savedSessionsList.size > 1) "s" else ""} enregistrée${if (savedSessionsList.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(onClick = { showChartDialog = true }) {
                                Icon(imageVector = Icons.Default.AutoGraph, contentDescription = "Graphique MOA", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(
                                onClick = {
                                    val file = File(context.filesDir, "sessions_history.json")
                                    if (file.exists()) file.delete()
                                    savedSessionsList = emptyList()
                                    Toast.makeText(context, "Historique réinitialisé.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Vider l'historique", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barre de recherche
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        placeholder = { Text("Rechercher par arme, cible, calibre...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filtres de distance
                    val distanceFilterOptions = listOf("Toutes", "10m", "25m", "50m", "100m", "200m+")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(distanceFilterOptions) { f ->
                            FilterChip(
                                selected = historyDistanceFilter == f,
                                onClick = { historyDistanceFilter = f },
                                label = { Text(f, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtrage des séances
                    val filteredSessions = remember(savedSessionsList, historySearchQuery, historyDistanceFilter) {
                        savedSessionsList.filter { s ->
                            val queryMatch = historySearchQuery.isBlank() ||
                                    s.optString("title").contains(historySearchQuery, ignoreCase = true) ||
                                    s.optString("weapon").contains(historySearchQuery, ignoreCase = true) ||
                                    s.optString("caliber").contains(historySearchQuery, ignoreCase = true) ||
                                    s.optString("targetType").contains(historySearchQuery, ignoreCase = true) ||
                                    s.optString("ammo").contains(historySearchQuery, ignoreCase = true)

                            val dist = s.optDouble("distanceMeters", 0.0)
                            val distMatch = when (historyDistanceFilter) {
                                "10m" -> dist.toInt() == 10
                                "25m" -> dist.toInt() == 25
                                "50m" -> dist.toInt() == 50
                                "100m" -> dist.toInt() == 100
                                "200m+" -> dist >= 200
                                else -> true
                            }
                            queryMatch && distMatch
                        }
                    }

                    if (filteredSessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.CrisisAlert, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Aucune séance correspondante", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Enregistrez une nouvelle séance pour la retrouver ici.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredSessions) { s ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { loadSessionForRetroactiveAdjustment(s) },
                                    shape = RoundedCornerShape(14.dp),
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
                                                    text = s.optString("title", "Séance"),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = s.optString("date", ""),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Score Pill
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                ) {
                                                    val score = s.optInt("totalScore", 0)
                                                    val inTens = s.optInt("innerTens", 0)
                                                    Text(
                                                        text = "$score pts${if (inTens > 0) " (${inTens}X)" else ""}",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }

                                                // MOA Pill
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.2f", s.optDouble("moa", 0.0))} MOA",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "${s.optString("weapon", "Arme")} (${s.optString("caliber", "")}) • ${s.optString("targetType", "C50")} à ${s.optDouble("distanceMeters", 50.0).toInt()}m",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        if (s.optString("ammo").isNotBlank()) {
                                            Text(
                                                text = "Munitions : ${s.optString("ammo")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ES: ${String.format(Locale.US, "%.1f", s.optDouble("esMm", 0.0))} mm | ${s.optInt("shotsCount", 0)} coups",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )

                                            OutlinedButton(
                                                onClick = { loadSessionForRetroactiveAdjustment(s) },
                                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ajuster les points", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AppTab.ARMORY -> {
                // Coffre d'armes (Accessible via navigation)
            }
        }
    }

    // DIALOG: Armory Dialog
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

    // DIALOG: Ajouter une arme
    if (showAddWeaponDialog) {
        val weaponTypes = listOf("Pistolet", "Carabine", "Fusil d'assaut", "Fusil de précision", "Revolver", "Air comprimé")
        val caliberSuggestions = listOf("9x19 mm", ".22 LR", "5.56x45 mm / .223 Rem", ".308 Win", "7.62x39 mm", ".38 Special", ".357 Magnum", "4.5 mm")

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
}
