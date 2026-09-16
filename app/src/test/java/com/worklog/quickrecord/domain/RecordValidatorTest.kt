package com.worklog.quickrecord.domain

import com.worklog.quickrecord.domain.RecordValidator.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordValidatorTest {

    @Test
    fun `地点与内容都填了就算通过`() {
        assertEquals(
            ValidationResult.Ok,
            RecordValidator.validate("高二(3)班", "投影仪不亮，换了灯泡"),
        )
    }

    @Test
    fun `缺少地点不通过`() {
        assertEquals(
            ValidationResult.MissingPlace,
            RecordValidator.validate("   ", "投影仪不亮"),
        )
    }

    @Test
    fun `缺少内容不通过`() {
        assertEquals(
            ValidationResult.MissingDescription,
            RecordValidator.validate("高二(3)班", ""),
        )
    }

    @Test
    fun `地点缺失优先于内容缺失报出`() {
        assertEquals(
            ValidationResult.MissingPlace,
            RecordValidator.validate("", ""),
        )
    }
}
