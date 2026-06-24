package com.bowlingtracker.core.domain.model

import com.bowlingtracker.core.common.Point2D

/** One detected delivery within a session: its insights + path on the pitch plane. */
data class DeliveryResult(
    val index: Int,                       // 1-based delivery number
    val startSeconds: Double,
    val endSeconds: Double,
    val insights: Insights,
    val lengthZone: LengthZone,
    /** Ball positions on the pitch plane (metres) for drawing the flight path. */
    val pathMeters: List<Point2D>,
)

/** The result of processing a whole recorded session. */
data class SessionResult(
    val deliveries: List<DeliveryResult>,
    val engineVersion: String,
) {
    val count: Int get() = deliveries.size
}
