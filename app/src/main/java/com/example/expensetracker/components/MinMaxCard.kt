package com.example.expensetracker.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.Expense
import java.time.format.DateTimeFormatter

/**
 * A card that displays either the minimum or maximum expense with its details
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MinMaxSpentCard(
    modifier: Modifier = Modifier,
    isMin: Boolean,
    expenses: List<Expense>,
) {
    // Find min or max expense
    val expense = if (isMin) {
        expenses.minByOrNull { it.amount }
    } else {
        expenses.maxByOrNull { it.amount }
    }

    // If no expenses are available, show an empty card
    if (expense == null || expenses.isEmpty()) {
        EmptyMinMaxCard(modifier, isMin)
        return
    }

    // Calculate color based on expense amount relative to min/max
    val minValue = expenses.minOf { it.amount }
    val maxValue = expenses.maxOf { it.amount }

    // Normalized position of current expense between min and max (0 to 1)
    val normalizedValue = if (maxValue - minValue > 0) {
        (expense.amount - minValue) / (maxValue - minValue)
    } else {
        if (isMin) 0.0 else 1.0
    }

    // Generate color based on value - blue for minimum, red for maximum
    val cardColor = if (isMin) {
        Color(red = 0.1f, green = 0.4f, blue = 0.8f, alpha = (0.3f + 0.4f * normalizedValue.toFloat()))
    } else {
        Color(red = 0.8f, green = 0.2f, blue = 0.2f, alpha = (0.3f + 0.4f * normalizedValue.toFloat()))
    }

    val textColor = if (normalizedValue > 0.7) Color.White else Color.Black

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = textColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card title
            Text(
                text = if (isMin) "Minimum Expense" else "Maximum Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Expense amount
            Text(
                text = "Ksh.${expense.amount}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            Text(
                text = expense.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Label,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category chip from Ui Expense and category,need icons first
            CategoryChip(expense)

        }
    }
}

@Composable
private fun EmptyMinMaxCard(modifier: Modifier, isMin: Boolean) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMin) "Minimum Expense" else "Maximum Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No expense data available",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
        }
    }
}