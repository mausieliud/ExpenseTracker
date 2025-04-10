package com.example.expensetracker.data.database

import com.example.expensetracker.data.dao.BudgetDao
import com.example.expensetracker.data.dao.DailyAdjustmentDao
import com.example.expensetracker.data.dao.ExpenseDao
import com.example.expensetracker.data.entity.Budget
import com.example.expensetracker.data.entity.DailyAdjustment
import com.example.expensetracker.data.entity.Expense


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [Expense::class, Budget::class, DailyAdjustment::class],
    version = 1,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun dailyAdjustmentDao(): DailyAdjustmentDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "BudgetDB"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

    // Repository class for database operations
    class Repository(private val database: BudgetDatabase) {
        // Expose DAOs or create methods that use them
        private val expenseDao = database.expenseDao()
        private val budgetDao = database.budgetDao()
        private val dailyAdjustmentDao = database.dailyAdjustmentDao()

        // Reset all data
        suspend fun resetAllData() {
            expenseDao.deleteAllExpenses()
            budgetDao.deleteBudget()
            dailyAdjustmentDao.deleteAllAdjustments()
        }
    }
}