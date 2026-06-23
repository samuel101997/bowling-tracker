# Module design: engine:physics

> Example module-design doc (the model for all others). The **contract** lives in `docs/contracts/engine-physics.md`; this file explains the *internal design and rationale*.

## Purpose
Convert a calibrated ball trajectory (positions in metres + timestamps) into the cricket insights the user cares about: **release speed, pitching point (line & length), swing, and bowling path**.

## Why it's a separate, pure module
- Speed/kinematics is the most accuracy-sensitive logic in the app; isolating it lets us unit-test against **known-answer fixtures** on the JVM with no device, no camera, no CV libs (ARCHITECTURE.md P3/P7).
- Keeping it free of OpenCV/LiteRT means ball-detection changes never risk the math, and the math can be reused (e.g. multi-camera, iOS) later.

## Internal design (not part of the contract)
- **Speed:** measure displacement between trajectory points near release; divide by Δt from timestamps; take a robust estimate (e.g. fit over the first N reliable points rather than a single pair) to reduce noise. Higher capture fps ⇒ more points ⇒ lower variance.
- **Bounce detection:** find where vertical position reverses (descending → ascending). That frame's mapped position is the `PitchPoint`.
- **Swing:** fit the pre-bounce path; lateral deviation from the straight release→bounce line gives magnitude + direction.
- **Path angles:** release-to-pitch angle; optional post-bounce deviation by comparing pre/post-bounce headings.
- **Confidence:** derived from number of points, residuals of the fit, and geometry quality. Surfaced, never hidden.

## Inputs it trusts / does not trust
- Trusts that calibration already mapped pixels → metres (that's `engine:calibration`'s job).
- Does **not** trust point count or geometry — guards with typed errors (`InsufficientTrajectory`, `NoBounceDetected`, `DegenerateGeometry`).

## Testing
- Known-answer fixtures: synthetic trajectories with analytically computed speed/bounce/swing → assert within tolerance.
- Edge cases: too few points, no bounce in frame, near-vertical degenerate geometry.
- Coverage target ≥80% (ARCHITECTURE.md §6.7).

## Change log pointers
- Any change to *how* a metric is computed → ADR + CHANGELOG `Changed` + engine-version bump.

## Open questions / future
- Drag/air-resistance modelling for a more physical speed-at-release estimate.
- Consume a multi-view `TrajectorySource` (see ADR-0003) for true 3D swing magnitude.
