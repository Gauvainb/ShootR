package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Ammo
import com.example.Weapon
import java.util.Locale

/** What the home screen shows of the saved sessions. */
data class HomeSummary(
    val sessionsCount: Int,
    val lastTitle: String?,
    val lastDate: String?,
    val lastScore: Int?,
    val lastMoa: Double?,
    val bestMoa: Double?
)

/** Entry point of the app: one large button per section. */
@Composable
fun HomeScreen(
    summary: HomeSummary,
    weaponsCount: Int,
    ammoCount: Int,
    sessionInProgress: String?,
    onResume: () -> Unit,
    onNewSession: () -> Unit,
    onHistory: () -> Unit,
    onArmory: () -> Unit,
    onAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Bienvenue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                text = when (summary.sessionsCount) {
                    0 -> "Aucune séance enregistrée pour l'instant"
                    1 -> "1 séance enregistrée"
                    else -> "${summary.sessionsCount} séances enregistrées"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (sessionInProgress != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onResume).testTag("home_resume")
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reprendre la séance en cours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(sessionInProgress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Main action, full width.
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(96.dp).clickable(onClick = onNewSession).testTag("home_new_session")
        ) {
            Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Nouvelle séance", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Séance, photo de la cible, impacts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile(
                title = "Historique",
                subtitle = if (summary.sessionsCount == 0) "Vide" else "${summary.sessionsCount} séance${if (summary.sessionsCount > 1) "s" else ""}",
                icon = Icons.Default.History,
                accent = Color(0xFF38BDF8),
                onClick = onHistory,
                modifier = Modifier.weight(1f).testTag("home_history")
            )
            HomeTile(
                title = "Râtelier",
                subtitle = "$weaponsCount arme${if (weaponsCount > 1) "s" else ""} · $ammoCount munition${if (ammoCount > 1) "s" else ""}",
                icon = Icons.Default.Inventory2,
                accent = Color(0xFFFBBF24),
                onClick = onArmory,
                modifier = Modifier.weight(1f).testTag("home_armory")
            )
        }
        HomeTile(
            title = "Analyses",
            subtitle = summary.bestMoa?.let { "Meilleur groupement : ${String.format(Locale.US, "%.2f", it)} MOA" } ?: "Évolution de la précision",
            icon = Icons.Default.AutoGraph,
            accent = Color(0xFF10B981),
            onClick = onAnalysis,
            modifier = Modifier.fillMaxWidth().testTag("home_analysis")
        )

        if (summary.lastTitle != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onHistory)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dernière séance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(summary.lastTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(summary.lastDate.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${summary.lastScore ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        summary.lastMoa?.let {
                            Text("${String.format(Locale.US, "%.2f", it)} MOA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.heightIn(min = 112.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Title block shared by the section screens. */
@Composable
fun ScreenTitle(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** The weapons and ammunition the user owns, with a tab for each. */
@Composable
fun ArmoryScreen(
    weapons: List<Weapon>,
    ammo: List<Ammo>,
    onAddWeapon: () -> Unit,
    onDeleteWeapon: (Weapon) -> Unit,
    onAddAmmo: () -> Unit,
    onDeleteAmmo: (Ammo) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAmmo by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).testTag("armory_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTitle("Râtelier", "Vos armes et munitions, pour pré-remplir une séance")

        ModeSelector(
            options = listOf(
                ModeOption(false, "Armes (${weapons.size})", Icons.Default.Shield, Color(0xFFFBBF24)),
                ModeOption(true, "Munitions (${ammo.size})", Icons.Default.Inventory2, Color(0xFF38BDF8))
            ),
            selected = showAmmo,
            onSelect = { showAmmo = it }
        )

        Button(
            onClick = if (showAmmo) onAddAmmo else onAddWeapon,
            modifier = Modifier.fillMaxWidth().height(ActionHeight).testTag(if (showAmmo) "add_ammo_button" else "add_weapon_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showAmmo) "Ajouter une munition" else "Ajouter une arme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        val empty = if (showAmmo) ammo.isEmpty() else weapons.isEmpty()
        if (empty) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    if (showAmmo) "Aucune munition enregistrée." else "Aucune arme enregistrée.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                if (showAmmo) {
                    items(ammo, key = { it.id }) { a ->
                        ArmoryRow(
                            title = a.name,
                            tags = listOf(a.caliber),
                            detail = a.notes,
                            onDelete = { onDeleteAmmo(a) }
                        )
                    }
                } else {
                    items(weapons, key = { it.id }) { w ->
                        ArmoryRow(
                            title = w.name,
                            tags = listOf(w.type, w.caliber),
                            detail = w.optics,
                            onDelete = { onDeleteWeapon(w) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArmoryRow(title: String, tags: List<String>, detail: String, onDelete: (() -> Unit)?, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.filter { it.isNotBlank() }.forEachIndexed { i, tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (i == tags.lastIndex) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (i == tags.lastIndex) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** Precision over time: MOA per session, with a few headline figures. */
@Composable
fun AnalysisScreen(
    sessions: List<SessionChartData>,
    modifier: Modifier = Modifier
) {
    var weaponFilter by rememberSaveable { mutableStateOf("Toutes") }
    var chartType by rememberSaveable { mutableStateOf(ChartDisplayType.LINE) }
    val weapons = listOf("Toutes") + sessions.map { it.weapon }.distinct()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("analysis_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTitle("Analyses", "Évolution de la précision (MOA) au fil des séances, ${sessions.size} séance${if (sessions.size > 1) "s" else ""}")

        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = chartType == ChartDisplayType.LINE,
                        onClick = { chartType = ChartDisplayType.LINE },
                        label = { Text("Courbe") },
                        leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.height(40.dp)
                    )
                    FilterChip(
                        selected = chartType == ChartDisplayType.HISTOGRAM,
                        onClick = { chartType = ChartDisplayType.HISTOGRAM },
                        label = { Text("Histogramme") },
                        leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.height(40.dp)
                    )
                }
                Text("Arme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(weapons) { w ->
                        FilterChip(
                            selected = weaponFilter == w,
                            onClick = { weaponFilter = w },
                            label = { Text(w) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
                MoaProgressionChart(sessions = sessions, selectedWeapon = weaponFilter, chartType = chartType)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Bottom bar: the four sections around a round home button. */
@Composable
fun AppBottomBar(
    current: Int,
    onSelect: (Int) -> Unit,
    historyBadge: Int,
    modifier: Modifier = Modifier
) {
    // Index 2 is home.
    data class Entry(val label: String, val icon: ImageVector, val tag: String)
    val entries = listOf(
        Entry("Séance", Icons.Default.AddCircle, "nav_session"),
        Entry("Historique", Icons.Default.History, "nav_history"),
        Entry("Accueil", Icons.Default.Home, "nav_home"),
        Entry("Râtelier", Icons.Default.Inventory2, "nav_armory"),
        Entry("Analyses", Icons.Default.AutoGraph, "nav_analysis")
    )
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEachIndexed { i, e ->
                val selected = i == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(i) }
                        .testTag(e.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (i == 2) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(e.icon, contentDescription = e.label, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Box {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
                            ) {
                                Icon(e.icon, contentDescription = null, tint = tint, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp).size(24.dp))
                            }
                            if (i == 1 && historyBadge > 0) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (historyBadge > 99) "99" else "$historyBadge", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            e.label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
