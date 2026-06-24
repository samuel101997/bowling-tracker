package com.bowlingtracker.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Binds a back-camera preview to a PreviewView. Preview only (no capture). */
suspend fun bindPreviewOnly(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
) = suspendCancellableCoroutine<Unit> { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        val provider = future.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
        )
        cont.resume(Unit)
    }, ContextCompat.getMainExecutor(context))
}
