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

## 11. The internal-API finding — raised, found wrong, resolved differently

**Status 2026-09-19: resolved. 021 is implemented, unmerged, and awaiting CI on the new head, the owner
walkthrough, and the merge decision.**

The final review of `660adc7` raised one finding: the **file-level**
`@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")` at
`android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/screens/settings/SettingsSheet.kt:1`, with
the Kotlin compiler's own warning against it — *"might compile and work, but the compiler behavior is
UNSPECIFIED and WILL NOT BE PRESERVED."*

**The objection was right. The analysis under it was wrong, and the remedy it prescribed was impossible.**
Both halves are recorded here, because the method that produced the wrong analysis will be reached for again.

### What the finding claimed, and what was actually true

The finding asserted **exactly two** reaches into `material3` internals, and that the 350 ms reveal was
produced *"entirely by the public override"* `SettingsSheetMotionScheme`. It instructed that the suppression
be removed or narrowed onto a single function.

Removing it does not compile. In the pinned `material3-android 1.4.0` the **entire expressive motion API is
`internal`** — seven reaches, not two:

| Reach | Site before the fix |
|---|---|
| `ExperimentalMaterial3ExpressiveApi` | import, and the `@OptIn` at `:76` |
| `MotionScheme` | import, and the `SettingsSheetMotionScheme` supertype |
| `MaterialTheme.motionScheme` | `:117` |
| `MaterialTheme(colorScheme, motionScheme, shapes, typography, content)` | `:148` — the five-argument overload |
| `BottomSheetDefaults.PositionalThreshold` | `:105` |
| `BottomSheetDefaults.VelocityThreshold` | `:108` |
| `SettingsSheetMotionScheme` itself | `:405-408` |

So `SettingsSheetMotionScheme` — the override the finding called **public**, and credited with producing the
reveal — is itself built entirely on internal API. The suppression could not be removed, and could not be
narrowed onto one function, because the reaches are scattered through the composable body.

**Why the analysis was wrong: `javap` cannot see Kotlin `internal` on types.** Kotlin `internal` *members* are
name-mangled (`setShowMotionSpec$material3`), which is how the finding correctly spotted the dead writes.
Kotlin `internal` *classes and interfaces* are emitted as **public JVM types**, with the real visibility
recorded only in `@kotlin.Metadata`. Disassembly is therefore systematically blind to exactly the kind of
internal the motion API uses. The finding's confident note that *"`SheetState(...)` is public — `javap` shows
it unmangled"* happens to be true, but was reached by a method that cannot establish it. **The Kotlin
compiler, not `javap`, is the authority on Kotlin visibility.** Ask it first.

`1.4.0` is the latest stable; the expressive API becomes public-experimental only in `1.5.0-alpha*`.
Upgrading a release-signed app to an alpha to launder a suppression was considered and rejected.

### What was actually done

The finding's *objection* survived its analysis intact, and it was the objection that mattered: **a
file-level blanket licensed internal-API access across all 426 lines of a file that is mostly settings UI,
appearance selection, import/export and reset confirmation** — none of which has any business touching
`material3` internals.

Two commits, each one bounded Codex session, no test edited at any point:

**`109529d` — the dead code the finding got right.**
The two writes at `:119-120` were confirmed dead and deleted: `ModalBottomSheetKt` assigns both itself
post-composition, so the app's composition-time writes are discarded before any animation runs.
`reducedMotionLayoutOffset()` and its `Modifier.offset` branch were **deleted outright** — the open question
the finding could not answer. Under reduced motion the sheet is built at `SheetValue.Expanded` with every
spec `snap()`, so the correction computed 0 and the reach was never load-bearing. Proven by the **unedited**
`reducedMotionIsImmediateWhileTheDimmingScrimRemains` passing at **both 360 and 411 dp** with the function
gone.

