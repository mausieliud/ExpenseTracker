package com.example.expensetracker.state

import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.DailyAdjustment
import com.example.expensetracker.data.entity.Expense

data class BudgetState(
    val budget: Budget? = null,
    val expenses: List<Expense> = emptyList(),
    val dailyAdjustments: List<DailyAdjustment> = emptyList(),
    val isLoading: Boolean = true,
    val remainingBudgetForToday: Double = 0.0,
    val totalSpentToday: Double = 0.0,
    val daysLeft: Int = 0,
    val dailyBudgetRate: Double = 0.0,
    val hasUnderflow: Boolean = false,
    val underflowAmount: Double = 0.0,
    val error: String? = null
)