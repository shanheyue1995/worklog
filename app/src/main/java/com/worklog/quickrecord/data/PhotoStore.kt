package com.worklog.quickrecord.data

import java.io.File
import java.time.LocalDateTime

/**
 * 照片文件存放位置。
 *
 * 全部放在 App 私有目录，不写系统相册——这是产品设计里已经确认的决策。
 * 数据库只存相对路径，这里负责把相对路径换算成真实文件。
 */
class PhotoStore(
    private val rootDir: File,
    private val tempDir: File = File(rootDir.parentFile, "tmp"),
) {

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
        if (!tempDir.exists()) tempDir.mkdirs()
    }

    fun fileFor(relativePath: String): File = File(rootDir, relativePath)

    /** 新照片的相对路径，按年月分目录，避免单个目录塞进上千个文件。 */
    fun newRelativePath(occurredAt: LocalDateTime, suffix: String = "jpg"): String {
        val dir = "%04d%02d".format(occurredAt.year, occurredAt.monthValue)
        val name = "${System.currentTimeMillis()}-${(1000..9999).random()}.$suffix"
        return "$dir/$name"
    }

    fun delete(relativePath: String) {
        val file = fileFor(relativePath)
        if (file.exists()) file.delete()
    }

    /**
     * 系统相机需要一个可写入的文件位置，先落到临时目录，
     * 压缩完再挪到正式目录，避免把未处理的原图当成品保存。
     */
    fun newTempFile(): File {
        if (!tempDir.exists()) tempDir.mkdirs()
        return File(tempDir, "capture-${System.currentTimeMillis()}.jpg")
    }

    fun deleteFile(file: File) {
        if (file.exists()) file.delete()
    }
}
