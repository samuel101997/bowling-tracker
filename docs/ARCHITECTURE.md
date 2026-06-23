# Bowling Tracker — Master Architecture

> **Status:** Living document. This is the single source of truth for *how the system is built*.
> Any code that contradicts this document is a bug in the code or a needed update to this document — never silently diverge.
> **Audience:** every developer and AI agent (including Claude) working on this repo. Read this **before** writing any code.

---

## 0. How to use this document

1. **Before any change**, read the relevant module contract in `docs/contracts/` and the rules here in §3–§6.
2. **Every change** must follow the change-management process in [`docs/CONTRIBUTING.md`](CONTRIBUTING.md): a CHANGELOG entry, updated module doc, and an ADR if it's an architectural decision.
3. **Never** introduce a dependency that violates the dependency rule in §4. The build is configured to fail if you do.
4. If a requirement can't be met without breaking a rule here, **stop and write an ADR proposing the change** rather than working around it.

Related documents:
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — change management, branching, definition of done.
- [`CLAUDE.md`](CLAUDE.md) — operating guide for AI agents building this app.
- [`docs/contracts/`](contracts/) — the public API contract of each module.
- [`docs/modules/`](modules/) — per-module design notes.
- [`docs/adr/`](adr/) — Architecture Decision Records (the "why").
- [`CHANGELOG.md`](../CHANGELOG.md) — every notable change, chronologically.

---

## 1. Product summary

A single Android phone on a tripod at the non-striker end records a delivery at high frame rate. After the user marks the batting-end stumps (calibration), the app analyses the recorded clip and reports **ball speed, swing direction/amount, pitching point (line & length), and bowling path**, then stores each delivery for session history and trend analysis.

**Processing model:** *record-then-process, fully on-device, offline.* See [ADR-0002](adr/0002-on-device-record-then-process.md).

**Accuracy target:** ±2–4 km/h speed; line/length within ~15–40 cm. Sub-1 km/h "broadcast grade" is **out of scope on a single camera** (physically unattainable) — the architecture leaves a clean seam for an optional second camera. See [ADR-0003](adr/0003-single-camera-accuracy-bounds.md).

---

## 2. Architectural principles (non-negotiable)

These principles drive every structural decision. They exist to keep the system **decoupled, testable, and documented** at enterprise standard.

| # | Principle | What it means in practice |
|---|---|---|
| P1 | **Strict layering / one-way dependencies** | Dependencies point inward only (UI → domain → nothing). The pure analysis engine depends on *nothing Android*. Enforced by the build (§4). |
| P2 | **Decoupling via contracts, not implementations** | Modules talk to each other only through **interfaces + data models** defined in a contract. Implementations are swappable and hidden. |
| P3 | **The analysis engine is a pure library** | The CV/physics core has **zero** Android, camera, or UI dependencies. It takes `frames + calibration` in and returns `insights` out. This makes it unit-testable on a PC/JVM. |
| P4 | **Single responsibility per module** | Each module owns exactly one concern (vision, tracking, physics, persistence, one UI feature…). No "util grab-bags" beyond `core:common`. |
| P5 | **Stable interfaces, hidden internals** | A module's `api` package is its contract. Everything else is `internal`. Changing internals must never break consumers. |
| P6 | **Everything documented** | No undocumented public API, no architectural decision without an ADR, no notable change without a CHANGELOG entry. Documentation is part of the definition of done. |
| P7 | **Testable by construction** | Pure logic in pure modules; side effects (camera, disk, ML runtime) behind interfaces so they can be faked in tests. Target ≥80% coverage on `engine:*` modules. |
| P8 | **Replaceable infrastructure** | OpenCV, LiteRT, Room, CameraX are *implementation details* behind interfaces. Swapping any of them must touch only its owning module. |
| P9 | **Fail loud, degrade gracefully** | Invalid input → typed errors (`Result`/sealed types), never silent wrong numbers. Low-confidence outputs are flagged, not hidden. |
| P10 | **Reproducible analysis** | Given the same frames + calibration + engine version, output is deterministic. Engine version is recorded with every stored delivery. |

---

## 3. System overview

