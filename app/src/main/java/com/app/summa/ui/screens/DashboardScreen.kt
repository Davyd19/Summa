package com.app.summa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.HabitItem
import com.app.summa.data.model.Task
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalBlue
import com.app.summa.ui.theme.BrutalWhite
import com.app.summa.ui.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

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
    onNavigateToIdentityProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToHabits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val dayLabel = today.dayOfMonth.toString().padStart(2, '0')
    val monthLabel = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BrutalistHeader(
            day = dayLabel,
            month = monthLabel,
            greeting = uiState.greeting,
            currentMode = currentMode,
            onModeClick = { showModeDialog = true },
            onSettingsClick = onNavigateToSettings
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrutalistDailyGoalCard(
                progress = uiState.todayProgress,
                completedHabits = uiState.completedHabits,
                totalHabits = uiState.todayHabits.size
            )

            BrutalistStatGrid(
                tasksLeft = uiState.activeTasks,
                points = uiState.summaPoints,
                paperclips = uiState.totalPaperclips
            )

            BrutalistNextActionCard(
                task = uiState.nextTask,
                onPrimaryAction = onNavigateToPlanner,
                onProfileClick = onNavigateToIdentityProfile
            )

            BrutalistHabitsSection(
                habits = uiState.todayHabits,
                onHabitClick = onNavigateToHabitDetail
            )

            BrutalistQuickAccessRow(
                netWorth = uiState.totalNetWorth,
                onMoneyClick = onNavigateToMoney,
                onNotesClick = onNavigateToNotes,
                onReflectionClick = onNavigateToReflections
            )
        }

        BrutalistActionSection(
            onStartSession = onNavigateToPlanner,
            onEditHabits = onNavigateToHabits,
            onHistoryClick = onNavigateToReflections
        )
    }

    if (showModeDialog) {
        BrutalistModeDialog(
            currentMode = currentMode,
            onDismiss = { showModeDialog = false },
            onModeSelected = {
                onModeSelected(it)
                showModeDialog = false
            }
        )
    }
}

@Composable
private fun BrutalistHeader(
    day: String,
    month: String,
    greeting: String,
    currentMode: String,
    onModeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = day,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = MaterialTheme.typography.displayMedium.lineHeight * 0.9f
            )
            Text(
                text = month,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .shadow(0.dp)
                .brutalBorder(),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = "Hello, Summa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = onModeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .brutalBorder()
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Mode")
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .brutalBorder()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
private fun BrutalistDailyGoalCard(
    progress: Float,
    completedHabits: Int,
    totalHabits: Int
) {
    val percentage = (progress.coerceIn(0f, 1f) * 100).roundToInt()

    BrutalistCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Daily Goal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "+ keep your streak alive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.displaySmall,
                color = BrutalBlue,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BrutalistProgressBar(progress = progress)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$completedHabits / $totalHabits selesai",
                style = MaterialTheme.typography.labelLarge
            )
            BrutalistTag(label = "ACTIVE")
        }
    }
}

