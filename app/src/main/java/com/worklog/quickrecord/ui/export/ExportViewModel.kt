package com.worklog.quickrecord.ui.export

import android.content.Context

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worklog.quickrecord.data.PhotoStore
import com.worklog.quickrecord.data.Preferences
import com.worklog.quickrecord.data.RecordRepository
import com.worklog.quickrecord.data.ReportExporter
import com.worklog.quickrecord.domain.DateRangeCalculator
import com.worklog.quickrecord.domain.ExportRange
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.widget.WidgetRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ExportSummary(
    val start: LocalDate = LocalDate.now().withDayOfMonth(1),
    val end: LocalDate = LocalDate.now(),
    val recordCount: Int = 0,
    val photoCount: Int = 0,
)

sealed interface ExportState {
    data object Choosing : ExportState
    data object Working : ExportState
    data object Empty : ExportState
    data object Failed : ExportState
    data class Done(
        val file: File,
        val pageCount: Int,
        val recordCount: Int,
        val photoCount: Int,
    ) : ExportState
}

class ExportViewModel(
    private val repository: RecordRepository,
    private val photoStore: PhotoStore,
    private val exportDir: File,
    private val preferences: Preferences,
    private val appContext: Context,
) : ViewModel() {

    var range by mutableStateOf(ExportRange.ThisMonth)
        private set

    var summary by mutableStateOf(ExportSummary())
        private set

    var state by mutableStateOf<ExportState>(ExportState.Choosing)
        private set

    init {
        reload()
    }

    fun selectRange(value: ExportRange) {
        if (value == range) return
        range = value
        state = ExportState.Choosing
        reload()
    }

    fun backToChoosing() {
        state = ExportState.Choosing
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val period = DateRangeCalculator.period(range, LocalDate.now())
            val records = load(period.start, period.end)
            summary = ExportSummary(
                start = period.start,
                end = period.end,
                recordCount = records.size,
                photoCount = records.sumOf { it.photos.size },
            )
        }
    }

    fun generate() {
        state = ExportState.Working
        viewModelScope.launch {
            val period = DateRangeCalculator.period(range, LocalDate.now())
            val records = load(period.start, period.end)

            if (records.isEmpty()) {
                state = ExportState.Empty
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val file = File(exportDir, "工作记录-${period.start}-${period.end}.pdf")
                    ReportExporter.export(
                        records = records,
                        output = file,
                        from = period.start,
                        to = period.end,
                        resolvePhoto = photoStore::fileFor,
                    )
                } catch (error: Exception) {
                    null
                }
            }

            state = if (result == null) {
                ExportState.Failed
            } else {
                preferences.setLastExportDate(LocalDate.now())
                WidgetRefresh.refresh(appContext)
                ExportState.Done(
                    file = result.file,
                    pageCount = result.pageCount,
                    recordCount = result.recordCount,
                    photoCount = result.photoCount,
                )
            }
        }
    }

    private suspend fun load(start: LocalDate, end: LocalDate): List<Record> =
        repository.listInRange(
            start.atStartOfDay(),
            LocalDateTime.of(end, LocalTime.of(23, 59, 59)),
        )
}
