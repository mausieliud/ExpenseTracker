package com.example.expensetracker.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.BudgetDatabase
import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.DailyAdjustment
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.event.ExpenseFormEvent
import com.example.expensetracker.event.BudgetEvent
import com.example.expensetracker.state.BudgetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BudgetDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()
    private val budgetDao = database.budgetDao()
    private val dailyAdjustmentDao = database.dailyAdjustmentDao()
    private val repository = BudgetDatabase.Repository(database)

    // State flows
    private val _budgetState = MutableStateFlow(BudgetState())
    val budgetState: StateFlow<BudgetState> = _budgetState.asStateFlow()

    // Current date
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currentDate: String = dateFormat.format(Date())

    init {
        loadData()
        loadRolloverSettings()
    }

    private fun loadRolloverSettings() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences(
                "BudgetPrefs", Context.MODE_PRIVATE
            )

            val isEnabled = prefs.getBoolean("AUTOMATIC_ROLLOVER_ENABLED", false)
            val allowOverflow = prefs.getBoolean("ALLOW_OVERFLOW", true)
            val allowUnderflow = prefs.getBoolean("ALLOW_UNDERFLOW", true)
            val maxOverflow = prefs.getFloat("MAX_OVERFLOW_PERCENTAGE", 100f).toDouble()
            val maxUnderflow = prefs.getFloat("MAX_UNDERFLOW_PERCENTAGE", 50f).toDouble()

            _budgetState.update { it.copy(
                isAutomaticRolloverEnabled = isEnabled,
                allowOverflow = allowOverflow,
                allowUnderflow = allowUnderflow,
                maxOverflowPercentage = maxOverflow,
                maxUnderflowPercentage = maxUnderflow
            )}

            // If automatic rollover is enabled, check for day changes
            if (isEnabled) {
                checkForDayEnd()
            }
        }
    }
    private fun loadData() {
        viewModelScope.launch {
            _budgetState.update { it.copy(isLoading = true) }

            // Combine the three flows into one
            combine(
                budgetDao.getBudget(),
                expenseDao.getAllExpenses(),
                dailyAdjustmentDao.getAllAdjustments()
            ) { budget, expenses, adjustments ->
                calculateBudgetState(budget, expenses, adjustments)
            }.collect { newState ->
                _budgetState.update { newState }
            }
        }
    }

    private fun calculateBudgetState(
        budget: Budget?,
        expenses: List<Expense>,
        adjustments: List<DailyAdjustment>
    ): BudgetState {
        val todayExpenses = expenses.filter { it.date == currentDate }
        val totalSpentToday = todayExpenses.sumOf { it.amount }

        val todayAdjustment = adjustments.find { it.date == currentDate }?.adjustment ?: 0.0

        val remainingBudgetForToday = budget?.let {
            // Logic to calculate daily budget including adjustments
            val allocatedDaily = it.allocation_per_day
            allocatedDaily + todayAdjustment - totalSpentToday
        } ?: 0.0

        return BudgetState(
            budget = budget,
            expenses = expenses,
            dailyAdjustments = adjustments,
            isLoading = false,
            remainingBudgetForToday = max(0.0, remainingBudgetForToday),
            totalSpentToday = totalSpentToday
        )
    }

    fun onEvent(event: BudgetEvent) {
        when (event) {
            is BudgetEvent.LoadBudget -> loadData()

            is BudgetEvent.DeleteExpense -> {
                viewModelScope.launch {
                    // Get the expense before deleting it to update the budget
                    val expense = event.expense
                    expenseDao.deleteExpense(expense)

                    // Add the expense amount back to the remaining budget
                    _budgetState.value.budget?.let { budget ->
                        budgetDao.updateBudget(
                            budget.copy(
                                remaining_budget = budget.remaining_budget + expense.amount
                            )
                        )
                    }
                }
            }

            is BudgetEvent.AddDailyAdjustment -> {
                viewModelScope.launch {
                    // Check if there's an existing adjustment for this date
                    val existingAdjustment = dailyAdjustmentDao.getAdjustmentForDate(event.date)
                    val adjustmentDifference = if (existingAdjustment != null) {
                        // If updating an existing adjustment, calculate the difference
                        event.amount - existingAdjustment.adjustment
                    } else {
                        // If it's a new adjustment, use the full amount
                        event.amount
                    }

                    val adjustment = DailyAdjustment(
                        date = event.date,
                        adjustment = event.amount
                    )
                    dailyAdjustmentDao.insertAdjustment(adjustment)

                    // Update remaining budget correctly
                    // For an adjustment, we SUBTRACT from the remaining budget
                    // (positive adjustment means more money for today, less for future days)
                    _budgetState.value.budget?.let { budget ->
                        budgetDao.updateBudget(
                            budget.copy(
                                remaining_budget = budget.remaining_budget - adjustmentDifference
                            )
                        )
                    }
                }
            }

            is BudgetEvent.ResetAllData -> {
                viewModelScope.launch {
                    repository.resetAllData()
                }
            }
            is BudgetEvent.CheckForDayEnd -> {
                checkForDayEnd()
            }

            is BudgetEvent.SetupAutomaticRollover -> {
                setupAutomaticDailyCheck()
            }

            is BudgetEvent.ConfigureRolloverSettings -> {
                saveRolloverSettings(event)
            }

            else -> {} // todo Handling navigation events in the UI layer
        }
    }

    private fun setupAutomaticDailyCheck() {
        viewModelScope.launch {
            getApplication<Application>().getSharedPreferences(
                "BudgetPrefs", Context.MODE_PRIVATE
            ).edit().putBoolean("AUTOMATIC_ROLLOVER_ENABLED", true).apply()

            // Update state immediately
            _budgetState.update { it.copy(isAutomaticRolloverEnabled = true) }

            // Immediately check if we need to process any past days
            checkForDayEnd()
        }
    }

    private fun saveRolloverSettings(event: BudgetEvent.ConfigureRolloverSettings) {
        val prefs = getApplication<Application>().getSharedPreferences(
            "BudgetPrefs", Context.MODE_PRIVATE
        ).edit()

        prefs.putBoolean("AUTOMATIC_ROLLOVER_ENABLED", event.isEnabled)
        prefs.putBoolean("ALLOW_OVERFLOW", event.allowOverflow)
        prefs.putBoolean("ALLOW_UNDERFLOW", event.allowUnderflow)
        prefs.putFloat("MAX_OVERFLOW_PERCENTAGE", event.maxOverflowPercentage.toFloat())
        prefs.putFloat("MAX_UNDERFLOW_PERCENTAGE", event.maxUnderflowPercentage.toFloat())
        prefs.apply()

        // Also update the state immediately
        _budgetState.update { it.copy(
            isAutomaticRolloverEnabled = event.isEnabled,
            allowOverflow = event.allowOverflow,
            allowUnderflow = event.allowUnderflow,
            maxOverflowPercentage = event.maxOverflowPercentage,
            maxUnderflowPercentage = event.maxUnderflowPercentage
        )}

        // If enabled, run the day check
        if (event.isEnabled) {
            checkForDayEnd()
        }
    }

    //Funtions below aAdded for handling overflow and underflow
    private fun handleBudgetOverflowUnderflow() {
        viewModelScope.launch {
            val budget = _budgetState.value.budget ?: return@launch
            val todayExpenses = _budgetState.value.expenses.filter { it.date == currentDate }
            val todayAdjustment = _budgetState.value.dailyAdjustments
                .find { it.date == currentDate }?.adjustment ?: 0.0

            val todayAllocated = budget.allocation_per_day + todayAdjustment
            val todaySpent = todayExpenses.sumOf { it.amount }

            // Calculate overflow/underflow
            val difference = todayAllocated - todaySpent

            if (difference != 0.0) {
                // Get remaining days in budget period
                val remainingDays = calculateRemainingDaysInBudgetPeriod(budget)

                if (remainingDays > 0) {
                    // Calculate adjustment per day
                    val adjustmentPerDay = difference / remainingDays

                    // Create adjustments for future days
                    distributeAdjustment(adjustmentPerDay, remainingDays)

                    // Log the adjustment for tracking
                    println("Budget ${if (difference > 0) "overflow" else "underflow"} of $difference distributed across $remainingDays days at $adjustmentPerDay per day")
                }
            }
        }
    }

    private suspend fun distributeAdjustment(adjustmentPerDay: Double, remainingDays: Int) {
        val prefs = getApplication<Application>().getSharedPreferences(
            "BudgetPrefs", Context.MODE_PRIVATE
        )
        val allowOverflow = prefs.getBoolean("ALLOW_OVERFLOW", true)
        val allowUnderflow = prefs.getBoolean("ALLOW_UNDERFLOW", true)
        val maxOverflowPercentage = prefs.getFloat("MAX_OVERFLOW_PERCENTAGE", 100f).toDouble()
        val maxUnderflowPercentage = prefs.getFloat("MAX_UNDERFLOW_PERCENTAGE", 50f).toDouble()

        // If adjustment is positive (overflow) but overflow not allowed, return
        if (adjustmentPerDay > 0 && !allowOverflow) return

        // If adjustment is negative (underflow) but underflow not allowed, return
        if (adjustmentPerDay < 0 && !allowUnderflow) return

        val budget = _budgetState.value.budget ?: return
        val dailyBudget = budget.allocation_per_day

        // Apply limits based on settings
        val limitedAdjustment = when {
            adjustmentPerDay > 0 -> {
                // Overflow - limit by percentage
                val maxOverflow = dailyBudget * maxOverflowPercentage / 100.0
                minOf(adjustmentPerDay, maxOverflow)
            }
            adjustmentPerDay < 0 -> {
                // Underflow - limit by percentage
                val maxUnderflow = dailyBudget * maxUnderflowPercentage / 100.0
                maxOf(adjustmentPerDay, -maxUnderflow)
            }
            else -> 0.0
        }

        // If after limits, there's no adjustment to make, return
        if (limitedAdjustment == 0.0) return

        // Now distribute the limited adjustment
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Skip today
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)

        // Apply adjustment to each future day
        for (i in 0 until remainingDays) {
            val futureDate = dateFormat.format(calendar.time)

            // Check if adjustment already exists
            val existingAdjustment = dailyAdjustmentDao.getAdjustmentForDate(futureDate)
            val newAdjustment = (existingAdjustment?.adjustment ?: 0.0) + limitedAdjustment

            // Create or update adjustment
            dailyAdjustmentDao.insertAdjustment(
                DailyAdjustment(
                    date = futureDate,
                    adjustment = newAdjustment
                )
            )

            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
    }
    private fun calculateRemainingDaysInBudgetPeriod(budget: Budget): Int {
        // This will depend on your budget period definition
        // For example, if budget is monthly:
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val lastDayOfMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        return lastDayOfMonth - currentDay
    }

    //Checks if the day has ended and triggers overflow/underflow handling
    fun checkForDayEnd() {
        viewModelScope.launch {
            val lastCheckedDatePref = getApplication<Application>().getSharedPreferences(
                "BudgetPrefs", Context.MODE_PRIVATE
            ).getString("LAST_CHECKED_DATE", "") ?: ""

            if (lastCheckedDatePref != currentDate && lastCheckedDatePref.isNotEmpty()) {
                // It's a new day, process the previous day's overflow/underflow
                // We need to use the previous day's data
                val previousDate = lastCheckedDatePref
                processPreviousDayOverflowUnderflow(previousDate)
            }

            // Update the last checked date
            getApplication<Application>().getSharedPreferences(
                "BudgetPrefs", Context.MODE_PRIVATE
            ).edit().putString("LAST_CHECKED_DATE", currentDate).apply()
        }
    }

    private suspend fun processPreviousDayOverflowUnderflow(previousDate: String) {
        val budget = _budgetState.value.budget ?: return
        val previousDayExpenses = _budgetState.value.expenses.filter { it.date == previousDate }
        val previousDayAdjustment = _budgetState.value.dailyAdjustments
            .find { it.date == previousDate }?.adjustment ?: 0.0

        val previousDayAllocated = budget.allocation_per_day + previousDayAdjustment
        val previousDaySpent = previousDayExpenses.sumOf { it.amount }

        // Calculate overflow/underflow
        val difference = previousDayAllocated - previousDaySpent

        if (difference != 0.0) {
            // Get remaining days in budget period including today
            val remainingDays = calculateRemainingDaysInBudgetPeriod(budget)

            if (remainingDays > 0) {
                // Calculate adjustment per day
                val adjustmentPerDay = difference / remainingDays

                // Create adjustments for future days including today
                distributeAdjustment(adjustmentPerDay, remainingDays)

                // Log the adjustment for tracking
                println("Previous day budget ${if (difference > 0) "overflow" else "underflow"} of $difference distributed across $remainingDays days at $adjustmentPerDay per day")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BudgetViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}