package com.example.expensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.BudgetDatabase
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.event.ExpenseFormEvent
import com.example.expensetracker.state.ExpenseFormState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseFormViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BudgetDatabase.getDatabase(application)
    private val expenseDao = database.expenseDao()
    private val budgetDao = database.budgetDao()

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState: StateFlow<ExpenseFormState> = _formState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Set default date to today
        _formState.update { it.copy(date = dateFormat.format(Date())) }
    }

    fun onEvent(event: ExpenseFormEvent) {
        when (event) {
            is ExpenseFormEvent.DescriptionChanged -> {
                _formState.update { it.copy(description = event.description) }
            }

            is ExpenseFormEvent.AmountChanged -> {
                _formState.update { it.copy(amount = event.amount) }
            }

            is ExpenseFormEvent.CategoryChanged -> {
                _formState.update { it.copy(category = event.category) }
            }

            is ExpenseFormEvent.DateChanged -> {
                _formState.update { it.copy(date = event.date) }
            }

            is ExpenseFormEvent.SaveExpense -> {
                saveExpense()
            }

            is ExpenseFormEvent.CancelEdit -> {
                resetForm()
            }
        }
    }

    private fun saveExpense() {
        val state = _formState.value

        val amountValue = state.amount.toDoubleOrNull() ?: return

        if (state.description.isBlank() || state.category.isBlank() || state.date.isBlank()) {
            return
        }

        viewModelScope.launch {
            val expense = Expense(
                id = if (state.isEditing) state.currentExpenseId else 0,
                description = state.description,
                amount = amountValue,
                category = state.category,
                date = state.date
            )

            if (state.isEditing) {
                expenseDao.updateExpense(expense)
            } else {
                expenseDao.insertExpense(expense)

                // Update budget remaining amount
                budgetDao.getBudget().collect { budget ->
                    budget?.let {
                        budgetDao.updateBudget(
                            it.copy(
                                remaining_budget = it.remaining_budget - amountValue
                            )
                        )
                    }
                    return@collect
                }
            }

            resetForm()
        }
    }

    fun setExpenseForEdit(expense: Expense) {
        _formState.update {
            it.copy(
                description = expense.description,
                amount = expense.amount.toString(),
                category = expense.category,
                date = expense.date,
                isEditing = true,
                currentExpenseId = expense.id
            )
        }
    }

    private fun resetForm() {
        _formState.update {
            ExpenseFormState(date = dateFormat.format(Date()))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseFormViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseFormViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}