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

### Changed
- _nothing yet_

### Deprecated
- _nothing yet_

### Removed
- _nothing yet_

### Fixed
- _nothing yet_

### Security
- _nothing yet_

---

_When you cut a release, move `[Unreleased]` items under a new `## [x.y.z] - YYYY-MM-DD` heading._
