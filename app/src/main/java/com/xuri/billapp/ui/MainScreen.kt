package com.xuri.billapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.theme.Green500
import com.xuri.billapp.ui.theme.TextPrimary
import com.xuri.billapp.ui.theme.TextSecondary
import com.xuri.billapp.ui.theme.getCategoryColorByIndex
import com.xuri.billapp.viewmodel.BillViewModel
import kotlinx.coroutines.launch

/**
 * 底部导航 Tab 密封类
 */
sealed class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : AppTab("记账", Icons.Default.Home)
    object Stats : AppTab("统计", Icons.Default.Star)
    object Calendar : AppTab("日历", Icons.Default.DateRange)
    object Mine : AppTab("我的", Icons.Default.AccountCircle)

    companion object {
        fun allTabs(): List<AppTab> = listOf(Home, Stats, Calendar, Mine)
    }
}

/**
 * 主屏幕组件
 * 包含底部 4 个 Tab 导航
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: BillViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf<AppTab>(AppTab.Home) }
    var showAddSheet by remember { mutableStateOf(false) }
    var addSheetCategoryId by remember { mutableStateOf<Long?>(null) }
    var showEditSheet by remember { mutableStateOf<Bill?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val todayBills by viewModel.todayBills.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val comparisonPercentage by viewModel.comparisonPercentage.collectAsState()
    val dailyBudget by viewModel.dailyBudget.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTab.title,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.allTabs().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            AppTab.Home -> {
                BillPage(
                    modifier = Modifier.padding(padding),
                    todayTotal = todayTotal,
                    comparisonPercentage = comparisonPercentage,
                    dailyBudget = dailyBudget,
                    todayBills = todayBills,
                    categories = categories,
                    viewModel = viewModel,
                    onEditBill = { bill -> showEditSheet = bill },
                    onQuickAdd = { categoryId ->
                        addSheetCategoryId = categoryId
                        showAddSheet = true
                    }
                )
            }
            AppTab.Stats -> {
                StatsPage(
                    modifier = Modifier.padding(padding),
                    categories = categories,
                    viewModel = viewModel
                )
            }
            AppTab.Calendar -> {
                CalendarPage(
                    modifier = Modifier.padding(padding),
                    categories = categories,
                    viewModel = viewModel
                )
            }
            AppTab.Mine -> {
                MinePage(
                    modifier = Modifier.padding(padding),
                    dailyBudget = dailyBudget,
                    viewModel = viewModel,
                    onClearData = { showClearConfirm = true }
                )
            }
        }
    }

    // 新增账单底部弹窗
    if (showAddSheet) {
        AddBillBottomSheet(
            categories = categories,
            initialCategoryId = addSheetCategoryId,
            onDismiss = {
                showAddSheet = false
                addSheetCategoryId = null
            },
            onSave = { amount, categoryId, note ->
                viewModel.addBill(amount, categoryId, note)
                showAddSheet = false
                addSheetCategoryId = null
            }
        )
    }

    // 编辑账单底部弹窗
    if (showEditSheet != null) {
        EditBillBottomSheet(
            bill = showEditSheet!!,
            categories = categories,
            onDismiss = { showEditSheet = null },
            onSave = { amount, categoryId, note ->
                showEditSheet?.let { viewModel.updateBill(it, amount, categoryId, note) }
                showEditSheet = null
            }
        )
    }

    // 清空数据确认对话框
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空数据") },
            text = { Text("确定要清空所有账单数据吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllData()
                    showClearConfirm = false
                }) {
                    Text("确定", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 编辑账单底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillBottomSheet(
    bill: Bill,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: Long, note: String) -> Unit
) {
    var amount by remember { mutableStateOf(String.format("%.2f", bill.amount)) }
    var selectedCategoryId by remember { mutableStateOf(bill.categoryId) }
    var note by remember { mutableStateOf(bill.note) }

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "编辑账单",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = com.xuri.billapp.ui.theme.TextPrimary
                )
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    androidx.compose.material3.Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = com.xuri.billapp.ui.theme.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 金额输入
            Text(
                text = "金额",
                fontSize = 12.sp,
                color = com.xuri.billapp.ui.theme.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amount = newValue
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("¥ ", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 分类选择
            Text(
                text = "选择分类",
                fontSize = 12.sp,
                color = com.xuri.billapp.ui.theme.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(categories) { index, category ->
                    EditCategoryItem(
                        category = category,
                        color = getCategoryColorByIndex(index),
                        isSelected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 备注
            Text(
                text = "备注（可选）",
                fontSize = 12.sp,
                color = com.xuri.billapp.ui.theme.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("添加备注...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0) {
                        onSave(amountValue, selectedCategoryId, note)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.xuri.billapp.ui.theme.Green500
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 编辑弹窗中的分类选择项
 */
@Composable
private fun EditCategoryItem(
    category: Category,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) color else color.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category.icon),
                contentDescription = category.name,
                tint = if (isSelected) Color.White else color,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            fontSize = 11.sp,
            color = if (isSelected) color else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 我的页面组件
 */
@Composable
fun MinePage(
    modifier: Modifier = Modifier,
    dailyBudget: Double,
    viewModel: BillViewModel,
    onClearData: () -> Unit
) {
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 预算设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                budgetInput = if (dailyBudget > 0) String.format("%.2f", dailyBudget) else ""
                showBudgetDialog = true
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "每日预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (dailyBudget > 0) "¥ ${String.format("%.2f", dailyBudget)}" else "未设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 关于我们卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "关于我们",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "随手记 v1.0\n一款简洁的记账工具",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 清空数据卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onClick = { onClearData() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "清空所有数据",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "删除所有账单记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 预算设置对话框
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("设置每日预算") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            budgetInput = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入预算金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("¥ ") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = budgetInput.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        viewModel.setBudget(amount)
                    } else if (budgetInput.isEmpty()) {
                        viewModel.setBudget(0.0)
                    }
                    showBudgetDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (budgetInput.isEmpty()) {
                        viewModel.setBudget(0.0)
                    }
                    showBudgetDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }
}
