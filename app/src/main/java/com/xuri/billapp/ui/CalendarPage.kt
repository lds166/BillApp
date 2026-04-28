package com.xuri.billapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.components.CalendarView
import com.xuri.billapp.viewmodel.BillViewModel
import java.util.Calendar

/**
 * 日历视图页组件
 * 展示月历消费视图，点击日期可查看详情
 */
@Composable
fun CalendarPage(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    viewModel: BillViewModel
) {
    val today = Calendar.getInstance()
    var selectedYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH) + 1) }
    var dailyTotals by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectedDayBills by remember { mutableStateOf<List<Bill>>(emptyList()) }

    // 加载月份数据
    LaunchedEffect(selectedYear, selectedMonth) {
        val totals = viewModel.getMonthDailyTotals(selectedYear, selectedMonth)
        dailyTotals = totals.associate { it.first to it.second }
        selectedDay = null
        selectedDayBills = emptyList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 日历组件
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    CalendarView(
                        year = selectedYear,
                        month = selectedMonth,
                        dailyTotals = dailyTotals,
                        onMonthChange = { newYear, newMonth ->
                            selectedYear = newYear
                            selectedMonth = newMonth
                        },
                        onDateClick = { day ->
                            selectedDay = day
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 选中日期的账单列表
        item {
            if (selectedDay != null) {
                LaunchedEffect(selectedDay) {
                    val calendar = Calendar.getInstance()
                    calendar.set(selectedYear, selectedMonth - 1, selectedDay!!)
                    selectedDayBills = viewModel.getBillsByDate(calendar.timeInMillis)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val dayText = selectedYear.toString() + "年" + selectedMonth.toString() + "月" + selectedDay.toString() + "日"
                        Text(
                            text = dayText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (selectedDayBills.isEmpty()) {
                            Text(
                                text = "当天无消费记录",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            val dayTotal = selectedDayBills.sumOf { it.amount }
                            val totalText = "合计：¥" + String.format("%.2f", dayTotal)
                            Text(
                                text = totalText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 账单列表
        if (selectedDay != null) {
            items(selectedDayBills) { bill ->
                val category = categories.find { it.id == bill.categoryId }
                BillItem(
                    bill = bill,
                    category = category,
                    timeString = viewModel.formatDate(bill.date),
                    amountString = viewModel.formatAmount(bill.amount),
                    onDelete = { viewModel.deleteBill(bill) }
                )
            }
        }

        // 未选中日期时的提示
        if (selectedDay == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击日历中的日期查看当天账单",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
