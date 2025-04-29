package com.example.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.screens.BudgetOverviewScreen
import com.example.expensetracker.screens.BudgetSetupScreen
import com.example.expensetracker.screens.ExpenseFormScreen
import com.example.expensetracker.screens.ReportScreen
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.viewmodel.BudgetFormViewModel
import com.example.expensetracker.ui.viewmodel.BudgetViewModel
import com.example.expensetracker.ui.viewmodel.ExpenseFormViewModel
import com.example.expensetracker.ui.viewmodel.ReportViewModel
import com.example.expensetracker.mpesa.screen.MPesaMessageParserScreen

/**
 * Also functions as nav controller
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ExpenseTracker"
    }

    // Track SMS permission state
    private val hasSmsPermission = mutableStateOf(false)

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission.value = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for SMS permission
        hasSmsPermission.value = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        // Request SMS permission if not granted
        if (!hasSmsPermission.value) {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Create a single shared instance of ExpenseFormViewModel
                    val sharedExpenseViewModel: ExpenseFormViewModel = viewModel(
                        factory = ExpenseFormViewModel.Factory(application)
                    )

                    NavHost(navController = navController, startDestination = "overview") {
                        composable("overview") {
                            val viewModel: BudgetViewModel = viewModel(
                                factory = BudgetViewModel.Factory(application)
                            )
                            BudgetOverviewScreen(
                                viewModel = viewModel,
                                navigateToAddExpense = { navController.navigate("add_expense") },
                                navigateToBudgetSetup = { navController.navigate("budget_setup") },
                                navigateToReports = { navController.navigate("reports") },
                                navigateToMPesaParser = { navController.navigate("mpesa_parser") }
                            )
                        }

                        composable("add_expense") {
                            // Use the shared ViewModel instance instead of creating a new one
                            ExpenseFormScreen(
                                viewModel = sharedExpenseViewModel,
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

                        composable("reports") {
                            val viewModel: ReportViewModel = viewModel(
                                factory = ReportViewModel.Factory(application)
                            )
                            ReportScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // Use the same shared ViewModel for M-Pesa parser(expense view model)
                        composable("mpesa_parser") {
                            MPesaMessageParserScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToExpenseForm = { navController.navigate("add_expense") },
                                expenseViewModel = sharedExpenseViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}