**`55fbaed` — the suppression confined instead of removed.**
The internal-API-touching motion plumbing moved to a new sibling, `SettingsSheetMotion.kt` (90 lines), which
now carries the `@file:Suppress` alone: `settingsSheetRevealSpec`, `SettingsSheetRevealDurationMillis`, the
`SettingsSheetMotionScheme` class, and two new helpers — `rememberSettingsSheetState(reducedMotion)` holding
the `SheetState` construction, and `SettingsSheetMotionTheme(reducedMotion) { }` holding the
`MaterialTheme.motionScheme` read and the five-argument `MaterialTheme(...)` wrap.

`SettingsSheet.kt` carries **no suppression and zero internal reach** — verified mechanically:
`grep -c "INVISIBLE_MEMBER" SettingsSheet.kt` returns **0**, and no reference to `MotionScheme`,
`motionScheme`, `ExpressiveApi`, `PositionalThreshold` or `VelocityThreshold` remains in it. Its blanket
`@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)` narrowed to
`@OptIn(ExperimentalMaterial3Api::class)`, with the expressive opt-in now scoped to the one function in the
motion file that needs it.

**The blanket went from 426 lines of mixed UI to 90 lines of motion plumbing that exists for nothing else.**
Behaviour is byte-for-byte unchanged: both `remember` keyings preserved exactly, the `MaterialTheme`
passthrough identical, no value, easing or timing touched.

### What the residual risk actually is

Unchanged by this work, and worth stating plainly: **a `compose-bom` bump can still silently change sheet
motion, with the 350 ms assertion the only thing between that and a shipped regression.** Confinement makes
the coupling visible and small; it does not remove it. The pin is `compose-bom 2026.08.00`. If a future bump
moves the expressive API to public-experimental, `SettingsSheetMotion.kt` is the single file to revisit, and
the `@file:Suppress` becomes an ordinary `@OptIn`.

### Verification of the fix

| Gate | Result | By whom |
|---|---|---|
| `SettingsSheet.kt` suppression count | **0** | reviewer, mechanically |
| Unit + assemble, `--rerun-tasks` | **385 tests, 0 failures, 0 errors, 0 skipped** (44 result files) | reviewer, own run |
| Instrumented | **17 tests, 0 failed** on Pixel_10 / API 37 | implementer — **CI is the arbiter** |

The local instrumented run was on `Pixel_10 / API 37 / arm64-v8a`, not CI's pinned `pixel_6 / API 34 /
x86_64`. Per §10's defect, `instrumented` on the exact head is what closes this, not the local run.

## 12. Resume here

1. **CI on the pushed head** — `test`, `build` and `instrumented` all green. `instrumented` is the arbiter for
   anything in `SettingsSheet.kt` or `SettingsSheetMotion.kt`.
2. Post the final review on that head. GitHub blocks approving a PR opened by the same account, so the
   approval statement goes in as a review **comment** — that comment is the gate artifact.
3. **Owner walkthrough**, carrying `waves/wave-e.md` **checkpoint 4 and checkpoint 5 (the wave sign-off)** —
   on a device, both colour schemes, `screencap` not `uiautomator dump`.
4. Present the merge decision to the owner. As with 017-020, the merge is theirs to authorise.

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
  `feat/021-android-m3-motion`, clean. **The repo moved** from `~/Documents/VS Code/` to `~/Documents/Repos/`.
- **The branch was merged forward from `main` at `7a70636`**, picking up the two release-signing PRs (#1, #2)
  that landed after this item was cut. Items now merge to `main`; `integration/v1` is stale.
- `gh` must be authenticated as the account with write access, **`irodriguez-io`**; any other account produces a 403 on push.
- Android gates need both of these exported, or Gradle fails before any test runs — once on a missing JDK,
  once on a missing SDK:
  `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` (or Android Studio's bundled
  `"/Applications/Android Studio.app/Contents/jbr/Contents/Home"`) and `ANDROID_HOME="$HOME/Library/Android/sdk"`.
- No Codex sessions left open — swept.
