package com.bowlingtracker.core.domain.port

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.model.Trajectory

/**
 * Port: turns a captured clip + calibration into [Insights].
 * Implemented by `engine:analysis` (on-device). Per ADR-0002 a remote/PC
 * implementation could replace it without touching features.
 */
interface AnalysisEngine {
    suspend fun analyze(clip: ClipFrames, calibration: Calibration): Result<Insights, DomainError>
}

/**
 * Port: a coherent ball [Trajectory] extracted from frames. Lets `engine:analysis`
 * accept either a single-camera source or (future) a multi-view 3D source
 * without changing physics or UI (ARCHITECTURE.md §9, ADR-0003).
 */
interface TrajectorySource {
    suspend fun extract(clip: ClipFrames): Result<Trajectory, DomainError>
}

/** Opaque handle to the extracted frames of one delivery (owned by data:media). */
data class ClipFrames(val clipRef: String, val frameCount: Int, val fps: Double)
