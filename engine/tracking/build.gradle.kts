plugins {
    alias(libs.plugins.kotlin.jvm)
}

// engine:tracking — pure-Kotlin trajectory linking (ARCHITECTURE.md L2).
// Depends only on :core:common and :core:domain models. No Android.
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(17) }
