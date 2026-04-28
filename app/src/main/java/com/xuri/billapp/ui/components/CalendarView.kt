package com.xuri.billapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/**
 * 日历视图组件
 * 展示月历，每日格子根据消费金额显示不同深浅的颜色
 *
 * @param year 当前年份
 * @param month 当前月份（1-12）
 * @param dailyTotals 每日消费总额列表 (dayOfMonth, amount)
 * @param onMonthChange 切换月份的回调
 * @param onDateClick 点击日期的回调
 */
@Composable
fun CalendarView(
    year: Int,
    month: Int,
    dailyTotals: Map<Int, Double>,
    onMonthChange: (newYear: Int, newMonth: Int) -> Unit,
    onDateClick: (day: Int) -> Unit
) {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = 周日, 2 = 周一...
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // 找出最大消费额用于颜色映射
    val maxAmount = dailyTotals.values.maxOrNull() ?: 0.0

    // 计算日历前面需要填充的空格数（周一为第一天）
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 月份导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val newMonth = if (month == 1) 12 else month - 1
                val newYear = if (month == 1) year - 1 else year
                onMonthChange(newYear, newMonth)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月")
            }
            Text(
                text = "${year}年${month}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                val newMonth = if (month == 12) 1 else month + 1
                val newYear = if (month == 12) year + 1 else year
                onMonthChange(newYear, newMonth)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月")
            }
        }

        // 星期标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 日历格子
        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month - 1
        val todayDate = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

        // 计算总行数
        val totalCells = offset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                for (col in 0..6) {
                    val dayIndex = row * 7 + col
                    val day = dayIndex - offset + 1

                    if (dayIndex >= offset && day <= daysInMonth) {
                        val amount = dailyTotals[day] ?: 0.0
                        val isToday = day == todayDate

                        CalendarCell(
                            day = day,
                            amount = amount,
                            maxAmount = maxAmount,
                            isToday = isToday,
                            onClick = { onDateClick(day) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 空白格子
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 日历单元格组件
 * 根据消费金额显示不同深浅的背景色
 */
@Composable
private fun CalendarCell(
    day: Int,
    amount: Double,
    maxAmount: Double,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据消费金额计算颜色深度
    val bgColor = if (amount > 0 && maxAmount > 0) {
        val ratio = (amount / maxAmount).coerceIn(0.0, 1.0)
        Color.Green.copy(alpha = 0.1f + ratio.toFloat() * 0.5f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clickable(onClick = onClick)
            .background(
                color = bgColor,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = day.toString(),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            if (amount > 0) {
                Text(
                    text = String.format("%.0f", amount),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
