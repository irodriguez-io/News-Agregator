# 019 — Material 3 Expressive Discover · evidence

**Branch:** `feat/019-android-m3-discover`, cut from `main` at `9c0dd7f`\
**Slices:** 3, all done · **Tests:** 343 → **362** unit, plus 8 instrumented, 0 failures throughout\
**Implementer:** Codex (`gpt-5.6-sol high`), three fresh sessions\
**Reviewer:** the orchestrating Claude session — spec and plan author, not code author
(`execution-model.md` §5)\
**Ran concurrently with item 020**, in a separate worktree, sharing no file.

---

## 1. Forecast reconciliation — Step 0.4, and it changed the item

Items 017 and 018 merged after this item was designed, and so did PR #32's CI change. Four of five
assumptions held. **The third did not, and correcting it altered what slice 3 does.**

`slices.md` assumed item 012's composition-order tests existed and that slice 3's job was to keep them green.
**They do not exist.** Only `DiscoverScrollTargetsTest` does, covering scroll arithmetic. Item 012 says why
in its `spec.md` §5.2:

> *"Composition order is not observable in `testDebugUnitTest`. There is no composition, and the instrumented
> source set is out of CI."*

**Both reasons held at design time. The second stopped holding hours earlier**, when PR #32 put instrumented
tests into CI on a pinned 411 dp emulator.

So `design.md` **D3 rested on a false premise.** It warns that the failure mode is *"a re-layout that quietly
reverts the ordering while nobody is asserting it"* and names 012's tests as the guard. **There was no
guard**, and had not been since Amendment 7 was written.

Slice 3 was amended at `8d71c53` to **build** the guard rather than inherit it.

## 2. Gate runs

Reproduced by the reviewer with `--rerun-tasks` in a throwaway worktree, per §5.1 control 1.

| Slice | RED | GREEN | Reviewer-reproduced |
|---|---|---|---|
| 1 | `6649a19` | `7d245ff` | 355 unit, 0 failures |
| 2 | `9d2797d` | `dd0b680` | 359 unit, 0 failures |
| 3 | `657ecdc` + `c276bdb` | `38b3272` | **362 unit, 0 failures; 8 instrumented, 0 failed** |

`assembleDebug` and `assembleDebugAndroidTest` green on every slice. Baseline 343. Net **+19 unit tests plus
2 instrumented**, none deleted or suppressed.

## 3. Failing-first evidence

All three slices produced **value-failing REDs**, reproduced independently:

- **Slice 1** — `355 tests completed, 8 failed`, including `excerpt maxLines expected:<2> but was:<4>` and
  `headline maxLines expected:<3>`.
- **Slice 2** — `359 tests completed, 5 failed`, including `circular triage control count expected:<2>`.
- **Slice 3** — `362 tests completed, 2 failed`.

**Where a RED could not fail, that was stated rather than contrived.** Guard 1 asserts an ordering that
already held — Amendment 7 shipped in item 012 — so it is a *regression guard* with nothing to fail against.
The brief asked for that to be reported honestly instead of manufacturing a failure, and it was.

## 4. Item 012 §1.4 is closed, and it is gated

**Guard 1 — Amendment 7's ordering, asserted for the first time.** `masthead.top < card.top` and
`card.top < operationalBlock.top`, by `boundsInRoot`.

**Guard 2 — the fold.** `longDatasetCardFitsAboveTheFoldAt360Dp` forces 360 dp, locates all three rail
controls by content description, and asserts their bounds within the viewport. Verified passing at forced
360 dp, at 411 dp, **and with the emulator physically narrowed to 320 dp.**

Both establish their own width via `DeviceConfigurationOverride.ForcedSize` with every `dp.toPx()` baseline
**inside** the override, per `execution-model.md` §8.3 — the bug item 018 shipped and took two rounds to find.

`walkthrough/item019-fold-closed-360dp.png` is the visual proof: headline clamped at three lines with
ellipsis, excerpt at two, **all three action controls above the bottom bar** — where item 017's walkthrough
had a five-line headline and `Read article` cut in half.

