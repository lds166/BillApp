package com.xuri.billapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.theme.Green500
import com.xuri.billapp.ui.theme.TextPrimary
import com.xuri.billapp.ui.theme.TextSecondary
import com.xuri.billapp.ui.theme.getCategoryColorByIndex
import kotlinx.coroutines.launch

/**
 * 记账底部弹窗
 * 圆角16dp，上滑展开。包含金额输入、分类选择、备注输入。
 * 支持预填分类（从快捷入口传入 categoryId）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillBottomSheet(
    categories: List<Category>,
    initialCategoryId: Long? = null,
    initialAmount: String = "",
    initialNote: String = "",
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: Long, note: String) -> Unit
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var selectedCategoryId by remember { mutableStateOf(initialCategoryId) }
    var note by remember { mutableStateOf(initialNote) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
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
                    text = "记一笔",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 金额输入 - 大号
            Text(
                text = "金额",
                fontSize = 12.sp,
                color = TextSecondary,
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
                placeholder = { Text("0.00", fontSize = 28.sp, color = TextSecondary.copy(alpha = 0.5f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("¥ ", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green500,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedPrefixColor = TextPrimary,
                    unfocusedPrefixColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 分类选择
            Text(
                text = "选择分类",
                fontSize = 12.sp,
                color = TextSecondary,
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
                    CategoryChip(
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
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("添加备注...", color = TextSecondary.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green500,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && selectedCategoryId != null) {
                        onSave(amountValue, selectedCategoryId!!, note)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && selectedCategoryId != null,
                colors = ButtonDefaults.buttonColors(containerColor = Green500),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 分类选择项
 */
@Composable
private fun CategoryChip(
    category: Category,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
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
