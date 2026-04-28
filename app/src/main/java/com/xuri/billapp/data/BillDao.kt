package com.xuri.billapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills ORDER BY date DESC")
    suspend fun getAllBillsOnce(): List<Bill>

    @Query("SELECT * FROM bills WHERE date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getBillsByDateRange(startTime: Long, endTime: Long): Flow<List<Bill>>

    @Query("SELECT SUM(amount) FROM bills WHERE date BETWEEN :startTime AND :endTime")
    fun getTotalByDateRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE date BETWEEN :startTime AND :endTime")
    suspend fun getTotalByDateRangeOnce(startTime: Long, endTime: Long): Double?

    @Insert
    suspend fun insert(bill: Bill): Long

    @Delete
    suspend fun delete(bill: Bill)

    @Update
    suspend fun update(bill: Bill)

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): Bill?

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