```
                         ┌──────────────────────────────┐
                         │            :app              │   Assembly + DI wiring only.
                         │  (navigation, DI container)  │   No business logic.
                         └───────────────┬──────────────┘
            ┌────────────────────────────┼────────────────────────────┐
            ▼                            ▼                            ▼
   ┌─────────────────┐        ┌──────────────────┐        ┌──────────────────┐
   │ feature:capture │        │ feature:calibration│      │ feature:results  │  …feature:history
   │   (UI + VM)     │        │     (UI + VM)     │        │    (UI + VM)     │
   └────────┬────────┘        └─────────┬────────┘        └─────────┬────────┘
            │  use cases / interfaces (no impl knowledge)          │
            └──────────────────────────┬──────────────────────────┘
                                       ▼
                         ┌──────────────────────────────┐
                         │         core:domain          │  Entities, value objects,
                         │  (models, use cases, ports)  │  use cases, repository PORTS.
                         └───────────────┬──────────────┘
              ┌───────────────────────────┼───────────────────────────┐
              ▼                           ▼                           ▼
   ┌────────────────────┐    ┌──────────────────────────┐   ┌──────────────────┐
   │  data:persistence  │    │   engine:analysis        │   │   data:media     │
   │ (Room → repo impl) │    │  (orchestrates engine)   │   │ (clip/frame I/O) │
   └────────────────────┘    └────────────┬─────────────┘   └──────────────────┘
                                          │ depends on pure engine libs
                  ┌───────────────────────┼────────────────────────┐
                  ▼               ▼               ▼                ▼
         ┌──────────────┐ ┌─────────────┐ ┌──────────────┐ ┌──────────────┐
         │engine:vision │ │engine:tracking│ │engine:calibration│ │engine:physics│
         │ (detect ball)│ │ (link track) │ │ (homography) │ │ (speed/swing)│
         └──────────────┘ └─────────────┘ └──────────────┘ └──────────────┘
                  └───────────────┴───────┬───────┴────────────────┘
                                          ▼
                              ┌────────────────────────┐
                              │      core:common       │  Result types, units,
                              │  (pure kotlin only)    │  geometry primitives, logging API.
                              └────────────────────────┘
```

**Reading the diagram:** arrows = "depends on". Nothing in `engine:*` knows Android exists. `:app` knows everyone but contains no logic. Features never talk to each other — they coordinate through `core:domain`.

---

## 4. The dependency rule (enforced)

This is the backbone of decoupling. Modules are grouped into **layers**; a module may only depend on modules in a **lower** layer.

| Layer | Modules | May depend on |
|---|---|---|
| L5 App | `:app` | L0–L4 (assembly only) |
| L4 Feature (UI) | `feature:capture`, `feature:calibration`, `feature:results`, `feature:history` | `core:domain`, `core:ui`, `core:common`, `core:testing` (test) |
| L3 Data | `data:persistence`, `data:media` | `core:domain`, `core:common` |
| L3 Engine-orchestrator | `engine:analysis` | `engine:vision/tracking/calibration/physics`, `core:domain`, `core:common` |
| L2 Engine-pure | `engine:vision`, `engine:tracking`, `engine:calibration`, `engine:physics` | `core:common` only |
| L1 Domain | `core:domain` | `core:common` |
| L0 Foundation | `core:common`, `core:ui`, `core:testing` | (nothing app-specific) |

**Hard rules:**
- **No feature → feature** dependency. Ever.
- **No engine module may depend on Android, CameraX, Room, or any UI.** `engine:*` modules are pure Kotlin/JVM libraries (OpenCV/LiteRT are allowed *only* inside `engine:vision`/`engine:calibration` and hidden behind interfaces — see P8).
- **`core:domain` depends on nothing but `core:common`** and defines **ports** (interfaces) that data/engine modules implement.
- Dependencies flow **down only**. No cycles. The build enforces this (Gradle module graph + a CI check / lint rule). A violating PR must not merge.

> Why so strict? Because this is the difference between "I can change ball detection without breaking the UI" and "everything is tangled." Every rule above maps to principle P1/P2/P8.

---

## 5. Module catalogue

Each module has a one-line responsibility, an owner concern, and a contract in `docs/contracts/<module>.md`. Full per-module design lives in `docs/modules/<module>.md`.

### Foundation (L0)
- **`core:common`** — Pure-Kotlin primitives shared everywhere: `Result`/error types, units (`Speed`, `Distance`, `Angle`), geometry (`Point2D`, `Vector`), time, logging *interface* (no impl). Zero third-party heavy deps.
- **`core:ui`** — Design system: Compose theme, colors, typography, reusable components (pitch-map view, trajectory overlay, charts). No feature logic.
- **`core:testing`** — Test fixtures, fakes, sample data (e.g. a canned set of frames with a known answer), assertion helpers. Test-scope only.

### Domain (L1)
- **`core:domain`** — The heart. Entities (`Delivery`, `Session`, `Calibration`, `Trajectory`, `BallDetection`, `PitchPoint`, `Insights`), use cases (`AnalyzeDeliveryUseCase`, `SaveDeliveryUseCase`, `GetSessionTrendsUseCase`), and **ports**: `DeliveryRepository`, `MediaStore`, `AnalysisEngine`. Pure Kotlin. Knows no framework.

