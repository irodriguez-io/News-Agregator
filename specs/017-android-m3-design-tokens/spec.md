# 017 — Material 3 Expressive design tokens and theme

**Item:** 017\
**Branch:** `feat/017-android-m3-design-tokens`\
**Wave:** E, first item — blocks 018, 019, 020 and 021\
**Cut from:** `main`

Cites, and amends nothing: `docs/v1/06-ui-ux.md` §§11.2, 12.2, 14.2, 15.2, 16.2, 22.2, 27.2, 34.2, 35.2,
73.1, 76.1, 77, 78; `docs/v1/08-security-dependencies.md` §7.1–§7.2; `android/THIRD-PARTY-FONTS.md`.

---

## 1. Why this item exists

Amendment 9 gave the Android client its own visual direction. **This item ships the theme that direction
is made of, and nothing that consumes it.** Every other wave-E item reads its tokens; none of them may
name a colour, radius, duration or font of its own (§77.1).

The screens keep rendering after this item merges, in the new palette, because Material 3 roles map
through. They will not yet be *laid out* to the new design — that is 018 through 021.

### 1.1 The hard part is not colour

The palette was settled before this item was designed. Ten seeds, both schemes, every value recorded in
`06-ui-ux.md` §77.4 and §77.5. **This item does not re-decide a single colour.**

The hard part is that **17 files consume the theme across 205 token call sites**, using thirteen field
names on `IntentionalReadingTokens`:

```text
bg  surface  fg  muted  border  accent  accentSoft  surfaceHover
strongBorder  quietInk  toastSurface  toastInk  backdrop
```

Replacing that object with a ten-seed shape under new names breaks all 17 files at compile time. That
would violate this item's definition of done, and it is exactly the shape of item 014's slice-1 failure —
a signature changed away from the call sites that pass it (`execution-model.md` §2.1, edge two).

### 1.2 So the token object grows; it is not replaced

**Every one of the thirteen existing field names survives this item.** Their *values* change to the new
palette. The new roles — the ten seeds by name, plus `card`, `container`, `primarySoft`, `outlineVariant`,
`outlineControl`, `quiet`, `onPrimary`, `onTonal` — are **added alongside** them.

Nothing outside the theme package changes, and nothing outside the theme package needs to. Items 018–020
migrate their own call sites to the new names as they re-lay out their own components, which is work they
are doing anyway.

This is `execution-model.md` §2.1 rule 4 run in mirror image. That rule says: when a change widens what a
producer *emits*, land the consumers first. Here the producer widens what it *offers*, so the safe order
inverts — extend the producer while nothing consumes the new fields, and let each consumer turn them on in
its own item, on a tree that is already green.

**Transitional duplication is intended, not accidental.** For the length of this wave, `accent` and
`primary` are the same colour under two names. Wave E's close is where the old names come out, once no
file references them.

### 1.3 The contrast floor is the one thing here that must be computed

`06-ui-ux.md` §73.1 requires that **where a control's boundary is the only thing identifying it as a
control, that boundary reach 3:1 against the surface behind it, in both schemes.**

Three candidate values for that boundary failed the floor during the design pass and **every one of them
looked acceptable on screen**: the design source's own `#7692FF` triage ring at 2.9:1, a hairline one
lightness step off the card at 1.6:1, and the `border` seed at 1.4:1 in dark, where it is darker than the
card it sits on.

So §78.3 splits one outline token into two roles with different obligations, and this item's tests must
**assert the ratio, not the value**. A test that freezes `outlineControl`'s hex proves nothing about
whether it clears the floor after any later reseed.

### 1.4 A latent defect this item closes

`LocalStateMessages.kt:48` renders with `MaterialTheme.typography.titleMedium`. **That slot is not defined
in `IntentionalReadingTypography`**, so it silently falls back to Material 3's default face — one string in
the shipped app has never used the authored type stack.

The app consumes twelve M3 typography slots. `06-ui-ux.md` §76.1 authors nine styles. Closing the gap is
part of this item, and §5.2 states how without inventing metrics the specification is silent about.

---

## 2. Story

As **the reader**, I want the application to look like the design it was given, in both light and dark, so
that reading it feels like a deliberate publication rather than a default.

As **the implementer of items 018–021**, I want every colour, size, radius and spacing value to arrive
from the theme, so that no component of mine has to name one.

---

## 3. Out of scope

