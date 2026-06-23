package com.bowlingtracker.core.domain.usecase

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Delivery
import com.bowlingtracker.core.domain.model.DeliveryId
import com.bowlingtracker.core.domain.port.DeliveryRepository

class SaveDeliveryUseCase(private val repository: DeliveryRepository) {
    suspend operator fun invoke(delivery: Delivery): Result<DeliveryId, DomainError> =
        repository.saveDelivery(delivery)
}
