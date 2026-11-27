package com.aurora.store.util

import org.junit.Assert.*
import org.junit.Test

class ApkeepConfigTest {
    @Test fun readsOnlyGoogleSectionAndPreservesEquals() {
        val result = ApkeepConfig.parse("[other]\nemail=wrong\n[google]\nemail=test@example.com\naas_token=FAKE==\n")
        assertEquals("test@example.com", result.email)
        assertEquals("FAKE==", result.token)
    }
    @Test fun rejectsDuplicates() {
        assertThrows(IllegalArgumentException::class.java) {
            ApkeepConfig.parse("[google]\nemail=test@example.com\naas_token=x\naas_token=y")
        }
    }
    @Test fun rejectsMissingToken() {
        assertThrows(IllegalArgumentException::class.java) { ApkeepConfig.parse("[google]\nemail=test@example.com") }
    }
    @Test fun rejectsOversizeInput() {
        assertThrows(IllegalArgumentException::class.java) { ApkeepConfig.read(ByteArray(65537).inputStream()) }
    }
}
