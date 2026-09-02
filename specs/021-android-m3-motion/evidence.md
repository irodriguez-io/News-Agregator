# 021 — Material 3 Expressive motion · evidence

**Branch:** `feat/021-android-m3-motion`, cut from `main` at `e251886`\
**Slices:** 2, both done · **Tests:** 381 → **385** unit, plus 12 → **17** instrumented, 0 failures throughout\
**Implementer:** Codex (`gpt-5.6-sol high`), two fresh sessions\
**Reviewer:** the orchestrating Claude session — spec and plan author, not code author
(`execution-model.md` §5)\
**The last item of wave E.**

---

## 1. Forecast reconciliation — Step 0.4

All five assumptions were verified against `main` at `e251886` and **all five held**: the bare
`when (destination)` at line 261; `reducedMotion` resolving at line 92 in the same composable;
`SettingsSheet` still a `ModalBottomSheet`; the destination order unchanged; and `UndoToast` (341),
`LiveStatusMessage` (347) and `SettingsSheet` (354) all hosted **after and outside** the destination branch —
which is what makes Amendment 8's cross-destination offer work. Item 018 had edited only the app bar,
shifting lines by four without restructuring.

**Two things changed since this item was designed**, both recorded at `b173b72`:

**Its most important claims became gateable.** `spec.md` §5.2 listed reduced-motion suppression, sheet focus
handling, and undo-surviving-a-destination-change as *"assertable in a Compose UI test"* — which, at design
time, meant **written and never run**, because the instrumented source set was out of CI. PR #32 changed
that, so those guarantees moved from walkthrough notes into gated tests.

**Its "likeliest casualty" stopped being abstract.** §5.3 warned that *"any test asserting screen content by
composition structure"* could break when `AnimatedContent` adds a layer. By dispatch there were **12
instrumented tests across 7 files, five locating nodes by `boundsInRoot`**, and the reconciliation named the
most exposed individually: `MainActivityLaunchSmokeTest`, item 020's `ReadingListLayoutTest`, and item 019's
`DiscoverScreenLayoutTest`. **Neither 019's nor 020's guard existed when this item was designed.**

## 2. Gate runs

Reproduced by the reviewer with `--rerun-tasks` in a throwaway worktree, per §5.1 control 1. **The
instrumented suite was run by the reviewer directly**, not accepted on report.

| Slice | RED | GREEN | Reviewer-reproduced |
|---|---|---|---|
| 1 | `1d160a3` | `48c4483` | 382 unit, 0 failures; **14 instrumented, 0 failed** |
| 2 | `3fefd05` | `660adc7` | 385 unit, 0 failures; **17 instrumented, 0 failed** |

`assembleDebug` and `assembleDebugAndroidTest` green on both. Baseline 381 unit / 12 instrumented. Net **+4
unit, +5 instrumented**, none deleted or suppressed.

## 3. Failing-first evidence

**Slice 1** — `READ_LATER -> DISCOVER expected:<FROM_RIGHT> but was:<FROM_LEFT>`. The direction function was
written in RED returning a deliberately wrong direction, and is unit-tested over **all six ordered pairs**.

**Slice 2** — unit: `expected:<shapes.modalSheet…>`, `expected:<tokens.card>`. **Instrumented, and stronger:**
*"reduced motion moved the title from top=2545 to top=442"* and *"dismissal removed the sheet before reverse
tuck"* — real geometry and sequencing failures.

**Where a RED could not fail, that was reported rather than contrived.** Slice 1's reduced-motion and
undo-offer assertions *passed against the original bare `when`*, because they guard behaviour that already
held. Slice 2's focus-trapping assertions likewise. The implementer said so both times.

**No vacuous duration assertion exists.** D1 forbade asserting that a duration constant equals 300 — item
016's canonical failure — and the direction function carries the only real logic.

## 4. The predicted casualty happened, and the protocol worked

`MainActivityLaunchSmokeTest` **failed** on slice 1's first full post-change run: `AnimatedContent` evaluated
its transition during initial equal-state composition.

**The implementer reported it before editing anything**, then fixed **production** — a no-op transform when
the destination is unchanged — and left the test untouched. That is §2.1 rule 5 executed exactly as written,
on the one occasion in this wave it was needed.

## 5. The "test looks wrong" branch, reached for the first time

