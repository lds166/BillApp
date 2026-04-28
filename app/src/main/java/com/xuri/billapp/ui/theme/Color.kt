package com.xuri.billapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 应用主色 - 绿色系
 */
val Green500 = Color(0xFF10B981)
val Green600 = Color(0xFF059669)
val Green50 = Color(0xFFECFDF5)

/**
 * 警告色 - 橙色（预算 80%）
 */
val Orange500 = Color(0xFFF59E0B)
val Orange600 = Color(0xFFD97706)

/**
 * 危险色 - 红色（超支）
 */
val Red500 = Color(0xFFEF4444)
val Red600 = Color(0xFFDC2626)
val Red50 = Color(0xFFFEE2E2)

/**
 * 中性色
 */
val BackgroundLight = Color(0xFFF8FAFC)
val CardLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val DividerLight = Color(0xFFE2E8F0)

/**
 * 分类颜色映射
 */
fun getCategoryColorByIndex(index: Int): Color {
    return when (index % 8) {
        0 -> Color(0xFF10B981) // 绿 - 餐饮
        1 -> Color(0xFFF59E0B) // 橙 - 购物
        2 -> Color(0xFF3B82F6) // 蓝 - 交通
        3 -> Color(0xFF8B5CF6) // 紫 - 娱乐
        4 -> Color(0xFFEC4899) // 粉 - 居住
        5 -> Color(0xFFEF4444) // 红 - 医疗
        6 -> Color(0xFF06B6D4) // 青 - 教育
        7 -> Color(0xFF6B7280) // 灰 - 其他
        else -> Color(0xFF6B7280)
    }
}
