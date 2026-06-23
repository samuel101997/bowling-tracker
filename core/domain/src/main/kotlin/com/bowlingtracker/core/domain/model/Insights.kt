package com.bowlingtracker.core.domain.model

import com.bowlingtracker.core.common.Angle
import com.bowlingtracker.core.common.Confidence
import com.bowlingtracker.core.common.Distance
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Speed

/**
 * The user-facing result of analysing one delivery. Engine-agnostic: the
 * `engine:analysis` module maps its own output into this domain model so the
 * UI never depends on engine types (ARCHITECTURE.md §6.3).
 */
data class Insights(
    val releaseSpeed: Speed,
    val speedConfidence: Confidence,
    val pitchPoint: PitchPoint?,
    val swing: SwingInsight,
    val releaseToPitchAngle: Angle,
    val postBounceDeviation: Angle?,
    val engineVersion: String,
)

data class PitchPoint(val location: Point2D, val confidence: Confidence)

data class SwingInsight(
    val lateral: Distance,
    val direction: SwingDirection,
    val confidence: Confidence,
)

enum class SwingDirection { IN, OUT, NONE }
