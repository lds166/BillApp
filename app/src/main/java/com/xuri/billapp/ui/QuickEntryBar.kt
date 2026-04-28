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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.data.Category
import com.xuri.billapp.ui.theme.TextSecondary
import com.xuri.billapp.ui.theme.getCategoryColorByIndex

/**
 * 快捷记账入口
 * 水平滚动展示常用分类，点击直接进入记账弹窗
 */
@Composable
fun QuickEntryBar(
    categories: List<Category>,
    onCategoryClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "快捷记账",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = com.xuri.billapp.ui.theme.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(categories.take(6)) { index, category ->
                QuickEntryItem(
                    category = category,
                    color = getCategoryColorByIndex(index),
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 单个快捷入口项
 */
@Composable
private fun QuickEntryItem(
    category: Category,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = color.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getCategoryIcon(category.icon),
                contentDescription = category.name,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 根据分类图标名称映射到对应的 Material 图标
 */
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_car" -> Icons.Default.DirectionsCar
        "theater_comedy" -> Icons.Default.TheaterComedy
        "home" -> Icons.Default.Home
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "more_horiz" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Home
    }
}

/**
 * 根据分类名称映射到对应的颜色（保留向后兼容）
 */
fun getCategoryColor(categoryName: String): Color {
    val index = when (categoryName) {
        "餐饮" -> 0
        "购物" -> 1
        "交通" -> 2
        "娱乐" -> 3
        "居住" -> 4
        "医疗" -> 5
        "教育" -> 6
        else -> 7
    }
    return getCategoryColorByIndex(index)
}
