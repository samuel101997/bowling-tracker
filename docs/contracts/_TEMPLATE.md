# Contract: <module> (`group:module`)

> The **public surface** of this module. Consumers may depend ONLY on what is listed here.
> Anything not listed is `internal` and may change without notice. Changing this file is a documented, versioned event (see CONTRIBUTING.md §5).

- **Layer:** L# (see ARCHITECTURE.md §4)
- **Responsibility (one line):**
- **May depend on:** <lower-layer modules>
- **Must NOT depend on:** <forbidden, e.g. Android, other features>
- **Owns these third-party libs (hidden behind interfaces):**

## Public types
| Type | Kind | Purpose |
|---|---|---|
| `Xxx` | interface | … |
| `Yyy` | data class | … |

## Public interfaces (the API)
```kotlin
// signatures only — the contract, not the implementation
interface Xxx {
    fun doThing(input: In): Result<Out, DomainError>
}
```

## Inputs / Outputs
- **In:** …
- **Out:** …
- **Errors:** which `DomainError` variants can be returned.

## Invariants & guarantees
- Determinism, thread-safety, units, confidence semantics, etc.

## Versioning notes
- Additive changes → minor. Breaking changes → ADR + update all consumers.
