package com.example.expensetracker.state

data class BudgetFormState(
    val totalBudget: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val desiredSavings: String = "0.0",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)