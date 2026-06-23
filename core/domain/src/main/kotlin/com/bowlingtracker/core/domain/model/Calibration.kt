package com.bowlingtracker.core.domain.model

import com.bowlingtracker.core.common.Point2D

/**
 * Maps image pixels to the real-world pitch plane (metres).
 *
 * Built from user-tapped stump points + the known pitch geometry. The actual
 * homography math lives in `engine:calibration`; this domain model just carries
 * the resulting 3x3 matrix (row-major) plus provenance, with NO framework types.
 */
data class Calibration(
    val id: CalibrationId,
    val homographyRowMajor: List<Double>, // 9 values, image -> pitch metres
    val nearStumpsImage: Point2D,
    val farStumpsImage: Point2D,
    val createdAtEpochMs: Long,
) {
    init {
        require(homographyRowMajor.size == 9) {
            "Homography must have 9 elements, was ${homographyRowMajor.size}"
        }
    }
}

@JvmInline value class CalibrationId(val value: String)

/** Standard cricket pitch constants (ARCHITECTURE.md §3.1 / ADR-0003). */
object PitchGeometry {
    const val CREASE_TO_CREASE_M = 20.12
    const val STUMP_HEIGHT_M = 0.711
    const val STUMPS_WIDTH_M = 0.2286
}
