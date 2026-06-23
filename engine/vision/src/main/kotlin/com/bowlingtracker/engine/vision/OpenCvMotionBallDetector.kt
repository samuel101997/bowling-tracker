package com.bowlingtracker.engine.vision

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.FrameSequence

/**
 * Motion-based ball detector (OpenCV). Implements the domain [BallDetector]
 * port (ADR-0004).
 *
 * STATUS: scaffold. Written against the OpenCV Android API but NOT yet wired
 * (OpenCV dependency commented out in build.gradle) and CANNOT be compiled or
 * verified in the sandbox — requires an Android Studio / on-device build.
 * See docs/modules/engine-vision.md for the verification plan.
 *
 * Algorithm (ARCHITECTURE.md §3.2):
 *  1. absdiff / BackgroundSubtractor between consecutive frames (ball = motion).
 *  2. threshold + morphological open to remove noise.
 *  3. findContours; keep blobs matching ball area/circularity.
 *  4. emit the best candidate per frame as a BallDetection (image px).
 */
internal class OpenCvMotionBallDetector(
    private val minRadiusPx: Double = 3.0,
    private val maxRadiusPx: Double = 40.0,
    private val minCircularity: Double = 0.6,
) : BallDetector {

    override fun detect(frames: FrameSequence): Result<List<BallDetection>, DomainError> {
        // TODO(on-device): implement with OpenCV. Typed error until wired so a
        // caller never receives a silently-empty result (ARCHITECTURE.md P9).
        return DomainError.Unexpected(
            "OpenCvMotionBallDetector not yet wired — enable OpenCV and implement " +
                "detect(); see docs/modules/engine-vision.md.",
        ).asFailure()
    }
}
