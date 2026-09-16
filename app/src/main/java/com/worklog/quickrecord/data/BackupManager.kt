package com.worklog.quickrecord.data

import java.io.File
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 整包备份与恢复。
 *
 * 数据只存在手机里，所以"导出一份备份"是这套架构的必要配套，而不是附加功能。
 * 备份包是一个压缩包，里面是 backup.json 加照片原图，换手机时整包恢复即可。
 */
class BackupManager(
    private val repository: RecordRepository,
    private val photoStore: PhotoStore,
) {

    companion object {
        const val ENTRY_NAME = "backup.json"
        const val PHOTO_PREFIX = "photos/"
    }

    /** 导出全部数据，返回记录条数。 */
    suspend fun export(target: File): Int {
        val records = repository.listAll()
        val backup = BackupCodec.toBackup(records, LocalDateTime.now())

        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            zip.write(BackupCodec.encode(backup).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            records.flatMap { it.photos }.forEach { photo ->
                val file = photoStore.fileFor(photo.relativePath)
                if (!file.exists()) return@forEach
                zip.putNextEntry(ZipEntry(PHOTO_PREFIX + photo.relativePath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return records.size
    }

    /** 从备份包恢复，返回写入的记录条数；文件损坏或格式不符时返回 null。 */
    suspend fun import(source: File): Int? {
        var backupText: String? = null
        val photoBytes = mutableMapOf<String, ByteArray>()

        ZipInputStream(source.inputStream().buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == ENTRY_NAME ->
                        backupText = zip.readBytes().toString(Charsets.UTF_8)

                    entry.name.startsWith(PHOTO_PREFIX) ->
                        photoBytes[entry.name.removePrefix(PHOTO_PREFIX)] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val backup = backupText?.let(BackupCodec::decode) ?: return null
        val records = BackupCodec.toRecords(backup)

        // 先落照片再写库：照片写入失败时数据库不会指向不存在的文件。
        photoBytes.forEach { (relativePath, bytes) ->
            val file = photoStore.fileFor(relativePath)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }

        repository.replaceAll(records)
        return records.size
    }
}
