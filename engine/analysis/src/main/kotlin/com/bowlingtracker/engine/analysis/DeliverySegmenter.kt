package com.bowlingtracker.engine.analysis

/**
 * Pure delivery segmentation: groups above-threshold motion frames into
 * delivery segments, merging close ones and dropping too-short ones.
 * Verified against a Python prototype (8/8 bursts). No dependencies → unit-testable.
 */
internal object DeliverySegmenter {
    fun segment(
        motion: List<Double>,
        fps: Double,
        thresholdFactor: Double = 2.5,
        minDurationSec: Double = 0.3,
        mergeGapSec: Double = 0.4,
    ): List<Pair<Int, Int>> {
        if (motion.isEmpty()) return emptyList()
        val median = motion.sorted()[motion.size / 2]
        val thresh = (median * thresholdFactor).coerceAtLeast(1.0)

        val raw = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (i < motion.size) {
            if (motion[i] > thresh) {
                var j = i
                while (j < motion.size && motion[j] > thresh) j++
                raw.add(i to j)
                i = j
            } else i++
        }
        val merged = ArrayList<Pair<Int, Int>>()
        for (s in raw) {
            val last = merged.lastOrNull()
            if (last != null && (s.first - last.second) / fps < mergeGapSec) {
                merged[merged.size - 1] = last.first to s.second
            } else merged.add(s)
        }
        return merged.filter { (it.second - it.first) / fps >= minDurationSec }
    }
}
