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
import androidx.compose.ui.graphics.PathEffect
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
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
private const val REPULSION_FORCE = 5000f // Gaya tolak antar node (agar tidak tumpang tindih)
private const val ATTRACTION_FORCE = 0.05f // Gaya tarik pegas (untuk node yg terhubung)
private const val SPRING_LENGTH = 150f     // Panjang ideal garis penghubung
private const val DAMPING = 0.9f           // Gesekan (agar animasi berhenti perlahan)
private const val CENTER_GRAVITY = 0.01f   // Menarik node liar ke tengah

@Composable
fun KnowledgeGraphView(
    notes: List<KnowledgeNote>,
    links: List<NoteLink>,
    modifier: Modifier = Modifier,
    onNodeClick: (Long) -> Unit
) {
    // 1. Inisialisasi State Node (Hanya sekali saat data notes berubah)
    val graphNodes = remember(notes) {
        notes.map { note ->
            GraphNode(
                id = note.id,
                title = note.title.ifBlank { "Untitled" },
                isPermanent = note.isPermanent,
                // Posisi awal random di sekitar tengah
                x = Random.nextFloat() * 500f + 200f,
                y = Random.nextFloat() * 800f + 400f,
                color = if (note.isPermanent) Color(0xFF8B5CF6) else Color(0xFF0D9488)
            )
        }
    }

    // State untuk Viewport (Zoom & Pan)
    var scale by remember { mutableStateOf(0.8f) }
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    // State Interaksi
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }
    var dragging by remember { mutableStateOf(false) }

    val textMeasurer = rememberTextMeasurer()

    // PRE-CALCULATE COLORS (Must be done in Composable scope)
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val labelTextColor = MaterialTheme.colorScheme.onSurface
    val labelBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)

    // 2. SIMULASI FISIKA (Loop Animasi)
    LaunchedEffect(graphNodes, links, dragging) {
        while (isActive) {
            val width = 1000f // Area simulasi virtual
            val height = 1500f
            val centerX = width / 2
            val centerY = height / 2

            // Reset gaya
            val forcesX = FloatArray(graphNodes.size) { 0f }
            val forcesY = FloatArray(graphNodes.size) { 0f }

            // A. GAYA TOLAK (Repulsion) - Semua node saling tolak
            for (i in graphNodes.indices) {
                for (j in i + 1 until graphNodes.size) {
                    val n1 = graphNodes[i]
                    val n2 = graphNodes[j]

                    val dx = n1.x - n2.x
                    val dy = n1.y - n2.y
                    val distSq = dx * dx + dy * dy
                    val dist = sqrt(distSq).coerceAtLeast(1f) // Hindari bagi nol

                    // Rumus Coulomb Sederhana: F = k / dist
                    val force = REPULSION_FORCE / dist

                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    forcesX[i] += fx
                    forcesY[i] += fy
                    forcesX[j] -= fx
                    forcesY[j] -= fy
                }
            }

            // B. GAYA TARIK (Attraction) - Pegas antar node terhubung
            links.forEach { link ->
                val sourceIndex = graphNodes.indexOfFirst { it.id == link.sourceNoteId }
                val targetIndex = graphNodes.indexOfFirst { it.id == link.targetNoteId }

                if (sourceIndex != -1 && targetIndex != -1) {
                    val n1 = graphNodes[sourceIndex]
                    val n2 = graphNodes[targetIndex]

                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

                    // Hukum Hooke: F = k * (jarak_sekarang - panjang_ideal)
                    val force = (dist - SPRING_LENGTH) * ATTRACTION_FORCE

                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force

                    forcesX[sourceIndex] += fx
                    forcesY[sourceIndex] += fy
                    forcesX[targetIndex] -= fx
                    forcesY[targetIndex] -= fy
                }
            }

            // C. CENTER GRAVITY & UPDATE POSISI
            for (i in graphNodes.indices) {
                val node = graphNodes[i]

                // Jangan gerakkan node yang sedang di-drag user
                if (node.id == draggedNodeId) continue

                // Tarik sedikit ke tengah agar tidak kabur
                val dxCenter = centerX - node.x
                val dyCenter = centerY - node.y
                forcesX[i] += dxCenter * CENTER_GRAVITY
                forcesY[i] += dyCenter * CENTER_GRAVITY

                // Update Kecepatan + Damping
                node.vx = (node.vx + forcesX[i]) * DAMPING
                node.vy = (node.vy + forcesY[i]) * DAMPING

                // Batas Kecepatan Maksimum (agar tidak meledak)
                node.vx = node.vx.coerceIn(-20f, 20f)
                node.vy = node.vy.coerceIn(-20f, 20f)

                // Update Posisi
                node.x += node.vx
                node.y += node.vy
            }

            // Frame Rate limit (~60fps)
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // Gesture: Zoom & Pan Area
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.1f, 3f)
                    offset += pan
                }
            }
            // Gesture: Drag Node
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        // Cari node terdekat dengan sentuhan
                        val touchX = (startOffset.x - offset.x) / scale
                        val touchY = (startOffset.y - offset.y) / scale

                        // Hitung jarak ke node terdekat
                        val nearestNode = graphNodes.minByOrNull { node ->
                            val dx = node.x - touchX
                            val dy = node.y - touchY
                            dx * dx + dy * dy
                        }

                        // Jika cukup dekat (radius 50), mulai drag
                        if (nearestNode != null) {
                            val dist = sqrt(
                                (nearestNode.x - touchX).pow(2) + (nearestNode.y - touchY).pow(2)
                            )
                            if (dist < 50f) {
                                draggedNodeId = nearestNode.id
                                dragging = true
                                // Stop momentum saat dipegang
                                nearestNode.vx = 0f
                                nearestNode.vy = 0f
                            }
                        }
                    },
                    onDragEnd = {
                        draggedNodeId = null
                        dragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedNodeId?.let { id ->
                            val node = graphNodes.find { it.id == id }
                            if (node != null) {
                                node.x += dragAmount.x / scale
                                node.y += dragAmount.y / scale
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
                // Terapkan Transformasi Global (Zoom & Pan)

                // 1. GAMBAR GARIS PENGHUBUNG (Links)
                links.forEach { link ->
                    val n1 = graphNodes.find { it.id == link.sourceNoteId }
                    val n2 = graphNodes.find { it.id == link.targetNoteId }

                    if (n1 != null && n2 != null) {
                        val start = Offset(n1.x * scale + offset.x, n1.y * scale + offset.y)
                        val end = Offset(n2.x * scale + offset.x, n2.y * scale + offset.y)

                        drawLine(
                            color = lineColor,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx() * scale
                        )
                    }
                }

                // 2. GAMBAR NODE (Lingkaran)
                graphNodes.forEach { node ->
                    val screenX = node.x * scale + offset.x
                    val screenY = node.y * scale + offset.y
                    val radius = (if (node.isPermanent) 18.dp.toPx() else 12.dp.toPx()) * scale

                    // Cek apakah node ada di layar (Performance Culling)
                    if (screenX > -100 && screenX < size.width + 100 &&
                        screenY > -100 && screenY < size.height + 100
                    ) {
                        drawCircle(
                            color = node.color,
                            radius = radius,
                            center = Offset(screenX, screenY)
                        )

                        // Outer Glow jika sedang di-drag
                        if (node.id == draggedNodeId) {
                            drawCircle(
                                color = node.color.copy(alpha = 0.3f),
                                radius = radius * 1.5f,
                                center = Offset(screenX, screenY)
                            )
                        }

                        // Teks Judul (Hanya jika zoom cukup besar)
                        if (scale > 0.6f) {
                            val textLayout = textMeasurer.measure(
                                text = node.title,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 10.sp * scale,
                                    color = labelTextColor, // Use pre-calculated color
                                    background = labelBackgroundColor // Use pre-calculated color
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    screenX - textLayout.size.width / 2,
                                    screenY + radius + 5f
                                )
                            )
                        }
                    }
                }
            }

            // HUD Kontrol
            Text(
                "Pinch to Zoom • Drag to Move",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}