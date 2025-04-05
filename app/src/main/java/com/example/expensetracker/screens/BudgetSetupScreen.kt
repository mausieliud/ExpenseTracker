package com.example.expensetracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.event.BudgetFormEvent
import com.example.expensetracker.ui.viewmodel.BudgetFormViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    viewModel: BudgetFormViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.formState.collectAsState()

    // Load existing budget if available
    LaunchedEffect(Unit) {
        viewModel.loadExistingBudget()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Total budget field
            OutlinedTextField(
                value = state.totalBudget,
                onValueChange = { viewModel.onEvent(BudgetFormEvent.TotalBudgetChanged(it)) },
                label = { Text("Total Budget") },
                prefix = { Text("Ksh.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Start date field
            OutlinedTextField(
                value = state.startDate,
                onValueChange = { viewModel.onEvent(BudgetFormEvent.StartDateChanged(it)) },
                label = { Text("Start Date (YYYY-MM-DD)") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    // In a real app, clicking this would show a date picker
                },
                modifier = Modifier.fillMaxWidth()
            )

            // End date field
            OutlinedTextField(
                value = state.endDate,
                onValueChange = { viewModel.onEvent(BudgetFormEvent.EndDateChanged(it)) },
                label = { Text("End Date (YYYY-MM-DD)") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Savings field
            OutlinedTextField(
                value = state.desiredSavings,
                onValueChange = { viewModel.onEvent(BudgetFormEvent.DesiredSavingsChanged(it)) },
                label = { Text("Savings Goal (optional)") },
                prefix = { Text("Ksh.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Error message if any
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.onEvent(BudgetFormEvent.CancelSetup) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        viewModel.onEvent(BudgetFormEvent.SaveBudget)
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}