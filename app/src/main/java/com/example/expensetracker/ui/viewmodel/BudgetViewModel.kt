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
import com.example.expensetracker.event.RolloverOption
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
            val optionOrdinal = prefs.getInt("ROLLOVER_OPTION", RolloverOption.REALLOCATE.ordinal)
            val option = RolloverOption.values().getOrNull(optionOrdinal) ?: RolloverOption.REALLOCATE

            _budgetState.update { it.copy(
                isAutomaticRolloverEnabled = isEnabled,
                rolloverOption = option
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


            is BudgetEvent.SetAutomaticRollover -> {
                saveRolloverSettings(event.isEnabled, event.option)
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


    private fun saveRolloverSettings(isEnabled: Boolean, option: RolloverOption) {
        val prefs = getApplication<Application>().getSharedPreferences(
            "BudgetPrefs", Context.MODE_PRIVATE
        ).edit()

        prefs.putBoolean("AUTOMATIC_ROLLOVER_ENABLED", isEnabled)
        prefs.putInt("ROLLOVER_OPTION", option.ordinal)
        prefs.apply()

        // Also update the state immediately
        _budgetState.update { it.copy(
            isAutomaticRolloverEnabled = isEnabled,
            rolloverOption = option
        )}

        // If enabled, run the day check
        if (isEnabled) {
            checkForDayEnd()
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


    private suspend fun processPreviousDayOverflowUnderflow(previousDate: String) {
        val budget = _budgetState.value.budget ?: return
        val previousDayExpenses = _budgetState.value.expenses.filter { it.date == previousDate }
        val previousDayAdjustment = _budgetState.value.dailyAdjustments
            .find { it.date == previousDate }?.adjustment ?: 0.0

        val previousDayAllocated = budget.allocation_per_day + previousDayAdjustment
        val previousDaySpent = previousDayExpenses.sumOf { it.amount }

        // Calculate unspent amount (can be positive or negative)
        val unspentAmount = previousDayAllocated - previousDaySpent

        // Skip if there's nothing to process
        if (unspentAmount == 0.0) return

        // Process according to selected option
        when (_budgetState.value.rolloverOption) {
            RolloverOption.REALLOCATE -> reallocateToRemainingDays(unspentAmount)
            RolloverOption.SAVE -> addToSavings(unspentAmount)
            RolloverOption.ADD_TO_TOMORROW -> addToTomorrow(unspentAmount)
            else -> {} // No action for NONE
        }

        // Update state with the last rollover info for UI feedback
        _budgetState.update { it.copy(
            lastRolloverAmount = unspentAmount,
            lastRolloverDate = previousDate
        )}
    }
    private suspend fun reallocateToRemainingDays(amount: Double) {
        val budget = _budgetState.value.budget ?: return
        val remainingDays = calculateRemainingDaysInBudgetPeriod(budget)

        if (remainingDays <= 0) {
            // If no days left, treat as savings
            addToSavings(amount)
            return
        }

        // Calculate adjustment per day
        val adjustmentPerDay = amount / remainingDays

        // Apply to all remaining days including today
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Start from today
        val startDate = dateFormat.format(calendar.time)

        for (i in 0 until remainingDays) {
            val dateToAdjust = if (i == 0) startDate else {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                dateFormat.format(calendar.time)
            }

            // Check if adjustment already exists for this date
            val existingAdjustment = dailyAdjustmentDao.getAdjustmentForDate(dateToAdjust)
            val newAdjustment = (existingAdjustment?.adjustment ?: 0.0) + adjustmentPerDay

            // Save the adjustment
            dailyAdjustmentDao.insertAdjustment(
                DailyAdjustment(
                    date = dateToAdjust,
                    adjustment = newAdjustment
                )
            )
        }

        // Log the action
        android.util.Log.d("BudgetRollover", "Reallocated $amount across $remainingDays days at $adjustmentPerDay per day")
    }

    private suspend fun addToSavings(amount: Double) {
        // Add to overall remaining budget without affecting daily allocations
        val budget = _budgetState.value.budget ?: return

        // Update the budget's remaining amount
        budgetDao.updateBudget(
            budget.copy(
                remaining_budget = budget.remaining_budget + amount
            )
        )

        // Log the action
        android.util.Log.d("BudgetRollover", "Added $amount to savings")
    }

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

    private suspend fun addToTomorrow(amount: Double) {
        // Add the entire amount to tomorrow's budget only
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Get tomorrow's date
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val tomorrow = dateFormat.format(calendar.time)

        // Check if adjustment already exists
        val existingAdjustment = dailyAdjustmentDao.getAdjustmentForDate(tomorrow)
        val newAdjustment = (existingAdjustment?.adjustment ?: 0.0) + amount

        // Save the adjustment
        dailyAdjustmentDao.insertAdjustment(
            DailyAdjustment(
                date = tomorrow,
                adjustment = newAdjustment
            )
        )

        // Log the action
        android.util.Log.d("BudgetRollover", "Added $amount to tomorrow's budget")
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