plugins {
    alias(libs.plugins.kotlin.jvm)
}

// engine:physics — pure-Kotlin analysis library (ARCHITECTURE.md L2).
// MUST NOT depend on Android, OpenCV, LiteRT, or any other engine module.
// Depends only on :core:common.
dependencies {
    implementation(project(":core:common"))

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }
