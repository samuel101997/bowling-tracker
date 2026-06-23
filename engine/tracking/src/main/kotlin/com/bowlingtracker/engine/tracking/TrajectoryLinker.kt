package com.bowlingtracker.engine.tracking

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.model.Trajectory

/**
 * Links noisy per-frame [BallDetection]s into a clean [Trajectory], rejecting
 * outliers (birds, hand, shadows) that don't fit a smooth flight path.
 *
 * Pure & deterministic (ARCHITECTURE.md P10): RANSAC uses a fixed seed so the
 * same input always yields the same track. Algorithm verified against a Python
 * prototype before implementation.
 */
interface TrajectoryLinker {
    fun link(detections: List<BallDetection>): Result<Trajectory, DomainError>
}

fun trajectoryLinker(
    residualThresholdPx: Double = 30.0,
    minTrackLength: Int = 4,
    iterations: Int = 200,
    seed: Long = 42L,
): TrajectoryLinker = RansacTrajectoryLinker(residualThresholdPx, minTrackLength, iterations, seed)
