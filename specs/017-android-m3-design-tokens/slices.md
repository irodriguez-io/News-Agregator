# 017 — slice plan

Three slices, strictly sequential. Each closes as a failing-first test commit plus an implementation
commit, and each reaches a green gate on its own.

`main` is at **297 tests, 0 failures** (`e126cbb`). This item only adds tests.

---

## Fixed for this item — do not re-decide these mid-implementation

1. **The ten seeds, both schemes.** `06-ui-ux.md` §77.4 and §77.5. Owner-approved 2026-09-01.
   **If you find yourself choosing a colour, stop and report it.**
2. **Every legacy token field name survives** (`design.md` D1). Renaming one is out of scope and breaks
   205 call sites.
3. **No file outside `ui/theme/**`, `res/values/**`, `res/values-night/**` and this item's own tests.**
   Verified by the diff check in `spec.md` §5.1.
4. **No API version gate on `variationSettings`** (D2). `minSdk` is 26, which is the gate's own threshold.
5. **The three unauthored slots map to authored styles** (D7). No new metrics.
6. **Contrast is asserted as a ratio, never as a hex** (D3).
7. **No `dp` literal in `ui/components/**` or `ui/screens/**` is migrated** (D6).

---

## Slice 1: the ten seeds, the derivation, and the contrast floor

**Objective.** Replace the six-seed authored palette with the ten approved seeds in both schemes, derive
every role from them, and prove the control-boundary floor by measurement.

- **Scenarios:** the authored light seeds are the approved values; the dark scheme is derived, not authored;
  derived roles come from the stated mixes; a near-achromatic endpoint adopts the other endpoint's hue; a
  control-boundary outline clears the floor in both schemes; the decorative hairline is not held to the
  control floor; the launch background still equals the composed background; every legacy token field still
  exists.
- **Files:** `ui/theme/Tokens.kt`, `ui/theme/Color.kt`, `res/values/colors.xml`,
  `res/values-night/colors.xml`, `test/…/ui/theme/ThemeDerivationTest.kt`, and a new contrast test in
  `test/…/ui/theme/`.
- **Must not touch:** `ui/theme/Theme.kt` (slice 3), `ui/theme/Type.kt` (slice 2), anything outside the
  theme package.
- **How it reaches a green gate alone:** every legacy field name is preserved, so all 205 consumer call
  sites compile untouched; `ThemeDerivationTest` is rewritten in this slice to the new table; and
  `LaunchBackgroundTest` stays green because the `bg` seed and both `colors.xml` files change **inside this
  slice** — splitting them would fail that test between slices.
- **Definition of done:** both gates green; the contrast test asserts ratios in both schemes; `Color.kt`
  carries the achromatic-hue rule; the diff touches no file outside the boundary above.
- **Status:** **done** — RED `48c22f0`, GREEN `86acd5b`. Gate reproduced independently with
  `--rerun-tasks` in a throwaway worktree: **305 tests, 0 failures, 0 errors**, `assembleDebug` successful
  (baseline 297). Slice review PASS.

**Verified beyond the gate, because the RED was weak.** The RED commit failed at Kotlin compilation rather
than as a value failure — unavoidable for a purely additive data-class change unless the fields are stubbed
first, which the brief did not require. Discrimination was obtained by perturbation instead: reseeding
`secondary` to the design's own `#7692FF` (2.9:1) fails 3 tests; disabling the achromatic-hue rule fails 2;
a one-step shift in the `bg` seed fails 5. **All 10 dark seeds and all 16 derived roles match an
independent computation.** Slices 2 and 3 require stub-first RED so this is not repeated.

**Carry-forward to slice 3, not a slice-1 defect:** `strongBorder` now resolves to `outlineControl`, and
`Theme.kt:61` maps M3's `outlineVariant` from `strongBorder`. So M3's **decorative** outline role currently
receives the **control-grade** value. Slice 3 must re-point it, or decorative dividers ship over-contrasted.

**Why `colors.xml` is here and not in a slice of its own.** `LaunchBackgroundTest` asserts
`argbHex(lightTokens().bg)` equals the `launch_background` resource. The seed and the resource are one
atomic change; any boundary between them is a red gate by construction.

---

## Slice 2: the type scale on the two bundled families

**Objective.** Load Playfair Display and Roboto Flex from `res/font/`, author the nine styles of §76.1, and
close the twelve consumed Material 3 slots.

