package com.example.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_adjustments")
data class DailyAdjustment(
    @PrimaryKey
    val date: String,
    val adjustment: Double
)