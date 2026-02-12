package com.app.summa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalWhite
import kotlin.math.roundToInt

@Composable
fun Modifier.brutalBorder(
    strokeWidth: Dp = 3.dp, // Thicker border
    color: Color = MaterialTheme.colorScheme.outline, // Use outline color
    cornerRadius: Dp = 6.dp // Sharper corners
) = this.then(
    border(
        width = strokeWidth,
        color = color,
        shape = RoundedCornerShape(cornerRadius)
    )
)

@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(6.dp),
                clip = false
            )
            .brutalBorder(strokeWidth = 3.dp, cornerRadius = 6.dp),
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}


@Composable
fun BrutalistTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.5.dp, color),
        color = Color.Transparent
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}


// New metric components
@Composable
fun BrutalistMetricCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    BrutalistCard(
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Icon badge in top-right corner
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .brutalBorder(strokeWidth = 2.dp, cornerRadius = 20.dp),
                shape = CircleShape,
                color = backgroundColor,
                contentColor = contentColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Sisa",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}


@Composable
fun BrutalistDateDisplay(
    day: String,
    month: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            lineHeight = MaterialTheme.typography.displayLarge.fontSize * 0.8
        )
        Text(
            text = month.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun BrutalistLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ArrowForward
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun BrutalistProgressBar(
    progress: Float,
    segments: Int = 4,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val filledSegments = (clamped * segments).toInt()
    val partial = (clamped * segments) - filledSegments

    Row(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            }
            .fillMaxWidth()
            .height(52.dp)
            .brutalBorder()
            .padding(6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
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
fun BrutalistStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
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
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrutalTopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(
                    onClick = onNavigationClick,
                    modifier = Modifier.brutalBorder(strokeWidth = 2.dp)
                ) {
                    Icon(navigationIcon, contentDescription = "Back")
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrutalIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.brutalBorder(strokeWidth = 2.dp)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrutalFab(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.brutalBorder(strokeWidth = 2.dp, cornerRadius = 12.dp)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}


@Composable
fun brutalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent
)

// --- DASHBOARD COMPONENTS ---

@Composable
fun BrutalistDailyGoalCard(
    progress: Float,
    completedHabits: Int,
    totalHabits: Int,
    modifier: Modifier = Modifier
) {
    BrutalistCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "GOAL HARI INI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .brutalBorder(cornerRadius = 50.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                        contentDescription = "Target Tercapai",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BrutalistProgressBar(progress = progress)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$completedHabits dari $totalHabits kebiasaan selesai",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
fun BrutalistStatGrid(
    tasksLeft: Int,
    points: Int,
    paperclips: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        BrutalistStatCard(
            title = "TASK",
            value = "$tasksLeft",
            subtitle = "Sisa",
            icon = androidx.compose.material.icons.Icons.Filled.Schedule,
            inverted = false,
            modifier = Modifier.weight(1f)
        )
        BrutalistStatCard(
            title = "XP",
            value = "$points",
            subtitle = "Total",
            icon = androidx.compose.material.icons.Icons.Filled.BarChart,
            inverted = true, // Black card
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun BrutalistNextActionCard(
    task: com.app.summa.data.model.Task?, // Assuming Task is available
    onPrimaryAction: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (task == null) {
        BrutalistCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Semua Beres! 🎉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                BrutalTextButton(text = "RENCANA BESOK", onClick = onPrimaryAction)
            }
        }
        return
    }

    BrutalistCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                BrutalistTag(text = "NEXT ACTION", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                     task.scheduledTime ?: "Now",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                BrutalTextButton(
                    text = "MULAI",
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).brutalBorder(cornerRadius =4.dp)
                )
            }
        }
    }
}


@Composable
fun BrutalistHabitsSection(
    habits: List<com.app.summa.data.model.HabitItem>,
    onHabitClick: (com.app.summa.data.model.HabitItem) -> Unit
) {
    if (habits.isNotEmpty()) {
        Text("KEBIASAAN (PRIORITAS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom=8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            habits.take(3).forEach { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth().brutalBorder(strokeWidth = 2.dp).padding(12.dp).clickable(role = Role.Button) { onHabitClick(habit) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (habit.targetCount > 0 && habit.currentCount >= habit.targetCount) {
                        Icon(Icons.Filled.CheckCircle, "Selesai", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("${habit.currentCount}/${habit.targetCount}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun BrutalistQuickAccessRow(
    netWorth: String,
    onMoneyClick: () -> Unit,
    onNotesClick: () -> Unit,
    onReflectionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Money
        BrutalistCard(
            modifier = Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { onMoneyClick() },
            containerColor = com.app.summa.ui.theme.DeepTeal,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, null)
                Spacer(modifier = Modifier.height(4.dp))
                Text(netWorth, fontWeight = FontWeight.Bold)
            }
        }
        // Notes
        BrutalistCard(
            modifier = Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button) { onNotesClick() }
        ) {
             Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Book, null)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Notes", fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
fun BrutalistActionSection(
    onStartSession: () -> Unit,
    onEditHabits: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // Quick Actions
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        BrutalIconAction(Icons.Outlined.PlayCircleOutline, "Fokus", onStartSession)
        BrutalIconAction(Icons.Filled.CheckCircle, "Habits", onEditHabits)
        BrutalIconAction(Icons.Filled.RateReview, "Refleksi", onHistoryClick)
    }
}


@Composable
fun BrutalistModeDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onModeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Mode", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Normal", "Fokus", "Pagi", "Malam").forEach { mode ->
                    val isSelected = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .brutalBorder(strokeWidth = if(isSelected) 3.dp else 1.dp, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            .background(if(isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(
                                selected = isSelected,
                                onClick = { onModeSelected(mode) },
                                role = Role.RadioButton
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            mode.uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { BrutalTextButton("Tutup", onDismiss) },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.brutalBorder()
    )
}

@Composable
fun BrutalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun BrutalistHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.onBackground)
        )
    }
}


// === NEW BRUTALIST COMPONENTS FOR OVERHAUL ===

@Composable
fun BrutalistHeaderBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BrutalBlack,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp) // Sharp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = BrutalWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun BrutalistDigitalClock(
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while(true) {
            val now = java.time.LocalTime.now()
            time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    Surface(
        modifier = modifier
            .brutalBorder(strokeWidth = 2.dp, cornerRadius = 0.dp), // Boxed
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BrutalistBlockProgressBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = current.toFloat(),
                    range = 0f..max.toFloat(),
                    steps = max
                )
            }
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 1..max) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f) // Square blocks
                    .background(
                        if (i <= current) activeColor else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                    )
            )
        }
    }
}

@Composable
fun BrutalistSystemFooter(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Battery
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .brutalBorder(strokeWidth = 3.dp, cornerRadius = 0.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("BATTERY", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("92%", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(12.dp, 24.dp).background(Color.Black))
                }
            }
        }
        
        // Network
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .brutalBorder(strokeWidth = 3.dp, cornerRadius = 0.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("NETWORK", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                Icon(Icons.Default.Wifi, null)
            }
        }
    }
}

