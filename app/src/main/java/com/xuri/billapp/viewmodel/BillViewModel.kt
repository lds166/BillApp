package com.xuri.billapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.BillDatabase
import com.xuri.billapp.data.BillRepository
import com.xuri.billapp.data.BudgetManager
import com.xuri.billapp.data.Category
import com.xuri.billapp.data.CategoryPreset
import com.xuri.billapp.data.CategoryType
import com.xuri.billapp.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BillViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BillRepository
    private val budgetManager: BudgetManager

    // 支出分类列表
    val expenseCategories: StateFlow<List<Category>> = MutableStateFlow<List<Category>>(emptyList()).also { flow ->
        viewModelScope.launch {
            BillDatabase.getDatabase(application).categoryDao()
                .getCategoriesByType(CategoryType.EXPENSE)
                .collect { newValue -> flow.value = newValue }
        }
    }

    // 初始化：若数据库无分类则插入预置数据
    init {
        val db = BillDatabase.getDatabase(application)
        repository = BillRepository(db.billDao(), db.categoryDao())
        budgetManager = BudgetManager(application)

        viewModelScope.launch {
            val existing = repository.getExpenseCategories().first()
            if (existing.isEmpty()) {
                repository.insertCategories(CategoryPreset.DEFAULT_EXPENSE_CATEGORIES)
            }
        }
    }

    // 今日账单列表
    val todayBills: StateFlow<List<Bill>> = MutableStateFlow<List<Bill>>(emptyList()).also { flow ->
        viewModelScope.launch {
            val range = getTodayRange()
            repository.getBillsByDateRange(range.first, range.second)
                .collect { newValue -> flow.value = newValue }
        }
    }

    // 今日总消费
    val todayTotal: StateFlow<Double> = MutableStateFlow(0.0).also { flow ->
        viewModelScope.launch {
            val range = getTodayRange()
            repository.getTotalByDateRange(range.first, range.second)
                .collect { newValue -> flow.value = newValue ?: 0.0 }
        }
    }

    // 昨日总消费
    val yesterdayTotal: StateFlow<Double> = MutableStateFlow(0.0).also { flow ->
        viewModelScope.launch {
            val range = getYesterdayRange()
            repository.getTotalByDateRange(range.first, range.second)
                .collect { newValue -> flow.value = newValue ?: 0.0 }
        }
    }

    // 每日预算
    val dailyBudget: StateFlow<Double> = MutableStateFlow(0.0).also { flow ->
        viewModelScope.launch {
            budgetManager.dailyBudget.collect { newValue -> flow.value = newValue }
        }
    }

    // 今日与昨日消费对比百分比
    val comparisonPercentage: StateFlow<Double?> = combine(todayTotal, yesterdayTotal) { today, yesterday ->
        if (yesterday > 0) {
            ((today - yesterday) / yesterday) * 100
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 统计触发器：当账单变化时更新，通知统计页刷新数据
    private val _statsRefreshTrigger = MutableStateFlow(0L)
    val statsRefreshTrigger: StateFlow<Long> = _statsRefreshTrigger

    // 记录是否已发送过 80% 和 100% 通知，避免重复发送
    private var notified80Percent = false
    private var notified100Percent = false

    // 今日总消费（用于通知触发）
    private val todayTotalForNotification: StateFlow<Double> = MutableStateFlow(0.0).also { flow ->
        viewModelScope.launch {
            val range = getTodayRange()
            repository.getTotalByDateRange(range.first, range.second)
                .collect { newValue ->
                    val total = newValue ?: 0.0
                    flow.value = total
                    // 检查预算预警
                    checkBudgetNotification(total)
                }
        }
    }

    // 检查预算并发出通知
    private fun checkBudgetNotification(todayTotal: Double) {
        val budget = dailyBudget.value
        if (budget <= 0) return

        val ratio = todayTotal / budget
        val context = getApplication<Application>()

        if (ratio >= 1.0 && !notified100Percent) {
            // 超过 100%：发送超支通知
            NotificationHelper.sendBudgetExceededNotification(context, todayTotal, budget)
            notified100Percent = true
            notified80Percent = true // 超支意味着 80% 通知也已发送过
        } else if (ratio >= 0.8 && !notified80Percent) {
            // 超过 80%：发送预警通知
            NotificationHelper.sendBudget80PercentNotification(context, todayTotal, budget)
            notified80Percent = true
        }

        // 如果消费回到阈值以下，重置通知标记
        if (ratio < 0.8) {
            notified80Percent = false
            notified100Percent = false
        } else if (ratio < 1.0) {
            notified100Percent = false
        }
    }

    // 新增账单
    fun addBill(amount: Double, categoryId: Long, note: String, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val bill = Bill(
                amount = amount,
                categoryId = categoryId,
                note = note,
                date = date,
                createdAt = System.currentTimeMillis()
            )
            repository.insertBill(bill)
            _statsRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    // 更新账单
    fun updateBill(oldBill: Bill, amount: Double, categoryId: Long, note: String) {
        viewModelScope.launch {
            val newBill = oldBill.copy(
                amount = amount,
                categoryId = categoryId,
                note = note
            )
            repository.updateBill(newBill)
            _statsRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    // 删除账单
    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            _statsRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    // 设置每日预算
    fun setBudget(amount: Double) {
        viewModelScope.launch {
            budgetManager.setBudget(amount)
        }
    }

    // 清空所有数据
    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllBills()
            budgetManager.clearBudget()
        }
    }

    // 获取今日时间范围
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    // 获取昨日时间范围
    private fun getYesterdayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return Pair(start, end)
    }

    // 格式化时间为 HH:mm
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // 格式化金额为保留两位小数
    fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }

    // 获取指定日期范围的账单列表（一次性查询，用于统计）
    suspend fun getBillsInRange(startTime: Long, endTime: Long): List<Bill> {
        return repository.getBillsByDateRange(startTime, endTime).first()
    }

    // 获取指定日期范围的各分类消费总额（用于饼图）
    suspend fun getCategoryTotalsInRange(startTime: Long, endTime: Long): Map<Long, Double> {
        val bills = getBillsInRange(startTime, endTime)
        return bills.groupBy { it.categoryId }.mapValues { (_, billList) ->
            billList.sumOf { it.amount }
        }
    }

    // 获取近 N 天的每日消费总额（用于折线图）
    suspend fun getDailyTotals(days: Int): List<Pair<Long, Double>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val start = calendar.timeInMillis

        val bills = getBillsInRange(start, end)
        // 按天分组
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val grouped = bills.groupBy { sdf.format(Date(it.date)) }
        val result = mutableListOf<Pair<Long, Double>>()

        val cal = Calendar.getInstance()
        cal.timeInMillis = start
        for (i in 0 until days) {
            val dayKey = sdf.format(cal.time)
            val dayTotal = grouped[dayKey]?.sumOf { it.amount } ?: 0.0
            result.add(Pair(cal.timeInMillis, dayTotal))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    // 获取指定日期的账单列表（用于日历点击）
    suspend fun getBillsByDate(dateTimestamp: Long): List<Bill> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return getBillsInRange(start, end)
    }

    // 获取指定月份每日消费总额（用于日历）
    suspend fun getMonthDailyTotals(year: Int, month: Int): List<Pair<Int, Double>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        // 找到该月最后一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis

        val bills = getBillsInRange(start, end)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val grouped = bills.groupBy { sdf.format(Date(it.date)) }.mapValues { (_, billList) ->
            billList.sumOf { it.amount }
        }

        val result = mutableListOf<Pair<Int, Double>>()
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dayKey = sdf.format(cal.time)
            result.add(Pair(day, grouped[dayKey] ?: 0.0))
        }
        return result
    }
}
