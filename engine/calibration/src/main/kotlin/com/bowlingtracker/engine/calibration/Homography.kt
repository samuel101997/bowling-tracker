package com.bowlingtracker.engine.calibration

import com.bowlingtracker.core.common.Point2D

/**
 * A 3x3 projective transform mapping image pixels -> pitch-plane metres.
 * Stored row-major (9 values). Apply maps (u,v,1) then divides by w.
 */
data class Homography(val m: DoubleArray) {
    init { require(m.size == 9) { "Homography needs 9 elements, was ${m.size}" } }

    fun apply(p: Point2D): Point2D {
        val x = m[0] * p.x + m[1] * p.y + m[2]
        val y = m[3] * p.x + m[4] * p.y + m[5]
        val w = m[6] * p.x + m[7] * p.y + m[8]
        return Point2D(x / w, y / w)
    }

    override fun equals(other: Any?) = other is Homography && m.contentEquals(other.m)
    override fun hashCode() = m.contentHashCode()
}

/** A pixel point paired with its known real-world position in metres. */
data class PointCorrespondence(val imagePx: Point2D, val worldM: Point2D)
