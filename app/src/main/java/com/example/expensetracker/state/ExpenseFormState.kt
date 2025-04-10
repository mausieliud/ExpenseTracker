package com.example.expensetracker.state

data class ExpenseFormState(
    val description: String = "",
    val amount: String = "",
    val category: String = "",
    val date: String = "",
    val isEditing: Boolean = false,
    val currentExpenseId: Long = 0,
    val categories: List<String> = listOf("Food", "Transport", "Entertainment", "Bills", "Other")
)