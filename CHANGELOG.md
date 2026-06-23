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
- ADRs: 0001 modular architecture, 0002 on-device record-then-process, 0003 single-camera accuracy bounds.
- Contract template + `engine:physics` example contract; module-doc template + `engine:physics` example.
- Gradle build foundation: `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, version catalog (`gradle/libs.versions.toml`).
- `core:common` module — pure-Kotlin primitives: typed `Result`/`DomainError`, units (`Speed`/`Distance`/`Angle`/`Duration`), geometry (`Point2D`/`Vector2`), `Confidence`. Unit tests included.
- `engine:physics` module — `PhysicsAnalyzer` computing release speed, bounce/pitch point, swing, and path angles from a calibrated trajectory; returns typed errors and confidence. Known-answer fixture tests (algorithms pre-verified in a Python prototype).

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
