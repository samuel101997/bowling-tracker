package com.bowlingtracker.core.domain.port

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.Delivery
import com.bowlingtracker.core.domain.model.DeliveryId
import com.bowlingtracker.core.domain.model.Session
import com.bowlingtracker.core.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

/** Port: persistence of sessions, deliveries, calibrations. Impl: data:persistence (Room). */
interface DeliveryRepository {
    suspend fun saveDelivery(delivery: Delivery): Result<DeliveryId, DomainError>
    suspend fun getDelivery(id: DeliveryId): Result<Delivery, DomainError>
    fun observeDeliveries(sessionId: SessionId): Flow<List<Delivery>>

    suspend fun createSession(session: Session): Result<SessionId, DomainError>
    fun observeSessions(): Flow<List<Session>>

    suspend fun saveCalibration(calibration: Calibration): Result<Unit, DomainError>
    suspend fun getLatestCalibration(): Result<Calibration?, DomainError>
}
