# 013 — A Discover card must accept a swipe as soon as it is on screen

**Workstream:** `android-client`, under Amendment 6. Owned paths: `android/**` plus this item's own
`specs/013-android-undo-gesture-reset/`. Forbidden: `pipeline/**`, `config/**`, `js/**`, `css/**`,
`index.html`, `scripts/**`, `tests/**`, **and `docs/v1/**`** — this item proposes no specification
amendment and needs none.

**Cut from:** `main` at `2613959`.

> **Third revision, 2026-08-27.** The first version named the wrong cause (§1.3). The second version got
> a cause right, fixed it, and shipped a green instrumented guard for it — and the walkthrough then showed
> the headline symptom still live on the Undo path (§1.4). This revision keeps everything the second
> version proved, states plainly that **the remaining mechanism is not known**, and makes diagnosing it a
> gate that must close before any third fix is written (§5.1). It also records the owner's artificial-lag
> proposal and what evaluating it concluded (§1.6, `design.md` D9).

---

## 1. Why this item exists

### 1.1 What the reader sees

Swipe a card, tap **Undo**, then swipe the card that comes back. Roughly a third to half a second after
Undo, that swipe does nothing at all — the card does not move, no cue appears, nothing is recorded. Wait
a beat longer and it works.

### 1.2 What the first two revisions established, and it stands

`ArticleCard.kt` built a new `SwipeGesture.State` per article through `remember(article.id, …)` and keyed
the card's touch handling on it with `Modifier.pointerInput(gestureState)`. **`pointerInput` restarts its
handler whenever its key changes**, so every change of the Discover head article tore the gesture handler
down and relaunched it, and the relaunch was starved by `DiscoverScreen.kt:74-87`'s article-change scroll
animating on the same frame clock. Measured attach latency: **381–777 ms** behind the card becoming
visible and settled.

```
10:58:10.825  compose ArticleCard article=eb011ad…    ← restored card enters composition
10:58:10.931  scroll article effect changed=true       ← the D12 scroll runs
10:58:10.978  cardBounds … centerY=1317.0             ← settled at its final position
10:58:11.206  pointerInput start article=eb011ad…     ← handler finally attaches, +381 ms
              (no "awaitFirstDown returned" — the DOWN was discarded)
```

That was real, it was fixed at `29344aa` by keying the handler on `Unit` and reading per-article values
through current state, and **the fix works on the path it was aimed at**. Two fast consecutive swipes
0.35 s apart, no Undo, now produce two records and move two source weights, where before the fix they
produced one (`investigation/post-fix-walkthrough.md`, `walkthrough-state-fastpair.json`). That evidence
is not in question and `29344aa` stays.

### 1.3 What the first revision got wrong

It blamed `SwipeGesture.State.commitInFlight` staying latched after a successful commit, and a fix
(`bf79c42`, `releaseCommitLock()`) was written, reviewed and merged against that theory. In the failing
trial `gestureState.down(...)` was never called at all; whenever it was called it returned `true` with
`commitInFlight` already `false`. The theory was wrong. `bf79c42` stays, re-justified (§1.7).

### 1.4 What the second revision got wrong

Not the diagnosis — the **scope of the claim**. `design.md` D1 asserted that keeping one pointer handler
attached across head-article changes makes the card touchable. It does, for a **re-key**. It does not
cover the Undo restore path, and `spec.md` §5.3's instrumented guard could not have noticed, because that
test asserts handler persistence across a re-key, which is exactly what was fixed.

Measured after the fix at `29344aa`, on the `Pixel_10`, records counted after the reswipe (Undo removes
the record, so **1 = the reswipe landed, 0 = it was discarded**):

| delay after Undo | y = 1760 | y = 1285 (card text area) |
| --- | --- | --- |
| 0.2 s | 1 | 1 |
| 0.3 s | 0, 0 *(2 runs)* | — |
| 0.4 s | 0, 0, 0 *(3 runs)* | **0** |
| 0.5 s | 0, 0 *(2 runs)* | — |
| 0.8 s | 1 | 1 |

Also 0 at 0.4 s with a 300 ms swipe rather than 100 ms, so it is not a fling-sampling artifact, and two
different y values inside the card's true bounds, so it is not a coordinate artifact. A 900 ms drag begun
0.4 s after Undo, photographed 400 ms in, shows the card **not moved at all** — no translation, no cue
(`walkthrough-mid-drag-undo-0.4s.png`); the identical harness at 1.2 s shows it translated and rotated
(`walkthrough-mid-drag-undo-1.2s.png`). The touch is not reaching the handler.

### 1.5 What is not known, and is the whole of this revision

**Why.** And in particular **why 0.2 s passes.** A pure attach-latency story predicts a monotonic floor:
too soon fails, later works. The measurements are not monotonic — they are a **band**, open on both sides.
Whatever is happening at 0.3–0.5 s is either not present at 0.2 s or not yet armed, and is gone by 0.8 s.
No mechanism has been proposed that accounts for that, so no fix can be aimed at one.

