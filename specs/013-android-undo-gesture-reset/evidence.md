# 013 — evidence

**A Discover card must accept a swipe as soon as it is on screen.** Shipped 2026-08-28, after **four
passes**. Three named a cause that was wrong, or right but too narrow, or right with a verification design
that could not work. Recording all four, because the process lessons are the more valuable half of this
item and tidying them away is how they get relearned.

Branch `feat/013-android-undo-gesture-reset`, cut from `main` at `2613959`.

---

## What the reader gets

Swipe a card, tap **Undo**, swipe the card that comes back. It works — at every delay, immediately, on the
article the reader is looking at. Before this item, a swipe landing roughly a third to half a second after
Undo did nothing at all: no movement, no cue, nothing recorded.

## The fix, in one line

`ArticleCard.kt:125`, `requireUnconsumed = true` → `false`.

While `DiscoverScreen.kt:74-87`'s article-change `animateScrollTo` is in flight, the ancestor `ScrollView`
consumes the DOWN in the **Initial** pointer pass — Compose's ordinary "a touch stops a running scroll
animation" behaviour. `awaitFirstDown(requireUnconsumed = true)` will not adopt a consumed DOWN, so the
gesture never started. The card's gesture is horizontal and the ancestor's is vertical, so the card is
entitled to look at a DOWN the scroll has claimed; `SwipeGesture`'s intent slop is what stops it stealing a
vertical drag, and `SwipeGesture.kt` was not touched to achieve any of it.

---

## The four passes

### Pass 1 — the wrong cause, and a fix built on it

**Theory:** `SwipeGesture.State.commitInFlight` stayed latched after a successful commit, so the next
gesture was refused. **Fix:** `releaseCommitLock()` (`85a94e4`, `bf79c42`), written, reviewed and merged
against that theory.

**Falsified by instrumentation.** In the failing trial `gestureState.down(...)` was never called at all,
and whenever it *was* called it returned `true` with `commitInFlight` already `false`.

`bf79c42` **stays**, re-justified rather than reverted (`design.md` D2): with one persistent handler, a
gesture state still latched from a previous commit can be swapped in behind it and nothing would release
it. The lock release is the precondition that makes a persistent handler safe. The code was right; the
reason was wrong, and those are different failures. A future reader deserves to see which one happened.

### Pass 2 — the right cause, with a verification design that could not work

**Theory, correct:** `ArticleCard.kt` keyed its pointer handler on `gestureState`, an object
`remember(article.id, …)` rebuilds for every head article. `Modifier.pointerInput(key)` restarts its
handler whenever the key changes, so every deck change tore the card's touch handling down, and the
relaunch was starved by the article-change scroll on the same frame clock. Measured attach latency
**381–777 ms**.

**Where it stopped:** the gate required an instrumented test that reproduced the *symptom* — a touch
dropped during the attach window. **Seven harness variations all passed against unchanged production
code** (`investigation/step0-reproduction.md`).

The assumption was backwards, and this is the lesson: **a harness built to remove timing windows cannot
observe one.** `ComposeTestRule` idles until composition and its coroutines have settled *before* it
injects, which is precisely the condition under which there is no window. The verification design was the
flaw, not the diagnosis.

### Pass 3 — right, fixed, gated green, and still broken

`29344aa` keyed the handler on `Unit` and read per-article values through current state; `1f19cea` guarded
it with an instrumented test asserting the *cause* — that a gesture spanning a head-article change still
commits — whose RED was reproduced independently in a throwaway worktree. Both stay.

**It works, on the path it was aimed at.** Two fast consecutive swipes 0.35 s apart went from one record
to two, with two source weights moving (`investigation/post-fix-walkthrough.md`).

**And the walkthrough then found the headline symptom still live on the Undo path.** A swipe 0.3–0.5 s
after Undo was still discarded, with the card settled, visible and correctly positioned. A 900 ms drag
begun 0.4 s after Undo, photographed mid-flight, showed the card **not moved at all**.

What was wrong was the **scope of the claim**, not the diagnosis. `pointerInput(Unit)` stops the handler
being *re-keyed*; it says nothing about a DOWN that never reaches the handler. And §5.3's guard could not
have noticed — it asserts handler persistence across a re-key, which is exactly what had been fixed.

**This is the expensive lesson, and it is the new one: an instrumented test that asserts a cause is only
as good as the cause.** A green gate defending a true statement about the wrong half of the problem reads
exactly like a green gate defending a fix.

### Pass 4 — diagnosis before code, and it held

The item was re-designed so that **slice 1 wrote no product code at all** (`slices.md`, third revision).
`design.md` D8 listed four candidate mechanisms with a discriminator each; `spec.md` §5.1 made naming one
— *and explaining why 0.2 s passed* — a gate that had to close before a third fix could be written.

