package com.bowlingtracker.engine.physics

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Known-answer fixtures (ARCHITECTURE.md §6.7). Expected values were
 * independently verified in a Python prototype before this code was written.
 */
class DefaultPhysicsAnalyzerTest {

    private val analyzer = physicsAnalyzer(releaseWindow = 10)

    private fun success(r: Result<PhysicsInsights, DomainError>): PhysicsInsights {
        assertTrue(r is Result.Success, "expected success, got $r")
        return (r as Result.Success).value
    }

    @Test fun `constant velocity straight ball - 40 mps = 144 kmh, no swing`() {
        // 20.0 m down-pitch in 0.5 s over 11 samples.
        val pts = (0..10).map { i ->
            TrajectoryPoint(t = 0.05 * i, position = Point2D(0.0, 2.0 * i))
        }
        val r = success(analyzer.analyze(CalibratedTrajectory(pts)))
        assertEquals(40.0, r.releaseSpeed.metersPerSecond, 1e-6)
        assertEquals(144.0, r.releaseSpeed.kmPerHour, 1e-6)
        assertEquals(Swing.Direction.NONE, r.swing.direction)
        assertEquals(0.0, r.swing.lateral.meters, 1e-9)
    }

    @Test fun `bounce detected at the height minimum`() {
        // Height parabola with minimum at index 6.
        val pts = (0..12).map { i ->
            val h = (i - 6.0) * (i - 6.0)
            TrajectoryPoint(t = 0.05 * i, position = Point2D(0.0, 1.5 * i), height = h)
        }
        val r = success(analyzer.analyze(CalibratedTrajectory(pts)))
        assertNotNull(r.pitchPoint)
        // pitch point should be the position at index 6 → y = 9.0
        assertEquals(9.0, r.pitchPoint!!.location.y, 1e-9)
    }

    @Test fun `outswing of 0.30 m is detected`() {
        val pts = (0..12).map { i ->
            val x = if (i == 6) 0.30 else 0.0
            TrajectoryPoint(t = 0.05 * i, position = Point2D(x, 1.5 * i))
        }
        val r = success(analyzer.analyze(CalibratedTrajectory(pts)))
        assertEquals(0.30, r.swing.lateral.meters, 1e-6)
        assertTrue(r.swing.direction != Swing.Direction.NONE)
    }

    @Test fun `too few points returns InsufficientTrajectory`() {
        val pts = listOf(
            TrajectoryPoint(0.0, Point2D(0.0, 0.0)),
            TrajectoryPoint(0.1, Point2D(0.0, 1.0)),
        )
        val r = analyzer.analyze(CalibratedTrajectory(pts))
        assertTrue(r is Result.Failure)
        assertTrue((r as Result.Failure).error is DomainError.InsufficientTrajectory)
    }

    @Test fun `zero time span returns DegenerateGeometry`() {
        val pts = (0..4).map { TrajectoryPoint(t = 0.0, position = Point2D(0.0, it.toDouble())) }
        val r = analyzer.analyze(CalibratedTrajectory(pts))
        assertTrue(r is Result.Failure)
        assertEquals(DomainError.DegenerateGeometry, (r as Result.Failure).error)
    }
}
