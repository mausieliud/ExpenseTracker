package com.example.expensetracker.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.expensetracker.SelectableTransactionCard
import com.example.expensetracker.mpesa.MPesaTransaction
import com.example.expensetracker.mpesa.SortField
import com.example.expensetracker.mpesa.SortOrder
import com.example.expensetracker.event.ExpenseFormEvent
import com.example.expensetracker.mpesa.TransactionFilter
import com.example.expensetracker.mpesa.TransactionSort
import com.example.expensetracker.mpesa.extractMPesaTransactions
import com.example.expensetracker.mpesa.toExpense
import com.example.expensetracker.ui.viewmodel.ExpenseFormViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MPesaMessageParserScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExpenseForm: () -> Unit,
    expenseViewModel: ExpenseFormViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check permission status
    var hasReadSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var transactions by remember { mutableStateOf<List<MPesaTransaction>>(emptyList()) }
    var debugMessage by remember { mutableStateOf<String?>(null) }

    // Selected transactions for batch processing
    val selectedTransactions = remember { mutableStateListOf<MPesaTransaction>() }

    // Create permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasReadSmsPermission = isGranted
            if (isGranted) {
                val result = extractMPesaTransactions(context)
                transactions = result.first
                debugMessage = "Found ${result.first.size} transactions. Debug: ${result.second}"
            }
        }
    )

    var filter by remember { mutableStateOf(TransactionFilter()) }
    var sort by remember { mutableStateOf(TransactionSort()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showSortOptions by remember { mutableStateOf(false) }
    var showFilterOptions by remember { mutableStateOf(false) }
    var showDateRangeFilter by remember { mutableStateOf(false) }

    // Date range filter
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var dateRangeText by remember { mutableStateOf("Filter by date range") }

    fun getFilteredAndSortedTransactions(): List<MPesaTransaction> {
        // First filter
        val filtered = transactions.filter { transaction ->
            // Check if transaction type is in selected types
            val typeMatches = filter.types.isEmpty() ||
                    filter.types.contains(transaction.transactionType)

            // Check if any field contains the search query
            val queryMatches = filter.query.isEmpty() ||
                    transaction.transactionId.contains(filter.query, ignoreCase = true) ||
                    transaction.amount.contains(filter.query, ignoreCase = true) ||
                    transaction.senderOrRecipient.contains(filter.query, ignoreCase = true) ||
                    transaction.rawMessage.contains(filter.query, ignoreCase = true)

            // Check date range
            val dateMatches = if (startDate != null && endDate != null) {
                transaction.dateTime >= startDate!!.time && transaction.dateTime <= endDate!!.time
            } else true

            typeMatches && queryMatches && dateMatches
        }

        // Then sort
        return when (sort.field) {
            SortField.DATE -> {
                if (sort.order == SortOrder.ASCENDING) {
                    filtered.sortedBy { it.dateTime }
                } else {
                    filtered.sortedByDescending { it.dateTime }
                }
            }
            SortField.AMOUNT -> {
                if (sort.order == SortOrder.ASCENDING) {
                    filtered.sortedBy {
                        it.amount.replace("KSh ", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                } else {
                    filtered.sortedByDescending {
                        it.amount.replace("KSh ", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                }
            }
            SortField.TYPE -> {
                if (sort.order == SortOrder.ASCENDING) {
                    filtered.sortedBy { it.transactionType }
                } else {
                    filtered.sortedByDescending { it.transactionType }
                }
            }
        }
    }

    // Function to toggle selection of a transaction
    fun toggleTransactionSelection(transaction: MPesaTransaction) {
        if (selectedTransactions.contains(transaction)) {
            selectedTransactions.remove(transaction)
        } else {
            selectedTransactions.add(transaction)
        }
    }

    // Function to add selected transactions as expenses
    fun addSelectedTransactionsAsExpenses() {
        if (selectedTransactions.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No transactions selected")
            }
            return
        }

        // If only one transaction is selected, navigate to expense form
        if (selectedTransactions.size == 1) {
            val transaction = selectedTransactions.first()
            val expense = transaction.toExpense()

            // Prefill the expense form
            expenseViewModel.onEvent(ExpenseFormEvent.DescriptionChanged(expense.description))
            expenseViewModel.onEvent(ExpenseFormEvent.AmountChanged(expense.amount.toString()))
            expenseViewModel.onEvent(ExpenseFormEvent.CategoryChanged(expense.category))
            expenseViewModel.onEvent(ExpenseFormEvent.DateChanged(expense.date))

            // Clear selection
            selectedTransactions.clear()

            // Navigate to expense form for final edits
            onNavigateToExpenseForm()
        } else {
            // For multiple transactions, process them as a batch
            coroutineScope.launch {
                // Process batch using the batch functionality
                val success = expenseViewModel.saveBatchTransactions(selectedTransactions.toList())

                // Show confirmation
                if (success) {
                    snackbarHostState.showSnackbar(
                        "Processing ${selectedTransactions.size} transactions as expenses"
                    )
                } else {
                    snackbarHostState.showSnackbar("Failed to process batch")
                }

                // Clear selection
                selectedTransactions.clear()
            }
        }
    }

    // Function to handle adding a single transaction as an expense
    fun addTransactionAsExpense(transaction: MPesaTransaction) {
        // If a single transaction is added via the "Add" button on the card,
        // navigate to expense form for editing
        val expense = transaction.toExpense()

        // Prefill the expense form
        expenseViewModel.onEvent(ExpenseFormEvent.DescriptionChanged(expense.description))
        expenseViewModel.onEvent(ExpenseFormEvent.AmountChanged(expense.amount.toString()))
        expenseViewModel.onEvent(ExpenseFormEvent.CategoryChanged(expense.category))
        expenseViewModel.onEvent(ExpenseFormEvent.DateChanged(expense.date))

        // Navigate to expense form
        onNavigateToExpenseForm()
    }

    // Function to set date range filter
    fun setLastNDays(days: Int) {
        val calendar = Calendar.getInstance()
        endDate = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, -days)
        startDate = calendar.time

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        dateRangeText = "Last $days days (${dateFormat.format(startDate!!)} - ${dateFormat.format(endDate!!)})"

        showDateRangeFilter = false
    }

    // Check and trigger permission request on screen load
    //redundant...not in MainActivity.Kt but useful as single app.
    LaunchedEffect(key1 = Unit) {
        if (!hasReadSmsPermission) {
            // Explicitly launch permission request
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            val result = extractMPesaTransactions(context)
            transactions = result.first
            debugMessage = "Found ${result.first.size} transactions. Debug: ${result.second}"
        }
    }

    // Track batch processing state
    val isBatchProcessing = expenseViewModel.processingBatch.value
    val batchProgress = expenseViewModel.batchProgress.value
    val batchTotal = expenseViewModel.batchTotal.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M-Pesa Transaction History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTransactions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { addSelectedTransactionsAsExpenses() },
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (selectedTransactions.size == 1)
                            "Edit Selected Transaction" else "Add Selected Transactions"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                // Show batch processing progress if active
                if (isBatchProcessing) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column {
                            Text("Processing transactions: $batchProgress of $batchTotal")
                            // Could add a progress indicator here
                        }
                    }
                } else {
                    Snackbar(it)
                }
            }
        }
    ) { paddingValues ->
        if (!hasReadSmsPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "SMS permission is required to read M-Pesa messages",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Grant SMS Permission", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        } else if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("No M-Pesa messages found")
                    debugMessage?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        filter = filter.copy(query = it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 8.dp),
                    placeholder = { Text("Search transactions...") },
                    singleLine = true
                )

                // Filter and sort controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter button
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showFilterOptions = !showFilterOptions },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Filter Type")
                        }

                        DropdownMenu(
                            expanded = showFilterOptions,
                            onDismissRequest = { showFilterOptions = false }
                        ) {
                            Text(
                                "Transaction Types",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            val allTypes = listOf("SENT", "RECEIVED", "PAID", "WITHDRAW", "DEPOSIT")
                            allTypes.forEach { type ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Toggle the type in the filter
                                            filter = if (filter.types.contains(type)) {
                                                filter.copy(types = filter.types - type)
                                            } else {
                                                filter.copy(types = filter.types + type)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = filter.types.contains(type),
                                        onCheckedChange = null // handled by the row click
                                    )
                                    Text(text = type, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }

                    // Date range filter
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showDateRangeFilter = !showDateRangeFilter },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (startDate != null) Color(0xFF2196F3) else Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date Range"
                            )
                        }

                        DropdownMenu(
                            expanded = showDateRangeFilter,
                            onDismissRequest = { showDateRangeFilter = false }
                        ) {
                            Text(
                                "Filter by Date",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            DropdownMenuItem(
                                text = { Text("Today") },
                                onClick = { setLastNDays(0) }
                            )

                            DropdownMenuItem(
                                text = { Text("Last 7 days") },
                                onClick = { setLastNDays(7) }
                            )

                            DropdownMenuItem(
                                text = { Text("Last 30 days") },
                                onClick = { setLastNDays(30) }
                            )

                            DropdownMenuItem(
                                text = { Text("Last 90 days") },
                                onClick = { setLastNDays(90) }
                            )

                            DropdownMenuItem(
                                text = { Text("All time") },
                                onClick = {
                                    startDate = null
                                    endDate = null
                                    dateRangeText = "Filter by date range"
                                    showDateRangeFilter = false
                                }
                            )
                        }
                    }

                    // Sort controls
                    Box {
                        IconButton(onClick = { showSortOptions = !showSortOptions }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortOptions,
                            onDismissRequest = { showSortOptions = false }
                        ) {
                            Text(
                                "Sort By",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            DropdownMenuItem(
                                text = { Text("Date") },
                                onClick = {
                                    sort = if (sort.field == SortField.DATE) {
                                        sort.copy(order = if (sort.order == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING)
                                    } else {
                                        sort.copy(field = SortField.DATE, order = SortOrder.DESCENDING)
                                    }
                                    showSortOptions = false
                                },
                                trailingIcon = {
                                    if (sort.field == SortField.DATE) {
                                        Icon(
                                            imageVector = if (sort.order == SortOrder.ASCENDING)
                                                Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Sort Direction"
                                        )
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Amount") },
                                onClick = {
                                    sort = if (sort.field == SortField.AMOUNT) {
                                        sort.copy(order = if (sort.order == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING)
                                    } else {
                                        sort.copy(field = SortField.AMOUNT, order = SortOrder.DESCENDING)
                                    }
                                    showSortOptions = false
                                },
                                trailingIcon = {
                                    if (sort.field == SortField.AMOUNT) {
                                        Icon(
                                            imageVector = if (sort.order == SortOrder.ASCENDING)
                                                Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Sort Direction"
                                        )
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Type") },
                                onClick = {
                                    sort = if (sort.field == SortField.TYPE) {
                                        sort.copy(order = if (sort.order == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING)
                                    } else {
                                        sort.copy(field = SortField.TYPE, order = SortOrder.ASCENDING)
                                    }
                                    showSortOptions = false
                                },
                                trailingIcon = {
                                    if (sort.field == SortField.TYPE) {
                                        Icon(
                                            imageVector = if (sort.order == SortOrder.ASCENDING)
                                                Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Sort Direction"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Active filters display
                if (startDate != null || filter.types.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active filters: ",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (startDate != null) {
                            Text(
                                text = dateRangeText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (filter.types.isNotEmpty()) {
                            Text(
                                text = if (startDate != null) " | " else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Types: ${filter.types.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Selection summary with different message for single vs multiple selections
                if (selectedTransactions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTransactions.size == 1)
                                    "1 transaction selected (will open in form)"
                                else
                                    "${selectedTransactions.size} transactions selected (will add as batch)",
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { addSelectedTransactionsAsExpenses() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text(if (selectedTransactions.size == 1) "Edit" else "Add All")
                            }
                        }
                    }
                }

                // Transaction count summary
                val filteredTransactions = getFilteredAndSortedTransactions()
                Text(
                    text = "Showing ${filteredTransactions.size} of ${transactions.size} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // LazyColumn for transactions
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        SelectableTransactionCard(
                            transaction = transaction,
                            isSelected = selectedTransactions.contains(transaction),
                            onSelect = { toggleTransactionSelection(transaction) },
                            onAddAsExpense = { addTransactionAsExpense(it) }
                        )
                    }
                }
            }
        }
    }
}
