package com.app.summa.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
import kotlin.math.floor
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
    var vx: Float = 0f,
    var vy: Float = 0f,
    val color: Color
)

// --- KONFIGURASI FISIKA (Disesuaikan untuk Stabilitas) ---
private const val REPULSION_FORCE = 8000f
private const val ATTRACTION_FORCE = 0.04f
private const val SPRING_LENGTH = 200f
private const val DAMPING = 0.85f
private const val CENTER_GRAVITY = 0.015f
private const val GRID_SIZE = 250f

@Composable
fun KnowledgeGraphView(
    notes: List<KnowledgeNote>,
    links: List<NoteLink>,
    modifier: Modifier = Modifier,
    onNodeClick: (Long) -> Unit
) {
    // 1. Inisialisasi Node (Hanya dijalankan jika daftar notes berubah)
    val graphNodes = remember(notes) {
        notes.map { note ->
            GraphNode(
                id = note.id,
                title = note.title.ifBlank { "Untitled" },
                isPermanent = note.isPermanent,
                x = Random.nextFloat() * 800f + 100f,
                y = Random.nextFloat() * 1200f + 200f,
                color = if (note.isPermanent) Color(0xFF8B5CF6) else Color(0xFF0D9488)
            )
        }
    }

    // State Viewport (Zoom & Pan)
    var scale by remember { mutableStateOf(0.6f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    // State Interaksi
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }

    // State Simulasi (Energy System)
    var simulationAlpha by remember { mutableStateOf(1.0f) }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface
    val labelBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    fun wakeUpSimulation() {
        simulationAlpha = 1.0f
    }

    LaunchedEffect(notes, links) { wakeUpSimulation() }

    // 2. LOOP SIMULASI FISIKA (OPTIMASI PERFORMANCE)
    LaunchedEffect(Unit) {
        while (isActive) {
            // OPTIMASI: Jika sistem "dingin" dan tidak ada interaksi, stop loop (Hemat Baterai)
            if (simulationAlpha < 0.05f && draggedNodeId == null) {
                delay(200) // Cek lagi nanti dengan interval sangat lambat
                continue
            }

            val width = 2000f
            val height = 2000f
            val centerX = width / 2
            val centerY = height / 2

            val forcesX = FloatArray(graphNodes.size)
            val forcesY = FloatArray(graphNodes.size)

            // --- SPATIAL HASHING / GRID OPTIMIZATION ---
            val grid = HashMap<Int, ArrayList<Int>>()
            for (i in graphNodes.indices) {
                val node = graphNodes[i]
                val gridX = floor(node.x / GRID_SIZE).toInt()
                val gridY = floor(node.y / GRID_SIZE).toInt()
                val key = (gridX * 73856093) xor (gridY * 19349663)
                if (!grid.containsKey(key)) {
                    grid[key] = ArrayList()
                }
                grid[key]?.add(i)
            }

            // Hitung Repulsion (Tolak-menolak)
            for (i in graphNodes.indices) {
                val n1 = graphNodes[i]
                val gridX = floor(n1.x / GRID_SIZE).toInt()
                val gridY = floor(n1.y / GRID_SIZE).toInt()

                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val key = ((gridX + dx) * 73856093) xor ((gridY + dy) * 19349663)
                        val neighbors = grid[key] ?: continue

                        for (j in neighbors) {
                            if (i == j) continue
                            val n2 = graphNodes[j]
                            val distX = n1.x - n2.x
                            val distY = n1.y - n2.y
                            val distSq = distX * distX + distY * distY

                            if (distSq > GRID_SIZE * GRID_SIZE) continue

                            val dist = sqrt(distSq).coerceAtLeast(1f)
                            val force = REPULSION_FORCE / (dist * dist)

                            val fx = (distX / dist) * force
                            val fy = (distY / dist) * force

                            forcesX[i] += fx
                            forcesY[i] += fy
                        }
                    }
                }
            }

            // Hitung Attraction (Tarik-menarik Link)
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

                    forcesX[sIdx] += fx
                    forcesY[sIdx] += fy
                    forcesX[tIdx] -= fx
                    forcesY[tIdx] -= fy
                }
            }

            // Integrasi & Update Posisi
            var maxVel = 0f
            for (i in graphNodes.indices) {
                val node = graphNodes[i]

                if (node.id == draggedNodeId) {
                    node.vx = 0f
                    node.vy = 0f
                    continue
                }

                val dxCenter = centerX - node.x
                val dyCenter = centerY - node.y
                forcesX[i] += dxCenter * CENTER_GRAVITY
                forcesY[i] += dyCenter * CENTER_GRAVITY

                node.vx = (node.vx + forcesX[i]) * DAMPING * simulationAlpha
                node.vy = (node.vy + forcesY[i]) * DAMPING * simulationAlpha

                node.vx = node.vx.coerceIn(-50f, 50f)
                node.vy = node.vy.coerceIn(-50f, 50f)

                node.x += node.vx
                node.y += node.vy

                val speed = abs(node.vx) + abs(node.vy)
                if (speed > maxVel) maxVel = speed
            }

            // Energy Decay
            if (draggedNodeId == null) {
                simulationAlpha *= 0.98f
            }

            // OPTIMASI: Matikan simulasi lebih agresif jika gerakan minim
            if (maxVel < 0.5f) simulationAlpha = 0f

            // OPTIMASI: Cap framerate ke ~30 FPS (33ms) bukan 60 FPS (16ms)
            // Graf tidak butuh sehalus game action, ini menghemat baterai signifikan
            delay(33)
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
                    // Bangunkan simulasi sebentar agar UI responsif
                    if (simulationAlpha < 0.1f) simulationAlpha = 0.5f
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val touchX = (startOffset.x - offset.x) / scale
                        val touchY = (startOffset.y - offset.y) / scale
                        val nearestNode = graphNodes.minByOrNull {
                            val dx = it.x - touchX
                            val dy = it.y - touchY
                            dx * dx + dy * dy
                        }
                        if (nearestNode != null) {
                            val dist = sqrt(
                                (nearestNode.x - touchX).pow(2) + (nearestNode.y - touchY).pow(2)
                            )
                            if (dist < 60f / scale) {
                                draggedNodeId = nearestNode.id
                                wakeUpSimulation()
                            }
                        }
                    },
                    onDragEnd = { draggedNodeId = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedNodeId?.let { id ->
                            val node = graphNodes.find { it.id == id }
                            if (node != null) {
                                node.x += dragAmount.x / scale
                                node.y += dragAmount.y / scale
                                wakeUpSimulation()
                            }
                        }
                    },
                    onDragCancel = { draggedNodeId = null }
                )
            }
    ) {
        if (graphNodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada catatan untuk dipetakan", color = Color.Gray)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Apply transform global sekali saja untuk efisiensi drawing
                withTransform({
                    translate(offset.x, offset.y)
                    scale(scale, scale, Offset.Zero)
                }) {
                    // Layar virtual boundary untuk culling
                    val viewportLeft = -offset.x / scale
                    val viewportTop = -offset.y / scale
                    val viewportRight = viewportLeft + size.width / scale
                    val viewportBottom = viewportTop + size.height / scale
                    val buffer = 50f // Buffer agar elemen di tepi tidak terpotong kasar

                    // A. Gambar Link
                    links.forEach { link ->
                        val n1 = graphNodes.find { it.id == link.sourceNoteId }
                        val n2 = graphNodes.find { it.id == link.targetNoteId }

                        if (n1 != null && n2 != null) {
                            // Simple Culling untuk Garis
                            val minX = minOf(n1.x, n2.x); val maxX = maxOf(n1.x, n2.x)
                            val minY = minOf(n1.y, n2.y); val maxY = maxOf(n1.y, n2.y)

                            if (maxX > viewportLeft - buffer && minX < viewportRight + buffer &&
                                maxY > viewportTop - buffer && minY < viewportBottom + buffer) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(n1.x, n1.y),
                                    end = Offset(n2.x, n2.y),
                                    strokeWidth = (2.dp.toPx() / scale).coerceAtLeast(1f) // Keep visual thickness constant-ish
                                )
                            }
                        }
                    }

                    // B. Gambar Node
                    graphNodes.forEach { node ->
                        // Culling Node
                        if (node.x > viewportLeft - buffer && node.x < viewportRight + buffer &&
                            node.y > viewportTop - buffer && node.y < viewportBottom + buffer) {

                            val radius = if (node.isPermanent) 18.dp.toPx() else 12.dp.toPx()

                            drawCircle(
                                color = node.color,
                                radius = radius,
                                center = Offset(node.x, node.y)
                            )

                            if (node.isPermanent) {
                                drawCircle(
                                    color = node.color.copy(alpha = 0.3f),
                                    radius = radius + 4.dp.toPx(),
                                    center = Offset(node.x, node.y),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }

                            // Text Label (Hanya jika zoom in cukup dekat)
                            if (scale > 0.4f) {
                                val textLayout = textMeasurer.measure(
                                    text = node.title,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        color = labelTextColor,
                                        background = labelBackgroundColor
                                    )
                                )
                                drawText(
                                    textLayout,
                                    topLeft = Offset(
                                        node.x - textLayout.size.width / 2,
                                        node.y + radius + 4.dp.toPx()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}