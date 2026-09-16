package com.worklog.quickrecord.reminder

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 导出提醒的判断规则。
 *
 * 全部是纯函数，不碰 Android 类型，便于在电脑上验证边界。
 */
object ReminderRules {

    /** 超过这个天数没有导出就提示，产品设计里暂定 30 天。 */
    const val STALE_DAYS = 30L

    /** 每月提醒的固定时刻：1 号早上 9 点。 */
    const val REMINDER_HOUR = 9

    sealed interface State {
        /** 不需要提醒。 */
        data object None : State

        /** 有记录但从来没导出过。 */
        data object NeverExported : State

        /** 距离上次导出已经过去若干天。 */
        data class Stale(val days: Long) : State
    }

    fun evaluate(
        lastExport: LocalDate?,
        today: LocalDate,
        hasRecords: Boolean,
        reminderEnabled: Boolean,
    ): State {
        if (!reminderEnabled) return State.None
        if (!hasRecords) return State.None
        if (lastExport == null) return State.NeverExported

        val days = ChronoUnit.DAYS.between(lastExport, today)
        return if (days >= STALE_DAYS) State.Stale(days) else State.None
    }

    /**
     * 下一次该提醒的时刻。
     *
     * 按日历走而不是按累计天数走：老师交台账本来就是按月交的。
     * 本月的 1 号 9 点还没到就定在本月，已过则顺延到下个月。
     */
    fun nextReminderMoment(now: LocalDateTime): LocalDateTime {
        val thisMonth = now.toLocalDate().withDayOfMonth(1).atTime(REMINDER_HOUR, 0)
        return if (now.isBefore(thisMonth)) thisMonth else thisMonth.plusMonths(1)
    }

    /** 距离下一个提醒时刻还有多少毫秒，用于安排后台任务。 */
    fun delayMillis(now: LocalDateTime): Long =
        Duration.between(now, nextReminderMoment(now)).toMillis().coerceAtLeast(0L)
}
