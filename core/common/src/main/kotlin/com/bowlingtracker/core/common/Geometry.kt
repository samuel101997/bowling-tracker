package com.bowlingtracker.core.common

import kotlin.math.atan2
import kotlin.math.hypot

/** A 2D point. Used both for image pixels and for real-world plane coords (metres). */
data class Point2D(val x: Double, val y: Double) {
    operator fun minus(o: Point2D) = Vector2(x - o.x, y - o.y)
    operator fun plus(v: Vector2) = Point2D(x + v.dx, y + v.dy)
    fun distanceTo(o: Point2D): Double = hypot(x - o.x, y - o.y)
}

/** A 2D vector (a displacement / direction). */
data class Vector2(val dx: Double, val dy: Double) {
    val magnitude: Double get() = hypot(dx, dy)
    /** Signed angle of this vector from the +x axis. */
    val heading: Angle get() = Angle.ofRadians(atan2(dy, dx))
    operator fun plus(o: Vector2) = Vector2(dx + o.dx, dy + o.dy)
    operator fun times(s: Double) = Vector2(dx * s, dy * s)
}
