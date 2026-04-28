package com.xuri.billapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuri.billapp.ui.theme.Green500
import com.xuri.billapp.ui.theme.Orange500
import com.xuri.billapp.ui.theme.Red50
import com.xuri.billapp.ui.theme.Red500
import com.xuri.billapp.ui.theme.TextPrimary
import com.xuri.billapp.ui.theme.TextSecondary

/**
 * 今日消费卡片
 * 白色背景圆角卡片，展示今日消费总额、昨日对比、预算进度条
 * 超支时顶部显示红色横幅
 */
@Composable
fun StatsCard(
    todayTotal: Double,
    comparisonPercentage: Double?,
    dailyBudget: Double,
    modifier: Modifier = Modifier
) {
    val budgetProgress = if (dailyBudget > 0) (todayTotal / dailyBudget).coerceIn(0.0, 1.0) else 0.0
    val budgetRatio = if (dailyBudget > 0) todayTotal / dailyBudget else 0.0
    val isOverBudget = budgetRatio > 1.0

    // 预算进度条颜色：绿色 → 橙色(80%) → 红色(100%)
    val progressColor = when {
        budgetRatio >= 1.0 -> Red500
        budgetRatio >= 0.8 -> Orange500
        else -> Green500
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 超支横幅
            AnimatedVisibility(
                visible = isOverBudget,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Red50, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠",
                        fontSize = 14.sp,
                        color = Red500
                    )
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text(
                        text = "已超支 ¥${String.format("%.2f", todayTotal - dailyBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Red500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 今日已消费标题
            Text(
                text = "今日消费",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 总金额 - 大号 36sp 半粗体
            Text(
                text = String.format("¥%.2f", todayTotal),
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 昨日对比
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comparisonPercentage != null) {
                    val isIncrease = comparisonPercentage > 0
                    Text(
                        text = if (isIncrease) "↑" else "↓",
                        fontSize = 14.sp,
                        color = if (isIncrease) Red500 else Green500
                    )
                    Text(
                        text = if (isIncrease)
                            "比昨日 +${String.format("%.1f", comparisonPercentage)}%"
                        else
                            "比昨日 ${String.format("%.1f", comparisonPercentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIncrease) Red500 else Green500,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "暂无昨日数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 预算进度条
            if (dailyBudget > 0) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预算 ¥${String.format("%.2f", dailyBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${String.format("%.0f", budgetProgress * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { budgetProgress.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = progressColor,
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
