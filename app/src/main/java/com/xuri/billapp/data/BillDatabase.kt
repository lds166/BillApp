package com.xuri.billapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class CategoryTypeConverter {
    @TypeConverter
    fun fromCategoryType(type: CategoryType): String = type.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)
}

@Database(
    entities = [Bill::class, Category::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CategoryTypeConverter::class)
abstract class BillDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: BillDatabase? = null

        fun getDatabase(context: Context): BillDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BillDatabase::class.java,
                    "bill_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
