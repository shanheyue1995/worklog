package com.worklog.quickrecord.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportActionTest {

    @Test
    fun `只生成报告看预览不算已经导出`() {
        assertFalse(ExportAction.Preview.countsAsExport)
    }

    @Test
    fun `保存成文件算已经导出`() {
        assertTrue(ExportAction.SaveFile.countsAsExport)
    }

    @Test
    fun `分享出去算已经导出`() {
        assertTrue(ExportAction.Share.countsAsExport)
    }
}
