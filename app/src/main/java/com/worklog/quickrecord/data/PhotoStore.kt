package com.worklog.quickrecord.data

import java.io.File
import java.time.LocalDateTime

/**
 * 照片文件存放位置。
 *
 * 全部放在 App 私有目录，不写系统相册——这是产品设计里已经确认的决策。
 * 数据库只存相对路径，这里负责把相对路径换算成真实文件。
 */
class PhotoStore(private val rootDir: File) {

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
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
}
