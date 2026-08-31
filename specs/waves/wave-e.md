# Wave E — Material 3 Expressive redesign

**Items:** 017, then 018, then 019 ∥ 020, then 021
**Prerequisite:** wave D merged
**Cut from:** `main`

Self-contained brief. Read `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`,
`specs/execution-model.md`, then this file, then **both design sources**, which are vendored into the
repo so they cannot drift out of `~/Downloads`:

- `specs/design/m3-expressive-DESIGN.md` — the design system: tokens, brand, components.
- `specs/design/m3-expressive-PRD.md` — the product brief: IA, component states, motion specs.

---

## What this wave is, and what it deliberately is not

A full visual redesign of the Android client onto **Material 3 Expressive**: new palette, new type
pairing (Playfair Display for editorial, Roboto Flex for functional), 24dp card radii, pill chips, an
expressive bottom bar, and directional motion between destinations.

**The information architecture does not change.** Three destinations plus a Settings overlay, exactly as
today. Nothing in this wave alters what the app does, only how it looks and moves. **No behaviour change
is authorized by this brief** — if a redesign item finds itself changing a state transition, a count, a
ranking, or an undo path, that is a defect in the item's scope, not a licence.

### Imagery is out of scope, by decision

The design sources call for a 16:9 top-aligned image slot on the deck card, an 80dp square thumbnail on
the Queue Row, and 20dp internal rounding on media slots.

**`ArticleDataset v1` carries no image field of any kind.** The Article contract (`contracts.md` §5–6) is
`id`, `title`, `url`, `source`, `category`, `publishedAt`, `author`, `excerpt`, `readingTimeMinutes`,
`tags`, `contentType`, `score` — no image, no thumbnail, no media, no enclosure. The Android client
consumes that contract **read-only** and must not modify `pipeline/**` or `config/**` (`AGENTS.md`).

Adding imagery is therefore not a UI change. It is a frozen-contract change: a new Article field, a
`schemaVersion` bump, image extraction in the pipeline, validator and web-runtime updates, and a decision
about hotlinking third-party images. That is its own wave, ahead of this one, touching every frozen
boundary the project has.

**The owner chose on 2026-08-31 to ship this wave without imagery.** The deck card leads with the
Playfair headline instead of a media slot; the Queue Row drops its thumbnail and uses the horizontal
space for the headline and meta. Most of the design's editorial character lives in the type, palette,
shape and motion, and all of that is deliverable today.

**Design every component so imagery can be added later without a re-layout.** If a media wave ever runs,
this wave should not need redoing.

---

## Two gaps in the design sources that the design passes must close

**The palette is light-only.** `m3-expressive-DESIGN.md` ships one complete set of tokens and no dark
scheme. Item 010 shipped launch theming and Settings offers Light / Dark / System today. **Shipping
light-only tokens would regress a shipped feature.** Item 017 must derive a full M3 dark scheme from the
same seed and get it approved — this is an owner checkpoint, not an implementer decision.

**Two type families, and the cheap path is assets.** Playfair Display and Roboto Flex can be bundled as
`res/font/` files, which are assets and need no Gradle dependency. Using
`androidx.compose.ui.text.googlefonts` instead **would** be a new dependency and needs explicit approval
under `AGENTS.md`. Bundle unless there is a reason not to, and say which was chosen and why.

---

## Numbers, and why they are allocated here

`execution-model.md` §3's table is scoped to items 005–011 only; everything after follows
`future-items.md`'s "allocated at design time". These five are allocated **here** for the same reason §3
gave: so two concurrent design sessions in this wave cannot both reach for 019.

| # | Slug | Branch |
|---|---|---|
| 017 | `017-android-m3-design-tokens` | `feat/017-android-m3-design-tokens` |
| 018 | `018-android-m3-shared-components` | `feat/018-android-m3-shared-components` |
| 019 | `019-android-m3-discover` | `feat/019-android-m3-discover` |
| 020 | `020-android-m3-readlater-history` | `feat/020-android-m3-readlater-history` |
| 021 | `021-android-m3-motion` | `feat/021-android-m3-motion` |

---

## Collisions and order

| Hub file | 017 | 018 | 019 | 020 | 021 |
|---|---|---|---|---|---|
| `ui/theme/**`, `res/values/**`, `res/font/**` | ● | | | | |
| `ui/components/**` (shared: app bar, nav bar, chips, buttons) | | ● | | | |
| `ui/components/ArticleCard.kt`, `ui/screens/discover/**` | | | ● | | |
| `ui/screens/readlater/**`, `ui/screens/history/**` | | | | ● | |
| `ui/IntentionalReadingApp.kt` (navigation, sheet, transitions) | | ● | | | ● |

**017 blocks everything.** Every other item consumes its tokens. Nothing in this wave may hardcode a
colour, a radius, or a font — if an item needs a token 017 did not define, that is a report to the
supervisor and an amendment to 017's output, not a literal in a composable.

