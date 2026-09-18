package com.worklog.quickrecord.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalDateTime

/**
 * 先选日期再选时间。
 *
 * 补录是常见场景（现场没空记、晚上回来补），所以时间必须能改；
 * 用系统自带的选择器，不自己画日历。
 */
fun showDateTimePicker(
    context: Context,
    initial: LocalDateTime,
    onPicked: (LocalDateTime) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute -> onPicked(LocalDateTime.of(year, month + 1, day, hour, minute)) },
                initial.hour,
                initial.minute,
                true,
            ).show()
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

/** 只选日期，用于导出范围这类不关心具体时刻的场景。 */
fun showDatePicker(
    context: Context,
    initial: java.time.LocalDate,
    onPicked: (java.time.LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(java.time.LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}
