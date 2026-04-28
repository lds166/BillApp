package com.xuri.billapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 饼图数据项
 * @param label 标签名称（如分类名）
 * @param value 数值（如消费金额）
 * @param color 扇区颜色
 */
data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * 使用 Canvas 手绘的简易饼图组件
 * 用于展示分类消费占比
 *
 * @param data 饼图数据列表
 * @param modifier 修饰符
 * @param onSliceClick 点击扇区的回调，返回被点击的数据项
 */
@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    onSliceClick: ((PieChartData) -> Unit)? = null
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val total = data.sumOf { it.value }
    if (total <= 0) {
        Box(
            modifier = modifier
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无消费",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    Canvas(
        modifier = modifier.size(200.dp)
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 10f

        var startAngle = -90f // 从顶部开始
        data.forEach { item ->
            val sweepAngle = (item.value / total).toFloat() * 360f
            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            // 画分隔线
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = androidx.compose.ui.geometry.Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 2f)
            )
            startAngle += sweepAngle
        }
    }
}

/**
 * 饼图图例组件
 * 显示各分类的名称、金额和占比
 */
@Composable
fun PieChartLegend(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    onLegendClick: ((PieChartData) -> Unit)? = null
) {
    val total = data.sumOf { it.value }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        data.forEach { item ->
            val percentage = if (total > 0) (item.value / total * 100) else 0.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { onLegendClick?.invoke(item) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 颜色圆点
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(item.color, CircleShape)
                    )
                    Text(
                        text = item.label,
                        modifier = Modifier.padding(start = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = String.format("%.2f 元  (%.1f%%)", item.value, percentage),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
