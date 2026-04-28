package com.xuri.billapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "amount")
    val amount: Double,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "note")
    val note: String = "",
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
