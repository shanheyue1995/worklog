package com.worklog.quickrecord.domain

import java.time.LocalDateTime

/**
 * 一条工作记录。
 *
 * 字段严格对应《产品设计文档》4.1 节：时间、地点、问题与解决方法、照片。
 * 时间用 LocalDateTime 而不是时间戳，避免在业务代码里反复做换算。
 */
data class Record(
    val id: Long = 0L,
    val occurredAt: LocalDateTime,
    val place: String,
    val description: String,
    val photos: List<Photo> = emptyList(),
)

/**
 * 记录附带的一张照片。relativePath 是相对于 App 私有照片目录的路径，
 * 不存绝对路径，方便日后整体迁移目录。
 */
data class Photo(
    val id: Long = 0L,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val orderIndex: Int = 0,
)

/** 一条记录在保存前必须满足的条件。 */
object RecordValidator {

    const val MAX_PHOTOS = 9

    fun validate(place: String, description: String): ValidationResult {
        if (place.isBlank()) return ValidationResult.MissingPlace
        if (description.isBlank()) return ValidationResult.MissingDescription
        return ValidationResult.Ok
    }

    sealed interface ValidationResult {
        data object Ok : ValidationResult
        data object MissingPlace : ValidationResult
        data object MissingDescription : ValidationResult
    }
}
