package com.bowlingtracker.core.common

import kotlin.math.PI

/**
 * Typed units. Crossing a module boundary with a raw [Double] is forbidden
 * (ARCHITECTURE.md §6.3) — use these value objects so a metres value can never
 * be mistaken for a km/h value.
 */

@JvmInline
value class Distance private constructor(val meters: Double) {
    val centimeters: Double get() = meters * 100.0
    operator fun plus(o: Distance) = Distance(meters + o.meters)
    operator fun minus(o: Distance) = Distance(meters - o.meters)
    companion object {
        val ZERO = Distance(0.0)
        fun ofMeters(m: Double) = Distance(m)
        fun ofCentimeters(cm: Double) = Distance(cm / 100.0)
    }
}

@JvmInline
value class Speed private constructor(val metersPerSecond: Double) {
    val kmPerHour: Double get() = metersPerSecond * 3.6
    val mph: Double get() = metersPerSecond * 2.2369362921
    companion object {
        val ZERO = Speed(0.0)
        fun ofMetersPerSecond(mps: Double) = Speed(mps)
        fun ofKmPerHour(kmh: Double) = Speed(kmh / 3.6)
    }
}

/** Angle stored in radians; degrees exposed for UI. */
@JvmInline
value class Angle private constructor(val radians: Double) {
    val degrees: Double get() = radians * 180.0 / PI
    operator fun minus(o: Angle) = Angle(radians - o.radians)
    companion object {
        val ZERO = Angle(0.0)
        fun ofRadians(r: Double) = Angle(r)
        fun ofDegrees(d: Double) = Angle(d * PI / 180.0)
    }
}

/** Seconds as a typed value to avoid mixing with other doubles. */
@JvmInline
value class Duration private constructor(val seconds: Double) {
    operator fun minus(o: Duration) = Duration(seconds - o.seconds)
    companion object {
        val ZERO = Duration(0.0)
        fun ofSeconds(s: Double) = Duration(s)
        fun ofMillis(ms: Double) = Duration(ms / 1000.0)
    }
}