This item has now stopped twice, and both stops have the same shape: a plausible mechanism was promoted to
a cause, and code was written against it. `design.md` D8 lists the four candidate mechanisms and the
observation that separates each from the others; §5.1 makes closing that question a gate that must pass
before slice 2 exists. **A third fix written against an untested cause is the failure mode this revision
exists to prevent.**

### 1.6 The artificial-lag proposal, and what evaluating it concluded

The owner proposed, on 2026-08-27, introducing a deliberate lag after a successful swipe so that a second
swipe cannot land close enough in time to be lost. It was evaluated in full (`design.md` D9) and is
**rejected as an input lag**, on this item's own measurements:

- Lag after a **successful swipe** guards the deck-advance path, and §1.2 shows that path already accepts
  a swipe 0.35 s later. It does not touch the path that is broken.
- Extended to the **Undo restore**, the gate would have to span 0–0.5 s to cover the band — and §1.4 shows
  **0.2 s works today**. It would refuse a swipe that currently lands.
- A silent refusal is the defect wearing a policy hat: the reader still swipes and nothing still happens.
  Making it honest needs a visible non-interactive card state, and `docs/v1/06-ui-ux.md` §39–§45 specify
  the swipe surface, the threshold, 280 ms motion and a 180 ms toast while saying nothing about a card
  that declines touch. `AGENTS.md` forbids inventing a requirement where the specification is silent.

One form survives, and only as a **fallback remedy, not as this item's plan**: lag on the *state change*
rather than on the input — hold the restored head article until the frame clock is quiet, so the card
enters composition already able to hear touch. It refuses nothing, it is invisible to the reader, and it
invents no UX. `design.md` D10 pre-registers it against one specific §5.1 outcome, bounded by an observable
settle signal rather than by a wall-clock constant.

### 1.7 What stays on the branch

`bf79c42` (`releaseCommitLock()`), `1f19cea` (the instrumented gesture-spanning test) and `29344aa` (the
persistent-handler fix) all stay, with their four plus one tests exactly as written. They are partial,
correct, evidenced work: the deck-advance path is fixed and measured, and the lock release is the
precondition that makes a persistent handler safe. The record of *why* each was committed is corrected in
place rather than rewritten, because a future reader deserves to see that code can be right for a reason
that is wrong.

### 1.8 What is somebody else's item

- **The Save for later button** fails in the same window and never consults the gesture state, so no fix
  here would address it. **Item 014**, from its own reproduction. Owner decision, 2026-08-27.
- **Undo in Read Later and History**, and **Discover's header order** (item 012). Unchanged.

---

## 2. Story

As a **reader**, I want a Discover card to accept my swipe as soon as it is on screen, so that a swipe I
make immediately after the deck changes — including immediately after Undo returns a card — is not
silently lost.

---

## 3. Out of scope

- **Any input lockout, refusal window, or visible non-interactive card state.** §1.6, `design.md` D9/D11.
- **Any change to `docs/v1/**`.** This item proposes no amendment.
- **Item 014's buttons.** §1.8.
- **Which inputs offer Undo.** The labeled buttons stay non-undoable and no keyboard shortcut is added:
  `contracts.md` §31, `06-ui-ux.md` §70, 007 `spec.md` §1.1, 008 D8.
- **Undo in Read Later and History.**
- **`SwipeGesture.kt`, `SwipeGestureTest.kt`, and the undo offer's duration, copy or affordance.**
- **Discover's header layout and the existence of its three scroll effects** — item 012's ground. If §5.1
  names one of those effects, this item may change **how a DOWN is arbitrated against it** or **when the
  restored article is published**; it still may not restructure the header or delete an effect.
- **Instrumented tests in CI.** Parked (002 slice 4). Both guards stay local and on-demand.

---

## 4. Scenarios

### Already satisfied at `29344aa`, and must not regress

### Scenario: a card accepts a swipe immediately after the deck advances

Given a Discover card has just been committed by a swipe\
And the next card has become the head article\
When the reader swipes that new card as soon as it is on screen\
Then the swipe is received and commits its action

### Scenario: the gesture state is still per-card

Given the head article changes\
When the new card is swiped\
Then its travel and its commit lock are its own\
And no travel or latched commit carries over from the card that left

### Scenario: the touch handler survives a head-article change

Given a swipe is in progress on the Discover card\
When the head article changes while the pointer is still down\
Then the gesture is not cancelled\
And carrying it past the commit threshold and lifting still commits

### Scenario: a gesture commits against the card it started on

Given a swipe is in progress on the Discover card\
When the head article changes while the pointer is still down\
And the reader carries the gesture past the commit threshold and lifts\
Then the committed article is the one the gesture started on

### Scenario: a committing card still refuses a second gesture

