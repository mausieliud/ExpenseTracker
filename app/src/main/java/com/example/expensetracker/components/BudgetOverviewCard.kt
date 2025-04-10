package com.example.expensetracker.components

import BudgetProgressBar
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun BudgetOverviewCard(
    remainingToday: Double,
    totalSpentToday: Double,
    totalBudget: Double,
    remainingBudget: Double,
    todayAdjustment: Double = 0.0,  // Added parameter for today's adjustment
    rolloverInfo: String? = null     // Optional info about recent rollovers
) {
    BudgetProgressBar(totalBudget, remainingBudget)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Budget Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budget:")
                Text("Ksh.${String.format("%.2f", totalBudget)}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Remaining Overall:")
                Text(
                    "Ksh.${String.format("%.2f", remainingBudget)}",
                    color = if (remainingBudget > 0) Color.Green else Color.Red
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Show today's adjustment if any
            if (todayAdjustment != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Today's Adjustment:")
                    Text(
                        "Ksh.${String.format("%.2f", todayAdjustment)}",
                        color = if (todayAdjustment > 0) Color.Green else Color.Red
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Spent Today:")
                Text("Ksh.${String.format("%.2f", totalSpentToday)}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Remaining Today:")
                Text(
                    "Ksh.${String.format("%.2f", remainingToday)}",
                    color = if (remainingToday > 0) Color.Green else Color.Red
                )
            }

            // Show rollover information if provided
            rolloverInfo?.let {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}