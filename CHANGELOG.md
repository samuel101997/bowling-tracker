# Changelog

All notable changes to Bowling Tracker are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) · Versioning: [SemVer](https://semver.org/).

> Every notable change gets an entry. See [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) §4 for the rules.

## [Unreleased]

### Added
- Project scaffolding: enterprise multi-module folder structure (app / core / data / feature / engine). See [ADR-0001](docs/adr/0001-modular-architecture.md).
- `docs/ARCHITECTURE.md` — master architecture reference (principles, dependency rule, module catalogue, data flow, tech stack).
- `docs/CONTRIBUTING.md` — change-management process and Definition of Done.
- `CLAUDE.md` — operating guide for AI agents.
- ADRs: 0001 modular architecture, 0002 on-device record-then-process, 0003 single-camera accuracy bounds, 0004 BallDetector as a domain port.
- Contract template + `engine:physics` example contract; module-doc template + `engine:physics` example.
- Gradle build foundation: `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, version catalog (`gradle/libs.versions.toml`).
- `core:common` module — pure-Kotlin primitives: typed `Result`/`DomainError`, units (`Speed`/`Distance`/`Angle`/`Duration`), geometry (`Point2D`/`Vector2`), `Confidence`. Unit tests included.
- `engine:physics` module — `PhysicsAnalyzer` computing release speed, bounce/pitch point, swing, and path angles from a calibrated trajectory; returns typed errors and confidence. Known-answer fixture tests (algorithms pre-verified in a Python prototype).
- `core:domain` module — framework-free entities (Delivery, Session, Calibration, Trajectory, Insights), typed IDs, and ports (`AnalysisEngine`, `TrajectorySource`, `DeliveryRepository`, `MediaStore`); use cases `AnalyzeDeliveryUseCase`, `SaveDeliveryUseCase`. The `TrajectorySource` port keeps the multi-camera 3D seam open (ADR-0003). Use-case tests with fakes.
- `engine:calibration` module — `Calibrator` computing the image→pitch homography via DLT (self-contained Jacobi eigensolver, no external linear-algebra dep). Known-answer tests; algorithm verified against a NumPy prototype and a Python port of the in-module solver.
- `engine:tracking` module — RANSAC `TrajectoryLinker` that links per-frame detections into a clean `Trajectory` via a quadratic-in-time flight model and residual-based outlier rejection. Deterministic; verified against a Python prototype (seed-robust across 200 seeds). Known-answer tests.
- `engine:vision` module (Android library) — `BallDetector` port implementation scaffold (`OpenCvMotionBallDetector`). OpenCV/LiteRT confined here behind the port. Requires Android Studio / on-device build to compile and verify (golden-frame fixtures).
- `engine:analysis` module — `DefaultAnalysisEngine` implementing the `AnalysisEngine` port; orchestrates detect→link→calibrate→physics and maps to domain `Insights`, stamped with engine version. End-to-end pipeline test with a fake detector + real tracking/physics (144 km/h known answer).
- `core:ui` module — Compose Material3 theme (`BowlingTrackerTheme`).
- `data:media` module — `AndroidMediaStore` implementing the `MediaStore` port (passthrough frame metadata for the first runnable build; real MediaCodec extraction to follow).
- `:app` module — **walking-skeleton app**: launches, runs the real analysis engine on a synthetic straight-line delivery via a `SyntheticBallDetector`, and displays the computed speed/swing (144 km/h) on a Compose home screen. No camera/Room yet; proves the pipeline runs on-device.
- Version catalog extended with Compose/Hilt/Room/CameraX; `gradle-wrapper.properties` (Gradle 8.9). `settings.gradle.kts` trimmed to currently-buildable modules.

### Changed
- **Session workflow**: combined **Calibrate & Record** screen (live camera → tap 4 corners → pitch overlay with colored length zones snapped to taps + "Calibrated, ready" → manual Record). Multi-delivery processing: `SessionAnalyzer` segments a recorded session into N deliveries (motion-burst segmentation, verified 8/8 + seed-robust) and analyzes each. New **session results** screen: "N deliveries bowled", per-ball list (speed/zone), and a **pitch map** drawing all pitching points + flight paths over Yorker/Full/Good/Short zones. Length zones measured from batsman stumps (standard bands). New `MotionProfiler` port + `OpenCvMotionProfiler`.
- `:app` — replaced the synthetic-only home screen with a real flow: Home → Record (CameraX video capture with runtime camera permission) → Result. Recording produces a real clip; analysis still uses the synthetic detector until OpenCV detection is wired. Added CAMERA permission and CameraX deps.
- **Full app features**: stump **calibration** screen (tap 4 pitch corners → homography via engine:calibration); real **OpenCV ball detection** (`OpenCvMotionBallDetector`, org.opencv:opencv 4.11.0) on frames extracted by `data:media` (MediaMetadataRetriever); **Room persistence** of every delivery; **History** screen with session avg/max trends. OpenCV initialized in `BowlingApp`. Navigation: Home → Calibrate/Record/History → Result.

### Deprecated
- _nothing yet_

### Removed
- _nothing yet_

### Fixed
- Calibration draws a **batsman-stumps graphic** (3 stumps + bails) at the far edge of the pitch guide; taps 3 & 4 mark the batsman stump bases, whose midpoint is stored as the exact batting-end position.
- Calibration now shows the **live camera** (not a static green box) with a tap overlay and a faint pitch-outline guide, so the user aligns to the real stumps and taps the actual pitch corners.
- _nothing yet_

### Security
- _nothing yet_

---

_When you cut a release, move `[Unreleased]` items under a new `## [x.y.z] - YYYY-MM-DD` heading._
