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

// ---- Foundation (L0) ----
include(":core:common")
include(":core:domain")
include(":core:ui")
include(":core:testing")

// ---- Data (L3) ----
include(":data:persistence")
include(":data:media")

// ---- Engine: pure analysis libraries (L2) + orchestrator (L3) ----
include(":engine:vision")
include(":engine:tracking")
include(":engine:calibration")
include(":engine:physics")
include(":engine:analysis")

// ---- Feature UI (L4) ----
include(":feature:capture")
include(":feature:calibration")
include(":feature:results")
include(":feature:history")

// ---- App (L5) ----
include(":app")
