package com.bowlingtracker.engine.calibration

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Known-answer fixtures. The correspondences and the expected mapping were
 * verified with a NumPy SVD homography prototype (errors < 1e-9) before this
 * Kotlin DLT implementation was written.
 */
class DltCalibratorTest {

    private val calibrator = calibrator()

    // Perspective trapezoid (image px) -> pitch rectangle (metres).
    private val correspondences = listOf(
        PointCorrespondence(Point2D(100.0, 500.0), Point2D(0.0, 0.0)),
        PointCorrespondence(Point2D(300.0, 500.0), Point2D(1.5, 0.0)),
        PointCorrespondence(Point2D(260.0, 100.0), Point2D(1.5, 20.12)),
        PointCorrespondence(Point2D(140.0, 100.0), Point2D(0.0, 20.12)),
    )

    @Test fun `homography maps each correspondence to its known world point`() {
        val r = calibrator.computeHomography(correspondences)
        assertTrue(r is Result.Success, "expected success, got $r")
        val h = (r as Result.Success).value
        for (c in correspondences) {
            val got = h.apply(c.imagePx)
            assertEquals(c.worldM.x, got.x, 1e-6, "X for ${c.imagePx}")
            assertEquals(c.worldM.y, got.y, 1e-6, "Y for ${c.imagePx}")
        }
    }

    @Test fun `interior point maps to the value verified by the prototype`() {
        val h = (calibrator.computeHomography(correspondences) as Result.Success).value
        val got = h.apply(Point2D(200.0, 300.0))
        // From the NumPy prototype: (0.7500, 7.5450) metres.
        assertEquals(0.7500, got.x, 1e-4)
        assertEquals(7.5450, got.y, 1e-4)
    }

    @Test fun `fewer than four correspondences is rejected`() {
        val r = calibrator.computeHomography(correspondences.take(3))
        assertTrue(r is Result.Failure)
        assertTrue((r as Result.Failure).error is DomainError.InvalidCalibration)
    }
}
