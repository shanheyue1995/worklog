package com.worklog.quickrecord.util

/**
 * 报告里照片的排布换算。
 *
 * 抽出来是因为这里的判断容易出错，而且不依赖 Android 类型，可以在电脑上直接验。
 * 规则：每行放固定几张，等分内容宽度；高度按原始比例算，超过上限就整体等比缩小，
 * 保证照片不会被拉变形。
 */
object ReportLayout {

    const val MAX_PHOTO_HEIGHT = 240f
    const val COLUMN_GAP = 10f

    data class Size(val width: Float, val height: Float)

    fun photoSize(contentWidth: Float, perRow: Int, photoWidth: Int, photoHeight: Int): Size {
        val columns = perRow.coerceAtLeast(1)
        val columnWidth = (contentWidth - COLUMN_GAP * (columns - 1)) / columns
        val ratio = if (photoWidth > 0 && photoHeight > 0) {
            photoHeight.toFloat() / photoWidth.toFloat()
        } else {
            0.75f
        }
        val rawHeight = columnWidth * ratio
        val scale = if (rawHeight > MAX_PHOTO_HEIGHT) MAX_PHOTO_HEIGHT / rawHeight else 1f
        return Size(columnWidth * scale, rawHeight * scale)
    }

    fun rowCount(photoCount: Int, perRow: Int): Int {
        val columns = perRow.coerceAtLeast(1)
        if (photoCount <= 0) return 0
        return (photoCount + columns - 1) / columns
    }
}
