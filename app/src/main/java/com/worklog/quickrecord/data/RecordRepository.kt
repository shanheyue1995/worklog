package com.worklog.quickrecord.data

import androidx.room.withTransaction
import com.worklog.quickrecord.domain.Photo
import com.worklog.quickrecord.domain.Record
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class RecordRepository(
    private val database: AppDatabase,
    private val photoStore: PhotoStore,
) {
    private val dao = database.recordDao()

    fun observeRecords(): Flow<List<Record>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeRecord(id: Long): Flow<Record?> =
        dao.observeById(id).map { row -> row?.toDomain() }

    suspend fun listInRange(from: LocalDateTime, to: LocalDateTime): List<Record> =
        dao.listInRange(from, to).map { it.toDomain() }

    suspend fun findById(id: Long): Record? =
        dao.listInRange(LocalDateTime.MIN, LocalDateTime.MAX)
            .firstOrNull { it.record.id == id }
            ?.toDomain()

    suspend fun countRecords(): Int = dao.countRecords()

    /**
     * 新增或更新一条记录。照片采用"先删后插"的简单策略，整体包在事务里，
     * 避免中途失败留下半条记录；被移除的照片文件同时删掉，不留垃圾。
     */
    suspend fun save(record: Record): Long = database.withTransaction {
        val id = if (record.id == 0L) {
            dao.insertRecord(record.toEntity())
        } else {
            dao.updateRecord(record.toEntity())
            record.id
        }

        val previousPaths = dao.listInRange(LocalDateTime.MIN, LocalDateTime.MAX)
            .firstOrNull { it.record.id == id }
            ?.photos
            ?.map { it.relativePath }
            .orEmpty()
        val keptPaths = record.photos.map { it.relativePath }.toSet()
        previousPaths.filterNot { keptPaths.contains(it) }.forEach(photoStore::delete)

        dao.deletePhotosOf(id)
        if (record.photos.isNotEmpty()) {
            dao.insertPhotos(record.photos.mapIndexed { index, photo -> photo.toEntity(id, index) })
        }
        id
    }

    suspend fun delete(id: Long) = database.withTransaction {
        val paths = dao.listInRange(LocalDateTime.MIN, LocalDateTime.MAX)
            .firstOrNull { it.record.id == id }
            ?.photos
            ?.map { it.relativePath }
            .orEmpty()

        dao.deleteRecord(id)
        paths.forEach(photoStore::delete)
    }
}

internal fun RecordWithPhotos.toDomain(): Record = Record(
    id = record.id,
    occurredAt = record.occurredAt,
    place = record.place,
    description = record.description,
    photos = photos.sortedBy { it.orderIndex }.map {
        Photo(
            id = it.id,
            relativePath = it.relativePath,
            width = it.width,
            height = it.height,
            orderIndex = it.orderIndex,
        )
    },
)

internal fun Record.toEntity(): RecordEntity = RecordEntity(
    id = id,
    occurredAt = occurredAt.withSecond(0).withNano(0),
    place = place.trim(),
    description = description.trim(),
)

internal fun Photo.toEntity(recordId: Long, index: Int): PhotoEntity = PhotoEntity(
    id = id,
    recordId = recordId,
    relativePath = relativePath,
    width = width,
    height = height,
    orderIndex = index,
)
