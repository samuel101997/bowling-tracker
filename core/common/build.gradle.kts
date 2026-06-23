plugins {
    alias(libs.plugins.kotlin.jvm)
}

// core:common — pure-Kotlin foundation (ARCHITECTURE.md L0).
// MUST NOT depend on Android or any other project module.
dependencies {
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }
