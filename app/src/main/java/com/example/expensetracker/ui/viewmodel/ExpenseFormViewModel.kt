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
import kotlinx.coroutines.flow.first
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

    // Store the original expense amount when editing
    private var originalExpenseAmount: Double = 0.0

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
            is ExpenseFormEvent.AddCustomCategory -> {
                val newCategories = _formState.value.categories + event.category
                _formState.update { it.copy(
                    categories = newCategories,
                    category = event.category
                ) }
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

                // Update the budget correctly when editing an expense
                // We need to account for the difference between the new and original amount
                val budget = budgetDao.getBudget().first()
                budget?.let {
                    val expenseDifference = amountValue - originalExpenseAmount
                    budgetDao.updateBudget(
                        it.copy(
                            remaining_budget = it.remaining_budget - expenseDifference
                        )
                    )
                }
            } else {
                expenseDao.insertExpense(expense)

                // Update budget remaining amount for new expenses
                val budget = budgetDao.getBudget().first()
                budget?.let {
                    budgetDao.updateBudget(
                        it.copy(
                            remaining_budget = it.remaining_budget - amountValue
                        )
                    )
                }
            }

            resetForm()
        }
    }

    fun setExpenseForEdit(expense: Expense) {
        // Store the original amount for correctly updating budget later
        originalExpenseAmount = expense.amount

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
        originalExpenseAmount = 0.0
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