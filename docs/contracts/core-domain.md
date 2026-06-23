# Contract: core:domain (`core:domain`)

- **Layer:** L1
- **Responsibility:** Entities, value objects, use cases, and ports (interfaces) that bind features to data/engine.
- **May depend on:** `core:common`, kotlinx-coroutines.
- **Must NOT depend on:** Android, Room, Compose, OpenCV, LiteRT, or any other module.

## Public types
| Type | Kind | Purpose |
|---|---|---|
| `Delivery`, `Session`, `Calibration`, `Trajectory`, `BallDetection`, `Insights`, `PitchPoint`, `SwingInsight` | data class | Framework-free domain models. |
| `DeliveryId`, `SessionId`, `CalibrationId` | value class | Typed identifiers. |
| `PitchGeometry` | object | Pitch constants (20.12 m, stump dims). |
| `AnalysisEngine`, `TrajectorySource`, `DeliveryRepository`, `MediaStore` | interface (ports) | Implemented by engine/data modules. |
| `AnalyzeDeliveryUseCase`, `SaveDeliveryUseCase` | class | Application use cases over ports. |

## Guarantees
- No framework types in any model (ARCHITECTURE.md §6.3).
- Ports return `Result<_, DomainError>`; suspend where I/O-bound.
- `TrajectorySource` keeps the multi-camera 3D seam open (ADR-0003).
