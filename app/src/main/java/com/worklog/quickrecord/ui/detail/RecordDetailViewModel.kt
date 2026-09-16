package com.worklog.quickrecord.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worklog.quickrecord.data.RecordRepository
import com.worklog.quickrecord.domain.Record
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 详情页的 ViewModel。
 *
 * 用 select 而不是构造参数，是因为实例由界面容器复用，
 * 每次进入详情只是换一条记录，不需要新建 ViewModel。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailViewModel(private val repository: RecordRepository) : ViewModel() {

    private val recordId = MutableStateFlow<Long?>(null)

    val record: StateFlow<Record?> = recordId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeRecord(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(id: Long) {
        recordId.value = id
    }

    fun delete(onDeleted: () -> Unit) {
        val id = recordId.value ?: return
        viewModelScope.launch {
            repository.delete(id)
            onDeleted()
        }
    }
}
