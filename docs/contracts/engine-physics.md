# Contract: engine:physics (`engine:physics`)

> Example contract, fully specified, to be used as the model for all other module contracts.
> The **public surface** of this module. Consumers depend ONLY on what is listed here.

- **Layer:** L2 (Engine-pure)
- **Responsibility (one line):** From a calibrated ball trajectory, compute speed, bounce/pitch point, swing, and bowling-path angles.
- **May depend on:** `core:common` only.
- **Must NOT depend on:** Android, CameraX, OpenCV, LiteRT, Room, any UI, any other engine module.
- **Owns these third-party libs:** none (pure Kotlin math). This module must stay dependency-light so it runs on the JVM in unit tests.

## Public types
| Type | Kind | Purpose |
|---|---|---|
| `PhysicsAnalyzer` | interface | Entry point; turns a calibrated trajectory into physics insights. |
| `CalibratedTrajectory` | data class (input) | Ball positions in real-world metres + per-point timestamps. |
| `PhysicsInsights` | data class (output) | Speed, pitch point, swing, path angles, each with confidence. |
| `Speed` | value object (from `core:common`) | Magnitude + unit (km/h / m/s). |
| `PitchPoint` | data class | Bounce location on the pitch plane (line, length) in metres. |
| `Swing` | data class | Lateral deviation magnitude + direction (IN/OUT/NONE). |
| `Confidence` | enum/float (from `core:common`) | Trust level surfaced to UI. |

## Public interface (the API)
```kotlin
interface PhysicsAnalyzer {
    /**
     * Pure, deterministic. Same input + same engine version ⇒ same output.
     * Returns typed errors instead of throwing for expected failure modes
     * (e.g. too few points, no detectable bounce).
     */
    fun analyze(trajectory: CalibratedTrajectory): Result<PhysicsInsights, DomainError>
}
```

## Inputs / Outputs
- **In:** `CalibratedTrajectory` — ordered ball positions in metres (already mapped from pixels by `engine:calibration`) with frame timestamps.
- **Out:** `PhysicsInsights` { `releaseSpeed: Speed`, `pitchPoint: PitchPoint`, `swing: Swing`, `releaseToPitchAngle: Angle`, `postBounceDeviation: Angle?`, each accompanied by `Confidence` }.
- **Errors:** `DomainError.InsufficientTrajectory`, `DomainError.NoBounceDetected`, `DomainError.DegenerateGeometry`.

## Invariants & guarantees
- **Pure & deterministic** (ARCHITECTURE.md P10). No I/O, no time-of-day, no randomness.
- **Units are explicit** via `core:common` value objects — never raw `Double` across the boundary.
- **Confidence is mandatory**: low-confidence results are returned and flagged, never silently dropped or fabricated (P9).
- **Thread-safe / stateless**: a single `PhysicsAnalyzer` may be called concurrently.

## Versioning notes
- Adding a new field to `PhysicsInsights` is additive → minor bump + CHANGELOG.
- Changing how `releaseSpeed` is computed is a semantic change → **ADR required** + CHANGELOG `Changed` + bump engine version (results stamped per delivery).
