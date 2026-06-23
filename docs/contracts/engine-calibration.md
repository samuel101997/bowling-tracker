# Contract: engine:calibration (`engine:calibration`)

- **Layer:** L2 (Engine-pure)
- **Responsibility:** Compute and apply the image→pitch-plane homography from point correspondences.
- **May depend on:** `core:common` only.
- **Must NOT depend on:** Android, OpenCV, other engine modules.

## Public types
| Type | Kind | Purpose |
|---|---|---|
| `Calibrator` | interface | `computeHomography(correspondences)` → `Result<Homography, DomainError>`. |
| `calibrator()` | factory fun | Returns the default DLT implementation. |
| `Homography` | data class | 3×3 row-major transform; `apply(Point2D)` maps px→metres. |
| `PointCorrespondence` | data class | image pixel ↔ known world metres. |

## Guarantees
- Pure & deterministic (P10). DLT algorithm, no external linear-algebra dependency.
- Requires ≥4 correspondences; else `DomainError.InvalidCalibration`.
- Verified against a NumPy SVD prototype AND a faithful Python port of the
  in-module Jacobi solver (errors < 1e-6) before implementation.
