# Contract: engine:analysis (`engine:analysis`)

- **Layer:** L3 (orchestrator)
- **Responsibility:** Implement the `AnalysisEngine` port: clip+calibration → domain `Insights`.
- **May depend on:** core:common, core:domain, engine:tracking/calibration/physics, coroutines.
- **Must NOT depend on:** Android, engine:vision (uses the `BallDetector` port instead — ADR-0004).

## Public types
| Type | Kind | Purpose |
|---|---|---|
| `DefaultAnalysisEngine` | class | Orchestrates detect→link→calibrate→physics; maps to `Insights`. |

## Construction (injected at :app)
`DefaultAnalysisEngine(detector: BallDetector, linker: TrajectoryLinker, physics: PhysicsAnalyzer, engineVersion)`.

## Guarantees
- Pure coordination over interfaces/pure libs → JVM-unit-testable with fakes (P3/P7).
- Stamps every result with `engineVersion` (P10).
- Propagates typed `DomainError` from any stage; never fabricates results (P9).
