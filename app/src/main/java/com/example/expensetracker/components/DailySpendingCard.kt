// Create a new file: DailySpendingCard.kt
package com.example.expensetracker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.roundToDecimalPlaces
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@Composable
fun DailySpendingCard(
    dailyTotals: Map<String, Double>,
    dailyBudget: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Daily Spending",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (dailyTotals.isEmpty()) {
                Text(
                    text = "No daily spending data available",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Format for display
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())


                // Show up to 7 days of data, most recent first
                dailyTotals.entries.take(7).forEach { (dateStr, total) ->
                    val date = try {
                        dateFormat.parse(dateStr)
                    } catch (e: Exception) {
                        Date() // Fallback
                    }

                    val overBudget = dailyBudget > 0 && total > dailyBudget
                    val percentOfBudget = if (dailyBudget > 0) (total / dailyBudget * 100) else 0.0

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (date != null) displayFormat.format(date) else dateStr,
                                fontWeight = FontWeight.Medium
                            )
                            Row {
                                Text(
                                    text = "Ksh.${total.roundToDecimalPlaces(2)}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (overBudget) MaterialTheme.colorScheme.error else Color.Unspecified
                                )
                                if (dailyBudget > 0) {
                                    Text(
                                        text = " (${String.format("%.0f", percentOfBudget)}%)",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        // Daily spending progress bar
                        if (dailyBudget > 0) {
                            val progressPercentage = (percentOfBudget / 100).coerceAtMost(1.0).toFloat()
                            val spendingColor = when {
                                percentOfBudget > 100 -> MaterialTheme.colorScheme.error
                                percentOfBudget > 80 -> Color(0xFFF9A825) // Orange
                                else -> MaterialTheme.colorScheme.primary
                            }

                            LinearProgressIndicator(
                                progress = progressPercentage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = spendingColor
                            )
                        }
                    }
                }
            }
        }
    }
}