// engine:analysis — orchestrates the engine and implements the AnalysisEngine
// domain port (ARCHITECTURE.md L3). Pure kotlin-jvm: it depends on the
// framework-free engine libs and the BallDetector PORT (ADR-0004), never on the
// Android engine:vision module. The concrete detector is injected at :app.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":engine:tracking"))
    implementation(project(":engine:calibration"))
    implementation(project(":engine:physics"))
    implementation(libs.kotlin.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.kotlin.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
}
tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
