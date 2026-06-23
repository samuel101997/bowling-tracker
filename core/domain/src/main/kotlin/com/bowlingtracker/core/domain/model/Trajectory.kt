package com.bowlingtracker.core.domain.model

import com.bowlingtracker.core.common.Point2D

/** A ball detected in one frame (image space, pixels). */
data class BallDetection(
    val frameIndex: Int,
    val tSeconds: Double,
    val imagePoint: Point2D,
    val radiusPx: Double,
    val score: Double,
)

/** The linked path of the ball across frames (image space). */
data class Trajectory(val detections: List<BallDetection>)
