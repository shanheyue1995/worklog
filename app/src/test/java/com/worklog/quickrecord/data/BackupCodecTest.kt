package com.worklog.quickrecord.data

import com.worklog.quickrecord.domain.Photo
import com.worklog.quickrecord.domain.Record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class BackupCodecTest {

    private val records = listOf(
        Record(
            id = 1,
            occurredAt = LocalDateTime.of(2026, 9, 15, 14, 20),
            place = "高二(3)班",
            description = "投影仪不亮，更换灯泡后恢复正常。",
            photos = listOf(
                Photo(relativePath = "202609/a.jpg", width = 1600, height = 1200, orderIndex = 0),
                Photo(relativePath = "202609/b.jpg", width = 1200, height = 1600, orderIndex = 1),
            ),
        ),
        Record(
            id = 2,
            occurredAt = LocalDateTime.of(2026, 9, 10, 16, 15),
            place = "阶梯教室",
            description = "电子白板定位偏移，重新校准。",
        ),
    )

    @Test
    fun `导出再读回来内容一致`() {
        val backup = BackupCodec.toBackup(records, LocalDateTime.of(2026, 9, 16, 9, 0))
        val restored = BackupCodec.decode(BackupCodec.encode(backup))

        assertNotNull(restored)
        val restoredRecords = BackupCodec.toRecords(restored!!)
        assertEquals(2, restoredRecords.size)

        val first = restoredRecords.first()
        assertEquals("高二(3)班", first.place)
        assertEquals(LocalDateTime.of(2026, 9, 15, 14, 20), first.occurredAt)
        assertEquals(2, first.photos.size)
        assertEquals("202609/a.jpg", first.photos[0].relativePath)
        assertEquals(1600, first.photos[0].width)
        assertEquals(1, first.photos[1].orderIndex)
    }

    @Test
    fun `没有照片的记录也能正常往返`() {
        val backup = BackupCodec.toBackup(records, LocalDateTime.now())
        val restored = BackupCodec.decode(BackupCodec.encode(backup))!!
        assertTrue(BackupCodec.toRecords(restored).last().photos.isEmpty())
    }

    @Test
    fun `备份里记录了版本号与导出时间`() {
        val backup = BackupCodec.toBackup(records, LocalDateTime.of(2026, 9, 16, 9, 0))
        assertEquals(BackupFile.CURRENT_VERSION, backup.version)
        assertEquals("2026-09-16T09:00:00", backup.exportedAt)
    }

    @Test
    fun `损坏的内容返回空而不是抛异常`() {
        assertNull(BackupCodec.decode("这不是一个备份文件"))
        assertNull(BackupCodec.decode(""))
    }

    @Test
    fun `高版本的备份拒绝读取`() {
        val text = """{"version":99,"exportedAt":"","records":[]}"""
        assertNull(BackupCodec.decode(text))
    }

    @Test
    fun `多出未知字段的备份仍可读取`() {
        val text = """
            {
              "version": 1,
              "exportedAt": "2026-09-16T09:00:00",
              "future": "unknown",
              "records": [
                {
                  "occurredAt": "2026-09-15T14:20:00",
                  "place": "高二(3)班",
                  "description": "更换灯泡",
                  "photos": [],
                  "extra": 1
                }
              ]
            }
        """.trimIndent()
        val restored = BackupCodec.decode(text)
        assertNotNull(restored)
        assertEquals(1, BackupCodec.toRecords(restored!!).size)
    }

    @Test
    fun `时间格式不对的条目被跳过而不是整体失败`() {
        val text = """
            {
              "version": 1,
              "records": [
                {"occurredAt": "坏掉的时间", "place": "A", "description": "x"},
                {"occurredAt": "2026-09-15T14:20:00", "place": "B", "description": "y"}
              ]
            }
        """.trimIndent()
        val restored = BackupCodec.decode(text)!!
        val list = BackupCodec.toRecords(restored)
        assertEquals(1, list.size)
        assertEquals("B", list.first().place)
    }
}
