package com.bowlingtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.bowlingtracker.core.ui.BowlingTrackerTheme

private enum class Screen { HOME, RECORD, RESULT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BowlingTrackerTheme {
                Surface(Modifier.fillMaxSize()) { AppNav() }
            }
        }
    }
}

@Composable
private fun AppNav(viewModel: HomeViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val state by viewModel.state.collectAsState()

    when (screen) {
        Screen.HOME -> HomeScreen(
            onRecord = { screen = Screen.RECORD },
        )
        Screen.RECORD -> RecordScreen(
            onClipRecorded = { file ->
                viewModel.analyzeClip(file.absolutePath)
                screen = Screen.RESULT
            },
        )
        Screen.RESULT -> ResultScreen(
            state = state,
            onDone = { viewModel.reset(); screen = Screen.HOME },
        )
    }
}

@Composable
private fun HomeScreen(onRecord: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Bowling Tracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Record a delivery and get its speed.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRecord, modifier = Modifier.padding(top = 24.dp)) {
            Text("Record a delivery")
        }
    }
}

@Composable
private fun ResultScreen(state: AnalysisUiState, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            is AnalysisUiState.Running, AnalysisUiState.Idle ->
                CircularProgressIndicator()
            is AnalysisUiState.Done -> {
                val i = state.insights
                Text("Speed", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${"%.1f".format(i.releaseSpeed.kmPerHour)} km/h",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text("Swing: ${i.swing.direction} (${"%.2f".format(i.swing.lateral.meters)} m)")
                Text(
                    "Confidence: ${i.speedConfidence.level}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            is AnalysisUiState.Error ->
                Text("Couldn't analyze: ${state.message}")
        }
        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) {
            Text("Done")
        }
    }
}
