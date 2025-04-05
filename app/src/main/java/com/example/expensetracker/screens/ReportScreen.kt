package com.example.expensetracker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.components.CategoryBreakdownCard
import com.example.expensetracker.components.CategoryChip
import com.example.expensetracker.components.CategoryDonutChart
import com.example.expensetracker.components.CategoryLegend
import com.example.expensetracker.components.DailySpendingCard
import com.example.expensetracker.components.ExpenseChart
import com.example.expensetracker.components.MinMaxSpentCard
import com.example.expensetracker.components.getCategoryColor
import com.example.expensetracker.roundToDecimalPlaces
import com.example.expensetracker.ui.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import com.example.expensetracker.calculateTrend
import com.example.expensetracker.calculateWeeklyAverages
import com.example.expensetracker.convertToDatePairs
import com.example.expensetracker.data.entity.Expense
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId

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

                    //Spennding summary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Spending Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Spent:")
                                Text(
                                    text = "Ksh.${state.totalSpent.roundToDecimalPlaces(2)}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Daily Average:")
                                Text(
                                    text = "Ksh.${state.dailyAverage.roundToDecimalPlaces(2)}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Budget Utilization:")
                                val budgetAmount = state.budget?.total_budget ?: 0.0
                                val utilization = if (budgetAmount > 0) (state.totalSpent / budgetAmount * 100) else 0.0
                                Text(
                                    text = "${utilization.roundToDecimalPlaces(1)}%",
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        utilization > 90 -> MaterialTheme.colorScheme.error
                                        utilization > 75 -> Color(0xFFFF9800) // Orange
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }

                            // Find the biggest expense if available
                            val biggestExpense = state.expenses.maxByOrNull { it.amount }
                            if (biggestExpense != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Largest Expense:")
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Ksh.${biggestExpense.amount.roundToDecimalPlaces(2)}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = biggestExpense.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            // Find most frequent category and create a dummy Expense to use with CategoryChip
                            val categoryFrequencies = state.expenses.groupBy { it.category }
                            val mostFrequentCategory = categoryFrequencies.maxByOrNull { it.value.size }

                            if (mostFrequentCategory != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Most Frequent Category:")
                                    // Use the first expense from the most frequent category to display the chip
                                    CategoryChip(expense = mostFrequentCategory.value.first())
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Min Spent Card
                        MinMaxSpentCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(IntrinsicSize.Min),
                            isMin = true,
                            expenses = state.expenses

                        )

                        // Max Spent Card
                        MinMaxSpentCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(IntrinsicSize.Min),
                            isMin = false,
                            expenses = state.expenses

                        )
                    }

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
                    DailySpendingCard(
                        dailyTotals = state.dailySpending,
                        dailyBudget = state.budget?.allocation_per_day ?: 0.0
                    )



                    // Category Visualization Card
                    if (state.categoryBreakdown.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Category Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Convert Map to List of Pairs for easier processing
                                val categoryTotals = state.categoryBreakdown.toList()

                                if (categoryTotals.isEmpty()) {
                                    Text(
                                        text = "No expense data available",
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    // Chart and Legend in a Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Add the Donut Chart
                                        Box(
                                            modifier = Modifier
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CategoryDonutChart(
                                                categoryBreakdown = state.categoryBreakdown,
                                                totalSpent = state.totalSpent
                                            )
                                        }

                                        // Add the Legend
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                        ) {
                                            CategoryLegend(
                                                categories = categoryTotals.map { it.first },
                                                getCategoryColor = ::getCategoryColor
                                            )
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                                    // Calculate percentages for each category
                                    val totalExpenses = state.totalSpent

                                    categoryTotals.forEach { (category, total) ->
                                        val percentage = if (totalExpenses > 0) (total / totalExpenses * 100) else 0.0

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Creating a dummy expense for the category chip
                                                val dummyExpense = Expense(
                                                    id = 0,  // Use a dummy ID
                                                    amount = 0.0,  // Use a dummy amount
                                                    description = "",  // Empty description
                                                    date = "",  // Empty date
                                                    category = category  // Use the current category
                                                )
                                                CategoryChip(expense = dummyExpense)

                                                Row {
                                                    Text(
                                                        text = "Ksh.${total.roundToDecimalPlaces(2)}",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = " (${String.format("%.1f", percentage)}%)",
                                                        color = Color.Gray,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }

                                            // Category progress bar
                                            LinearProgressIndicator(
                                                progress = (percentage / 100).toFloat(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .padding(top = 4.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = getCategoryColor(category)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //Top expenses
                    // Top Expenses List
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Top Expenses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (state.expenses.isEmpty()) {
                                Text(
                                    text = "No expenses to display",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                state.expenses.sortedByDescending { it.amount }.take(10).forEach { expense ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = expense.description,
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CategoryChip(expense = expense)
                                                Text(
                                                    text = expense.date,
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Ksh.${expense.amount.roundToDecimalPlaces(2)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    //trends
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Spending Trends",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val dailyTotals = convertToDatePairs(state.dailySpending)

                            if (state.expenses.isEmpty() || dailyTotals.size < 2) {
                                Text(
                                    text = "Not enough data to analyze trends",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                // Calculate trend (increasing/decreasing)
                                val trend = calculateTrend(dailyTotals)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when {
                                            trend > 0 -> Icons.Default.TrendingUp
                                            trend < 0 -> Icons.Default.TrendingDown
                                            else -> Icons.Default.TrendingFlat
                                        },
                                        contentDescription = "Spending Trend",
                                        tint = when {
                                            trend > 0 -> MaterialTheme.colorScheme.error
                                            trend < 0 -> Color.Green
                                            else -> Color.Gray
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = when {
                                            trend > 0.1 -> "Your spending is increasing"
                                            trend < -0.1 -> "Your spending is decreasing"
                                            else -> "Your spending is stable"
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Weekly Averages Section
                                if (state.expenses.size >= 7) {
                                    val weeklyAverages = calculateWeeklyAverages(state.expenses)

                                    Text(
                                        text = "Weekly Averages",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )

                                    weeklyAverages.forEach { (weekStart, average) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val formatter = DateTimeFormatter.ofPattern("MMM dd")
                                            Text(
                                                text = "${weekStart.format(formatter)} - ${weekStart.plusDays(6).format(formatter)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Ksh.${average.roundToDecimalPlaces(2)}",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
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