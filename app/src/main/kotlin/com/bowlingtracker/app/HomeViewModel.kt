package com.bowlingtracker.app

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bowlingtracker.app.data.DeliveryEntity
import com.bowlingtracker.app.data.DeliveryStore
import com.bowlingtracker.core.common.Point2D
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.domain.model.Calibration
import com.bowlingtracker.core.domain.model.CalibrationId
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.model.PitchGeometry
import com.bowlingtracker.data.media.AndroidMediaStore
import com.bowlingtracker.engine.analysis.DefaultAnalysisEngine
import com.bowlingtracker.engine.calibration.PointCorrespondence
import com.bowlingtracker.engine.calibration.calibrator
import com.bowlingtracker.engine.physics.physicsAnalyzer
import com.bowlingtracker.engine.tracking.trajectoryLinker
import com.bowlingtracker.engine.vision.OpenCvMotionBallDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Running : AnalysisUiState
    data class Done(val insights: Insights) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val mediaStore = AndroidMediaStore(app)
    private val engine = DefaultAnalysisEngine(
        detector = OpenCvMotionBallDetector(),
        linker = trajectoryLinker(),
        physics = physicsAnalyzer(releaseWindow = 10),
    )
    private val store = DeliveryStore(app)
    private val calib = calibrator()

    /** Current calibration; defaults to identity until the user calibrates. */
    private var calibration: Calibration = identity()

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    val history: StateFlow<List<DeliveryEntity>> =
        store.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Build a homography from the 4 tapped pitch corners (image px) → metres. */
    fun calibrateFromTaps(taps: List<Offset>) {
        if (taps.size != 4) return
        val w = PitchGeometry.STUMPS_WIDTH_M * 6 // ~1.37 m usable lateral band
        val l = PitchGeometry.CREASE_TO_CREASE_M
        // world rectangle: near-left(0,0) near-right(w,0) far-right(w,l) far-left(0,l)
        val world = listOf(
            Point2D(0.0, 0.0), Point2D(w, 0.0), Point2D(w, l), Point2D(0.0, l),
        )
        val corr = taps.mapIndexed { i, o ->
            PointCorrespondence(Point2D(o.x.toDouble(), o.y.toDouble()), world[i])
        }
        when (val r = calib.computeHomography(corr)) {
            is Result.Success -> {
                // near stumps midpoint = between taps 0 (near-L) and 1 (near-R)
                val nearMid = Point2D(
                    (taps[0].x + taps[1].x) / 2.0,
                    (taps[0].y + taps[1].y) / 2.0,
                )
                // BATSMAN stumps midpoint = between taps 2 (far-R) and 3 (far-L)
                val farMid = Point2D(
                    (taps[2].x + taps[3].x) / 2.0,
                    (taps[2].y + taps[3].y) / 2.0,
                )
                calibration = Calibration(
                    id = CalibrationId("user"),
                    homographyRowMajor = r.value.m.toList(),
                    nearStumpsImage = nearMid,
                    farStumpsImage = farMid, // exact batsman-stumps location in image
                    createdAtEpochMs = System.currentTimeMillis(),
                )
            }
            is Result.Failure -> { /* keep previous calibration */ }
        }
    }

    fun analyzeClip(clipPath: String) {
        _state.value = AnalysisUiState.Running
        viewModelScope.launch {
            val framesResult = mediaStore.extractFrames(clipPath)
            val result = when (framesResult) {
                is Result.Success -> engine.analyze(framesResult.value, calibration)
                is Result.Failure -> framesResult
            }
            _state.value = when (result) {
                is Result.Success -> {
                    store.save(result.value)
                    AnalysisUiState.Done(result.value)
                }
                is Result.Failure -> AnalysisUiState.Error(result.error.message)
            }
        }
    }

    fun reset() { _state.value = AnalysisUiState.Idle }

    private fun identity() = Calibration(
        id = CalibrationId("default"),
        homographyRowMajor = listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0),
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 20.0),
        createdAtEpochMs = 0L,
    )
}
