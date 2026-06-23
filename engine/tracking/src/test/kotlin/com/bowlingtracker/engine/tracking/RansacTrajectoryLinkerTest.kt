package com.bowlingtracker.engine.tracking

import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.BallDetection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Known-answer fixture mirrored from a Python RANSAC prototype: 12 clean
 * detections on a parabola + 2 gross outliers (a "bird" and a "shadow").
 * Expected: all 12 kept, both outliers removed, deterministically.
 */
class RansacTrajectoryLinkerTest {

    private val fps = 240.0
    private fun det(frame: Int, x: Double, y: Double, score: Double = 0.9) =
        BallDetection(frame, frame / fps, Point2D(x, y), radiusPx = 5.0, score = score)

    private fun cleanParabola(): List<BallDetection> =
        (0 until 12).map { i ->
            det(i, x = 100.0 + 30.0 * i, y = 400.0 - (i - 6.0) * (i - 6.0) * 2.0)
        }

    @Test fun `keeps all clean points and rejects gross outliers`() {
        val noisy = buildList {
            addAll(cleanParabola().take(5))
            add(det(5, 9999.0, 50.0, score = 0.4))   // bird
            addAll(cleanParabola().subList(5, 8))
            add(det(8, -500.0, 900.0, score = 0.3))  // shadow
            addAll(cleanParabola().subList(8, 12))
        }
        val r = trajectoryLinker().link(noisy)
        assertTrue(r is Result.Success, "expected success, got $r")
        val kept = (r as Result.Success).value.detections
        assertEquals(12, kept.size)
        val xs = kept.map { it.imagePoint.x }
        assertFalse(xs.contains(9999.0), "bird outlier must be removed")
        assertFalse(xs.contains(-500.0), "shadow outlier must be removed")
    }

    @Test fun `deterministic across runs`() {
        val noisy = cleanParabola() + det(20, 5000.0, 5000.0, 0.2)
        val a = trajectoryLinker().link(noisy)
        val b = trajectoryLinker().link(noisy)
        assertEquals(
            (a as Result.Success).value.detections.map { it.frameIndex },
            (b as Result.Success).value.detections.map { it.frameIndex },
        )
    }

    @Test fun `too few detections fails`() {
        val r = trajectoryLinker().link(listOf(det(0, 0.0, 0.0), det(1, 1.0, 1.0)))
        assertTrue(r is Result.Failure)
    }
}
