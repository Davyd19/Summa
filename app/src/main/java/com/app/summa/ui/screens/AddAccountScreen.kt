package com.app.summa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.app.summa.data.model.AccountType
import com.app.summa.ui.components.*
import com.app.summa.ui.theme.*
import com.app.summa.ui.viewmodel.MoneyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    viewModel: MoneyViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CASH) }
    var balance by remember { mutableStateOf("") }
    var isInvestment by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf("#000000") } // String hex for color

    Scaffold(
        topBar = {
            BrutalTopAppBar(
                title = "TAMBAH AKUN",
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
                        if (name.isNotBlank()) {
                            val balanceValue = balance.toDoubleOrNull() ?: 0.0
                            viewModel.addAccount(
                                name = name,
                                type = selectedType,
                                balance = balanceValue,
                                isInvestment = isInvestment,
                                color = selectedColor
                            )
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    text = "SIMPAN AKUN"
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
            
            BrutalistTextField(
                value = name,
                onValueChange = { name = it },
                label = "NAMA AKUN",
                placeholder = "Contoh: BCA Utama, Dompet Saku"
            )

            // Account Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "TIPE AKUN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                // Manual Wrap Layout for Enums
                val types = AccountType.values()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.toList().chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { typeOption ->
                                val isSelected = selectedType == typeOption
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            selectedType = typeOption 
                                            isInvestment = (typeOption == AccountType.INVESTMENT)
                                        }
                                        .brutalBorder(
                                            strokeWidth = if(isSelected) 3.dp else 1.dp,
                                            color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        .background(if(isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        typeOption.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if(isSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                }
                            }
                            // Fill empty space if row is incomplete
                            if (row.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            BrutalistTextField(
                value = balance,
                onValueChange = { if(it.all { char -> char.isDigit() || char == '.' }) balance = it },
                label = "SALDO AWAL",
                placeholder = "0"
            )

            // Investment Toggle (Auto checked if INVESTMENT type selected, but user can override)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isInvestment = !isInvestment },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = isInvestment,
                    onCheckedChange = { isInvestment = it }
                )
                Column {
                    Text("Akun Investasi?", fontWeight = FontWeight.Bold)
                    Text(
                        "Saldo tidak akan dihitung sebagai 'Liquid Cash' tapi masuk Net Worth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
