package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.HabitViewModel
import com.app.summa.ui.model.HabitItem
import com.app.summa.data.model.HabitLog
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Crossfade(targetState = uiState.selectedHabit, label = "HabitScreen") { selectedHabit ->
        if (selectedHabit != null) {
            ModernHabitDetailScreen(
                habit = selectedHabit,
                onBack = { viewModel.onBackFromDetail() },
                logs = uiState.habitLogs
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Kebiasaan",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Bangun identitas melalui konsistensi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        actions = {
                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.padding(end = 12.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Tambah", fontWeight = FontWeight.SemiBold)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(uiState.habits) { habit ->
                            EnhancedHabitItem(
                                habit = habit,
                                onClick = { viewModel.selectHabit(habit) },
                                onIncrement = { viewModel.incrementHabit(habit) },
                                onDecrement = { viewModel.decrementHabit(habit) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ModernAddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, icon, target ->
                viewModel.addHabit(name, icon, target)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EnhancedHabitItem(
    habit: HabitItem,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isComplete = habit.targetCount > 0 && habit.currentCount >= habit.targetCount
    val isOverAchieved = habit.targetCount > 0 && habit.currentCount > habit.targetCount
    val progress = if (habit.targetCount > 0) {
        (habit.currentCount.toFloat() / habit.targetCount).coerceAtMost(1f)
    } else 0f

    // Celebration animation when complete
    val scale by animateFloatAsState(
        targetValue = if (isComplete) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverAchieved -> GoldContainer
                isComplete -> SuccessGreenBg
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isComplete) 4.dp else 1.dp
        ),
        border = BorderStroke(
            2.dp,
            when {
                isOverAchieved -> GoldAccent.copy(alpha = 0.3f)
                isComplete -> SuccessGreen.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Top section with icon and streaks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon dengan gradient background
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = when {
                                    isOverAchieved -> listOf(
                                        GoldAccent.copy(alpha = 0.3f),
                                        GoldDark.copy(alpha = 0.2f)
                                    )
                                    isComplete -> listOf(
                                        SuccessGreen.copy(alpha = 0.3f),
                                        SuccessGreenDark.copy(alpha = 0.2f)
                                    )
                                    else -> listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    )
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = habit.icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (habit.currentStreak > 0) {
                        EnhancedStreakBadge(
                            emoji = "🔥",
                            count = habit.currentStreak,
                            color = StreakOrange,
                            label = "Konsisten"
                        )
                    }
                    if (habit.perfectStreak > 0) {
                        EnhancedStreakBadge(
                            emoji = "👑",
                            count = habit.perfectStreak,
                            color = GoldAccent,
                            label = "Sempurna"
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Habit name
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            // Progress and controls section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Counter controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        enabled = habit.currentCount > 0
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Kurangi",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedContent(
                        targetState = habit.currentCount,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(
                                slideOutVertically { -it } + fadeOut()
                            )
                        },
                        label = "count_anim"
                    ) { count ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.widthIn(min = 100.dp)
                        ) {
                            Text(
                                text = if (habit.targetCount > 1) "$count / ${habit.targetCount}"
                                else "$count",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isOverAchieved -> GoldAccent
                                    isComplete -> SuccessGreen
                                    count > 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                }
                            )
                            if (habit.targetCount > 0) {
                                Text(
                                    "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = onIncrement,
                        modifier = Modifier.size(52.dp),
                        containerColor = when {
                            isOverAchieved -> GoldAccent
                            isComplete -> SuccessGreen
                            else -> MaterialTheme.colorScheme.primary
                        },
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }

                // Completion badge with animation
                if (isComplete) {
                    val infiniteTransition = rememberInfiniteTransition(label = "badge")
                    val badgeScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "badge_scale"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isOverAchieved -> GoldAccent
                            else -> SuccessGreen
                        },
                        modifier = Modifier.scale(badgeScale),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isOverAchieved) Icons.Default.Star else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                if (isOverAchieved) "Luar Biasa!" else "Selesai",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (habit.targetCount > 0) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = when {
                        isOverAchieved -> GoldAccent
                        isComplete -> SuccessGreen
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Total points indicator
            if (habit.totalSum > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "⭐",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${habit.totalSum} Summa Points",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = GoldAccent
                    )
                }
            }
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, target: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var target by remember { mutableStateOf("1") }

    val commonEmojis = listOf(
        "🎯", "📚", "💪", "🧘", "🏃", "🎨", "✍️", "💻",
        "🎵", "🌱", "💧", "🍎", "😴", "🧹", "📱", "💼"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Kebiasaan Baru",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kebiasaan") },
                    placeholder = { Text("Contoh: Baca buku") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Pilih Ikon",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commonEmojis.size) { index ->
                            val emoji = commonEmojis[index]
                            Surface(
                                onClick = { icon = emoji },
                                shape = RoundedCornerShape(12.dp),
                                color = if (icon == emoji)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { if (it.all { char -> char.isDigit() }) target = it },
                    label = { Text("Target Harian") },
                    placeholder = { Text("Berapa kali per hari?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, icon, target.toIntOrNull() ?: 1)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Tambah", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHabitDetailScreen(
    habit: HabitItem,
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
            // Hero Header
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

            // Stats Cards
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

            // Heatmap
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