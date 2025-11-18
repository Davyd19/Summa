package com.app.summa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // PERBAIKAN: Menggunakan warna dari MaterialTheme
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    padding: Dp = 16.dp, // Mengurangi padding default untuk fleksibilitas
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = backgroundColor)
    // PERBAIKAN: Mengganti elevasi dengan border tipis
    val border = BorderStroke(1.dp, borderColor)

    // PERBAIKAN: Menstandarisasi bentuk rounded
    val shape = MaterialTheme.shapes.large // (12.dp)

    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            shape = shape,
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Tanpa bayangan
            border = border
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Tanpa bayangan
            border = border
        ) {
            Column(
                modifier = Modifier.padding(padding),
                content = content
            )
        }
    }
}