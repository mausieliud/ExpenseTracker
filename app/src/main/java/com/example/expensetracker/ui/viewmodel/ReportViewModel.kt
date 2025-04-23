package com.example.expensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.BudgetDatabase
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.model.TimeSeriesDataPoint
import com.example.expensetracker.helpers.roundToDecimalPlaces
import com.example.expensetracker.state.ReportState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val database = BudgetDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()

    private val _reportState = MutableStateFlow(ReportState())
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val budgetDao = database.budgetDao()
    init {
        loadReportData("This Month")
        loadBudgetData()
    }

    fun loadReportData(timeRange: String) {
        viewModelScope.launch {
            _reportState.update { it.copy(isLoading = true, timeRange = timeRange, error = null) }

            try {
                // Get date range based on selected time period
                val (startDate, endDate) = getDateRange(timeRange)

                // Clear custom date range if not using custom range
                if (timeRange != "Custom Range") {
                    _reportState.update { it.copy(startDate = null, endDate = null) }
                }

                // Get expenses within the date range
                expenseDao.getAllExpenses().collect { allExpenses ->
                    val filteredExpenses = filterExpensesByDateRange(allExpenses, startDate, endDate)
                    processExpenses(filteredExpenses, startDate, endDate, timeRange)
                }
            } catch (e: Exception) {
                _reportState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading report data: ${e.message}"
                    )
                }
            }
        }
    }

    // Add this function
    private fun loadBudgetData() {
        viewModelScope.launch {
            try {
                budgetDao.getBudget().collect { budget ->
                    _reportState.update { it.copy(budget = budget) }
                }
            } catch (e: Exception) {
                _reportState.update {
                    it.copy(
                        error = "Error loading budget data: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadCustomDateRangeData(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _reportState.update {
                it.copy(
                    isLoading = true,
                    timeRange = "Custom Range",
                    startDate = startDate,
                    endDate = endDate,
                    error = null
                )
            }

            try {
                val formattedStartDate = dateFormat.format(startDate)
                val formattedEndDate = dateFormat.format(endDate)

                expenseDao.getAllExpenses().collect { allExpenses ->
                    val filteredExpenses = filterExpensesByDateRange(
                        allExpenses,
                        formattedStartDate,
                        formattedEndDate
                    )
                    processExpenses(
                        filteredExpenses,
                        formattedStartDate,
                        formattedEndDate,
                        "Custom Range"
                    )
                }
            } catch (e: Exception) {
                _reportState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error processing custom date range: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshData() {
        val currentTimeRange = _reportState.value.timeRange
        if (currentTimeRange == "Custom Range" && _reportState.value.startDate != null && _reportState.value.endDate != null) {
            loadCustomDateRangeData(_reportState.value.startDate!!, _reportState.value.endDate!!)
        } else {
            loadReportData(currentTimeRange)
        }
    }

    private fun processExpenses(
        expenses: List<Expense>,
        startDate: String,
        endDate: String,
        timeRange: String
    ) {
        if (expenses.isEmpty()) {
            _reportState.update {
                it.copy(
                    expenses = emptyList(),
                    categoryBreakdown = emptyMap(),
                    totalSpent = 0.0,
                    dailySpending = emptyMap(),
                    dailyAverage = 0.0,
                    timeSeriesData = emptyList(),
                    isLoading = false
                )
            }
            return
        }

        // Calculate total spent
        val totalSpent = expenses.sumOf { it.amount }

        // Calculate category breakdown
        val categoryBreakdown = expenses
            .groupBy { it.category }
            .mapValues { (_, expensesInCategory) ->
                expensesInCategory.sumOf { it.amount }
            }
            .toList()
            .sortedByDescending { (_, amount) -> amount }
            .toMap()

        // Calculate daily average
        val dailySpending = calculateDailySpending(expenses)
        val startDateObj = dateFormat.parse(startDate)
        val endDateObj = dateFormat.parse(endDate)
        val daysInRange = if (startDateObj != null && endDateObj != null) {
            val diffInMillis = endDateObj.time - startDateObj.time
            TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1
        } else {
            1
        }
        val dailyAverage = totalSpent / daysInRange.toDouble()

        // Generate time series data based on the time range
        val timeSeriesData = generateTimeSeriesData(expenses, startDate, endDate, timeRange)

        _reportState.update {
            it.copy(
                expenses = expenses,
                categoryBreakdown = categoryBreakdown,
                dailySpending = dailySpending,
                totalSpent = totalSpent,
                dailyAverage = dailyAverage,
                timeSeriesData = timeSeriesData,
                isLoading = false
            )
        }
    }

    private fun generateTimeSeriesData(
        expenses: List<Expense>,
        startDate: String,
        endDate: String,
        timeRange: String
    ): List<TimeSeriesDataPoint> {
        // Parse the start and end dates
        val startDateObj = dateFormat.parse(startDate) ?: return emptyList()
        val endDateObj = dateFormat.parse(endDate) ?: return emptyList()

        // Create calendar instances for iteration
        val startCalendar = Calendar.getInstance().apply { time = startDateObj }
        val endCalendar = Calendar.getInstance().apply { time = endDateObj }

        // Determine the appropriate grouping based on time range
        val (groupBy, calendarField, format) = when (timeRange) {
            "Today" -> Triple("Hour", Calendar.HOUR_OF_DAY, SimpleDateFormat("HH:mm", Locale.getDefault()))
            "This Week" -> Triple("Day", Calendar.DAY_OF_WEEK, displayDateFormat)
            "This Month" -> Triple("Day", Calendar.DAY_OF_MONTH, displayDateFormat)
            "Past 3 Months" -> Triple("Week", Calendar.WEEK_OF_YEAR, displayDateFormat)
            "This Year" -> Triple("Month", Calendar.MONTH, monthYearFormat)
            "Custom Range" -> {
                val diffInDays = TimeUnit.MILLISECONDS.toDays(endDateObj.time - startDateObj.time)
                when {
                    diffInDays <= 1 -> Triple("Hour", Calendar.HOUR_OF_DAY, SimpleDateFormat("HH:mm", Locale.getDefault()))
                    diffInDays <= 31 -> Triple("Day", Calendar.DAY_OF_MONTH, displayDateFormat)
                    diffInDays <= 90 -> Triple("Week", Calendar.WEEK_OF_YEAR, displayDateFormat)
                    else -> Triple("Month", Calendar.MONTH, monthYearFormat)
                }
            }
            else -> Triple("Day", Calendar.DAY_OF_MONTH, displayDateFormat)
        }

        // Map to hold aggregated expenses by time period
        val timeSeriesMap = mutableMapOf<String, Double>()

        // Generate all date labels first (including zeros)
        val current = Calendar.getInstance().apply { time = startDateObj }
        while (!current.after(endCalendar)) {
            val label = format.format(current.time)
            timeSeriesMap[label] = 0.0

            when (groupBy) {
                "Hour" -> current.add(Calendar.HOUR_OF_DAY, 1)
                "Day" -> current.add(Calendar.DAY_OF_MONTH, 1)
                "Week" -> current.add(Calendar.WEEK_OF_YEAR, 1)
                "Month" -> current.add(Calendar.MONTH, 1)
            }
        }

        // Now aggregate expenses into the appropriate time buckets
        for (expense in expenses) {
            val expenseDate = dateFormat.parse(expense.date) ?: continue
            val label = format.format(expenseDate)

            timeSeriesMap[label] = (timeSeriesMap[label] ?: 0.0) + expense.amount
        }

        // Convert to list of data points
        return timeSeriesMap.entries
            .map { (label, amount) -> TimeSeriesDataPoint(label, amount) }
            .sortedBy {
                // Sort based on the actual date, not just the label
                when (groupBy) {
                    "Hour" -> SimpleDateFormat("HH:mm", Locale.getDefault()).parse(it.label)?.time ?: 0
                    "Day" -> {
                        try {
                            displayDateFormat.parse(it.label)?.time ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    }
                    "Month" -> {
                        try {
                            monthYearFormat.parse(it.label)?.time ?: 0
                        } catch (e: Exception) {
                            0
                        }
                    }
                    else -> 0
                }
            }
    }

    private fun getDateRange(timeRange: String): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)

        when (timeRange) {
            "Today" -> return Pair(endDate, endDate)
            "This Week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "This Month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "Past 3 Months" -> {
                calendar.add(Calendar.MONTH, -3)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "This Year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            "Custom Range" -> {
                // If we already have a custom range, use it
                val customStartDate = _reportState.value.startDate
                val customEndDate = _reportState.value.endDate

                if (customStartDate != null && customEndDate != null) {
                    return Pair(
                        dateFormat.format(customStartDate),
                        dateFormat.format(customEndDate)
                    )
                }

                // Default to last 30 days if no custom range is set
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
            else -> {
                calendar.add(Calendar.MONTH, -1)
                val startDate = dateFormat.format(calendar.time)
                return Pair(startDate, endDate)
            }
        }
    }

    private fun filterExpensesByDateRange(
        expenses: List<Expense>,
        startDate: String,
        endDate: String
    ): List<Expense> {
        val startDateObj = dateFormat.parse(startDate)
        val endDateObj = dateFormat.parse(endDate)

        return expenses.filter { expense ->
            val expenseDate = dateFormat.parse(expense.date)
            expenseDate != null &&
                    expenseDate.compareTo(startDateObj) >= 0 &&
                    expenseDate.compareTo(endDateObj) <= 0
        }
    }
    // for daily spending card(Calculates daily spending"
    private fun calculateDailySpending(expenses: List<Expense>): Map<String, Double> {
        return expenses
            .groupBy { it.date }
            .mapValues { (_, expensesOnDay) -> expensesOnDay.sumOf { it.amount } }
            .toSortedMap(compareByDescending { it })  // Sort by date, most recent first
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReportViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }

    }

    /**
     * Generates a text summary report of the current expense data
     */
    fun generateTextReport(): String {
        val state = _reportState.value
        val sb = StringBuilder()

        // Report header
        sb.appendLine("EXPENSE REPORT SUMMARY")
        sb.appendLine("Time Period: ${state.timeRange}")
        if (state.timeRange == "Custom Range" && state.startDate != null && state.endDate != null) {
            sb.appendLine("Date Range: ${dateFormat.format(state.startDate)} to ${dateFormat.format(state.endDate)}")
        }
        sb.appendLine("")

        // Spending summary
        sb.appendLine("SPENDING SUMMARY")
        sb.appendLine("Total Spent: Ksh.${state.totalSpent.roundToDecimalPlaces(2)}")
        sb.appendLine("Daily Average: Ksh.${state.dailyAverage.roundToDecimalPlaces(2)}")

        // Budget info if available
        state.budget?.let { budget ->
            val utilization = if (budget.total_budget > 0) (state.totalSpent / budget.total_budget * 100) else 0.0
            sb.appendLine("Budget: Ksh.${budget.total_budget}")
            sb.appendLine("Budget Utilization: ${utilization.roundToDecimalPlaces(1)}%")
        }
        sb.appendLine("")

        // Category breakdown
        sb.appendLine("CATEGORY BREAKDOWN")
        if (state.categoryBreakdown.isEmpty()) {
            sb.appendLine("No expense data available")
        } else {
            state.categoryBreakdown.forEach { (category, amount) ->
                val percentage = if (state.totalSpent > 0) (amount / state.totalSpent * 100) else 0.0
                sb.appendLine("$category: Ksh.${amount.roundToDecimalPlaces(2)} (${percentage.roundToDecimalPlaces(1)}%)")
            }
        }
        sb.appendLine("")

        // Top expenses
        sb.appendLine("TOP EXPENSES")
        if (state.expenses.isEmpty()) {
            sb.appendLine("No expenses to display")
        } else {
            state.expenses.sortedByDescending { it.amount }.take(10).forEach { expense ->
                sb.appendLine("${expense.description} (${expense.category}) - Ksh.${expense.amount.roundToDecimalPlaces(2)} on ${expense.date}")
            }
        }

        // Daily spending
        sb.appendLine("")
        sb.appendLine("DAILY SPENDING")
        if (state.dailySpending.isEmpty()) {
            sb.appendLine("No daily spending data available")
        } else {
            state.dailySpending.entries.take(10).forEach { (date, amount) ->
                sb.appendLine("$date: Ksh.${amount.roundToDecimalPlaces(2)}")
            }
        }

        return sb.toString()
    }

    /**
     * Generates a CSV formatted report of expenses
     */
    fun generateCSVReport(): String {
        val state = _reportState.value
        val sb = StringBuilder()

        // CSV header
        sb.appendLine("Date,Amount,Category,Description")

        // Expense data
        state.expenses.forEach { expense ->
            // Escape description in case it contains commas
            val safeDescription = "\"${expense.description.replace("\"", "\"\"")}\""
            sb.appendLine("${expense.date},${expense.amount},${expense.category},$safeDescription")
        }

        return sb.toString()
    }
}