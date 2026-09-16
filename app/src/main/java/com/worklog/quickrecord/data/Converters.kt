package com.worklog.quickrecord.data

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 时间以固定宽度的字符串入库。
 *
 * 记录的时间是"老师看到的墙上时间"，不是某个瞬时时刻，所以不能存时间戳——
 * 否则换时区后显示的时间会跟着变。用定长 ISO 字符串还有个好处：
 * 字符串的字典序与时间先后一致，范围查询可以直接比较。
 */
private val DB_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

class Converters {

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime): String = value.format(DB_TIME_FORMAT)

    @TypeConverter
    fun toLocalDateTime(value: String): LocalDateTime = LocalDateTime.parse(value, DB_TIME_FORMAT)
}
