package com.bowlingtracker.core.common

/**
 * Trust level attached to every analysis output (ARCHITECTURE.md P9).
 * The UI MUST surface low confidence rather than hiding it.
 *
 * [score] is in 0.0..1.0; [level] is a coarse bucket for display.
 */
data class Confidence(val score: Double) {
    init { require(score in 0.0..1.0) { "Confidence score must be in 0.0..1.0, was $score" } }

    val level: Level get() = when {
        score >= 0.75 -> Level.HIGH
        score >= 0.45 -> Level.MEDIUM
        else -> Level.LOW
    }

    enum class Level { LOW, MEDIUM, HIGH }

    companion object {
        val NONE = Confidence(0.0)
        val FULL = Confidence(1.0)
    }
}
