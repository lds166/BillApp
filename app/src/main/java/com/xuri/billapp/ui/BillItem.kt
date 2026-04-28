package com.xuri.billapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.data.Bill
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.theme.TextPrimary
import com.xuri.billapp.ui.theme.TextSecondary
import com.xuri.billapp.ui.theme.getCategoryColorByIndex

/**
 * 账单列表项
 * 左侧圆形分类图标 + 中间分类名+备注 + 右侧金额+时间
 * 支持长按进入批量删除模式
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun BillItem(
    bill: Bill,
    category: Category?,
    timeString: String,
    amountString: String,
    batchMode: Boolean = false,
    isSelected: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    // 查找分类索引用于颜色映射
    val presetNames = listOf("餐饮", "购物", "交通", "娱乐", "居住", "医疗", "教育", "其他")
    val categoryIndex = category?.let { presetNames.indexOf(it.name) }?.takeIf { it >= 0 } ?: 7
    val categoryColor = getCategoryColorByIndex(categoryIndex)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 批量选择 checkbox
        if (batchMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 分类图标 - 圆形背景 + 首字
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category?.name?.firstOrNull()?.toString() ?: "?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 中间信息
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = category?.name ?: "未知",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (bill.note.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = bill.note,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 金额
        Text(
            text = "-$amountString",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/**
 * 批量删除动画包装器
 * 为要删除的列表项提供滑出消失效果
 */
@Composable
fun BillItemWithDeleteAnimation(
    bill: Bill,
    category: Category?,
    timeString: String,
    amountString: String,
    batchMode: Boolean = false,
    isSelected: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(200)) +
                shrinkVertically(animationSpec = tween(200))
    ) {
        BillItem(
            bill = bill,
            category = category,
            timeString = timeString,
            amountString = amountString,
            batchMode = batchMode,
            isSelected = isSelected,
            onEdit = onEdit,
            onDelete = {
                isVisible = false
                // 直接调用删除，动画由 AnimatedVisibility 自动处理
                onDelete()
            },
            onToggleSelect = onToggleSelect,
            onLongPress = onLongPress
        )
    }
}
