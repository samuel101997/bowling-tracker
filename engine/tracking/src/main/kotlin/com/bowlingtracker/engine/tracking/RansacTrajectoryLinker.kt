package com.bowlingtracker.engine.tracking

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.model.BallDetection
import com.bowlingtracker.core.domain.model.Trajectory
import kotlin.math.hypot
import kotlin.random.Random

/**
 * RANSAC over a quadratic-in-time flight model.
 *
 * x(t) ≈ a0 + a1·t + a2·t²  (and the same for y); the ball's path is
 * near-parabolic, so genuine detections fit a low-residual quadratic while
 * birds/shadows/hand do not. We sample 3 points, fit, count inliers, keep the
 * best consensus set, then refit. Deterministic via a fixed [seed].
 */
internal class RansacTrajectoryLinker(
    private val residualThresholdPx: Double,
    private val minTrackLength: Int,
    private val iterations: Int,
    private val seed: Long,
) : TrajectoryLinker {

    override fun link(detections: List<BallDetection>): Result<Trajectory, DomainError> {
        if (detections.size < 3) {
            return DomainError.InsufficientTrajectory(detections.size, 3).asFailure()
        }
        val dets = detections.sortedBy { it.tSeconds }
        val rnd = Random(seed)

        var best: List<BallDetection> = emptyList()
        repeat(iterations) {
            val sample = pickThree(dets, rnd) ?: return@repeat
            val ts = sample.map { it.tSeconds }
            if (ts.toSet().size < 3) return@repeat
            val cx = fitQuad(ts, sample.map { it.imagePoint.x }) ?: return@repeat
            val cy = fitQuad(ts, sample.map { it.imagePoint.y }) ?: return@repeat
            val inliers = dets.filter { residual(it, cx, cy) <= residualThresholdPx }
            if (inliers.size > best.size) best = inliers
        }

        if (best.size < minTrackLength) {
            return DomainError.InsufficientTrajectory(best.size, minTrackLength).asFailure()
        }

        // Final refit on the consensus set, then a tight residual pass.
        val ts = best.map { it.tSeconds }
        val cx = fitQuad(ts, best.map { it.imagePoint.x })
        val cy = fitQuad(ts, best.map { it.imagePoint.y })
        val finalSet = if (cx != null && cy != null) {
            best.filter { residual(it, cx, cy) <= residualThresholdPx }
        } else {
            best
        }.sortedBy { it.tSeconds }

        if (finalSet.size < minTrackLength) {
            return DomainError.InsufficientTrajectory(finalSet.size, minTrackLength).asFailure()
        }
        return Trajectory(finalSet).asSuccess()
    }

    private fun pickThree(dets: List<BallDetection>, rnd: Random): List<BallDetection>? {
        if (dets.size < 3) return null
        val idx = mutableSetOf<Int>()
        var guard = 0
        while (idx.size < 3 && guard < 50) { idx.add(rnd.nextInt(dets.size)); guard++ }
        return if (idx.size == 3) idx.map { dets[it] } else null
    }

    private fun residual(d: BallDetection, cx: DoubleArray, cy: DoubleArray): Double {
        val ex = evalQuad(cx, d.tSeconds)
        val ey = evalQuad(cy, d.tSeconds)
        return hypot(d.imagePoint.x - ex, d.imagePoint.y - ey)
    }

    private fun evalQuad(c: DoubleArray, t: Double): Double = c[0] + c[1] * t + c[2] * t * t

    /** Least-squares fit of a0 + a1 t + a2 t² via 3x3 normal equations. */
    private fun fitQuad(ts: List<Double>, vs: List<Double>): DoubleArray? {
        val s = Array(3) { DoubleArray(3) }
        val b = DoubleArray(3)
        for (k in ts.indices) {
            val t = ts[k]
            val basis = doubleArrayOf(1.0, t, t * t)
            for (i in 0 until 3) {
                b[i] += basis[i] * vs[k]
                for (j in 0 until 3) s[i][j] += basis[i] * basis[j]
            }
        }
        return solve3(s, b)
    }

    /** Gaussian elimination with partial pivoting for a 3x3 system. */
    private fun solve3(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
        val m = Array(3) { i -> doubleArrayOf(a[i][0], a[i][1], a[i][2], b[i]) }
        for (c in 0 until 3) {
            var piv = c
            for (r in c until 3) if (kotlin.math.abs(m[r][c]) > kotlin.math.abs(m[piv][c])) piv = r
            val tmp = m[c]; m[c] = m[piv]; m[piv] = tmp
            val d = m[c][c]
            if (kotlin.math.abs(d) < 1e-12) return null
            for (j in 0..3) m[c][j] /= d
            for (r in 0 until 3) if (r != c) {
                val f = m[r][c]
                for (j in 0..3) m[r][j] -= f * m[c][j]
            }
        }
        return doubleArrayOf(m[0][3], m[1][3], m[2][3])
    }
}
