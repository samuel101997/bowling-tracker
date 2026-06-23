package com.bowlingtracker.engine.calibration

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Direct Linear Transform homography.
 *
 * For each correspondence (u,v) -> (X,Y) we add two rows to A (2N x 9), then
 * find h minimizing ||A h|| subject to ||h||=1 — the eigenvector of AᵀA with
 * the smallest eigenvalue. We compute it with a Jacobi eigen-decomposition of
 * the 9x9 symmetric matrix AᵀA (no external linear-algebra dependency).
 */
internal class DltCalibrator : Calibrator {

    override fun computeHomography(
        correspondences: List<PointCorrespondence>,
    ): Result<Homography, DomainError> {
        if (correspondences.size < 4) {
            return DomainError.InvalidCalibration(
                "Need >= 4 correspondences, got ${correspondences.size}",
            ).asFailure()
        }

        // Build AᵀA (9x9) directly by accumulating each row's outer product.
        val ata = Array(9) { DoubleArray(9) }
        for (c in correspondences) {
            val (u, v) = c.imagePx.x to c.imagePx.y
            val (x, y) = c.worldM.x to c.worldM.y
            val r1 = doubleArrayOf(-u, -v, -1.0, 0.0, 0.0, 0.0, u * x, v * x, x)
            val r2 = doubleArrayOf(0.0, 0.0, 0.0, -u, -v, -1.0, u * y, v * y, y)
            accumulateOuter(ata, r1)
            accumulateOuter(ata, r2)
        }

        val (eigenvalues, eigenvectors) = jacobiEigen(ata)
        // smallest eigenvalue's eigenvector = solution h
        var minIdx = 0
        for (i in 1 until 9) if (eigenvalues[i] < eigenvalues[minIdx]) minIdx = i
        val h = DoubleArray(9) { eigenvectors[it][minIdx] }

        if (abs(h[8]) < 1e-12) return DomainError.DegenerateGeometry.asFailure()
        for (i in 0 until 9) h[i] /= h[8] // normalize so m[8] = 1
        return Homography(h).asSuccess()
    }

    private fun accumulateOuter(ata: Array<DoubleArray>, r: DoubleArray) {
        for (i in 0 until 9) {
            val ri = r[i]
            if (ri == 0.0) continue
            for (j in 0 until 9) ata[i][j] += ri * r[j]
        }
    }

    /**
     * Classic cyclic Jacobi eigen-decomposition for a real symmetric matrix.
     * Returns (eigenvalues, eigenvectors) where eigenvectors[*][k] is the
     * k-th eigenvector. Converges quadratically; ample for a 9x9.
     */
    private fun jacobiEigen(input: Array<DoubleArray>): Pair<DoubleArray, Array<DoubleArray>> {
        val n = 9
        val a = Array(n) { i -> input[i].copyOf() }
        val v = Array(n) { i -> DoubleArray(n) { j -> if (i == j) 1.0 else 0.0 } }

        repeat(100) {
            var off = 0.0
            for (p in 0 until n) for (q in p + 1 until n) off = hypot(off, a[p][q])
            if (off < 1e-14) return@repeat

            for (p in 0 until n) for (q in p + 1 until n) {
                if (abs(a[p][q]) < 1e-300) continue
                val theta = (a[q][q] - a[p][p]) / (2.0 * a[p][q])
                val t = (if (theta >= 0) 1.0 else -1.0) /
                    (abs(theta) + hypot(theta, 1.0))
                val c = 1.0 / hypot(t, 1.0)
                val s = t * c
                for (i in 0 until n) {
                    val aip = a[i][p]; val aiq = a[i][q]
                    a[i][p] = c * aip - s * aiq
                    a[i][q] = s * aip + c * aiq
                }
                for (i in 0 until n) {
                    val api = a[p][i]; val aqi = a[q][i]
                    a[p][i] = c * api - s * aqi
                    a[q][i] = s * api + c * aqi
                }
                for (i in 0 until n) {
                    val vip = v[i][p]; val viq = v[i][q]
                    v[i][p] = c * vip - s * viq
                    v[i][q] = s * vip + c * viq
                }
            }
        }
        val eig = DoubleArray(n) { a[it][it] }
        return eig to v
    }
}
