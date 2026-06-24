package com.bowlingtracker.core.domain.port

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result

/**
 * Port: produce a per-frame motion magnitude profile across a frame sequence,
 * used to segment a long session into individual deliveries. Implemented by
 * engine:vision (OpenCV frame differencing); kept as a port so engine:analysis
 * stays pure JVM (ADR-0004).
 */
interface MotionProfiler {
    /** @return motion magnitude per frame (index-aligned to frames), or error. */
    fun profile(frames: FrameSequence): Result<List<Double>, DomainError>
}
