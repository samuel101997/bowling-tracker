package com.bowlingtracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bowlingtracker.core.ui.BowlingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BowlingTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Bowling Tracker", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Walking skeleton — runs the analysis engine on a synthetic delivery.",
            style = MaterialTheme.typography.bodyMedium,
        )

        when (val s = state) {
            is HomeUiState.Idle ->
                Button(onClick = { viewModel.analyzeSampleDelivery() }, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Analyze sample delivery")
                }
            is HomeUiState.Running ->
                CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            is HomeUiState.Done -> {
                val i = s.insights
                Text(
                    "Speed: ${"%.1f".format(i.releaseSpeed.kmPerHour)} km/h",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text("Swing: ${i.swing.direction} (${"%.2f".format(i.swing.lateral.meters)} m)")
                Text("Engine: ${i.engineVersion}")
                Button(onClick = { viewModel.analyzeSampleDelivery() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Run again")
                }
            }
            is HomeUiState.Error ->
                Text("Error: ${s.message}", modifier = Modifier.padding(top = 24.dp))
        }
    }
}