**018 blocks 019 and 020**, which consume the shared components.

**019 ∥ 020 run concurrently.** Disjoint screen packages, and after 018 there is nothing left to share.

**021 runs last.** Directional tab motion is indexed to bottom-bar position, so it needs 018's nav bar
final, and the modal sheet reveal touches the same `IntentionalReadingApp` scaffold.

**021 also collides with wave D's 014/016 ground** (`IntentionalReadingApp.kt`). Wave D must be merged
before this wave starts, which `execution-model.md` §2 requires anyway.

---

## 017 — Design tokens and theme

The full token set from `m3-expressive-DESIGN.md`: colour roles, the type scale, shape scale, spacing
rhythm. Bundled fonts. **Plus the dark scheme the design source does not supply.**

**Definition of done includes that no other file changed.** This item ships a theme and nothing that
consumes it; the screens still render, in the new palette, because M3 roles map through.

## 018 — Shared components

TopAppBar with the centred brand title and trailing settings gear. BottomNavBar, M3 Expressive, 28dp top
radius, active pill indicator. Category chips, 40dp pill, solid primary when active and outlined when
not. Buttons: filled primary 52dp, tonal secondary, 56dp circular icon buttons with the 1.5dp border for
triage.

Component **states** are specified in `m3-expressive-PRD.md` §5 and are part of the contract: pressed is
a 12% overlay with a 0.95 scale-down, disabled is 38% opacity and non-interactive.

**Touch targets stay ≥ 48×48dp.** The design's 56dp icon buttons satisfy this; nothing in this wave may
shrink a target below it.

## 019 — Discover

The Article Deck Card: 24dp radius, white surface, soft ambient shadow with a Midnight Navy tint rather
than neutral grey, bold serif headline. Truncation is specified and testable — **headlines 3 lines,
descriptions 2 lines**, then ellipsis.

**Preserve item 012's placement rule.** Wave D moved Discover's operational header below the card and
wrote the intent into `docs/v1/**` as a rule, not a widget order. This item inherits that rule and must
not quietly restore the old ordering while re-laying out the screen.

## 020 — Read Later and History

The Queue Row: 16dp radius, tonal fill, horizontal layout — **without the 80dp thumbnail**, per the
imagery decision. The StatBand: a 3-column pill container grouping the reading metrics.
`readingTimeMinutes` already exists on `Article` and is nullable — **the design assumes a number is
always there and it is not**; decide the null presentation at design time.

History's high-fidelity empty state, per `m3-expressive-PRD.md` §4.

**Any new user-visible string is shared-copy territory.** Item 011 shipped web/Android copy parity;
`11-web-validator-parity` is the precedent for how a string is introduced. StatBand labels and the empty
state are the likely offenders.

## 021 — Motion

Directional tab navigation: 300ms, M3 Emphasized easing, lateral slide indexed to bottom-bar position,
with a scale-down and 0.8 opacity fade on the outgoing screen. Settings sheet: 350ms slide-up with
fade-in on a Decelerated curve, reverse-tuck exit, scrim dimming the feed.

**Reduced motion is already honoured in this codebase** — `reducedMotion` is threaded through
`IntentionalReadingApp` today. Every animation this item adds must respect it, and a test must say so.

---

## The amendment

**This wave rewrites `06-ui-ux.md`.** That is a `docs/v1/**` change of a size no feature workstream may
make silently, and it is larger than the narrow amendments 012 and 016 needed — it is closer in kind to
Amendment 6, which authorized the Android client in the first place.

**Write the amendment once, before item 017 is designed**, and have every item in the wave cite it rather
than amend separately. Five items each amending `06-ui-ux.md` on five branches is a merge nobody should
be asked to review.

The amendment must state what is **not** changing as clearly as what is: the IA, the three destinations,
the state machine, the ranking, the counts, the copy. A redesign amendment that reads as open season on
behaviour is the failure mode.

---

## Gates

Per `execution-model.md` §8. `android.yml` only — unless 020 introduces shared copy, which pulls in
`js/**` and its validators and makes that item a two-gate item. Decide that at 020's design pass, not at
its PR.

---

## Owner checkpoints

1. **The amendment**, before 017 is designed. Blocks the wave.
2. **The dark scheme**, derived in 017. The design source does not supply one and Settings already offers
   the switch.
3. **Fonts bundled versus a Google Fonts dependency**, if the design pass wants the dependency.
4. **A walkthrough at each merged item, not only at wave close.** Five items rewriting every screen is
   the most visually regressive thing this project has attempted; the wave's own note should be able to
   say the owner saw each screen.
5. **Wave sign-off** against merged `main`, on a device, in both light and dark.

---

## Definition of wave done

All five merged; `evidence.md` per item; walkthroughs recorded; `backlog.md` updated;
`wave-e-note.md` written. At that point the Android client is the app the design sources describe, minus
imagery, and the only thing standing between it and the full design is a dataset that carries pictures.
