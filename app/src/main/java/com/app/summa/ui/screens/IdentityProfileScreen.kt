package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.ui.components.BrutalFab
import com.app.summa.ui.components.BrutalIconAction
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.IdentityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityProfileScreen(
    viewModel: IdentityViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Jika ada identitas terpilih, tampilkan Detail Sheet
    if (uiState.selectedIdentity != null) {
        IdentityDetailSheet(
            identity = uiState.selectedIdentity!!,
            xpToNext = uiState.xpToNextLevel,
            logs = uiState.selectedIdentityLogs,
            onDismiss = { viewModel.clearSelection() }
        )
    }

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "Profil Karakter",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        },
        floatingActionButton = {
            BrutalFab(
                onClick = { showAddDialog = true },
                icon = Icons.Default.Add,
                contentDescription = "Tambah Identitas"
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Header Level
                item {
                    ProfileHeader(totalLevel = uiState.totalLevel)
                }

                // 2. Radar Chart
                item {
                    if (uiState.identities.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RadarChart(
                                identities = uiState.identities,
                                modifier = Modifier.size(300.dp)
                            )
                        }
                    } else {
                        EmptyIdentityState()
                    }
                }

                // 3. Daftar Identitas (Sekarang Clickable)
                item {
                    Text(
                        "Atribut Identitas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(uiState.identities) { identity ->
                    BrutalistIdentityStatCard(
                        identity = identity,
                        onClick = { viewModel.selectIdentity(identity) } // KLIK UNTUK DETAIL
                    )
                }

                // 4. Jejak Bukti Global
                item {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HistoryEdu, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Aktivitas Terbaru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.recentActivityLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                                .height(100.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada aktivitas.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(uiState.recentActivityLogs) { log ->
                        BrutalistEvidenceItem(log)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddIdentityDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, desc ->
                viewModel.addIdentity(name, desc)
                showAddDialog = false
            }
        )
    }
}

// --- SHEET DETAIL IDENTITAS (CHARACTER SHEET) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityDetailSheet(
    identity: Identity,
    xpToNext: Int,
    logs: List<KnowledgeNote>,
    onDismiss: () -> Unit
) {
    val level = identity.progress / 100
    val currentXp = identity.progress % 100
    val progress = currentXp / 100f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Icon Besar & Nama
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .brutalBorder(cornerRadius = 100.dp, strokeWidth = 3.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    identity.name.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(identity.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (identity.description.isNotBlank()) {
                Text(
                    identity.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Level & Progress Bar Besar
            BrutalistCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("LEVEL $level", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = GoldDark)
                        Text("${currentXp}/100 XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    BrutalistProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$xpToNext XP lagi untuk naik level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Riwayat Bukti Spesifik
            Text(
                "Riwayat Bukti (Evidence)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Text(
                    "Belum ada bukti spesifik untuk identitas ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(logs) { log ->
                        BrutalistEvidenceItem(log)
                    }
                }
            }
        }
    }
}

// --- DIALOG BARU: Add Identity ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIdentityDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Identitas Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Siapa yang ingin Anda jadi?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Identitas") },
                    placeholder = { Text("Contoh: Musisi, Orang Sehat") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi/Mantra (Opsional)") },
                    placeholder = { Text("Contoh: Saya menciptakan musik setiap hari") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, description) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ... (Komponen lain seperti EvidenceTimelineItem, ProfileHeader, RadarChart, dll. tetap sama)
@Composable
fun EvidenceTimelineItem(note: KnowledgeNote) {
    val identityName = if (note.title.startsWith("Bukti: ")) {
        note.title.removePrefix("Bukti: ")
    } else if (note.title.startsWith("Jurnal Identitas: ")) {
        note.title.removePrefix("Jurnal Identitas: ")
    } else {
        "Aktivitas"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .padding(top = 4.dp)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }

        Spacer(Modifier.width(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        identityName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = GoldAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "+XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.createdAt ?: 0)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyIdentityState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PersonOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Belum ada identitas terbentuk", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("Buat kebiasaan baru untuk memulai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ProfileHeader(totalLevel: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .brutalBorder(cornerRadius = 100.dp, strokeWidth = 3.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepTeal, TealLight)
                        ), CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "YOU",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = GoldAccent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .brutalBorder(cornerRadius = 4.dp),
                shadowElevation = 0.dp
            ) {
                Text(
                    "LVL $totalLevel",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun RadarChart(identities: List<Identity>, modifier: Modifier = Modifier) {
    // ... (Keep existing implementation logic but ensure colors match)
    val labels = identities.map { it.name }
    val maxVal = 1000f
    val values = identities.map { min(it.progress.toFloat(), maxVal) / maxVal }
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val cornerRadius = size.width / 2 * 0.8f
        val stepAngle = 360f / values.size

        for (i in 1..4) {
            val r = radius * (i / 4f)
            val path = Path()
            for (j in values.indices) {
                val angle = Math.toRadians((stepAngle * j - 90).toDouble())
                val x = center.x + r * cos(angle).toFloat()
                val y = center.y + r * sin(angle).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color = surfaceColor.copy(alpha = 0.1f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)) // Thicker stroke
        }

        val dataPath = Path()
        values.forEachIndexed { index, value ->
            val r = radius * value
            val angle = Math.toRadians((stepAngle * index - 90).toDouble())
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            drawCircle(color = primaryColor, cornerRadius = 6.dp.toPx(), center = Offset(x, y)) // Bigger dots
        }
        dataPath.close()
        drawPath(path = dataPath, color = primaryColor.copy(alpha = 0.3f), style = Fill)
        drawPath(path = dataPath, color = primaryColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)) // Thicker stroke

        values.indices.forEach { index ->
            val angle = Math.toRadians((stepAngle * index - 90).toDouble())
            val endX = center.x + radius * cos(angle).toFloat()
            val endY = center.y + radius * sin(angle).toFloat()
            drawLine(color = surfaceColor.copy(alpha = 0.1f), start = center, end = Offset(endX, endY), strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
fun BrutalistIdentityStatCard(
    identity: Identity,
    onClick: () -> Unit // Parameter Baru
) {
    val level = identity.progress / 100
    val currentXp = identity.progress % 100
    val progress = currentXp / 100f

    BrutalistCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    identity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "LVL $level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = GoldDark
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrutalistProgressBar(
                    progress = progress,
                    modifier = Modifier.weight(1f).height(12.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "$currentXp / 100 XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BrutalistEvidenceItem(note: KnowledgeNote) {
    val identityName = if (note.title.startsWith("Bukti: ")) {
        note.title.removePrefix("Bukti: ")
    } else if (note.title.startsWith("Jurnal Identitas: ")) {
        note.title.removePrefix("Jurnal Identitas: ")
    } else {
        "Aktivitas"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .brutalBorder(cornerRadius =12.dp, strokeWidth=1.dp)
                    .background(GoldAccent, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface)
            )
        }

        Spacer(Modifier.width(12.dp))

        BrutalistCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        identityName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = GoldAccent,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.brutalBorder(cornerRadius =4.dp, strokeWidth=1.dp)
                    ) {
                        Text(
                            "+XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.createdAt ?: 0)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyIdentityBox() {
    BrutalistCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.PersonOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("BELUM ADA IDENTITAS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
            Text("Buat kebiasaan baru untuk memulai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

// ... (IdentityDetailSheet & AddIdentityDialog need updates too but for now linking usages)


