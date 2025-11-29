package com.app.summa.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Warna Emas untuk Koin
private val CoinGold = Color(0xFFFFD700)
private val CoinDarkGold = Color(0xFFDAA520)
private val CoinLightGold = Color(0xFFFFF8DC)

data class Particle(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val rotationSpeed: Float,
    val color: Color
)

@Composable
fun CoinExplosionAnimation(
    modifier: Modifier = Modifier,
    trigger: Boolean, // Trigger animasi saat true
    onFinished: () -> Unit
) {
    if (!trigger) return

    val particles = remember {
        (0..40).map { id -> // 40 Koin
            val angle = Random.nextFloat() * 360f
            val speed = Random.nextFloat() * 20f + 10f
            val size = Random.nextFloat() * 15f + 10f
            val color = when (Random.nextInt(3)) {
                0 -> CoinGold
                1 -> CoinDarkGold
                else -> CoinLightGold
            }
            Particle(
                id = id,
                startX = 0.5f, // Tengah relatif (0.0 - 1.0)
                startY = 0.5f,
                angle = angle,
                speed = speed,
                size = size,
                rotationSpeed = Random.nextFloat() * 10f - 5f,
                color = color
            )
        }
    }

    // Animasi Progress (0f -> 1f)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        particles.forEach { particle ->
            // Fisika Sederhana: Gerak Lurus + Gravitasi
            val time = progress.value * 50f // Skala waktu

            // Posisi X: Gerak lurus berdasarkan sudut
            val radians = Math.toRadians(particle.angle.toDouble())
            val dx = cos(radians).toFloat() * particle.speed * time

            // Posisi Y: Gerak lurus + Gravitasi (0.5 * g * t^2)
            val gravity = 0.8f // Kekuatan gravitasi
            val dy = (sin(radians).toFloat() * particle.speed * time) + (0.5f * gravity * time * time)

            val x = centerX + dx
            val y = centerY + dy - 100f // Sedikit ke atas agar meledak dari tengah kartu

            // Rotasi koin (visual 3D sederhana)
            val rotation = progress.value * 360f * 2 + (particle.id * 10f)
            val scaleX = cos(Math.toRadians(rotation.toDouble())).toFloat()

            if (y < height + 50f && progress.value < 1f) {
                withTransform({
                    translate(left = x, top = y)
                    rotate(rotation)
                    scale(scaleX, 1f)
                }) {
                    drawCircle(
                        color = particle.color,
                        radius = particle.size,
                        alpha = 1f - progress.value // Fade out di akhir
                    )
                }
            }
        }
    }
}