### Engine — pure analysis libraries (L2)
- **`engine:vision`** — Detect the ball in a single frame. Hides OpenCV/LiteRT behind a `BallDetector` interface. In: frame + region hints. Out: candidate detections with confidence.
- **`engine:tracking`** — Link per-frame detections into a coherent `Trajectory`, reject outliers (hand, birds, shadows) by physical plausibility.
- **`engine:calibration`** — Compute and apply the **homography** from user-tapped stump points + known pitch dimensions. Pixel ↔ real-world (metres) mapping. Pure math.
- **`engine:physics`** — From a calibrated trajectory: compute **speed**, detect **bounce/pitch point**, compute **swing** (lateral deviation), and **bowling path** angles. Pure math + kinematics.

### Engine — orchestration (L3)
- **`engine:analysis`** — Implements the `AnalysisEngine` port from `core:domain`. Orchestrates vision → tracking → calibration → physics into a single `Insights` result. The only engine module that knows the others exist. Stamps output with engine version (P10).

### Data (L3)
- **`data:persistence`** — Implements `DeliveryRepository` using **Room/SQLite**. Maps domain entities ↔ DB entities (mapping isolated here, never leaked). Migrations live here.
- **`data:media`** — Implements `MediaStore`: capture-clip storage, frame extraction, file lifecycle/cleanup. Hides Android storage + frame-decode details.

### Feature (UI, L4)
- **`feature:capture`** — Camera preview + high-fps recording UI and ViewModel. Uses **CameraX 1.5 high-speed API** behind an interface; produces a clip handle the domain understands.
- **`feature:calibration`** — Tap-the-stumps UI; turns taps into a `Calibration` via the domain use case.
- **`feature:results`** — Renders `Insights`: speed, pitch map (line/length), swing, trajectory overlay on video, confidence badges.
- **`feature:history`** — Session list + trend charts (speed over session, swing consistency, pitch heatmap).

### App (L5)
- **`:app`** — Single Activity, navigation graph, DI container (Hilt) wiring concrete impls to domain ports. **No business logic** — pure composition root.

---

## 6. Cross-cutting contracts & conventions

### 6.1 Public vs internal
Each module exposes a thin `api` (or top-level) package; everything else is Kotlin `internal`. The contract file in `docs/contracts/` mirrors exactly what's public. **Changing a contract = a documented, versioned event** (CHANGELOG + possibly ADR).

### 6.2 Error handling (P9)
- No exceptions across module boundaries for *expected* failures. Use `Result<T, DomainError>` (sealed `DomainError`).
- Low-confidence analysis returns `Insights` with explicit `confidence` fields; the UI must surface them, never hide them.

### 6.3 Data models & immutability
- Domain models are **immutable** `data class`es in `core:domain`. Engine and data modules map to/from their own internal models; **domain models never carry framework types** (no `Bitmap`, no Room annotations, no Compose types).

### 6.4 Dependency injection
- **Hilt**, wired only in `:app`. Modules expose interfaces; impls are bound at the composition root. This keeps every module independently testable with fakes.

### 6.5 Concurrency
- Kotlin **Coroutines + Flow**. Analysis runs on a dedicated dispatcher; UI observes state via `StateFlow`. No blocking the main thread; no shared mutable state across coroutines without a documented owner.

### 6.6 Versioning
- **Engine version** (semantic) stamped onto every stored `Delivery` (P10). Re-analysis with a newer engine is allowed and recorded.
- **DB schema version** managed by Room migrations in `data:persistence`; every migration documented + tested.

### 6.7 Testing strategy (P7)
| Module type | Required tests |
|---|---|
| `engine:*` | Unit tests with known-answer fixtures; ≥80% coverage; golden-clip regression tests for `engine:analysis`. |
| `core:domain` | Use-case unit tests with fake ports. |
| `data:*` | Room migration tests; mapper round-trip tests. |
| `feature:*` | ViewModel state tests with fake use cases; minimal UI/instrumentation for critical flows. |
| `:app` | Smoke / navigation test. |

### 6.8 Build & tooling
- Gradle **version catalog** (`gradle/libs.versions.toml`) — single source of dependency versions.
- **Convention plugins** for shared module config (avoid copy-paste build files).
- Static analysis: **ktlint** + **detekt**; module-dependency rule check in CI (fails on illegal dependency).
- CI runs: lint → unit tests → dependency-rule check → assemble. Green required to merge.

---

## 7. End-to-end data flow (one delivery)

