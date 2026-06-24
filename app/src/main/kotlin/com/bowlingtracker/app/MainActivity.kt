package com.bowlingtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bowlingtracker.app.data.DeliveryEntity
import com.bowlingtracker.core.ui.BowlingTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Screen { HOME, CAPTURE, SESSION, HISTORY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BowlingTrackerTheme { Surface(Modifier.fillMaxSize()) { AppNav() } } }
    }
}

@Composable
private fun AppNav(viewModel: HomeViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val session by viewModel.session.collectAsState()
    val history by viewModel.history.collectAsState()

    when (screen) {
        Screen.HOME -> HomeScreen(
            onStart = { screen = Screen.CAPTURE },
            onHistory = { screen = Screen.HISTORY },
        )
        Screen.CAPTURE -> CaptureScreen(
            onCalibrated = { taps -> viewModel.calibrateFromTaps(taps) },
            onSessionRecorded = { file ->
                viewModel.processSession(file.absolutePath)
                screen = Screen.SESSION
            },
        )
        Screen.SESSION -> SessionResultScreen(
            state = session ?: SessionUiState.Running,
            onDone = { viewModel.resetSession(); screen = Screen.HOME },
        )
        Screen.HISTORY -> HistoryScreen(history = history, onBack = { screen = Screen.HOME })
    }
}

@Composable
private fun HomeScreen(onStart: () -> Unit, onHistory: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Bowling Tracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Set up your tripod, calibrate the pitch, and record a session.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onStart, modifier = Modifier.padding(top = 24.dp)) {
            Text("Calibrate & Record")
        }
        Button(onClick = onHistory, modifier = Modifier.padding(top = 8.dp)) { Text("History") }
    }
}

@Composable
private fun HistoryScreen(history: List<DeliveryEntity>, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        if (history.isEmpty()) {
            Text("No deliveries yet.", modifier = Modifier.padding(top = 12.dp))
        } else {
            val avg = history.map { it.speedKmh }.average()
            val max = history.maxOf { it.speedKmh }
            Text(
                "${history.size} balls · avg ${"%.1f".format(avg)} · max ${"%.1f".format(max)} km/h",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(history) { d ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${"%.1f".format(d.speedKmh)} km/h", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${fmt.format(Date(d.recordedAtEpochMs))} · swing ${d.swingDirection} · ${d.confidence}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider(Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
    }
}
