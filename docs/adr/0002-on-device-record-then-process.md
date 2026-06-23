# ADR-0002: On-device, record-then-process analysis

- **Status:** Accepted
- **Date:** 2026-06-23
- **Deciders:** Project owner (sam), architecture
- **Affected modules:** feature:capture, data:media, engine:analysis, core:domain

## Context
The app is a personal practice tool. We must choose where/when ball analysis runs: live on-device, record-then-process on-device, or offload to cloud/PC. High-fps frames arrive faster than a phone can analyse in real time; the owner values offline use and privacy; there is no budget for server infrastructure.

## Decision
We will **record a short high-fps clip of each delivery, then process the saved frames on-device** a couple of seconds later. The `AnalysisEngine` is defined as a **port** in `core:domain`, so a future remote/PC implementation can replace the on-device one without touching features or physics.

## Options considered
1. **Live real-time on-device** — drops frames under high-fps load, hurts accuracy, highest complexity. Rejected.
2. **Record-then-process on-device (chosen)** — frame-accurate (no real-time pressure), offline, private, free. Slight delay after each ball — acceptable.
3. **Cloud/PC processing** — most compute but needs internet, adds latency, costs money; unnecessary at single-user scale. Kept as a future option behind the port.

## Consequences
- Positive: accuracy (processes every frame), offline, private, zero infra cost.
- Negative: a few seconds latency per delivery; storage management for clips/frames (owned by `data:media`).
- Follow-ups: define clip/frame lifecycle + cleanup in `data:media`; engine version stamping (ARCHITECTURE.md §6.6).

## Compliance
Keeps analysis behind the `AnalysisEngine` domain port (decoupling P2/P8). No feature module knows whether processing is local or remote.
