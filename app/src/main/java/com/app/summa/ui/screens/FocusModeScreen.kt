package com.app.summa.ui.screens

import com.app.summa.ui.components.*

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.app.summa.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UniversalFocusModeScreen(
    title: String,
    initialTarget: Int = 10,
    onComplete: (Int, Long) -> Unit, // Returns: clipsCollected, startTime
    onCancel: () -> Unit
) {
    var isSetupPhase by remember { mutableStateOf(true) }
    var targetClips by remember { mutableStateOf(initialTarget.toFloat()) }

    if (isSetupPhase) {
        FocusSetupScreen(
            title = title,
            targetClips = targetClips,
            onTargetChange = { targetClips = it },
            onStart = { isSetupPhase = false },
            onCancel = onCancel
        )
    } else {
        FocusRunningScreen(
            title = title,
            targetClips = targetClips.toInt(),
            onComplete = onComplete,
            onCancel = onCancel
        )
    }
}

// ... FocusSetupScreen (Tidak berubah, dianggap sama seperti sebelumnya) ...
@Composable
fun FocusSetupScreen(
    title: String,
    targetClips: Float,
    onTargetChange: (Float) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Persiapan Fokus",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        val estimatedTime = targetClips.toInt() * 2
        Text(
            "$estimatedTime Menit",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = DeepTeal
        )
        Text(
            "Estimasi Waktu",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(32.dp))

        Text("Target Klip: ${targetClips.toInt()}", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = targetClips,
            onValueChange = onTargetChange,
            valueRange = 1f..50f,
            steps = 49,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            "Geser untuk menambah beban kerja",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .brutalBorder(radius=4.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("MULAI SESI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onCancel) {
            Text("Batal")
        }
    }
}

@Composable
fun FocusRunningScreen(
    title: String,
    targetClips: Int,
    onComplete: (Int, Long) -> Unit,
    onCancel: () -> Unit
) {
    // Timer State
    var elapsedTimeSeconds by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    // Paperclip Logic
    var paperclipsLeft by remember { mutableStateOf(targetClips) }
    var paperclipsMoved by remember { mutableStateOf(0) }
    val startTime by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    // Drag State
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Helper untuk memindahkan klip (digunakan oleh Drag dan Click)
    fun moveClip() {
        if (paperclipsLeft > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            paperclipsLeft--
            paperclipsMoved++

            if (!isRunning) isRunning = true

            // Cek selesai otomatis
            if (paperclipsLeft == 0) {
                // Beri jeda sedikit agar user melihat klip terakhir pindah
                scope.launch {
                    delay(500)
                    onComplete(paperclipsMoved, startTime)
                }
            }
        }
    }

    // Timer Logic
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTimeNano = System.nanoTime()
            val initialElapsed = elapsedTimeSeconds
            while (isRunning) {
                delay(1000)
                elapsedTimeSeconds = initialElapsed + ((System.nanoTime() - startTimeNano) / 1_000_000_000).toInt()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Batal")
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            TextButton(
                onClick = { onComplete(paperclipsMoved, startTime) },
                enabled = paperclipsMoved > 0
            ) {
                Text("Selesai")
            }
        }

        Spacer(Modifier.height(32.dp))

        // Timer Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .brutalBorder(radius=220.dp, strokeWidth=4.dp, color=DeepTeal)
                .border(4.dp, DeepTeal.copy(alpha = 0.2f), CircleShape)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%02d:%02d", elapsedTimeSeconds / 60, elapsedTimeSeconds % 60),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = DeepTeal
                )
                Text(
                    if (isRunning) "FOKUS..." else "Geser klip untuk mulai",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Interactive Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SOURCE PILE
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .brutalBorder(strokeWidth = 3.dp, radius = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    // PERBAIKAN: Klik juga bisa memindahkan klip (Aksesibilitas)
                    .clickable(
                        enabled = paperclipsLeft > 0,
                        onClickLabel = "Ambil klip kertas"
                    ) {
                        moveClip()
                    }
            ) {
                if (paperclipsLeft > 0) {
                    Text(
                        "📎",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    animatedOffsetX.value.roundToInt() + dragOffset.x.roundToInt(),
                                    animatedOffsetY.value.roundToInt() + dragOffset.y.roundToInt()
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (!isRunning) isRunning = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        // Jarak drop minimal (misal 100px ke kanan)
                                        if (dragOffset.x > 100) {
                                            moveClip()
                                            scope.launch {
                                                animatedOffsetX.snapTo(0f)
                                                animatedOffsetY.snapTo(0f)
                                            }
                                            dragOffset = Offset.Zero
                                        } else {
                                            // GAGAL (SNAP BACK)
                                            scope.launch {
                                                val endX = dragOffset.x
                                                val endY = dragOffset.y
                                                animatedOffsetX.snapTo(endX)
                                                animatedOffsetY.snapTo(endY)
                                                dragOffset = Offset.Zero
                                                launch { animatedOffsetX.animateTo(0f) }
                                                launch { animatedOffsetY.animateTo(0f) }
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                    }
                                )
                            }
                            // Semantic untuk TalkBack
                            .semantics {
                                contentDescription = "Tumpukan klip kertas. Geser ke kanan atau ketuk dua kali untuk memindahkan."
                            }
                    )
                } else {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Badge Sisa
                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            "$paperclipsLeft",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )

            // TARGET PILE
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .brutalBorder(radius=8.dp, strokeWidth=2.dp, color=SuccessGreen)
                    .semantics { contentDescription = "Target pengumpulan klip" }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📎",
                        style = MaterialTheme.typography.displayMedium,
                        color = SuccessGreen.copy(alpha = 0.5f)
                    )
                    Text(
                        "$paperclipsMoved",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // PERBAIKAN: Tombol Alternatif (Aksesibilitas Eksplisit)
        // Berguna jika pengguna bingung dengan gesture drag
        if (paperclipsLeft > 0) {
            FilledTonalButton(
                onClick = { moveClip() },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ambil Klip (Ketuk)")
            }
        } else {
            Text(
                "Semua klip terkumpul!",
                style = MaterialTheme.typography.bodyLarge,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}