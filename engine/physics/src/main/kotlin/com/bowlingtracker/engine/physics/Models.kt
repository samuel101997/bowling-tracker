package com.bowlingtracker.engine.physics

import com.bowlingtracker.core.common.Angle
import com.bowlingtracker.core.common.Confidence
import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Speed

/**
 * One sampled ball position on the pitch plane, in real-world metres.
 *
 * @param t        seconds since the first sample
 * @param position lateral (x = line) and down-pitch (y = length) in metres
 * @param height   ball height above the ground in metres, if known. Used to
 *                 detect the bounce (the minimum). Null if unavailable.
 */
data class TrajectoryPoint(
    val t: Double,
    val position: Point2D,
    val height: Double? = null,
)

/** Calibrated trajectory: ball positions already mapped pixels -> metres. */
data class CalibratedTrajectory(val points: List<TrajectoryPoint>)

/** Lateral movement of the ball through the air. */
data class Swing(val lateral: Distance, val direction: Direction, val confidence: Confidence) {
    enum class Direction { IN, OUT, NONE }
}

/** Where the ball bounced, on the pitch plane. */
data class PitchPoint(val location: Point2D, val confidence: Confidence)

/** The full physics result for one delivery. */
data class PhysicsInsights(
    val releaseSpeed: Speed,
    val speedConfidence: Confidence,
    val pitchPoint: PitchPoint?,
    val swing: Swing,
    val releaseToPitchAngle: Angle,
    val postBounceDeviation: Angle?,
)
