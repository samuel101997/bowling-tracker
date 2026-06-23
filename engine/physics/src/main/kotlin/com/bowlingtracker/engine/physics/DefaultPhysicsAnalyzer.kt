package com.bowlingtracker.engine.physics

import com.bowlingtracker.core.common.Angle
import com.bowlingtracker.core.common.Confidence
import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.Speed
import com.bowlingtracker.core.common.Vector2
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import kotlin.math.abs

/**
 * Default implementation. Internal: consumers depend on [PhysicsAnalyzer].
 *
 * @param releaseWindow number of leading segments used for the robust
 *        release-speed estimate (more points ⇒ less noise).
 */
internal class DefaultPhysicsAnalyzer(
    private val releaseWindow: Int = 5,
) : PhysicsAnalyzer {

    override fun analyze(
        trajectory: CalibratedTrajectory,
    ): Result<PhysicsInsights, DomainError> {
        val pts = trajectory.points
        if (pts.size < MIN_POINTS) {
            return DomainError.InsufficientTrajectory(pts.size, MIN_POINTS).asFailure()
        }
        val totalTime = pts.last().t - pts.first().t
        if (totalTime <= 0.0) return DomainError.DegenerateGeometry.asFailure()

        val (speed, speedConf) = releaseSpeed(pts)
        val bounceIdx = bounceIndex(pts)
        val pitchPoint = bounceIdx?.let {
            PitchPoint(pts[it].position, confidenceFromCount(pts.size))
        }
        val swing = swing(pts)
        val releaseToPitch = headingBetween(
            pts.first().position,
            (bounceIdx?.let { pts[it].position }) ?: pts.last().position,
        )
        val postBounce = bounceIdx
            ?.takeIf { it in 1 until pts.lastIndex }
            ?.let { headingBetween(pts[it].position, pts.last().position) - releaseToPitch }

        return PhysicsInsights(
            releaseSpeed = speed,
            speedConfidence = speedConf,
            pitchPoint = pitchPoint,
            swing = swing,
            releaseToPitchAngle = releaseToPitch,
            postBounceDeviation = postBounce,
        ).asSuccess()
    }

    /** Robust speed over the first [releaseWindow] segments: pathLength / Δt. */
    private fun releaseSpeed(pts: List<TrajectoryPoint>): Pair<Speed, Confidence> {
        val n = minOf(releaseWindow, pts.size - 1)
        val window = pts.take(n + 1)
        var dist = 0.0
        for (i in 0 until window.lastIndex) {
            dist += window[i].position.distanceTo(window[i + 1].position)
        }
        val dt = window.last().t - window.first().t
        val mps = if (dt > 0.0) dist / dt else 0.0
        return Speed.ofMetersPerSecond(mps) to confidenceFromCount(window.size)
    }

    /** Bounce = index of minimum height. Null if no height channel present. */
    private fun bounceIndex(pts: List<TrajectoryPoint>): Int? {
        if (pts.any { it.height == null }) return null
        var minI = 0
        var minH = Double.MAX_VALUE
        pts.forEachIndexed { i, p ->
            val h = p.height!!
            if (h < minH) { minH = h; minI = i }
        }
        // a bounce must be an interior reversal, not the first/last sample
        return minI.takeIf { it in 1 until pts.lastIndex }
    }

    /** Signed perpendicular deviation of the midpoint from the release→end line. */
    private fun swing(pts: List<TrajectoryPoint>): Swing {
        val p0 = pts.first().position
        val p1 = pts.last().position
        val mid = pts[pts.size / 2].position
        val line = p1 - p0
        val len = line.magnitude
        if (len < EPS) {
            return Swing(Distance.ZERO, Swing.Direction.NONE, Confidence.NONE)
        }
        val cross = line.dx * (mid.y - p0.y) - line.dy * (mid.x - p0.x)
        val signed = cross / len
        val dir = when {
            abs(signed) < SWING_DEADZONE_M -> Swing.Direction.NONE
            signed > 0 -> Swing.Direction.OUT
            else -> Swing.Direction.IN
        }
        return Swing(Distance.ofMeters(abs(signed)), dir, confidenceFromCount(pts.size))
    }

    private fun headingBetween(a: Point2D, b: Point2D): Angle {
        val v: Vector2 = b - a
        return if (v.magnitude < EPS) Angle.ZERO else v.heading
    }

    private fun confidenceFromCount(count: Int): Confidence {
        // More samples ⇒ more confidence, saturating at GOOD_SAMPLE_COUNT.
        val s = (count.toDouble() / GOOD_SAMPLE_COUNT).coerceIn(0.0, 1.0)
        return Confidence(s)
    }

    private companion object {
        const val MIN_POINTS = 3
        const val EPS = 1e-9
        const val SWING_DEADZONE_M = 0.02 // 2 cm: below this we report NONE
        const val GOOD_SAMPLE_COUNT = 12.0
    }
}

/** Factory — the only public way to obtain an analyzer. */
fun physicsAnalyzer(releaseWindow: Int = 5): PhysicsAnalyzer =
    DefaultPhysicsAnalyzer(releaseWindow)
