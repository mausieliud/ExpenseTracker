package com.example.expensetracker.event

sealed class ExpenseFormEvent {
    data class DescriptionChanged(val description: String) : ExpenseFormEvent()
    data class AmountChanged(val amount: String) : ExpenseFormEvent()
    data class CategoryChanged(val category: String) : ExpenseFormEvent()
    data class DateChanged(val date: String) : ExpenseFormEvent()
    data object SaveExpense : ExpenseFormEvent()
    data object CancelEdit : ExpenseFormEvent()
    data class AddCustomCategory(val category: String) : ExpenseFormEvent()
}