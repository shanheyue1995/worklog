package com.worklog.quickrecord.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置。
 *
 * 目前只存两件事：上次导出的日期（提醒和小组件都依赖它）与提醒开关。
 * 与记录数据分开存，清空记录不会连带丢失这两项。
 */
class Preferences(private val context: Context) {

    private object Keys {
        val LAST_EXPORT_DATE = stringPreferencesKey("last_export_date")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }

    val lastExportDate: Flow<LocalDate?> = context.settingsStore.data.map { preferences ->
        preferences[Keys.LAST_EXPORT_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    val reminderEnabled: Flow<Boolean> = context.settingsStore.data.map { preferences ->
        preferences[Keys.REMINDER_ENABLED] ?: true
    }

    suspend fun setLastExportDate(value: LocalDate) {
        context.settingsStore.edit { it[Keys.LAST_EXPORT_DATE] = value.toString() }
    }

    suspend fun setReminderEnabled(value: Boolean) {
        context.settingsStore.edit { it[Keys.REMINDER_ENABLED] = value }
    }
}
