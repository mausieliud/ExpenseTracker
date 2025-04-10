package com.example.expensetracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
    var allowOverflow by remember { mutableStateOf(state.allowOverflow) }
    var allowUnderflow by remember { mutableStateOf(state.allowUnderflow) }
    var maxOverflowPercentage by remember { mutableStateOf(state.maxOverflowPercentage) }
    var maxUnderflowPercentage by remember { mutableStateOf(state.maxUnderflowPercentage) }

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
            // Main switch to enable/disable automatic rollover
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Automatic Budget Rollover",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Automatically redistribute unspent budget or overspending across remaining days",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Automatic Rollover")
                        Switch(
                            checked = enableRollover,
                            onCheckedChange = { isEnabled ->
                                enableRollover = isEnabled
                                viewModel.onEvent(BudgetEvent.ConfigureRolloverSettings(
                                    allowOverflow = allowOverflow,
                                    allowUnderflow = allowUnderflow,
                                    maxOverflowPercentage = maxOverflowPercentage,
                                    maxUnderflowPercentage = maxUnderflowPercentage,
                                    isEnabled = isEnabled  // Pass the enabled state to the event
                                ))
                            }
                        )
                    }
                }
            }

            // Overflow settings
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Budget Overflow Settings",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "What happens when you don't spend your daily budget",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Budget Overflow")
                        Switch(
                            checked = allowOverflow,
                            onCheckedChange = {
                                allowOverflow = it
                                viewModel.onEvent(
                                    BudgetEvent.ConfigureRolloverSettings(
                                        allowOverflow = it,
                                        allowUnderflow = allowUnderflow,
                                        maxOverflowPercentage = maxOverflowPercentage,
                                        maxUnderflowPercentage = maxUnderflowPercentage
                                    )
                                )
                            },
                            enabled = enableRollover
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Maximum Overflow Percentage: ${maxOverflowPercentage.toInt()}%")
                    Slider(
                        value = maxOverflowPercentage.toFloat(),
                        onValueChange = {
                            maxOverflowPercentage = it.toDouble()
                            viewModel.onEvent(
                                BudgetEvent.ConfigureRolloverSettings(
                                    allowOverflow = allowOverflow,
                                    allowUnderflow = allowUnderflow,
                                    maxOverflowPercentage = maxOverflowPercentage,
                                    maxUnderflowPercentage = maxUnderflowPercentage
                                )
                            )
                        },
                        valueRange = 0f..100f,
                        steps = 10,
                        enabled = enableRollover && allowOverflow
                    )

                    Text(
                        "This controls what percentage of your daily budget can be carried over to future days",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Underflow settings
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Budget Underflow Settings",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "What happens when you spend more than your daily budget",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allow Budget Underflow")
                        Switch(
                            checked = allowUnderflow,
                            onCheckedChange = {
                                allowUnderflow = it
                                viewModel.onEvent(
                                    BudgetEvent.ConfigureRolloverSettings(
                                        allowOverflow = allowOverflow,
                                        allowUnderflow = it,
                                        maxOverflowPercentage = maxOverflowPercentage,
                                        maxUnderflowPercentage = maxUnderflowPercentage
                                    )
                                )
                            },
                            enabled = enableRollover
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Maximum Underflow Percentage: ${maxUnderflowPercentage.toInt()}%")
                    Slider(
                        value = maxUnderflowPercentage.toFloat(),
                        onValueChange = {
                            maxUnderflowPercentage = it.toDouble()
                            viewModel.onEvent(
                                BudgetEvent.ConfigureRolloverSettings(
                                    allowOverflow = allowOverflow,
                                    allowUnderflow = allowUnderflow,
                                    maxOverflowPercentage = maxOverflowPercentage,
                                    maxUnderflowPercentage = maxUnderflowPercentage
                                )
                            )
                        },
                        valueRange = 0f..100f,
                        steps = 10,
                        enabled = enableRollover && allowUnderflow
                    )

                    Text(
                        "This controls what percentage of your daily budget you can 'borrow' from future days",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}