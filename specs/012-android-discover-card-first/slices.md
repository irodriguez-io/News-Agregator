# 012 — slice plan

**Size: S — two slices.** They are split so that the slice with a real failing test runs first and the
reorder lands on top of a scroll target that is already correct. One branch
(`feat/012-android-discover-card-first`), one PR targeting `main`.

`«pkg»` = `android/app/src/main/kotlin/io/irodriguez/intentionalreading/`; JVM tests under
`android/app/src/test/kotlin/io/irodriguez/intentionalreading/`.

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew :app:assembleDebug
```

## Fixed for this item — do not re-decide these mid-implementation

- **No behaviour change.** Same widgets, same strings, same state, new positions (`spec.md` §3).
- **No copy authored, no string resource added or reworded.** Every `stringResource` call moves unchanged.
- **`«pkg»/ui/components/ArticleCard.kt` is not touched**, and neither is `ui/gesture/**`.
- **`EditorialHeader`'s general three-argument overload keeps its exact behaviour** — Read Later and History
  depend on it (`spec.md` §3).
- **`«pkg»/ui/AppViewModel.kt` is not touched.** It is item 015's ground for the whole of wave D and 015 is
  in flight concurrently.
- **Neither of the other two scroll effects is deleted** (`design.md` D2).
- **`docs/v1/**` is not edited.** Amendment 7 is already committed when the branch is cut.
- **No new dependency**, no new token, type style, radius or motion spec — wave E owns all of that.

---

## Slice 1: aim the opened-article scroll at the card's action rail

- **Scenarios:** *the Mark read button is on screen after returning from the publisher* (`spec.md` §4).
- **Files:**
  - **new** `«pkg»/ui/screens/discover/DiscoverScrollTargets.kt` — the pure function in `design.md` D3.
  - **new** `…/test/…/ui/screens/discover/DiscoverScrollTargetsTest.kt`.
  - `«pkg»/ui/screens/discover/DiscoverScreen.kt` — capture the card's bottom offset alongside the existing
    `cardTopOffset` (`:146-148`), source the viewport height, and replace `scrollState.maxValue` at `:98`
    and `:100` with the function's result.
- **Failing-first commit:** `DiscoverScrollTargetsTest`, RED because the function does not exist yet. State
  that in the commit message — `design.md` D3 explains why a compile-level RED is the honest shape here and
  what the behavioural proof is instead.
- **Definition of done:**
  - A card that already fits the viewport yields `0`; a card taller than the viewport yields exactly the
    offset that places its bottom edge at the bottom of the viewport; a target beyond the content clamps to
    `maxValue`; a card whose bottom is above the current viewport does not produce a negative scroll.
  - `DiscoverScreen` no longer passes `scrollState.maxValue` as the became-opened target.
  - Both gates green, `test-results` deleted first, count recorded at the moment of the run.
  - **Report which viewport source was used** — `scrollState.viewportSize` or `BoxWithConstraints`.
- **Status:** pending

## Slice 2: put the card first and the operational block below it

- **Scenarios:** every remaining scenario in `spec.md` §4.
- **Files:**
  - **new** `«pkg»/ui/screens/discover/DiscoverHeader.kt` — `DiscoverMasthead` and
    `DiscoverOperationalBar`, per `design.md` D1. Same strings, same styles, same relative order inside the
    operational bar as the current header has.
  - `«pkg»/ui/components/EditorialHeader.kt` — remove the Discover-specific overload (`:22-79`). Leave the
    general overload untouched.
  - `«pkg»/ui/screens/discover/DiscoverScreen.kt` — the column becomes masthead → state body →
    operational bar. The three `LaunchedEffect`s keep their keys and their bodies except for slice 1's
    change.
- **Definition of done:**
  - The five non-scroll scenarios in `spec.md` §4 hold on the emulator, evidenced by `screencap` at 360 dp
    and 411 dp per `spec.md` §5.3.
  - `EditorialHeader.kt` exports only the general overload, and Read Later and History render exactly as
    before — checked by `screencap` on both destinations.
  - Both gates green. **No existing test edited**; if one moves, report the case name and why before
    changing it (`spec.md` §5.5).
- **Not assertable in this slice, and enforced by the walkthrough instead:** composition order and
  first-viewport fit. `spec.md` §5.2 says why, and this slice therefore carries **no** failing-first unit
  test. That is deliberate and it is the exception, not a pattern — slice 1 carries the item's RED.
- **Status:** pending

---

## Assumptions, each checkable at dispatch

Per `waves/wave-d.md`'s rule 2.

1. **Amendment 7 is committed on `main`** before this branch is cut, with `06-ui-ux.md` §21 already
   reworded. If it is not, stop — the implementer may not edit `docs/v1/**`.
2. **Item 015 has not touched `DiscoverScreen.kt`, `EditorialHeader.kt` or `ui/screens/discover/**`.** Its
   design confines it to `AppViewModel.kt` and four lambda arguments in `IntentionalReadingApp.kt`
   (`specs/015-android-undo-swipe-attribution/design.md` D6). **If 015's landed diff touches any file in
   this item's list, the concurrency claim in `waves/wave-d.md` is void and this item must be re-based and
   re-reconciled before dispatch.**
3. **`DiscoverScreen.kt`'s three `LaunchedEffect`s are still at `:71-73`, `:74-87` and `:91-103`** in the
   shape `design.md` D2 tabulates.
4. **`scrollState.viewportSize` exists in Compose BOM `2026.08.00`.** If not, `design.md` D3 names the
   fallback.

## On existing assertions

**No existing assertion is frozen.** `UiStateMapperTest` asserts what the header's strings *say*, not where
they sit, so nothing is expected to move — but if a case does, the implementer says which and why rather
than working around it. Writing "no existing assertion may be edited" is the sentence that made item 006
unimplementable (`waves/wave-c-note.md` §2) and it is not written here.
