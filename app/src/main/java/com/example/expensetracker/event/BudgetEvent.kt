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
}