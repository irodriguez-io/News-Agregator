# 023 — The card leaves, and the next one arrives · evidence

**Branch:** `feat/023-android-card-swipe-motion`, cut from `main` at `15e082c`, **rebased onto `22ef12d`**\
**Slices:** 2, both done · **Tests:** 391 → **400** unit, 20 → **23** instrumented, 0 failures at every gate\
**Implementer:** Codex, six fresh sessions (`codex-023-s1`, `-s1b`, `-s1c`, `-s2`, `-s2-fu`, `-s2-fu2`)\
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

## 1. Four implementer stops across the item, and every one was right

This is the item's most transferable record. **Three of the four were the brief failing to survive contact
with the code, not the implementer failing to follow it** — and two of those three originate in this item's
own `design.md`. The fourth, in slice 2, was the implementer catching a bug in its own test and refusing to
edit around it; it is recorded in §3.

**Nothing was dispatched blind afterwards.** From slice 2 onward every factual claim in a brief was checked
against the file, with line numbers, before the brief was written.

| # | Session | What it refused | Verdict |
|---|---|---|---|
| 1 | `codex-023-s1` | D8's claim that `snap` and an M3 easing set are *"already imported"* in `ArticleCard.kt` | **Correct.** Lines 3-7 import `Animatable`, `AnimationSpec`, `AnimationVector1D`, `CubicBezierEasing`, `tween` — and neither of those. |
| 2 | `codex-023-s1` | D8's claim that `SwipeGesture`'s constant block *"already carries a citation per constant"* | **Correct.** It was eight bare `const val`s with no comments at all. |
| 3 | `codex-023-s1b` | That the brief demanded `EXIT_DURATION_MS = 300` **and** that item 008's tests pass unedited | **Correct.** `SwipeGestureTest.kt:139` asserted `assertEquals(280, SwipeGesture.EXIT_DURATION_MS)`. The two requirements cannot both hold. |

**`design.md` D8 was wrong on both of its factual claims. It has been corrected in place**, with the
original text retained beneath the correction so the change is visible rather than silent. It was believed
twice — once by the design pass, once by the orchestrator who copied it into a brief. This is exactly the
failure `execution-model.md` §5 names: *a claim written when true and stale when used, with no gate that
distinguishes the two.*

**The decision D8 records was never in doubt** — no new dependency, no new file, constants in
`SwipeGesture`. Only its claims about the file were false, which is what makes it the expensive kind of
error: the conclusion looked fine, so nobody re-read the premises.

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

**Commits:** RED `d4844d7`, GREEN `0e48ad8`, harness correction `37d46db`, review finding `5dbf8a8`.

**Gates, re-run independently by the reviewer with `--rerun-tasks`:**

| Gate | Result |
|---|---|
| `:app:testDebugUnitTest` | **400 tests, 0 failures, 0 errors, 0 skipped** (397 → 400) |
| `:app:assembleDebug` | ✓ |
| `:app:assembleDebugAndroidTest` | ✓ |
| `:app:connectedDebugAndroidTest` | **23 tests, 0 failures, 0 skipped**, Pixel_10 AVD, API 37 (20 → 23) |

Both guards **byte-identical to `main`** throughout, verified by `git diff` over both paths at every gate.

### What the production change is

One `Animatable(0f)` named `entranceProgress`, added to the **existing** `remember(article.id, …)` block and
driven by `LaunchedEffect(gestureValues)`. The existing `graphicsLayer` gains two lines:

```kotlin
alpha = gestureValues.alpha.value * entranceProgress
translationY = entranceRisePx * (1f - entranceProgress)
```

Multiplying the exit's alpha by the entrance's progress composes both without either knowing about the
other. `ENTRANCE_DURATION_MS = 300` and `ENTRANCE_RISE_DP = 12f` join the constant block with §79.5
citations. **Twenty-one added lines of production code in total.**

### The curve was ambiguous between §79.5 and D3, and was resolved before dispatch

§79.5's table says `easing decelerated`. D3 says *"Material 3 Emphasized Decelerate"* — **which exists
nowhere in this codebase.** §79.2 says *"Decelerated (Out) curve"* and item 021 shipped that as
`LinearOutSlowInEasing` (`SettingsSheetMotion.kt:29`), and D3's own argument cites §79.2 as its precedent.

