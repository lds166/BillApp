package com.xuri.billapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BillRepository(
    private val billDao: BillDao,
    private val categoryDao: CategoryDao
) {
    // 账单相关操作
    fun getAllBills(): Flow<List<Bill>> = billDao.getAllBills()

    fun getBillsByDateRange(startTime: Long, endTime: Long): Flow<List<Bill>> =
        billDao.getBillsByDateRange(startTime, endTime)

    fun getTotalByDateRange(startTime: Long, endTime: Long): Flow<Double?> =
        billDao.getTotalByDateRange(startTime, endTime)

    suspend fun getTotalByDateRangeOnce(startTime: Long, endTime: Long): Double? =
        billDao.getTotalByDateRangeOnce(startTime, endTime)

    suspend fun insertBill(bill: Bill): Long = billDao.insert(bill)

    suspend fun deleteBill(bill: Bill) = billDao.delete(bill)

    suspend fun updateBill(bill: Bill) = billDao.update(bill)

    suspend fun getBillById(id: Long): Bill? = billDao.getBillById(id)

    suspend fun deleteAllBills() = billDao.deleteAllBills()

    // 分类相关操作
    fun getExpenseCategories(): Flow<List<Category>> =
        categoryDao.getCategoriesByType(CategoryType.EXPENSE)

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategories(categories: List<Category>) = categoryDao.insertAll(categories)
}
