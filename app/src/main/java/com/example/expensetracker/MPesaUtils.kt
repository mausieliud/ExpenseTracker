package com.example.expensetracker

import com.example.expensetracker.mpesa.MPesaTransaction
import com.example.expensetracker.data.entity.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun MPesaTransaction.toExpense(): Expense {
    // Determine an appropriate category based on transaction type
    val category = when (transactionType) {
        "SENT" -> "Transfer"
        "PAID" -> "Payment"
        "WITHDRAW" -> "Withdrawal"
        "RECEIVED" -> "Income"
        "DEPOSIT" -> "Deposit"
        else -> "Other"
    }

    // Parse amount string to double, removing "KSh " prefix and commas
    val amountValue = amount.replace("KSh ", "").replace(",", "").toDoubleOrNull() ?: 0.0

    // Format date to app's standard format
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(dateTime))

    // Create description using transaction type and sender/recipient
    val description = "$transactionType: $senderOrRecipient (M-Pesa: $transactionId)"

    return Expense(
        id = 0, // New expense, ID will be generated
        description = description,
        amount = amountValue,
        category = category,
        date = formattedDate
    )
}