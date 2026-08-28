# 013 — slice plan (third revision, 2026-08-27)

Sized **S → three slices**, and the split is the point of this revision: the first slice writes **no
product code at all**. One item branch (`feat/013-android-undo-gesture-reset`), one PR targeting `main`.

Scenario names refer to `spec.md` §4. `«pkg»` = `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`;
JVM tests under `android/app/src/test/kotlin/io/irodriguez/intentionalreading/`; instrumented tests under
`android/app/src/androidTest/kotlin/io/irodriguez/intentionalreading/`.

**Every Gradle invocation needs both exported first** — `java` is not on this machine's `PATH` and a
worktree has no `local.properties`:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

Head of this branch: **258 tests, 0 failures, `BUILD SUCCESSFUL`**. Delete `test-results` before every run
and read the `BUILD SUCCESSFUL` line, not the counts (`waves/wave-b-note.md` §7).

## What changed since the last plan, and why

The last plan's single slice ran to completion. Its RED was real, its fix was merged to the branch, all
three gates went green — and the walkthrough then found the headline symptom still live on the Undo path
(`investigation/post-fix-walkthrough.md`). The deck-advance half is genuinely fixed and stays.

So this revision does one thing: **it stops the item from writing a third fix against an untested cause.**
`spec.md` §1.5 states plainly that the remaining mechanism is unknown and that nothing yet explains why a
swipe 0.2 s after Undo lands while one at 0.4 s does not. `design.md` D8 lists four candidates with a
discriminator each, D10 pre-registers a fix shape per candidate, and slice 1 below is the diagnosis that
picks one. Slice 2 may not begin until it has.

The owner's artificial-lag proposal was evaluated and is **rejected as an input lag** — it does not touch
the failing path, and a gate wide enough to cover the band would refuse the 0.2 s swipe that works today
(`spec.md` §1.6, `design.md` D9). One form of it survives as a fallback remedy inside D10, and only under
one specific diagnosis outcome.

## Fixed for this item — do not re-decide these mid-implementation

- **No product code before `spec.md` §5.1 closes.** Slice 1's output is a named mechanism, an explanation
  of the 0.2 s and 0.8 s passes, and three candidates ruled out with evidence. Anything less is a
  stop-and-report, not a licence to start on the leading candidate.
- **Do not try to reproduce the timing window in `ComposeTestRule`.** Seven variations already failed to
  (`investigation/step0-reproduction.md`). The one thing that is now permitted, and only under D10's M1
  branch, is holding a **programmatic scroll animation** mid-flight on a paused `mainClock` — that is a
  controllable animation, not an attach race.
- **No input lockout, refusal window, or delay on a reader's touch.** `design.md` D9. If a deferral is used
  at all it defers the *state change*, and it is bounded by an observable settle signal, never by a
  millisecond literal chosen to sit outside a measured band.
- **No visible non-interactive card state**, and no `docs/v1/**` amendment (`design.md` D11).
- **`«pkg»/ui/gesture/SwipeGesture.kt` is not touched**, and `SwipeGestureTest.kt` stays byte-identical,
  including `:164` (`design.md` D2, D6).
- **`ArticleCardGestureTest` (`1f19cea`) stays byte-identical** unless slice 2's fix genuinely changes what
  it asserts. Do not extend it to chase the Undo window; it cannot see it (`spec.md` §5.3).
- **The gesture state stays per-article**, and every per-article value the handler reads comes through
  current state rather than capture (`design.md` D5, D6).
- **Discover's header layout and the existence of its three scroll effects are item 012's ground.** If the
  diagnosis names one of them, this item may change how a DOWN is arbitrated against it, or when the
  restored article is published — not the header, and not by deleting an effect (`spec.md` §3).
- **No new dependency.** `androidx.compose.ui.test.junit4` and `…ui.test.manifest` are already declared;
  do not add `androidx.test.uiautomator` (`design.md` D3, rejected alternative).
- **No test-only parameter on `ArticleCard`** (`design.md` D3, rejected alternative).
- **Instrumented tests stay out of CI** (002 slice 4). Both guards are local and on-demand.
- **Item 014's buttons are out of scope**, including in the walkthrough.
- **A card leaving the deck is not proof that the action landed.** Every claim in slices 1 and 2 is
  confirmed against a pulled state document. That mistake has produced a false pass twice on this branch.

---

