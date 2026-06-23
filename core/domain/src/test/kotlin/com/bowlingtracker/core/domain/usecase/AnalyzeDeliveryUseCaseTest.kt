package com.bowlingtracker.core.domain.usecase

import com.bowlingtracker.core.common.Angle
import com.bowlingtracker.core.common.Confidence
import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.Speed
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.CalibrationId
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.model.SwingDirection
import com.bowlingtracker.core.domain.model.SwingInsight
import com.bowlingtracker.core.domain.port.AnalysisEngine
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.MediaStore
import com.bowlingtracker.core.common.Point2D
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnalyzeDeliveryUseCaseTest {

    private val calibration = Calibration(
        id = CalibrationId("c1"),
        homographyRowMajor = List(9) { if (it % 4 == 0) 1.0 else 0.0 },
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 100.0),
        createdAtEpochMs = 0L,
    )

    private val fakeInsights = Insights(
        releaseSpeed = Speed.ofKmPerHour(130.0),
        speedConfidence = Confidence.FULL,
        pitchPoint = null,
        swing = SwingInsight(Distance.ZERO, SwingDirection.NONE, Confidence.NONE),
        releaseToPitchAngle = Angle.ZERO,
        postBounceDeviation = null,
        engineVersion = "test",
    )

    @Test fun `happy path extracts frames then analyzes`() = runTest {
        val media = object : MediaStore {
            override suspend fun extractFrames(clipRef: String) =
                ClipFrames(clipRef, frameCount = 120, fps = 240.0).asSuccess()
            override suspend fun deleteClip(clipRef: String) = Unit.asSuccess()
        }
        val engine = object : AnalysisEngine {
            override suspend fun analyze(clip: ClipFrames, calibration: Calibration) =
                fakeInsights.asSuccess()
        }
        val result = AnalyzeDeliveryUseCase(media, engine)("clip-1", calibration)
        assertTrue(result is Result.Success)
        assertEquals(130.0, (result as Result.Success).value.releaseSpeed.kmPerHour, 1e-9)
    }

    @Test fun `frame extraction failure short-circuits before analysis`() = runTest {
        var analyzeCalled = false
        val media = object : MediaStore {
            override suspend fun extractFrames(clipRef: String) =
                DomainError.Unexpected("no frames").asFailure()
            override suspend fun deleteClip(clipRef: String) = Unit.asSuccess()
        }
        val engine = object : AnalysisEngine {
            override suspend fun analyze(clip: ClipFrames, calibration: Calibration): Result<Insights, DomainError> {
                analyzeCalled = true
                return fakeInsights.asSuccess()
            }
        }
        val result = AnalyzeDeliveryUseCase(media, engine)("clip-1", calibration)
        assertTrue(result is Result.Failure)
        assertTrue(!analyzeCalled, "analysis must not run if frame extraction failed")
    }
}
