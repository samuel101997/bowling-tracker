# ADR-0004: Move the BallDetector abstraction to a domain port

- **Status:** Accepted
- **Date:** 2026-06-23
- **Deciders:** Project owner (sam), architecture
- **Affected modules:** core:domain, engine:vision, engine:analysis

## Context
`engine:analysis` orchestrates the pipeline and must stay a pure kotlin-jvm
library so its coordination logic is unit-testable on the JVM. It needs to call
ball detection, but the detector implementation (`engine:vision`) is an **Android
library** (OpenCV/LiteRT). A JVM module cannot depend on an Android library, and
even if it could, doing so would drag Android into the orchestrator — violating
P1/P3 (engine stays framework-free) and P8 (infrastructure hidden behind
interfaces).

## Decision
Define the **`BallDetector` interface and its DTOs (`FrameSequence`, `FrameRef`)
as a port in `core:domain`**. `engine:analysis` depends only on that port (pure
JVM). `engine:vision` *implements* the port using OpenCV/LiteRT and is wired to
the orchestrator at the composition root (`:app`) via DI — never by a direct
module dependency from analysis to vision.

## Options considered
1. **analysis → vision (Android) direct dependency** — illegal (JVM→Android) and
   couples the orchestrator to OpenCV. Rejected.
2. **Make analysis an Android module too** — loses JVM unit-testability of the
   orchestration logic (P3/P7). Rejected.
3. **BallDetector as a domain port (chosen)** — analysis stays pure JVM, vision
   stays the sole owner of OpenCV/LiteRT, wiring happens in :app. Upholds P1/P3/P8.

## Consequences
- Positive: orchestrator unit-testable with a fake detector; no Android leak.
- Negative: the detector's frame DTOs live in domain (slightly more in domain),
  acceptable since they are framework-free handles.
- Follow-up: `engine:vision` now depends on `core:domain` and implements the port;
  `engine:analysis` does NOT depend on `engine:vision`.

## Compliance
Restores the dependency rule (ARCHITECTURE.md §4): no engine→Android edge, no
JVM→Android edge. Detection is now decoupled exactly like the other ports
(AnalysisEngine, DeliveryRepository, MediaStore).
