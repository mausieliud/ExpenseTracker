package com.example.expensetracker.mpesa.classes

data class TransactionSort(
    val field: SortField = SortField.DATE,
    val order: SortOrder = SortOrder.DESCENDING
)