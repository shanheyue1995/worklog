package com.worklog.quickrecord.widget

import com.worklog.quickrecord.domain.Record
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WidgetRefreshRulesTest {

    private fun record(id: Long, place: String, description: String = "投影仪没信号") = Record(
        id = id,
        occurredAt = LocalDateTime.of(2026, 9, 19, 9, 0),
        place = place,
        description = description,
    )

    @Test
    fun `应用刚启动时先刷一次，免得停在旧画面`() {
        assertTrue(WidgetRefreshRules.shouldRefresh(null, listOf(record(1, "高一(1)班"))))
        assertTrue(WidgetRefreshRules.shouldRefresh(null, emptyList()))
    }

    @Test
    fun `新记一条要刷新`() {
        val before = listOf(record(2, "阶梯教室"))
        val after = listOf(record(3, "高一(1)班"), record(2, "阶梯教室"))
        assertTrue(WidgetRefreshRules.shouldRefresh(before, after))
    }

    @Test
    fun `删掉最新一条要刷新`() {
        val before = listOf(record(3, "高一(1)班"), record(2, "阶梯教室"))
        val after = listOf(record(2, "阶梯教室"))
        assertTrue(WidgetRefreshRules.shouldRefresh(before, after))
    }

    @Test
    fun `删掉最后一条也要刷新`() {
        assertTrue(WidgetRefreshRules.shouldRefresh(listOf(record(1, "阶梯教室")), emptyList()))
    }

    @Test
    fun `改最新一条的内容要刷新`() {
        val before = listOf(record(3, "高一(1)班", "投影仪没信号"))
        val after = listOf(record(3, "高一(1)班", "投影仪没信号，换了 HDMI 线"))
        assertTrue(WidgetRefreshRules.shouldRefresh(before, after))
    }

    @Test
    fun `只改旧记录不用刷新`() {
        val before = listOf(record(3, "高一(1)班"), record(2, "阶梯教室", "换音频线"))
        val after = listOf(record(3, "高一(1)班"), record(2, "阶梯教室", "换音频线并重新对频道"))
        assertFalse(WidgetRefreshRules.shouldRefresh(before, after))
    }

    @Test
    fun `补录一条更早的记录不用刷新`() {
        val before = listOf(record(3, "高一(1)班"))
        val after = listOf(record(3, "高一(1)班"), record(1, "高二(3)班"))
        assertFalse(WidgetRefreshRules.shouldRefresh(before, after))
    }

    @Test
    fun `内容没变不用刷新`() {
        val records = listOf(record(3, "高一(1)班"), record(2, "阶梯教室"))
        assertFalse(WidgetRefreshRules.shouldRefresh(records, records))
        assertFalse(WidgetRefreshRules.shouldRefresh(emptyList(), emptyList()))
    }
}