In slice 2, with a working implementation uncommitted and one assertion between it and a green gate, the
implementer **refused to edit the test and asked for authority.**

Its claim was verified before anything was granted. The RED showed a **~2100 px** slide (2545 → 442); the
implementation reduced it to **0.1–0.2 px**. The animation was genuinely suppressed; the residue was layout
rounding. **Exact `Rect` float equality was the wrong instrument for "did it move"** — the same over-precision
that caused item 018's `ForcedSize` bug.

Authority was granted **narrowly**: that one translation assertion, tolerance ≤ 1 px, failure message
preserved, and the fade assertion explicitly **not** to be touched. Delivered at **0.5f** — tighter than the
ceiling — with the fade check still exact.

*Narrowing recorded:* the assertion now compares `.top` rather than the whole `Rect`, because JUnit has no
delta overload for `Rect` and vertical translation is the axis a bottom sheet moves on. It checks less than
the original did, while still catching the observed failure with four thousand times the headroom.

## 6. Existing assertions changed

**None.** Wave D's undo tests (items 012, 013, 014, 016), slice 1's transition tests, and **all five
bounds-locating instrumented tests** — `MainActivityLaunchSmokeTest`, `ReadingListLayoutTest`,
`DiscoverScreenLayoutTest`, `CategoryChipRowLayoutTest`, `ArticleRowLayoutTest` — stayed green and unedited.
No `@Ignore`, `@Disabled` or `assumeTrue`.

The only test change in this item is §5's authorised tolerance, on an assertion **this item wrote itself.**

## 7. Wave D's ground is intact

`IntentionalReadingApp.kt` is 509 lines and carries items 012–016's work. This item's diff adds animation
imports, wraps the `when (destination)`, and changes the sheet's hosting argument. **`UndoToast`,
`LiveStatusMessage`, the back handler and the recovery notice are all untouched**, and Amendment 8's
cross-destination undo offer is asserted to survive a destination change.

M3 Emphasized easing is implemented as a real **two-segment `PathEasing`**, not a guessed cubic-bezier.

## 8. Definition of done

| Item | Status |
|---|---|
| Direction indexed to §18's bar order, pure and unit-tested over all six pairs (§79.1, D1) | ✓ |
| 300 ms, M3 Emphasized easing, outgoing scale-down and 0.8 fade | ✓ |
| **Reduced motion: destination change immediate** (§48, §79.3) | ✓ **instrumented** |
| Sheet: 350 ms decelerated rise, reverse-tuck exit (§79.2) | ✓ **instrumented** |
| Sheet: 28 dp top corners, dimming scrim, not a destination (§76.7, §64.2) | ✓ |
| **Reduced motion: no slide or fade, AND the scrim still dims** (D2) | ✓ **both halves asserted** |
| Focus trapped while open, restored on close (§64) | ✓ |
| **A live undo offer survives a destination change** (§70, Amendment 8) | ✓ asserted |
| Back still returns to Discover | ✓ |
| No bounce, overshoot, pulse or reward motion (§47, §44) | ✓ |
| No navigation library introduced (D4) | ✓ |
| No new dependency, no new string, no colour/radius/size/font literal | ✓ |
| All pre-existing instrumented tests pass, unedited | ✓ 12 → 17, none edited |

## 9. Walkthrough

**Outstanding, and it carries the wave's own sign-off.** `wave-e.md` checkpoint 4 wants an owner look at this
merge, and **checkpoint 5 is the wave sign-off** — on a device, in both schemes — which lands with this item
because it is the last.

`spec.md` §5.4 states the steps. **Two things no test settles:**

- **Does the motion read as expressive or as busy?** §47's list is prohibitive but the boundary is taste, and
  this is the item where the wave's character is decided. `spec.md` §5.5 names it as the one judgment only
  the owner can make.
- **Item 019's triage labels.** The owner decided on 2026-09-02 to keep them icon-only and **revisit at wave
  close** — which is now.

Score the undo check **by article id**: the toast's tap target moves with its message width, and a missed
Undo looks exactly like a passing run. `uiautomator dump` cannot see the toast; use `screencap`.

## 10. Hosted CI

Recorded at PR time. Four tasks as of PR #32, including `connectedDebugAndroidTest` on a pinned 411 dp
emulator.
