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

    @Test
    fun `页数少时预览保持最清晰倍率`() {
        // A4 一页按 1.4 倍渲染约 3.9MB，几页的报告完全吃得下
        assertEquals(ReportLayout.PREVIEW_MAX_SCALE, ReportLayout.previewScale(1, 595, 842), 0.001f)
        assertEquals(ReportLayout.PREVIEW_MAX_SCALE, ReportLayout.previewScale(4, 595, 842), 0.001f)
    }

    @Test
    fun `页数多时预览自动降低倍率以控制内存`() {
        val scale = ReportLayout.previewScale(20, 595, 842)
        assertTrue("倍率应当降下来，实际 $scale", scale < ReportLayout.PREVIEW_MAX_SCALE)
        assertTrue("倍率不应低于下限，实际 $scale", scale >= ReportLayout.PREVIEW_MIN_SCALE)
    }

    @Test
    fun `预览图占用的内存始终不超过预算`() {
        // 倍率是 Float，回算字节数会有极小的舍入误差，留千分之一的余量
        val budget = ReportLayout.PREVIEW_BUDGET_BYTES * 1.001
        (1..20).forEach { pages ->
            val scale = ReportLayout.previewScale(pages, 595, 842)
            val bytes = pages * 595.0 * 842.0 * scale * scale * 4.0
            assertTrue(
                "$pages 页占用 ${bytes.toLong()} 字节，超出预算",
                bytes <= budget,
            )
        }
    }

    @Test
    fun `页数越多预览倍率只会变小`() {
        val scales = listOf(1, 2, 4, 8, 16, 20).map { ReportLayout.previewScale(it, 595, 842) }
        scales.zipWithNext().forEach { (previous, next) ->
            assertTrue("$previous -> $next", next <= previous + 0.0001f)
        }
    }

    @Test
    fun `页面尺寸异常时退回默认倍率而不是崩溃`() {
        assertEquals(ReportLayout.PREVIEW_MAX_SCALE, ReportLayout.previewScale(0, 595, 842), 0.001f)
        assertEquals(ReportLayout.PREVIEW_MAX_SCALE, ReportLayout.previewScale(3, 0, 842), 0.001f)
        assertEquals(ReportLayout.PREVIEW_MAX_SCALE, ReportLayout.previewScale(3, 595, 0), 0.001f)
    }
}
