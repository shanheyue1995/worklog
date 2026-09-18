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
import com.worklog.quickrecord.domain.DatePeriod
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

    /** 自定义范围的起止日期，切换范围时保留上次的选择。 */
    var customStart by mutableStateOf(LocalDate.now().withDayOfMonth(1))
        private set

    var customEnd by mutableStateOf(LocalDate.now())
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

    fun updateCustomStart(value: LocalDate) {
        customStart = value
        if (customEnd.isBefore(value)) customEnd = value
        state = ExportState.Choosing
        reload()
    }

    fun updateCustomEnd(value: LocalDate) {
        customEnd = value
        if (customStart.isAfter(value)) customStart = value
        state = ExportState.Choosing
        reload()
    }

    private fun period(today: LocalDate) = when (range) {
        ExportRange.Custom -> DatePeriod(customStart, customEnd)
        else -> DateRangeCalculator.period(range, today)
    }

    /**
     * 重新统计当前范围内的记录数。
     *
     * 界面每次进入都要调一次：ViewModel 是复用的，不刷新的话会拿上次的数字，
     * 出现"显示 0 条、导出却有内容"这种自相矛盾的情况。
     */
    fun refresh() {
        // 每次进入导出页都回到"选范围"这一步：
        // 否则会直接显示上一次生成的那份报告，用户新加的记录看不到，像是没更新。
        state = ExportState.Choosing
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val period = period(LocalDate.now())
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
            val period = period(LocalDate.now())
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
