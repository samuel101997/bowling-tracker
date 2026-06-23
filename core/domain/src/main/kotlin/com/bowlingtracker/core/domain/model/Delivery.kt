package com.bowlingtracker.core.domain.model

/** One recorded + analysed ball. */
data class Delivery(
    val id: DeliveryId,
    val sessionId: SessionId,
    val recordedAtEpochMs: Long,
    val calibrationId: CalibrationId,
    val insights: Insights,
    val clipRef: String?, // opaque handle owned by data:media; domain doesn't interpret it
)

@JvmInline value class DeliveryId(val value: String)

/** A practice session grouping many deliveries. */
data class Session(
    val id: SessionId,
    val startedAtEpochMs: Long,
    val label: String?,
)

@JvmInline value class SessionId(val value: String)
