package com.worklog.quickrecord.reminder

import com.worklog.quickrecord.reminder.ReminderRules.State
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderRulesTest {

    private val today = LocalDate.of(2026, 9, 16)

    @Test
    fun `没有记录时不提醒`() {
        val state = ReminderRules.evaluate(null, today, hasRecords = false, reminderEnabled = true)
        assertEquals(State.None, state)
    }

    @Test
    fun `用户关掉提醒后不再提醒`() {
        val state = ReminderRules.evaluate(
            lastExport = LocalDate.of(2026, 1, 1),
            today = today,
            hasRecords = true,
            reminderEnabled = false,
        )
        assertEquals(State.None, state)
    }

    @Test
    fun `有记录但从没导出过要提醒`() {
        val state = ReminderRules.evaluate(null, today, hasRecords = true, reminderEnabled = true)
        assertEquals(State.NeverExported, state)
    }

    @Test
    fun `未满三十天不提醒`() {
        val state = ReminderRules.evaluate(
            lastExport = today.minusDays(29),
            today = today,
            hasRecords = true,
            reminderEnabled = true,
        )
        assertEquals(State.None, state)
    }

    @Test
    fun `刚好三十天要提醒`() {
        val state = ReminderRules.evaluate(
            lastExport = today.minusDays(30),
            today = today,
            hasRecords = true,
            reminderEnabled = true,
        )
        assertEquals(State.Stale(30), state)
    }

    @Test
    fun `跨月天数计算正确`() {
        val state = ReminderRules.evaluate(
            lastExport = LocalDate.of(2026, 8, 4),
            today = today,
            hasRecords = true,
            reminderEnabled = true,
        )
        assertEquals(State.Stale(43), state)
    }

    @Test
    fun `本月一号还没到九点时下一次提醒就在本月`() {
        val next = ReminderRules.nextReminderMoment(LocalDateTime.of(2026, 9, 1, 8, 30))
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), next)
    }

    @Test
    fun `本月一号九点已过则顺延到下个月`() {
        val next = ReminderRules.nextReminderMoment(LocalDateTime.of(2026, 9, 1, 9, 0))
        assertEquals(LocalDateTime.of(2026, 10, 1, 9, 0), next)
    }

    @Test
    fun `年末提醒会正确跨年`() {
        val next = ReminderRules.nextReminderMoment(LocalDateTime.of(2026, 12, 20, 15, 0))
        assertEquals(LocalDateTime.of(2027, 1, 1, 9, 0), next)
    }

    @Test
    fun `延迟毫秒数与真实间隔一致`() {
        val delay = ReminderRules.delayMillis(LocalDateTime.of(2026, 9, 16, 10, 0))
        val expected = 14L * 24 * 60 * 60 * 1000 + 23L * 60 * 60 * 1000
        assertEquals(expected, delay)
    }
}
