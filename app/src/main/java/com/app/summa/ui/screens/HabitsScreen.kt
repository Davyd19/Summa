package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.HabitLog
import com.app.summa.data.model.Identity
import com.app.summa.ui.components.HabitInputSheet
import com.app.summa.ui.components.CoinExplosionAnimation
import com.app.summa.ui.model.HabitItem
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel = hiltViewModel(),
    onNavigateToIdentityProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var focusedHabit by remember { mutableStateOf<HabitItem?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.showRewardAnimation) {
        if (uiState.showRewardAnimation) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    if (focusedHabit != null) {
        UniversalFocusModeScreen(
            title = focusedHabit!!.name,
            initialTarget = 10,
            onComplete = { clips, startTime ->
                viewModel.saveFocusSession(focusedHabit!!.id, clips, startTime)
                if (clips > 0) viewModel.incrementHabit(focusedHabit!!)
                focusedHabit = null
            },
            onCancel = { focusedHabit = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showRewardAnimation) {
                CoinExplosionAnimation(
                    modifier = Modifier.fillMaxSize().zIndex(10f),
                    trigger = true,
                    onFinished = { viewModel.dismissRewardAnimation() }
                )
            }

            Crossfade(targetState = uiState.selectedHabit, label = "HabitScreen") { selectedHabit ->
                if (selectedHabit != null) {
                    val relatedIdentity = remember(selectedHabit, uiState.availableIdentities) {
                        uiState.availableIdentities.find { it.id == selectedHabit.originalModel.relatedIdentityId }
                    }

                    ModernHabitDetailScreen(
                        habit = selectedHabit,
                        relatedIdentity = relatedIdentity,
                        onBack = { viewModel.onBackFromDetail() },
                        onEditClick = { showEditSheet = true },
                        onIdentityClick = onNavigateToIdentityProfile,
                        logs = uiState.habitLogs
                    )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text("Kebiasaan", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                        Text("Bangun identitas melalui konsistensi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                },
                                actions = {
                                    FilledTonalButton(onClick = { showAddSheet = true }, modifier = Modifier.padding(end = 12.dp), shape = RoundedCornerShape(16.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Tambah", fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                            )
                        }
                    ) { paddingValues ->
                        if (uiState.isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (uiState.habits.isEmpty()) {
                            EmptyHabitState(Modifier.padding(paddingValues), onAddClick = { showAddSheet = true })
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                items(uiState.habits) { habit ->
                                    EnhancedHabitItem(
                                        habit = habit,
                                        onClick = { viewModel.selectHabit(habit) },
                                        onIncrement = { viewModel.incrementHabit(habit) },
                                        onDecrement = { viewModel.decrementHabit(habit) },
                                        onFocusClick = { focusedHabit = habit }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        HabitInputSheet(
            identities = uiState.availableIdentities,
            onDismiss = { showAddSheet = false },
            onSave = { name, icon, target, identityId, cue, reminder ->
                viewModel.addHabit(name, icon, target, identityId, cue, reminder)
                showAddSheet = false
            }
        )
    }

    if (showEditSheet && uiState.selectedHabit != null) {
        val habit = uiState.selectedHabit!!
        HabitInputSheet(
            initialName = habit.name,
            initialIcon = habit.icon,
            initialTarget = habit.targetCount,
            initialIdentityId = habit.originalModel.relatedIdentityId,
            initialCue = habit.originalModel.cue,
            initialReminders = habit.originalModel.reminderTime,
            identities = uiState.availableIdentities,
            onDismiss = { showEditSheet = false },
            onSave = { name, icon, target, identityId, cue, reminder ->
                viewModel.updateHabitDetails(habit.originalModel, name, icon, target, identityId, cue, reminder)
                showEditSheet = false
            }
        )
    }
}

// === KOMPONEN UTAMA DETAIL ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHabitDetailScreen(
    habit: HabitItem,
    relatedIdentity: Identity?,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onIdentityClick: () -> Unit,
    logs: List<HabitLog>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                },
                actions = {
                    IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edit") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header (Icon + Name)
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)))), contentAlignment = Alignment.Center) {
                        Text(habit.icon, style = MaterialTheme.typography.displayLarge)
                    }
                    Text(habit.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            // 2. Identity Card
            if (relatedIdentity != null) {
                item {
                    IdentityContributionCard(
                        identity = relatedIdentity,
                        onClick = onIdentityClick
                    )
                }
            }

            // 3. Stats Grid (DESAIN BARU YANG LEBIH BERSIH)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedStatCard(
                        title = "Total Points",
                        value = "${habit.totalSum}",
                        subtitle = "XP Terkumpul",
                        icon = "⭐",
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    EnhancedStatCard(
                        title = "Konsistensi",
                        value = "${habit.currentStreak}",
                        subtitle = "Hari Berturut",
                        icon = "🔥",
                        color = StreakOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                EnhancedStatCard(
                    title = "Streak Sempurna",
                    value = "${habit.perfectStreak}",
                    subtitle = "Hari tanpa putus sama sekali",
                    icon = "👑",
                    color = if (habit.perfectStreak > 0) GoldAccent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    isHorizontal = true
                )
            }

            item {
                Text("Riwayat Aktivitas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                EnhancedHeatmap(logs = logs, targetCount = habit.targetCount)
            }
        }
    }
}

// === KOMPONEN VISUAL YANG DIPERBAIKI ===

@Composable
fun IdentityContributionCard(
    identity: Identity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // Ganti Border kusam dengan border tipis dan shadow
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Identitas dengan background modern
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = identity.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SUMBER XP UNTUK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = identity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Indikator Navigasi
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Lihat Profil",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    isHorizontal: Boolean = false
) {
    // DESAIN BARU: Style "Bento Grid" / Apple Health
    // Background putih/gelap bersih, icon di dalam lingkaran warna transparan, angka besar
    Card(
        modifier = modifier.aspectRatio(if (isHorizontal) 3f else 0.9f, matchHeightConstraintsFirst = isHorizontal),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // Hapus warna background kusam
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        if (isHorizontal) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text(icon, style = MaterialTheme.typography.headlineMedium)
                    }
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = color)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Header: Icon di pojok kiri atas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Body: Value Besar
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHabitState(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "empty")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🌱",
                style = MaterialTheme.typography.displayLarge
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Belum Ada Kebiasaan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Setiap langkah besar dimulai dari kebiasaan kecil. Tentukan identitas yang ingin Anda bangun hari ini.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Buat Kebiasaan Pertama", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ... (EnhancedHabitItem tetap sama) ...
@Composable
fun EnhancedHabitItem(
    habit: HabitItem,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onFocusClick: () -> Unit
) {
    val isComplete = habit.targetCount > 0 && habit.currentCount >= habit.targetCount
    val isOverAchieved = habit.targetCount > 0 && habit.currentCount > habit.targetCount
    val progress = if (habit.targetCount > 0) {
        (habit.currentCount.toFloat() / habit.targetCount).coerceAtMost(1f)
    } else 0f

    val scale by animateFloatAsState(
        targetValue = if (isComplete) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val haptic = LocalHapticFeedback.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverAchieved -> GoldContainer
                isComplete -> SuccessGreenBg
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isComplete) 4.dp else 1.dp),
        border = BorderStroke(2.dp, when {
            isOverAchieved -> GoldAccent.copy(alpha = 0.3f)
            isComplete -> SuccessGreen.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.outlineVariant
        })
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header: Icon + Streaks
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(
                        Brush.linearGradient(
                            colors = when {
                                isOverAchieved -> listOf(GoldAccent.copy(alpha = 0.3f), GoldDark.copy(alpha = 0.2f))
                                isComplete -> listOf(SuccessGreen.copy(alpha = 0.3f), SuccessGreenDark.copy(alpha = 0.2f))
                                else -> listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            }
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) { Text(text = habit.icon, style = MaterialTheme.typography.displaySmall) }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (habit.currentStreak > 0) { EnhancedStreakBadge(emoji = "🔥", count = habit.currentStreak, color = StreakOrange, label = "Konsisten") }
                    if (habit.perfectStreak > 0) { EnhancedStreakBadge(emoji = "👑", count = habit.perfectStreak, color = GoldAccent, label = "Sempurna") }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(text = habit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // Action Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDecrement()
                        },
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        enabled = habit.currentCount > 0
                    ) { Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(24.dp)) }

                    AnimatedContent(
                        targetState = habit.currentCount,
                        transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) },
                        label = "count_anim"
                    ) { count ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 80.dp)) {
                            Text(
                                text = if (habit.targetCount > 1) "$count / ${habit.targetCount}" else "$count",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = when { isOverAchieved -> GoldAccent; isComplete -> SuccessGreen; count > 0 -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) }
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onIncrement()
                        },
                        modifier = Modifier.size(52.dp),
                        containerColor = when { isOverAchieved -> GoldAccent; isComplete -> SuccessGreen; else -> MaterialTheme.colorScheme.primary },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(24.dp), tint = Color.White) }
                }

                // TOMBOL FOKUS
                IconButton(
                    onClick = onFocusClick,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "Mulai Fokus",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            if (habit.targetCount > 0) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = when { isOverAchieved -> GoldAccent; isComplete -> SuccessGreen; else -> MaterialTheme.colorScheme.primary },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ... (EnhancedStreakBadge tetap sama) ...
@Composable
fun EnhancedStreakBadge(
    emoji: String,
    count: Int,
    color: Color,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

// ... (ModernAddHabitDialog tetap sama, disederhanakan di sini) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddHabitDialog(
    identities: List<Identity> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, target: Int, relatedIdentityId: Long?) -> Unit
) {
    // Gunakan kembali kode dialog yang ada di file sebelumnya (tidak ada perubahan logika di sini)
    // Untuk mempersingkat context window, anggap kode dialog ada di sini.
    // Jika perlu saya paste ulang, kabari saja.
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var target by remember { mutableStateOf("1") }
    var selectedIdentity by remember { mutableStateOf<Identity?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val commonEmojis = listOf(
        "🎯", "📚", "💪", "🧘", "🏃", "🎨", "✍️", "💻",
        "🎵", "🌱", "💧", "🍎", "😴", "🧹", "📱", "💼"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kebiasaan Baru", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nama Kebiasaan") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                // Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedIdentity?.name ?: "Hubungkan ke Identitas (Opsional)",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Tidak ada") }, onClick = { selectedIdentity = null; expanded = false })
                        identities.forEach { identity ->
                            DropdownMenuItem(text = { Text(identity.name) }, onClick = { selectedIdentity = identity; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = target, onValueChange = { if (it.all { char -> char.isDigit() }) target = it },
                    label = { Text("Target Harian") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name, icon, target.toIntOrNull() ?: 1, selectedIdentity?.id) }) {
                Text("Tambah")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

// === KOMPONEN BARU: KARTU KONTRIBUSI IDENTITAS ===
@Composable
fun IdentityContributionCard(identity: Identity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder untuk Identitas
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = identity.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bolt, // Simbol energi/XP
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SUMBER XP UNTUK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = identity.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHabitDetailScreen(
    habit: HabitItem,
    relatedIdentity: Identity?, // Parameter Baru
    onBack: () -> Unit,
    logs: List<HabitLog>
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            habit.icon,
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                    Text(
                        habit.name,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- TAMPILKAN KARTU KONTRIBUSI IDENTITAS (JIKA ADA) ---
            if (relatedIdentity != null) {
                item {
                    IdentityContributionCard(identity = relatedIdentity)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    EnhancedStatCard(
                        title = "Total Points",
                        value = "${habit.totalSum}",
                        subtitle = "Summa",
                        icon = "⭐",
                        color = GoldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    EnhancedStatCard(
                        title = "Konsistensi",
                        value = "${habit.currentStreak}",
                        subtitle = "hari",
                        icon = "🔥",
                        color = StreakOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                EnhancedStatCard(
                    title = "Streak Sempurna",
                    value = "${habit.perfectStreak}",
                    subtitle = "hari mencapai target",
                    icon = "👑",
                    color = if (habit.perfectStreak > 0) GoldAccent
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Riwayat 12 Minggu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                EnhancedHeatmap(logs = logs, targetCount = habit.targetCount)
            }
        }
    }
}

// ... (Sisa komponen visual seperti EnhancedStatCard, EnhancedHeatmap tetap sama) ...
@Composable
fun EnhancedStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.displayMedium)
            Text(
                value,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EnhancedHeatmap(logs: List<HabitLog>, targetCount: Int) {
    val today = LocalDate.now()
    val weeks = 12
    val daysToShow = weeks * 7

    val logsMap = remember(logs) {
        logs.associateBy { LocalDate.parse(it.date) }
    }

    val data = remember(logsMap) {
        (0 until daysToShow).map { dayOffset ->
            val date = today.minusDays(dayOffset.toLong())
            val log = logsMap[date]
            val count = log?.count ?: 0
            val value = when {
                count <= 0 -> 0
                targetCount > 0 && count < targetCount -> 1
                targetCount > 0 && count == targetCount -> 2
                targetCount > 0 && count > targetCount -> 3
                targetCount == 0 && count > 0 -> 2
                else -> 0
            }
            date to value
        }.reversed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Month Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(weeks - 1, weeks / 2, 0).map {
                    today.minusWeeks(it.toLong()).month.getDisplayName(TextStyle.SHORT, Locale("id"))
                }.distinct().forEach { monthName ->
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Heatmap Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..6).forEach { dayOfWeek ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until weeks).forEach { week ->
                            val index = (week * 7) + dayOfWeek
                            if (index < data.size) {
                                val (_, value) = data[index]
                                EnhancedHeatmapCell(value = value)
                            } else {
                                Box(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tidak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                HeatmapLegendItem(0)
                HeatmapLegendItem(1)
                HeatmapLegendItem(2)
                HeatmapLegendItem(3)
                Text(
                    "Sempurna+",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EnhancedHeatmapCell(value: Int) {
    val color = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> GoldAccent
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
    )
}

@Composable
fun HeatmapLegendItem(value: Int) {
    val color = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> GoldAccent
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}