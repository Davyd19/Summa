package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.summa.data.model.Account
import com.app.summa.data.model.TransactionType
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.MoneyViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MoneyViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedAccount by remember { mutableStateOf(uiState.accounts.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    
    // Dropdown for Account
    var accountExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "TRANSAKSI BARU",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .brutalBorder(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onBackground)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                BrutalButton(
                    onClick = {
                        if (selectedAccount != null && amount.isNotBlank()) {
                            viewModel.addTransaction(
                                accountId = selectedAccount!!.id,
                                type = type,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                category = category,
                                note = note
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    text = "SIMPAN TRANSAKSI",
                    containerColor = if (type == TransactionType.EXPENSE) ErrorRed else SuccessGreen
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Type Selector (Segmented Control style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME },
                    label = { 
                        Text(
                            "MASUK", 
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), 
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Black
                        ) 
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreen,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = true, borderColor = SuccessGreen, borderWidth = 2.dp
                    )
                )
                
                FilterChip(
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE },
                    label = { 
                         Text(
                            "KELUAR", 
                            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), 
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Black
                        ) 
                    },
                     modifier = Modifier.weight(1f).height(50.dp),
                     shape = RoundedCornerShape(4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = true, borderColor = ErrorRed, borderWidth = 2.dp
                    )
                )
            }
            
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Amount Input (Big)
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "JUMLAH (RP)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                 OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .brutalBorder(cornerRadius = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = run {
                        val color = if (type == TransactionType.EXPENSE) ErrorRed else SuccessGreen
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = color,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = color
                        )
                    },
                    textStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    singleLine = true,
                    placeholder = { Text("0", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) }
                )
             }
             
             // Account Dropdown
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "AKUN / DOMPET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Pilih Akun",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .brutalBorder(cornerRadius = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = brutalTextFieldColors(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        uiState.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { 
                                     Column {
                                        Text(account.name, fontWeight = FontWeight.Bold)
                                        Text(account.type.name, style = MaterialTheme.typography.labelSmall)
                                     }
                                },
                                onClick = { 
                                    selectedAccount = account
                                    accountExpanded = false 
                                }
                            )
                        }
                    }
                }
            }

            BrutalistTextField(
                value = category,
                onValueChange = { category = it },
                label = "KATEGORI",
                placeholder = "Contoh: Makanan, Transport"
            )

            BrutalistTextField(
                value = note,
                onValueChange = { note = it },
                label = "CATATAN (OPSIONAL)",
                placeholder = "Keterangan tambahan..."
            )
            
            Spacer(modifier = Modifier.height(300.dp)) // Extra space for scrolling
        }
    }
}
