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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.expensetracker.mpesa.MPesaTransaction
import com.example.expensetracker.mpesa.SortField
import com.example.expensetracker.mpesa.SortOrder
import com.example.expensetracker.TransactionCard
import com.example.expensetracker.mpesa.TransactionFilter
import com.example.expensetracker.mpesa.TransactionSort
import com.example.expensetracker.mpesa.extractMPesaTransactions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MPesaMessageParserScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

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

            typeMatches && queryMatches
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

    // Check and trigger permission request on screen load
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

                // Filter and sort controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter button
                    Box {
                        Button(
                            onClick = { showFilterOptions = !showFilterOptions },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Filter")
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

                // Transaction count summary
                val filteredTransactions = getFilteredAndSortedTransactions()
                Text(
                    text = "Showing ${filteredTransactions.size} of ${transactions.size} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 8.dp)
                )

                // Debug message display if present
                debugMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp, 4.dp)
                    )
                }

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
                        TransactionCard(transaction)
                    }
                }
            }
        }
    }
}