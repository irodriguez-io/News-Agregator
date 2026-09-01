# 017 — Material 3 Expressive design tokens and theme · evidence

**Branch:** `feat/017-android-m3-design-tokens`\
**Cut from:** `main` at `4bdb54a`, rebased onto `47885e4`\
**Slices:** 3, all done\
**Tests:** 297 → **315**, 0 failures throughout\
**Implementer:** Codex (`gpt-5.6-sol high`), three fresh sessions, one per slice\
**Reviewer:** the orchestrating Claude session — spec and plan author, not code author
(`execution-model.md` §5)

---

## 1. Forecast reconciliation — `/feature-implementation` Step 0.4

Nothing merged beneath this item between design and dispatch. `slices.md`'s five assumptions were checked at
dispatch and all five held:

| Assumption | Held? |
|---|---|
| `main` at `e126cbb` or later, `res/font/` present | yes — branch rebased onto `47885e4` |
| `ThemeDerivationTest` freezes 24 hexes positionally against a twelve-field list | yes |
| `LaunchBackgroundTest` asserts the invariant, not a hex | yes — and it stayed green untouched |
| `titleMedium` undefined and consumed at `LocalStateMessages.kt:48` | yes — closed by slice 2 |
| `variationSettings` compiles under Compose BOM 2026.08.00 | yes, with `@OptIn(ExperimentalTextApi::class)` |

## 2. Gate runs

Every figure below was **reproduced by the reviewer** with `--rerun-tasks` in a throwaway worktree at
`/tmp/verify-017-red`, per `execution-model.md` §5.1 control 1. No implementer-reported number was accepted
as evidence.

| Slice | RED | GREEN | Reviewer-reproduced gate |
|---|---|---|---|
| 1 | `48c22f0` | `86acd5b` | 305 tests, 0 failures, 0 errors; `assembleDebug` ✓ |
| 2 | `edf8842` | `e4a08c0` | 308 tests, 0 failures, 0 errors, 0 skipped; `assembleDebug` ✓ |
| 3 | `24ab94e` | `ceef959` | 315 tests, 0 failures, 0 errors, 0 skipped; `assembleDebug` ✓ |

Baseline at branch point: **297 tests, 0 failures.** Net **+18 tests**, no test deleted or suppressed.

## 3. Failing-first evidence — and the one place it was weak

**Slice 1 — weak, and the brief's fault.** The RED commit failed at **Kotlin compilation** with unresolved
references, not as a value failure. No test ran.

That is unavoidable for a purely additive change to a data class — a test naming a field that does not exist
cannot run — **unless the brief requires the fields to be stubbed with wrong values first, and slice 1's
brief did not.** `execution-model.md` §2.1 rule 3 forbids the *next slice's* compile error as evidence; this
was the slice's own, which is less bad and still weaker than a value failure, because it proves a field is
*missing* rather than proving the test catches a field being *wrong*.

**The reviewer obtained the missing evidence by perturbation** rather than accepting the slice on a compile
error. In the throwaway worktree, against `86acd5b`:

| Perturbation | Result |
|---|---|
| `secondary` seed reset to the design source's own `#7692FF` (2.9:1, the real-world mistake) | **3 tests fail** |
| achromatic-hue rule disabled (`NEGLIGIBLE_CHROMA = -1.0`), restoring the olive bug | **2 tests fail** |
| `bg` seed shifted one step, `#F7F9FD` → `#F7F9FC` | **5 tests fail** |

**Slices 2 and 3 — strong.** The plan was amended after slice 1 (`d58a097`) to require a value-failing RED,
and both delivered one:

- **Slice 2**, reproduced as `308 tests completed, 3 failed`: *"displayMedium still equals the Material 3
  default"*; *"display-lg family expected:<FontListFontFamily(… weight=FontWeight(weight=600) …)>"* against
  an actual of `FontFamily.Serif`.
- **Slice 3**, reproduced as `315 tests completed, 6 failed`: decorative outline expecting `#CBCCDE` and
  receiving the control-grade `#757686`; light surface tint expecting `#212B56` at 10% and receiving
  transparent; navigation indicator expecting `#ABD2FA`; the 48-role map's first mismatch on `secondary`;
  spacing expecting `[4, 12, 16, 18, 24, 32, 680].dp`; the shape scale.

**Slice 2 found a better pattern than the brief prescribed.** Rather than stubbing production symbols with
wrong values, it asserted against the **existing** Typography slots, which still held the old serif/Avenir
metrics — so the suite compiled and ran and failed on values, with no scaffolding to remove. Slice 3 reused
it: its only production change in RED was `private fun` → `internal fun` plus a `darkTheme` parameter so the
test could reach the factory. **No value was fixed in any RED commit.**

## 4. Existing assertions changed

| Test | Disposition |
|---|---|
| `ThemeDerivationTest` | **rewritten by slice 1**, as its DoD required. Coverage grew **2 → 8 tests**. |
| `LaunchBackgroundTest` | **green and unedited throughout.** It asserts the invariant `argbHex(lightTokens().bg) == colors.xml launch_background`, so it survived a total palette change — item 010 having applied wave C's comparator lesson is what made that possible. |
| `LaunchThemeInheritanceTest` | untouched — structural only. |
| `LaunchNightModeTest`, `AppViewModelTest`, `SampleDatasetTest` | untouched. |
| Everything else | untouched. No `@Ignore`, `@Disabled` or `assumeTrue` introduced anywhere. |

