// engine:vision — ball detection. Unlike the other engine modules this one is
// an ANDROID LIBRARY because it uses OpenCV/LiteRT to process image frames.
// Those libs are confined here and hidden behind the BallDetector interface
// (ARCHITECTURE.md P8). The interface itself stays framework-free so consumers
// (engine:analysis) never see OpenCV/LiteRT types.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bowlingtracker.engine.vision"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.opencv)
    // OpenCV + LiteRT are added when wiring the on-device impl (see module doc).
    // implementation("org.opencv:opencv:4.10.0")
    // implementation("com.google.ai.edge.litert:litert:1.0.1")
}
