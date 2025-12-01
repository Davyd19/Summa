package com.app.summa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.data.model.NoteLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

// --- PHYSICS ENGINE DATA CLASSES ---

class GraphNode(
    val id: Long,
    val title: String,
    val isPermanent: Boolean,
    var x: Float,
    var y: Float,
    var vx: Float = 0f, // Kecepatan X
    var vy: Float = 0f, // Kecepatan Y
    var color: Color
)

// --- KONFIGURASI FISIKA ---
private const val REPULSION_FORCE = 5000f
private const val ATTRACTION_FORCE = 0.05f
private const val SPRING_LENGTH = 150f
private const val DAMPING = 0.9f
private const val CENTER_GRAVITY = 0.01f
private const val VELOCITY_THRESHOLD = 0.1f // Batas kecepatan untuk berhenti simulasi (Hemat Baterai)

@Composable
fun KnowledgeGraphView(
    notes: List<KnowledgeNote>,
    links: List<NoteLink>,
    modifier: Modifier = Modifier,
    onNodeClick: (Long) -> Unit
) {
    // 1. Inisialisasi State Node
    val graphNodes = remember(notes) {
        notes.map { note ->
            GraphNode(
                id = note.id,
                title = note.title.ifBlank { "Untitled" },
                isPermanent = note.isPermanent,
                x = Random.nextFloat() * 500f + 200f,
                y = Random.nextFloat() * 800f + 400f,
                color = if (note.isPermanent) Color(0xFF8B5CF6) else Color(0xFF0D9488)
            )
        }
    }

    var scale by remember { mutableStateOf(0.8f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    var draggedNodeId by remember { mutableStateOf<Long?>(null) }
    var dragging by remember { mutableStateOf(false) }

    // PERBAIKAN: State untuk mengontrol simulasi (Hemat Baterai)
    var isSimulationRunning by remember { mutableStateOf(true) }
    // Trigger simulasi ulang jika data berubah atau user interaksi
    LaunchedEffect(notes, links, dragging) { isSimulationRunning = true }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface
    val labelBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

    // 2. SIMULASI FISIKA
    LaunchedEffect(isSimulationRunning) {
        if (!isSimulationRunning) return@LaunchedEffect

        while (isActive && isSimulationRunning) {
            val width = 1000f
            val height = 1500f
            val centerX = width / 2
            val centerY = height / 2

            val forcesX = FloatArray(graphNodes.size) { 0f }
            val forcesY = FloatArray(graphNodes.size) { 0f }
            var maxVelocity = 0f // Melacak energi kinetik total

            // A. GAYA TOLAK (Repulsion)
            for (i in graphNodes.indices) {
                for (j in i + 1 until graphNodes.size) {
                    val n1 = graphNodes[i]
                    val n2 = graphNodes[j]
                    val dx = n1.x - n2.x
                    val dy = n1.y - n2.y
                    val distSq = dx * dx + dy * dy
                    val dist = sqrt(distSq).coerceAtLeast(1f)
                    val force = REPULSION_FORCE / dist
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    forcesX[i] += fx; forcesY[i] += fy
                    forcesX[j] -= fx; forcesY[j] -= fy
                }
            }

            // B. GAYA TARIK (Attraction)
            links.forEach { link ->
                val sIdx = graphNodes.indexOfFirst { it.id == link.sourceNoteId }
                val tIdx = graphNodes.indexOfFirst { it.id == link.targetNoteId }
                if (sIdx != -1 && tIdx != -1) {
                    val n1 = graphNodes[sIdx]
                    val n2 = graphNodes[tIdx]
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val force = (dist - SPRING_LENGTH) * ATTRACTION_FORCE
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    forcesX[sIdx] += fx; forcesY[sIdx] += fy
                    forcesX[tIdx] -= fx; forcesY[tIdx] -= fy
                }
            }

            // C. UPDATE POSISI & CEK KECEPATAN
            for (i in graphNodes.indices) {
                val node = graphNodes[i]
                if (node.id == draggedNodeId) continue

                val dxCenter = centerX - node.x
                val dyCenter = centerY - node.y
                forcesX[i] += dxCenter * CENTER_GRAVITY
                forcesY[i] += dyCenter * CENTER_GRAVITY

                node.vx = (node.vx + forcesX[i]) * DAMPING
                node.vy = (node.vy + forcesY[i]) * DAMPING

                // Limit speed
                node.vx = node.vx.coerceIn(-20f, 20f)
                node.vy = node.vy.coerceIn(-20f, 20f)

                node.x += node.vx
                node.y += node.vy

                // Track max speed
                val speed = abs(node.vx) + abs(node.vy)
                if (speed > maxVelocity) maxVelocity = speed
            }

            // PERBAIKAN: Jika semua node diam (energi rendah) dan user tidak drag, stop loop
            if (maxVelocity < VELOCITY_THRESHOLD && !dragging) {
                isSimulationRunning = false
            }

            delay(16) // ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 3f)
                    offset += pan
                    isSimulationRunning = true // Bangunkan simulasi saat zoom/pan
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val touchX = (startOffset.x - offset.x) / scale
                        val touchY = (startOffset.y - offset.y) / scale
                        val nearestNode = graphNodes.minByOrNull {
                            (it.x - touchX).pow(2) + (it.y - touchY).pow(2)
                        }
                        if (nearestNode != null) {
                            val dist = sqrt((nearestNode.x - touchX).pow(2) + (nearestNode.y - touchY).pow(2))
                            if (dist < 50f) {
                                draggedNodeId = nearestNode.id
                                dragging = true
                                isSimulationRunning = true // Bangunkan simulasi
                                nearestNode.vx = 0f; nearestNode.vy = 0f
                            }
                        }
                    },
                    onDragEnd = { draggedNodeId = null; dragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedNodeId?.let { id ->
                            val node = graphNodes.find { it.id == id }
                            if (node != null) {
                                node.x += dragAmount.x / scale
                                node.y += dragAmount.y / scale
                                isSimulationRunning = true // Tetap bangun saat drag
                            }
                        }
                    }
                )
            }
    ) {
        if (graphNodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada catatan untuk dipetakan", color = Color.Gray)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                links.forEach { link ->
                    val n1 = graphNodes.find { it.id == link.sourceNoteId }
                    val n2 = graphNodes.find { it.id == link.targetNoteId }
                    if (n1 != null && n2 != null) {
                        drawLine(
                            color = lineColor,
                            start = Offset(n1.x * scale + offset.x, n1.y * scale + offset.y),
                            end = Offset(n2.x * scale + offset.x, n2.y * scale + offset.y),
                            strokeWidth = 2.dp.toPx() * scale
                        )
                    }
                }

                graphNodes.forEach { node ->
                    val screenX = node.x * scale + offset.x
                    val screenY = node.y * scale + offset.y
                    val radius = (if (node.isPermanent) 18.dp.toPx() else 12.dp.toPx()) * scale

                    if (screenX > -100 && screenX < size.width + 100 && screenY > -100 && screenY < size.height + 100) {
                        drawCircle(color = node.color, radius = radius, center = Offset(screenX, screenY))

                        if (scale > 0.6f) {
                            val textLayout = textMeasurer.measure(
                                text = node.title,
                                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp * scale, color = labelTextColor, background = labelBackgroundColor)
                            )
                            drawText(textLayout, topLeft = Offset(screenX - textLayout.size.width / 2, screenY + radius + 5f))
                        }
                    }
                }
            }
        }
    }
}