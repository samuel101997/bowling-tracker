package com.bowlingtracker.app

import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.FrameSequence
import com.bowlingtracker.core.common.DomainError

/**
 * Walking-skeleton detector: instead of analysing real frames, it emits a
 * known straight-line delivery (2 m down-pitch per frame at 20 fps → 40 m/s →
 * 144 km/h with the identity calibration). Lets the REAL engine pipeline run
 * end-to-end on device and display a real computed number before the OpenCV
 * detector (engine:vision) is wired.
 */
class SyntheticBallDetector : BallDetector {
    override fun detect(frames: FrameSequence): Result<List<BallDetection>, DomainError> =
        (0..10).map { i ->
            BallDetection(
                frameIndex = i,
                tSeconds = 0.05 * i,
                imagePoint = Point2D(0.0, 2.0 * i),
                radiusPx = 5.0,
                score = 0.95,
            )
        }.asSuccess()
}