@Composable
private fun BrutalistProgressBar(progress: Float, segments: Int = 4) {
    val clamped = progress.coerceIn(0f, 1f)
    val filledSegments = (clamped * segments).toInt()
    val partial = (clamped * segments) - filledSegments

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .brutalBorder()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(segments) { index ->
            val isFilled = index < filledSegments
            val isPartial = index == filledSegments && partial > 0f && filledSegments < segments

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .brutalBorder()
                    .background(
                        when {
                            isFilled -> MaterialTheme.colorScheme.primary
                            isPartial -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.background
                        }
                    )
            ) {
                if (isPartial) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    start = Offset.Zero,
                                    end = Offset(30f, 30f),
                                    tileMode = TileMode.Repeated
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BrutalistStatGrid(
    tasksLeft: Int,
    points: Int,
    paperclips: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrutalistStatCard(
            title = "Summa Points",
            value = points.toString(),
            subtitle = "$paperclips paperclips",
            icon = Icons.Default.BarChart,
            inverted = false,
            modifier = Modifier.weight(1f)
        )
        BrutalistStatCard(
            title = "Tasks Left",
            value = tasksLeft.toString().padStart(2, '0'),
            subtitle = "Focus pending",
            icon = Icons.Default.Schedule,
            inverted = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BrutalistStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    inverted: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (inverted) BrutalBlack else MaterialTheme.colorScheme.surface
    val contentColor = if (inverted) BrutalWhite else MaterialTheme.colorScheme.onSurface

    BrutalistCard(
        modifier = modifier,
        containerColor = background,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .brutalBorder(color = contentColor)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun BrutalistNextActionCard(
    task: Task?,
    onPrimaryAction: () -> Unit,
    onProfileClick: () -> Unit
) {
    BrutalistCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (task != null) "Next Action" else "Mulai Hari",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = task?.title ?: "Tambah tugas atau sesi fokus",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task?.scheduledTime.isNullOrEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = task?.scheduledTime ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.brutalBorder()
                ) {
                    Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (task != null) "Mulai" else "Tambah")
                }
                TextButton(onClick = onProfileClick) {
                    Text("Identity profile", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun BrutalistHabitsSection(
    habits: List<HabitItem>,
    onHabitClick: (HabitItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kebiasaan Hari Ini",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            BrutalistTag(label = "${habits.count { it.currentCount >= it.targetCount && it.targetCount > 0 }}/${habits.size}")
        }

        if (habits.isEmpty()) {
            BrutalistCard {
                Text(
                    text = "Belum ada kebiasaan terjadwal.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                habits.forEach { habit ->
                    BrutalistHabitChip(
                        habit = habit,
                        onClick = { onHabitClick(habit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrutalistHabitChip(
    habit: HabitItem,
    onClick: () -> Unit
) {
    val isComplete = habit.currentCount >= habit.targetCount && habit.targetCount > 0

    Surface(
        modifier = Modifier
            .width(200.dp)
            .brutalBorder()
            .clickable { onClick() },
        color = if (isComplete) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
                if (isComplete) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            ProgressBlocks(
                progress = if (habit.targetCount > 0)
                    (habit.currentCount.toFloat() / habit.targetCount).coerceIn(0f, 1f) else 0f
            )
        }
    }
}

@Composable
private fun ProgressBlocks(progress: Float, blocks: Int = 6) {
    val clamped = progress.coerceIn(0f, 1f)
    val filled = (clamped * blocks).roundToInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(blocks) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (index < filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .brutalBorder(strokeWidth = 1.dp)
            )
        }
    }
}

@Composable
private fun BrutalistQuickAccessRow(
    netWorth: Double,
    onMoneyClick: () -> Unit,
    onNotesClick: () -> Unit,
    onReflectionClick: () -> Unit
) {
    val currency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Akses Cepat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickLink(
                title = "Keuangan",
                subtitle = currency.format(netWorth),
                onClick = onMoneyClick,
                modifier = Modifier.weight(1f)
            )
            QuickLink(
                title = "Pustaka",
                subtitle = "Ide & catatan",
                onClick = onNotesClick,
                modifier = Modifier.weight(1f)
            )
        }
        QuickLink(
            title = "Refleksi",
            subtitle = "Tinjau hari ini",
            onClick = onReflectionClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickLink(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.brutalBorder(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun BrutalistActionSection(
    onStartSession: () -> Unit,
    onEditHabits: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onStartSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .brutalBorder(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onEditHabits) {
                Text("Edit Habits", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onHistoryClick) {
                Text("History", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun BrutalistTag(
    label: String,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(2.dp, color),
        color = Color.Transparent
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BrutalistModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    val options = listOf(
        "Normal" to "Tampilan lengkap",
        "Fokus" to "Minim gangguan",
        "Pagi" to "Rutinitas awal hari"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Mode",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { (mode, desc) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .brutalBorder(),
                        color = if (currentMode == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        onClick = { onModeSelected(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            if (currentMode == mode) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun BrutalistCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .brutalBorder()
            .shadow(
                elevation = 0.dp,
                spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            ),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun Modifier.brutalBorder(
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onBackground
): Modifier = this.border(
    width = strokeWidth,
    color = color,
    shape = RoundedCornerShape(6.dp)
)
