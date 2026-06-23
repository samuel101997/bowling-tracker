package com.bowlingtracker.core.domain.usecase

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.flatMap
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.port.AnalysisEngine
import com.bowlingtracker.core.domain.port.MediaStore

/**
 * Orchestrates: clip -> frames (MediaStore) -> insights (AnalysisEngine).
 * Pure coordination over ports; no framework, fully unit-testable with fakes.
 */
class AnalyzeDeliveryUseCase(
    private val mediaStore: MediaStore,
    private val analysisEngine: AnalysisEngine,
) {
    suspend operator fun invoke(
        clipRef: String,
        calibration: Calibration,
    ): Result<Insights, DomainError> =
        mediaStore.extractFrames(clipRef).let { framesResult ->
            framesResult.flatMap { frames -> analysisEngine.analyze(frames, calibration) }
        }
}
