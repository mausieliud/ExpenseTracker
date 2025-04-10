package com.example.expensetracker.state

import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.DailyAdjustment
import com.example.expensetracker.data.entity.Expense

data class BudgetState(
    val budget: Budget? = null,
    val expenses: List<Expense> = emptyList(),
    val dailyAdjustments: List<DailyAdjustment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val remainingBudgetForToday: Double = 0.0,
    val totalSpentToday: Double = 0.0
)