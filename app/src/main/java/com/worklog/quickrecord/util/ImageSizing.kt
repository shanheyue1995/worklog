package com.worklog.quickrecord.util

/**
 * 图片尺寸换算。
 *
 * 这部分不碰任何 Android 类型，所以能直接在电脑上跑测试。
 * 采样率必须是 2 的幂，这是 BitmapFactory 内部的约定。
 */
object ImageSizing {

    fun sampleSize(width: Int, height: Int, maxLongSide: Int): Int {
        if (width <= 0 || height <= 0 || maxLongSide <= 0) return 1
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= maxLongSide) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    /** 等比缩放后的尺寸，长边不超过 maxLongSide。已经足够小则原样返回。 */
    fun scaledSize(width: Int, height: Int, maxLongSide: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxLongSide <= 0) return width to height
        val longest = maxOf(width, height)
        if (longest <= maxLongSide) return width to height

        val ratio = maxLongSide.toFloat() / longest.toFloat()
        return maxOf(1, (width * ratio).toInt()) to maxOf(1, (height * ratio).toInt())
    }
}
