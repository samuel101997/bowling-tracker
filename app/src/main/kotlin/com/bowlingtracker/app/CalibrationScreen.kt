package com.bowlingtracker.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

/**
 * Calibration: the user taps the 4 pitch corners in order
 * (near-left stump base, near-right, far-right, far-left). These map to the
 * known pitch rectangle in metres, giving the homography (engine:calibration).
 *
 * For this first version we calibrate on a static instruction screen with a
 * tappable area; a future version overlays this on a live/frozen camera frame.
 */
@Composable
fun CalibrationScreen(onCalibrated: (List<Offset>, Float, Float) -> Unit, onSkip: () -> Unit) {
    val taps = remember { mutableStateListOf<Offset>() }
    var width = 0f
    var height = 0f

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calibration", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Tap the 4 pitch corners in order: near-left, near-right, " +
                "far-right, far-left. (${taps.size}/4)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(Color(0xFF1B5E20))
                .pointerInput(Unit) {
                    width = size.width.toFloat()
                    height = size.height.toFloat()
                    detectTapGestures { o -> if (taps.size < 4) taps.add(o) }
                }
                .weight(1f),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                width = size.width
                height = size.height
                taps.forEach { p ->
                    drawCircle(Color.Yellow, radius = 14f, center = p)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onCalibrated(taps.toList(), width, height) },
                enabled = taps.size == 4,
            ) { Text("Use these points") }
            Button(onClick = { taps.clear() }) { Text("Reset taps") }
            Button(onClick = onSkip) { Text("Skip (use default calibration)") }
        }
    }
}
