package com.example.expensetracker.mpesa

data class TransactionSort(
    val field: SortField = SortField.DATE,
    val order: SortOrder = SortOrder.DESCENDING
)