- **Any file outside `ui/theme/**`, `res/values/**`, `res/values-night/**` and this item's own tests.**
  This is a definition-of-done condition, not a preference — see §5.1.
- **Re-deciding any colour.** The seeds are owner-approved and recorded in §77.4/§77.5.
- **Migrating the ~160 hardcoded `dp` literals** in `ui/components/**` and `ui/screens/**` to the new
  spacing and shape scales. This item *defines* the scales. Items 018–020 migrate their own literals.
- **Removing the thirteen legacy token field names.** They stay for the whole wave (§1.2).
- **Fetching or modifying the font binaries.** They are already vendored, hashed and licensed
  (`android/THIRD-PARTY-FONTS.md`). This item loads them.
- **Any component's appearance beyond what re-mapping M3 roles produces.** No composable is re-laid out.
- **`androidx.compose.ui.text.googlefonts`**, or any new Gradle dependency.
- **Motion.** Item 021.
- Imagery, in any form (§74.2).

---

## 4. Scenarios

### Scenario: the authored light seeds are the approved values
Given the light scheme
When the ten seed colours are read
Then they are exactly the values `06-ui-ux.md` §77.4 records
And no eleventh colour is authored in the light scheme

### Scenario: the dark scheme is derived, not authored
Given a brand seed and its dark counterpart
When both are converted to Oklch
Then the dark counterpart's hue and chroma equal the light seed's to within rounding
And only its lightness differs
And this holds for `primary`, `secondary`, `tonal`, `tertiary` and `error`

### Scenario: derived roles come from the stated mixes
Given the ten seeds in either scheme
When each derived role is computed by the rule `06-ui-ux.md` §78.2 states for it
Then the result equals the value the theme exposes for that role

### Scenario: a near-achromatic endpoint adopts the other endpoint's hue
Given a saturated blue and pure white
When they are mixed in Oklch
Then the result's hue equals the blue's hue
And the result is not shifted toward yellow or green

### Scenario: a control-boundary outline clears the floor in both schemes
Given the light scheme and the dark scheme
When the control-boundary outline is measured against the card surface behind it
Then the WCAG contrast ratio is at least 3:1 in each scheme
And the same holds for the triage control's outline seed

### Scenario: the decorative hairline is not held to the control floor
Given either scheme
When the decorative hairline is measured against the card
Then it is visible as a divider
And no assertion requires it to reach 3:1, because it divides rather than identifies

### Scenario: the launch background still equals the composed background
Given the light scheme and the dark scheme
When the launch background resource is compared with the composed background token
Then they are equal in both schemes

### Scenario: every typography slot the application consumes is authored
Given the set of Material 3 typography slots referenced anywhere in `ui/**`
When each is read from the theme
Then every one of them is an authored style
And none of them is Material 3's default for that slot

### Scenario: the authored type styles carry their specified metrics
Given the nine styles `06-ui-ux.md` §76.1 specifies
When each is read from the theme
Then its family, size, line height, weight and letter spacing are the values §76.1 states
And sizes are expressed in `sp`

### Scenario: both bundled families load and resolve their weight axis
Given the two `res/font/` resources
When a style requests a weight the family supports
Then that weight resolves from the bundled variable font
And no font is requested over the network

### Scenario: every legacy token field still exists
Given the thirteen field names listed in §1.1
When the token object is read
Then every one of them is still present
And the application's 205 existing call sites compile unchanged

### Scenario: no colour is named outside the theme package
Given the source tree after this item
When files outside `ui/theme/**` are inspected for colour literals
Then the count is unchanged from before this item

---

## 5. Verification

### 5.1 Gates

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
  ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Both must pass. `main` is at **297 tests, 0 failures** at `e126cbb`; this item only adds tests.

`android.yml` fires. `test.yml` also fires, on every PR regardless of paths — see PR #27 on
`execution-model.md` §8's inaccuracy about that.

**And the definition-of-done check, which is a diff check rather than a gate:**

```bash
git diff --name-only main... | grep -vE '^android/app/src/(main/kotlin/io/irodriguez/intentionalreading/ui/theme/|main/res/values(-night)?/|test/kotlin/io/irodriguez/intentionalreading/ui/theme/)' | grep -v '^specs/'
```

This must print nothing. Any other path is out of scope by §3 and is a finding at slice review.

### 5.2 What is assertable at the JVM layer, and what is not

