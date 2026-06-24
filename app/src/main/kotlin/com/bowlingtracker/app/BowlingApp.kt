package com.bowlingtracker.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * Initializes OpenCV's native libraries once at startup. With the Maven
 * `org.opencv:opencv` artifact, initLocal() loads the bundled .so files.
 */
class BowlingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initLocal()) {
            Log.e("BowlingApp", "OpenCV failed to initialize")
        } else {
            Log.i("BowlingApp", "OpenCV initialized")
        }
    }
}
