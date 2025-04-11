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
    private var underflowHandledForToday = false

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

        // Calculate remaining budget for today and check for underflow
        var remainingBudgetForToday = 0.0
        var hasUnderflow = false
        var underflowAmount = 0.0

        budget?.let {
            try {
                // Parse dates to calculate remaining days
                val startDateObj = dateFormat.parse(it.start_date)
                val endDateObj = dateFormat.parse(it.end_date)
                val todayDateObj = dateFormat.parse(currentDate)

                if (startDateObj != null && endDateObj != null && todayDateObj != null) {
                    // If today is before budget start date or after budget end date, return 0
                    if (todayDateObj.before(startDateObj) || todayDateObj.after(endDateObj)) {
                        remainingBudgetForToday = 0.0
                    } else {
                        // Calculate remaining days including today
                        val daysUntilEnd = ((endDateObj.time - todayDateObj.time) / (1000 * 60 * 60 * 24)).toInt() + 1

                        // Calculate daily budget allocation
                        val dailyBudget = if (daysUntilEnd > 0) {
                            it.remaining_budget / daysUntilEnd
                        } else {
                            it.allocation_per_day
                        }

                        // Calculate today's total allocation (daily budget + any adjustments)
                        val todayAllocation = dailyBudget + todayAdjustment

                        // Calculate remaining amount after today's expenses
                        remainingBudgetForToday = todayAllocation - totalSpentToday

                        // Check for underflow (unused daily budget)
                        if (remainingBudgetForToday > 0 && endDateObj.after(todayDateObj) && !underflowHandledForToday) {
                            hasUnderflow = true
                            underflowAmount = remainingBudgetForToday
                        }
                    }
                } else {
                    remainingBudgetForToday = it.allocation_per_day + todayAdjustment - totalSpentToday
                }
            } catch (e: Exception) {
                // Fallback to simple calculation if date parsing fails
                remainingBudgetForToday = it.allocation_per_day + todayAdjustment - totalSpentToday
            }
        }

        // Calculate days left in the budget period
        val daysLeft = budget?.let {
            try {
                val endDateObj = dateFormat.parse(it.end_date)
                val todayDateObj = dateFormat.parse(currentDate)

                if (endDateObj != null && todayDateObj != null) {
                    max(0, ((endDateObj.time - todayDateObj.time) / (1000 * 60 * 60 * 24)).toInt())
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        } ?: 0

        // Calculate daily budget rate based on remaining budget and days
        val dailyBudgetRate = if (daysLeft > 0 && budget != null) {
            budget.remaining_budget / daysLeft
        } else {
            0.0
        }

        return BudgetState(
            budget = budget,
            expenses = expenses,
            dailyAdjustments = adjustments,
            isLoading = false,
            remainingBudgetForToday = max(0.0, remainingBudgetForToday),
            totalSpentToday = totalSpentToday,
            daysLeft = daysLeft,
            dailyBudgetRate = dailyBudgetRate,
            hasUnderflow = hasUnderflow,
            underflowAmount = underflowAmount
        )

        //For overflow underflow handling..after presentations modify underflow detection logic to
        //// In calculateBudgetState function, update the hasUnderflow check:
        //// Check for underflow (unused daily budget) - add check for the current time of day
        //val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        //val isEndOfDay = currentHour >= 20 // end of day after 8 PM
        //
        //if (remainingBudgetForToday > 0 && endDateObj.after(todayDateObj) && isEndOfDay) {
        //    hasUnderflow = true
        //    underflowAmount = remainingBudgetForToday
        //}
        //So that it appears after 8pm most probable time it would be useful for user.
    }

    fun onEvent(event: BudgetEvent) {
        when (event) {
            is BudgetEvent.LoadBudget -> loadData()

            is BudgetEvent.DeleteExpense -> {
                viewModelScope.launch {
                    val expense = event.expense
                    expenseDao.deleteExpense(expense)

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
                    val existingAdjustment = dailyAdjustmentDao.getAdjustmentForDate(event.date)
                    val adjustmentDifference = if (existingAdjustment != null) {
                        event.amount - existingAdjustment.adjustment
                    } else {
                        event.amount
                    }

                    val adjustment = DailyAdjustment(
                        date = event.date,
                        adjustment = event.amount
                    )
                    dailyAdjustmentDao.insertAdjustment(adjustment)

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

            // New event handlers for underflow
            is BudgetEvent.SaveUnderflowToSavings -> {
                viewModelScope.launch {
                    val underflowAmount = _budgetState.value.underflowAmount
                    _budgetState.value.budget?.let { budget ->
                        // Update the budget by reducing remaining budget and increasing savings
                        budgetDao.updateBudget(
                            budget.copy(
                                remaining_budget = budget.remaining_budget - underflowAmount,
                                savings = budget.savings + underflowAmount
                            )
                        )

                        // Add a negative adjustment for today to mark the underflow as used
                        val adjustment = DailyAdjustment(
                            date = currentDate,
                            adjustment = -underflowAmount
                        )
                        dailyAdjustmentDao.insertAdjustment(adjustment)

                        // Reload data to update UI
                        loadData()
                    }
                }
            }

            is BudgetEvent.RolloverUnderflow -> {
                // Todo Do nothing - the default behavior is to rollover by letting
                // the remaining amount be part of the remaining budget
                // Just add a record of the rollover for tracking purposes
                viewModelScope.launch {
                    val adjustment = DailyAdjustment(
                        date = currentDate,
                        adjustment = -_budgetState.value.underflowAmount
                    )
                    dailyAdjustmentDao.insertAdjustment(adjustment)

                    // Reload data to update UI (remove underflow indicator)
                    loadData()
                }
            }

            is BudgetEvent.IgnoreUnderflow -> {
                underflowHandledForToday = true
                //update state to mark underflow as handled so the prompt disappears
                viewModelScope.launch {
                    _budgetState.update { it.copy(
                        hasUnderflow = false,
                        underflowAmount = 0.0
                    )}
                }
            }

            else -> {} // Handle navigation events in the UI layer
        }
    }

    fun resetDailyFlags() {
        underflowHandledForToday = false
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