package com.example.expensetracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import com.example.expensetracker.event.BudgetEvent
import com.example.expensetracker.ui.viewmodel.BudgetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetRolloverSettingsScreen(
    viewModel: BudgetViewModel,
    navigateBack: () -> Unit
) {
    val state by viewModel.budgetState.collectAsState()

    var enableRollover by remember { mutableStateOf(state.isAutomaticRolloverEnabled) }
    var selectedOption by remember { mutableStateOf(state.rolloverOption) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Rollover Settings") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
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
            // Main description card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Unspent Daily Budget Handling",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Choose how you want to handle unspent budget at the end of each day",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enableRollover,
                            onCheckedChange = { isEnabled ->
                                enableRollover = isEnabled
                                viewModel.onEvent(BudgetEvent.SetAutomaticRollover(
                                    isEnabled = isEnabled,
                                    option = selectedOption
                                ))
                            }
                        )
                        Text(
                            text = if (enableRollover) "Automatic handling enabled" else "Manual handling only",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Options card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "When you don't spend your daily budget:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Reallocate
                    RolloverOption(
                        title = "Reallocate to remaining days",
                        description = "Evenly distribute unspent amount across all remaining days in the budget period",
                        isSelected = selectedOption == com.example.expensetracker.event.RolloverOption.REALLOCATE,
                        onSelect = {
                            selectedOption =
                                com.example.expensetracker.event.RolloverOption.REALLOCATE
                            viewModel.onEvent(BudgetEvent.SetAutomaticRollover(enableRollover,
                                com.example.expensetracker.event.RolloverOption.REALLOCATE
                            ))
                        },
                        enabled = enableRollover
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Save
                    RolloverOption(
                        title = "Add to savings",
                        description = "Add unspent amount to your savings without affecting future daily budgets",
                        isSelected = selectedOption == com.example.expensetracker.event.RolloverOption.SAVE,
                        onSelect = {
                            selectedOption = com.example.expensetracker.event.RolloverOption.SAVE
                            viewModel.onEvent(BudgetEvent.SetAutomaticRollover(enableRollover,
                                com.example.expensetracker.event.RolloverOption.SAVE
                            ))
                        },
                        enabled = enableRollover
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 3: Add to tomorrow
                    RolloverOption(
                        title = "Add to tomorrow only",
                        description = "Add unspent amount only to tomorrow's budget allocation",
                        isSelected = selectedOption == com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW,
                        onSelect = {
                            selectedOption =
                                com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW
                            viewModel.onEvent(BudgetEvent.SetAutomaticRollover(enableRollover,
                                com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW
                            ))
                        },
                        enabled = enableRollover
                    )
                }
            }

            // Example card showing the effect
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "How it works",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val exampleText = when (selectedOption) {
                        com.example.expensetracker.event.RolloverOption.REALLOCATE ->
                            "If you don't spend Ksh.200 today with 10 days left in your budget period, Ksh.20 will be added to each remaining day."
                        com.example.expensetracker.event.RolloverOption.SAVE ->
                            "If you don't spend Ksh.200 today, it will be added to your savings total without changing your daily budget."
                        com.example.expensetracker.event.RolloverOption.ADD_TO_TOMORROW ->
                            "If you don't spend Ksh.200 today, tomorrow's budget will increase by Ksh.200."
                        else -> ""
                    }

                    Text(exampleText)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.onEvent(BudgetEvent.CheckForDayEnd) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Process Today's Budget Now")
                    }
                }
            }
        }
    }
}

@Composable
fun RolloverOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            enabled = enabled
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}