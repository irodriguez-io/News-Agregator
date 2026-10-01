# 026 — One name for each colour

**Surface:** Android, `ui/theme/**` and the consumers of four token fields. No colour on screen changes.
**Authority:** `specs/017-android-m3-design-tokens/design.md` D1 (the mapping table and its *"Retires in"*
column); `specs/backlog.md` Road to done row 3; `specs/waves/wave-e-note.md` (*"It came due at the close and is
still owed"*).
**Branch:** `feat/026-android-legacy-token-names`, cut from `main` at `498c7d7`, PR targets `main`.

---

## 1. Why this item exists

### 1.1 What 017 left behind, and when it was due

Item 017 kept every legacy field of `IntentionalReadingTokens` so that no consumer had to change inside a
token item (D1). For four of them that meant **one colour under two names**, and D1 scheduled exactly those
four to retire at wave E's close:

| Legacy field | Same value as | Retires in (D1) |
|---|---|---|
| `accent` | `primary` | wave E close |
| `accentSoft` | `primarySoft` | wave E close |
| `quietInk` | `quiet` | wave E close |
| `strongBorder` | `outlineControl` | wave E close |

The equalities are asserted today, at `ThemeDerivationTest.kt:167-171`.

### 1.2 Why four, not thirteen

The backlog row and the wave E note say *"the 13 legacy token names"*. D1 retires four. The other nine have
no second name, so there is no duplicate to retire: `bg`, `surface`, `fg`, `muted`, `border` are the seeds
themselves, and `surfaceHover`, `toastSurface`, `toastInk`, `backdrop` have no new-name equivalent. D1 marks
all nine *"never"*. **Owner's decision, 2026-09-30: retire the four.** The row's wording is corrected at ship.

### 1.3 Where the four are used today

15 call sites in 7 consumer files, the token file itself, and two test files:

| File | Lines | Fields |
|---|---|---|
| `ui/components/LocalStateMessages.kt` | 37, 54, 63, 69, 89 | `accentSoft`, `quietInk` ×2, `accent`, `strongBorder` |
| `ui/components/ResetConfirmation.kt` | 63, 74 | `quietInk`, `strongBorder` |
| `ui/components/ImportConfirmation.kt` | 64, 75 | `quietInk`, `strongBorder` |
| `ui/screens/settings/SettingsSheet.kt` | 281, 302, 313 | `strongBorder` ×3 |
| `ui/components/UndoToast.kt` | 36 | `strongBorder` |
| `ui/components/EditorialHeader.kt` | 57 | `strongBorder` |
| `ui/screens/discover/DiscoverHeader.kt` | 105 | `quietInk` |
| `ui/theme/Tokens.kt` | 20, 21, 23, 24 (fields); 62-66 (`blendTokens`); 174-178 (`tokensFrom`) | all four |
| `test/…/ui/theme/ThemeDerivationTest.kt` | 167, 168, 170, 171 (`assertLegacyMappings`) | all four |
| `test/…/ui/theme/AppearanceTransitionTest.kt` | 182, 183, 185, 186 (`namedColors`); comment at 32 (*"all 26"*) | all four |

Paths are under `android/app/src/main/kotlin/io/irodriguez/intentionalreading/` unless shown as `test/…`.

---

## 2. Story

As the owner, I want each colour in the Android theme to have one name, so that a future change to a colour
cannot update one name and leave its twin behind.

---

## 3. Out of scope

- The nine fields in §1.2. Not renamed, not moved, not re-valued.
- Any colour value. Every replacement name already holds the identical value (§1.1).
- Any composable's layout, spacing, `dp` literal, or behaviour. Each touched line changes one identifier.
- `ui/theme/Theme.kt` and `Color.kt` — neither references the four.
- `docs/v1/**`. Its `--accent` / `accent-soft` are the web's CSS names, not these Kotlin fields.
- `pipeline/**`, `config/**`, the web runtime, Gradle files, `.github/workflows/**`.

---

## 4. Scenarios

### Scenario: the four duplicate names no longer exist
Given the token object after this item
When its fields are listed
Then none is named `accent`, `accentSoft`, `quietInk` or `strongBorder`
And `primary`, `primarySoft`, `quiet` and `outlineControl` are all still present

### Scenario: every former use resolves to the same colour
Given each call site in §1.3
When it is rewritten
Then `accent` becomes `primary`, `accentSoft` becomes `primarySoft`, `quietInk` becomes `quiet`, and
`strongBorder` becomes `outlineControl`
And nothing else on that line changes

### Scenario: the nine remaining legacy fields keep their values
Given `bg`, `surface`, `fg`, `muted`, `border`, `surfaceHover`, `toastSurface`, `toastInk` and `backdrop`
When the token object is read in either scheme
Then each still equals the value `ThemeDerivationTest` asserts for it today

### Scenario: the appearance switch still blends every field
Given `blendTokens` between the light and dark tokens
When it is called at fraction 0, an interior fraction, and 1
Then `AppearanceTransitionTest` passes with the four entries removed from its `namedColors()` list and its
*"all 26"* comment reading 22, and no other change to it

---

## 5. Verification

### 5.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Counts: **404 JVM** (403 + the one absence test) **/ 28 instrumented**, unchanged.

### 5.2 The names are gone

`grep -rnwE "accent|accentSoft|quietInk|strongBorder" android/app/src --include=*.kt` returns only the new
absence test's string literals.

(Quote the glob under zsh: `--include='*.kt'`.)

### 5.3 The diff is identifier-only

`git diff main -- android/app/src/main ':!**/ui/theme/Tokens.kt'` shows only lines where one of the four
names became its replacement — no other token, literal or argument moves.

### 5.4 No pixel changes — walkthrough, not a gate

Because every replacement holds the identical value (asserted at `ThemeDerivationTest.kt:167-171` on the
parent commit), no screenshot comparison is required. One look at Settings, the import and reset
confirmations, and the empty/error state in both schemes on a debug build, recorded in `evidence.md`.
