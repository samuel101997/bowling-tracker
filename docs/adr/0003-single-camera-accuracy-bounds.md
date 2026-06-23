# ADR-0003: Single-camera accuracy bounds and the second-camera seam

- **Status:** Accepted
- **Date:** 2026-06-23
- **Deciders:** Project owner (sam), architecture
- **Affected modules:** engine:calibration, engine:physics, engine:analysis, feature:results

## Context
The owner initially requested broadcast-grade (sub-1 km/h, Hawk-Eye-like) accuracy from a single Android phone. A single camera cannot recover true 3D depth; broadcast systems use 6+ synchronised cameras. We must set honest, achievable targets and avoid building on an impossible premise, while leaving room to improve later.

## Decision
We will target **±2–4 km/h** speed and **~15–40 cm** line/length accuracy on a single phone, using a **homography calibrated from the known pitch geometry** (stump-to-stump 20.12 m, stump dimensions). Every reported value carries a **confidence**, surfaced in the UI. We will design `engine:analysis` to consume trajectories via a `TrajectorySource` abstraction so a **second-camera (multi-view) source** can be added later for true 3D without changing physics or UI.

## Options considered
1. **Claim broadcast-grade on one phone** — physically impossible; would mislead. Rejected.
2. **Single-camera with honest bounds + confidence (chosen)** — achievable, transparent, useful for practice.
3. **Require multi-camera from day one** — higher accuracy but more hardware/sync complexity than a personal tool needs now. Deferred behind a clean seam.

## Consequences
- Positive: realistic, trustworthy numbers; clear upgrade path; UI honesty via confidence.
- Negative: depth-dependent metrics (length, swing magnitude) remain approximate until a 2nd camera is added.
- Follow-ups: `TrajectorySource` abstraction in `engine:analysis`; future ADR for multi-view triangulation.

## Compliance
Encoded as the accuracy target in `ARCHITECTURE.md` §1 and the extension seam in §9. Upholds P9 (fail loud / flag low confidence) and P8 (replaceable/extendable behind interfaces).
