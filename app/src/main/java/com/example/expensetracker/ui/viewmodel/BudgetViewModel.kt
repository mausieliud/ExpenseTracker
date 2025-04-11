package com.example.expensetracker.ui.viewmodel

import android.app.Application
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

        // Calculate remaining budget for today based on days left in budget period
        val remainingBudgetForToday = budget?.let {
            try {
                // Parse dates to calculate remaining days
                val startDateObj = dateFormat.parse(it.start_date)
                val endDateObj = dateFormat.parse(it.end_date)
                val todayDateObj = dateFormat.parse(currentDate)

                if (startDateObj != null && endDateObj != null && todayDateObj != null) {
                    // If today is before budget start date, return 0
                    if (todayDateObj.before(startDateObj)) {
                        return@let 0.0
                    }

                    // If today is after budget end date, return 0
                    if (todayDateObj.after(endDateObj)) {
                        return@let 0.0
                    }

                    // Calculate remaining days including today
                    val daysUntilEnd = ((endDateObj.time - todayDateObj.time) / (1000 * 60 * 60 * 24)).toInt() + 1

                    // Calculate remaining budget per day based on remaining days
                    val remainingDailyBudget = if (daysUntilEnd > 0) {
                        it.remaining_budget / daysUntilEnd
                    } else {
                        0.0
                    }

                    // Add today's adjustment and subtract today's expenses
                    remainingDailyBudget + todayAdjustment - totalSpentToday
                } else {
                    it.allocation_per_day + todayAdjustment - totalSpentToday
                }
            } catch (e: Exception) {
                // Fallback to simple calculation if date parsing fails
                it.allocation_per_day + todayAdjustment - totalSpentToday
            }
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

            else -> {} // Handle navigation events in the UI layer
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