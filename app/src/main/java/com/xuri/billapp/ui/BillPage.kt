package com.xuri.billapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.theme.Green500
import com.xuri.billapp.ui.theme.Red500
import com.xuri.billapp.ui.theme.TextPrimary
import com.xuri.billapp.ui.theme.TextSecondary
import com.xuri.billapp.viewmodel.BillViewModel

/**
 * 记账页 - 三段式布局
 * 1. 顶部今日消费卡片
 * 2. 中部快捷记账入口
 * 3. 底部今日账单列表
 */
@Composable
fun BillPage(
    modifier: Modifier = Modifier,
    todayTotal: Double,
    comparisonPercentage: Double?,
    dailyBudget: Double,
    todayBills: List<Bill>,
    categories: List<Category>,
    viewModel: BillViewModel,
    onEditBill: (Bill) -> Unit,
    onQuickAdd: (Long) -> Unit
) {
    // 批量删除模式状态
    var batchMode by remember { mutableStateOf(false) }
    val selectedBills = remember { mutableStateOf<Set<Bill>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 批量删除顶部工具栏
        AnimatedVisibility(
            visible = batchMode,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    batchMode = false
                    selectedBills.value = emptySet()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "取消", tint = TextSecondary)
                }
                Text(
                    text = "已选 ${selectedBills.value.size} 项",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = { if (selectedBills.value.isNotEmpty()) showDeleteConfirm = true },
                    enabled = selectedBills.value.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除选中",
                        tint = if (selectedBills.value.isNotEmpty()) Red500 else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // 可滚动内容区
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部消费卡片
            item {
                StatsCard(
                    todayTotal = todayTotal,
                    comparisonPercentage = comparisonPercentage,
                    dailyBudget = dailyBudget,
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // 快捷记账入口
            item {
                QuickEntryBar(
                    categories = categories,
                    onCategoryClick = { categoryId -> onQuickAdd(categoryId) }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 分割线
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE2E8F0))
                        .padding(horizontal = 16.dp)
                )
            }

            // 账单列表标题行
            if (todayBills.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "今日账单",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "共 ${todayBills.size} 笔",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 账单列表
            itemsIndexed(todayBills) { _, bill ->
                val category = categories.find { it.id == bill.categoryId }
                BillItem(
                    bill = bill,
                    category = category,
                    timeString = viewModel.formatDate(bill.date),
                    amountString = viewModel.formatAmount(bill.amount),
                    batchMode = batchMode,
                    isSelected = selectedBills.value.contains(bill),
                    onEdit = { onEditBill(bill) },
                    onDelete = { viewModel.deleteBill(bill) },
                    onToggleSelect = {
                        selectedBills.value = if (selectedBills.value.contains(bill)) {
                            selectedBills.value - bill
                        } else {
                            selectedBills.value + bill
                        }
                    },
                    onLongPress = { batchMode = true }
                )
            }

            // 空状态
            if (todayBills.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    color = Green500.copy(alpha = 0.08f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Green500.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "还没有账单",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击上方分类快捷开始记录吧",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // 批量删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除账单") },
            text = { Text("确定要删除选中的 ${selectedBills.value.size} 笔账单吗？") },
            confirmButton = {
                TextButton(onClick = {
                    selectedBills.value.forEach { viewModel.deleteBill(it) }
                    selectedBills.value = emptySet()
                    batchMode = false
                    showDeleteConfirm = false
                }) {
                    Text("确定", color = Red500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
