package com.bowlingtracker.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bowlingtracker.app.camera.bindPreviewOnly

private val CORNER_LABELS = listOf(
    "1: near crease LEFT",
    "2: near crease RIGHT",
    "3: batsman stump RIGHT base",
    "4: batsman stump LEFT base",
)

/**
 * Live-camera calibration: point the phone at the pitch and tap the 4 corners
 * of the pitch area in order. The taps (in preview pixels) become the
 * image→pitch homography. Replaces the old static green box.
 */
@Composable
fun CalibrationScreen(
    onCalibrated: (List<Offset>, Float, Float) -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
            Text("Camera permission is required to calibrate against the stumps.")
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera access")
            }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    val taps = remember { mutableStateListOf<Offset>() }
    var w by remember { mutableStateOf(0f) }
    var h by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { bindPreviewOnly(context, lifecycleOwner, previewView) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Tap + draw overlay on top of the live camera.
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { o -> if (taps.size < 4) taps.add(o) }
                },
        ) {
            w = size.width
            h = size.height
            // Faint guide trapezoid: align the real pitch roughly to this shape,
            // then tap the actual corners for precise calibration.
            val gl = size.width * 0.30f; val gr = size.width * 0.70f
            val gtl = size.width * 0.42f; val gtr = size.width * 0.58f
            val top = size.height * 0.20f; val bot = size.height * 0.80f
            val guide = Color(0x66FFFFFF)
            drawLine(guide, Offset(gl, bot), Offset(gr, bot), strokeWidth = 3f)
            drawLine(guide, Offset(gr, bot), Offset(gtr, top), strokeWidth = 3f)
            drawLine(guide, Offset(gtr, top), Offset(gtl, top), strokeWidth = 3f)
            drawLine(guide, Offset(gtl, top), Offset(gl, bot), strokeWidth = 3f)

            // Batting-end stumps graphic at the far (top) edge of the guide:
            // three stumps + bails, centered between the far corners. Helps the
            // user see where the batsman's stumps should sit before tapping.
            val stumpsCenterX = (gtl + gtr) / 2f
            val stumpsBaseY = top
            val stumpH = size.height * 0.10f
            val stumpW = (gtr - gtl) * 0.5f
            val stumpColor = Color(0xFFFFFFFF)
            for (k in -1..1) {
                val sx = stumpsCenterX + k * (stumpW / 2f)
                drawLine(
                    stumpColor,
                    Offset(sx, stumpsBaseY),
                    Offset(sx, stumpsBaseY - stumpH),
                    strokeWidth = 6f,
                )
            }
            // bails across the top of the stumps
            drawLine(
                stumpColor,
                Offset(stumpsCenterX - stumpW / 2f, stumpsBaseY - stumpH),
                Offset(stumpsCenterX + stumpW / 2f, stumpsBaseY - stumpH),
                strokeWidth = 4f,
            )

            taps.forEachIndexed { i, p ->
                drawCircle(Color.Yellow, radius = 16f, center = p)
            }
            // connect the marked corners
            if (taps.size >= 2) {
                for (i in 0 until taps.size - 1) {
                    drawLine(Color.Yellow, taps[i], taps[i + 1], strokeWidth = 4f)
                }
                if (taps.size == 4) drawLine(Color.Yellow, taps[3], taps[0], strokeWidth = 4f)
            }
        }

        // Instructions + controls
        Surface(
            color = Color(0xCC000000),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Align the white stumps marker to the real batsman stumps, " +
                        "then tap 4 points: near crease (L,R) and the batsman " +
                        "stump bases (R,L).",
                    color = Color.White, style = TextStyle(fontSize = 13.sp),
                )
                val next = if (taps.size < 4) CORNER_LABELS[taps.size] else "all set"
                Text("Next tap → $next   (${taps.size}/4)", color = Color.Yellow)
            }
        }

        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { taps.clear() }) { Text("Reset") }
            Button(onClick = onSkip) { Text("Skip") }
            Button(
                onClick = { onCalibrated(taps.toList(), w, h) },
                enabled = taps.size == 4,
            ) { Text("Confirm") }
        }
    }
}
