package com.bowlingtracker.core.domain.model

import com.bowlingtracker.core.common.Distance

/**
 * Pitching length zones, measured as distance from the BATSMAN stumps toward
 * the bowler. Standard broadcast bands (ARCHITECTURE.md §3.3 + user spec).
 */
enum class LengthZone(val label: String) {
    YORKER("Yorker"),
    FULL("Full"),
    GOOD("Good"),
    SHORT("Short"),
    UNKNOWN("Unknown");

    companion object {
        // Distances from the batsman stumps, in metres.
        fun classify(distanceFromBatsmanStumps: Distance): LengthZone {
            val m = distanceFromBatsmanStumps.meters
            return when {
                m < 0.0 -> UNKNOWN
                m < 1.0 -> YORKER   // 0–1 m
                m < 3.0 -> FULL     // 1–3 m
                m < 6.0 -> GOOD     // 3–6 m
                else -> SHORT       // 6 m+
            }
        }
    }
}
