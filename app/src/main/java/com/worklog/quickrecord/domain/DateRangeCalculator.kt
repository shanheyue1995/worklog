package com.worklog.quickrecord.domain

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 导出时可选择的时间范围。 */
enum class ExportRange {
    ThisWeek,
    ThisMonth,
    LastMonth,
}

/** 一个闭区间的日期范围。 */
data class DatePeriod(val start: LocalDate, val end: LocalDate)

/**
 * 根据"今天"推算导出用的时间范围。
 *
 * 单独抽成纯函数，不依赖任何 Android 类型，便于在电脑上直接跑单元测试。
 */
object DateRangeCalculator {

    fun period(range: ExportRange, today: LocalDate): DatePeriod = when (range) {
        ExportRange.ThisWeek -> {
            // DayOfWeek 里周一是 1、周日是 7，先换算成"距本周一几天"。
            val daysSinceMonday = (today.dayOfWeek.value - 1).toLong()
            val monday = today.minusDays(daysSinceMonday)
            DatePeriod(monday, monday.plusDays(6))
        }

        ExportRange.ThisMonth -> {
            val first = today.withDayOfMonth(1)
            DatePeriod(first, today.with(TemporalAdjusters.lastDayOfMonth()))
        }

        ExportRange.LastMonth -> {
            val lastOfPrevious = today.withDayOfMonth(1).minusDays(1)
            DatePeriod(lastOfPrevious.withDayOfMonth(1), lastOfPrevious)
        }
    }
}
