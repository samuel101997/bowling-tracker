package com.bowlingtracker.engine.analysis

import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.CalibrationId
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.FrameSequence
import com.bowlingtracker.engine.physics.physicsAnalyzer
import com.bowlingtracker.engine.tracking.trajectoryLinker
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * End-to-end test of the pure pipeline: a fake detector emits a clean
 * constant-velocity track; the REAL tracking + physics run; with an identity
 * homography (px == metres) the engine should report 40 m/s = 144 km/h.
 */
class DefaultAnalysisEngineTest {

    // Identity homography: image px are treated directly as metres.
    private val identity = listOf(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
    )

    private val calibration = Calibration(
        id = CalibrationId("c1"),
        homographyRowMajor = identity,
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 20.0),
        createdAtEpochMs = 0L,
    )

    // Fake detector: straight ball, 2 m per frame over 11 frames at 20 fps
    // → 20 m in 0.5 s → 40 m/s. (fps here only sets timestamps via the fake.)
    private val fakeDetector = object : BallDetector {
        override fun detect(frames: FrameSequence): Result<List<BallDetection>, Nothing> =
            (0..10).map { i ->
                BallDetection(
                    frameIndex = i,
                    tSeconds = 0.05 * i,
                    imagePoint = Point2D(0.0, 2.0 * i),
                    radiusPx = 5.0,
                    score = 0.9,
                )
            }.asSuccess()
    }

    @Test fun `pipeline computes 144 kmh for a clean straight delivery`() = runTest {
        val engine = DefaultAnalysisEngine(
            detector = fakeDetector,
            linker = trajectoryLinker(),
            physics = physicsAnalyzer(releaseWindow = 10),
        )
        val clip = ClipFrames(clipRef = "clip-1", frameCount = 11, fps = 20.0)

        val r = engine.analyze(clip, calibration)
        assertTrue(r is Result.Success, "expected success, got $r")
        val insights = (r as Result.Success).value
        assertEquals(144.0, insights.releaseSpeed.kmPerHour, 1e-3)
        assertEquals("0.1.0", insights.engineVersion)
    }
}
