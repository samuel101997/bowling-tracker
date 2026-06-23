package com.bowlingtracker.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeometryTest {
    @Test fun `vector magnitude is euclidean`() {
        val v = Point2D(3.0, 4.0) - Point2D(0.0, 0.0)
        assertEquals(5.0, v.magnitude, 1e-9)
    }

    @Test fun `heading of +x axis is zero`() {
        assertEquals(0.0, Vector2(1.0, 0.0).heading.degrees, 1e-9)
    }

    @Test fun `heading of +y axis is 90 deg`() {
        assertEquals(90.0, Vector2(0.0, 1.0).heading.degrees, 1e-9)
    }
}
