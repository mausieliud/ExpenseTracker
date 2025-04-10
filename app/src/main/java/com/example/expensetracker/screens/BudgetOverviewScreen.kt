package com.example.expensetracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.RequestPage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.components.BudgetOverviewCard
import com.example.expensetracker.components.ExpenseItem
import com.example.expensetracker.event.BudgetEvent
import com.example.expensetracker.state.BudgetState
import com.example.expensetracker.ui.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetOverviewScreen(
    viewModel: BudgetViewModel,
    navigateToAddExpense: () -> Unit,
    navigateToBudgetSetup: () -> Unit,
    navigateToReports: () -> Unit,
    navigateToMPesaParser: () -> Unit
) {
    val state by viewModel.budgetState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Tracker") },
                actions = {
                   IconButton(onClick = navigateToMPesaParser) {
                       Icon(Icons.Default.ManageHistory, contentDescription = "navigate to mpesaparser")
                   }
                    IconButton(onClick = navigateToReports) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reports")
                    }
                    IconButton(onClick = navigateToBudgetSetup) {
                        Icon(Icons.Default.Settings, contentDescription = "Budget Settings")
                    }

                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = navigateToAddExpense) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.budget == null) {
            // No budget set up yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No budget has been set up yet",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tap the settings icon to create your first budget",
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Main budget content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Budget overview card
                BudgetOverviewCard(
                    totalBudget = state.budget?.total_budget ?: 0.0,
                    remainingBudget = state.budget?.remaining_budget ?: 0.0,
                    remainingToday = state.remainingBudgetForToday,
                    totalSpentToday = state.totalSpentToday,
                    todayAdjustment = state.dailyAdjustments.find { it.date == getCurrentDate() }?.adjustment ?: 0.0,
                    rolloverInfo = generateRolloverInfoText(state)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recent expenses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Expenses",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(state.expenses.take(10)) { expense ->
                        ExpenseItem(
                            expense = expense,
                            onDelete = { viewModel.onEvent(BudgetEvent.DeleteExpense(expense)) }
                        )
                    }
                }
            }
        }
    }
}



private fun generateRolloverInfoText(state: BudgetState): String? {
    // Only show info if we have recent rollover data
    if (state.lastRolloverAmount == 0.0 || state.lastRolloverDate.isEmpty()) {
        return null
    }

    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val formattedDate = try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(state.lastRolloverDate)
        dateFormat.format(parsedDate)
    } catch (e: Exception) {
        state.lastRolloverDate // Fallback
    }

    val amount = String.format("%.2f", kotlin.math.abs(state.lastRolloverAmount))

    return when {
        state.lastRolloverAmount > 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.REALLOCATE ->
            "Unspent Ksh.$amount from $formattedDate was distributed to remaining days"
        state.lastRolloverAmount > 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.SAVE ->
            "Unspent Ksh.$amount from $formattedDate was added to your savings"
        state.lastRolloverAmount > 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW ->
            "Unspent Ksh.$amount from $formattedDate was added to tomorrow's budget"
        state.lastRolloverAmount < 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.REALLOCATE ->
            "Overspent Ksh.$amount from $formattedDate was deducted from remaining days"
        state.lastRolloverAmount < 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.SAVE ->
            "Overspent Ksh.$amount from $formattedDate was deducted from your savings"
        state.lastRolloverAmount < 0 && state.rolloverOption == com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW ->
            "Overspent Ksh.$amount from $formattedDate was deducted from tomorrow's budget"
        else -> null
    }
}

// Helper function to get current date
private fun getCurrentDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}