1. **Calibrate** (`feature:calibration`): user taps stumps → `CreateCalibrationUseCase` → `engine:calibration` builds homography → stored as `Calibration`.
2. **Capture** (`feature:capture`): high-fps clip recorded via CameraX → `data:media` saves clip + extracts frames → returns `ClipHandle`.
3. **Analyse** (`AnalyzeDeliveryUseCase` → `engine:analysis`):
   `engine:vision` detects ball per frame → `engine:tracking` links into `Trajectory` → `engine:calibration` maps to metres → `engine:physics` computes speed, pitch point, swing, path → returns `Insights` (+ confidence + engine version).
4. **Persist** (`SaveDeliveryUseCase` → `data:persistence`): `Delivery` saved to Room.
5. **Present** (`feature:results`): `Insights` rendered (speed, pitch map, swing, overlay).
6. **Aggregate** (`feature:history`): trends across the session.

Each numbered step crosses a module boundary **only through a domain port or contract** — never a concrete class. That's the decoupling guarantee in action.

---

## 8. Tech stack (authoritative)

| Concern | Choice | Confined to module(s) |
|---|---|---|
| Language | Kotlin | all |
| UI | Jetpack Compose | `core:ui`, `feature:*` |
| Camera / high-fps | CameraX 1.5 high-speed API (Camera2 fallback) | `feature:capture` (behind interface) |
| Computer vision | OpenCV (Android) | `engine:vision`, `engine:calibration` (behind interfaces) |
| ML ball detector | LiteRT (ex-TFLite) + MediaPipe | `engine:vision` (behind `BallDetector`) |
| Persistence | Room / SQLite | `data:persistence` |
| Charts | Vico (or MPAndroidChart) | `core:ui` / `feature:history` |
| DI | Hilt | `:app` (wiring), interfaces everywhere |
| Async | Coroutines + Flow | all |
| Tests | JUnit5, Turbine, Robolectric (where needed), Room test | per §6.7 |

> Every third-party library above is an **implementation detail** behind an interface (P8). Swapping, e.g., OpenCV for another CV lib must not touch any module other than the one that owns it.

---

## 9. The decoupling seam for future scale

Designed-in extension points (each isolated so adding it touches one place):
- **Second camera (3D)** — `engine:analysis` consumes a `TrajectorySource`; a multi-view source can be added without touching physics or UI. See [ADR-0003](adr/0003-single-camera-accuracy-bounds.md).
- **Cloud/PC processing** — `AnalysisEngine` is a port; a remote implementation can replace the on-device one. See [ADR-0002](adr/0002-on-device-record-then-process.md).
- **iOS** — pure `engine:*` + `core:domain` are framework-free Kotlin and could target Kotlin Multiplatform later.
- **New insights** — add to `engine:physics` + extend `Insights`; results UI renders new fields. No other module changes.

---

## 10. What "done" means for any change

A change is complete only when **all** of the following are true (full checklist in [`CONTRIBUTING.md`](CONTRIBUTING.md)):

1. Code respects the dependency rule (§4) — verified by the CI check.
2. Public API change (if any) is reflected in the module's `docs/contracts/` file.
3. `CHANGELOG.md` has an entry.
4. An ADR exists if an architectural decision was made.
5. Tests added/updated per §6.7 and green.
6. The relevant `docs/modules/<module>.md` is updated.

This document and the governance docs are themselves under change control: updating architecture is a normal, expected, **documented** action — not something done by surprise.

---

## 11. Version control & credentials

- **Remote:** `https://github.com/samuel101997/bowling-tracker` — branch `main` is canonical.
- **Commit policy:** every logical change is its own commit using conventional-commit messages (`type(scope): summary`); each is reflected in `CHANGELOG.md`. See [`CONTRIBUTING.md`](CONTRIBUTING.md).
- **Authentication:** pushes use a GitHub **Personal Access Token** with *Contents: Read & write* on this repo only.

### Never commit the token
A PAT is a password equivalent. It must **never** be written into any tracked file (including this one), because committed secrets are pushed to GitHub, scanned by bots within seconds, and auto-revoked. `.gitignore` excludes `*.token` and `secrets.properties` for this reason.

**Store the token locally instead, one of:**
- **Git credential helper** (recommended): `git config --global credential.helper store` then push once and enter the token — Git caches it in `~/.git-credentials` (outside the repo).
- **Environment variable** for scripted pushes: keep it in your shell profile, not the repo.
- **`secrets.properties`** in the project root (git-ignored) if a build step ever needs it.

To rotate: revoke at <https://github.com/settings/tokens> and create a new one — no repo change required.
