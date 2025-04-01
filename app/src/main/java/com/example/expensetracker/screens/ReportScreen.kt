package com.example.expensetracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.components.CategoryBreakdownCard
import com.example.expensetracker.components.ExpenseChart
import com.example.expensetracker.components.StatsSummaryCard
import com.example.expensetracker.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.reportState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // For timeRange dropdown
    var expanded by remember { mutableStateOf(false) }
    val timeRangeOptions = listOf("Today", "This Week", "This Month", "Past 3 Months", "This Year", "Custom Range")

    // For custom date range picker
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Handle error display
    LaunchedEffect(state.error) {
        state.error?.let { errorMessage ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    actionLabel = "Retry"
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(
                            onClick = {
                                viewModel.refreshData()
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        ) {
                            Text("RETRY")
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading report data...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Time range selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state.timeRange,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Time Range") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                timeRangeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            if (option == "Custom Range") {
                                                showDateRangePicker = true
                                            } else {
                                                viewModel.loadReportData(option)
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (state.timeRange == "Custom Range") {
                            IconButton(onClick = { showDateRangePicker = true }) {
                                Icon(
                                    Icons.Outlined.DateRange,
                                    contentDescription = "Select Date Range"
                                )
                            }
                        }
                    }

                    // Display date range if custom range is selected
                    if (state.timeRange == "Custom Range" && state.startDate != null && state.endDate != null) {
                        Text(
                            text = "${dateFormatter.format(state.startDate!!)} - ${dateFormatter.format(state.endDate!!)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats summary card
                    StatsSummaryCard(
                        totalSpent = state.totalSpent,
                        dailyAverage = state.dailyAverage,
                        timeRange = state.timeRange
                    )

                    // Expense chart
                    ExpenseChart(
                        expenseData = state.timeSeriesData,
                        timeRange = state.timeRange
                    )

                    // Category breakdown card
                    if (state.categoryBreakdown.isNotEmpty()) {
                        CategoryBreakdownCard(
                            categoryBreakdown = state.categoryBreakdown,
                            totalSpent = state.totalSpent
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No expenses found for this time period",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateBack
                                ) {
                                    Text("Add an expense")
                                }
                            }
                        }
                    }
                }
            }

            // Date Range Picker Dialog
            if (showDateRangePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDateRangePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                dateRangePickerState.selectedStartDateMillis?.let { startDate ->
                                    dateRangePickerState.selectedEndDateMillis?.let { endDate ->
                                        viewModel.loadCustomDateRangeData(
                                            Date(startDate),
                                            Date(endDate)
                                        )
                                    }
                                }
                                showDateRangePicker = false
                            },
                            enabled = dateRangePickerState.selectedStartDateMillis != null &&
                                    dateRangePickerState.selectedEndDateMillis != null
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDateRangePicker = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        title = { Text("Select Date Range") }
                    )
                }
            }
        }
    }
}