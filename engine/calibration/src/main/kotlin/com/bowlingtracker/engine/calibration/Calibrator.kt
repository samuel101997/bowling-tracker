package com.bowlingtracker.engine.calibration

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result

/**
 * Computes a [Homography] from >= 4 point correspondences via the DLT algorithm.
 * Pure & deterministic (ARCHITECTURE.md P10). Algorithm verified against a
 * NumPy SVD prototype before implementation.
 */
interface Calibrator {
    fun computeHomography(
        correspondences: List<PointCorrespondence>,
    ): Result<Homography, DomainError>
}

fun calibrator(): Calibrator = DltCalibrator()
