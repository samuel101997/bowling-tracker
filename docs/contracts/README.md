# Module Contracts

Each file here is the **public API contract** of one module — the only surface other modules may depend on. Anything not in a contract is `internal`.

Changing a contract is a documented, versioned event — see [../CONTRIBUTING.md](../CONTRIBUTING.md) §5.

## Index
- [`_TEMPLATE.md`](_TEMPLATE.md) — copy this to create a new module contract.
- [`engine-physics.md`](engine-physics.md) — **fully-worked example**; use as the model for all others.

## To be written (one per module, as it is built)
core:common · core:domain · core:ui · data:persistence · data:media ·
engine:vision · engine:tracking · engine:calibration · engine:analysis ·
feature:capture · feature:calibration · feature:results · feature:history