**The entrance therefore uses `LinearOutSlowInEasing` at §79.5's `300ms`** — not §79.2's `350ms`, and not a
newly authored path. Resolved by the orchestrator in the brief rather than left for an implementer, because
slice 1 had just shown what an unresolved claim costs.

### A fourth stop, and the second exception — this one on the implementer's own test

The first slice-2 session wrote both new instrumented tests, wrote the implementation, and then **stopped on
a bug in its own test.** `DiscoverScreenLayoutTest.kt:82` and `:103` asserted

```kotlin
assertEquals(host.entranceObservedAt, composeTestRule.mainClock.currentTime)
```

— that injecting touch input leaves the test clock unchanged. **It does not**; `performTouchInput` advances
it by a frame or two (`expected:<752> but was:<784>`, and `<768>`).

**The exception was granted, and deliberately not the one that was asked for.** The session proposed
asserting that the gesture *finishes within* the entrance duration, which quietly shifts what is proven. The
orchestrator required instead a strict bound on elapsed time:

```kotlin
composeTestRule.mainClock.currentTime - host.entranceObservedAt < SwipeGesture.ENTRANCE_DURATION_MS
```

**Grounds:** the assertion was new, authored in this slice's own RED; it asserted a property of the *test
harness*, which no specification section states; and the production code was not implicated — the entrance
and pointer-tracking assertions above it had already passed. **What it was for — proving the swipe lands
while the entrance is running — is this slice's whole acceptance criterion and had to survive.**

It landed in its own commit, `37d46db`, separate from GREEN, so the gate could see the test change on its
own. The attribution assertion is byte-identical. The stronger positional proof was declined with a stated
reason — a mid-gesture position also carries the swipe rotation, and the resting baseline is local to the
unchanged helper — rather than skipped silently.

### The slice gate returned one finding, and closing it needed no production change

**FINDINGS:** *"a restored card arrives the same way"* (`spec.md` §5) was covered **only by a structural
argument** — an `ArticleCardTest` case reasoning from source text that because the entrance rides
`remember(article.id, …)`, an undo-restored head must get it too. `grep -i undo` over `androidTest/`
returned nothing.

**The reasoning was right — and that is not what this project accepts on this surface.** `design.md` D4
rejects exactly this argument for the input scenario: satisfied-by-construction *"is the kind of claim that
stops being true after an unrelated refactor."* **Item 015 exists because an undo-adjacent path behaved
differently than structural reasoning predicted.**

`5dbf8a8` adds `anUndoRestoredCardRisesAndFadesAndImmediatelyTracksASwipe` — 56 lines, one file, additions
only, **no production change needed.** It holds the restored card to the replacement case's exact standard:
a rise bounded between `rest + 0.5f` and `rest + ENTRANCE_RISE_DP * density`, a fill pixel that differs from
the settled opaque one **and** from the magenta backdrop — so "still fading" cannot be satisfied by a card
that never appears — two tracked pointer movements, and the elapsed-time bound.

**The structural argument was correct. It is now held by a test instead of by reasoning.**

### A known consequence, named rather than fixed

The `remember` key has **six** members, not one, so the entrance also replays on a configuration change —
rotation, density, a reduced-motion toggle — not only on a new article. D3 rode that key deliberately to
avoid new state plumbing, and a separate key is exactly the plumbing it avoided. **Accepted, recorded in the
GREEN commit message, and left alone.**

---

## 4. The owner walkthrough — performed 2026-09-22, and it did not pass

Signed **release** build on the Pixel_10 emulator, `animator_duration_scale` = **1.0**, live dataset (209
articles, content age 1d). Both preconditions §6.4 demands were satisfied and recorded before the pass
began. `keystore.properties` was copied into the worktree for the build and deleted immediately after; the
release package was uninstalled afterwards so the next `connectedDebugAndroidTest` is not blocked.

**Every mechanical claim in §6.4 holds. §6.5's character judgement does not.** The owner's verdict, in the
owner's own terms:

| § | Step | Verdict |
|---|---|---|
| 1 | The card fades as it leaves | **Visible, and better than before — but still too fast**, and the exit *speeds up* on release rather than continuing at the speed the gesture had. |
| 2 | The replacement rises and fades | **Present but barely perceptible.** *"The movement is so fast that the human brain barely recognises there was one"* — rise and fade are not distinguishable from each other. |
| 3 | A swipe during the entrance is accepted | **True, and confirmed by hand.** A tap timed to the card's arrival is read and acted on. A tap during the *gap before* it is not — because there is no card yet. |
| 4 | Undo restores and is immediately swipeable | **True, and it is the benchmark.** *"If we could reduce the gap between swipes to be similar to the gap between the undo and the restored card appearing, we will have an optimal UX."* |
| 5 | Reduced motion | Not separately reported; superseded by the finding below. |
| 6 | A vertical drag still scrolls | No regression reported. |

