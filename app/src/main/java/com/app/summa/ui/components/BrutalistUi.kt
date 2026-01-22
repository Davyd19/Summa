package com.app.summa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.app.summa.ui.theme.BrutalBlack
import com.app.summa.ui.theme.BrutalWhite
import kotlin.math.roundToInt

@Composable
fun Modifier.brutalBorder(
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.onBackground,
    radius: Dp = 6.dp
): Modifier = this.border(
    width = strokeWidth,
    color = color,
    shape = RoundedCornerShape(radius)
)

@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .brutalBorder(color = borderColor)
            .shadow(
                elevation = 0.dp,
                spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            ),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun BrutalistTag(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Surface(
        modifier = modifier,
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
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.brutalBorder(strokeWidth = 2.dp)
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
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(2.dp)
                .brutalBorder(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
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
                        .brutalBorder(radius = 50.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                        contentDescription = null,
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
                BrutalistTag(label = "NEXT ACTION", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp)).brutalBorder(radius=4.dp)
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
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            habits.take(3).forEach { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth().brutalBorder(strokeWidth = 2.dp).padding(12.dp).androidx.compose.foundation.clickable{ onHabitClick(habit) },
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(habit.icon, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (habit.targetCount > 0 && habit.currentCount >= habit.targetCount) {
                        Icon(androidx.compose.material.icons.Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
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
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        // Money
        BrutalistCard(
            modifier = Modifier.weight(1f).fillMaxHeight().androidx.compose.foundation.clickable{ onMoneyClick() },
            containerColor = com.app.summa.ui.theme.DeepTeal,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.AccountBalanceWallet, null)
                Spacer(modifier = Modifier.height(4.dp))
                Text(netWorth, fontWeight = FontWeight.Bold)
            }
        }
        // Notes
        BrutalistCard(
            modifier = Modifier.weight(1f).fillMaxHeight().androidx.compose.foundation.clickable{ onNotesClick() }
        ) {
             Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Book, null)
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
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
        BrutalIconAction(androidx.compose.material.icons.Outlined.PlayCircleOutline, "Fokus", onStartSession)
        BrutalIconAction(androidx.compose.material.icons.Filled.CheckCircle, "Habits", onEditHabits)
        BrutalIconAction(androidx.compose.material.icons.Filled.RateReview, "Refleksi", onHistoryClick)
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
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf("Normal", "Fokus", "Pagi", "Malam").forEach { mode ->
                    val isSelected = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .brutalBorder(strokeWidth = if(isSelected) 3.dp else 1.dp, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            .background(if(isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .androidx.compose.foundation.clickable { onModeSelected(mode) }
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
