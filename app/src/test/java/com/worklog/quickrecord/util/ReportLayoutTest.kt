package com.worklog.quickrecord.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLayoutTest {

    private val contentWidth = 515f

    @Test
    fun `两列排布时等分宽度并留出间隙`() {
        // (515 - 10) / 2 = 252.5
        val size = ReportLayout.photoSize(contentWidth, 2, 4000, 3000)
        assertEquals(252.5f, size.width, 0.01f)
    }

    @Test
    fun `横图不超高时保持原始比例`() {
        val size = ReportLayout.photoSize(contentWidth, 2, 4000, 3000)
        assertEquals(252.5f * 0.75f, size.height, 0.01f)
    }

    @Test
    fun `竖图超过高度上限时整体等比缩小，不被拉变形`() {
        val size = ReportLayout.photoSize(contentWidth, 2, 3000, 4000)
        assertEquals(ReportLayout.MAX_PHOTO_HEIGHT, size.height, 0.01f)
        // 缩小后宽高比仍然是 3:4
        assertEquals(3f / 4f, size.width / size.height, 0.001f)
    }

    @Test
    fun `单张独占一行时高度同样不超过上限`() {
        val size = ReportLayout.photoSize(contentWidth, 1, 4000, 3000)
        assertEquals(ReportLayout.MAX_PHOTO_HEIGHT, size.height, 0.01f)
        assertEquals(4f / 3f, size.width / size.height, 0.001f)
    }

    @Test
    fun `尺寸缺失时按四比三兜底而不是崩溃`() {
        val size = ReportLayout.photoSize(contentWidth, 2, 0, 0)
        assertTrue(size.width > 0f)
        assertTrue(size.height > 0f)
        assertEquals(0.75f, size.height / size.width, 0.001f)
    }

    @Test
    fun `每行张数非法时按一张处理`() {
        val illegal = ReportLayout.photoSize(contentWidth, 0, 4000, 3000)
        val single = ReportLayout.photoSize(contentWidth, 1, 4000, 3000)
        assertEquals(single.width, illegal.width, 0.01f)
        assertEquals(single.height, illegal.height, 0.01f)
    }

    @Test
    fun `行数按每行张数向上取整`() {
        assertEquals(0, ReportLayout.rowCount(0, 2))
        assertEquals(1, ReportLayout.rowCount(1, 2))
        assertEquals(2, ReportLayout.rowCount(3, 2))
        assertEquals(3, ReportLayout.rowCount(5, 2))
    }

    @Test
    fun `单张照片变窄后整行居中`() {
        // 320 宽的照片放在 515 宽的内容区里，左边留 97.5
        assertEquals(97.5f, ReportLayout.rowStartX(515f, 320f), 0.01f)
    }

    @Test
    fun `占满整行时不偏移`() {
        assertEquals(0f, ReportLayout.rowStartX(515f, 515f), 0.01f)
    }

    @Test
    fun `内容比内容区还宽时不产生负偏移`() {
        assertEquals(0f, ReportLayout.rowStartX(515f, 600f), 0.01f)
    }
}
