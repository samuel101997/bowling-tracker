plugins {
    alias(libs.plugins.kotlin.jvm)
}

// core:domain — entities, use cases, and ports (ARCHITECTURE.md L1).
// Depends ONLY on :core:common. No Android, no framework types.
dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlin.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.kotlin.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
