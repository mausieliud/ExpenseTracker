package com.example.expensetracker.mpesa.classes

data class MPesaTransaction(
    val transactionId: String,
    val amount: String,
    val senderOrRecipient: String,
    val transactionType: String,
    val dateTime: Long,
    val balance: String? = null,
    val fee: String? = null,
    val rawMessage: String
)