**Assertable, and therefore required:** every seed value; the hue-and-chroma invariance of the dark
derivation; every derived role against its stated mix; the achromatic-hue rule; **WCAG contrast ratios,
computed**; the launch-background equality; the presence of every legacy field; the family, size, weight
and tracking of every authored style; and that every consumed M3 slot differs from Material 3's default.

**Not assertable at the JVM layer, and therefore walkthrough evidence:** whether the rendered result reads
as the intended design; whether the Playfair headline is actually loading rather than silently falling back
to a platform serif; whether dark is pleasant rather than merely compliant.

**The silent-fallback risk deserves naming.** A missing or misnamed font resource does not fail a JVM test
— Compose falls back and the text still renders. The test asserting that the family is the bundled resource
is the only cheap guard, and the walkthrough is the real one.

**On the three consumed slots §76.1 does not author.** The application consumes `bodySmall`, `labelSmall`
and `titleMedium`; §76.1 authors neither. `AGENTS.md` forbids inventing requirements where a specification
is silent, so this item **must not invent metrics for them.** Each is assigned to one of the nine authored
styles:

```text
bodySmall   → body-md
labelSmall  → label-md
titleMedium → headline-sm
```

No new size, weight or tracking value is created. If a reviewer believes a different assignment is right,
that is a specification question for the supervisor, not an implementer's choice.

### 5.3 On the existing assertion surface

Per `execution-model.md` §2.1 rule 5, the following is **named with reasons and is not frozen**. It was
accurate when written, against `e126cbb`. **An unlisted failure must be reported before it is edited.**

| Test | Why this item reaches it | Expected |
|---|---|---|
| `ThemeDerivationTest` | freezes 24 hexes positionally against the twelve-field list | **rewritten by this item** — new table, extended to cover the added roles |
| `LaunchBackgroundTest` | asserts `lightTokens().bg` equals `colors.xml`'s `launch_background` | **stays green** if and only if the seeds and both `colors.xml` files change in the same slice |
| `LaunchThemeInheritanceTest` | parses style parents and items only | untouched |
| `LaunchNightModeTest` | night-mode push behaviour | untouched — this item changes no behaviour |
| `MainActivityLaunchSmokeTest` (instrumented) | launches the activity | untouched; not run by the JVM gate |

`LaunchBackgroundTest` is the item's own safety net and it exists because item 010 asserted the
*invariant* rather than the hex — the comparator lesson from wave C, banked and now paying out.

### 5.4 Walkthrough — required evidence

Batched at wave close per `execution-model.md` §6, **plus one owner look at this item's merge** per
`wave-e.md`'s checkpoint 4. Drive over `adb` on one AVD:

1. Cold launch in light. **Does the launch frame match the first composed frame, with no flash of the old
   palette?**
2. Discover, light. Is the headline in Playfair, or has it fallen back to a platform serif?
3. Settings → Dark. Discover, Read Later, History, Settings in dark.
4. Settings → Light, then System, with the device in each state.
5. At 360 dp and at the emulator's native width.

And after each step, the second question `execution-model.md` §9 requires: *and is what the reader needs
next actually on screen?*

### 5.5 Owner checkpoints

**None outstanding.** The palette and the derived dark scheme were approved on 2026-09-01 before this item
was designed, and the font-bundling decision landed in PR #28. The only owner involvement is the
walkthrough look at §5.4.

### 5.6 Stop conditions

Report to the supervisor rather than proceeding, if:

- a role any wave-E item needs is not derivable from the ten seeds — that is an amendment to this item's
  output, never a literal in a composable (§77.1);
- the control-boundary derivation cannot reach 3:1 in a scheme;
- `variationSettings` requires an opt-in or API level this project cannot satisfy at `minSdk 26`;
- a test not listed in §5.3 fails;
- the definition-of-done diff check in §5.1 cannot be satisfied.

---

## 6. The gap this item does not close

**The application will look half-redesigned when this merges, and that is correct.** New palette and new
type, on the old layout — 24 dp radii, pill chips, the tonal nav indicator and the expressive bottom bar
all arrive with items 018–021.

**The thirteen legacy token names remain**, so for the length of wave E two names exist for several
colours. Retiring them is wave E's close, not this item.

**~160 `dp` literals remain in the components.** This item gives them a scale to move to; it does not move
them.

**`stat-num` will be defined and unused** until item 020 builds the StatBand. That is the same
land-it-additively shape as the fonts themselves.
