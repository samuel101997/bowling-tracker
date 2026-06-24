package com.bowlingtracker.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bowlingtracker.app.camera.ClipRecorder
import com.bowlingtracker.app.camera.bindPreviewOnly
import kotlinx.coroutines.launch
import java.io.File

private enum class Phase { ALIGN, CALIBRATED, RECORDING }

/** Tap-order labels for the 4 pitch corners. */
private val TAP_LABELS = listOf(
    "near crease LEFT", "near crease RIGHT", "batsman stump RIGHT", "batsman stump LEFT",
)

/**
 * Combined calibrate + record. The user aligns the tripod, taps the 4 pitch
 * corners (live camera), the pitch overlay with colored length zones snaps to
 * those corners and shows "Calibrated — ready to record", then a Record button
 * captures the session video.
 */
@Composable
fun CaptureScreen(
    onCalibrated: (taps: List<Offset>) -> Unit,
    onSessionRecorded: (File) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) { if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera permission is required.")
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Grant camera access") }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    val recorder = remember { ClipRecorder(context, lifecycleOwner) }
    val taps = remember { mutableStateListOf<Offset>() }
    var phase by remember { mutableStateOf(Phase.ALIGN) }

    LaunchedEffect(Unit) {
        // Bind the recorder (which also drives the preview) so one camera serves both.
        recorder.bind(previewView)
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Canvas(
            Modifier.fillMaxSize().pointerInput(phase) {
                if (phase == Phase.ALIGN) {
                    detectTapGestures { o ->
                        if (taps.size < 4) {
                            taps.add(o)
                            if (taps.size == 4) { phase = Phase.CALIBRATED; onCalibrated(taps.toList()) }
                        }
                    }
                }
            },
        ) {
            if (phase == Phase.ALIGN) {
                drawAlignGuide()
                taps.forEachIndexed { i, p -> drawCircle(Color.Yellow, 16f, p) }
                for (i in 0 until taps.size - 1) drawLine(Color.Yellow, taps[i], taps[i + 1], strokeWidth = 4f)
            } else {
                // Calibrated: draw the pitch with colored length zones snapped to taps.
                drawPitchZones(taps)
            }
        }

        // Top status bar
        Surface(color = Color(0xCC000000), modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding()) {
            Column(Modifier.padding(12.dp)) {
                when (phase) {
                    Phase.ALIGN -> {
                        Text("Align tripod to the pitch, then tap 4 corners.", color = Color.White, fontSize = 13.sp)
                        val next = if (taps.size < 4) TAP_LABELS[taps.size] else "done"
                        Text("Next tap → $next   (${taps.size}/4)", color = Color.Yellow)
                    }
                    Phase.CALIBRATED -> Text("✅ Calibrated — ready to record", color = Color(0xFF7CFF7C), fontSize = 16.sp)
                    Phase.RECORDING -> Text("● Recording session…", color = Color(0xFFFF6B6B), fontSize = 16.sp)
                }
            }
        }

        // Bottom controls
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            when (phase) {
                Phase.ALIGN -> Button(onClick = { taps.clear() }) { Text("Reset taps") }
                Phase.CALIBRATED -> {
                    Button(onClick = { taps.clear(); phase = Phase.ALIGN }) { Text("Recalibrate") }
                    Button(onClick = {
                        phase = Phase.RECORDING
                        recorder.start { file ->
                            phase = Phase.CALIBRATED
                            if (file != null) scope.launch { onSessionRecorded(file) }
                        }
                    }) { Text("● Record") }
                }
                Phase.RECORDING -> Button(onClick = { recorder.stop() }) { Text("■ Stop") }
            }
        }
    }
}