It closed (`investigation/step0-undo-window.md`). The mechanism is **M1**, confirmed by a temporary sibling
probe using `awaitFirstDown(requireUnconsumed = false)` that sees every DOWN whether or not it was claimed:

```
FAIL  27.238 animateScrollTo BEGIN from=445 to=1084
      27.508 PROBE down consumed=true          ← no awaitFirstDown, no record
PASS  37.440 animateScrollTo END at=892
      37.801 awaitFirstDown RETURNED           ← consumed=false, commits
```

Same pixel `(753, 1235)`, same node, one variable. Under reduced motion — where `DiscoverScreen.kt:81-85`
substitutes an instant `scrollTo` — the window closes completely.

M2 (node recreated) falsified: `pointerInput START` logs once per process and node identity holds across
the restore. M3 (overlay) falsified: the DOWN commits unconsumed at 0.8 s with the `UNDO_COMPLETED`
overlay mounted, 900 px from the toast's bounds. M4 (gesture state refuses) falsified:
`gestureState.down` returns `true` whenever reached, and is never reached when the DOWN is consumed.

**And the mystery dissolved rather than being solved.** `spec.md` §1.5 treated the band as open on both
sides and asked why 0.2 s passed. It never passed: at the bottom of the range the DOWN is adopted against
the **outgoing** article — `awaitFirstDown RETURNED article=8c80f6f9…` fires 22 ms *before* the restored
card composes, and the state document records the wrong source. The previous walkthrough scored it by
"did a weight move", a weight did move, and it counted as a success. Scored by *which* article moved, the
low end is a second defect. The shape is a plain window that opens when the scroll starts and closes when
it ends.

---

## Definition of done

- [x] **Mechanism named and gated** — `spec.md` §5.1, all four conditions, before any third fix.
      `investigation/step0-undo-window.md`.
- [x] **RED before GREEN** — `49c1ecc` (test) precedes `11d8353` (fix). RED reproduced independently in a
      throwaway worktree at `49c1ecc` with the fix absent:
      `expected:<[Article(id=visible, …)]> but was:<[]>`, `BUILD FAILED`.
- [x] **One production file, one line** — `ArticleCard.kt:125`.
- [x] **`spec.md` §4 scenarios** — three new ones satisfied, five prior ones regression-checked.
- [x] **§5.4 walkthrough, all four steps** — `investigation/slice2-walkthrough.md`, driven by the
      orchestrator, not the author.
- [x] **`SwipeGesture.kt`, `SwipeGestureTest.kt` (incl. `:164`) and `ArticleCardGestureTest.kt`
      byte-identical** across slice 2.
- [x] **No input lockout, refusal window, or delay on a reader's touch** (`design.md` D9); **no visible
      non-interactive card state** (D11); **no `docs/v1/**` amendment**.
- [x] **No new dependency**, no test-only parameter on `ArticleCard`.
- [x] **Gates on the head** — `:app:testDebugUnitTest` **258 tests, 0 failures**; `:app:assembleDebug`
      `BUILD SUCCESSFUL`; `:app:connectedDebugAndroidTest` **4 tests**, `BUILD SUCCESSFUL`.

## Walkthrough result — `spec.md` §5.4

Scored by **which article moved**, per the correction slice 1 owed to §5.4. Deck head after `pm clear` is
`322d8f25…` (`science_aaas`).

| delay after Undo | before | after |
| --- | --- | --- |
| 0.05 / 0.1 / 0.15 s | — | commits, `322d8f25` |
| 0.2 / 0.3 / 0.4 s | **discarded** | **commits, `322d8f25`** |
| 0.5 / 0.6 / 0.7 / 0.8 / 1.2 s | commits | commits, `322d8f25` |

Each of the five §5.4 delays also raised its own undo offer. Step 1 (two fast swipes, both commit) and
step 4 (settled control) hold; the save direction still works (`322d8f25:saved`, weight `+0.45`). Step 3's
photograph — `investigation/walkthrough-s2-mid-drag-undo-0.4s.png` — shows the card translated, rotated
and showing its cue at the delay where `walkthrough-mid-drag-undo-0.4s.png` showed it unmoved.

**The regression the fix actually risked was checked explicitly.** `requireUnconsumed = false` is what
lets the card contend for a claimed DOWN, so a vertical drag on the card must still belong to the scroll:
it scrolls the page (card label `y = 1530` → `y = 638`) and commits nothing. Slice review found this
unguarded and `5a86003` added the guard — **verified non-vacuous by mutation**: forcing
`Intent.HORIZONTAL` in `SwipeGesture.move()` makes it fail with *"Expected the ancestor scroll to move."*

