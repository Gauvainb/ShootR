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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.BallisticsCalculator
import com.example.domain.CalibrationReference
import com.example.domain.Caliber
import com.example.data.LocalJsonStore
import com.example.domain.ImageFit
import com.example.domain.Impact
import com.example.domain.ImpactDetectionParams
import com.example.domain.ImpactDetector
import com.example.domain.TargetCalibrationDefaults
import com.example.domain.TargetDetectionResult
import com.example.domain.TargetRecognitionEngine
import com.example.domain.TargetSpecs
import com.example.ui.components.CalibrationAssistantPanel
import com.example.ui.components.CalibrationStep
import com.example.ui.components.ChartDisplayType
import com.example.ui.components.MoaProgressionChart
import com.example.ui.components.SessionChartData
import com.example.ui.components.ActionHeight
import com.example.ui.components.StepIndicator
import com.example.ui.components.SessionSummaryCard
import com.example.ui.components.StatTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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

/** Steps of recording a session, one screen each. */
enum class ShootingStep {
    SESSION,
    TARGET,
    IMPACTS
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
    val isFlyer: Boolean = false,
    // Placed by the automatic detection and not yet confirmed (moved or edited) by the user.
    val isAuto: Boolean = false
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
    // One cached layout per impact number: the default cache (8 entries) thrashes on every frame
    // as soon as more impacts than that are drawn.
    val textMeasurer = rememberTextMeasurer(cacheSize = 64)
    val impactNumberStyle = remember { TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black) }
    val calibrationLabelStyle = remember { TextStyle(color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    val calibrationRingDash = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f) }
    val calibrationRadiusDash = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f) }
    val groupCircleDash = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }
    val autoImpactDash = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f) }

    var currentTab by remember { mutableStateOf(AppTab.SHOOTING_RANGE) }
    var shootingStep by remember { mutableStateOf(ShootingStep.SESSION) }

    var targetTypeInput by remember { mutableStateOf("C50 (25m/50m)") }

    // 1. Photo selection & Recognition state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedTargetResult by remember { mutableStateOf<TargetDetectionResult?>(null) }
    var isAnalyzingTarget by remember { mutableStateOf(false) }

    // 2. Caractérisation de la séance
    var sessionTitleInput by remember { mutableStateOf("Séance de tir") }
    var weaponNameInput by remember { mutableStateOf("CZ 457 Varmint") }
    var weaponCaliberInput by remember { mutableStateOf(".22 LR") }
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
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        weaponsList.addAll(0, LocalJsonStore.loadWeapons(context))
    }

    // 4. Canvas interaction state
    var activeMode by remember { mutableStateOf(AnnotationMode.IMPACT) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // Until the target is laid out, assume it spans the screen width minus the margins.
    val expectedCanvasPx = with(LocalDensity.current) { (LocalConfiguration.current.screenWidthDp.dp - 32.dp).toPx() }
    var canvasWidthPx by remember { mutableFloatStateOf(expectedCanvasPx) }
    var canvasHeightPx by remember { mutableFloatStateOf(expectedCanvasPx) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragHudText by remember { mutableStateOf("") }
    var pendingImpactOffset by remember { mutableStateOf<Offset?>(null) }
    var pendingImpactScore by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var draggedImpactIndexState by remember { mutableStateOf<Int?>(null) }

    var selectedCalibrationRef by remember {
        mutableStateOf(TargetCalibrationDefaults.references[0])
    }
    var calibrationStep by remember { mutableStateOf(CalibrationStep.SET_CENTER) }
    var calibrationEdgePx by remember { mutableStateOf<Offset?>(null) }
    var pixelPerMm by remember { mutableFloatStateOf(0f) }

    // Keep calibration preset in sync with target type selection
    LaunchedEffect(targetTypeInput) {
        val matchingRef = TargetCalibrationDefaults.getReferenceForTargetType(targetTypeInput)
        selectedCalibrationRef = matchingRef
    }

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

    // The in-memory list is the source of truth; the file is read once and written in the background.
    var historyLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        savedSessionsList = LocalJsonStore.loadSessions(context)
        historyLoaded = true
    }

    fun saveHistory(list: List<JSONObject>) {
        savedSessionsList = list
        LocalJsonStore.saveSessions(context, list)
    }

    // Ballistics stats based on impacts (ignoring flyers)
    val currentStats by remember(distanceMeters) {
        derivedStateOf {
            val activeImpacts = impactsList.filter { !it.isFlyer }.map { it.realMm }
            BallisticsCalculator.calculateStats(
                impacts = activeImpacts,
                targetCenter = Impact(0f, 0f),
                distanceMeters = distanceMeters
            )
        }
    }

    val totalScore = remember { derivedStateOf { impactsList.sumOf { it.score } } }
    val innerTensCount = remember { derivedStateOf { impactsList.count { it.isInnerTen } } }

    // Recomputes real-world coordinates and scores of every impact after the center or scale moved.
    fun recomputeImpacts(center: Offset, pxMm: Float) {
        if (impactsList.isEmpty() || pxMm <= 0f) return
        for (i in impactsList.indices) {
            val imp = impactsList[i]
            val realMm = Impact((imp.canvasOffset.x - center.x) / pxMm, (center.y - imp.canvasOffset.y) / pxMm)
            val (sc, inner) = computeScoreForImpact(realMm, targetTypeInput)
            impactsList[i] = imp.copy(realMm = realMm, score = sc, isInnerTen = inner)
        }
    }

    // Real diameter of the black visual of the selected target, which gives the photo's scale;
    // null for targets without one (IPSC, metal plates).
    fun visualDiameterMm(): Float? {
        TargetSpecs.forTargetType(targetTypeInput)?.let { spec ->
            return spec.blackVisualDiameterMm.takeIf { it > 0f }
        }
        return TargetCalibrationDefaults.references
            .firstOrNull { it.targetType.equals(targetTypeInput, ignoreCase = true) }
            ?.diameterMm
    }

    // Centers and scales the canvas on the visual found on the photo. Impacts already placed are
    // rescored against the new center and scale.
    fun applyDetectedCalibration(result: TargetDetectionResult): Boolean {
        if (!result.visualFound || result.visualDiameterMm <= 0f) return false
        val detectedCenter = ImageFit.toCanvas(
            result.detectedCenterNormalized, canvasWidthPx, canvasHeightPx, result.imageAspectRatio
        )
        val shownImageWidthPx = ImageFit.displayedWidth(canvasWidthPx, canvasHeightPx, result.imageAspectRatio)
        val visualRadiusPx = result.detectedBlackRadiusNormalized * shownImageWidthPx
        if (visualRadiusPx <= 0f) return false

        targetCenterPx = detectedCenter
        pixelPerMm = visualRadiusPx * 2f / result.visualDiameterMm
        // Show the calibration ring on the visual it was measured on
        TargetCalibrationDefaults.references
            .firstOrNull { it.targetType.equals(result.targetType, ignoreCase = true) && it.diameterMm == result.visualDiameterMm }
            ?.let { selectedCalibrationRef = it }
        val ringRadiusPx = selectedCalibrationRef.radiusMm * pixelPerMm
        calibrationEdgePx = Offset(detectedCenter.x + ringRadiusPx, detectedCenter.y)
        calibrationStep = CalibrationStep.CALIBRATED
        recomputeImpacts(detectedCenter, pixelPerMm)
        return true
    }

    // Automatic impact detection on the photo, using the current calibration (center, scale),
    // the target type and the caliber to know what a bullet hole looks like.
    var isDetectingImpacts by remember { mutableStateOf(false) }

    fun detectImpactsOnPhoto() {
        val imageUri = selectedImageUri ?: return
        if (isDetectingImpacts) return
        if (isAnalyzingTarget) {
            Toast.makeText(context, "Analyse de la photo en cours, réessayez dans un instant.", Toast.LENGTH_SHORT).show()
            return
        }
        isDetectingImpacts = true
        coroutineScope.launch(Dispatchers.Main.immediate) {
            try {
                val targetRes = detectedTargetResult
                    ?: TargetRecognitionEngine.analyzeImage(context, imageUri, targetTypeInput, visualDiameterMm())
                if (selectedImageUri != imageUri) return@launch

                val aspect = targetRes.imageAspectRatio
                val shownWidthPx = ImageFit.displayedWidth(canvasWidthPx, canvasHeightPx, aspect)
                val caliberMm = Caliber.diameterMm(weaponCaliberInput)

                val params = if (pixelPerMm > 0f && shownWidthPx > 0f) {
                    val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                    val mmToPhoto = pixelPerMm / shownWidthPx
                    val spec = TargetSpecs.forTargetType(targetTypeInput)
                    ImpactDetectionParams(
                        centerNormalized = ImageFit.toImage(center, canvasWidthPx, canvasHeightPx, aspect),
                        blackRadiusNormalized = spec?.let { it.blackVisualDiameterMm / 2f * mmToPhoto }
                            ?: targetRes.detectedBlackRadiusNormalized,
                        mmToPhoto = mmToPhoto,
                        caliberMm = caliberMm,
                        cardHalfSizeNormalized = spec?.cardSizeMm?.let { it * 0.49f * mmToPhoto },
                        digitRadiiMm = spec?.digitRadiiMm ?: emptyList()
                    )
                } else {
                    // No scale at all (no visual found, nothing calibrated): search around the
                    // on-screen center; impacts are scored with the default scale like taps are.
                    val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                    ImpactDetectionParams(
                        centerNormalized = ImageFit.toImage(center, canvasWidthPx, canvasHeightPx, aspect),
                        blackRadiusNormalized = 0f,
                        caliberMm = caliberMm
                    )
                }

                val detected = TargetRecognitionEngine.detectImpacts(context, imageUri, params)
                    .map { ImageFit.toCanvas(it, canvasWidthPx, canvasHeightPx, aspect) }
                // The photo was changed while detecting: these holes belong to the previous one.
                if (selectedImageUri != imageUri) return@launch

                // Impacts already on the canvas are kept as they are: only new holes are added.
                val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                val minGapPx = maxOf(10f, (caliberMm ?: 0f) * pxMm * 0.5f)
                val newPoints = ImpactDetector.excludeKnown(impactsList.map { it.canvasOffset }, detected, minGapPx)

                newPoints.forEach { canvasOffset ->
                    val realImp = Impact((canvasOffset.x - center.x) / pxMm, (center.y - canvasOffset.y) / pxMm)
                    val (sc, inner) = computeScoreForImpact(realImp, targetTypeInput)
                    impactsList.add(
                        ScreenImpact(
                            index = impactsList.size + 1,
                            canvasOffset = canvasOffset,
                            realMm = realImp,
                            score = sc,
                            isInnerTen = inner,
                            isAuto = true
                        )
                    )
                }

                val message = when {
                    newPoints.isNotEmpty() ->
                        "${newPoints.size} impact${if (newPoints.size > 1) "s" else ""} détecté${if (newPoints.size > 1) "s" else ""} : vérifiez-les et corrigez si besoin."
                    detected.isNotEmpty() -> "Aucun nouvel impact : ceux détectés sont déjà placés."
                    else -> "Aucun impact détecté. Placez-les en touchant la cible."
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } finally {
                isDetectingImpacts = false
            }
        }
    }

    // A new photo: whatever was found or calibrated on the previous one no longer applies.
    fun resetPhotoAnalysis() {
        detectedTargetResult = null
        pixelPerMm = 0f
        calibrationEdgePx = null
        calibrationStep = CalibrationStep.SET_CENTER
        targetCenterPx = Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
    }

    // Looks for the black visual. The result is dropped if the photo changed in the meantime.
    fun analyzeSelectedPhoto() {
        val uri = selectedImageUri ?: return
        isAnalyzingTarget = true
        coroutineScope.launch(Dispatchers.Main.immediate) {
            try {
                val result = TargetRecognitionEngine.analyzeImage(context, uri, targetTypeInput, visualDiameterMm())
                if (selectedImageUri == uri) {
                    detectedTargetResult = result
                    if (!applyDetectedCalibration(result)) calibrationStep = CalibrationStep.SET_CENTER
                }
            } finally {
                isAnalyzingTarget = false
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            resetPhotoAnalysis()
            analyzeSelectedPhoto()
        }
    }

    // Moves to a step. The target step is the only one where the calibration can change, the
    // impacts step the only one where impacts are placed or detected.
    fun goToStep(step: ShootingStep) {
        shootingStep = step
        when (step) {
            ShootingStep.SESSION -> Unit
            ShootingStep.TARGET -> {
                activeMode = AnnotationMode.CALIBRATE
                calibrationStep = if (pixelPerMm > 0f) CalibrationStep.CALIBRATED else CalibrationStep.SET_CENTER
                // A photo that was never analyzed (or whose target type changed): look for the visual.
                if (selectedImageUri != null && pixelPerMm <= 0f && detectedTargetResult == null && !isAnalyzingTarget) {
                    analyzeSelectedPhoto()
                }
            }
            ShootingStep.IMPACTS -> {
                activeMode = AnnotationMode.IMPACT
                // First visit with a calibrated photo: look for the impacts right away.
                if (selectedImageUri != null && pixelPerMm > 0f && impactsList.isEmpty()) detectImpactsOnPhoto()
            }
        }
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
            // Calibration is not saved with a session: what is on screen belongs to another photo.
            resetPhotoAnalysis()

            // Reconstruct impacts
            impactsList.clear()
            val imps = sessionObj.optJSONArray("impacts")
            val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4.0f
            val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)

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
            shootingStep = ShootingStep.IMPACTS
            activeMode = AnnotationMode.IMPACT
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

        if (!historyLoaded) return

        try {
            val currentList = savedSessionsList.toMutableList()

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
                                isAuto = false,
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
                    onClick = { currentTab = AppTab.HISTORY },
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
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("shooting_range_container"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Correction d'une séance enregistrée",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        editingSessionId = null
                                        impactsList.clear()
                                        selectedImageUri = null
                                        resetPhotoAnalysis()
                                        goToStep(ShootingStep.SESSION)
                                        sessionTitleInput = "Séance de tir"
                                        Toast.makeText(context, "Nouvelle séance vierge réinitialisée.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Nouveau tir", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    StepIndicator(
                        steps = listOf("Séance", "Cible", "Impacts"),
                        currentIndex = shootingStep.ordinal,
                        onSelect = { goToStep(ShootingStep.entries[it]) },
                        modifier = Modifier.testTag("step_indicator")
                    )

                    if (shootingStep == ShootingStep.SESSION) {
                    // ÉTAPE 1 : LA SÉANCE
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("session_form"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Votre séance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { showWeaponsDialog = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choisir dans mes armes (${weaponsList.size})", fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
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
                                    Text("Type de cible", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (detectedTargetResult?.visualFound == true) {
                                        Text(
                                            text = "🎯 Visuel repéré",
                                            style = MaterialTheme.typography.labelLarge,
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
                                                if (target != targetTypeInput && pixelPerMm > 0f) {
                                                    // Another target means another visual size: calibrate again.
                                                    resetPhotoAnalysis()
                                                }
                                                targetTypeInput = target
                                                if (target.contains("10m")) distanceInput = "10"
                                                else if (target.contains("200m")) distanceInput = "200"
                                            },
                                            label = { Text(target, style = MaterialTheme.typography.labelLarge) },
                                            modifier = Modifier.height(44.dp)
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

                    Button(
                        onClick = { goToStep(ShootingStep.TARGET) },
                        modifier = Modifier.fillMaxWidth().height(ActionHeight).testTag("next_to_target_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Suivant : la cible", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                    } else {
                    // ÉTAPES 2 ET 3 : rappel de la séance, puis la cible
                    SessionSummaryCard(
                        title = sessionTitleInput,
                        targetType = targetTypeInput,
                        distanceMeters = distanceInput,
                        weapon = weaponNameInput,
                        caliber = weaponCaliberInput,
                        ammo = ammoInput,
                        onEdit = { goToStep(ShootingStep.SESSION) },
                        modifier = Modifier.testTag("session_characterization_card")
                    )

                    if (shootingStep == ShootingStep.TARGET) {
                        TargetStatusCard(
                            hasPhoto = selectedImageUri != null,
                            isAnalyzing = isAnalyzingTarget,
                            result = detectedTargetResult,
                            isCalibrated = pixelPerMm > 0f,
                            onRetry = { analyzeSelectedPhoto() }
                        )
                    }

                    // La cible n'apparaît à l'étape 2 qu'avec une photo à calibrer.
                    if (shootingStep == ShootingStep.IMPACTS || selectedImageUri != null) {
                    // 4. CANEVAS AVEC PHOTO ET OVERLAYS
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131821))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .clipToBounds()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                                    val firstDownPos = firstDown.position
                                    var isMultiTouch = false
                                    var prevCentroid = Offset.Zero
                                    var prevDistance = 0f

                                    var activeDragTarget: String? = null // "CENTER", "DIAMETER", "IMPACT"
                                    var draggedImpactIndex: Int? = null
                                    var dragMoved = false
                                    var lastScreenPos = firstDownPos

                                    // Local touch coordinates
                                    val currentCenter = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                                    val initialLocalX = (firstDownPos.x - offset.x) / scale
                                    val initialLocalY = (firstDownPos.y - offset.y) / scale
                                    val initialLocalPoint = Offset(initialLocalX, initialLocalY)

                                    val visualRadiusPx = if (pixelPerMm > 0f) selectedCalibrationRef.radiusMm * pixelPerMm else (canvasWidthPx * 0.25f)
                                    val distToCenter = hypot(initialLocalPoint.x - currentCenter.x, initialLocalPoint.y - currentCenter.y)
                                    val distToRing = kotlin.math.abs(distToCenter - visualRadiusPx)

                                    // Check if down on an existing impact
                                    val hitRadiusImpact = 24.dp.toPx() / scale
                                    val tappedImpactIdx = impactsList.indexOfFirst { imp ->
                                        hypot(imp.canvasOffset.x - initialLocalPoint.x, imp.canvasOffset.y - initialLocalPoint.y) <= hitRadiusImpact
                                    }

                                    if (activeMode == AnnotationMode.CALIBRATE) {
                                        activeDragTarget = when {
                                            calibrationStep == CalibrationStep.SET_CENTER -> "CENTER"
                                            calibrationStep == CalibrationStep.CLICK_EDGE -> "DIAMETER"
                                            distToCenter < 28.dp.toPx() / scale -> "CENTER"
                                            distToRing < 28.dp.toPx() / scale -> "DIAMETER"
                                            else -> "DIAMETER"
                                        }
                                    } else if (activeMode == AnnotationMode.TARGET_CENTER) {
                                        activeDragTarget = "CENTER"
                                    } else if (activeMode == AnnotationMode.IMPACT) {
                                        if (tappedImpactIdx >= 0) {
                                            draggedImpactIndex = tappedImpactIdx
                                            draggedImpactIndexState = tappedImpactIdx
                                            activeDragTarget = "EXISTING_IMPACT"
                                        } else {
                                            activeDragTarget = "NEW_IMPACT"
                                            val center = currentCenter
                                            val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                            val xMm = (initialLocalPoint.x - center.x) / pxMm
                                            val yMm = (center.y - initialLocalPoint.y) / pxMm
                                            val realImp = Impact(xMm, yMm)
                                            val scorePair = computeScoreForImpact(realImp, targetTypeInput)
                                            pendingImpactOffset = initialLocalPoint
                                            pendingImpactScore = scorePair
                                        }
                                    }

                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedPointers = event.changes.filter { it.pressed }

                                        if (pressedPointers.size >= 2) {
                                            // MULTI-TOUCH : UNICITÉ DU DÉFILEMENT ET DU ZOOM À 2 DOIGTS
                                            isMultiTouch = true
                                            isUserDragging = false
                                            dragHudText = ""
                                            pendingImpactOffset = null
                                            pendingImpactScore = null
                                            draggedImpactIndexState = null

                                            val p1 = pressedPointers[0].position
                                            val p2 = pressedPointers[1].position
                                            val currentCentroid = (p1 + p2) / 2f
                                            val currentDistance = hypot(p1.x - p2.x, p1.y - p2.y)

                                            if (prevDistance > 0f) {
                                                val zoomChange = currentDistance / prevDistance
                                                val newScale = (scale * zoomChange).coerceIn(0.8f, 10f)
                                                val pan = currentCentroid - prevCentroid

                                                // Zoom centré avec précision absolue sur le centroïde des 2 doigts
                                                val localCentroidX = (currentCentroid.x - offset.x) / scale
                                                val localCentroidY = (currentCentroid.y - offset.y) / scale
                                                val newOffsetX = currentCentroid.x - localCentroidX * newScale + pan.x
                                                val newOffsetY = currentCentroid.y - localCentroidY * newScale + pan.y

                                                scale = newScale
                                                offset = Offset(newOffsetX, newOffsetY)
                                            }

                                            prevCentroid = currentCentroid
                                            prevDistance = currentDistance
                                            event.changes.forEach { it.consume() }

                                        } else if (pressedPointers.size == 1 && !isMultiTouch) {
                                            // UN SEUL DOIGT : RÉGLAGE DYNAMIQUE DU CENTRE ET DU DIAMÈTRE SANS DÉFILER LA PHOTO
                                            val change = pressedPointers[0]
                                            val currentScreenPos = change.position
                                            lastScreenPos = currentScreenPos
                                            val moveDist = hypot(currentScreenPos.x - firstDownPos.x, currentScreenPos.y - firstDownPos.y)
                                            if (moveDist > 6f) {
                                                dragMoved = true
                                            }

                                            val currentLocalX = (currentScreenPos.x - offset.x) / scale
                                            val currentLocalY = (currentScreenPos.y - offset.y) / scale
                                            val currentLocalPoint = Offset(currentLocalX, currentLocalY)

                                            isUserDragging = true

                                            when (activeMode) {
                                                AnnotationMode.CALIBRATE -> {
                                                    if (activeDragTarget == "CENTER") {
                                                        // Déplacement dynamique du centre vert en gardant le doigt appuyé
                                                        targetCenterPx = currentLocalPoint
                                                        dragHudText = "🎯 Centre : (${currentLocalPoint.x.toInt()}, ${currentLocalPoint.y.toInt()})"

                                                        // Recalcul en temps réel des coordonnées réelles des impacts
                                                        recomputeImpacts(currentLocalPoint, pixelPerMm)
                                                    } else {
                                                        // Ajustement dynamique du diamètre du visuel en gardant le doigt appuyé
                                                        val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                                                        val rPx = hypot(currentLocalPoint.x - center.x, currentLocalPoint.y - center.y)
                                                        if (rPx > 10f && selectedCalibrationRef.radiusMm > 0f) {
                                                            pixelPerMm = rPx / selectedCalibrationRef.radiusMm
                                                            calibrationEdgePx = currentLocalPoint
                                                            calibrationStep = CalibrationStep.CALIBRATED

                                                            dragHudText = "📏 Visuel ${selectedCalibrationRef.diameterMm.toInt()} mm (${String.format(Locale.US, "%.2f", pixelPerMm)} px/mm)"

                                                            // Recalcul en temps réel de tous les impacts avec la nouvelle échelle
                                                            recomputeImpacts(center, pixelPerMm)
                                                        }
                                                    }
                                                    change.consume()
                                                }
                                                AnnotationMode.TARGET_CENTER -> {
                                                    targetCenterPx = currentLocalPoint
                                                    dragHudText = "🎯 Centre : (${currentLocalPoint.x.toInt()}, ${currentLocalPoint.y.toInt()})"
                                                    recomputeImpacts(currentLocalPoint, if (pixelPerMm > 0f) pixelPerMm else 4f)
                                                    change.consume()
                                                }
                                                AnnotationMode.IMPACT -> {
                                                    val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                                                    val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                                    val xMm = (currentLocalPoint.x - center.x) / pxMm
                                                    val yMm = (center.y - currentLocalPoint.y) / pxMm
                                                    val realImp = Impact(xMm, yMm)
                                                    val (sc, inner) = computeScoreForImpact(realImp, targetTypeInput)
                                                    val scoreLabel = if (inner) "10 Mouche" else "$sc pts"

                                                    if (activeDragTarget == "EXISTING_IMPACT" && draggedImpactIndex != null) {
                                                        val idx = draggedImpactIndex!!
                                                        impactsList[idx] = impactsList[idx].copy(
                                                            isAuto = false,
                                                            canvasOffset = currentLocalPoint,
                                                            realMm = realImp,
                                                            score = sc,
                                                            isInnerTen = inner
                                                        )
                                                        draggedImpactIndexState = idx
                                                        dragHudText = "🎯 Coup #${impactsList[idx].index} : $scoreLabel (${String.format(Locale.US, "%+.1f", xMm)}, ${String.format(Locale.US, "%+.1f", yMm)} mm)"
                                                    } else {
                                                        // Glissement dynamique du nouvel impact en temps réel
                                                        pendingImpactOffset = currentLocalPoint
                                                        pendingImpactScore = Pair(sc, inner)
                                                        dragHudText = "🎯 Coup #${impactsList.size + 1} : $scoreLabel (${String.format(Locale.US, "%+.1f", xMm)}, ${String.format(Locale.US, "%+.1f", yMm)} mm)"
                                                    }
                                                    change.consume()
                                                }
                                            }
                                        }
                                    } while (event.changes.any { it.pressed })

                                    isUserDragging = false
                                    dragHudText = ""

                                    // Relâchement du doigt
                                    if (!isMultiTouch) {
                                        val upScreenPos = lastScreenPos
                                        val localX = (upScreenPos.x - offset.x) / scale
                                        val localY = (upScreenPos.y - offset.y) / scale
                                        val upLocalPoint = Offset(localX, localY)
                                        val totalMove = hypot(upScreenPos.x - firstDownPos.x, upScreenPos.y - firstDownPos.y)

                                        when (activeMode) {
                                            AnnotationMode.CALIBRATE -> {
                                                if (activeDragTarget == "CENTER" || calibrationStep == CalibrationStep.SET_CENTER) {
                                                    targetCenterPx = upLocalPoint
                                                    calibrationStep = CalibrationStep.CLICK_EDGE
                                                    Toast.makeText(context, "Centre vert positionné. Touchez ou glissez sur le bord du visuel noir (${selectedCalibrationRef.diameterMm.toInt()} mm).", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                                                    val rPx = hypot(upLocalPoint.x - center.x, upLocalPoint.y - center.y)
                                                    if (rPx > 10f && selectedCalibrationRef.radiusMm > 0f) {
                                                        pixelPerMm = rPx / selectedCalibrationRef.radiusMm
                                                        calibrationEdgePx = upLocalPoint
                                                        calibrationStep = CalibrationStep.CALIBRATED
                                                    }
                                                }
                                            }
                                            AnnotationMode.TARGET_CENTER -> {
                                                targetCenterPx = upLocalPoint
                                            }
                                            AnnotationMode.IMPACT -> {
                                                val center = targetCenterPx ?: Offset(canvasWidthPx / 2f, canvasHeightPx / 2f)
                                                val pxMm = if (pixelPerMm > 0f) pixelPerMm else 4f
                                                val xMm = (upLocalPoint.x - center.x) / pxMm
                                                val yMm = (center.y - upLocalPoint.y) / pxMm
                                                val realImp = Impact(xMm, yMm)
                                                val (sc, inner) = computeScoreForImpact(realImp, targetTypeInput)

                                                if (activeDragTarget == "EXISTING_IMPACT" && draggedImpactIndex != null) {
                                                    val idx = draggedImpactIndex!!
                                                    if (totalMove < 12f) {
                                                        // Tap court sans déplacement : ouvre le dialogue d'ajustement
                                                        impactUnderEdit = impactsList[idx]
                                                        showImpactEditDialog = true
                                                    } else {
                                                        // Déplacement finalisé par glissement
                                                        impactsList[idx] = impactsList[idx].copy(
                                                            isAuto = false,
                                                            canvasOffset = upLocalPoint,
                                                            realMm = realImp,
                                                            score = sc,
                                                            isInnerTen = inner
                                                        )
                                                        val label = if (inner) "10 Mouche" else "$sc pts"
                                                        Toast.makeText(context, "Coup #${impactsList[idx].index} ajusté ($label) !", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    // Nouvel impact positionné au tap ou au glissement
                                                    impactsList.add(
                                                        ScreenImpact(
                                                            index = impactsList.size + 1,
                                                            canvasOffset = upLocalPoint,
                                                            realMm = realImp,
                                                            score = sc,
                                                            isInnerTen = inner
                                                        )
                                                    )
                                                }
                                                pendingImpactOffset = null
                                                pendingImpactScore = null
                                                draggedImpactIndexState = null
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("target_photo_canvas")
                    ) {
                        val currentWidthPx = constraints.maxWidth.toFloat()
                        val currentHeightPx = constraints.maxHeight.toFloat()
                        LaunchedEffect(currentWidthPx, currentHeightPx) {
                            if (currentWidthPx > 0f && currentHeightPx > 0f) {
                                canvasWidthPx = currentWidthPx
                                canvasHeightPx = currentHeightPx
                                if (targetCenterPx == null) {
                                    targetCenterPx = Offset(currentWidthPx / 2f, currentHeightPx / 2f)
                                }
                            }
                        }

                        val imageRequest = remember(selectedImageUri) {
                            selectedImageUri?.let { ImageRequest.Builder(context).data(it).crossfade(true).build() }
                        }
                        // Only the boolean is observed here, so pinch-zoom frames do not recompose the canvas content.
                        val isZoomed by remember { derivedStateOf { scale > 1.05f || offset != Offset.Zero } }

                        // CONTENEUR TRANSFORMÉ UNIQUE : La photo et le canvas partagent exactement le même espace de coordonnées
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                        ) {
                            if (imageRequest != null) {
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Cible de tir",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (impactsList.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Touchez la cible pour placer chaque impact à la main.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = targetCenterPx ?: Offset(size.width / 2f, size.height / 2f)

                                // Centre de la cible (croix et réticule vert)
                                // Overlay sizes in dp, so markers keep the same physical size on every screen.
                                val k = 0.75f * density
                                val r = 18f * k
                                drawCircle(color = Color(0xFF10B981).copy(alpha = 0.35f), radius = r + 6f * k, center = center)
                                drawCircle(color = Color(0xFF10B981), radius = r, center = center, style = Stroke(width = 2.5f * k))
                                drawCircle(color = Color(0xFF10B981), radius = 3.5f * k, center = center)
                                val arm = 28f * k
                                drawLine(color = Color(0xFF10B981), start = Offset(center.x - arm, center.y), end = Offset(center.x + arm, center.y), strokeWidth = 2.2f * k)
                                drawLine(color = Color(0xFF10B981), start = Offset(center.x, center.y - arm), end = Offset(center.x, center.y + arm), strokeWidth = 2.2f * k)

                                // Anneau d'étalonnage du visuel cible
                                if (pixelPerMm > 0f) {
                                    val visualRadiusPx = selectedCalibrationRef.radiusMm * pixelPerMm

                                    if (activeMode == AnnotationMode.CALIBRATE) {
                                        // Anneau visible et poignée d'ajustement en mode calibration
                                        drawCircle(
                                            color = Color(0xFFFBBF24).copy(alpha = 0.15f),
                                            radius = visualRadiusPx,
                                            center = center
                                        )
                                        drawCircle(
                                            color = Color(0xFFFBBF24),
                                            radius = visualRadiusPx,
                                            center = center,
                                            style = Stroke(width = 3f * k, pathEffect = calibrationRingDash)
                                        )

                                        // Poignée d'ajustement sur le bord du visuel
                                        val edgePoint = calibrationEdgePx ?: Offset(center.x + visualRadiusPx, center.y)
                                        drawCircle(color = Color(0xFFFBBF24).copy(alpha = 0.4f), radius = 14f * k, center = edgePoint)
                                        drawCircle(color = Color(0xFFFBBF24), radius = 7f * k, center = edgePoint)
                                        drawCircle(color = Color.White, radius = 7f * k, center = edgePoint, style = Stroke(width = 2f * k))

                                        drawLine(
                                            color = Color(0xFFFBBF24).copy(alpha = 0.85f),
                                            start = center,
                                            end = edgePoint,
                                            strokeWidth = 2f * k,
                                            pathEffect = calibrationRadiusDash
                                        )

                                        val label = "${selectedCalibrationRef.diameterMm.toInt()} mm (${selectedCalibrationRef.shortName})"
                                        val textLayout = textMeasurer.measure(label, style = calibrationLabelStyle)
                                        drawText(
                                            textLayoutResult = textLayout,
                                            topLeft = Offset(center.x - textLayout.size.width / 2f, center.y - visualRadiusPx - textLayout.size.height - 6f)
                                        )
                                    } else {
                                        // Anneau discret en mode tir
                                        drawCircle(
                                            color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                                            radius = visualRadiusPx,
                                            center = center,
                                            style = Stroke(width = 1.5f * k)
                                        )
                                    }
                                }

                                // Cercle de groupement (hors flyers)
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
                                        radius = maxRadius + 8f * k,
                                        center = mpiPos,
                                        style = Stroke(width = 2f * k, pathEffect = groupCircleDash)
                                    )

                                    val mpiArm = 18f * k
                                    drawCircle(color = Color(0xFF0284C7), radius = 4f * k, center = mpiPos)
                                    drawLine(color = Color(0xFF38BDF8), start = Offset(mpiPos.x - mpiArm, mpiPos.y), end = Offset(mpiPos.x + mpiArm, mpiPos.y), strokeWidth = 2.5f * k)
                                    drawLine(color = Color(0xFF38BDF8), start = Offset(mpiPos.x, mpiPos.y - mpiArm), end = Offset(mpiPos.x, mpiPos.y + mpiArm), strokeWidth = 2.5f * k)
                                }

                                // Dessin des impacts avec numérotation
                                val radius = 13f * k
                                impactsList.forEachIndexed { idx, imp ->
                                    val pos = imp.canvasOffset
                                    val isLatest = idx == impactsList.lastIndex
                                    val isBeingDragged = draggedImpactIndexState == idx

                                    if (isBeingDragged) {
                                        // Réticule et halo d'ajustement en temps réel pour l'impact déplacé
                                        val arm = 24f * k
                                        drawCircle(color = Color(0xFFFF3B30).copy(alpha = 0.35f), radius = radius + 9f * k, center = pos)
                                        drawLine(color = Color(0xFFFF3B30), start = Offset(pos.x - arm, pos.y), end = Offset(pos.x + arm, pos.y), strokeWidth = 2f * k)
                                        drawLine(color = Color(0xFFFF3B30), start = Offset(pos.x, pos.y - arm), end = Offset(pos.x, pos.y + arm), strokeWidth = 2f * k)
                                    }

                                    drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = radius + 2f * k, center = Offset(pos.x + 1f * k, pos.y + 1f * k))
                                    val impactColor = when {
                                        imp.isFlyer -> Color(0xFF6B7280) // Flyer grisé
                                        imp.isInnerTen -> Color(0xFF10B981) // Mouche verte
                                        imp.score == 10 -> Color(0xFFEAB308) // 10 doré
                                        isLatest || isBeingDragged -> Color(0xFFFF3B30)
                                        else -> Color(0xFFDC2626)
                                    }
                                    drawCircle(color = impactColor, radius = radius, center = pos)
                                    drawCircle(color = Color.White, radius = radius, center = pos, style = Stroke(width = (if (isBeingDragged) 2.8f else 2f) * k))
                                    if (imp.isAuto) {
                                        // Detected automatically, not confirmed yet
                                        drawCircle(color = Color(0xFF38BDF8), radius = radius + 5f * k, center = pos, style = Stroke(width = 2f * k, pathEffect = autoImpactDash))
                                    }

                                    val numLayout = textMeasurer.measure(
                                        imp.index.toString(),
                                        style = impactNumberStyle
                                    )
                                    drawText(
                                        textLayoutResult = numLayout,
                                        topLeft = Offset(pos.x - numLayout.size.width / 2f, pos.y - numLayout.size.height / 2f)
                                    )
                                }

                                // Dessin du nouvel impact en cours d'ajustement dynamique au doigt
                                pendingImpactOffset?.let { pos ->
                                    val newIdx = impactsList.size + 1
                                    val arm = 24f * k
                                    drawCircle(color = Color(0xFFFF3B30).copy(alpha = 0.35f), radius = radius + 9f * k, center = pos)
                                    drawLine(color = Color(0xFFFF3B30), start = Offset(pos.x - arm, pos.y), end = Offset(pos.x + arm, pos.y), strokeWidth = 2f * k)
                                    drawLine(color = Color(0xFFFF3B30), start = Offset(pos.x, pos.y - arm), end = Offset(pos.x, pos.y + arm), strokeWidth = 2f * k)

                                    val (sc, inner) = pendingImpactScore ?: Pair(10, false)
                                    val impactColor = when {
                                        inner -> Color(0xFF10B981)
                                        sc == 10 -> Color(0xFFEAB308)
                                        else -> Color(0xFFFF3B30)
                                    }
                                    drawCircle(color = impactColor, radius = radius, center = pos)
                                    drawCircle(color = Color.White, radius = radius, center = pos, style = Stroke(width = 2.8f * k))

                                    val numLayout = textMeasurer.measure(
                                        newIdx.toString(),
                                        style = impactNumberStyle
                                    )
                                    drawText(
                                        textLayoutResult = numLayout,
                                        topLeft = Offset(pos.x - numLayout.size.width / 2f, pos.y - numLayout.size.height / 2f)
                                    )
                                }
                            }
                        }

                        // HUD FLOTTANT SUPÉRIEUR : Affiche les instructions et les valeurs en direct lors du glissement
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xDD0F172A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                if (isUserDragging && dragHudText.isNotBlank()) {
                                    Text(
                                        text = dragHudText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                } else {
                                    val modeText = when (activeMode) {
                                        AnnotationMode.CALIBRATE ->
                                            if (calibrationStep == CalibrationStep.SET_CENTER) "Touchez le centre de la cible"
                                            else "Glissez jusqu'au bord du visuel"
                                        AnnotationMode.TARGET_CENTER -> "Touchez ou glissez pour placer le centre"
                                        AnnotationMode.IMPACT -> "Touchez pour placer un impact"
                                    }
                                    Text(
                                        text = modeText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // BADGE INDICATEUR DE ZOOM EN BAS À DROITE
                        if (isZoomed) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xDD000000)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { scale = 1f; offset = Offset.Zero }
                                        .heightIn(min = 44.dp)
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", scale)}× · Recadrer",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    }

                    if (shootingStep == ShootingStep.TARGET) {
                    // ÉTAPE 2 : LA CIBLE (photo, repérage, calibration)
                    Button(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(ActionHeight).testTag("load_photo_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedImageUri == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedImageUri == null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedImageUri != null) "Changer la photo" else "Charger la photo de la cible",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    // Panneau d'assistance guidé à la calibration du visuel
                    if (selectedImageUri != null && !isAnalyzingTarget) RecomposeScope {
                        CalibrationAssistantPanel(
                            selectedReference = selectedCalibrationRef,
                            onSelectReference = { ref ->
                                selectedCalibrationRef = ref
                                if (calibrationEdgePx != null && targetCenterPx != null) {
                                    val rPx = hypot(calibrationEdgePx!!.x - targetCenterPx!!.x, calibrationEdgePx!!.y - targetCenterPx!!.y)
                                    if (rPx > 5f && ref.radiusMm > 0f) {
                                        pixelPerMm = rPx / ref.radiusMm
                                    }
                                }
                            },
                            currentStep = calibrationStep,
                            onStepChange = { step -> calibrationStep = step },
                            pixelPerMm = pixelPerMm,
                            isCenterPlaced = targetCenterPx != null
                        )
                    }


                    StepNavigation(
                        onBack = { goToStep(ShootingStep.SESSION) },
                        nextLabel = if (selectedImageUri == null) "Sans photo" else "Suivant : impacts",
                        nextEnabled = selectedImageUri == null || (pixelPerMm > 0f && !isAnalyzingTarget),
                        onNext = { goToStep(ShootingStep.IMPACTS) },
                        nextTag = "next_to_impacts_button",
                        nextProminent = selectedImageUri != null
                    )
                    if (selectedImageUri != null && pixelPerMm <= 0f && !isAnalyzingTarget) {
                        Text(
                            text = "Réglez le centre et le bord du visuel pour continuer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    } else {
                    // ÉTAPE 3 : LES IMPACTS
                    // 5. IMPACTS ET POINTS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Impacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = if (impactsList.isEmpty()) "Aucun impact placé" else "${impactsList.size} coup${if (impactsList.size > 1) "s" else ""} · touchez-en un pour le corriger",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${totalScore.value}",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        lineHeight = 32.sp
                                    )
                                    Text(
                                        text = if (innerTensCount.value > 0) "points · ${innerTensCount.value} mouche${if (innerTensCount.value > 1) "s" else ""}" else "points",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (impactsList.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(impactsList) { imp ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                imp.isFlyer -> MaterialTheme.colorScheme.surface
                                                imp.isInnerTen -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                imp.score >= 9 -> Color(0xFFFBBF24).copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.surface
                                            },
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (imp.isAuto) 2.dp else 1.dp,
                                                when {
                                                    imp.isAuto -> Color(0xFF38BDF8)
                                                    imp.isInnerTen -> Color(0xFF10B981)
                                                    else -> MaterialTheme.colorScheme.outlineVariant
                                                }
                                            ),
                                            modifier = Modifier
                                                .clickable {
                                                    impactUnderEdit = imp
                                                    showImpactEditDialog = true
                                                }
                                                .testTag("shot_chip_${imp.index}")
                                        ) {
                                            Column(
                                                modifier = Modifier.width(58.dp).padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("n° ${imp.index}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = if (imp.isInnerTen) "10X" else "${imp.score}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 20.sp,
                                                    color = if (imp.isInnerTen) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val autoCount = impactsList.count { it.isAuto }
                            if (autoCount > 0) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF38BDF8).copy(alpha = 0.12f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$autoCount détecté${if (autoCount > 1) "s" else ""} automatiquement, cerclé${if (autoCount > 1) "s" else ""} de bleu : vérifiez-les",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF7DD3FC),
                                            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                                        )
                                        TextButton(
                                            onClick = {
                                                val kept = impactsList.filter { !it.isAuto }.mapIndexed { i, imp -> imp.copy(index = i + 1) }
                                                impactsList.clear()
                                                impactsList.addAll(kept)
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp).testTag("remove_detected_impacts_button")
                                        ) {
                                            Text("Les retirer", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { detectImpactsOnPhoto() },
                                    enabled = selectedImageUri != null && pixelPerMm > 0f && !isDetectingImpacts,
                                    modifier = Modifier.weight(1f).height(52.dp).testTag("detect_impacts_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isDetectingImpacts) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isDetectingImpacts) "Détection…" else "Détecter", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { if (impactsList.isNotEmpty()) impactsList.removeAt(impactsList.lastIndex) },
                                    enabled = impactsList.isNotEmpty(),
                                    modifier = Modifier.weight(1f).height(52.dp).testTag("undo_last_impact_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Annuler", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                    }

                    // 6. GROUPEMENT
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("live_metrics_panel"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Groupement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = "${impactsList.count { !it.isFlyer }} coup${if (impactsList.count { !it.isFlyer } > 1) "s" else ""} retenus à ${distanceMeters.toInt()} m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f", currentStats.moa),
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary,
                                        lineHeight = 32.sp
                                    )
                                    Text("MOA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile("Écart extrême", String.format(Locale.US, "%.1f", currentStats.esMm), "mm")
                                StatTile("H + L", String.format(Locale.US, "%.1f", currentStats.hPlusL), "mm")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile("Hauteur", String.format(Locale.US, "%.1f", currentStats.hMm), "mm")
                                StatTile("Largeur", String.format(Locale.US, "%.1f", currentStats.lMm), "mm")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile("Décalage horizontal", String.format(Locale.US, "%+.1f", currentStats.mpiDeltaXMm), "mm", Color(0xFF38BDF8))
                                StatTile("Décalage vertical", String.format(Locale.US, "%+.1f", currentStats.mpiDeltaYMm), "mm", Color(0xFF38BDF8))
                            }
                        }
                    }

                    // 7. RETOUR / ENREGISTRER
                    StepNavigation(
                        onBack = { goToStep(ShootingStep.TARGET) },
                        nextLabel = if (editingSessionId != null) "Mettre à jour" else "Enregistrer",
                        nextEnabled = historyLoaded,
                        onNext = { saveSession() },
                        nextTag = "save_session_button",
                        nextIcon = Icons.Default.Save
                    )
                    }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Historique",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${savedSessionsList.size} séance${if (savedSessionsList.size > 1) "s" else ""} enregistrée${if (savedSessionsList.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (savedSessionsList.isNotEmpty()) {
                            IconButton(onClick = { showClearHistoryDialog = true }, modifier = Modifier.size(48.dp)) {
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtres de distance
                    val distanceFilterOptions = listOf("Toutes", "10m", "25m", "50m", "100m", "200m+")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(distanceFilterOptions) { f ->
                            FilterChip(
                                selected = historyDistanceFilter == f,
                                onClick = { historyDistanceFilter = f },
                                label = { Text(if (f == "Toutes") "Toutes distances" else f, style = MaterialTheme.typography.labelLarge) },
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                val score = s.optInt("totalScore", 0)
                                val inTens = s.optInt("innerTens", 0)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { loadSessionForRetroactiveAdjustment(s) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = s.optString("title", "Séance"),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = s.optString("date", ""),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                                                Text("$score", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, lineHeight = 28.sp)
                                                Text(
                                                    text = if (inTens > 0) "pts · ${inTens}X" else "points",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = listOf(
                                                "${s.optString("targetType", "C50").substringBefore(" (")} à ${s.optDouble("distanceMeters", 50.0).toInt()} m",
                                                s.optString("weapon", ""),
                                                s.optString("caliber", "")
                                            ).filter { it.isNotBlank() }.joinToString(" · "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            HistoryPill("${String.format(Locale.US, "%.2f", s.optDouble("moa", 0.0))} MOA", MaterialTheme.colorScheme.secondary)
                                            HistoryPill("ES ${String.format(Locale.US, "%.1f", s.optDouble("esMm", 0.0))} mm", MaterialTheme.colorScheme.onSurfaceVariant)
                                            HistoryPill("${s.optInt("shotsCount", 0)} coups", MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Corriger la séance",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
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

    // DIALOG: Vider l'historique
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Vider l'historique ?") },
            text = { Text("Les ${savedSessionsList.size} séances enregistrées seront supprimées définitivement.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveHistory(emptyList())
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Historique vidé.", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Tout supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("Annuler") } }
        )
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
                                                LocalJsonStore.saveWeapons(context, weaponsList.toList())
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
                            LocalJsonStore.saveWeapons(context, weaponsList.toList())
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

/**
 * Gives [content] its own recomposition scope, so state read inside it does not invalidate the
 * (inline) layouts of the caller.
 */
@Composable
private fun RecomposeScope(content: @Composable () -> Unit) = content()

@Composable
private fun HistoryPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
/** Back and forward buttons at the bottom of a step. */
@Composable
private fun StepNavigation(
    onBack: () -> Unit,
    nextLabel: String,
    nextEnabled: Boolean,
    onNext: () -> Unit,
    nextTag: String,
    nextIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    // False when another button on the screen is the main action.
    nextProminent: Boolean = true
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.height(ActionHeight),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retour", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.weight(1f).height(ActionHeight).testTag(nextTag),
            shape = RoundedCornerShape(14.dp),
            colors = if (nextProminent) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(nextLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = nextIcon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

/** What happened to the photo on the target step, in one card. */
@Composable
private fun TargetStatusCard(
    hasPhoto: Boolean,
    isAnalyzing: Boolean,
    result: TargetDetectionResult?,
    isCalibrated: Boolean,
    onRetry: () -> Unit
) {
    val (title, body, color) = when {
        !hasPhoto -> Triple(
            "Photographiez votre cible",
            "Le centre et l'échelle seront repérés automatiquement. Sans photo, vous placerez les impacts à la main.",
            MaterialTheme.colorScheme.onSurface
        )
        isAnalyzing -> Triple("Repérage de la cible…", "Recherche du visuel noir sur la photo.", MaterialTheme.colorScheme.secondary)
        result?.visualFound == true && isCalibrated -> Triple(
            "Cible repérée",
            "Vérifiez que le cercle jaune suit le bord du visuel noir et que la croix verte est au centre. Corrigez-les au doigt si besoin.",
            Color(0xFF10B981)
        )
        isCalibrated -> Triple("Cible réglée", "Le centre et l'échelle sont réglés.", Color(0xFF10B981))
        else -> Triple(
            "Cible non repérée",
            (result?.description ?: "Le visuel noir n'a pas été trouvé.") + " Réglez le centre et le bord à la main ci-dessous.",
            MaterialTheme.colorScheme.secondary
        )
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().testTag("target_status")) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = color)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hasPhoto && !isAnalyzing && result?.visualFound != true) {
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Réessayer", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
