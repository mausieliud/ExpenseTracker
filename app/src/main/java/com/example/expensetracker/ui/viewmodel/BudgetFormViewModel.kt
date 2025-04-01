package com.example.expensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.BudgetDatabase
import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.event.BudgetFormEvent
import com.example.expensetracker.state.BudgetFormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetFormViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BudgetDatabase.getDatabase(application)
    private val budgetDao = database.budgetDao()

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Set default start date to today
        _formState.update { it.copy(startDate = dateFormat.format(Date())) }
    }

    fun onEvent(event: BudgetFormEvent) {
        when (event) {
            is BudgetFormEvent.TotalBudgetChanged -> {
                _formState.update { it.copy(totalBudget = event.amount) }
            }

            is BudgetFormEvent.StartDateChanged -> {
                _formState.update { it.copy(startDate = event.date) }
            }

            is BudgetFormEvent.EndDateChanged -> {
                _formState.update { it.copy(endDate = event.date) }
            }

            is BudgetFormEvent.DesiredSavingsChanged -> {
                _formState.update { it.copy(desiredSavings = event.amount) }
            }

            is BudgetFormEvent.SaveBudget -> {
                saveBudget()
            }

            is BudgetFormEvent.CancelSetup -> {
                resetForm()
            }
        }
    }

    private fun saveBudget() {
        val state = _formState.value
        _formState.update { it.copy(isSubmitting = true) }

        try {
            val totalBudget = state.totalBudget.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid budget amount")

            val savings = state.desiredSavings.toDoubleOrNull() ?: 0.0

            if (state.startDate.isBlank() || state.endDate.isBlank()) {
                throw IllegalArgumentException("Dates cannot be empty")
            }

            // Parse dates to calculate allocation per day
            val startDate = dateFormat.parse(state.startDate)
                ?: throw IllegalArgumentException("Invalid start date")
            val endDate = dateFormat.parse(state.endDate)
                ?: throw IllegalArgumentException("Invalid end date")

            val daysInPeriod = ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1

            if (daysInPeriod <= 0) {
                throw IllegalArgumentException("End date must be after start date")
            }

            if (savings > totalBudget) {
                throw IllegalArgumentException("Savings cannot exceed total budget")
            }

            val spendableBudget = totalBudget - savings
            val allocatedPerDay = spendableBudget / daysInPeriod

            viewModelScope.launch {
                val budget = Budget(
                    total_budget = totalBudget,
                    start_date = state.startDate,
                    end_date = state.endDate,
                    allocation_per_day = allocatedPerDay,
                    remaining_budget = spendableBudget,
                    savings = savings
                )

                budgetDao.insertBudget(budget)
                resetForm()
            }
        } catch (e: Exception) {
            _formState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = e.message ?: "Error saving budget"
                )
            }
        }
    }

    private fun resetForm() {
        _formState.update {
            BudgetFormState(startDate = dateFormat.format(Date()))
        }
    }

    fun loadExistingBudget() {
        viewModelScope.launch {
            budgetDao.getBudget().collect { budget ->
                budget?.let {
                    _formState.update { state ->
                        state.copy(
                            totalBudget = it.total_budget.toString(),
                            startDate = it.start_date,
                            endDate = it.end_date,
                            desiredSavings = it.savings.toString()
                        )
                    }
                }
                return@collect
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BudgetFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}