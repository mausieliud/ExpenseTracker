package com.example.expensetracker.event

import com.example.expensetracker.data.entity.Expense

sealed class BudgetEvent {
    data object LoadBudget : BudgetEvent()
    data class DeleteExpense(val expense: Expense) : BudgetEvent()
    data class AddDailyAdjustment(val date: String, val amount: Double) : BudgetEvent()
    data object ResetAllData : BudgetEvent()
    data object CheckForDayEnd : BudgetEvent()
    data object SetupAutomaticRollover : BudgetEvent()

    // Replace the old ConfigureRolloverSettings with simpler version
    data class SetAutomaticRollover(
        val isEnabled: Boolean,
        val option: RolloverOption = RolloverOption.NONE
    ) : BudgetEvent()
}   data class ManualRolloverFromDate(val fromDate: String) : BudgetEvent()


