# 024 — The next card arrives without a pause · evidence

**Branch:** `feat/024-android-card-arrival-gap`, cut from `main` at `7667ddf`\
**Slices:** 1, done · **Tests:** 400 → **403** unit, 23 → **28** instrumented, 0 failures at every gate\
**Implementer:** Codex, two fresh sessions (`codex-024-s1`, `codex-024-s1b`)\
**Reviewer:** the orchestrating Claude session — brief author, not code author
(`execution-model.md` §5)\
**Amendment:** 12, authored by this item.

---

## 0. The design pass corrected the record it inherited

The handover and the backlog entry for 024 named one cause and one fix: the save ran on the main thread
after the exit, so start the save alongside the exit. **Reading the code against those claims found three of
them wrong** (`spec.md` §1.2):

- The disk write is on `Dispatchers.IO` (`LocalStateRepository.kt:23`), not `Dispatchers.Main.immediate`.
- The deck re-rank is in `publish()`, not `adoptPersistedState`.
- **Most of the gap was not the save.** The Emphasized curve carries a phone-width card off-screen ~55–60 ms
  into its 300 ms; the commit waited out the remaining ~245 ms of curve that drew nothing. Saving alongside
  the exit would have removed only the smaller part.

This is 023's `evidence.md` §1 lesson recurring one level up: a claim written when believed and stale when
used. It was caught because the design pass verified the handover's claims against the code with line
numbers before writing any of its own. **Treat a handover's claims like a design note's.**

## 1. Slice 1 — the card requests its commit once it has left the viewport

**RED `ecc6892`, harness correction `e884150`, GREEN `123528a`.** Slice gate PASS.

| Gate | Result |
|---|---|
| `:app:testDebugUnitTest` | **403 tests, 0 failures, 0 skipped** (400 → 403) |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest` | pass |
| `:app:connectedDebugAndroidTest` | **28 tests, 0 failures, 0 skipped** (23 → 28), `Pixel_6_API_34` emulator |

All four were re-run independently by the orchestrator on `123528a` after the implementer reported.

### What the production change is

- `SwipeGesture.hasDepartedViewport` — a pure function: the card's rotated bounds, translated, are wholly
  outside `[0, viewportWidth]`. Half-extent `(w/2)·cos θ + (h/2)·sin |θ|` about the centre.
- `ArticleCard` measures itself with `onPlaced`, placed **before** `graphicsLayer` so the drag and exit
  transforms are not counted twice. The commit branch runs a new `animateExit` whose translation frame block
  evaluates departure; the commit is requested on the first departed frame, or after the exit returns if it
  never departs. One local guard makes both paths request it at most once.
- A failed save mid-exit cancels the running exit (a new `animateTo` on the same `Animatable`), so the
  fallback request after the exit is never reached — the "no second commit" instrumented assertion holds it.

### The implementer stopped once, and was right to

The first session's own RED test for the wide-viewport fallback laid the host out at 840 dp while
`LocalConfiguration.screenWidthDp` still reported the device's 411 dp — and `ArticleCard` reads its viewport
from `LocalConfiguration`. The test therefore could not reach the condition it named. The session reported
it rather than editing the test. **`e884150` is a harness-only correction:** `departureHost` provides a
`LocalConfiguration` whose width matches the width it lays out, for every host width. No assertion, timing
or test name changed.

### One bounded test exception

The RED commit removed one assertion from `ArticleCardTest`'s source-text test — a regex requiring
`animateToGestureState()` to be followed immediately by `currentOnSwipeCommit(...)`, messaged *"the state
action must still wait for the entire exit."* That is exactly the behaviour Amendment 12 replaces; it could
not survive this item. The test's other assertions (the coordinated fade, the draw-time alpha) remain, and
the new timing is held by the instrumented tests. Removed in RED, visibly, not edited to pass in GREEN.

### Protected tests

`ArticleCardGestureTest`, `ArticleCardScrollGestureTest` and 023's three entrance tests
(`DiscoverScreenLayoutTest.kt:57-176`) are byte-identical to `main` and pass.

## 2. The owner walkthrough — performed 2026-09-29, and it passed

Signed **release** build (signer SHA-256 `baf9fe55…4325`, verified with `apksigner`), `Pixel_6_API_34`
emulator, `animator_duration_scale` = **1.0**, live dataset (193 articles, content age 2h).
`keystore.properties` was copied into the worktree for the build and deleted immediately after; the release
package was uninstalled afterwards.

| § | Step | Verdict, in the owner's words |
|---|---|---|
| 1 | Swipe left and right | *"no pause between new card and old card"* |
| 2 | Side by side with undo | *"is the same lapse"* |
| 3 | Swipe the replacement as it arrives | *"second card is responsive and takes the action"* |
| 4 | A vertical drag still scrolls | *"vertically drag scroll the page up and down"* |
| 5 | Reduced motion | Not performed — the owner declined it. Covered by the instrumented `reducedMotionIsUnchangedForDepartureAndArrival`. |

**`design.md` D7's prediction held:** the remaining wait is the save, and it reads like undo's. The lever
held in reserve — starting the save before departure — is not needed.

## 3. Outstanding at close

- **Landscape and viewports above ~615 dp keep the old timing.** The card leaves them by fading, not by
  travel, so the curve-completion fallback governs (`spec.md` §4). Not a regression; not walked.
- **The exit still discards the gesture's release velocity**, and destination transitions still show both
  tabs' text at once — both recorded in `backlog.md`, neither this item's.
- **§44's *tactile, quiet, controlled* judgement.** The named cause the walkthrough of 023 found is gone and
  the owner found nothing else wrong; the owner did not separately state the character judgement.
- **Hosted CI on the exact final head** — recorded in the PR once green.
- **Environment, new this item:** the Android SDK, emulator and AVD were installed on this machine for the
  first time (2026-09-29). Codex's workspace sandbox cannot write a linked worktree's git index even with the
  common `.git` added as a writable directory; every commit needed an approval.
