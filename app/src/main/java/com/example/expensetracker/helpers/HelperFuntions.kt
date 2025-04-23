package com.example.expensetracker.helpers

import androidx.compose.ui.graphics.Color
import com.example.expensetracker.data.entity.Expense
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.pow

// Function to calculate color based on remaining percentage
fun calculateColor(remaining: Double, total: Double): Color {
    val percentage = if (total > 0) remaining / total else 0.0
    val safePercentage = percentage.coerceIn(0.0, 1.0)

    val red = (255 * (1 - safePercentage)).toInt()
    val green = (255 * safePercentage).toInt()

    return Color(red, green, 0)
}
//extension functions for rounding decimal places
fun Double.roundToDecimalPlaces(places: Int): Double {
    val factor = 10.0.pow(places.toDouble())
    return kotlin.math.round(this * factor) / factor
}

fun calculateTrend(dailyTotals: List<Pair<Date, Double>>): Double {
    if (dailyTotals.size < 2) return 0.0

    // Simple linear regression to detect trend direction
    val n = dailyTotals.size
    val x = List(n) { it.toDouble() }
    val y = dailyTotals.map { it.second }

    val sumX = x.sum()
    val sumY = y.sum()
    val sumXY = x.zip(y).sumOf { it.first * it.second }
    val sumXX = x.sumOf { it * it }

    // Calculate slope
    return if (n * sumXX - sumX * sumX != 0.0) {
        (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    } else {
        0.0
    }
}

// Helper function to convert date string to Date objects
fun convertToDatePairs(dailySpending: Map<String, Double>): List<Pair<Date, Double>> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dailySpending.map { (dateStr, amount) ->
        Pair(dateFormat.parse(dateStr) ?: Date(), amount)
    }.sortedBy { it.first }
}

 fun calculateWeeklyAverages(expenses: List<Expense>): List<Pair<LocalDate, Double>> {
    // Group expenses by week
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val expensesByWeek = expenses
        .groupBy { expense ->
            val date = dateFormat.parse(expense.date) ?: return@groupBy null
            val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            // Find the start of the week (assuming Sunday is first day)
            localDate.minusDays(localDate.dayOfWeek.value % 7L)
        }
        .filterKeys { it != null }
        .mapKeys { it.key!! }

    // Calculate average for each week
    return expensesByWeek.map { (weekStart, expensesInWeek) ->
        Pair(weekStart, expensesInWeek.sumOf { it.amount } / 7.0)
    }.sortedBy { it.first }
}