**No unlisted test failed at any point**, so §2.1 rule 5's report-before-editing protocol was never invoked.

## 5. Departures from the plan

**One, and it made the plan stricter rather than looser.** After slice 1, `slices.md` was amended at
`d58a097` to require slices 2 and 3 to produce a value-failing RED, and slice 3's entry gained the two
carry-forward defects below. No DoD was weakened and no scenario was changed.

## 6. Independent verification beyond the gate

**All ten dark seeds and all sixteen derived roles were checked against an independent computation** — a
Python implementation of the same Oklch derivation written during the design pass, before any code existed.
Agreement was exact on all 26 values, in both schemes. Two independent implementations agreeing is the
strongest available evidence that §77.5 and §78.2 are implemented correctly.

## 7. Two defects carried between slices, both closed

Slice 1 re-pointed the legacy `strongBorder` field to `outlineControl` per `design.md` D1. `Theme.kt:61`
mapped Material 3's **decorative** `outlineVariant` from `strongBorder`, so between slice 1 and slice 3 the
decorative outline role received the **control-grade** contrast value. Caught at slice 1's review, recorded
in `slices.md`, and closed by slice 3 — which now maps `outline` ← `outlineControl` and `outlineVariant` ←
`outlineVariant` as §78.3's two distinct roles.

`surfaceTint` remained the transparent placeholder `tokens.surface.copy(alpha = 0f)` until slice 3, which
gave it `(if (darkTheme) bg else tertiary).copy(alpha = 0.10f)` — the dark branch being near-black rather
than navy, per §78.4.

## 8. Slice reviews

Three PASS verdicts, one per slice, each stated against the dimensions in `/feature-review` Step 1 and each
recorded in `slices.md` alongside the slice it gated. No FINDINGS were raised and no follow-up brief was
dispatched.

**Non-blocking observations recorded rather than filed as findings:**

- `tokensFrom` reads the module-level `LightSeeds`/`DarkSeeds` for its `onTonal` candidates, making it
  dependent on top-level declaration order. Safe as written and it would fail loudly, not silently.
- `robotoFlex` declares `FontWeight.Medium` (500), which no §76.1 style uses. Harmless on a single variable
  resource and likely useful to items 018–020.
- `TypographyTest` duplicates the `variableFontFamily` helper rather than reusing production's. That is the
  right call — reusing it would make the family assertion tautological.
- The 48-role map test asserts role→token *mapping*, not values. Correct for its purpose; the values are
  asserted in `ThemeDerivationTest` and the dedicated per-role tests.

## 9. Definition of done

| Item | Status |
|---|---|
| Ten seeds, both schemes, exactly §77.4/§77.5 | ✓ verified against the spec and an independent computation |
| Dark derived by holding hue and chroma | ✓ asserted as an invariant with tolerance, not as values |
| Derived roles from §78.2's stated mixes | ✓ asserted by recomputing each mix, not by frozen hex |
| Achromatic endpoint adopts the other's hue (§78.1) | ✓ regression test asserts hue proximity **and** that the result is not in the 70–160° yellow-green band |
| Control boundary clears 3:1 in both schemes (§73.1) | ✓ asserted as a **ratio**; derivation picks `border` in light, `muted` in dark |
| Decorative hairline exempt from 3:1 | ✓ separate role, separate assertion |
| Launch background equals composed background | ✓ `LaunchBackgroundTest` green and unedited |
| Every legacy token field still exists | ✓ all 13 present; 205 consumer call sites compile unchanged |
| Nine §76.1 type styles, exact, in `sp` | ✓ |
| Twelve consumed M3 slots authored, none a default | ✓ closes `titleMedium` at `LocalStateMessages.kt:48` |
| Both families load from `res/font/`, weight axis resolves | ✓ no network fetch, no dependency, **no version gate** |
| 48 `ColorScheme` roles explicitly supplied | ✓ asserted |
| `surfaceTint` real (§16.2) | ✓ |
| Shape scale (§15.2), spacing rhythm (§14.2) | ✓ as `CompositionLocal`s, matching the existing idiom |
| No file outside `ui/theme/**`, `res/values{,-night}/**` and this item's tests | ✓ diff audit empty on every slice |
| No `dp` literal migrated in any consumer (D6) | ✓ no consumer file touched |
| No new dependency | ✓ |

## 10. Walkthrough

**Outstanding.** Batched at wave close per `execution-model.md` §6, plus the owner look this item's merge
requires (`wave-e.md` checkpoint 4). `spec.md` §5.4 states the steps.

**The one thing tests cannot catch is a silent font fallback** — a misnamed or unloadable font resource does
not fail a JVM test, because Compose falls back and the text still renders. The walkthrough is the only real
check that Playfair Display is actually on screen.

## 11. Hosted CI

Recorded at PR time. `android.yml` fires on `android/**`; `test.yml` fires on every PR regardless of paths,
per the `execution-model.md` §8 correction on PR #27.
