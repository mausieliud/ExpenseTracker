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
                    expenseDao.deleteExpense(event.expense)
                }
            }

            is BudgetEvent.AddDailyAdjustment -> {
                viewModelScope.launch {
                    val adjustment = DailyAdjustment(
                        date = event.date,
                        adjustment = event.amount
                    )
                    dailyAdjustmentDao.insertAdjustment(adjustment)

                    // Update remaining budget if needed
                    _budgetState.value.budget?.let { budget ->
                        budgetDao.updateBudget(
                            budget.copy(
                                remaining_budget = budget.remaining_budget - event.amount
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