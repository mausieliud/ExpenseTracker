package com.example.expensetracker.mpesa

data class TransactionFilter(
    val query: String = "",
    val types: Set<String> = setOf("SENT", "RECEIVED", "PAID", "WITHDRAW", "DEPOSIT"),
)