## Slice 1: name the mechanism behind the 0.3–0.5 s Undo window

**Diagnosis only. No product code, and therefore no failing-test-then-implementation pair** — the one
deliberate exception to the project's slice shape, because there is nothing to test until there is a named
cause. The commit is `docs(spec)`.

- **Scenarios:** none directly. This slice exists to make "a card accepts a swipe immediately after Undo
  returns it" and "a swipe is never discarded because the screen is still settling" fixable.
- **Gate:** `spec.md` §5.1, all four conditions. **Run the reduced-motion trial first** — it costs one
  `adb` invocation and it puts candidate M1 in or out before anything is built.
- **Method:** `design.md` D8's four candidates, each with its discriminator, cheapest first. Temporary
  `logcat` instrumentation is expected and **must not be committed** — the deliverable is the report and
  its artifacts, not a diff. Every swipe aimed at the card's true bounds from `adb shell uiautomator dump`.
- **Files:** `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md` plus its pulled
  state documents, dumps, `logcat` excerpts and any screenshots. Nothing under `android/` is committed by
  this slice.
- **Reuse:** `investigation/step0-reproduction.md` and `investigation/post-fix-walkthrough.md` as the shape
  of the report and the standard of evidence. The dataset-seeding trick from the walkthrough's method notes
  if the emulator has no DNS.
- **Reference:** `spec.md` §1.4 for the measurements, §1.5 for what is not known, §5.1 for the gate;
  `design.md` D8 for the candidates and D10 for what each implies.
- **Definition of done:**
  - One mechanism named, with the observation that confirmed it.
  - **The 0.2 s pass and the 0.8 s pass are both explained.** A story that only explains the failures does
    not close this gate.
  - The other three candidates falsified or ruled out, each with evidence rather than by assumption.
  - The `design.md` D10 fix shape it implies is named — or the report says none fits, and stops.
  - No file under `android/` modified; both JVM gates still green at 258 tests.
- **Status:** **done, 2026-08-28** — `investigation/step0-undo-window.md`. Mechanism is **M1**: while
  `DiscoverScreen.kt:74-87`'s `animateScrollTo` is in flight, the ancestor `ScrollView` consumes the DOWN
  in the Initial pass, and `ArticleCard.kt:125`'s `awaitFirstDown(requireUnconsumed = true)` will not adopt
  it. M2, M3 and M4 falsified with evidence. `design.md` D10's M1 branch applies unchanged.
  The 0.8 s pass is the scroll having finished; **the "0.2 s pass" was never a pass** — at the bottom of
  the range the DOWN is adopted against the *outgoing* article, and the previous walkthrough scored it as
  a success because it checked that a weight moved rather than which article moved.

---

## Slice 2: make the card accept a swipe on the Undo restore path

Begins only after slice 1's gate closes. Its content is whichever `design.md` D10 branch slice 1 named, so
this slice is deliberately written against the mechanism rather than against a file.

- **Scenarios:** "a card accepts a swipe immediately after Undo returns it", "a swipe is never discarded
  because the screen is still settling", "no reader input is refused in order to achieve this", plus the
  five already-satisfied scenarios in `spec.md` §4 as regressions.
- **RED first.** The test shape comes from D10's branch for the named mechanism. It must fail against
  unchanged production code **for the stated reason**. A pass, or a failure with a different signature, is
  a stop-and-report — that stop condition has saved this item once already.
- **Files:** whichever of `«pkg»/ui/components/ArticleCard.kt`, `«pkg»/ui/screens/discover/DiscoverScreen.kt`,
  `«pkg»/ui/AppViewModel.kt` or `«pkg»/ui/IntentionalReadingApp.kt` the named mechanism lives in — the
  narrowest one that carries it — plus its test. Name the file in the slice-2 brief once slice 1 has run;
  do not touch more than one production file without saying why.
- **Must not touch:** `«pkg»/ui/gesture/SwipeGesture.kt`, `«pkg»/domain/**`, `«pkg»/data/**`, `res/**`, any
  Gradle file, `docs/v1/**`, anything outside `android/`.
- **Reuse:** `MainActivityLaunchSmokeTest` and `ArticleCardGestureTest` as the terms of a local instrumented
  guard. `rememberUpdatedState`, already used in `ArticleCard.kt:117-118`. `SwipeGesture.State` unchanged.
