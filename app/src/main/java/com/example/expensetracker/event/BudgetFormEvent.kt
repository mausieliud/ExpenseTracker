package com.example.expensetracker.event

sealed class BudgetFormEvent {
    data class TotalBudgetChanged(val amount: String) : BudgetFormEvent()
    data class StartDateChanged(val date: String) : BudgetFormEvent()
    data class EndDateChanged(val date: String) : BudgetFormEvent()
    data class DesiredSavingsChanged(val amount: String) : BudgetFormEvent()
    data object SaveBudget : BudgetFormEvent()
    data object CancelSetup : BudgetFormEvent()
}