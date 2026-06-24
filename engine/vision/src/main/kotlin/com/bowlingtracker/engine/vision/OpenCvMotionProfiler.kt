package com.bowlingtracker.engine.vision

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.port.FrameSequence
import com.bowlingtracker.core.domain.port.MotionProfiler
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc

/**
 * Per-frame motion magnitude via consecutive-frame differencing (mean abs diff).
 * Used by SessionAnalyzer to segment a long clip into deliveries.
 */
class OpenCvMotionProfiler : MotionProfiler {
    override fun profile(frames: FrameSequence): Result<List<Double>, DomainError> {
        if (frames.frames.size < 2) {
            return DomainError.Unexpected("Need >= 2 frames to profile motion").asFailure()
        }
        val out = ArrayList<Double>(frames.frames.size)
        var prev: Mat? = null
        try {
            for (ref in frames.frames) {
                val color = Imgcodecs.imread(ref.handle)
                if (color.empty()) { color.release(); out.add(0.0); continue }
                val gray = Mat()
                Imgproc.cvtColor(color, gray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)
                color.release()
                val p = prev
                if (p == null) {
                    out.add(0.0)
                } else {
                    val diff = Mat()
                    Core.absdiff(p, gray, diff)
                    out.add(Core.mean(diff).`val`[0])
                    diff.release()
                    p.release()
                }
                prev = gray
            }
            prev?.release()
        } catch (e: Throwable) {
            return DomainError.Unexpected("Motion profiling failed: ${e.message}").asFailure()
        }
        return out.asSuccess()
    }
}
