package com.bowlingtracker.core.domain.port

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.BallDetection

/**
 * Port: detects the ball across a sequence of frames (image space).
 * Implemented by `engine:vision` (OpenCV/LiteRT, Android). Kept here as a
 * framework-free port so `engine:analysis` can stay pure JVM (ADR-0004).
 */
interface BallDetector {
    fun detect(frames: FrameSequence): Result<List<BallDetection>, DomainError>
}

/** A sequence of decoded frames to analyse (opaque handles). */
data class FrameSequence(val frames: List<FrameRef>, val fps: Double)

/** Opaque reference to one decoded frame (e.g. a file path or buffer id). */
data class FrameRef(val frameIndex: Int, val handle: String)
