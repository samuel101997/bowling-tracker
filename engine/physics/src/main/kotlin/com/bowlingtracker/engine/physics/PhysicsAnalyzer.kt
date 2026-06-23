package com.bowlingtracker.engine.physics

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result

/**
 * Turns a calibrated ball trajectory into cricket insights.
 *
 * Contract: docs/contracts/engine-physics.md.
 * Pure & deterministic (ARCHITECTURE.md P10): same input ⇒ same output,
 * no I/O, no randomness. Stateless and thread-safe.
 */
interface PhysicsAnalyzer {
    fun analyze(trajectory: CalibratedTrajectory): Result<PhysicsInsights, DomainError>
}