**RED must be a value failure, not a compile failure.** Slice 1's RED could only fail to compile, because
its assertions referenced fields that did not exist yet. Where this slice's tests reference something new,
introduce it in the RED commit with a deliberately wrong value so the test **runs and fails on the value**.
A compile error proves a thing is missing; it does not prove the test can catch a thing being wrong.

- **Scenarios:** every typography slot the application consumes is authored; the authored type styles carry
  their specified metrics; both bundled families load and resolve their weight axis.
- **Files:** `ui/theme/Type.kt`, and a new typography test in `test/…/ui/theme/`.
- **Must not touch:** `Tokens.kt`, `Color.kt`, `Theme.kt`, any consumer.
- **How it reaches a green gate alone:** `Type.kt` continues to supply every M3 slot the application reads,
  so the nine consuming files compile and render unchanged; only the metrics behind those slots change.
- **Definition of done:** both gates green; the nine §76.1 styles match their specified family, size, line
  height, weight and tracking; **no consumed slot equals Material 3's default**, which is what closes the
  `titleMedium` fallback at `LocalStateMessages.kt:48`; sizes are `sp`; no version gate; no new dependency.
- **Status:** pending

**The silent-fallback trap.** A misnamed font resource does not fail a test — Compose falls back and the
text still renders. Assert that each style's family resolves to the bundled resource, and treat the
walkthrough as the real check (`spec.md` §5.2).

---

## Slice 3: the Material 3 role map, the shape scale, and the spacing rhythm

**Objective.** Map the new roles onto the full `ColorScheme`, and add the shape and spacing scales that
items 018–020 will consume.

**Two things slice 1 left for this slice, both confirmed still open:**

1. **`Theme.kt:61` maps M3 `outlineVariant` from `strongBorder`, which slice 1 re-pointed to
   `outlineControl`.** M3's decorative outline role is therefore receiving the control-grade contrast value.
   Re-point `outlineVariant` at the `outlineVariant` token and `outline` at `outlineControl`, per §78.3's
   two roles.
2. **`surfaceTint` is still `tokens.surface.copy(alpha = 0f)`** — the transparent placeholder. §16.2
   requires a real value.

**RED must be a value failure, not a compile failure** — same requirement as slice 2, same reason.

- **Scenarios:** no colour is named outside the theme package. (This slice also completes the derived-role
  scenarios end to end, through `MaterialTheme`.)
- **Files:** `ui/theme/Theme.kt`, new `ui/theme/Shape.kt` and `ui/theme/Spacing.kt` (or one file if the
  implementer prefers), and their tests.
- **Must not touch:** any consumer; the seeds; the type scale.
- **How it reaches a green gate alone:** `Theme.kt` already supplies every one of the 48 M3 roles
  explicitly, so re-pointing them at better-named tokens is a value change with no signature change; the
  shape and spacing scales are pure additions that nothing consumes yet.
- **Definition of done:** both gates green; `surfaceTint` carries a real value rather than the transparent
  placeholder it holds today (§16.2); the tonal container role is `tonal` and the nav-pill role reads from
  it (§8.2); the shape scale carries §15.2's values and the spacing rhythm §14.2's; the diff touches no
  file outside the boundary.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

1. **`main` is at `e126cbb` or later**, so `res/font/` and `assets/licenses/` are present. Slice 2 cannot
   start otherwise.
2. **`ThemeDerivationTest` still freezes 24 hexes positionally against a twelve-field list.** If it has
   changed shape, report before editing.
3. **`LaunchBackgroundTest` still asserts the invariant** rather than a literal hex. If it has been changed
   to freeze a hex, that is a finding, not something to work around.
4. **`titleMedium` is still undefined in `IntentionalReadingTypography`** and still consumed at
   `LocalStateMessages.kt:48`. If it is now defined, slice 2's defect-closing claim is already satisfied and
   should be recorded as such rather than asserted twice.
5. **`variationSettings` compiles under Compose BOM 2026.08.00** without an unavailable opt-in. If it needs
   `@OptIn(ExperimentalTextApi::class)`, add it and note it; if it needs more than that, stop and report.

---

## On existing assertions

`spec.md` §5.3 names the five tests this item reaches and **why** it reaches each one. That list is
**not a freeze.** It was accurate against `e126cbb` and nothing has merged beneath this item since.

**Per `execution-model.md` §2.1 rule 5: if a test not on that list fails, report it before editing it.** An
assertion enumeration written at design time and used at dispatch time is exactly what went stale on item
016, and the protocol that caught it is this one.
