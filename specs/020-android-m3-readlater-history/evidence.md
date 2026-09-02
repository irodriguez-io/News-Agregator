# 020 — Material 3 Expressive Read Later and History · evidence

**Branch:** `feat/020-android-m3-readlater-history`, cut from `main` at `9c0dd7f`\
**Slices:** 3, all done · **Tests:** 343 → **360** unit, plus 9 instrumented, 0 failures throughout\
**Implementer:** Codex (`gpt-5.6-sol high`), three fresh sessions\
**Reviewer:** the orchestrating Claude session — spec and plan author, not code author
(`execution-model.md` §5)\
**Ran concurrently with item 019**, in a separate worktree, sharing no file.

---

## 1. Forecast reconciliation — Step 0.4

All six of `slices.md`'s assumptions were checked against `main` at `9c0dd7f` and **all six held**:
`StatBand`'s null logic intact; `ArticleRow` called only from the two screens; item 018 touched **none** of
this item's four component files; every empty-state string present, so this **remains a one-gate item**; the
Undo toast still hosted globally at `IntentionalReadingApp.kt:341`.

One assumption was **added** at `6510697`: PR #32 put instrumented tests into CI, which changed the
disposition of the toast-overlap scenario — `spec.md` §5.2 had called it *"assertable only on a device"* and
pointed at `screencap`. It is now gateable, so slice 3 asserts it rather than only photographing it.

## 2. Gate runs

Reproduced by the reviewer with `--rerun-tasks` in a throwaway worktree, per §5.1 control 1.

| Slice | RED | GREEN | Reviewer-reproduced |
|---|---|---|---|
| 1 | `d484feb` | `0ea9245` | 351 unit, 0 failures |
| 2 | `daaefb7` | `8ceda93` | 358 unit, 0 failures |
| 3 | `2643559` | `1d5f529` | **360 unit, 0 failures; 9 instrumented, 0 failed ×3 runs** |

`assembleDebug` and `assembleDebugAndroidTest` green on every slice. Baseline 343. Net **+17 unit tests plus
2 instrumented**, none deleted or suppressed.

## 3. Failing-first evidence

All three slices produced **value-failing REDs**, reproduced independently:

- **Slice 1** — `351 tests completed, 5 failed`, including `row action target dp expected:<48.0>` and
  `title max lines expected:<2>`.
- **Slice 2** — `358 tests completed, 5 failed`.
- **Slice 3** — `360 tests completed, 3 failed`, **and on the device the toast RED was a genuine geometry
  failure**: *"Read Later action bounds overlapped toast bounds"*, and the same for History. Not a compile
  error, not a value stub — the strongest form this evidence could take.

## 4. The item's named trap, and it was avoided

`wave-e.md` asked this item to *"decide the null presentation"* for `readingTimeMinutes`. **It was never this
item's decision** — §29 and §54 decided it and `StatBand.kt` already implemented it. `design.md` D1 is
therefore a **refusal to decide**, with a scenario as a guard, because *a re-layout is exactly when a working
omission rule gets replaced by a tidier-looking formatted zero.*

**Verified by the reviewer rather than accepted from the report:** `knownReadingTimeValue` and
`availableStatValue` are **byte-identical** to `main`, both hashing to `2b246291a633d1538f7922e5` on each
side. No reading-time or stat test was edited.

## 5. Wave D's toast observation is now a gated test

Wave D's walkthrough recorded that the Undo toast overlaps the bottom row's action rail while showing.
`ReadingListLayoutTest` now asserts, for **both** screens at a forced 360 dp, that the last row's action
bounds do not overlap a showing toast's — forcing 360×800 dp via `DeviceConfigurationOverride.ForcedSize`
with every `dp.toPx()` baseline **inside** the override (§8.3), passing at underlying widths of 411 dp and
320 dp.

**`UndoToast.kt` and `IntentionalReadingApp.kt` are untouched.** Per D4 the fix is a bottom inset owned by
the lists, because the toast's **global** hosting is what lets an offer raised on one destination survive a
destination change (§70, Amendment 8). Reaching into the toast would have put this item inside item 021's
file and wave D's ground.

## 6. Existing assertions changed

**None.** The reading-time, grouping, Mark-unread and undo tests all stayed green and **unedited**, as did
every 017/018 test. `strings.xml` untouched. No `@Ignore`, `@Disabled` or `assumeTrue`. **No unlisted test
failed**, so §2.1 rule 5's protocol was never invoked.

## 7. A flake reported honestly, investigated, and explained

The implementer reported that the **full** `connectedDebugAndroidTest` crashed twice at test 1/9 in
`ArticleCardGestureTest` — a pre-existing test outside this item — while passing when run alone. **It said so
rather than reporting a clean run.**

**Not reproducible.** The reviewer ran the full 9-test suite **three consecutive times** on the exact GREEN
commit: 9/9 passing each time.

**The likely cause is specific:** the implementer had just been toggling emulator density (`wm density 540` /
`reset`) for its own width verification, and `ArticleCardGestureTest` operates on touch coordinates — exactly
what a density change disturbs. CI runs a fresh emulator with a fixed profile and no toggling.

## 8. Definition of done

| Item | Status |
|---|---|
| Queue Row: 16 dp radius, tonal fill, **no shadow or elevation** (§58.2, §56.1) | ✓ asserted |
| No thumbnail, nothing reserved (§74.2) | ✓ |
| Row title clamps at 2 lines (§13.2) | ✓ asserted on the title element |
| Row action targets ≥ 48 dp, no hover requirement (§72.2, §34.2) | ✓ |
| StatBand: three columns, 16 dp, `container` fill, editorial numerals (§76.6) | ✓ |
| **Reading-time omission preserved, never zeroed** (§29, §54) | ✓ **byte-identical, verified** |
| Unavailable topic omitted, not inferred (§55) | ✓ |
| History's empty state at full weight, **existing copy only** (§63) | ✓ |
| **Last row's actions clear a showing Undo toast** | ✓ **gated for both screens** |
| History groups Today / Yesterday / Earlier by local date (§60) | ✓ tests green |
| Mark unread returns the article and updates both counts (§62) | ✓ tests green |
| Every displayed value unchanged | ✓ |
| No colour, radius, size or font literal | ✓ |
| `UndoToast.kt`, `IntentionalReadingApp.kt`, `strings.xml` untouched | ✓ |
| **One gate, not two** (§75.2) | ✓ no new string |

## 9. Walkthrough

**Outstanding.** Batched at wave close per §6, plus the owner look this item's merge requires
(`wave-e.md` checkpoint 4). `spec.md` §5.4 states the steps, against **real accumulated history** preserved
with `adb install -r`.

**Two things a test cannot settle.** Whether the toast is *legible* over a row while showing — the assertion
proves they do not overlap, not that the result reads well. And §74.2's question for this surface: does the
row, spending the thumbnail's width on type, read as a deliberate composition or as something missing?

The Undo tap target moves with the message width, so re-locate before every tap and **score by article id** —
a missed Undo looks exactly like a passing run. `uiautomator dump` cannot see the toast; use `screencap`.

## 10. Hosted CI

Recorded at PR time. `android.yml` runs four tasks as of PR #32, including `connectedDebugAndroidTest`.
