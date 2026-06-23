package com.bowlingtracker.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bowlingtracker.app.camera.ClipRecorder
import kotlinx.coroutines.launch
import java.io.File

/**
 * Records a real delivery clip with CameraX, then hands the file path back via
 * [onClipRecorded]. Handles the runtime camera permission.
 */
@Composable
fun RecordScreen(onClipRecorded: (File) -> Unit) {
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

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Camera permission is required to record a delivery.")
            Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant camera access")
            }
        }
        return
    }

    val recorder = remember { ClipRecorder(context, lifecycleOwner) }
    var isRecording by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) { recorder.bind(previewView) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = {
                if (!isRecording) {
                    isRecording = true
                    recorder.start { file ->
                        isRecording = false
                        if (file != null) scope.launch { onClipRecorded(file) }
                    }
                } else {
                    recorder.stop()
                }
            }) {
                Text(if (isRecording) "Stop" else "Record delivery")
            }
        }
    }
}