### The finding: the gap between exit and entrance, and it is not a motion defect

**The two animations do not overlap, and roughly half a second of nothing sits between them** — long enough,
in the owner's words, *"for my brain to doubt whether a new card will arrive."*

**The cause is in the commit path, not in any animation value.** `ArticleCard.kt:146-150` waits for
`animateToGestureState()` to complete, *then* calls `onSwipeCommit` →
`AppViewModel.launchArticleAction` → `onArticleAction`, which takes `stateMutex`, runs the transition, and
calls **`saveLocalState`** — a disk write — followed by `adoptPersistedState`, which re-ranks the whole
deck. All of it on `Dispatchers.Main.immediate`. Only when that returns does the head article change, the
`remember(article.id, …)` rebuild, and the entrance begin.

**The exit's 300 ms and the persistence are sequential when they could be concurrent.** The main thread is
largely idle while the exit animates on the frame clock, and the expensive work is queued behind it instead
of alongside it.

**This is why undo feels right and a swipe does not.** Undo persists too — `performUndo` takes the same
lock and the same `persistUndoTransition` path. What it lacks is a *preceding animation*: it is tap → state
change → entrance, with no dead zone between two moving things. The swipe path serialises
exit → persist → entrance, and the dead zone is what the eye reads as the card having gone missing.

**Item 023 did not create this gap. It made it legible.** Before this item the replacement simply
materialised, so there was nothing to wait for; now there is an entrance, and the wait in front of it has a
shape.

### Three findings, none of them inside this item's scope

1. **The exit→entrance gap.** Fixing it means starting the commit concurrently with the exit and swapping
   the head only when both have finished. That changes the commit sequencing, which is **D2** — this item's
   central constraint, adopted specifically to keep items 013 and 015 closed. It is a design pass, not a
   value change.
2. **The exit discards the gesture's velocity.** It is a fixed-duration `tween`, so a slow drag released
   at the threshold snaps to full speed. The browser does the same, which is why item 008 ported it that
   way. `Animatable` supports `animateDecay`, or `animateTo` with an `initialVelocity`, so the owner's
   *"smooth movement for the card vanishing"* is reachable — but §44.2 fixes the exit's curve and duration,
   so carrying velocity needs an amendment.
3. **Destination transitions show both tabs' text at once.** New, raised during this pass: moving between
   Read Later, Discover and History renders the outgoing and incoming labels simultaneously for an instant.
   That is item **021**'s `AnimatedContent` in `IntentionalReadingApp.kt:280-310`, not this item's ground.

**§6.5's second checkpoint — §44's *tactile, quiet, controlled*, open in `backlog.md` since wave B —
remains open.** It is now open with a named cause and a named lever for the first time, which is more than
any of items 008, 013 or 015 left behind.

---

## 5. Outstanding

- **The owner walkthrough, `spec.md` §6.4.** Not performed. It requires a **release** build and
  `animator_duration_scale` **non-zero** — both preconditions cost this project a misdiagnosis on
  2026-09-20. The scale currently reads `1.0`.
- **It carries §44's character judgement** — *tactile, quiet, controlled* — which `backlog.md` has held open
  since wave B and which items 008, 013 and 015 did not close. **This is the fourth item on this surface and
  the first to change how it animates; closing that judgement is the deliverable.**
- **If the finding is "sluggish", the lever is the entrance duration, not the exit** (D7). The exit is fixed
  by §44.2 and D1. The total gesture is now about 600 ms — exit 300, state action, entrance 300 — because
  D2 makes them sequential.
- **Hosted CI green on the exact final head**, then final review.
- **The Emphasized easing is defined twice** — `SwipeGesture.ExitEmphasizedEasing` and item 021's
  `IntentionalReadingApp.kt:271-279`. Belongs in `backlog.md`'s Debt section; absorb it when something next
  touches `IntentionalReadingApp.kt`.
- **`ArticleCardTest` asserts against source-file text in many places**, a convention that predates this
  item — eleven such assertions on `main` — and which this item followed and extended. Also Debt.
