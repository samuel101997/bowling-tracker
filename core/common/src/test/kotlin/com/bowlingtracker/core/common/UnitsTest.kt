package com.bowlingtracker.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnitsTest {
    private val eps = 1e-9

    @Test fun `speed converts mps to kmh`() {
        assertEquals(36.0, Speed.ofMetersPerSecond(10.0).kmPerHour, eps)
    }

    @Test fun `speed converts kmh to mps`() {
        assertEquals(10.0, Speed.ofKmPerHour(36.0).metersPerSecond, eps)
    }

    @Test fun `distance cm and m agree`() {
        assertEquals(150.0, Distance.ofMeters(1.5).centimeters, eps)
        assertEquals(1.5, Distance.ofCentimeters(150.0).meters, eps)
    }

    @Test fun `angle deg-rad roundtrip`() {
        assertEquals(90.0, Angle.ofDegrees(90.0).degrees, 1e-9)
    }
}
