plugins {
    alias(libs.plugins.kotlin.jvm)
}

// engine:calibration — pure-Kotlin homography math (ARCHITECTURE.md L2).
// MUST NOT depend on Android/OpenCV or other engine modules. Only :core:common.
dependencies {
    implementation(project(":core:common"))

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
