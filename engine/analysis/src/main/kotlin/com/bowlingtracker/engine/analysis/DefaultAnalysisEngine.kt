package com.bowlingtracker.engine.analysis

import com.bowlingtracker.core.common.Angle
import com.bowlingtracker.core.common.Confidence
import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.model.PitchPoint
import com.bowlingtracker.core.domain.model.SwingDirection
import com.bowlingtracker.core.domain.model.SwingInsight
import com.bowlingtracker.core.domain.port.AnalysisEngine
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.FrameRef
import com.bowlingtracker.core.domain.port.FrameSequence
import com.bowlingtracker.engine.calibration.Homography
import com.bowlingtracker.engine.physics.CalibratedTrajectory
import com.bowlingtracker.engine.physics.PhysicsAnalyzer
import com.bowlingtracker.engine.physics.Swing
import com.bowlingtracker.engine.physics.TrajectoryPoint
import com.bowlingtracker.engine.tracking.TrajectoryLinker

/**
 * Implements the [AnalysisEngine] port by orchestrating:
 *   detect (BallDetector port) -> link (TrajectoryLinker) ->
 *   map px->metres (Homography) -> physics (PhysicsAnalyzer)
 * and mapping the engine result into the framework-free domain [Insights].
 *
 * Pure coordination: every collaborator is an interface or pure-Kotlin lib, so
 * this class is unit-testable on the JVM with fakes (ARCHITECTURE.md P3/P7).
 * The concrete [BallDetector] (engine:vision, Android) is injected at :app.
 */
class DefaultAnalysisEngine(
    private val detector: BallDetector,
    private val linker: TrajectoryLinker,
    private val physics: PhysicsAnalyzer,
    private val engineVersion: String = ENGINE_VERSION,
) : AnalysisEngine {

    override suspend fun analyze(
        clip: ClipFrames,
        calibration: Calibration,
    ): Result<Insights, DomainError> {
        // clip.clipRef is the frames directory; build refs to the extracted
        // JPEGs (named f_0000.jpg ...). data:media wrote `clip.frameCount` of them.
        val sequence = FrameSequence(
            frames = (0 until clip.frameCount).map { i ->
                FrameRef(i, "${clip.clipRef}/f_%04d.jpg".format(i))
            },
            fps = clip.fps,
        )

        val detections = when (val r = detector.detect(sequence)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }

        val trajectory = when (val r = linker.link(detections)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }

        // Map each image detection to pitch-plane metres via the homography.
        val h = Homography(calibration.homographyRowMajor.toDoubleArray())
        val calibrated = CalibratedTrajectory(
            trajectory.detections.map { d ->
                val world = h.apply(d.imagePoint)
                TrajectoryPoint(t = d.tSeconds, position = world, height = null)
            },
        )

        val physicsResult = when (val r = physics.analyze(calibrated)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }

        return Insights(
            releaseSpeed = physicsResult.releaseSpeed,
            speedConfidence = physicsResult.speedConfidence,
            pitchPoint = physicsResult.pitchPoint?.let {
                PitchPoint(it.location, it.confidence)
            },
            swing = SwingInsight(
                lateral = physicsResult.swing.lateral,
                direction = physicsResult.swing.direction.toDomain(),
                confidence = physicsResult.swing.confidence,
            ),
            releaseToPitchAngle = physicsResult.releaseToPitchAngle,
            postBounceDeviation = physicsResult.postBounceDeviation,
            engineVersion = engineVersion,
        ).asSuccess()
    }

    private fun Swing.Direction.toDomain(): SwingDirection = when (this) {
        Swing.Direction.IN -> SwingDirection.IN
        Swing.Direction.OUT -> SwingDirection.OUT
        Swing.Direction.NONE -> SwingDirection.NONE
    }

    private companion object {
        const val ENGINE_VERSION = "0.1.0"
    }
}