Given a card whose swipe has committed and whose commit has not yet resolved\
When a second gesture is attempted on it\
Then the gesture is refused\
And this is the existing assertion at `SwipeGestureTest.kt:164`, which does not change

### What this revision adds

### Scenario: a card accepts a swipe immediately after Undo returns it

Given a swipe has been committed and the reader has tapped Undo\
When the reader swipes the returned card as soon as it is on screen\
Then the swipe is received and commits its action\
And it raises its own undo offer

### Scenario: a swipe is never discarded because the screen is still settling

Given the Discover card is visible and settled at its final position\
When the reader begins a swipe on it\
Then the card responds to the drag\
And it does so regardless of what else on the screen is animating

### Scenario: no reader input is refused in order to achieve this

Given the reader swipes a visible Discover card at any moment after it appears\
When the swipe passes the commit threshold and the pointer lifts\
Then the action commits\
And no interval exists in which the card deliberately declines a touch

---

## 5. Verification

### 5.1 The diagnosis gate — must close before any fix is written

**No product-code change may be committed for the Undo window until this gate reports a named mechanism.**
`design.md` D8 lists the four candidates and the observation that separates each. The gate closes when the
report does all four of these:

1. **Names one mechanism** and states the observation that confirmed it.
2. **Explains why 0.2 s passes** and why 0.8 s passes. A story that only explains the failures has not
   closed this gate — §1.5 is precisely the shape of the two previous stops.
3. **Falsifies, or explicitly rules out with evidence, the other three candidates.**
4. **Names the fix shape** it implies, from `design.md` D10's pre-registered map, or says that none of them
   fits — in which case **stop and report**, do not improvise a fix.

The cheapest discriminator runs first and needs no instrumentation: **repeat the 0.4 s trial with reduced
motion enabled.** `DiscoverScreen.kt:81-85` replaces `animateScrollTo` with an instant `scrollTo` in that
mode, so if the window closes under reduced motion, the article-change scroll is implicated and candidate
M1 is live; if it does not, M1 is out. Run it before anything is built.

Recording: `investigation/step0-undo-window.md`, with pulled state documents, `uiautomator` dumps, and
`logcat` for every claim, on the terms §5.4 already sets — **a card leaving the deck is not proof that the
action landed.**

### 5.2 Gates

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

Head of this branch: **258 tests, 0 failures, `BUILD SUCCESSFUL`**. Delete `test-results` before every run
and read the `BUILD SUCCESSFUL` line rather than the counts (`waves/wave-b-note.md` §7).

### 5.3 The existing instrumented guard stays, and is not the new evidence

`ArticleCardGestureTest` (`1f19cea`) asserts that a gesture spanning a head-article change still commits,
against the article it started on. Its RED was reproduced independently in a throwaway worktree with the
fix absent — `expected:<[Article(id=first, …)]> but was:<[]>`, no commit recorded. It is honest and it
stays byte-identical unless slice 2's fix genuinely changes what it asserts.

It is **not** evidence about the Undo window, and it did not catch it. Do not extend it to try.

Two constraints carried forward, both earned:

- **Do not try to reproduce a timing window in `ComposeTestRule`.** Seven variations failed to; the
  harness idles until composition and its coroutines settle, which is the condition under which there is
  no window (`investigation/step0-reproduction.md`).
- **A programmatic animation driven by the test clock is a different thing from an attach race.** If §5.1
  names M1, an ancestor scroll animation *can* be held mid-flight deterministically with a paused
  `mainClock`, and that is a legitimate RED. `design.md` D10 says so per mechanism. Nothing else about the
  first constraint is relaxed.

### 5.4 Walkthrough — required evidence

Driven over `adb` on the `Pixel_10`. **Aim every swipe at the card's true bounds, read from
`adb shell uiautomator dump`** — fixed coordinates confounded the first attempt at this item, and a
mid-trial layout shift produced a false failure in the second (`investigation/post-fix-walkthrough.md`).

1. Two fast consecutive swipes, no Undo. Both commit. *(Passes today; re-run as a regression.)*
2. Swipe → Undo → immediate swipe at **0.2, 0.3, 0.4, 0.5, 0.8 s**. All commit and all raise an offer.
   The three middle delays are the defect; 0.2 s and 0.8 s must not regress.
3. The 0.4 s case with a 900 ms drag, photographed mid-drag: the card must be translated and showing its
   cue, as `walkthrough-mid-drag-undo-1.2s.png` does today.
4. A settled control, 3 s apart, still commits.

For each, confirm from the pulled state document that a weight and a count actually moved.

### 5.5 Stop conditions

Stop and report rather than proceeding if: §5.1 cannot name a mechanism; §5.1 names one but cannot explain
the 0.2 s pass; the fix `design.md` D10 pre-registers for that mechanism does not produce a genuine RED; or
the §5.4 walkthrough fails after the gates are green. **The last one has now happened once, and catching it
is why the walkthrough is definition-of-done rather than optional.**
