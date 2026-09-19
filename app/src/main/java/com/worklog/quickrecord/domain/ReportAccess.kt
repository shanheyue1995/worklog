package com.worklog.quickrecord.domain

/**
 * 判断一次内容地址访问是不是「别的 App 把导出的报告读走了」。
 *
 * 这是「分享到底有没有发生」唯一靠得住的信号：系统分享面板不会告诉调用方
 * 用户选了哪个 App，用户取消也拿不到区别。别人真的读了文件，才算分享出去。
 * 抽成纯函数是为了能在电脑上直接验证边界（拍照是写文件、读自己的照片不算）。
 */
object ReportAccess {

    fun isSharedReportRead(mode: String, lastPathSegment: String?): Boolean {
        if (!mode.startsWith("r")) return false
        return lastPathSegment.orEmpty().endsWith(".pdf", ignoreCase = true)
    }
}
