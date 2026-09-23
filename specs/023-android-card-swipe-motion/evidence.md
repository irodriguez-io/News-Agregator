# 023 — The card leaves, and the next one arrives · evidence

**Branch:** `feat/023-android-card-swipe-motion`, cut from `main` at `15e082c`, **rebased onto `22ef12d`**\
**Slices:** 2 · **slice 1 done**, slice 2 pending · **Tests:** 391 → **397** unit, 20 instrumented, 0 failures\
**Implementer:** Codex, fresh sessions per dispatch (`codex-023-s1`, `codex-023-s1b`, `codex-023-s1c`)\
**Reviewer:** the orchestrating Claude session — brief author, not code author
(`execution-model.md` §5)\
**Amendment:** 11, authored by this item.

---

## 0. The rebase, and what it cost

Item 022 merged first, as `slices.md` directed. The rebase onto `main` at `22ef12d` hit the two conflicts
the slice plan predicted, in `docs/v1/README.md` and `docs/v1/06-ui-ux.md`, and both resolved **additively**:
Amendments 10 and 11 both stand, §79.4 and §79.5 both stand, §48's reduced-motion list carries both, and
§79.3's correction is applied **once** while enumerating all four subsections. §79.3's closing sentence was
extended to say where §79.5's `reducedMotion` flag sits, which neither branch had needed to say before.

The item's code citations were re-verified against post-022 `main` and resolve exactly. **022 never touched
`ArticleCard.kt`.** All four gates were re-run on the rebased head before any dispatch — 391 JVM, 20
instrumented, 0 failures — because the rebase, not the slice, was what had just changed the tree.

## 1. Three implementer stops before a line was written, and all three were right

This is the item's most transferable record. **No stop was a failure to follow the brief; every one was the
brief failing to survive contact with the code**, and two of the three originate in this item's own
`design.md`.

| # | Session | What it refused | Verdict |
|---|---|---|---|
| 1 | `codex-023-s1` | D8's claim that `snap` and an M3 easing set are *"already imported"* in `ArticleCard.kt` | **Correct.** Lines 3-7 import `Animatable`, `AnimationSpec`, `AnimationVector1D`, `CubicBezierEasing`, `tween` — and neither of those. |
| 2 | `codex-023-s1` | D8's claim that `SwipeGesture`'s constant block *"already carries a citation per constant"* | **Correct.** It was eight bare `const val`s with no comments at all. |
| 3 | `codex-023-s1b` | That the brief demanded `EXIT_DURATION_MS = 300` **and** that item 008's tests pass unedited | **Correct.** `SwipeGestureTest.kt:139` asserted `assertEquals(280, SwipeGesture.EXIT_DURATION_MS)`. The two requirements cannot both hold. |

**`design.md` D8 is wrong on both of its factual claims and must be corrected at ship bookkeeping.** It was
written from a reading of the file that was true of some other file, and it was believed twice — once by the
design pass, once by the orchestrator who copied it into a brief. This is exactly the failure
`execution-model.md` §5 names: *a claim written when true and stale when used, with no gate that
distinguishes the two.*

**Finding 3 was resolved by an exception, decided on the specification and bounded in writing.** That
assertion encodes §44's **first-edition** value, from when §44 carried one duration for both surfaces.
Amendment 9's second edition split §44 — §44.1 keeps `280ms` and the `cubic-bezier(0.2, 0.8, 0.2, 1)` for
the browser, §44.2 binds Android to M3 Emphasized at the M3 duration, which D1 records as `300ms`
(`06-ui-ux.md` §80 records the split). The assertion was not wrong about behaviour; it pointed at a
superseded value, and its enclosing test — `releasing at or past the threshold emits the direction's
action` — is about which action a release emits, which is untouched.

The exception was **one literal, `280` → `300`, in the RED commit** where the gate could see it. Deleting
the assertion was explicitly forbidden, as were any other change to that file and any restructuring of item
008's test. `EXIT_FRACTION`, `EXIT_MINIMUM_DP`, both `exitTranslationX` values and both `Action` results are
byte-identical.

