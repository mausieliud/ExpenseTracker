package com.example.expensetracker.state

import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.DailyAdjustment
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.event.RolloverOption


data class BudgetState(
    val budget: Budget? = null,
    val expenses: List<Expense> = emptyList(),
    val dailyAdjustments: List<DailyAdjustment> = emptyList(),
    val isLoading: Boolean = true,
    val remainingBudgetForToday: Double = 0.0,
    val totalSpentToday: Double = 0.0,
    val error: String? =null,

    // Rollover settings
    val isAutomaticRolloverEnabled: Boolean = false,
    val rolloverOption: RolloverOption = RolloverOption.NONE,

    // For UI feedback on adjustments
    val lastRolloverAmount: Double = 0.0,
    val lastRolloverDate: String = ""
)
