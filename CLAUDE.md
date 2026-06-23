# CLAUDE.md — Operating guide for AI agents on Bowling Tracker

> This file is loaded by Claude (and other agents) when working in this repo. Follow it strictly. It encodes the enterprise rules so that **every change stays decoupled and documented**.

## Read first, every session
1. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — the source of truth. Principles (§2), the dependency rule (§4), module catalogue (§5).
2. The contract of any module you will touch: [`docs/contracts/`](docs/contracts/).
3. [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) — the Definition of Done.

## Hard constraints (do not violate)
- **Never** add a dependency that breaks the dependency rule (ARCHITECTURE.md §4). No `feature → feature`. No `engine:* → Android/CameraX/Room/UI`.
- **Never** put framework types (`Bitmap`, Room annotations, Compose types) into `core:domain` models.
- **Never** let modules talk through concrete classes — only interfaces + domain models.
- **Never** return a silently-wrong number. Use typed `Result`/`DomainError`; surface low confidence.
- **Never** disable the CI dependency-rule check to make something pass.

## Definition of Done (every change)
Tick the checklist in [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) §2. In short:
respect deps → update the module contract if the public API changed → CHANGELOG entry → ADR if architectural → update `docs/modules/<module>.md` → tests green → lint clean.

## How to make a change (procedure)
1. State which module(s) you're touching and confirm the change fits that module's single responsibility.
2. If it needs a new module or a contract/library/approach change → **write an ADR first** (`docs/adr/`, from the template). Don't proceed until the decision is recorded.
3. Implement behind the existing contract; keep new types `internal` unless they're meant to be public.
4. Add/extend tests per ARCHITECTURE.md §6.7 (engine modules need known-answer fixtures).
5. Update: contract doc (if public API changed), `docs/modules/<module>.md`, `CHANGELOG.md`.
6. Self-check against the dependency rule before declaring done.

## When unsure
If a requirement seems to need a rule-breaking shortcut, **stop and propose an ADR** describing the trade-off instead of working around the architecture. Coupling introduced "just this once" is the thing this repo exists to prevent.

## Style
Kotlin, Coroutines/Flow, immutable domain models, ktlint/detekt clean, conventional commits (`type(scope): summary`).
