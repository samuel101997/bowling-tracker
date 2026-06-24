package com.bowlingtracker.engine.analysis

import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.DeliveryResult
import com.bowlingtracker.core.domain.model.LengthZone
import com.bowlingtracker.core.domain.model.SessionResult
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.FrameRef
import com.bowlingtracker.core.domain.port.FrameSequence
import com.bowlingtracker.core.domain.port.MotionProfiler
import com.bowlingtracker.engine.calibration.Homography

/**
 * Processes a whole recorded session: segments the clip into individual
 * deliveries (motion bursts) and analyzes each into a [DeliveryResult].
 *
 * Segmentation algorithm verified against a Python prototype (8/8 bursts).
 * Pure orchestration over ports + pure engine libs (P3/P7).
 */
class SessionAnalyzer(
    private val profiler: MotionProfiler,
    private val perDelivery: DefaultAnalysisEngine,
    private val detector: BallDetector,
    private val engineVersion: String = "0.2.0",
    private val motionThresholdFactor: Double = 2.5, // threshold = factor * median
    private val minDurationSec: Double = 0.3,
    private val mergeGapSec: Double = 0.4,
) {
    suspend fun analyzeSession(
        clip: ClipFrames,
        calibration: Calibration,
    ): Result<SessionResult, DomainError> {
        val allFrames = FrameSequence(
            frames = (0 until clip.frameCount).map { i ->
                FrameRef(i, "${clip.clipRef}/f_%04d.jpg".format(i))
            },
            fps = clip.fps,
        )

        val motion = when (val r = profiler.profile(allFrames)) {
            is Result.Success -> r.value
            is Result.Failure -> return r
        }
        val segments = segment(motion, clip.fps)
        if (segments.isEmpty()) {
            return DomainError.Unexpected("No deliveries detected in the session").asFailure()
        }

        val h = Homography(calibration.homographyRowMajor.toDoubleArray())
        val batsmanStumps = calibration.farStumpsImage.let { h.apply(it) }

        val results = ArrayList<DeliveryResult>()
        segments.forEachIndexed { idx, seg ->
            val segFrames = FrameSequence(
                frames = (seg.first until seg.second).map { i ->
                    FrameRef(i, "${clip.clipRef}/f_%04d.jpg".format(i))
                },
                fps = clip.fps,
            )
            val segClip = ClipFrames(clip.clipRef, seg.second - seg.first, clip.fps)
            // Per-delivery analysis (reuses the verified pipeline). The detector
            // is invoked through the same engine for consistency.
            val insightsRes = perDelivery.analyze(segClip, calibration)
            if (insightsRes is Result.Success) {
                val ins = insightsRes.value
                // Build the path in metres + classify length from batsman stumps.
                val path = buildPath(segFrames, h)
                val zone = ins.pitchPoint?.let {
                    val dy = kotlin.math.abs(it.location.y - batsmanStumps.y)
                    LengthZone.classify(Distance.ofMeters(dy))
                } ?: LengthZone.UNKNOWN
                results.add(
                    DeliveryResult(
                        index = idx + 1,
                        startSeconds = seg.first / clip.fps,
                        endSeconds = seg.second / clip.fps,
                        insights = ins,
                        lengthZone = zone,
                        pathMeters = path,
                    ),
                )
            }
        }
        if (results.isEmpty()) {
            return DomainError.Unexpected("Deliveries found but none could be analyzed").asFailure()
        }
        return SessionResult(results, engineVersion).asSuccess()
    }

    private fun buildPath(frames: FrameSequence, h: Homography): List<Point2D> {
        return when (val r = detector.detect(frames)) {
            is Result.Success -> r.value.map { h.apply(it.imagePoint) }
            is Result.Failure -> emptyList()
        }
    }

    /** Group above-threshold motion frames into delivery segments [start,end). */
    internal fun segment(motion: List<Double>, fps: Double): List<Pair<Int, Int>> =
        DeliverySegmenter.segment(motion, fps, motionThresholdFactor, minDurationSec, mergeGapSec)
}
