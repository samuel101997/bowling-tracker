package com.bowlingtracker.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.CalibrationId
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.engine.analysis.DefaultAnalysisEngine
import com.bowlingtracker.engine.physics.physicsAnalyzer
import com.bowlingtracker.engine.tracking.trajectoryLinker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Running : AnalysisUiState
    data class Done(val insights: Insights) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

class HomeViewModel : ViewModel() {

    // For now still uses the synthetic detector; OpenCV detection is wired in
    // the next feature. The clip path is accepted so the camera flow is real.
    private val engine = DefaultAnalysisEngine(
        detector = SyntheticBallDetector(),
        linker = trajectoryLinker(),
        physics = physicsAnalyzer(releaseWindow = 10),
    )

    private val identityCalibration = Calibration(
        id = CalibrationId("default"),
        homographyRowMajor = listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0),
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 20.0),
        createdAtEpochMs = 0L,
    )

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    /** Analyze a recorded clip. [clipPath] is the real recorded video for now
     *  passed through; frame count/fps are placeholders until extraction is wired. */
    fun analyzeClip(clipPath: String) {
        _state.value = AnalysisUiState.Running
        viewModelScope.launch {
            // clipRef encodes path|frameCount|fps for the passthrough MediaStore.
            val clip = ClipFrames(clipRef = clipPath, frameCount = 11, fps = 20.0)
            _state.value = when (val r = engine.analyze(clip, identityCalibration)) {
                is Result.Success -> AnalysisUiState.Done(r.value)
                is Result.Failure -> AnalysisUiState.Error(r.error.message)
            }
        }
    }

    fun reset() { _state.value = AnalysisUiState.Idle }
}
