package com.worklog.quickrecord.util

import kotlin.math.sqrt

/**
 * 报告里照片的排布换算。
 *
 * 抽出来是因为这里的判断容易出错，而且不依赖 Android 类型，可以在电脑上直接验。
 * 规则：每行放固定几张，等分内容宽度；高度按原始比例算，超过上限就整体等比缩小，
 * 保证照片不会被拉变形。
 */
object ReportLayout {

    /** 单张照片在报告里的最大高度。调小一点可以让一页容纳更多记录。 */
    const val MAX_PHOTO_HEIGHT = 190f
    const val COLUMN_GAP = 10f

    /** 预览时的最清晰倍率和最低倍率。 */
    const val PREVIEW_MAX_SCALE = 1.4f
    const val PREVIEW_MIN_SCALE = 0.75f

    /** 预览所有页面加起来允许占用的内存（字节）。手机堆内存有限，超过就会被系统杀掉。 */
    const val PREVIEW_BUDGET_BYTES = 24_000_000.0

    private const val BYTES_PER_PIXEL = 4.0

    data class Size(val width: Float, val height: Float)

    /**
     * 预览图的渲染倍率。
     *
     * 报告可能有很多页，每页都按最清晰倍率渲染成位图会吃掉大量内存，
     * 低配手机上会直接渲染失败、预览一片空白。这里按「所有页面加起来的像素总量」
     * 反推倍率：页数少时保持清晰，页数多时自动降一点清晰度，保证总占用不超标。
     */
    fun previewScale(pageCount: Int, pageWidth: Int, pageHeight: Int): Float {
        if (pageCount <= 0 || pageWidth <= 0 || pageHeight <= 0) return PREVIEW_MAX_SCALE
        val bytesAtFullScale =
            pageCount.toDouble() * pageWidth.toDouble() * pageHeight.toDouble() * BYTES_PER_PIXEL
        if (bytesAtFullScale <= 0.0 || !bytesAtFullScale.isFinite()) return PREVIEW_MAX_SCALE
        val scale = sqrt(PREVIEW_BUDGET_BYTES / bytesAtFullScale).toFloat()
        return scale.coerceIn(PREVIEW_MIN_SCALE, PREVIEW_MAX_SCALE)
    }

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

    /**
     * 一行照片在内容区里的起始横坐标。
     *
     * 照片被高度上限压低之后，一行往往占不满内容宽度，靠左排会留出大片空白，
     * 看起来像排版出错，所以整行居中。
     */
    fun rowStartX(contentWidth: Float, rowWidth: Float): Float {
        val offset = (contentWidth - rowWidth) / 2f
        return offset.coerceAtLeast(0f)
    }
}
