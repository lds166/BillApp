package com.xuri.billapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 折线图数据点
 * @param value 数值
 * @param label X 轴标签（如日期）
 */
data class ChartPoint(
    val value: Double,
    val label: String
)

/**
 * 使用 Canvas 手绘的消费趋势折线图
 * 功能：带网格线、Y 轴刻度、X 轴旋转日期标签、轴标题
 *
 * @param data 数据点列表
 * @param modifier 修饰符
 * @param lineColor 折线颜色
 * @param fillColor 填充颜色
 */
@Composable
fun LineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.1f)
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
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

    val maxValue = data.maxOfOrNull { it.value } ?: 0.0
    // 计算一个整洁的 Y 轴最大值（向上取整到整数）
    val yAxisMax = computeNiceMax(maxValue)
    // Y 轴刻度数量：5 个刻度（0, 25%, 50%, 75%, 100%）
    val yAxisTickCount = 5
    val yAxisStep = yAxisMax / (yAxisTickCount - 1)
    val yAxisValues = List(yAxisTickCount) { it * yAxisStep }
    val gridLineColor = Color(0xFFE2E8F0)
    val axisLabelColor = Color(0xFF94A3B8)
    val yAxisLabelWidth = 48.dp
    val xAxisLabelHeight = 24.dp

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧空白：Y 轴标签区域
            Column(
                modifier = Modifier
                    .width(yAxisLabelWidth)
                    .padding(end = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                yAxisValues.reversed().forEach { value ->
                    Text(
                        text = String.format("%.0f", value),
                        fontSize = 10.sp,
                        color = axisLabelColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // 图表 + X 轴标签区域
            Column(modifier = Modifier.weight(1f)) {
                // 折线图主体（包含网格线）
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val chartWidth = size.width
                    val chartHeight = size.height
                    val pointPadding = 8f // 左右边距

                    // 数据点坐标
                    val points = if (data.size == 1) {
                        listOf(
                            androidx.compose.ui.geometry.Offset(
                                chartWidth / 2,
                                chartHeight - (data[0].value / yAxisMax).toFloat() * chartHeight
                            )
                        )
                    } else {
                        data.mapIndexed { index, point ->
                            val x = pointPadding +
                                    (index.toFloat() / (data.size - 1)) * (chartWidth - pointPadding * 2)
                            val y = chartHeight - (point.value / yAxisMax).toFloat() * chartHeight
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                    }

                    // 画水平网格线（虚线）
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    for (i in 1 until yAxisTickCount) {
                        val y = (i.toFloat() / (yAxisTickCount - 1)) * chartHeight
                        drawLine(
                            color = gridLineColor,
                            start = androidx.compose.ui.geometry.Offset(pointPadding, y),
                            end = androidx.compose.ui.geometry.Offset(chartWidth - pointPadding, y),
                            strokeWidth = 1f,
                            pathEffect = dashPathEffect
                        )
                    }

                    // 画左侧 Y 轴实线
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = androidx.compose.ui.geometry.Offset(pointPadding, 0f),
                        end = androidx.compose.ui.geometry.Offset(pointPadding, chartHeight),
                        strokeWidth = 1.5f
                    )

                    // 画底部 X 轴实线
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = androidx.compose.ui.geometry.Offset(pointPadding, chartHeight),
                        end = androidx.compose.ui.geometry.Offset(chartWidth - pointPadding, chartHeight),
                        strokeWidth = 1.5f
                    )

                    if (data.size > 1) {
                        // 画填充区域
                        val fillPath = Path().apply {
                            moveTo(points[0].x, chartHeight)
                            points.forEach { p -> lineTo(p.x, p.y) }
                            lineTo(points.last().x, chartHeight)
                            close()
                        }
                        drawPath(path = fillPath, color = fillColor)

                        // 画折线
                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )

                        // 画数据点（外圈 + 内圈白点）
                        points.forEach { point ->
                            drawCircle(
                                color = lineColor,
                                radius = 5f,
                                center = point
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = point
                            )
                        }
                    } else {
                        // 单数据点
                        drawCircle(
                            color = lineColor,
                            radius = 6f,
                            center = points[0]
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = points[0]
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X 轴日期标签（旋转 30° 避免重叠）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(xAxisLabelHeight)
                ) {
                    data.forEach { point ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = point.label,
                                fontSize = 10.sp,
                                color = axisLabelColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer { rotationZ = -30f }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 计算一个"整洁"的 Y 轴最大值
 * 将原始最大值向上取整到合理的整数
 */
private fun computeNiceMax(maxValue: Double): Double {
    if (maxValue <= 0) return 100.0
    // 向上取整到 5 的倍数（小额）或 50/100 的倍数（大额）
    val niceMax = when {
        maxValue <= 100 -> ((maxValue / 5).roundToInt() + 1) * 5.0
        maxValue <= 1000 -> ((maxValue / 50).roundToInt() + 1) * 50.0
        maxValue <= 10000 -> ((maxValue / 500).roundToInt() + 1) * 500.0
        else -> ((maxValue / 1000).roundToInt() + 1) * 1000.0
    }
    return niceMax
}

/**
 * 将时间戳列表转换为折线图数据
 * @param dateFormat 日期格式，默认 MM/dd
 */
fun List<Pair<Long, Double>>.toChartPoints(
    dateFormat: String = "MM/dd"
): List<ChartPoint> {
    val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
    return this.map { (timestamp, value) ->
        ChartPoint(
            value = value,
            label = sdf.format(Date(timestamp))
        )
    }
}
