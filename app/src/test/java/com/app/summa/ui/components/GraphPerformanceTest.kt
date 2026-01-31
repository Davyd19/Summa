package com.app.summa.ui.components

import androidx.compose.ui.graphics.Color
import com.app.summa.data.model.NoteLink
import org.junit.Test
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.math.sqrt

class GraphPerformanceTest {

    @Test
    fun benchmarkAttractionLoop() {
        val nodeCount = 1000
        val linkCount = 2000

        // Setup data
        val nodes = List(nodeCount) { i ->
            GraphNode(
                id = i.toLong(),
                title = "Node $i",
                isPermanent = false,
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * 1000f,
                color = Color.Red
            )
        }

        val links = List(linkCount) {
            val sId = Random.nextInt(nodeCount).toLong()
            var tId = Random.nextInt(nodeCount).toLong()
            while (tId == sId) {
                tId = Random.nextInt(nodeCount).toLong()
            }
            NoteLink(sourceNoteId = sId, targetNoteId = tId)
        }

        // Dummy force arrays
        val forcesX = FloatArray(nodeCount)
        val forcesY = FloatArray(nodeCount)
        val ATTRACTION_FORCE = 0.04f
        val SPRING_LENGTH = 200f

        // Warmup
        repeat(10) {
            runOriginal(links, nodes, forcesX, forcesY, ATTRACTION_FORCE, SPRING_LENGTH)
        }

        // Benchmark Original
        val originalTime = measureNanoTime {
            repeat(100) {
                runOriginal(links, nodes, forcesX, forcesY, ATTRACTION_FORCE, SPRING_LENGTH)
            }
        } / 100.0

        println("Original Implementation Average Time: ${originalTime / 1_000_000.0} ms")

        // Benchmark Optimized
        val optimizedTime = measureNanoTime {
            repeat(100) {
                runOptimized(links, nodes, forcesX, forcesY, ATTRACTION_FORCE, SPRING_LENGTH)
            }
        } / 100.0

        println("Optimized Implementation Average Time: ${optimizedTime / 1_000_000.0} ms")

        val improvement = originalTime / optimizedTime
        println("Speedup: ${String.format("%.2f", improvement)}x")
    }

    private fun runOriginal(
        links: List<NoteLink>,
        graphNodes: List<GraphNode>,
        forcesX: FloatArray,
        forcesY: FloatArray,
        ATTRACTION_FORCE: Float,
        SPRING_LENGTH: Float
    ) {
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
    }

    private fun runOptimized(
        links: List<NoteLink>,
        graphNodes: List<GraphNode>,
        forcesX: FloatArray,
        forcesY: FloatArray,
        ATTRACTION_FORCE: Float,
        SPRING_LENGTH: Float
    ) {
        // Pre-calculation (included in the loop time as per the plan to do it every frame or just once?
        // In the real code it will be outside the loop if graphNodes is constant, or inside if dynamic.
        // Assuming graphNodes is constant within the frame loop scope (the LaunchedEffect logic),
        // we should create the map BEFORE the loop over links.

        // To be fair, if we put it inside the 'while' loop but outside the 'links.forEach',
        // it adds O(N) overhead once per frame.
        // The original code does O(N) * L inside the frame.
        // So O(N) + O(L) vs O(N*L).

        val nodeIndices = graphNodes.withIndex().associate { it.value.id to it.index }

        links.forEach { link ->
            val sIdx = nodeIndices[link.sourceNoteId] ?: -1
            val tIdx = nodeIndices[link.targetNoteId] ?: -1

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
    }
}
