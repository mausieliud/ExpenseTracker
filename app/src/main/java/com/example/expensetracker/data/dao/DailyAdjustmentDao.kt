package com.example.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expensetracker.data.entity.DailyAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyAdjustmentDao {
    @Query("SELECT * FROM daily_adjustments")
    fun getAllAdjustments(): Flow<List<DailyAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: DailyAdjustment)

    @Query("DELETE FROM daily_adjustments")
    suspend fun deleteAllAdjustments()
}