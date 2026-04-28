package com.xuri.billapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType {
    EXPENSE, INCOME
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon")
    val icon: String,
    @ColumnInfo(name = "type")
    val type: CategoryType = CategoryType.EXPENSE
)
