package com.bowlingtracker.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bowlingtracker.core.domain.model.DeliveryResult
import com.bowlingtracker.core.domain.model.LengthZone
import com.bowlingtracker.core.domain.model.SessionResult

sealed interface SessionUiState {
    data object Running : SessionUiState
    data class Done(val result: SessionResult) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

private fun zoneColor(z: LengthZone) = when (z) {
    LengthZone.YORKER -> Color(0xFFE0C341)
    LengthZone.FULL -> Color(0xFF43A047)
    LengthZone.GOOD -> Color(0xFFD32F2F)
    LengthZone.SHORT -> Color(0xFF1565C0)
    LengthZone.UNKNOWN -> Color.Gray
}

@Composable
fun SessionResultScreen(state: SessionUiState, onDone: () -> Unit) {
    when (state) {
        SessionUiState.Running -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Text("Processing session — detecting deliveries…", Modifier.padding(top = 16.dp))
        }
        is SessionUiState.Error -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Couldn't process session: ${state.message}")
            Button(onClick = onDone, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
        }
        is SessionUiState.Done -> SessionDone(state.result, onDone)
    }
}

@Composable
private fun SessionDone(result: SessionResult, onDone: () -> Unit) {
    var showAll by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${result.count} deliveries bowled", style = MaterialTheme.typography.headlineSmall)

        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAll = true; selected = null }) { Text("All paths") }
            Button(onClick = { showAll = false }) { Text("Per delivery") }
        }

        // Pitch map with colored zones + pitching points + (optional) flight paths
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.7f).background(Color(0xFF2E5D34)),
        ) {
            PitchMap(
                deliveries = result.deliveries,
                showAll = showAll,
                selected = selected,
            )
        }

        Text("Deliveries", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(result.deliveries) { d ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.padding(end = 8.dp)) {
                        Canvas(Modifier.padding(2.dp)) { drawCircle(zoneColor(d.lengthZone), 14f) }
                    }
                    Column {
                        Text(
                            "#${d.index}  ${"%.1f".format(d.insights.releaseSpeed.kmPerHour)} km/h  · ${d.lengthZone.label}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "swing ${d.insights.swing.direction} · ${d.insights.speedConfidence.level}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.padding(top = 8.dp)) { Text("Done") }
    }
}

/**
 * Draws the pitch map: colored length zones (Yorker→Short top-to-bottom),
 * each delivery's pitching point as a dot, and flight paths when shown.
 * Pitch coords are in metres (y = down-pitch from batsman stumps); we map to
 * the canvas with the batsman stumps at the top.
 */
@Composable
private fun PitchMap(deliveries: List<DeliveryResult>, showAll: Boolean, selected: Int?) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // Length zones as horizontal bands (top = batsman stumps).
        val bands = listOf(
            0f to 0.06f to LengthZone.YORKER,
            0.06f to 0.17f to LengthZone.FULL,
            0.17f to 0.34f to LengthZone.GOOD,
            0.34f to 1.0f to LengthZone.SHORT,
        )
        bands.forEach { (range, zone) ->
            val (t0, t1) = range
            drawRect(
                color = zoneColor(zone).copy(alpha = 0.5f),
                topLeft = Offset(0f, h * t0),
                size = androidx.compose.ui.geometry.Size(w, h * (t1 - t0)),
            )
        }
        // Map metres → canvas: assume usable length ~18 m, lateral ~3 m.
        val maxLen = 18.0; val maxLat = 3.0
        fun toCanvas(xm: Double, ym: Double): Offset {
            val cx = (w / 2f) + ((xm / maxLat) * (w * 0.4f)).toFloat()
            val cy = ((ym / maxLen) * h).toFloat().coerceIn(0f, h)
            return Offset(cx, cy)
        }
        deliveries.forEach { d ->
            val show = showAll || selected == d.index
            if (!show) return@forEach
            // flight path
            if (d.pathMeters.size >= 2) {
                for (i in 0 until d.pathMeters.size - 1) {
                    val a = d.pathMeters[i]; val b = d.pathMeters[i + 1]
                    drawLine(Color.White.copy(alpha = 0.7f), toCanvas(a.x, a.y), toCanvas(b.x, b.y), strokeWidth = 3f)
                }
            }
            // pitching point
            d.insights.pitchPoint?.let {
                drawCircle(zoneColor(d.lengthZone), 12f, toCanvas(it.location.x, it.location.y))
            }
        }
    }
}
