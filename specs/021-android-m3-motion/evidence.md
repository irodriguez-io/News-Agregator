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

PR **#35**, targeting `main`. Head **`e51ae186`**.

| Check | Result | Job |
|---|---|---|
| `build` | pass, 1m37s | [100335552499](https://github.com/irodriguez-io/News-Agregator/actions/runs/33656289696/job/100335552499) |
| `test` | pass, 21s | [100335552257](https://github.com/irodriguez-io/News-Agregator/actions/runs/33656289670/job/100335552257) |
| `instrumented` | pass, 5m21s | [100335552869](https://github.com/irodriguez-io/News-Agregator/actions/runs/33656289696/job/100335552869) |

The `instrumented` log prints `Starting 17 tests` / `Finished 17 tests` / `0 skipped` / `0 failed`.

### The instrumented gate found a real defect on this item

**This is the second time the new CI gate has earned itself, and the second time in the same way.**

On head `4f7b241`, `instrumented` failed: 15 of 17 passed, and both of this item's new sheet tests failed
identically with `Failed: assertExists. Reason: could not find any node that satisfies: (Text contains
'Settings')`. Not a wrong value — **the sheet never appeared**.

Both tests disable `mainClock.autoAdvance` and then click to open the sheet, but `ModalBottomSheet`'s
appearance is driven by *platform* animation, not solely Compose's test clock. CI's software-rendered
emulator on a shared runner never reached the open state. `dismissUsesAReverseTuckBeforeRemovingTheModal`
does not touch `animator_duration_scale` and failed identically, which ruled out the reduced-motion
manipulation as the cause.

**Same defect class as item 018's width test: asserting against an environment the test did not establish.**
There it was a 360 dp width the test hoped for; here it is fast hardware the test hoped for. Both passed 17/17
locally, and both times local passing was worth nothing — it is precisely what missed the defect.

Fixed at **`e51ae186`**, 13 insertions / 0 deletions in one file. `autoAdvance` is enabled *only* to reach the
open state, a `waitUntil` waits for exactly one `Settings` node, then `autoAdvance` goes back off before any
measurement. Every timing assertion still runs under the manual clock, and no assertion was weakened — the
0.5f translation tolerance, the exact scrim fingerprint, and both reverse-tuck presence assertions are intact.
Only the two methods CI failed were touched; the entrance-motion test that passed is byte-identical.

The implementer could not reproduce CI's original failure against the unchanged commit — isolated SwiftShader
passed, 10x animation scales passed. Only both together reproduced a failure, and against its own first
wait-only attempt rather than the original code. **Recorded as a gap, not glossed:** the causal story is
consistent but unproven, and CI on the exact head is what closed it. Five consecutive green runs of both
methods under SwiftShader plus 10x scales were captured, and all three scales restored to 1 afterwards
(verified independently).

## 11. Open at pause — 2026-09-02

**021 is implemented, green in CI, and NOT merged. The final review requested changes.** Nothing below blocks
on the owner except the merge decision and the walkthrough.

### The one open finding: the internal-API suppression at `660adc7`

`660adc7` added a **file-level** `@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")` at
`android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/settings/SettingsSheet.kt:1`. The
Kotlin compiler's own warning on it: *"might compile and work, but the compiler behavior is UNSPECIFIED and
WILL NOT BE PRESERVED."*

Verified by disassembling the pinned artifact `material3-android 1.4.0` with `javap`, not by reading docs:

1. **`SettingsSheet.kt:119-120` are dead writes.** `SheetState.setShowMotionSpec$material3` and
   `setHideMotionSpec$material3` are `internal`, and **`ModalBottomSheetKt` assigns both itself** — in
   `ModalBottomSheet_YbuCTN8$lambda$1$lambda$0`, a post-composition side effect that also sets
   `anchoredDraggableMotionSpec`. The values come from `MotionSchemeKeyTokens.DefaultSpatial` resolved via
   `MotionSchemeKt.value(...)`, i.e. from `MaterialTheme.motionScheme` — the very scheme this file already
   overrides **publicly** through `SettingsSheetMotionScheme` at :152. The app writes during composition; the
   library overwrites afterwards. So the 350 ms reveal is produced entirely by the public override, and these
   two lines are discarded before any animation runs. **Delete both; every motion assertion should stay
   green, and that check is what proves the claim.**
2. **`SettingsSheet.kt:404` — `reducedMotionLayoutOffset()`** reads `anchoredDraggableState.anchors` and
   `offset`, both `internal`, with `AnchoredDraggableState` living in the `androidx.compose.material3.internal`
   package. `offset` has a public sibling, `requireOffset()`; `anchors.positionOf(SheetValue.Expanded)` has
   none. Ask the smaller question first: under reduced motion the sheet is built with `initialValue =
   SheetValue.Expanded` and every spec is `snap()`, so it should already sit at the expanded anchor and this
   correction should compute 0. **Try deleting the function and its `Modifier.offset` branch** — if
   `reducedMotionIsImmediateWhileTheDimmingScrimRemains` still holds at both test sizes, the reach was never
   load-bearing. If it is needed, keep it but move the `@Suppress` from `@file:` onto that one function and
   use `requireOffset()` for the offset half. A file-level blanket licenses internal-API access across all 440
   lines and every future edit to this file.
3. **Not a finding, but load-bearing:** `SheetState(...)` at :105 **is public** in 1.4.0 — `javap` shows it
   unmangled. Removing the suppression does **not** force a constructor rewrite. Do not assume otherwise.

The realistic failure is a `compose-bom` bump silently changing sheet motion, with the 350 ms assertion as the
only thing standing between that and a shipped regression. This is a coupling-surface finding, not a live bug:
behaviour is correct and asserted today, which is exactly why no gate caught it.

### Resume here

1. Dispatch **one bounded Codex brief** for finding 1 and 2 above (fresh session, item branch/worktree). It is
   comfortably one context window: one production file, no new tests, no assertion changes permitted.
2. Re-verify: gates locally with `--rerun-tasks`, then **push and re-run CI** — `instrumented` is the arbiter
   for anything in this file, per the defect above.
3. Post the final review on the new head. GitHub blocks approving a PR opened by the same account, so the
   approval statement goes in as a review **comment** — that comment is the gate artifact.
4. **Owner walkthrough**, which carries `waves/wave-e.md` **checkpoint 4 and checkpoint 5 (the wave sign-off)**
   — on a device, both colour schemes, `screencap` not `uiautomator dump`.
5. Present the merge decision to the owner. As with 017-020, the merge is theirs to authorise.

### Two judgments still deferred to the owner

- **Does the motion read as *expressive* or *busy*?** §47's boundary is taste, and no test can settle it. This
  is the walkthrough's real question, not whether the 350 ms is 350 ms.
- **Item 019's triage labels** — the owner deferred this to wave close, which is now.

### Wave-close work that outlives this item

- `waves/wave-e-note.md`, and the batched walkthrough record.
- `specs/backlog.md` — close wave E.
- **Retire the 13 legacy token names** item 017 kept alive for the wave's duration. They exist only because 17
  files held 205 call sites at the time; that debt was scoped to the wave and comes due at its close.

### Environment notes

- Worktree `/Users/isidro.rodriguez/Documents/Repos/news-agregator-021`, branch
  `feat/021-android-m3-motion`, clean, pushed. **The repo moved this session** from `~/Documents/VS Code/` to
  `~/Documents/Repos/`.
- `gh` must be authenticated as the account with write access, **`irodriguez-io`**; any other account produces a 403 on push.
- Android gates need `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` and
  `ANDROID_HOME="$HOME/Library/Android/sdk"`.
- No Codex sessions left open — swept at pause.
