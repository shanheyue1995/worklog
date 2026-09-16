package com.worklog.quickrecord.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class RecordSearchTest {

    private fun record(
        id: Long,
        place: String,
        description: String,
        at: LocalDateTime = LocalDateTime.of(2026, 9, 16, 10, 0),
    ) = Record(id = id, occurredAt = at, place = place, description = description)

    private val records = listOf(
        record(1, "高二(3)班", "投影仪不亮，更换灯泡", LocalDateTime.of(2026, 9, 15, 14, 20)),
        record(2, "计算机房 2", "3 号机键盘失灵", LocalDateTime.of(2026, 9, 15, 9, 5)),
        record(3, "高二(3)班", "音箱无声，音频线松动", LocalDateTime.of(2026, 9, 12, 10, 40)),
    )

    @Test
    fun `空关键词返回全部记录`() {
        assertEquals(3, RecordSearch.filter(records, "").size)
        assertEquals(3, RecordSearch.filter(records, "   ").size)
    }

    @Test
    fun `按地点关键词过滤`() {
        val result = RecordSearch.filter(records, "高二")
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `按内容关键词过滤`() {
        val result = RecordSearch.filter(records, "键盘")
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `搜索忽略大小写与首尾空格`() {
        val mixed = listOf(record(9, "Computer Room", "Keyboard not working"))
        assertEquals(1, RecordSearch.filter(mixed, "  keyboard  ").size)
        assertEquals(1, RecordSearch.filter(mixed, "computer").size)
    }

    @Test
    fun `常用地点按出现次数排序`() {
        val places = RecordSearch.frequentPlaces(records)
        assertEquals("高二(3)班", places.first())
        assertEquals(2, places.size)
    }

    @Test
    fun `出现次数相同时最近用过的排前面`() {
        val sameCount = listOf(
            record(1, "阶梯教室", "a", LocalDateTime.of(2026, 9, 1, 8, 0)),
            record(2, "计算机房 2", "b", LocalDateTime.of(2026, 9, 20, 8, 0)),
        )
        assertEquals(listOf("计算机房 2", "阶梯教室"), RecordSearch.frequentPlaces(sameCount))
    }

    @Test
    fun `常用地点数量可以限制`() {
        assertEquals(1, RecordSearch.frequentPlaces(records, limit = 1).size)
    }

    @Test
    fun `空地点不会被当成常用地点`() {
        val withBlank = records + record(4, "   ", "无地点记录")
        val places = RecordSearch.frequentPlaces(withBlank)
        assertEquals(2, places.size)
    }
}
