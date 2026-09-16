package com.worklog.quickrecord.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 界面上的时间显示格式，集中一处，避免各页面写法不一致。 */
object DisplayFormat {

    private val FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val SHORT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val MONTH_ONLY = DateTimeFormatter.ofPattern("yyyy 年 M 月")
    private val DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun full(value: LocalDateTime): String = value.format(FULL)

    fun short(value: LocalDateTime): String = value.format(SHORT)

    fun monthOnly(value: LocalDateTime): String = value.format(MONTH_ONLY)

    fun dateOnly(value: LocalDateTime): String = value.format(DATE_ONLY)
}
