# Bowling Tracker

Android app: set a phone on a tripod at the non-striker end, mark the batting-end stumps, bowl, and get **ball speed, swing, pitching point (line & length), and bowling path** — analysed on-device.

## Start here (read in order)
1. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — the master architecture & source of truth.
2. **[docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)** — how to make a change (Definition of Done).
3. **[CLAUDE.md](CLAUDE.md)** — operating guide for AI agents.
4. **[Bowling-Tracker-Plan.md](Bowling-Tracker-Plan.md)** — the original product/feasibility plan.

## Architecture in one paragraph
A strict **multi-module, layered** Android project. UI (`feature:*`) → `core:domain` → pure analysis engine (`engine:*`) + data (`data:*`) → foundation (`core:*`). Dependencies flow one way only and are **build-enforced**. The CV/physics engine is **framework-free Kotlin**, so it's unit-testable on the JVM and infrastructure (OpenCV, LiteRT, Room, CameraX) is swappable behind interfaces. Processing is **on-device, record-then-process**. See [ADR-0001](docs/adr/0001-modular-architecture.md), [ADR-0002](docs/adr/0002-on-device-record-then-process.md), [ADR-0003](docs/adr/0003-single-camera-accuracy-bounds.md).

## Repository layout
```
app/                      L5  Composition root (navigation + DI). No logic.
core/
  common/                 L0  Pure-Kotlin primitives (Result, units, geometry).
  ui/                     L0  Design system (Compose theme, reusable views).
  domain/                 L1  Entities, use cases, repository/engine PORTS.
  testing/                L0  Test fixtures & fakes.
data/
  persistence/            L3  Room repository implementation.
  media/                  L3  Clip storage + frame extraction.
engine/                       Pure analysis libraries (NO Android):
  vision/                 L2  Ball detection (OpenCV/LiteRT behind interface).
  tracking/               L2  Link detections into a trajectory.
  calibration/            L2  Homography: pixels ↔ metres from stump taps.
  physics/                L2  Speed, bounce/pitch point, swing, path angles.
  analysis/               L3  Orchestrates the engine; implements AnalysisEngine port.
feature/
  capture/                L4  High-fps recording UI + ViewModel.
  calibration/            L4  Tap-the-stumps UI.
  results/                L4  Insights UI (speed, pitch map, overlay).
  history/                L4  Session history + trend charts.
docs/
  ARCHITECTURE.md             Master reference.
  CONTRIBUTING.md             Change management & Definition of Done.
  adr/                        Architecture Decision Records (the "why").
  contracts/                  Public API contract per module.
  modules/                    Per-module internal design notes.
  diagrams/                   (architecture diagrams)
CHANGELOG.md                  Every notable change.
CLAUDE.md                     AI-agent operating guide.
```

## The two rules that keep this enterprise-grade
1. **Decoupled** — modules depend on each other only through interfaces + immutable domain models, one direction only, enforced by CI ([ARCHITECTURE.md §4](docs/ARCHITECTURE.md)).
2. **Documented** — no change is done without a CHANGELOG entry, an updated contract (if the public API changed), and an ADR for any architectural decision ([CONTRIBUTING.md](docs/CONTRIBUTING.md)).

## Accuracy honesty
Single phone → realistic targets are **±2–4 km/h** speed and ~15–40 cm line/length, every value flagged with confidence. Sub-1 km/h "broadcast grade" needs multiple cameras and is out of scope; a clean seam exists to add a second camera later ([ADR-0003](docs/adr/0003-single-camera-accuracy-bounds.md)).

## Status
Architecture & governance defined. **No application code written yet** — implementation proceeds module-by-module per the phased plan in `Bowling-Tracker-Plan.md`, each change following `CONTRIBUTING.md`.
