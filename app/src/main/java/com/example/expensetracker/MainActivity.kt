package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.screens.BudgetOverviewScreen
import com.example.expensetracker.screens.BudgetSetupScreen
import com.example.expensetracker.screens.ExpenseFormScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.viewmodel.BudgetFormViewModel
import com.example.expensetracker.ui.viewmodel.BudgetViewModel
import com.example.expensetracker.ui.viewmodel.ExpenseFormViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme  {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "overview") {
                        composable("overview") {
                            val viewModel: BudgetViewModel = viewModel(
                                factory = BudgetViewModel.Factory(application)
                            )
                            BudgetOverviewScreen(
                                viewModel = viewModel,
                                navigateToAddExpense = { navController.navigate("add_expense") },
                                navigateToBudgetSetup = { navController.navigate("budget_setup") }
                            )
                        }

                        composable("add_expense") {
                            val viewModel: ExpenseFormViewModel = viewModel(
                                factory = ExpenseFormViewModel.Factory(application)
                            )
                            ExpenseFormScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("budget_setup") {
                            val viewModel: BudgetFormViewModel = viewModel(
                                factory = BudgetFormViewModel.Factory(application)
                            )
                            BudgetSetupScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}