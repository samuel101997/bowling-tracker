package com.bowlingtracker.engine.vision

import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * Initializes OpenCV's native libraries. Keeps the OpenCV import confined to
 * engine:vision (ARCHITECTURE.md P8) — callers (e.g. :app) invoke this without
 * referencing any OpenCV type.
 */
object VisionInit {
    fun initOpenCv(): Boolean {
        val ok = OpenCVLoader.initLocal()
        if (!ok) Log.e("VisionInit", "OpenCV failed to initialize")
        return ok
    }
}
