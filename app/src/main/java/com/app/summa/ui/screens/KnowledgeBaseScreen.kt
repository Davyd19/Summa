package com.app.summa.ui.screens

import com.app.summa.ui.components.*
import androidx.compose.foundation.clickable

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.KnowledgeNote
import com.app.summa.ui.components.BrutalistCard
import com.app.summa.ui.components.BrutalTopAppBar
import com.app.summa.ui.components.brutalBorder
import com.app.summa.ui.components.brutalTextFieldColors
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.KnowledgeViewModel
import kotlinx.coroutines.launch
import com.app.summa.ui.components.KnowledgeGraphView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KnowledgeBaseScreen(
    viewModel: KnowledgeViewModel = hiltViewModel(),
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("Inbox", "Pustaka", "Graph")

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "Pustaka Pengetahuan",
                subtitle = "Kelola ide dan pembelajaran Anda"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Cari catatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(8.dp),
                colors = brutalTextFieldColors(),
                singleLine = true
            )

            BrutalistCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Brutal segmented tabs container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Surface(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .brutalBorder(strokeWidth = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val filteredInbox = uiState.inboxNotes.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.tags.contains(searchQuery, ignoreCase = true)
                }

                val filteredPermanent = uiState.permanentNotes.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.tags.contains(searchQuery, ignoreCase = true)
                }

                when (page) {
                    0 -> EnhancedNoteList(
                        notes = filteredInbox,
                        onNoteClick = onNoteClick,
                        emptyIcon = "📥",
                        emptyTitle = if(searchQuery.isEmpty()) "Inbox Kosong" else "Tidak Ditemukan",
                        emptyText = if(searchQuery.isEmpty()) "Tekan tombol '+' untuk mencatat ide baru" else "Coba kata kunci lain",
                        // PERBAIKAN: Menambahkan callback untuk Quick Promote
                        onQuickPromote = { viewModel.promoteNote(it) }
                    )
                    1 -> EnhancedNoteList(
                        notes = filteredPermanent,
                        onNoteClick = onNoteClick,
                        emptyIcon = "📚",
                        emptyTitle = if(searchQuery.isEmpty()) "Pustaka Kosong" else "Tidak Ditemukan",
                        emptyText = if(searchQuery.isEmpty()) "Arsipkan catatan dari Inbox ke sini" else "Coba kata kunci lain"
                    )
                    2 -> {
                        val allNotes = filteredInbox + filteredPermanent
                        if (allNotes.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Tidak ada data untuk grafik", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            KnowledgeGraphView(
                                notes = allNotes,
                                links = uiState.allLinks,
                                onNodeClick = onNoteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedNoteList(
    notes: List<KnowledgeNote>,
    onNoteClick: (Long) -> Unit,
    emptyIcon: String,
    emptyTitle: String,
    emptyText: String,
    onQuickPromote: ((KnowledgeNote) -> Unit)? = null
) {
    if (notes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .brutalBorder(radius=120.dp, strokeWidth=1.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ), CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emptyIcon,
                    style = MaterialTheme.typography.displayLarge
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                emptyTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                EnhancedNoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onPromote = onQuickPromote
                )
            }
        }
    }
}

@Composable
fun EnhancedNoteCard(
    note: KnowledgeNote,
    onClick: () -> Unit,
    onPromote: ((KnowledgeNote) -> Unit)? = null
) {
    BrutalistCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bagian Kanan: Indikator atau Tombol Aksi
                if (note.isPermanent) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PurpleAccent.copy(alpha = 0.15f),
                        modifier = Modifier.brutalBorder(radius=4.dp, strokeWidth=1.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Permanen",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            tint = PurpleAccent
                        )
                    }
                } else if (onPromote != null) {
                    // TOMBOL QUICK PROMOTE UNTUK INBOX
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .brutalBorder(strokeWidth = 2.dp, radius = 40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clickable { onPromote(note) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DriveFileMove, // Icon pindah/arsip
                            contentDescription = "Simpan ke Pustaka",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (note.tags.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    note.tags.split(",").take(3).forEach { tag ->
                        BrutalistTag(text = tag.trim())
                    }
                }
            }
        }
    }
}
