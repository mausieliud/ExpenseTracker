package com.example.expensetracker.components

import BudgetProgressBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun BudgetOverviewCard(
    remainingToday: Double,
    totalSpentToday: Double,
    totalBudget: Double,
    remainingBudget: Double,
    budget: com.example.expensetracker.data.entity.Budget? = null,
    hasUnderflow: Boolean = false,
    underflowAmount: Double = 0.0,
    onSaveUnderflow: () -> Unit = {},
    onRolloverUnderflow: () -> Unit = {},
    onIgnoreUnderflow: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val currentDate = dateFormat.format(Date())

    // Calculate budget stats
    val daysLeft = budget?.let {
        try {
            val endDateObj = dateFormat.parse(it.end_date)
            val todayDateObj = dateFormat.parse(currentDate)

            if (endDateObj != null && todayDateObj != null) {
                max(0, ((endDateObj.time - todayDateObj.time) / (1000 * 60 * 60 * 24)).toInt() + 1)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    } ?: 0

    val dailyBudgetRate = if (daysLeft > 0) remainingBudget / daysLeft else 0.0
    val percentRemaining = if (totalBudget > 0) (remainingBudget / totalBudget) * 100 else 0.0
    val savings = budget?.savings ?: 0.0

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
                    "Ksh.${String.format("%.2f", remainingBudget)} (${String.format("%.1f", percentRemaining)}%)",
                    color = when {
                        percentRemaining > 40 -> Color.Green
                        percentRemaining > 20 -> Color(0xFFFFA500) // Orange
                        else -> Color.Red
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Savings:")
                Text(
                    "Ksh.${String.format("%.2f", savings)}",
                    color = Color(0xFF4CAF50) // Green color for savings
                )
            }

            if (daysLeft > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Days Remaining:")
                    Text("$daysLeft days")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Budget:")
                    Text("Ksh.${String.format("%.2f", dailyBudgetRate)}")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            // Show overspent warning if needed
            if (remainingToday < 0) {
                Text(
                    "⚠️ You've exceeded today's budget. This will affect your future daily allowance.",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Show underflow options if there's unused budget for today
            AnimatedVisibility(
                visible = hasUnderflow && underflowAmount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(8.dp)
                ) {
                    Text(
                        "You have Ksh.${String.format("%.2f", underflowAmount)} remaining in today's budget",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "What would you like to do with this amount?",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onSaveUnderflow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save It")
                        }

                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                        OutlinedButton(
                            onClick = onRolloverUnderflow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rollover")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onIgnoreUnderflow,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Ignore", color = Color.Gray)
                    }
                }
            }
        }
    }
}