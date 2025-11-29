package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// IMPORT PENTING YANG HILANG
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Identity
import com.app.summa.ui.theme.GoldAccent
import com.app.summa.ui.theme.SuccessGreen
import com.app.summa.ui.viewmodel.ReflectionViewModel
import com.app.summa.ui.viewmodel.VoteSuggestion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReflectionScreen(
    viewModel: ReflectionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Langkah ${pagerState.currentPage + 1}/4",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (pagerState.currentPage < 3) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Lanjut", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            } else {
                Button(
                    // PERBAIKAN: Panggil completeReflection sebelum kembali
                    onClick = {
                        viewModel.completeReflection()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Selesai & Simpan", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> RitualStepOne(uiState.summary?.completedTasks ?: emptyList(), uiState.summary?.completedHabits ?: emptyList())
                        1 -> RitualStepTwo(uiState.suggestions) { i, p, n -> viewModel.addVote(i, p, n) }
                        2 -> RitualStepThree(uiState.identities) { i, p, n -> viewModel.addVote(i, p, n) }
                        // PERBAIKAN: Gunakan updateReflectionText yang benar
                        3 -> RitualStepFour(uiState.reflectionText) { viewModel.updateReflectionText(it) }
                    }
                }
            }
        }
    }
}

// --- LANGKAH 1: KEMENANGAN KECIL (Review) ---
@Composable
fun RitualStepOne(tasks: List<com.app.summa.data.model.Task>, habits: List<com.app.summa.data.model.Habit>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🚀", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Kemenangan Hari Ini",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Rayakan setiap progres, sekecil apapun.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                WinSection("Tugas Selesai", tasks.map { it.title }, "Belum ada tugas selesai")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                WinSection("Kebiasaan", habits.map { it.name }, "Belum ada kebiasaan selesai")
            }
        }
    }
}

@Composable
fun WinSection(title: String, items: List<String>, empty: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        if (items.isEmpty()) {
            Text(empty, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        } else {
            items.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// --- LANGKAH 2: SMART SUGGESTIONS (Voting Cerdas) ---
@Composable
fun RitualStepTwo(suggestions: List<VoteSuggestion>, onVote: (Identity, Int, String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🗳️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Bukti Identitas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Aktivitas Anda membuktikan siapa Anda.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (suggestions.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Tidak ada saran otomatis hari ini.\nLanjut ke manual vote.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                suggestions.forEach { suggestion ->
                    SuggestionVoteCard(suggestion, onVote)
                }
            }
        }
    }
}

@Composable
fun SuggestionVoteCard(suggestion: VoteSuggestion, onVote: (Identity, Int, String) -> Unit) {
    var voted by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = !voted) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Vote untuk:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(suggestion.identity.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        onVote(suggestion.identity, suggestion.points, suggestion.reason)
                        voted = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+${suggestion.points}", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- LANGKAH 3: MANUAL VOTE (Eksplorasi) ---
@Composable
fun RitualStepThree(identities: List<Identity>, onVote: (Identity, Int, String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💎", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Siapa Kamu Hari Ini?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Beri poin tambahan untuk identitas yang kamu rasakan.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            identities.forEach { identity ->
                CompactIdentityVoteCard(identity, onVote)
            }
        }
    }
}

@Composable
fun CompactIdentityVoteCard(identity: Identity, onVote: (Identity, Int, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    val progress = (identity.progress % 1000) / 1000f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(identity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(if(expanded) Icons.Default.Close else Icons.Default.ThumbUp, null, modifier = Modifier.size(20.dp))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { Text("Kenapa? (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            onVote(identity, 10, note)
                            expanded = false
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Beri Suara (+10)")
                    }
                }
            }
        }
    }
}

// --- LANGKAH 4: JURNAL (Penutup) ---
@Composable
fun RitualStepFour(text: String, onTextChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌙", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Satu Pelajaran",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Apa satu hal yang ingin kamu ingat dari hari ini?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Tulis di sini...") },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            // PERBAIKAN: Menggunakan 28.sp alih-alih constructor salah
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
        )
    }
}