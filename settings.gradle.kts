pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "bowling-tracker"

// Only modules with build files are included so Gradle can sync. The remaining
// modules (core:testing, data:persistence, feature:*) are added as they are
// implemented — see docs/ARCHITECTURE.md §5 for the full target module set.

// ---- Foundation (L0) ----
include(":core:common")
include(":core:domain")
include(":core:ui")

// ---- Data (L3) ----
include(":data:media")

// ---- Engine (L2 pure + L3 orchestrator) ----
include(":engine:vision")
include(":engine:tracking")
include(":engine:calibration")
include(":engine:physics")
include(":engine:analysis")

// ---- App (L5) ----
include(":app")
