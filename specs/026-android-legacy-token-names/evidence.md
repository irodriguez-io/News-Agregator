# 026 — evidence

**Commits:** RED `ef69cd3733ccdf5e897b8e5bc6d47968ec93dd3f`, GREEN `d288ceba3b74c7a454cba37c72aa69ae1bbe06e5`.
Implementer: Codex, one session. Slice gate PASS, 2026-10-01.

---

## 1. RED fails for the intended reason

I reproduced it in a throwaway worktree at `ef69cd3`, running `:app:testDebugUnitTest --rerun-tasks`. All 404
tests ran and **one failed**, the new test:

```
ThemeDerivationTest > the four duplicate names no longer exist FAILED
java.lang.AssertionError: Duplicate token fields still exist
expected:<[]> but was:<[accent, accentSoft, strongBorder, quietInk]>
```

## 2. Gates (`spec.md` §5.1)

I reproduced these in a throwaway worktree at `d288ceb`, running `:app:testDebugUnitTest :app:assembleDebug
:app:assembleDebugAndroidTest :app:connectedDebugAndroidTest --rerun-tasks` on `Pixel_6_API_34`:

| Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| JVM | **404** (403 + the absence test) | 0 | 0 | 0 |
| Instrumented | **28** (unchanged) | 0 | 0 | 0 |

I restored `animator_duration_scale` to `1` afterwards.

## 3. The names are gone (§5.2)

```
$ grep -rnwE "accent|accentSoft|quietInk|strongBorder" android/app/src --include='*.kt'
android/app/src/test/kotlin/io/irodriguez/intentionalreading/ui/theme/ThemeDerivationTest.kt:20:        val duplicateNames = setOf("accent", "accentSoft", "quietInk", "strongBorder")
```

The only match is the absence test's own string literal.

## 4. The diff changes identifiers only (§5.3)

`git diff ef69cd3 d288ceb` changes 52 lines in total, and I read every one:

- **The 15 consumer lines** each swap one identifier, following the mapping. No other token, literal or
  argument moves. In two places the conditional's other branch, `tokens.border`, stays as it was
  (`ResetConfirmation.kt:74`, `ImportConfirmation.kt:75`).
- **`Tokens.kt`:** 12 lines are deleted, which is the four fields in each of the data class, `blendTokens`
  and `tokensFrom`. Nothing is added.
- **`ThemeDerivationTest.kt`:** the four duplicate equalities are deleted. The nine assertions on the kept
  fields remain, including the backdrop hex check.
- **`AppearanceTransitionTest.kt`:** four `namedColors()` entries are deleted, and the comment changes from
  "all 26" to "all 22".

The changed files are the 8 production files and 2 test files the slice plan allows, and no others.

## 5. Walkthrough (§5.4)

I ran this on a debug build of `d288ceb` on `Pixel_6_API_34`, on 2026-10-01. Every replacement already held
the identical value on the parent commit (`ThemeDerivationTest.kt:167-171` at `613316c`), so this check
confirms the screens render; it does not compare colours.

| View | Fields exercised | Result | Screenshot |
|---|---|---|---|
| Discover header, light | `outlineControl` (was `strongBorder`) on the Settings button | outline visible | `walkthrough/discover-light.png` |
| Reset confirmation, light | `outlineControl` border, `quiet` Cancel | renders as before | `walkthrough/reset-light.png` |
| Reset confirmation, dark | the same, dark scheme | renders, outline legible | `walkthrough/reset-dark.png` |

**Not walked:**
- The import confirmation needs a file picked through the system's file chooser.
- `LocalStateMessages` (the `primarySoft`, `primary` and `quiet` call sites) needs a failed or empty
  dataset.
- The undo toast needs a triage action.

These views use the same identifier swaps, but I did not exercise them on screen. I recorded the gap here
rather than staging those conditions.

The app is back on **System** appearance.
