// Root build file. Plugins are declared here with `apply false`
// and applied per-module. No allprojects/subprojects blocks — each
// module owns its config to keep boundaries explicit (ARCHITECTURE.md §6.8).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
