package com.worklog.quickrecord.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAccessTest {

    @Test
    fun `别的 App 来读导出的 PDF 算一次分享`() {
        assertTrue(ReportAccess.isSharedReportRead("r", "工作记录-2026-09-01-2026-09-30.pdf"))
    }

    @Test
    fun `相机来写照片不算分享`() {
        assertFalse(ReportAccess.isSharedReportRead("w", "工作记录-2026-09-01-2026-09-30.pdf"))
    }

    @Test
    fun `读照片不算分享`() {
        assertFalse(ReportAccess.isSharedReportRead("r", "photo-20260919.jpg"))
    }

    @Test
    fun `拿不到文件名时不误判`() {
        assertFalse(ReportAccess.isSharedReportRead("r", null))
        assertFalse(ReportAccess.isSharedReportRead("r", ""))
    }

    @Test
    fun `后缀大小写不影响判断`() {
        assertTrue(ReportAccess.isSharedReportRead("r", "REPORT.PDF"))
    }

    @Test
    fun `读写模式只按「有没有读」判断`() {
        assertFalse(ReportAccess.isSharedReportRead("rw", "photo.jpg"))
        assertTrue(ReportAccess.isSharedReportRead("rw", "report.pdf"))
    }
}