**That line is the antipattern `slices.md` fixed-decision 6 names** — *assert the selection, not the
literal.* Item 008 wrote it before the rule existed, and it is the whole reason an exception was needed.

## 2. Slice 1 — the card leaves on Android's curve, and fades as it goes

**Commits:** RED `95cd908`, GREEN `cc927d1`.

**Gates, re-run independently by the reviewer with `--rerun-tasks`** rather than accepted from the
implementer's report:

| Gate | Result |
|---|---|
| `:app:testDebugUnitTest` | **397 tests, 0 failures, 0 errors, 0 skipped** (391 → 397) |
| `:app:assembleDebug` | ✓ |
| `:app:assembleDebugAndroidTest` | ✓ |
| `:app:connectedDebugAndroidTest` | **20 tests, 0 failures, 0 skipped**, Pixel_10 AVD, API 37 |

`ArticleCardGestureTest` and `ArticleCardScrollGestureTest` are **byte-identical to `main`** — verified by
`git diff 22ef12d HEAD` over both paths, not by reading the implementer's claim.

### What the production change is

The `motionSpec` moved out of the `remember` block into `articleSwipeMotionSpec(reducedMotion)`, which is
`snap()` under the preference and otherwise a `300ms` tween on `SwipeGesture.ExitEmphasizedEasing`. The fade
is a third `Animatable(1f)` joined into `animateToGestureState()`'s **existing** `coroutineScope`, so the
exit remains one coordinated animation with one completion point — which is what keeps D2's sequencing, and
with it items 013 and 015, untouched by construction.

`SwipeGesture.State` gained a derived `alpha`:

```kotlin
val alpha: Float
    get() = if (reducedMotion || exitTranslationX == 0f) 1f else 0f
```

Derived rather than stored, so an uncommitted drag, a cancel, and a restore-after-failed-persistence are all
opaque without any of them needing to say so.

### One thing the implementer got right that the brief did not ask for

**`ExitEmphasizedEasing` defers its `Path` behind `by lazy`.** `androidx.compose.ui.graphics.Path` is
Android-backed and throws in a JVM unit test. Without the deferral, *selecting* the spec would have required
an Android runtime and slice 1's central test — that the exit spec selects Emphasized at the recorded
duration — could not have been a JVM test at all. The brief specified where the easing lived and said
nothing about this.

### The easing is now defined twice, deliberately and disclosed

`SwipeGesture.ExitEmphasizedEasing` carries the same two-cubic path as item 021's `emphasizedEasing` at
`ui/IntentionalReadingApp.kt:271-279`. Unifying them means editing that file, which is outside this slice's
boundary and is wave D's ground. The GREEN commit message says so in one line. **This belongs in
`backlog.md`'s Debt section** — absorb it when something next touches `IntentionalReadingApp.kt`.

### Reviewer observations that were not raised as findings

- **Several new tests assert against source file *text*** — `functionSource("ArticleCard").contains(...)`, a
  regex over `SwipeGesture.kt`. Brittle to a rename or a reformat. **Not this slice's invention:**
  `ArticleCardTest.kt` on `main` already carried eleven such assertions and all three helpers
  (`source`, `functionSource`, `articleCardSurface`) predate it. The slice followed the file's convention.
  **Debt, not a finding.**
- `assertNotEquals(CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f), selected.easing)` compares across types and can
  never fail. Near-vacuous — the `assertSame` on the following line is what carries the weight, so coverage
  is real and a re-dispatch would have cost more than the line is worth.

### The RED was partly a compilation failure

`SwipeGestureTest.kt:139` failed observably — *14 tests completed, 1 failed, expected 300, actual 280*. The
six new `ArticleCardTest` scenarios could not compile until the API existed. Ordinary in Kotlin and not a
rule breach, but it means the RED was less fully observable than a pure test-failure RED, and it is recorded
rather than glossed.

## 3. Slice 2 — the next card rises and fades into place

*Pending.*
