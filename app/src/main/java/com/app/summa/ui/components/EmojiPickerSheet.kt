package com.app.summa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    onDismissRequest: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Curated list of emojis suitable for habit tracking
    val emojis = listOf(
        // General / Success
        "🎯", "⭐", "🔥", "✨", "⚡", "🚀", "🏆", "🥇", "💯", "✅", "🎉",

        // Health & Fitness
        "💪", "🏃", "🧘", "💧", "🍎", "🥦", "💊", "😴", "🛀", "🦷", "🏋️", "🚴", "🤸", "🥗", "🥑", "🥕",

        // Learning & Productivity
        "📚", "💻", "✍️", "🧠", "💼", "📅", "📝", "🎓", "📈", "📊", "💡", "⏰", "👓", "🔬",

        // Hobbies & Creativity
        "🎨", "🎸", "🎮", "📷", "🍳", "✈️", "🧶", "🎹", "🎤", "🎧", "🎬", "🎭", "🎪", "🎫",

        // Home & Chores
        "🧹", "🧺", "🪴", "🏠", "🛌", "🔧", "🔨", "🧼", "🗑️", "🛒", "🐶", "🐱",

        // Money & Finance
        "💰", "💵", "💳", "🐖", "💎", "🏦",

        // Mindfulness & Spirit
        "🙏", "🕯️", "☮️", "☯️", "🧘‍♀️", "🍃", "☀️", "🌙"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Pilih Ikon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onEmojiSelected(emoji) }
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}
