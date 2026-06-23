package com.bowlingtracker.core.common

/**
 * Closed set of failure reasons that can cross a module boundary.
 *
 * Engine modules return these via [Result.Failure] instead of throwing,
 * so callers must handle every case (ARCHITECTURE.md P9).
 */
sealed interface DomainError {
    val message: String

    // --- Analysis / engine ---
    data class InsufficientTrajectory(val pointCount: Int, val required: Int) : DomainError {
        override val message = "Trajectory has $pointCount points; need at least $required."
    }
    data object NoBounceDetected : DomainError {
        override val message = "No bounce (vertical-motion reversal) found in the trajectory."
    }
    data object DegenerateGeometry : DomainError {
        override val message = "Geometry is degenerate (e.g. near-zero time span or collinear points)."
    }

    // --- Calibration ---
    data class InvalidCalibration(val reason: String) : DomainError {
        override val message = "Invalid calibration: $reason"
    }

    // --- Generic escape hatch (use sparingly) ---
    data class Unexpected(val reason: String) : DomainError {
        override val message = reason
    }
}
