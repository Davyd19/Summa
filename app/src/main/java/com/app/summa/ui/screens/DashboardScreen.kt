package com.app.summa.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.model.HabitItem
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (HabitItem) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToReflections: () -> Unit = {},
    onNavigateToIdentityProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ModernHeader(
                    greeting = uiState.greeting,
                    currentMode = currentMode,
                    onModeClick = { showModeDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Points Card
                    ModernPointsCard(
                        progress = uiState.todayProgress,
                        points = uiState.summaPoints,
                        modifier = Modifier
                            .weight(1.4f)
                            .clickable { onNavigateToIdentityProfile() }
                    )

                    // Paperclip Card
                    ModernPaperclipCard(
                        count = uiState.totalPaperclips,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // PERBAIKAN: Tampilkan Next Action CARD bahkan di Mode Fokus!
            // Ini inti dari fokus: mengetahui "apa selanjutnya".
            item {
                ModernNextActionCard(
                    task = uiState.nextTask,
                    onStartFocus = { /* TODO: Trigger UniversalFocusMode here if needed */ },
                    onAddTask = onNavigateToPlanner,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Habit hanya muncul jika bukan mode Fokus (agar tidak distraksi)
            if (currentMode != "Fokus") {
                item {
                    ModernSectionHeader(
                        title = "Kebiasaan Hari Ini",
                        trailing = "${uiState.todayHabits.count { it.currentCount >= it.targetCount }}/${uiState.todayHabits.size}",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                if (uiState.todayHabits.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(100.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada kebiasaan terjadwal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            items(uiState.todayHabits.size) { index ->
                                ModernHabitCard(
                                    habit = uiState.todayHabits[index],
                                    onClick = { onNavigateToHabitDetail(uiState.todayHabits[index]) }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access hanya di Normal
            if (currentMode == "Normal") {
                item {
                    ModernSectionHeader(
                        title = "Akses Cepat",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item {
                    ModernQuickAccessGrid(
                        totalNetWorth = uiState.totalNetWorth,
                        onMoneyClick = onNavigateToMoney,
                        onNotesClick = onNavigateToNotes,
                        onReflectionClick = onNavigateToReflections,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showModeDialog) {
        ModernModeDialog(
            currentMode = currentMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                onModeSelected(it)
                showModeDialog = false
            }
        )
    }
}

// KOMPONEN BARU: Kartu Penjepit Kertas
@Composable
fun ModernPaperclipCard(
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(180.dp), // Samakan tinggi visual dengan Points Card
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("📎", style = MaterialTheme.typography.titleMedium)
            }

            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Klip Fokus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ... Sisanya (ModernPointsCard, QuickAccess, dll) tetap sama, hanya ModernPointsCard perlu sedikit penyesuaian tinggi jika ingin sejajar sempurna,
// tapi saya biarkan flexibel dulu.

// --- PERBAIKAN TAMPILAN AKSES CEPAT (Sama seperti sebelumnya) ---

@Composable
fun ModernQuickAccessGrid(
    totalNetWorth: Double,
    onMoneyClick: () -> Unit,
    onNotesClick: () -> Unit,
    onReflectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernQuickAccessCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Keuangan",
                subtitle = formatter.format(totalNetWorth),
                color = BlueAccent,
                onClick = onMoneyClick,
                modifier = Modifier.weight(1f)
            )
            ModernQuickAccessCard(
                icon = Icons.Default.Book,
                title = "Pustaka",
                subtitle = "Ide & Catatan",
                color = PurpleAccent,
                onClick = onNotesClick,
                modifier = Modifier.weight(1f)
            )
        }
        ModernQuickAccessCard(
            icon = Icons.Default.RateReview,
            title = "Tinjauan Harian",
            subtitle = "Refleksi & evaluasi hari ini",
            color = PinkAccent,
            onClick = onReflectionClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ModernQuickAccessCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f * (1 - offset * 0.5f)),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f * offset),
                        Color.Transparent
                    )
                )
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHeader(
    greeting: String,
    currentMode: String,
    onModeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp)
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.fadeIn(tween(800)) +
                    androidx.compose.animation.slideInVertically(tween(800)) { -20 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$greeting 👋",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mari mulai hari produktif",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Surface(
                        onClick = onModeClick,
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                currentMode,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernPointsCard(
    progress: Float,
    points: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    val animatedPoints by animateIntAsState(
        targetValue = points,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "points"
    )

    // Ubah tinggi menjadi 180.dp agar sejajar dengan card paperclip
    Card(
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        )
    ) {
        Box {
            val scale by rememberInfiniteTransition(label = "circle").animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = 100.dp, y = (-50).dp) // Sesuaikan offset agar fit di card yang lebih sempit
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Summa Points",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        animatedPoints.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mini Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Target",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun ModernNextActionCard(
    task: com.app.summa.data.model.Task?,
    onStartFocus: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = if (task != null) {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            )
        )
    }

    Card(
        onClick = if (task != null) onStartFocus else onAddTask,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val scale by rememberInfiniteTransition(label = "icon").animateFloat(
                    initialValue = 1f,
                    targetValue = if (task != null) 1.15f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            if (task != null)
                                MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (task != null) Icons.Default.PlayArrow else Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (task != null) "Selanjutnya" else "Mulai Hari",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        task?.title ?: "Tambah tugas pertama",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task?.scheduledTime != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                task.scheduledTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Mulai",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ModernHabitCard(
    habit: HabitItem,
    onClick: () -> Unit
) {
    val isComplete = habit.currentCount >= habit.targetCount && habit.targetCount > 0
    val progressPercentage = if (habit.targetCount > 0) {
        (habit.currentCount.toFloat() / habit.targetCount).coerceAtMost(1f)
    } else 0f

    val scale by animateFloatAsState(
        targetValue = if (isComplete) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .scale(scale)
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                SuccessGreen.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isComplete) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isComplete) {
                                    listOf(SuccessGreen.copy(alpha = 0.2f), SuccessGreen.copy(alpha = 0.1f))
                                } else {
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        habit.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                if (habit.currentStreak > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StreakOrange.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${habit.currentStreak}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StreakOrange
                            )
                        }
                    }
                }
            }

            Text(
                habit.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (habit.targetCount > 1) "${habit.currentCount}/${habit.targetCount}"
                        else "${habit.currentCount}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) SuccessGreen
                        else MaterialTheme.colorScheme.primary
                    )
                    if (isComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selesai",
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isComplete) SuccessGreen
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun ModernSectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SuccessGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    trailing,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }
        }
    }
}

@Composable
fun ModernModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Mode Kontekstual",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("Normal", "Tampilan lengkap semua fitur", Icons.Default.Dashboard),
                    Triple("Fokus", "Hanya tugas prioritas", Icons.Default.TipsAndUpdates),
                    Triple("Pagi", "Rutinitas pagi hari", Icons.Default.WbSunny)
                ).forEach { (mode, desc, icon) ->
                    ModernModeOptionCard(
                        title = mode,
                        description = desc,
                        icon = icon,
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun ModernModeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}