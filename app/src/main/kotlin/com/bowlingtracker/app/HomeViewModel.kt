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
import com.bowlingtracker.data.media.AndroidMediaStore
import com.bowlingtracker.engine.analysis.DefaultAnalysisEngine
import com.bowlingtracker.engine.analysis.SessionAnalyzer
import com.bowlingtracker.engine.calibration.PointCorrespondence
import com.bowlingtracker.engine.calibration.calibrator
import com.bowlingtracker.engine.physics.physicsAnalyzer
import com.bowlingtracker.engine.tracking.trajectoryLinker
import com.bowlingtracker.engine.vision.OpenCvMotionBallDetector
import com.bowlingtracker.engine.vision.OpenCvMotionProfiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val mediaStore = AndroidMediaStore(app, maxFrames = 150) // longer sessions
    private val detector = OpenCvMotionBallDetector()
    private val perDelivery = DefaultAnalysisEngine(
        detector = detector,
        linker = trajectoryLinker(),
        physics = physicsAnalyzer(releaseWindow = 10),
    )
    private val sessionAnalyzer = SessionAnalyzer(
        profiler = OpenCvMotionProfiler(),
        perDelivery = perDelivery,
        detector = detector,
    )
    private val store = DeliveryStore(app)
    private val calib = calibrator()

    private var calibration: Calibration = identity()

    private val _session = MutableStateFlow<SessionUiState?>(null)
    val session: StateFlow<SessionUiState?> = _session.asStateFlow()

    val history: StateFlow<List<DeliveryEntity>> =
        store.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun calibrateFromTaps(taps: List<Offset>) {
        if (taps.size != 4) return
        val width = 1.37; val length = 20.12
        val world = listOf(
            Point2D(0.0, 0.0), Point2D(width, 0.0), Point2D(width, length), Point2D(0.0, length),
        )
        val corr = taps.mapIndexed { i, o ->
            PointCorrespondence(Point2D(o.x.toDouble(), o.y.toDouble()), world[i])
        }
        when (val r = calib.computeHomography(corr)) {
            is Result.Success -> {
                val farMid = Point2D((taps[2].x + taps[3].x) / 2.0, (taps[2].y + taps[3].y) / 2.0)
                val nearMid = Point2D((taps[0].x + taps[1].x) / 2.0, (taps[0].y + taps[1].y) / 2.0)
                calibration = Calibration(
                    id = CalibrationId("user"),
                    homographyRowMajor = r.value.m.toList(),
                    nearStumpsImage = nearMid,
                    farStumpsImage = farMid,
                    createdAtEpochMs = System.currentTimeMillis(),
                )
            }
            is Result.Failure -> { /* keep previous */ }
        }
    }

    fun processSession(clipPath: String) {
        _session.value = SessionUiState.Running
        viewModelScope.launch {
            val ui = try {
                withContext(Dispatchers.Default) {
                    val framesRes = mediaStore.extractFrames(clipPath)
                    when (framesRes) {
                        is Result.Success ->
                            when (val r = sessionAnalyzer.analyzeSession(framesRes.value, calibration)) {
                                is Result.Success -> {
                                    r.value.deliveries.forEach { store.save(it.insights) }
                                    SessionUiState.Done(r.value)
                                }
                                is Result.Failure -> SessionUiState.Error(r.error.message)
                            }
                        is Result.Failure -> SessionUiState.Error(framesRes.error.message)
                    }
                }
            } catch (e: Throwable) {
                // Never crash: surface the failure so the user sees it (P9).
                SessionUiState.Error("Processing failed: ${e.message ?: e.javaClass.simpleName}")
            }
            _session.value = ui
        }
    }

    fun resetSession() { _session.value = null }

    private fun identity() = Calibration(
        id = CalibrationId("default"),
        homographyRowMajor = listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0),
        nearStumpsImage = Point2D(0.0, 0.0),
        farStumpsImage = Point2D(0.0, 20.0),
        createdAtEpochMs = 0L,
    )
}
