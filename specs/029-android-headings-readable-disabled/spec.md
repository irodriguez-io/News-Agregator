# 029 — Screens announce their headings, and disabled text stays readable

**Surface:** Android only. Headings: `ui/screens/discover/**`, `ui/screens/history/HistoryScreen.kt`,
`ui/screens/settings/SettingsSheet.kt`, `ui/components/{EditorialHeader,EmptyStatePanel,ArticleCard,ArticleRow}.kt`.
Disabled text: `ui/screens/discover/{DiscoverHeader,DiscoverScreen}.kt`.
**Authority:** `docs/v1/06-ui-ux.md` §0 (*"Behaviour is never scoped … accessibility … binds both surfaces"*),
§37 shared, §37.2, §73, §75.1 (second edition); `specs/backlog.md` Road to done row 7a. **No amendment.**
**Branch:** `feat/029-android-headings-readable-disabled`, cut from `main` at `9ea2749`, PR targets `main`.

---

## 1. Why this item exists

Road 7, the second-edition sweep, found two accessibility rules that no item ever implemented on Android
(`specs/backlog.md` row 7):

1. **§73 requires *"semantic landmarks and headings"*.** No Android screen marks a heading: `src/main` has no
   `heading()` anywhere. A TalkBack reader cannot jump from section to section and has to swipe through
   every element.
2. **§37 shared: *"any text a reader is expected to read in a disabled state still meets §73's contrast
   requirement."*** While a refresh runs, the Discover header's button reads *"Refreshing…"*
   (`DiscoverHeader.kt:71-78`, label from `DiscoverScreen.kt:57-61`). While a retry runs, the error panel's
   *"Try again"* is disabled (`DiscoverScreen.kt:122-127`, `:215-222`). Both use Material's default disabled
   text colour, `onSurface` at 38%. That works out to about 2.4:1 on `bg` in light and about 3.1:1 in dark,
   against the 4.5:1 required. These figures are computed from the Material default and have not been
   measured on screen.

### 1.1 Which text is a heading: the browser already decides

The browser marks its headings with `h1`–`h3`. §75.1 makes behaviour parity mandatory, and §0 counts
accessibility as behaviour, so **the Android heading set is the browser's heading set, mapped to the same
text on Android.** This item invents no new list.

| Browser | Android | Where |
|---|---|---|
| `h1` view title (`discover.js:29`, `view-parts.js:8`) | *"Discover"*, *"Read Later"*, *"History"* | `DiscoverHeader.kt:35-38`; `EditorialHeader.kt:43-46` |
| `h2` card title (`discover.js:155`) | The Discover card's article title | `ArticleCard.kt:244-250` |
| `h2` Discover state title (`discover.js:72`) | The error and empty panel titles | `DiscoverScreen.kt:213` |
| `h2` empty Read Later and empty History (`read-later.js:8`, `history.js:10`) | The empty-state panel title | `EmptyStatePanel.kt:37` |
| `h2` / `h3` row titles (`read-later.js:49`, `history.js:36`) | Each Read Later and History row's title | `ArticleRow.kt:92-98` |
| `h2` History group (`history.js:96`) | *"Today"*, *"Yesterday"*, *"Earlier"* | `HistoryScreen.kt:142-146` |
| `h2` Settings, `h3` Appearance and Local data (`settings.js:235,58,243`) | *"Settings"*, *"Appearance"*, *"Local data"* | `SettingsSheet.kt:166`, `:205`, `:254` |
| — (Android only) | *"Content status"* | `SettingsSheet.kt:187` |

**Not headings, matching the browser:** eyebrows (*"A FINITE READING QUEUE"*, *"LOCAL PREFERENCES"*); the
app bar's *"Intentional Reading"* (a `<p>` in `index.html:19`); the loading panel (`discover.js:66-69`
renders no heading); StatBand labels, chips, tags, metadata, buttons and body copy.

**Two readings, for approval at the plan gate:**
- **"Content status"** has no browser counterpart, because the browser's Settings has no such section. On
  Android it is styled and placed as a peer of *"Appearance"* and *"Local data"*, so it is a heading.
