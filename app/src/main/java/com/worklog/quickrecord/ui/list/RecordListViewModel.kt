package com.worklog.quickrecord.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worklog.quickrecord.data.RecordRepository
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.domain.RecordSearch
import com.worklog.quickrecord.reminder.ReminderRules
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordListUiState(
    val records: List<Record> = emptyList(),
    val query: String = "",
    val frequentPlaces: List<String> = emptyList(),
    val totalCount: Int = 0,
    val reminder: ReminderRules.State = ReminderRules.State.None,
)

class RecordListViewModel(
    private val repository: RecordRepository,
    private val preferences: Preferences,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<RecordListUiState> = combine(
        repository.observeRecords(),
        query,
        preferences.lastExportDate,
        preferences.reminderEnabled,
    ) { records, keyword, lastExport, reminderEnabled ->
        RecordListUiState(
            records = RecordSearch.filter(records, keyword),
            query = keyword,
            frequentPlaces = RecordSearch.frequentPlaces(records),
            totalCount = records.size,
            reminder = ReminderRules.evaluate(
                lastExport = lastExport,
                today = LocalDate.now(),
                hasRecords = records.isNotEmpty(),
                reminderEnabled = reminderEnabled,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordListUiState(),
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
