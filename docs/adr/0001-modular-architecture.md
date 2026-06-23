# ADR-0001: Adopt a strict multi-module, layered architecture

- **Status:** Accepted
- **Date:** 2026-06-23
- **Deciders:** Project owner (sam), architecture
- **Affected modules:** all

## Context
The owner requires "enterprise-level" definition where every module and change is **decoupled and documented**. A single-module Android app would couple UI, camera, CV, and persistence, making the hard part (the CV/physics analysis) impossible to test in isolation and risky to change. We need testability of pure logic, swappable infrastructure, and enforceable boundaries.

## Decision
We will structure the app as a **multi-module Gradle project** with a strict one-way dependency rule across layers (App → Feature → Domain → Engine/Data → Foundation), as defined in `ARCHITECTURE.md` §3–§5. The analysis engine is split into pure, framework-free Kotlin libraries (`engine:vision/tracking/calibration/physics`) orchestrated by `engine:analysis`. Inter-module communication happens only via interfaces + immutable domain models.

## Options considered
1. **Single-module app** — fastest to start; fails the decoupling/testability/enterprise requirement. Rejected.
2. **Layered packages in one module** — some separation but boundaries are not *enforced*; coupling creeps in. Rejected.
3. **Multi-module with enforced dependency rule (chosen)** — boundaries enforced by the build; pure engine testable on JVM; infrastructure swappable. More upfront setup, worth it.

## Consequences
- Positive: enforceable decoupling, JVM-testable analysis core, swappable OpenCV/LiteRT/Room/CameraX, clear ownership.
- Negative: more build/config overhead; need convention plugins + a CI dependency check.
- Follow-ups: ADR-0002 (processing model), ADR-0003 (accuracy/second-camera seam); set up version catalog + convention plugins.

## Compliance
Defines and is the basis for the dependency rule in `ARCHITECTURE.md` §4. All future modules must slot into a layer and obey it.