## The artificial-lag evaluation — part of the record, not a discarded branch

The owner proposed (2026-08-27) a deliberate lag so a second swipe cannot land close enough in time to be
lost. All three forms are recorded in `design.md` D9 rather than only the survivor, because **the instinct
was sound and the measurement is what disqualified it.**

L1, lag after a successful commit, does not intersect the defect — it guards the deck-advance path, which
already accepted a swipe 0.35 s later. L2, lag after the Undo restore, would have to span the whole band
and would refuse swipes that land today, and a silent refusal is the defect wearing a policy hat: the
reader still swipes and nothing still happens. Making it honest needs a visible non-interactive card
state, which `docs/v1/06-ui-ux.md` does not specify and `AGENTS.md` forbids inventing.

L3 — lag the *state change*, not the input — survived as a pre-registered fallback under one specific
diagnosis outcome, bounded by an observable settle signal rather than a millisecond literal. **It was not
needed.** The diagnosis named M1 and M1's fix refuses nothing.

Worth stating plainly: the input arrives too *late* to be heard in one window and early enough in another,
which is not a problem lag can solve.

## Commits

| SHA | Subject |
| --- | --- |
| `fa3f352` | `docs(spec): design item 013 undo gesture reset` |
| `85a94e4` | `test(android): cover resolved swipe commit lock` |
| `bf79c42` | `fix(android): release resolved swipe commit lock` |
| `05ab609` | `docs(spec): rewrite item 013 against instrumented evidence` |
| `f1f381b` | `docs(spec): record the 005 evidence correction owed by 013` |
| `26dbc28` | `docs(spec): record the 013 Step 0 reproduction and the stop at the instrumented gate` |
| `60a32ec` | `docs(spec): re-design item 013 against the failed instrumented gate` |
| `1f19cea` | `test(android): cover gesture across head article change` |
| `29344aa` | `fix(android): keep card gesture handler attached` |
| `f55ec74` | `docs(spec): record the post-fix walkthrough failure on the Undo path` |
| `2bfd768` | `docs(spec): re-design item 013 around an unknown Undo-path cause` |
| `b28b674` | `docs(spec): name the Undo window mechanism and close the 013 diagnosis gate` |
| `49c1ecc` | `test(android): cover swipe during ancestor scroll` |
| `11d8353` | `fix(android): accept card down during ancestor scroll` |
| `a40febf` | `docs(spec): record the slice 2 walkthrough` |
| `5a86003` | `test(android): guard vertical scroll ownership on card drag` |
| `cb863fd` | `docs(spec): close item 013 slice 2` |

## Three lessons, stated outright

1. **A green unit gate never had a chance here.** 258 passing JVM tests coexisted with a live defect
   through all four passes, because this is Compose wiring with no pure object whose contract changes.
   Nothing about the arithmetic was ever wrong.
2. **A harness that synchronizes away timing cannot test timing.** Seven `ComposeTestRule` variations
   failed to reproduce a real window. What finally worked was not a better race — it was a *controllable
   programmatic animation* on a paused clock, which is a different thing entirely.
3. **An instrumented test that asserts a cause is only as good as the cause.** `1f19cea` is honest, its
   RED was real, and it defended a true statement about the wrong half of the problem. This is the
   expensive lesson of pass 3, and it is why pass 4 put diagnosis behind a gate with no product code in it.

A fourth, cheaper one, earned twice: **a check that is one notch too weak is indistinguishable from a
pass.** A card leaving the deck is not proof the action landed; and — found in pass 4 — *a record
appearing is not proof either.* The record must name the article the reader was looking at.

## Outstanding

- **The delay-0 misattribution — `backlog.md` item 015.** At a nominal delay of 0 the DOWN is still
  adopted against the outgoing article; three runs gave `ietf_oauth`, `ietf_oauth`, `science_aaas`. This
  is a publish-ordering defect, not a consumption one, it is outside `spec.md` §4, and slice 2's brief
  deliberately barred the implementer from absorbing it. It is real, it is recorded, and it needs its own
  item.
- **The instrumented suite is three classes and four tests, and still out of CI** (002 slice 4). The local
  `connectedDebugAndroidTest` run is the only thing that exercises any of them.
- **The owner's judgement on whether the swipe motion now feels right** (`06-ui-ux.md` §44). Carried from
  wave B's verification debt and still open — the earlier "smooth and nice" predates every landing fix.
- **Item 014** — the Discover card's buttons fail in the same window and never consult the gesture state,
  so nothing here addresses them. Its own reproduction, inheriting no cause (`spec.md` §1.8).
