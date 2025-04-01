package com.example.expensetracker.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.model.TimeSeriesDataPoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ExpenseChart(
    expenseData: List<TimeSeriesDataPoint>,
    timeRange: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Expense Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (expenseData.isEmpty()) {
                Text(
                    text = "No data available for this time range",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            } else {
                // Chart canvas
                val outlineColor = colorScheme.outline.copy(alpha = 0.3f)
                val primaryColor = colorScheme.primary
                val primaryColorAlpha = colorScheme.primary.copy(alpha = 0.2f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (expenseData.size > 1) {
                        val maxValue = expenseData.maxOf { it.amount } * 1.1f
                        val minValue = expenseData.minOf { it.amount }.coerceAtLeast(0.0) * 0.9f
                        val width = size.width
                        val height = size.height
                        val stepX = width / (expenseData.size - 1)

                        // Draw axis lines
                        drawLine(
                            color = outlineColor,  // Fixed
                            start = Offset(0f, height),
                            end = Offset(width, height),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw data points and connecting lines
                        val points = expenseData.mapIndexed { index, dataPoint ->
                            Offset(
                                x = index * stepX,
                                y = height - (height * ((dataPoint.amount - minValue) / (maxValue - minValue)).toFloat())
                            )
                        }

                        // Draw line graph
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = primaryColor,  // Fixed
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // Draw area under the curve
                        val path = Path().apply {
                            moveTo(0f, height)
                            lineTo(points.first().x, points.first().y)
                            points.forEach { point ->
                                lineTo(point.x, point.y)
                            }
                            lineTo(points.last().x, height)
                            close()
                        }

                        drawPath(
                            path = path,
                            color = primaryColorAlpha  // Fixed
                        )

                        // Draw points
                        points.forEach { point ->
                            drawCircle(
                                color = primaryColor,  // Fixed
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = point
                            )
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(8.dp))

                    // X-axis labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val displayLabels = when {
                            expenseData.size > 10 -> expenseData.filterIndexed { index, _ ->
                                index % (expenseData.size / 5) == 0 || index == expenseData.size - 1
                            }
                            else -> expenseData
                        }

                        displayLabels.forEachIndexed { index, dataPoint ->
                            Text(
                                text = dataPoint.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.outline,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = if (index == 0) TextAlign.Start
                                else if (index == displayLabels.size - 1) TextAlign.End
                                else TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display min/max values
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expenseData.isNotEmpty()) {
                        val maxEntry = expenseData.maxByOrNull { it.amount }
                        val minEntry = expenseData.minByOrNull { it.amount }

                        if (maxEntry != null && minEntry != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Highest Spending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.outline
                                )
                                Text(
                                    text = "${maxEntry.label}: ${currencyFormat.format(maxEntry.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lowest Spending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.outline
                                )
                                Text(
                                    text = "${minEntry.label}: ${currencyFormat.format(minEntry.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
