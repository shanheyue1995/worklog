package com.worklog.quickrecord.domain

/**
 * 列表页的搜索与地点补全规则。
 *
 * 保持纯函数，便于在电脑上直接测。地点补全是"十秒记一条"能否成立的关键，
 * 所以排序规则单独抽出来，不埋在界面代码里。
 */
object RecordSearch {

    /** 按地点或内容关键词过滤，大小写不敏感，空关键词返回全部。 */
    fun filter(records: List<Record>, query: String): List<Record> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return records
        return records.filter { record ->
            record.place.contains(keyword, ignoreCase = true) ||
                record.description.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 从历史记录里提炼常用地点：按出现次数从多到少，次数相同则最近用过的排前面。
     */
    fun frequentPlaces(records: List<Record>, limit: Int = 6): List<String> {
        val latestUse = mutableMapOf<String, java.time.LocalDateTime>()
        val count = mutableMapOf<String, Int>()

        for (record in records) {
            val place = record.place.trim()
            if (place.isEmpty()) continue
            count[place] = (count[place] ?: 0) + 1
            val previous = latestUse[place]
            if (previous == null || record.occurredAt.isAfter(previous)) {
                latestUse[place] = record.occurredAt
            }
        }

        return count.keys
            .sortedWith(
                compareByDescending<String> { count[it] ?: 0 }
                    .thenByDescending { latestUse[it] },
            )
            .take(limit)
    }
}