- **Reference:** `design.md` D10 for the fix shape, D5 for the stale-capture hazard `29344aa` created,
  D6 for the per-article invariant, D9 for what is not allowed to be the fix.
- **Definition of done:**
  - The new test failed first for the stated reason, and passes after the fix.
  - `./gradlew :app:connectedDebugAndroidTest` green locally against the running `Pixel_10`, both guards
    included.
  - Both JVM gates green at 258 tests plus any added, 0 failures.
  - `SwipeGesture.kt` and `SwipeGestureTest.kt` untouched; no existing assertion changed anywhere.
  - **`spec.md` §5.4's walkthrough, all four steps, driven by the orchestrator over `adb` and recorded** —
    including the 0.2 s and 0.8 s cases, which must not regress, and the mid-drag photograph. Each result
    confirmed against the pulled state document.
  - No input is refused at any delay, and no non-interactive treatment was added (`design.md` D9, D11).
- **Status:** **done, 2026-08-28** — `49c1ecc` (RED) then `11d8353` (fix: `ArticleCard.kt:125`,
  `requireUnconsumed = false`, one line, one file), plus `5a86003` adding the vertical-scroll ownership
  guard raised by slice review. Walkthrough: `investigation/slice2-walkthrough.md` — all four §5.4 steps
  pass and every delay from 0.05 s to 1.2 s now commits **against the restored article**, where 0.2–0.4 s
  were discarded before. Gates: 258 JVM tests 0 failures, `assembleDebug` and
  `connectedDebugAndroidTest` (4 tests) `BUILD SUCCESSFUL`. RED reproduced independently at `49c1ecc` with
  the fix absent; the new guard verified non-vacuous by mutation (forcing `Intent.HORIZONTAL` in
  `SwipeGesture.move()` fails it). `SwipeGesture.kt`, `SwipeGestureTest.kt` and `ArticleCardGestureTest.kt`
  byte-identical. The delay-0 misattribution is **still open** and needs its own item.

---

## Slice 3: close the item

Bookkeeping and evidence, once slice 2's walkthrough is green. `docs(spec)` commits only.

- **`evidence.md`** recording **all four passes** — the wrong cause and the fix built on it; the right
  cause with a verification design that could not work; the right-but-too-narrow cause, fixed, gated
  green, and caught by the walkthrough anyway; and what finally held. The process lessons are the more
  valuable half of this item and must not be tidied away. Three are worth stating outright: *a green unit
  gate never had a chance here*; *a harness that synchronizes away timing cannot test timing*; and *an
  instrumented test that asserts a cause is only as good as the cause* — the third one is new, and it is
  the expensive lesson of the second pass.
- **The artificial-lag evaluation** is part of the record, not a discarded branch: the owner's instinct was
  sound and the measurement is what disqualified it (`design.md` D9).
- **A correction to `specs/005-android-preference-learning/evidence.md`.** Its walkthrough section says
  *"nothing surfaced where the assertion held but the screen was wrong."* This item's defect was live
  throughout that walkthrough, and taps recorded there as "did not register" were attributed to the undo
  toast's 4.5 s timeout when at least some were this defect. 005's results stand — every step was confirmed
  against a pulled state diff — but the attribution was wrong. *(Owed since `f1f381b`; still not
  discharged. Discharge it here.)*
- **`backlog.md` gains item 014** — the Discover card's buttons in the same window, from its own
  reproduction, inheriting no cause (`spec.md` §1.8, `design.md` D7). 014 is free.
- **`backlog.md`: 013 moves to Shipped.** It has no Queued entry; one is not needed on the way through.
- **`backlog.md` Debt: the three `DiscoverScreen` scroll effects.** Update from a tidiness note to
  whatever slice 1 established — if M1 was named, one of them is implicated in a real defect and item 012
  should know it.
- **`backlog.md` Verification debt:** the instrumented suite is two tests and still out of CI; the local
  `connectedDebugAndroidTest` run is the only thing that exercises either. Add any residual Undo-window
  measurement here rather than describing it as intended behaviour (`design.md` D11).
- **`backlog.md`: the queue-pane undo entry** owed from the first design pass — widening
  `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` and adding undo affordances to Read Later
  and History, needing its own amendment and design pass like item 012, citing `contracts.md` §23.
- Note for item 006, next in wave C: confirm at close that this item still touches no file 006 touches.
- **Status:** blocked on slice 2
