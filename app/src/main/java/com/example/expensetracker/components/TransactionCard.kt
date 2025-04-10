package com.example.expensetracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.mpesa.MPesaTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Selectable transaction card component
@Composable
fun SelectableTransactionCard(
    transaction: MPesaTransaction,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAddAsExpense: (MPesaTransaction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for selection
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() }
            )

            // Transaction details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = transaction.transactionType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = transaction.senderOrRecipient,
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.amount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (transaction.transactionType) {
                            "RECEIVED", "DEPOSIT" -> Color(0xFF4CAF50)
                            else -> Color(0xFFE91E63)
                        }
                    )

                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(transaction.dateTime)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "ID: ${transaction.transactionId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add button (takes you to form for detailed editing)
            Button(
                onClick = { onAddAsExpense(transaction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Edit")
            }
        }
    }
}

//Initial Transaction Card.

//@Composable
//fun TransactionCard(
//    transaction: MPesaTransaction,
//    onAddAsExpense: (MPesaTransaction) -> Unit = {}
//) {
//    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
//    val date = dateFormat.format(Date(transaction.dateTime))
//
//    val backgroundColor = when (transaction.transactionType) {
//        "SENT" -> Color(0xFFC01D97)  // Light yellow
//        "RECEIVED" -> Color(0xFF19D383)  // Light green
//        "PAID" -> Color(0xFFF57F17)  // Light orange
//        "WITHDRAW" -> Color(0xFF006064)  // Light brown
//        "DEPOSIT" -> Color(0xFF429AE3)  // Light blue
//        else -> Color(0xFFEFEBE9)  // Light grey
//    }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = backgroundColor)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = transaction.transactionType,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//                Text(
//                    text = transaction.amount,
//                    fontWeight = FontWeight.Bold,
//                    fontSize = 16.sp
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(text = transaction.senderOrRecipient)
//            Text(text = "Ref: ${transaction.transactionId}")
//            Text(text = date)
//
//            transaction.balance?.let {
//                Text(text = "Balance: $it")
//            }
//
//            transaction.fee?.let {
//                Text(text = "Fee: $it", color = Color.Gray)
//            }
//
//            // For debugging, show part of the raw message
//            Text(
//                text = transaction.rawMessage.take(50) + "...",
//                fontSize = 10.sp,
//                color = Color.Gray,
//                modifier = Modifier.padding(top = 8.dp)
//            )
//
//            // Add button to add transaction as expense
//            Spacer(modifier = Modifier.height(8.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.End
//            ) {
//                TextButton(
//                    onClick = { onAddAsExpense(transaction) }
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "Add as expense"
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Text("Add as Expense", color = Color.Black)
//                }
//            }
//        }
//    }
//}
