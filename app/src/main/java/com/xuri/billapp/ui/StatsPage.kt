package com.xuri.billapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.components.ChartPoint
import com.xuri.billapp.ui.components.LineChart
import com.xuri.billapp.ui.components.PieChart
import com.xuri.billapp.ui.components.PieChartData
import com.xuri.billapp.ui.components.PieChartLegend
import com.xuri.billapp.ui.components.toChartPoints
import com.xuri.billapp.viewmodel.BillViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 统计页组件
 * 包含：消费趋势折线图 + 分类占比饼图 + 周期切换
 *
 * @param modifier 修饰符
 * @param categories 分类列表
 * @param viewModel ViewModel
 */
@Composable
fun StatsPage(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    viewModel: BillViewModel
) {
    // 监听刷新触发器，当账单变化时自动刷新数据
    val statsTrigger by viewModel.statsRefreshTrigger.collectAsState()

    var period by remember { mutableStateOf(StatsPeriod.WEEK) }
    var lineChartData by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    var pieChartData by remember { mutableStateOf<List<PieChartData>>(emptyList()) }
    var selectedCategoryBills by remember { mutableStateOf<List<Bill>>(emptyList()) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    // 加载统计数据（period 变化或账单变化时刷新）
    LaunchedEffect(period, statsTrigger) {
        val days = when (period) {
            StatsPeriod.WEEK -> 7
            StatsPeriod.MONTH -> 30
        }

        // 折线图数据：近 N 天每日消费
        val dailyData = viewModel.getDailyTotals(days)
        lineChartData = dailyData.toChartPoints()

        // 饼图数据：分类消费占比
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
        val start = calendar.timeInMillis

        val categoryTotals = viewModel.getCategoryTotalsInRange(start, end)
        pieChartData = categoryTotals.mapNotNull { (categoryId, total) ->
            val category = categories.find { it.id == categoryId }
            if (category != null && total > 0) {
                PieChartData(
                    label = category.name,
                    value = total,
                    color = getCategoryColor(category.name)
                )
            } else null
        }.sortedByDescending { it.value }

        // 清空分类明细
        selectedCategoryBills = emptyList()
        selectedCategoryName = null
    }

    // 点击图例显示分类明细
    val scope = rememberCoroutineScope()
    fun showCategoryDetail(categoryName: String) {
        val category = categories.find { it.name == categoryName }
        if (category != null) {
            val days = when (period) {
                StatsPeriod.WEEK -> 7
                StatsPeriod.MONTH -> 30
            }
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val end = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -(days - 1))
            val start = calendar.timeInMillis

            scope.launch {
                selectedCategoryBills = viewModel.getBillsInRange(start, end)
                    .filter { it.categoryId == category.id }
            }
            selectedCategoryName = categoryName
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 周期切换
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsPeriod.values().forEach { p ->
                        FilterChip(
                            selected = period == p,
                            onClick = { period = p },
                            label = { Text(p.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 折线图卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "消费趋势",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val totalAmount = lineChartData.sumOf { it.value }
                        if (totalAmount > 0) {
                            Text(
                                text = String.format("合计 %.2f 元", totalAmount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LineChart(
                        data = lineChartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 饼图卡片
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "分类占比",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (pieChartData.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无消费数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 饼图
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PieChart(
                                data = pieChartData
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 图例
                        PieChartLegend(
                            data = pieChartData,
                            onLegendClick = { item ->
                                showCategoryDetail(item.label)
                            }
                        )
                    }
                }
            }
        }

        // 分类明细
        if (selectedCategoryName != null && selectedCategoryBills.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategoryName + " 明细",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val catTotal = selectedCategoryBills.sumOf { it.amount }
                        Text(
                            text = "共 " + String.format("%.2f", catTotal) + " 元",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(selectedCategoryBills) { bill ->
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
    }
}

/**
 * 统计周期枚举
 */
enum class StatsPeriod(val label: String) {
    WEEK("本周"),
    MONTH("本月")
}
