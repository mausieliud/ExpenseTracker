package com.example.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget")
data class Budget(
    val total_budget: Double,
    val start_date: String,
    val end_date: String,
    val allocation_per_day: Double,
    val remaining_budget: Double,
    val savings: Double = 0.0,
    @PrimaryKey
    val id: Long = 1  // Only one budget record
)