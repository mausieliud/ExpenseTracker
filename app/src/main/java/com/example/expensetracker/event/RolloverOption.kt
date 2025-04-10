package com.example.expensetracker.event

enum class RolloverOption {
    NONE,           // No automatic handling
    REALLOCATE,     // Spread across remaining days
    SAVE,           // Add to savings without changing daily budget
    ADD_TO_TOMORROW // Add only to tomorrow's allocation
}