package com.example.expensetracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.mpesa.MPesaTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(transaction: MPesaTransaction) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val date = dateFormat.format(Date(transaction.dateTime))

    val backgroundColor = when (transaction.transactionType) {
        "SENT" -> Color(0xFFFFF9C4)  // Light yellow
        "RECEIVED" -> Color(0xFFDCEDC8)  // Light green
        "PAID" -> Color(0xFFFFCCBC)  // Light orange
        "WITHDRAW" -> Color(0xFFD7CCC8)  // Light brown
        "DEPOSIT" -> Color(0xFFBBDEFB)  // Light blue
        else -> Color(0xFFEFEBE9)  // Light grey
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.transactionType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = transaction.amount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = transaction.senderOrRecipient)
            Text(text = "Ref: ${transaction.transactionId}")
            Text(text = date)

            transaction.balance?.let {
                Text(text = "Balance: $it")
            }

            transaction.fee?.let {
                Text(text = "Fee: $it", color = Color.Gray)
            }

            // For debugging, show part of the raw message
            Text(
                text = transaction.rawMessage.take(50) + "...",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}