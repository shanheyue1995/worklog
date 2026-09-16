package com.worklog.quickrecord.data

import com.worklog.quickrecord.domain.Photo
import com.worklog.quickrecord.domain.Record
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 备份文件的结构。
 *
 * 单独定义一份，而不是直接序列化数据库实体：这样数据库结构以后调整
 * （改列名、加表）不会让老的备份包变得无法读取。
 */
@Serializable
data class BackupPhoto(
    val path: String,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class BackupRecord(
    val occurredAt: String,
    val place: String,
    val description: String,
    val photos: List<BackupPhoto> = emptyList(),
)

@Serializable
data class BackupFile(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String = "",
    val records: List<BackupRecord> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * 备份内容的编解码。
 *
 * 与文件读写分开，方便直接在电脑上验证"存进去再读出来"是否一致。
 */
object BackupCodec {

    private val TIME_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun toBackup(records: List<Record>, exportedAt: LocalDateTime): BackupFile = BackupFile(
        version = BackupFile.CURRENT_VERSION,
        exportedAt = exportedAt.format(TIME_FORMAT),
        records = records.map { record ->
            BackupRecord(
                occurredAt = record.occurredAt.format(TIME_FORMAT),
                place = record.place,
                description = record.description,
                photos = record.photos.map { photo ->
                    BackupPhoto(
                        path = photo.relativePath,
                        width = photo.width,
                        height = photo.height,
                    )
                },
            )
        },
    )

    fun encode(backup: BackupFile): String = json.encodeToString(backup)

    fun decode(text: String): BackupFile? = runCatching { json.decodeFromString<BackupFile>(text) }
        .getOrNull()
        ?.takeIf { it.version <= BackupFile.CURRENT_VERSION }

    fun toRecords(backup: BackupFile): List<Record> = backup.records.mapNotNull { record ->
        val occurredAt = runCatching {
            LocalDateTime.parse(record.occurredAt, TIME_FORMAT)
        }.getOrNull() ?: return@mapNotNull null

        Record(
            occurredAt = occurredAt,
            place = record.place,
            description = record.description,
            photos = record.photos.mapIndexed { index, photo ->
                Photo(
                    relativePath = photo.path,
                    width = photo.width,
                    height = photo.height,
                    orderIndex = index,
                )
            },
        )
    }
}
