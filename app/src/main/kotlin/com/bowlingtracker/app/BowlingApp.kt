package com.bowlingtracker.app

import android.app.Application
import com.bowlingtracker.engine.vision.VisionInit

/** Initializes OpenCV (via engine:vision, which owns the dependency). */
class BowlingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VisionInit.initOpenCv()
    }
}
