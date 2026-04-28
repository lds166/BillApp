package com.xuri.billapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY id")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)
}
