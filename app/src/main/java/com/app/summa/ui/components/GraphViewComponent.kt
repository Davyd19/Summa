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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.ceil
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

// --- KONFIGURASI FISIKA ---
private const val REPULSION_FORCE = 8000f // Gaya tolak lebih kuat agar tidak tumpang tindih
private const val ATTRACTION_FORCE = 0.04f // Gaya tarik pegas
private const val SPRING_LENGTH = 200f // Panjang ideal garis penghubung
private const val DAMPING = 0.85f // Gesekan udara (0.9 = licin, 0.7 = kental)
private const val CENTER_GRAVITY = 0.015f // Gravitasi ke tengah layar
private const val GRID_SIZE = 250f // Ukuran sel grid untuk optimasi spasial

@Composable
fun KnowledgeGraphView(
    notes: List<KnowledgeNote>,
    links: List<NoteLink>,
    modifier: Modifier = Modifier,
    onNodeClick: (Long) -> Unit
) {
    // 1. Inisialisasi Node (Hanya dijalankan jika daftar notes berubah)
    // Menggunakan remember(notes) agar posisi tidak reset saat recompose UI kecil lainnya
    val graphNodes = remember(notes) {
        notes.map { note ->
            GraphNode(
                id = note.id,
                title = note.title.ifBlank { "Untitled" },
                isPermanent = note.isPermanent,
                // Spawn acak di area tengah agar tidak meledak dari titik 0,0
                x = Random.nextFloat() * 800f + 100f,
                y = Random.nextFloat() * 1200f + 200f,
                color = if (note.isPermanent) Color(0xFF8B5CF6) else Color(0xFF0D9488) // Purple / Teal
            )
        }
    }

    // State Viewport (Zoom & Pan)
    var scale by remember { mutableStateOf(0.6f) } // Default agak zoom out agar terlihat banyak
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    // State Interaksi
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }

    // State Simulasi (Energy System)
    // alpha = 1.0 (Panas/Bergerak) -> 0.0 (Dingin/Berhenti)
    var simulationAlpha by remember { mutableStateOf(1.0f) }

    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface
    val labelBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)

    // --- FUNGSI UNTUK MEMBANGUNKAN SIMULASI ---
    fun wakeUpSimulation() {
        simulationAlpha = 1.0f
    }

    // Trigger bangun saat data berubah
    LaunchedEffect(notes, links) { wakeUpSimulation() }

    // 2. LOOP SIMULASI FISIKA (High Performance)
    LaunchedEffect(Unit) {
        while (isActive) {
            // Jika energi habis (dingin) dan user tidak sedang drag, stop kalkulasi (HEMAT BATERAI)
            if (simulationAlpha < 0.01f && draggedNodeId == null) {
                delay(100) // Cek lagi nanti dengan interval lambat
                continue
            }

            val width = 2000f // Virtual world width
            val height = 2000f // Virtual world height
            val centerX = width / 2
            val centerY = height / 2

            // Array gaya sementara
            val forcesX = FloatArray(graphNodes.size)
            val forcesY = FloatArray(graphNodes.size)

            // --- OPTIMASI 1: SPATIAL HASHING / GRID ---
            // Daripada O(N^2), kita masukkan node ke kotak-kotak (grid).
            // Kita hanya menghitung gaya tolak-menolak dengan node di kotak yang sama/tetangga.
            val grid = HashMap<Int, ArrayList<Int>>()

            // Masukkan node ke grid
            for (i in graphNodes.indices) {
                val node = graphNodes[i]
                val gridX = floor(node.x / GRID_SIZE).toInt()
                val gridY = floor(node.y / GRID_SIZE).toInt()
                val key = (gridX * 73856093) xor (gridY * 19349663) // Simple hash key

                if (!grid.containsKey(key)) {
                    grid[key] = ArrayList()
                }
                grid[key]?.add(i)
            }

            // Hitung Repulsion (Tolak-menolak) menggunakan Grid
            for (i in graphNodes.indices) {
                val n1 = graphNodes[i]
                val gridX = floor(n1.x / GRID_SIZE).toInt()
                val gridY = floor(n1.y / GRID_SIZE).toInt()

                // Cek sel ini dan 8 sel tetangga
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val key = ((gridX + dx) * 73856093) xor ((gridY + dy) * 19349663)
                        val neighbors = grid[key] ?: continue

                        for (j in neighbors) {
                            if (i == j) continue // Jangan tolak diri sendiri

                            val n2 = graphNodes[j]
                            val distX = n1.x - n2.x
                            val distY = n1.y - n2.y
                            // Jarak kuadrat (hindari sqrt untuk performa jika memungkinkan, tapi di sini kita butuh dist asli)
                            val distSq = distX * distX + distY * distY

                            // Optimasi: Jika terlalu jauh (di luar radius grid), abaikan gayanya
                            if (distSq > GRID_SIZE * GRID_SIZE) continue

                            val dist = sqrt(distSq).coerceAtLeast(1f) // Hindari pembagian 0
                            val force = REPULSION_FORCE / (dist * dist) // Inverse square law (mirip gravitasi/magnet)

                            val fx = (distX / dist) * force
                            val fy = (distY / dist) * force

                            forcesX[i] += fx
                            forcesY[i] += fy
                        }
                    }
                }
            }

            // Hitung Attraction (Tarik-menarik pegas pada Link) - O(E) links
            links.forEach { link ->
                val sIdx = graphNodes.indexOfFirst { it.id == link.sourceNoteId }
                val tIdx = graphNodes.indexOfFirst { it.id == link.targetNoteId }

                if (sIdx != -1 && tIdx != -1) {
                    val n1 = graphNodes[sIdx]
                    val n2 = graphNodes[tIdx]

                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

                    // Hukum Hooke: F = k * (current_length - target_length)
                    val force = (dist - SPRING_LENGTH) * ATTRACTION_FORCE

                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    forcesX[sIdx] += fx
                    forcesY[sIdx] += fy
                    forcesX[tIdx] -= fx
                    forcesY[tIdx] -= fy
                }
            }

            // Terapkan Gaya & Gravitasi Pusat
            var maxVel = 0f
            for (i in graphNodes.indices) {
                val node = graphNodes[i]

                // Jika sedang didrag, jangan update posisi fisika, ikuti jari user
                if (node.id == draggedNodeId) {
                    node.vx = 0f
                    node.vy = 0f
                    continue
                }

                // Gravitasi Lemah ke Tengah (agar graf tidak terbang menjauh ke infinity)
                val dxCenter = centerX - node.x
                val dyCenter = centerY - node.y
                forcesX[i] += dxCenter * CENTER_GRAVITY
                forcesY[i] += dyCenter * CENTER_GRAVITY

                // Update Kecepatan + Damping (Gesekan)
                // simulationAlpha membuat gerakan melambat seiring waktu (cooling down)
                node.vx = (node.vx + forcesX[i]) * DAMPING * simulationAlpha
                node.vy = (node.vy + forcesY[i]) * DAMPING * simulationAlpha

                // Batasi kecepatan maksimum (agar tidak meledak)
                node.vx = node.vx.coerceIn(-50f, 50f)
                node.vy = node.vy.coerceIn(-50f, 50f)

                // Update Posisi
                node.x += node.vx
                node.y += node.vy

                val speed = abs(node.vx) + abs(node.vy)
                if (speed > maxVel) maxVel = speed
            }

            // --- ENERGY DECAY (PENDINGINAN) ---
            // Kurangi suhu simulasi sedikit demi sedikit
            // Jika user drag, suhu tetap 1.0 (diatur di onDrag)
            if (draggedNodeId == null) {
                simulationAlpha *= 0.98f // Decay factor (semakin kecil, semakin cepat berhenti)
            }

            // Jika semua node hampir diam, matikan simulasi lebih cepat
            if (maxVel < 0.1f) simulationAlpha = 0f

            delay(16) // Target ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Gesture: Zoom & Pan
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 3f)
                    // Rotasi pan agar sesuai orientasi tidak diperlukan di sini
                    // Sesuaikan offset dengan scale agar zooming terasa natural
                    offset += pan
                    // Bangunkan simulasi sebentar saat user merubah view, agar UI terasa responsif
                    if (simulationAlpha < 0.1f) simulationAlpha = 0.5f
                }
            }
            // Gesture: Drag Node
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        // Konversi koordinat layar ke koordinat dunia graf
                        val touchX = (startOffset.x - offset.x) / scale
                        val touchY = (startOffset.y - offset.y) / scale

                        // Cari node terdekat
                        val nearestNode = graphNodes.minByOrNull {
                            val dx = it.x - touchX
                            val dy = it.y - touchY
                            dx * dx + dy * dy
                        }

                        if (nearestNode != null) {
                            val dist = sqrt(
                                (nearestNode.x - touchX).pow(2) +
                                        (nearestNode.y - touchY).pow(2)
                            )
                            // Hitbox sentuhan: 60 unit (sesuaikan dengan jari)
                            if (dist < 60f / scale) {
                                draggedNodeId = nearestNode.id
                                wakeUpSimulation() // Bangunkan simulasi penuh
                            }
                        }
                    },
                    onDragEnd = {
                        draggedNodeId = null
                        // Biarkan simulasi berjalan sebentar lalu cooling down
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedNodeId?.let { id ->
                            val node = graphNodes.find { it.id == id }
                            if (node != null) {
                                // Pindahkan node secara langsung
                                node.x += dragAmount.x / scale
                                node.y += dragAmount.y / scale
                                wakeUpSimulation() // Jaga simulasi tetap panas saat drag
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
                val screenWidth = size.width
                val screenHeight = size.height

                // --- 3. RENDERING (Optimized Culling) ---

                // A. Gambar Garis (Links)
                links.forEach { link ->
                    val n1 = graphNodes.find { it.id == link.sourceNoteId }
                    val n2 = graphNodes.find { it.id == link.targetNoteId }

                    if (n1 != null && n2 != null) {
                        val startX = n1.x * scale + offset.x
                        val startY = n1.y * scale + offset.y
                        val endX = n2.x * scale + offset.x
                        val endY = n2.y * scale + offset.y

                        // Culling Sederhana: Cek apakah garis setidaknya sebagian ada di layar
                        val minX = minOf(startX, endX); val maxX = maxOf(startX, endX)
                        val minY = minOf(startY, endY); val maxY = maxOf(startY, endY)

                        if (maxX > 0 && minX < screenWidth && maxY > 0 && minY < screenHeight) {
                            drawLine(
                                color = lineColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = (2.dp.toPx() * scale).coerceAtMost(5f)
                            )
                        }
                    }
                }

                // B. Gambar Nodes
                graphNodes.forEach { node ->
                    val screenX = node.x * scale + offset.x
                    val screenY = node.y * scale + offset.y

                    // Radius node
                    val baseRadius = if (node.isPermanent) 18.dp.toPx() else 12.dp.toPx()
                    val radius = baseRadius * scale

                    // Culling: Hanya gambar jika node masuk area layar (+ buffer sedikit)
                    if (screenX > -radius && screenX < screenWidth + radius &&
                        screenY > -radius && screenY < screenHeight + radius) {

                        drawCircle(
                            color = node.color,
                            radius = radius,
                            center = Offset(screenX, screenY)
                        )

                        // Efek outline untuk node permanen
                        if (node.isPermanent) {
                            drawCircle(
                                color = node.color.copy(alpha = 0.3f),
                                radius = radius + (4.dp.toPx() * scale),
                                center = Offset(screenX, screenY),
                                style = Stroke(width = 1.dp.toPx() * scale)
                            )
                        }

                        // Hanya gambar teks jika zoom cukup besar (Detail Level)
                        if (scale > 0.4f) {
                            val textLayout = textMeasurer.measure(
                                text = node.title,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = (10.sp.toPx() * scale).toSp(), // Font scaling manual
                                    color = labelTextColor,
                                    background = labelBackgroundColor
                                )
                            )
                            // Center text below node
                            drawText(
                                textLayout,
                                topLeft = Offset(
                                    screenX - textLayout.size.width / 2,
                                    screenY + radius + (4.dp.toPx() * scale)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Extension helper untuk konversi px ke sp
fun Float.toSp(): androidx.compose.ui.unit.TextUnit = (this / 3f).sp // Rough approximation