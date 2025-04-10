package com.example.expensetracker.event

import com.example.expensetracker.data.entity.Expense

sealed class BudgetEvent {
    data class LoadBudget(val forceRefresh: Boolean = false) : BudgetEvent()
    data object NavigateToAddExpense : BudgetEvent()
    data object NavigateToBudgetSetup : BudgetEvent()
    data class DeleteExpense(val expense: Expense) : BudgetEvent()
    data class EditExpense(val expense: Expense) : BudgetEvent()
    data class AddDailyAdjustment(val amount: Double, val date: String) : BudgetEvent()
    data object ResetAllData : BudgetEvent()
    //triggers day end check
    data object CheckForDayEnd : BudgetEvent()
    data object SetupAutomaticRollover : BudgetEvent()
    data class ConfigureRolloverSettings(
        val allowOverflow: Boolean = true,
        val allowUnderflow: Boolean = true,
        val maxOverflowPercentage: Double = 100.0,
        val maxUnderflowPercentage: Double = 50.0,
        val isEnabled: Boolean = false
    ) : BudgetEvent()
    //manually conrolling rollover
    // Add events for manually running rollover
    //todo...still not working needs proper integration with view model
    data class ManualRolloverFromDate(val fromDate: String) : BudgetEvent()
}