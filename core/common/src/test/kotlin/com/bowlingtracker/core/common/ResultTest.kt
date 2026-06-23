package com.bowlingtracker.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResultTest {
    @Test fun `map transforms success`() {
        val r = 2.asSuccess().map { it * 10 }
        assertEquals(20, r.getOrNull())
    }

    @Test fun `map leaves failure untouched`() {
        val r: Result<Int, DomainError> = DomainError.NoBounceDetected.asFailure()
        assertTrue(r.map { it + 1 } is Result.Failure)
    }

    @Test fun `getOrElse returns fallback on failure`() {
        val r: Result<Int, String> = "boom".asFailure()
        assertEquals(-1, r.getOrElse { -1 })
    }
}
