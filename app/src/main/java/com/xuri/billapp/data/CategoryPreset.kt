package com.xuri.billapp.data

object CategoryPreset {
    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        Category(name = "餐饮", icon = "restaurant", type = CategoryType.EXPENSE),
        Category(name = "购物", icon = "shopping_cart", type = CategoryType.EXPENSE),
        Category(name = "交通", icon = "directions_car", type = CategoryType.EXPENSE),
        Category(name = "娱乐", icon = "theater_comedy", type = CategoryType.EXPENSE),
        Category(name = "居住", icon = "home", type = CategoryType.EXPENSE),
        Category(name = "医疗", icon = "medical_services", type = CategoryType.EXPENSE),
        Category(name = "教育", icon = "school", type = CategoryType.EXPENSE),
        Category(name = "其他", icon = "more_horiz", type = CategoryType.EXPENSE),
    )
}
