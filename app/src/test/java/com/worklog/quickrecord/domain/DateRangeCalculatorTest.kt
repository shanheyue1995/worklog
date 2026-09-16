package com.worklog.quickrecord.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateRangeCalculatorTest {

    @Test
    fun `本周从周一开始到周日结束`() {
        // 2026-09-16 是周三
        val period = DateRangeCalculator.period(ExportRange.ThisWeek, LocalDate.of(2026, 9, 16))
        assertEquals(LocalDate.of(2026, 9, 14), period.start)
        assertEquals(LocalDate.of(2026, 9, 20), period.end)
    }

    @Test
    fun `周日属于上一周的末尾而不是下一周的开头`() {
        val period = DateRangeCalculator.period(ExportRange.ThisWeek, LocalDate.of(2026, 9, 20))
        assertEquals(LocalDate.of(2026, 9, 14), period.start)
        assertEquals(LocalDate.of(2026, 9, 20), period.end)
    }

    @Test
    fun `周一当天本周范围仍然正确`() {
        val period = DateRangeCalculator.period(ExportRange.ThisWeek, LocalDate.of(2026, 9, 21))
        assertEquals(LocalDate.of(2026, 9, 21), period.start)
        assertEquals(LocalDate.of(2026, 9, 27), period.end)
    }

    @Test
    fun `本月覆盖整月而不是到今天为止`() {
        val period = DateRangeCalculator.period(ExportRange.ThisMonth, LocalDate.of(2026, 9, 16))
        assertEquals(LocalDate.of(2026, 9, 1), period.start)
        assertEquals(LocalDate.of(2026, 9, 30), period.end)
    }

    @Test
    fun `上月能正确跨年`() {
        val period = DateRangeCalculator.period(ExportRange.LastMonth, LocalDate.of(2026, 1, 15))
        assertEquals(LocalDate.of(2025, 12, 1), period.start)
        assertEquals(LocalDate.of(2025, 12, 31), period.end)
    }

    @Test
    fun `上月在闰年二月取到二十九日`() {
        val period = DateRangeCalculator.period(ExportRange.LastMonth, LocalDate.of(2028, 3, 10))
        assertEquals(LocalDate.of(2028, 2, 1), period.start)
        assertEquals(LocalDate.of(2028, 2, 29), period.end)
    }

    @Test
    fun `上月在平年二月取到二十八日`() {
        val period = DateRangeCalculator.period(ExportRange.LastMonth, LocalDate.of(2027, 3, 10))
        assertEquals(LocalDate.of(2027, 2, 1), period.start)
        assertEquals(LocalDate.of(2027, 2, 28), period.end)
    }
}
