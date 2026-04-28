package com.xuri.billapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "budget_settings")

class BudgetManager(private val context: Context) {

    private val budgetKey = doublePreferencesKey("daily_budget")

    val dailyBudget: Flow<Double> = context.budgetDataStore.data
        .map { preferences ->
            preferences[budgetKey] ?: 0.0
        }

    suspend fun setBudget(amount: Double) {
        context.budgetDataStore.edit { preferences ->
            preferences[budgetKey] = amount
        }
    }

    suspend fun clearBudget() {
        context.budgetDataStore.edit { preferences ->
            preferences.remove(budgetKey)
        }
    }
}
