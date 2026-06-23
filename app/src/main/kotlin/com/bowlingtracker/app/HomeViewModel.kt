package com.bowlingtracker.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.CalibrationId
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.engine.analysis.DefaultAnalysisEngine
import com.bowlingtracker.engine.physics.physicsAnalyzer
import com.bowlingtracker.engine.tracking.trajectoryLinker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the skeleton home screen. */
sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Running : HomeUiState
    data class Done(val insights: Insights) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val engine = DefaultAnalysisEngine(
        detector = SyntheticBallDetector(),
        linker = trajectoryLinker(),
        physics = physicsAnalyzer(releaseWindow = 10),
    )

    // Identity homography → image px treated as metres (skeleton only).
    private val identityCalibration = Calibration(
        id = CalibrationId("skeleton"),
        homographyRowMajor = listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0),
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 20.0),
        createdAtEpochMs = 0L,
    )

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun analyzeSampleDelivery() {
        _state.value = HomeUiState.Running
        viewModelScope.launch {
            val clip = ClipFrames(clipRef = "synthetic", frameCount = 11, fps = 20.0)
            _state.value = when (val r = engine.analyze(clip, identityCalibration)) {
                is Result.Success -> HomeUiState.Done(r.value)
                is Result.Failure -> HomeUiState.Error(r.error.message)
            }
        }
    }
}
