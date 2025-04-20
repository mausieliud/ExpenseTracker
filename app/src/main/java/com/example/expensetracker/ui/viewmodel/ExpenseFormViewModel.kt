package com.example.expensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.BudgetDatabase
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.event.ExpenseFormEvent
import com.example.expensetracker.mpesa.MPesaTransaction
import com.example.expensetracker.state.ExpenseFormState
import com.example.expensetracker.mpesa.toExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
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

    // Add state for batch processing
    private val _processingBatch = MutableStateFlow(false)
    val processingBatch: StateFlow<Boolean> = _processingBatch.asStateFlow()

    // Track batch processing progress
    private val _batchProgress = MutableStateFlow(0)
    val batchProgress: StateFlow<Int> = _batchProgress.asStateFlow()

    // Total items in current batch
    private val _batchTotal = MutableStateFlow(0)
    val batchTotal: StateFlow<Int> = _batchTotal.asStateFlow()

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

    // New method to save expense without affecting budget if it's a historical expense
    fun saveExpenseDirectly(expense: Expense) {
        viewModelScope.launch {
            // Insert the expense
            expenseDao.insertExpense(expense)

            // Only update budget if the expense is from the current budget period
            if (isExpenseInCurrentBudgetPeriod(expense.date)) {
                val budget = budgetDao.getBudget().first()
                budget?.let {
                    budgetDao.updateBudget(
                        it.copy(
                            remaining_budget = it.remaining_budget - expense.amount
                        )
                    )
                }
            }
        }
    }

    // Helper method to determine if an expense falls within the current budget period
    //To prevent past expenses from affecting the budget.
    private fun isExpenseInCurrentBudgetPeriod(expenseDate: String): Boolean {
        try {
            // Parse the expense date
            val parsedExpenseDate = dateFormat.parse(expenseDate) ?: return false

            // Get current budget period start date (this implementation assumes monthly budget)
            val calendar = Calendar.getInstance()
            val today = calendar.time

            // Reset to first day of current month for budget period start
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val currentBudgetStart = calendar.time

            // Check if expense date is within current budget period
            return !parsedExpenseDate.before(currentBudgetStart) && !parsedExpenseDate.after(today)
        } catch (e: Exception) {
            // If there's any parsing error, default to not affecting budget
            return false
        }
    }

    // Method to save a batch of MPesa transactions as expenses
    fun saveBatchTransactions(transactions: List<MPesaTransaction>): Boolean {
        if (transactions.isEmpty()) return false

        _processingBatch.value = true
        _batchTotal.value = transactions.size
        _batchProgress.value = 0

        viewModelScope.launch {
            try {
                // Process each transaction in the batch
                transactions.forEachIndexed { index, transaction ->
                    val expense = transaction.toExpense()
                    saveExpenseDirectly(expense)

                    // Update progress
                    _batchProgress.value = index + 1
                }
            } finally {
                _processingBatch.value = false
            }
        }

        return true
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

                // Only update budget if expense is in current budget period
                if (isExpenseInCurrentBudgetPeriod(expense.date)) {
                    val budget = budgetDao.getBudget().first()
                    budget?.let {
                        val expenseDifference = amountValue - originalExpenseAmount
                        budgetDao.updateBudget(
                            it.copy(
                                remaining_budget = it.remaining_budget - expenseDifference
                            )
                        )
                    }
                }
            } else {
                expenseDao.insertExpense(expense)

                // Only update budget if expense is in current budget period
                if (isExpenseInCurrentBudgetPeriod(expense.date)) {
                    val budget = budgetDao.getBudget().first()
                    budget?.let {
                        budgetDao.updateBudget(
                            it.copy(
                                remaining_budget = it.remaining_budget - amountValue
                            )
                        )
                    }
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

    // Method to categorize transactions based on predefined rules
    fun suggestCategory(description: String, recipient: String): String {
        return when {
            description.contains("food", ignoreCase = true) ||
                    description.contains("restaurant", ignoreCase = true) ||
                    recipient.contains("restaurant", ignoreCase = true) ||
                    recipient.contains("cafe", ignoreCase = true) -> "Food"

            description.contains("transport", ignoreCase = true) ||
                    description.contains("taxi", ignoreCase = true) ||
                    description.contains("uber", ignoreCase = true) ||
                    description.contains("fare", ignoreCase = true) -> "Transport"

            description.contains("bill", ignoreCase = true) ||
                    description.contains("utility", ignoreCase = true) ||
                    description.contains("water", ignoreCase = true) ||
                    description.contains("electricity", ignoreCase = true) -> "Utilities"

            description.contains("shopping", ignoreCase = true) ||
                    description.contains("store", ignoreCase = true) ||
                    description.contains("market", ignoreCase = true) -> "Shopping"

            else -> "Other"
        }
    }

    // Helper method to check if batch processing is complete
    fun isBatchComplete(): Boolean {
        return !_processingBatch.value && _batchProgress.value == _batchTotal.value && _batchTotal.value > 0
    }

    // Reset batch processing state
    fun resetBatchState() {
        _processingBatch.value = false
        _batchProgress.value = 0
        _batchTotal.value = 0
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