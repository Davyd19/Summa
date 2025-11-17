package com.app.summa.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProgressCircle(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    showPercentage: Boolean = true
) {
    var animatedProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(progress) {
        animatedProgress = progress
    }

    val animProgress by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000)
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.toPx() / 2

            drawCircle(
                color = backgroundColor,
                radius = radius,
                style = Stroke(width = strokeWidth.toPx())
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animProgress,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        if (showPercentage) {
            Text(
                text = "${(animProgress * 100).toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}