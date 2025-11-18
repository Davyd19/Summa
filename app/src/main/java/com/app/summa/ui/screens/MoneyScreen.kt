package com.app.summa.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Account
import com.app.summa.data.model.AccountType
import com.app.summa.data.model.Transaction
import com.app.summa.data.model.TransactionType
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.MoneyViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Keuangan",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showAddAccountDialog = true }) {
                        Icon(Icons.Default.AddCard, contentDescription = "Tambah Akun")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    NetWorthCard(
                        totalNetWorth = uiState.totalNetWorth,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    Text(
                        "Akun Anda",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(uiState.accounts) { account ->
                            AccountCardItem(account = account)
                        }
                    }
                }

                item {
                    // PERBAIKAN: Tombol aksi dibuat lebih modern
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.ArrowDownward,
                            label = "Masuk",
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f),
                            onClick = { showAddTransactionDialog = true } // TODO: bedakan tipe
                        )
                        ActionButton(
                            icon = Icons.Default.ArrowUpward,
                            label = "Keluar",
                            color = ErrorRed,
                            modifier = Modifier.weight(1f),
                            onClick = { showAddTransactionDialog = true } // TODO: bedakan tipe
                        )
                        ActionButton(
                            icon = Icons.Default.SwapHoriz,
                            label = "Transfer",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { showTransferDialog = true }
                        )
                    }
                }

                item {
                    Text(
                        "Transaksi Terakhir",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // PERBAIKAN: Gunakan Column ber-border, bukan Card per item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.shapes.large
                            )
                    ) {
                        uiState.recentTransactions.forEachIndexed { index, transaction ->
                            TransactionListItem(transaction = transaction)
                            if (index < uiState.recentTransactions.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // (Dialog-dialog tidak berubah, jadi saya biarkan sama)
    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromAccount, toAccount, amount ->
                viewModel.transferMoney(fromAccount, toAccount, amount)
                showTransferDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAdd = { name, type, balance, isInvestment, color ->
                viewModel.addAccount(name, type, balance, isInvestment, color)
                showAddAccountDialog = false
            }
        )
    }

    if (showAddTransactionDialog) {
        AddTransactionDialog(
            accounts = uiState.accounts,
            onDismiss = { showAddTransactionDialog = false },
            onAdd = { account, type, amount, category, note ->
                viewModel.addTransaction(account.id, type, amount, category, note)
                showAddTransactionDialog = false
            }
        )
    }

    AnimatedVisibility(
        visible = uiState.showRewardAnimation,
        enter = scaleIn(animationSpec = tween(500)) + fadeIn(),
        exit = scaleOut(animationSpec = tween(500)) + fadeOut()
    ) {
        InvestmentRewardAnimation(
            onDismiss = { viewModel.dismissRewardAnimation() }
        )
    }
}

// (Helper parseColor & getGradient tidak berubah)
fun parseColor(colorString: String): Color {
    return try {
        Color(colorString.toColorInt())
    } catch (e: Exception) {
        DeepTeal // Fallback color
    }
}
fun getGradientForAccount(account: Account): List<Color> {
    val baseColor = parseColor(account.color)
    return listOf(
        baseColor,
        baseColor.copy(alpha = 0.7f) // Buat gradient sederhana
    )
}

// PERBAIKAN: Desain ulang NetWorthCard
@Composable
fun NetWorthCard(totalNetWorth: Double, modifier: Modifier = Modifier) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // Menggunakan SummaCard sebagai dasar
    SummaCard(
        modifier = modifier.fillMaxWidth(),
        // PERBAIKAN: Gradient yang lebih "branded" (Teal ke Emas)
        backgroundColor = MaterialTheme.colorScheme.primary,
        borderColor = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            // Aksen Emas di sudut
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Total Kekayaan Bersih",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatter.format(totalNetWorth),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "+5.2% bulan lalu", // Contoh
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// PERBAIKAN: Desain ulang AccountCardItem
@Composable
fun AccountCardItem(
    account: Account
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val gradient = remember(account.color) { getGradientForAccount(account) }

    // Menggunakan SummaCard sebagai dasar
    SummaCard(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        borderColor = Color.Transparent, // Hapus border
        padding = 0.dp // Hapus padding default
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = gradient
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    // PERBAIKAN: Ikon "chip" kartu
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = "Chip",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column {
                    Text(
                        formatter.format(account.balance),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// PERBAIKAN: Desain ulang Tombol Aksi
@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Menggunakan FilledTonalButton untuk tampilan yang lebih lembut
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

// PERBAIKAN: Mengganti TransactionItem dengan ListItem
@Composable
fun TransactionListItem(
    transaction: Transaction
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val isExpense = transaction.type == TransactionType.EXPENSE
    val color = if (isExpense) ErrorRed else SuccessGreen

    ListItem(
        headlineContent = {
            Text(
                transaction.note.ifBlank { transaction.category },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                "${transaction.date} • ${transaction.category}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Text(
                formatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}


// (Dialog-dialog tidak berubah)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: AccountType, balance: Double, isInvestment: Boolean, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK) }
    var isInvestment by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf("#4CAF50") } // Default Green

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Akun (cth: Bank Jago)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Saldo Awal (cth: 50000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type.name,
                    onValueChange = { type = AccountType.valueOf(it.uppercase()) },
                    label = { Text("Tipe (BANK, CASH, INVESTMENT)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Warna (cth: #4CAF50)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isInvestment,
                        onCheckedChange = { isInvestment = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ini adalah akun investasi")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, type, balance.toDoubleOrNull() ?: 0.0, isInvestment, color)
                }
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAdd: (Account, TransactionType, Double, String, String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == TransactionType.EXPENSE) "Tambah Pengeluaran" else "Tambah Pemasukan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Keluar") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Masuk") }
                    )
                }
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Pilih Akun",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Akun") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Jumlah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori (cth: Makanan)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAccount != null) {
                        onAdd(selectedAccount!!, type, amount.toDoubleOrNull() ?: 0.0, category, note)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Account, Account, Double) -> Unit
) {
    var fromAccount by remember { mutableStateOf(accounts.firstOrNull { !it.isInvestment } ?: accounts.first()) }
    var toAccount by remember { mutableStateOf(accounts.firstOrNull { it.isInvestment } ?: accounts.last()) }
    var amount by remember { mutableStateOf("") }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Dana") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedFrom,
                    onExpandedChange = { expandedFrom = !expandedFrom }
                ) {
                    OutlinedTextField(
                        value = fromAccount.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dari Akun") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrom) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrom,
                        onDismissRequest = { expandedFrom = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    fromAccount = account
                                    expandedFrom = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = expandedTo,
                    onExpandedChange = { expandedTo = !expandedTo }
                ) {
                    OutlinedTextField(
                        value = toAccount.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ke Akun") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTo,
                        onDismissRequest = { expandedTo = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    toAccount = account
                                    expandedTo = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Jumlah") },
                    placeholder = { Text("Rp 0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(fromAccount, toAccount, amountValue)
                }
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// PERBAIKAN: Desain ulang Animasi Reward
@Composable
fun InvestmentRewardAnimation(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        visible = false
        kotlinx.coroutines.delay(300)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            SummaCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 32.dp,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "reward_transition")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "reward_scale"
                    )

                    Text(
                        "👑",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.scale(scale)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Masa Depan Lebih Cerah!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Investasi Anda bertambah. Selamat atas langkah cerdas ini!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}