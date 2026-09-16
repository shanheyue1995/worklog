package com.worklog.quickrecord.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun `长边小于上限时不缩放`() {
        assertEquals(1, ImageSizing.sampleSize(800, 600, 1600))
        assertEquals(800 to 600, ImageSizing.scaledSize(800, 600, 1600))
    }

    @Test
    fun `采样率始终取二的幂`() {
        // 采样只负责把尺寸降到"刚不低于上限"，精确收尾交给等比缩放，
        // 这样解码出来的图不会被过度缩小，损失更少。
        assertEquals(4, ImageSizing.sampleSize(6400, 4800, 1600))
        assertEquals(2, ImageSizing.sampleSize(3200, 2400, 1600))
        assertEquals(2, ImageSizing.sampleSize(4000, 3000, 1600))
        // 略大于上限时不再降采样，避免直接掉到一半以下
        assertEquals(1, ImageSizing.sampleSize(1700, 1200, 1600))
        assertEquals(1, ImageSizing.sampleSize(2000, 1500, 1600))
    }

    @Test
    fun `竖图按高度这条长边计算采样率`() {
        // 宽度只有 1000、没超过上限，但高度 4000 超了，所以仍要降采样。
        // 如果实现错误地按宽度判断，这里会得到 1。
        assertEquals(2, ImageSizing.sampleSize(1000, 4000, 1600))
        assertEquals(4, ImageSizing.sampleSize(900, 7200, 1600))
    }

    @Test
    fun `等比缩放后长边正好等于上限`() {
        val (width, height) = ImageSizing.scaledSize(4000, 3000, 1600)
        assertEquals(1600, width)
        assertEquals(1200, height)
    }

    @Test
    fun `竖图缩放后高度等于上限`() {
        val (width, height) = ImageSizing.scaledSize(3000, 4000, 1600)
        assertEquals(1600, height)
        assertEquals(1200, width)
    }

    @Test
    fun `极窄的图缩放后宽高都不为零`() {
        val (width, height) = ImageSizing.scaledSize(10000, 3, 1600)
        assertEquals(1600, width)
        assertEquals(1, height)
    }

    @Test
    fun `非法尺寸不会导致除零或崩溃`() {
        assertEquals(1, ImageSizing.sampleSize(0, 0, 1600))
        assertEquals(1, ImageSizing.sampleSize(100, 100, 0))
        assertEquals(0 to 0, ImageSizing.scaledSize(0, 0, 1600))
    }
}