**This is what item 012 said it could not deliver and handed to wave E** (`012/spec.md` §1.4).

## 5. A gap in this item's own slice plan, found at review

Four `RoundedCornerShape(10.dp)` literals remained in `ArticleCard.kt` after slice 1 — **10 dp is §15.1's
*browser* control radius**, nothing Android's scale defines. Two were in `ArticleActions` (slice 2's ground);
**two were in `SwipeCue` and `OpenedAcknowledgment`, which no slice named at all.** As planned, this item
would have shipped browser radii on two Android surfaces.

Slice 2 was extended to take both, with their text, timing and offered choices untouched —
`OpenedAcknowledgment` renders §51's return state, where item 012 fixed `Mark read` being off-screen.

`ArticleCard.kt` now carries **no radius, colour, size or font literal anywhere in the file**, and slice 1's
scan — honestly scoped to its four owned composables — was widened to the whole file to keep it that way.

## 6. Existing assertions changed

**None.** `DiscoverScrollTargetsTest`, items 008 and 013's gesture tests, and every 017/018 test stayed green
and **unedited**. `SharedControls.kt` and `CategoryChipRow.kt` — item 018's — were not modified. No
`@Ignore`, `@Disabled` or `assumeTrue`. **No unlisted test failed**, so §2.1 rule 5's protocol was never
invoked.

## 7. Raised for the owner rather than decided at review

**The triage controls lost their visible text labels.** *"Not interested"* and *"Save for later"* now travel
only as `accessibleName` content descriptions, so a **sighted** reader sees bare `←` and `→`.

**It is authorised** — §76.5 says *"icon-only controls carry accessible names (§73)"*, and those names are
present and asserted. But §35's *"must not replace the labelled semantic understanding of the action"* reads
against it.

It is also **plausibly load-bearing for the fold fix**, since the labels cost vertical space at exactly the
width that was tight. *Whether the rail would still fit at 360 dp with them restored is unmeasured.*

**A visual judgment for the walkthrough.**

## 8. Definition of done

| Item | Status |
|---|---|
| Headline clamps at 3, excerpt at 2, both ellipsised (§13.2) | ✓ asserted on the text elements |
| Empty excerpt omitted, no placeholder (§26.2) | ✓ |
| Badge label unaltered (§28.2); reading time and age omitted when unknown (§29, §30) | ✓ |
| Tags capped at 5, neutral, non-interactive (§27.2) | ✓ |
| 24 dp radius, `card` fill, tertiary-tinted ambient shadow (§15.2, §16.2, §76.2) | ✓ |
| Action rail uses item 018's shared controls (§76.5) | ✓ all three |
| Triage targets ≥ 48 dp with accessible names (§72.2, §35.2) | ✓ |
| **Amendment 7's ordering preserved — and asserted** | ✓ **first automated guard** |
| **The whole card fits above the fold at 360 dp** | ✓ **gated, and screenshotted** |
| Empty, loading and failed-dataset states truthful with a route onward (§67–§69) | ✓ |
| Swipe behaviour unchanged (§39–§43) | ✓ tests green and unedited |
| No image area, nothing reserved (§74.1, §74.2) | ✓ |
| No colour, radius, size or font literal | ✓ whole-file scan |
| No new dependency, no new string | ✓ |

## 9. Walkthrough

**Partially driven.** The 360 dp fold screenshot is committed under `walkthrough/`. The owner look this
item's merge requires (`wave-e.md` checkpoint 4) is outstanding, and §7's label question is the judgment it
must settle.

`spec.md` §5.4's remaining steps — both schemes, swipe outcomes, return-state `Mark read`, category change
returning to the top, and the non-card states — are for that pass.

## 10. Hosted CI

Recorded at PR time. Note `android.yml` now runs **four** tasks as of PR #32: `testDebugUnitTest`,
`assembleDebug`, `assembleDebugAndroidTest`, and `connectedDebugAndroidTest` on an emulator.
