package com.bowlingtracker.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin CameraX wrapper that owns the preview + a video Recorder and records a
 * single clip to app storage. Kept inside :app for now; if reused it would move
 * behind a port in core:domain (ARCHITECTURE.md P8).
 *
 * NOTE: written against the CameraX 1.5 API. Cannot be compiled in the planning
 * sandbox — verified by building in Android Studio / running on device.
 */
class ClipRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    /** Bind preview + video capture to the given PreviewView. */
    suspend fun bind(previewView: PreviewView) = suspendCancellableCoroutine<Unit> { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            // Prefer the highest quality the device supports (high fps where available).
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.HIGHEST),
                )
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture,
            )
            cont.resume(Unit)
        }, ContextCompat.getMainExecutor(context))
    }

    /** Start recording to a file in app-specific storage. */
    fun start(onFinalized: (File?) -> Unit) {
        val vc = videoCapture ?: run { onFinalized(null); return }
        val outFile = File(context.filesDir, "delivery_${System.currentTimeMillis()}.mp4")
        val options = androidx.camera.video.FileOutputOptions.Builder(outFile).build()

        recording = vc.output
            .prepareRecording(context, options)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) onFinalized(null) else onFinalized(outFile)
                }
            }
    }

    fun stop() {
        recording?.stop()
        recording = null
    }
}
