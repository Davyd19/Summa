package com.app.summa.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToHabitDetail: (HabitItem) -> Unit = {},
    onNavigateToMoney: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToReflections: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle animated gradient background
        AnimatedGradientBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Modern Header with animated greeting
            item {
                ModernHeader(
                    greeting = uiState.greeting,
                    currentMode = uiState.currentMode,
                    onModeClick = { showModeDialog = true }
                )
            }

            // Hero Points Card with smooth animations
            item {
                ModernPointsCard(
                    progress = uiState.todayProgress,
                    points = uiState.summaPoints,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Next Action - Prominent and action-oriented
            if (uiState.currentMode != "Fokus") {
                item {
                    ModernNextActionCard(
                        task = uiState.nextTask,
                        onStartFocus = { /* TODO */ },
                        onAddTask = onNavigateToPlanner,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            // Today's Habits with modern cards
            if (uiState.currentMode != "Fokus") {
                item {
                    ModernSectionHeader(
                        title = "Kebiasaan Hari Ini",
                        trailing = "${uiState.todayHabits.count { it.currentCount >= it.targetCount }}/${uiState.todayHabits.size}",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

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

            // Quick Access with modern icons
            if (uiState.currentMode == "Normal") {
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

            // Bottom spacing
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showModeDialog) {
        ModernModeDialog(
            currentMode = uiState.currentMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                viewModel.setMode(it)
                showModeDialog = false
            }
        )
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
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Greeting with fade-in animation
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.fadeIn(tween(600)) +
                    androidx.compose.animation.slideInVertically(tween(600)) { -20 }
        ) {
            Column {
                Text(
                    text = "$greeting 👋",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mari kita mulai hari yang produktif",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    // Mode badge
                    Surface(
                        onClick = onModeClick,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                currentMode,
                                style = MaterialTheme.typography.labelMedium,
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box {
            // Decorative circles with subtle animation
            val scale by rememberInfiniteTransition(label = "circle").animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = 220.dp, y = (-40).dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Summa Points",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            animatedPoints.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Modern circular progress
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(88.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = GoldAccent,
                            strokeWidth = 8.dp,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Modern progress bar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = GoldAccent,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )

                    Text(
                        "Target harian tercapai ${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
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
    Card(
        onClick = if (task != null) onStartFocus else onAddTask,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (task != null)
                MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated icon
            val scale by rememberInfiniteTransition(label = "icon").animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(if (task != null) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (task != null)
                            MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (task != null) Icons.Default.PlayArrow else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (task != null) Color.White
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (task != null) "Selanjutnya" else "Mulai Hari",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    task?.title ?: "Tambah tugas pertama",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (task?.scheduledTime != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.scheduledTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Mulai",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
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

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete)
                SuccessGreen.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Start
            ) {
                Text(
                    habit.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (habit.currentStreak > 0) {
                    Surface(
                        shape = CircleShape,
                        color = StreakOrange.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${habit.currentStreak}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StreakOrange
                            )
                        }
                    }
                }
            }

            Text(
                habit.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        if (habit.targetCount > 1) "${habit.currentCount}/${habit.targetCount}"
                        else "${habit.currentCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) SuccessGreen
                        else MaterialTheme.colorScheme.primary
                    )
                    if (isComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selesai",
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SuccessGreen
            )
        }
    }
}

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
                gradient = listOf(BlueAccent, BlueAccent.copy(alpha = 0.7f)),
                onClick = onMoneyClick,
                modifier = Modifier.weight(1f)
            )
            ModernQuickAccessCard(
                icon = Icons.Default.Book,
                title = "Pustaka",
                subtitle = "Catat ide",
                gradient = listOf(PurpleAccent, PurpleAccent.copy(alpha = 0.7f)),
                onClick = onNotesClick,
                modifier = Modifier.weight(1f)
            )
        }
        ModernQuickAccessCard(
            icon = Icons.Default.RateReview,
            title = "Tinjauan Harian",
            subtitle = "Refleksi & evaluasi",
            gradient = listOf(PinkAccent, PinkAccent.copy(alpha = 0.7f)),
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
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = gradient[0].copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
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
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("Normal", "Tampilan lengkap", Icons.Default.Dashboard),
                    Triple("Fokus", "Hanya tugas penting", Icons.Default.TipsAndUpdates),
                    Triple("Pagi", "Rutinitas pagi", Icons.Default.WbSunny)
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
                Text("Tutup")
            }
        },
        shape = MaterialTheme.shapes.large
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
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
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}