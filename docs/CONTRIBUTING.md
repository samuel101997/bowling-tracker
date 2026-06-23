# Contributing & Change Management

> Enterprise rule: **no change is "done" until it is decoupled, tested, and documented.** This file defines the process. It applies to humans and AI agents equally.

## 1. Golden rules

1. **Respect the dependency rule** in [`ARCHITECTURE.md`](ARCHITECTURE.md) §4. A PR that adds an illegal module dependency must not merge — CI will block it.
2. **Cross boundaries only through contracts.** Depend on interfaces + domain models, never concrete classes from another module.
3. **One concern per change.** Keep PRs small and scoped to a single module where possible.
4. **Document as you go.** Documentation is part of the work, not an afterthought.

## 2. Definition of Done (checklist for every change)

Copy this into every PR description and tick each box:

```
- [ ] Code respects the dependency rule (ARCHITECTURE.md §4)
- [ ] No new feature→feature or engine→Android dependency
- [ ] Public API change reflected in docs/contracts/<module>.md
- [ ] CHANGELOG.md updated (see format below)
- [ ] ADR added if an architectural decision was made (docs/adr/)
- [ ] docs/modules/<module>.md updated if behaviour/design changed
- [ ] Tests added/updated and green (see ARCHITECTURE.md §6.7)
- [ ] ktlint + detekt pass
```

## 3. When do I need an ADR?

Write an Architecture Decision Record (`docs/adr/`) whenever you:
- add, remove, split, or merge a module;
- change the dependency rule or a module's responsibility;
- choose or replace a major library (CV, ML, DB, camera, DI);
- change a public contract in a breaking way;
- change the analysis/measurement approach (e.g. how speed is computed).

Small bug fixes and internal refactors that don't change a contract do **not** need an ADR — but they still need a CHANGELOG entry.

Use [`docs/adr/0000-template.md`](adr/0000-template.md). Number sequentially. ADRs are immutable once `Accepted`; to reverse one, write a new ADR that supersedes it.

## 4. CHANGELOG format

`CHANGELOG.md` follows *Keep a Changelog* + SemVer. Add your entry under `## [Unreleased]` in the right group:

```
### Added      — new capability
### Changed    — behaviour change to existing capability
### Deprecated — soon-to-be-removed
### Removed     — removed capability
### Fixed       — bug fix
### Security    — security-relevant change
```

Each line: imperative, references the module, links the ADR/contract if relevant. Example:
`- engine:physics: add post-bounce deviation angle to Insights (see ADR-0007)`

## 5. Contract changes (the decoupling guarantee)

A module's contract = its public surface, documented in `docs/contracts/<module>.md`.

- **Additive change** (new optional API): minor version bump of the module; update contract + CHANGELOG.
- **Breaking change** (signature/semantics change, removal): requires an ADR, a contract update, CHANGELOG `Changed`/`Removed`, and updates to every consumer in the same PR. Prefer adding-then-deprecating over breaking.

## 6. Branching & commits

- Branch per change: `feat/<module>-<short>`, `fix/<module>-<short>`, `docs/<short>`, `refactor/<module>-<short>`.
- Conventional commits: `type(scope): summary` — e.g. `feat(engine:physics): compute swing angle`.
- One logical change per commit; keep history reviewable.

## 7. Module creation procedure

1. Write an ADR proposing the module (purpose, layer, dependencies).
2. Add the module dir under the correct layer; wire it in `settings.gradle` and the version catalog.
3. Create `docs/contracts/<module>.md` (start from the contract template) and `docs/modules/<module>.md`.
4. Add the dependency-rule entry to `ARCHITECTURE.md` §4/§5.
5. CHANGELOG entry.

## 8. Review gates (CI must be green)

`ktlint → detekt → unit tests → module-dependency check → assemble`. A red pipeline blocks merge. The dependency check is the guardian of decoupling — never disable it to "get something through."
