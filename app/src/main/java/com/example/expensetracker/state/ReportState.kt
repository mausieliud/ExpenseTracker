package com.example.expensetracker.state

import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.model.TimeSeriesDataPoint
import java.util.Date

data class ReportState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val timeRange: String = "This Month",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val expenses: List<Expense> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val totalSpent: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val timeSeriesData: List<TimeSeriesDataPoint> = emptyList(),
    val budget: Budget? = null,
    val dailySpending: Map<String, Double> = emptyMap(),
)