- **Heading levels and landmarks.** Android's accessibility model has headings but no levels and no
  landmark role; TalkBack navigates *"by heading"* as one flat list. The browser's `h1`/`h2`/`h3` therefore
  all become one Android heading, and §73's *"landmarks"* has no Android element to implement.

### 1.2 Which disabled text must stay readable

The rule covers text *"a reader is expected to read in a disabled state"*. This item applies it to the two
Discover controls the sweep named: *"Refreshing…"*, which reports progress, and *"Try again"*, the error
panel's only action, which the reader is waiting on.

**Not covered:** buttons that keep their ordinary label while briefly disabled, such as Settings' Export and
Import during an import. Their label names the control rather than reporting anything. WCAG 1.4.3
exempts inactive controls from contrast, and §37.2's *"38% opacity"* is the stated Android treatment for
them. `ResetConfirmation.kt` and `ImportConfirmation.kt` already draw disabled labels in `muted`, and they
are left as they are.

---

## 2. Story

As a **reader using TalkBack**, I want each screen's sections announced as headings, so that I can move
between them without swiping through every element. As a **reader**, I want a busy control's text to stay
legible, so that I can see what the app is doing.

---

## 3. Scenarios

### Scenario: every screen's section titles are headings
Given Discover shows an article card
Then *"Discover"* and the article's title are headings
Given Read Later holds saved articles
Then *"Read Later"* and each row's title are headings
Given History holds articles read today, yesterday and earlier
Then *"History"*, *"Today"*, *"Yesterday"*, *"Earlier"* and each row's title are headings
Given the Settings sheet is open
Then *"Settings"*, *"Content status"*, *"Appearance"* and *"Local data"* are headings

### Scenario: state panels title themselves
Given Discover cannot load the dataset
Then the error panel's title is a heading
Given the selected category has no articles
Then the empty panel's title is a heading
Given Read Later is empty, or History is empty
Then the empty-state panel's title is a heading

### Scenario: nothing else is a heading
On each screen above
Then the set of heading nodes is exactly the set listed for that screen, and no eyebrow, metadata line,
tag, chip, StatBand label, button or body copy is a heading

### Scenario: a heading still reads as itself
Given any heading above
Then its text and its existing accessible name are unchanged
And no existing semantics test needs editing

### Scenario: "Refreshing…" is legible while it runs
Given a refresh is in progress
Then the header button reads *"Refreshing…"* and is not enabled
And its text contrast against `bg` is at least 4.5:1 in light and in dark

### Scenario: "Try again" is legible while a retry runs
Given the dataset failed and a retry is in progress
Then *"Try again"* is not enabled
And its text contrast against the panel's `container` is at least 4.5:1 in light and in dark

### Scenario: disabled still looks disabled
Given either control is disabled
Then its text colour differs from its enabled text colour, and its outline differs from its enabled outline
And tapping it does nothing

### Scenario: the enabled controls are unchanged
Given a refresh is available, or a retry is available
Then *"Refresh"* and *"Try again"* look and behave exactly as before

---

## 4. Out of scope

- Every other disabled control (§1.2).
- Focus indication (§38; the owner accepted the current state, 2026-10-01).
- New semantics beyond `heading()`: no pane titles, traversal order, custom actions or content-description
  changes.
- Visual changes to any heading. Only semantics change.
- The browser runtime, `pipeline/**`, `config/**`, `docs/v1/**`.

---

## 5. Verification

### 5.1 Gates

With `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` and
`ANDROID_HOME=$HOME/Library/Android/sdk` exported, from `android/`:
`./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`.
Baseline **405 JVM / 30 instrumented**; each slice reports the counts after.

**The tests assert the rendered result** (backlog guardrail, 2026-10-01): headings through the
semantics tree TalkBack reads, and colour through the text layout's resolved colour. Neither reads source
text.

### 5.2 Orchestrator

A screenshot of *"Refreshing…"* in light and dark, recorded in `evidence.md`, with the label's contrast
measured from the pixels.

### 5.3 Owner walkthrough on a signed release build

With TalkBack on, switch its reading control to *Headings* and swipe down through Discover, Read Later,
History and Settings. Each stop should be a section title from §3, and nothing else. Uninstall the release
build afterwards. Walkthrough findings go through the freeze test (`specs/backlog.md`, guardrails).
