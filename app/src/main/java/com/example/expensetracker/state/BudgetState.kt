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
    val totalSpentToday: Double = 0.0,
    //for handling overflow/underflow
    val isAutomaticRolloverEnabled: Boolean = false,
    val allowOverflow: Boolean = true,
    val allowUnderflow: Boolean = true,
    val maxOverflowPercentage: Double = 100.0,
    val maxUnderflowPercentage: Double = 50.0
)