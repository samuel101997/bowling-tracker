package com.bowlingtracker.engine.vision

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.port.BallDetector
import com.bowlingtracker.core.domain.port.FrameSequence
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

/**
 * Motion-based ball detector (OpenCV). For each consecutive frame pair, the
 * moving ball is isolated by frame differencing, thresholding and contour
 * analysis; the most ball-like blob (area + circularity) is emitted as a
 * [BallDetection] in image pixels.
 *
 * Each [com.bowlingtracker.core.domain.port.FrameRef.handle] is a file path to a
 * decoded frame image written by data:media. Detection quality WILL need tuning
 * against real footage (ARCHITECTURE.md ADR-0003 confidence is surfaced, never
 * faked, P9).
 */
class OpenCvMotionBallDetector(
    private val minAreaPx: Double = 12.0,
    private val maxAreaPx: Double = 4000.0,
    private val minCircularity: Double = 0.45,
) : BallDetector {

    override fun detect(frames: FrameSequence): Result<List<BallDetection>, DomainError> {
        if (frames.frames.size < 3) {
            return DomainError.InsufficientTrajectory(frames.frames.size, 3).asFailure()
        }
        val detections = mutableListOf<BallDetection>()
        var prevGray: Mat? = null
        val dtPerFrame = if (frames.fps > 0) 1.0 / frames.fps else 1.0 / 30.0

        try {
            frames.frames.forEachIndexed { index, ref ->
                val color = Imgcodecs.imread(ref.handle)
                if (color.empty()) { color.release(); return@forEachIndexed }
                val gray = Mat()
                Imgproc.cvtColor(color, gray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)
                color.release()

                val prev = prevGray
                if (prev != null) {
                    val diff = Mat()
                    Core.absdiff(prev, gray, diff)
                    Imgproc.threshold(diff, diff, 25.0, 255.0, Imgproc.THRESH_BINARY)
                    Imgproc.morphologyEx(
                        diff, diff, Imgproc.MORPH_OPEN,
                        Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, org.opencv.core.Size(3.0, 3.0)),
                    )
                    val contours = ArrayList<MatOfPoint>()
                    val hierarchy = Mat()
                    Imgproc.findContours(
                        diff, contours, hierarchy,
                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE,
                    )
                    bestBall(contours)?.let { (center, radius, score) ->
                        detections.add(
                            BallDetection(
                                frameIndex = index,
                                tSeconds = index * dtPerFrame,
                                imagePoint = center,
                                radiusPx = radius,
                                score = score,
                            ),
                        )
                    }
                    diff.release(); hierarchy.release()
                    contours.forEach { it.release() }
                    prev.release()
                }
                prevGray = gray
            }
            prevGray?.release()
        } catch (e: Throwable) {
            return DomainError.Unexpected("Detection failed: ${e.message}").asFailure()
        }

        return if (detections.size >= 3) detections.asSuccess()
        else DomainError.InsufficientTrajectory(detections.size, 3).asFailure()
    }

    /** Pick the most ball-like contour by area band + circularity. */
    private fun bestBall(contours: List<MatOfPoint>): Triple<Point2D, Double, Double>? {
        var best: Triple<Point2D, Double, Double>? = null
        var bestScore = 0.0
        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area < minAreaPx || area > maxAreaPx) continue
            val perimeter = Imgproc.arcLength(org.opencv.core.MatOfPoint2f(*c.toArray()), true)
            if (perimeter <= 0.0) continue
            val circularity = 4.0 * Math.PI * area / (perimeter * perimeter)
            if (circularity < minCircularity) continue
            val m = Imgproc.moments(c)
            if (m.m00 == 0.0) continue
            val cx = m.m10 / m.m00
            val cy = m.m01 / m.m00
            val radius = Math.sqrt(area / Math.PI)
            val score = circularity // higher = rounder = more ball-like
            if (score > bestScore) {
                bestScore = score
                best = Triple(Point2D(cx, cy), radius, score.coerceIn(0.0, 1.0))
            }
        }
        return best
    }
}
