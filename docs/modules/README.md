# Module Design Notes

One file per module describing its **internal design and rationale** (the contract — the public surface — lives in `../contracts/`).

## Index
- [`engine-physics.md`](engine-physics.md) — **fully-worked example**; use as the model for all others.

## To be written (one per module, as it is built)
core:common · core:domain · core:ui · core:testing · data:persistence · data:media ·
engine:vision · engine:tracking · engine:calibration · engine:analysis ·
feature:capture · feature:calibration · feature:results · feature:history

## What goes in a module-design doc
Purpose · why it's a separate module · internal design (not the contract) · what it trusts / guards · testing approach · open questions / future. See `engine-physics.md`.
