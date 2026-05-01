package com.efina.app.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efina.app.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTransactionScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val moods = listOf("😊", "😐", "😔", "😤", "🥰", "😴", "🤔")
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.value.replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (type) {
                                TransactionType.INCOME -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                TransactionType.EXPENSE -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        )
                    )
                }
            }
            
            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                leadingIcon = { Text("Rp", style = MaterialTheme.typography.bodyLarge) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Account Selector
            OutlinedTextField(
                value = uiState.accounts.find { it.id == selectedAccountId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Account") },
                placeholder = { Text("Select account") },
                trailingIcon = {
                    IconButton(onClick = { showAccountPicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Select")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category Selector
            OutlinedTextField(
                value = uiState.categories.find { it.id == selectedCategoryId }?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                placeholder = { Text("Select category") },
                trailingIcon = {
                    IconButton(onClick = { showCategoryPicker = true }) {
                        Icon(Icons.Default.ArrowDropDown, "Select")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Date Selector
            OutlinedTextField(
                value = selectedDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, "Select date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Mood Selector
            Text(
                text = "How do you feel?",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moods.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = if (selectedMood == mood) null else mood },
                        label = { Text(mood, style = MaterialTheme.typography.titleLarge) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Submit Button
            Button(
                onClick = {
                    val request = CreateTransactionRequest(
                        accountId = selectedAccountId ?: return@Button,
                        categoryId = selectedCategoryId,
                        amount = amount.toDoubleOrNull() ?: return@Button,
                        type = selectedType,
                        description = description.ifBlank { null },
                        date = selectedDate.format(dateFormatter),
                        mood = selectedMood
                    )
                    viewModel.createTransaction(request)
                    onNavigateBack()
                },
                enabled = selectedAccountId != null && amount.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Transaction")
                }
            }
        }
    }
    
    // Account Picker Dialog
    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            title = { Text("Select Account") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(uiState.accounts) { account ->
                        Card(
                            onClick = {
                                selectedAccountId = account.id
                                showAccountPicker = false
                            },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = account.name,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Category Picker Dialog
    if (showCategoryPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryPicker = false },
            title = { Text("Select Category") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(uiState.categories) { category ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(category.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Card(
                            onClick = {
                                selectedCategoryId = category.id
                                showCategoryPicker = false
                            },
                            modifier = Modifier.padding(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = color.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.padding(12.dp),
                                color = color
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 86400000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = LocalDate.ofEpochDay(it / 86400000)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
