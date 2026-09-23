# Backlog

The visibility surface for this project: what has shipped, what is queued, and what has been parked on
purpose. It is a tracking document, not a specification. Nothing here is approved scope, nothing here
binds `docs/v1/**`, and nothing here substitutes for the `spec.md` that `/feature-design` writes.

**On numbers.** Shipped items own their numbers. Numbers 005–011 were allocated by
`execution-model.md` §3 so that concurrent design sessions cannot both claim one; for these seven that
supersedes `future-items.md`'s "allocated at design time". Anything added below 011 follows the old rule.

**On execution.** `execution-model.md` says how these run — three waves, sequential, with concurrency
inside each wave. Per-wave briefs are in `specs/waves/`, each self-contained enough to hand to a fresh
session.

Last reviewed: 2026-09-22, with **wave E implemented and its close outstanding**. Items 017–022 are all
merged; the backlog holds item **023** and the wave-close work itself.

**On PR numbers.** The repository moved from `~/Documents/VS Code/` to `~/Documents/Repos/` and its remote
changed to `irodriguez-io` on 2026-09-18, which restarted PR numbering at #1. PRs #30–#34 and PRs #1–#4
therefore both exist and are not out of order — anything numbered below #5 is later than anything numbered
above #29.

---

## Shipped

| # | Item | Surface | Merged |
|---|---|---|---|
| 001 | Opened article return state | Browser | PR #3, 2026-08-22 |
| 002 | Android client foundation | Android | PR #4, 2026-08-22 |
| 003 | Android local state persistence | Android | PR #5, 2026-08-24 |
| 004 | Android dataset refresh | Android | PR #6, 2026-08-25 |
| 007 | Undo | Android | PR #10, 2026-08-25 |
| 010 | Launch theme | Android | PR #9, 2026-08-25 |
| 011 | Web validator parity and shared copy | Browser + Android | PR #8, 2026-08-25 |
| 008 | Swipe gestures | Android | PR #12, 2026-08-26 |
| 009 | Import and export | Android | PR #13, 2026-08-26 |
| 013 | Undo gesture reset — a card accepts a swipe as soon as it is on screen | Android | 2026-08-28 |
| 006 | Deck diversity sequencing | Android | 2026-08-31 |
| 015 | A swipe must be attributed to the article the reader saw | Android | PR #20, 2026-08-31 |
| 012 | The Discover card leads the viewport | Android | PR #21, 2026-08-31 |
| 014 | The undo offer follows the reversible action, not the gesture | Android | PR #22, 2026-08-31 |
| 016 | Widen what is reversible | Android | PR #24, 2026-08-31 |
| 017 | M3 design tokens and theme, with a derived dark scheme | Android | PR #30, 2026-09-01 |
| 018 | M3 shared components — app bar, bottom bar, chips, buttons | Android | PR #31, 2026-09-01 |
| — | Instrumented tests in CI (not a numbered item) | Android | PR #32, 2026-09-01 |
| 019 | M3 Discover — deck card, truncation rules | Android | PR #33, 2026-09-02 |
| 020 | M3 Read Later and History — Queue Row, StatBand, empty state | Android | PR #34, 2026-09-02 |
| 021 | M3 motion — directional tab slide, modal sheet reveal | Android | PR #3, 2026-09-19 |
| 022 | The appearance switch changes colour, not the screen | Android | PR #4, 2026-09-20 |

Each has `spec.md`, `design.md`, `slices.md`, and `evidence.md` under `specs/<n>-<slug>/`.

---

## Queued

**One item, and a wave close.** Wave E's five items are implemented and merged, and so is **022**, the
first defect the owner's wave-E walkthrough found. The queue holds **023** — the second defect from that
same walkthrough — and the wave-close bookkeeping 021's evidence itemised.

**Wave E's close is outstanding, and it is three distinct pieces of work.** `waves/wave-e-note.md` is
unwritten, this document's wave row is the only record that the wave finished, and **the thirteen legacy
token names item 017 kept alive for the wave's duration have not been retired** — they are still the first
thirteen fields of `ui/theme/Tokens.kt`. That debt was scoped to the wave and came due at its close.
*(021 `evidence.md`, §Wave-close work that outlives this item.)*

**The walkthrough itself was performed**, on a signed release build, on 2026-09-20. It found two defects,
both motion and both on Android: the appearance flash (**022**, shipped) and the card swipe exit and
entrance (**023**, designed, below). That is the fourth wave running in which the owner using the app found
the defects no gate did — see `waves/wave-c-note.md` and `waves/wave-d-note.md` for the same headline.

**Item 012's 360 dp finding carried into 019 and still stands after it.** The Discover card's full action
rail cannot be visible at 360 dp under any header arrangement, because card height is unbounded in the
article title. The measurements and the limitation are in
`specs/012-android-discover-card-first/spec.md` §1.4. The redesign did not change that and was not expected
to.

**All five waves are done.** `waves/wave-b-note.md` records what wave B cost and `waves/wave-c-note.md`
what wave C cost; **wave E's note is the piece of the close that is still missing.** Their shared headline
lesson, now four waves running: the most valuable defects were found by the owner using the app — none by
reading diffs, and none by any gate. Wave E is the clearest case yet, because it ran with the instrumented
suite in CI for the first time and the two defects it shipped were still found by hand.

| Wave | Items | Runs after | Brief |
|---|---|---|---|
| ~~A~~ | ~~007 Undo · 010 Launch theme · 011 Validator parity~~ | **merged 2026-08-25** | `waves/wave-a.md`, `waves/wave-a-note.md` |
| ~~B~~ | ~~008 Swipe · 009 Import/export~~ | **merged 2026-08-26** | `waves/wave-b.md`, `waves/wave-b-note.md` |
| ~~C~~ | ~~005 Learning · 006 Diversity~~ | **merged 2026-08-31** | `waves/wave-c.md`, `waves/wave-c-note.md` |
| ~~D~~ | ~~015 Undo race · 012 Card first · 014 Raise the offer · 016 Widen what is reversible~~ | **merged 2026-08-31** | `waves/wave-d.md`, `waves/wave-d-note.md`, `waves/wave-d-amendments.md` |
| ~~E~~ | ~~017 Tokens · 018 Components · 019 Discover · 020 Read Later + History · 021 Motion~~ | **all five merged 2026-09-01 → 2026-09-19; wave-close work still open** | `waves/wave-e.md`, `waves/wave-e-amendment.md` |

**Items 022 and 023 run outside the waves**, as unplanned defect items cut from `main` after wave E's
walkthrough. Both are Android motion; neither collides with the other — 022's surface is
`AndroidManifest.xml`, `ui/theme/Theme.kt` and `ui/theme/Tokens.kt`, and 023's is
`ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt` and `ui/screens/discover/**`.

**Item 013 ran outside the waves**, as an unplanned defect item cut from `main` at `2613959` while wave C
was open. It touched **no file item 006 touches** — confirmed at close: 013's surface is
`ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt` and the instrumented gesture tests; 006's is
`domain/**` and its JVM tests. The one real overlap is documentary — both edit `specs/backlog.md`, and
013 also edits `specs/005-android-preference-learning/evidence.md`, which 006's branch has already
touched. **006 should expect a conflict in those two files and keep both sides**, as `execution-model.md`
prescribes for rolling documents.

Waves are ordered by file collisions, not by value — see `execution-model.md` §2 for the hub-file matrix
that produced them. 007 led the programme deliberately, because `contracts.md` §23 ties Undo to the
`signalsApplied` reversal guard 005 introduces; that ordering has now been banked.

### ~~005 — Preference learning and personalized ranking~~  ·  **Shipped**

Ported `js/ranking/personalize.js` and the `INTERACTION_DELTAS` table. Four slices: the arithmetic, the
state machine and undo path, the reconciliation fold, and personalized scoring with deck order.
198 → 254 tests.

The inherited decision was taken by invariant rather than by marker (`design.md` D1): `interactions` is
exactly recomputable from the record set, weight is not, so the fold applies only the missing signals'
deltas and never audits a weight. It runs at two call sites — cold load and import — and deliberately
not at the single choke point every path funnels through, because that one also runs on every ordinary
save and would silently repair arithmetic bugs in the state machine.

*Evidence:* `specs/005-android-preference-learning/evidence.md`.

### ~~006 — Deck diversity sequencing~~  ·  **Shipped**

The −8 same-source and −5 third-consecutive-category penalties (`js/ranking/deck.js:26-38`), ported as a
greedy per-step algorithm rather than a sort key. One slice, 76 lines of production code, 258 → 275 tests.

**It has no reader-visible effect at today's surface, and its walkthrough says so at every step.** The
claim that the Android head article differed from the browser's until this landed was **wrong** — item
005 closed that gap by itself, because `penaltiesFor` reads `selected.at(-1)` and the first selection
step carries no penalty. Recorded in 005's `design.md` D12, this item's `spec.md` §1.1, and
`waves/wave-c-note.md` §4 so it stops being re-inherited. It shipped anyway, on the owner's decision of
2026-08-26, as a genuine spec-parity gap.

Its real cost was three plan defects — two of them supervisor arithmetic and scope errors caught by the
implementer refusing to build a contradiction, one a vacuous assertion caught at review. None reached
shipped code. The transferable lesson is in `waves/wave-c-note.md` §2: **assert a comparator against the
comparator, not through the algorithm that consumes it.**

*Evidence:* `specs/006-android-deck-diversity/evidence.md`. *Deferred by 002 §3, restated by 004 §3.*

### ~~014 — Raise the undo offer where the reversal already exists~~  ·  **Shipped**

**Re-scoped 2026-08-31 on the reversibility line** (was: "The Discover card's buttons in the Undo
window"). The number and every citation to it — including
`specs/013-android-undo-gesture-reset/spec.md` §1.8 — still resolve. `waves/wave-d.md` has the table.

`SAVE` and `DISMISS` are already in `ArticleStateMachine.reversibleActions`. Every surface that performs
them **except Discover's swipe** fails to ask for the offer, because **`undoable = true` is passed at
exactly one call site in the whole app** — `IntentionalReadingApp.kt:290`, `onSwipeCommit`. Everything
else takes the `undoable = false` default.

**Confirmed in the app on both the 006 and pre-006 builds** (`006/evidence.md` §5 step 5): the *Save for
later* button **commits the save** — the record is written, Read Later increments — and simply never
offers the reversal. Screencaps at 0.2 s, 1.0 s and 2.0 s show no toast; the swipe path raises one at
0.35 s.

**This is not "the button fails."** It succeeds. The original 014 wording described a failing button and
was wrong about the mechanism.

**Superseded 2026-08-31 at the design pass: this item does need an amendment.** It was recorded here as
amendment-free on the grounds that nothing about *what is reversible* changes — true, but Undo is scoped to
the **swipe gesture** in `contracts.md` §31, `05-personalization-state.md` §36, `06-ui-ux.md` §70,
`01-product.md` §14 and `09-testing-acceptance.md` §50, and items 007 (`spec.md` §1.1) and 008
(`design.md` D8) made the labelled buttons non-undoable on purpose. **This item reverses a specified
decision** and needs **Amendment 8**, shared with 016 and drafted in `waves/wave-d-amendments.md`. Owner
decision, 2026-08-31.

**Out of scope: *Mark read*, anywhere.** It is `MARK_READ`, it is not reversible today, and moving it to
016 keeps this item to one dimension — the surfaces, not the action set.

**It inherits no cause from 013.** 013's mechanism is an ancestor scroll consuming the pointer DOWN
before `ArticleCard`'s handler adopts it; a `Button` is not a `pointerInput` gesture and does not use
`awaitFirstDown`. **Do not start from 013's diagnosis** — that assumption cost 013 two of its four
passes, and the evidence now says the button is not failing at all.

*Owner decision 2026-08-27; mechanism corrected by 006's walkthrough, 2026-08-31.*

*Evidence:* `specs/014-android-undo-offer-surfaces/evidence.md`.

### ~~015 — A swipe immediately after Undo is attributed to the outgoing article~~  ·  **Shipped**

**Found by 013's slice 1, out of its scope, and left deliberately unfixed.** At a very short delay after
Undo — under roughly one frame — the pointer DOWN is adopted against the article that was *leaving*, not
the one Undo restored. `awaitFirstDown RETURNED article=8c80f6f9…` fires **22 ms before** the restored
card composes, and the state document records the wrong source. Three runs at delay 0 gave `ietf_oauth`,
`ietf_oauth`, `science_aaas` — a race, not a constant.

The reader sees a card, swipes it, and trains a preference for a different article.

**This is a publish-ordering defect, not a consumption one**, so 013's `requireUnconsumed` fix neither
addresses it nor makes it worse — verified unchanged in 013's walkthrough. It is outside 013's `spec.md`
§4 scenarios, and 013's slice-2 brief explicitly barred the implementer from absorbing it.

It is also **why 013 spent a pass chasing a mystery that did not exist**: scored by "did a weight move",
these runs counted as passes and made the failure window look like a band open on both sides. Any item
that touches this ground must score by **which** article moved.

**Designed 2026-08-31.** The fix is an identity guard in `AppViewModel` — a Discover-card action whose
article is not the published Discover head is refused — so it lands in neither `ArticleCard.kt` nor the
gesture, and `waves/wave-d.md`'s open `?` cell closes with 015 ∥ 012 intact
(`015/design.md` D1, D6).

*Raised by `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md` §2, 2026-08-28.*

*Evidence:* `specs/015-android-undo-swipe-attribution/evidence.md`.

### ~~016 — Widen what is reversible~~  ·  **Shipped**

**Re-scoped 2026-08-31 on the reversibility line** (was: "Undo in Read Later and History"). The number and
its citations still resolve.

Widened `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` to cover `MARK_READ`,
`MARK_UNREAD` and `REMOVE`, so Read Later's *Mark read* and *Remove*, History's *Mark unread* and
Discover's *Mark read* all raise an undo offer. Authorized by **Amendment 8**, shared with 014.
289 → 297 tests.

Two gates had to close and both did. The action set was the first; the second — the offer being requested
at one call site only — had already been closed by item 014, which deleted the `undoable` parameter, so no
call-site audit was needed. `UndoToast` was already hosted globally, so there was no per-pane affordance to
build; `ui/screens/readlater/**` and `ui/screens/history/**` were never touched.

**The arithmetic was settled on paper before dispatch and survived implementation unquestioned**
(`016/spec.md` §2). `REMOVE` moves no weight in either direction (`contracts.md` §24) and its previous
record is always `SAVED`. `MARK_READ`'s undo reverses the Read signal iff the forward transition applied it.
**Undoing `MARK_UNREAD` re-applies it** — the double negative was real, `UndoRecord.preferenceReversal`
could only say *reverse event E*, and widening the set alone would have restored a record claiming
`signalsApplied.read = true` with the weight still subtracted, silently. `design.md` D1 added
`preferenceReapplication` with an `init` require that the two fields are never both set. It is the only
place in V1 where reversing an action applies a signal.

**The cost landed in the slice plan, not the specification.** The original three-slice cut was
unimplementable in both directions and was re-cut mid-item into two, on owner approval — sinks first, then
source. `waves/wave-d-note.md` §§2–4 has the reasoning; it is what drove the `execution-model.md` §2.1
amendment.

*Evidence:* `specs/016-android-reversible-actions/evidence.md`.

### ~~012 — Discover header below the card~~  ·  **Shipped**

Move Discover's **operational** header below the article card: Refresh, content age, the failed-refresh
disclosure, the available count, and the category selector. The masthead — eyebrow, title, purpose copy —
stays on top.

Requested by the owner on 2026-08-26 after testing wave B's build, and framed precisely: the card should
be the first thing in the viewport, not the thing you scroll to.

**This needs a specification amendment and therefore its own design pass.** `06-ui-ux.md:570` says
*"Discover begins with an editorial header area"*, which is an ordering rule. §21 then lists the category
selector and the available-article context among things the header *may* include, so moving those is a
narrow change rather than a rewrite — but it is still a change to `docs/v1/**`, which no feature
workstream may make silently (`AGENTS.md`; `docs/v1/README.md` §14).

**It largely subsumes item 008's D12.** That fix scrolls the incoming card into view after a swipe, and
currently clamps at the content maximum rather than placing the card at the top, because Discover's
content is shorter than the scroll that would require. With the operational block below the card there is
little left for it to correct.

*Raised by the owner, 2026-08-26. Not a deferral of any shipped item.*

*Evidence:* `specs/012-android-discover-card-first/evidence.md`.

### ~~017–021 — Material 3 Expressive redesign~~  ·  *wave E*  ·  **Shipped**

The Android client redesigned onto **Material 3 Expressive**: new palette, Playfair Display for editorial
type against Roboto Flex for functional, 24dp card radii, pill chips, an expressive bottom bar, and
directional motion between destinations. Design sources are vendored at
`specs/design/m3-expressive-DESIGN.md` and `specs/design/m3-expressive-PRD.md` so they cannot drift.

**The information architecture does not change**, and **no behaviour change is authorized** — not a
transition, not a count, not a ranking, not an undo path.

| # | Item | Blocks / blocked by |
|---|---|---|
| 017 | Design tokens and theme, including a dark scheme | blocks all of 018–021 |
| 018 | Shared components — app bar, bottom bar, chips, buttons | after 017; blocks 019 and 020 |
| 019 | Discover — deck card, truncation rules | after 018; concurrent with 020 |
| 020 | Read Later and History — Queue Row, StatBand, empty state | after 018; concurrent with 019 |
| 021 | Motion — directional tab slide, modal sheet reveal | last; needs 018's nav bar final |

**Imagery is out of scope, by owner decision of 2026-08-31.** The design sources call for a 16:9 deck-card
image slot and an 80dp Queue Row thumbnail. **`ArticleDataset v1` carries no image field of any kind** —
the Article contract (`contracts.md` §5–6) has no image, thumbnail, media or enclosure, and Android
consumes that contract read-only. Adding imagery is a frozen-contract change — new field, `schemaVersion`
bump, pipeline extraction, validator and web-runtime updates, and a hotlinking decision — and would be
its own wave ahead of E. This wave ships type, colour, shape and motion, which is where the editorial
character lives. **Design every component so imagery can be added later without a re-layout.**

**Two gaps the design sources leave, both owner checkpoints:** the palette is **light-only** while item
010 shipped theming and Settings offers Light/Dark/System, so 017 must derive a dark scheme or regress a
shipped feature; and the two type families should be **bundled as `res/font/` assets**, which need no
Gradle dependency, rather than `androidx.compose.ui.text.googlefonts`, which is one and needs approval.

**One amendment for the whole wave, written before 017 is designed.** This rewrites `06-ui-ux.md` — closer
in kind to Amendment 6 than to the narrow amendments 012 and 016 need. Five items each amending the same
document on five branches is a merge nobody should be asked to review.

**All five shipped.** 297 → **391** JVM tests across the wave, and the instrumented suite grew 4 → **20**.
Both checkpoints held: the palette is ten seeds derived in Oklch with the dark scheme derived from the same
seeds (`06-ui-ux.md` §77.4/§77.5), and both type families ship as bundled `res/font/` assets with no Gradle
dependency. **No behaviour changed** — wave D's undo tests passed unedited through all five items.

**The wave left three things behind, and they are wave E's close, not new items:** `waves/wave-e-note.md`,
the thirteen legacy token names, and ~160 `dp` literals still sitting in the components that 017 gave a
scale to move to. See the Queued preamble above and the Debt section below.

*Raised by the owner, 2026-08-31. Brief: `waves/wave-e.md`.*
*Evidence:* `specs/017-…/evidence.md` through `specs/021-android-m3-motion/evidence.md`.

### ~~022 — The appearance switch changes colour, not the screen~~  ·  **Shipped**

Choosing Light, Dark or System made the whole screen flash white or black. In the owner's words, reporting
it on 2026-09-20: *"the screen flickers then changes color scheme. The UX feels like an unfinished
product."*

**The cause was a manifest omission, not a theming bug.** `MainActivity` declared no
`android:configChanges`, so `UiModeManager.setApplicationNightMode(...)` recreated the Activity and the
flash was that recreation. Nothing in `ui/theme/Theme.kt` was wrong.

**Item 010's design.md D5 predicted this exactly and wrote down the remedy**, declining it on scope rather
than on merit — *"If D2's fallback is ever taken, this is the next thing to try, and the reasoning above is
why it is written down."* This item took D5's own recommendation and paid its stated cost. It authored
**Amendment 10**, which specifies the transition §79 did not cover: a 300 ms cross-fade of the resolved
colour scheme on M3 Standard easing, colour only, immediate under reduced motion.

385 → 391 JVM tests, 17 → 20 instrumented.

**It also left an open question, recorded rather than written off:** the hosted instrumented job failed
twice on a head whose app code was byte-identical to a passing one. The case that it is environmental —
emulator boot warnings, `adb` retries, seven prior consecutive successes — is strong but not closed, and
both failures belong to the same workflow run, so they are not two independent samples. It belongs in
verification debt if it recurs.

*Found by the owner's wave-E walkthrough, 2026-09-20.*
*Evidence:* `specs/022-android-appearance-switch-motion/evidence.md`.

### 023 — The card leaves, and the next one arrives  ·  **Designed, next**

Reported 2026-09-20 in the same walkthrough: a swiped card *"moves horizontally and when it reaches the
border suddenly disappears and a new card appears in the center of Discovery, without any transition."*

**The first diagnosis was wrong and is recorded so it is not re-derived.** The exit *distance* is not the
fault — `js/ui/swipe.js:108`'s `Math.max(window.innerWidth * 0.82, 620)` and item 008's Android port agree
closely in viewport-relative terms. Three other things are:

1. **The Android exit runs the browser's curve.** `06-ui-ux.md` §80 records that §44 was split on curve only
   for the second edition; §44.2 requires M3 Emphasized easing on Android, and `ArticleCard.kt:103-110` uses
   §44.1's `CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)` at 280 ms. **A compliance gap needing no amendment** —
   item 008 shipped before the second edition and wave E's collision matrix gave motion to 021, whose scope
   was destinations and the Settings sheet. The card fell between the two.
2. **The card never fades.** The browser animates to `opacity: 0` alongside translate and rotate; Android's
   `graphicsLayer` sets `translationX` and `rotationZ` only. That is most of *"suddenly disappears"*.
3. **Nothing specifies the entrance, on either surface.** §43 step 3 says only *"the next eligible card
   appears"*. Authoring it is new motion where the specification is silent, which `AGENTS.md` forbids
   filling in from an implementation — so this item authors **Amendment 11**. The owner chose a
   rise-and-fade in place, with no lateral movement, on 2026-09-20.

**This is the fourth item in a row on the swipe surface**, after 008, 013 and 015, and that governs the
design. Two instrumented guards exist because of those items — `ArticleCardGestureTest` and
`ArticleCardScrollGestureTest` — and both must survive untouched. The settled behaviour that the card
accepts a swipe from the first frame it is on screen is load-bearing: §79.5 states explicitly that the
entrance does not gate input, including while the card is still transparent.

**Rebased onto `main` at `22ef12d` on 2026-09-22**, resolving the two expected conflicts in `docs/v1/**`
additively — Amendment 10 and Amendment 11 both stand, and §79.4 and §79.5 both stand. Its code citations
were re-verified against post-022 `main` and still resolve exactly; 022 never touched `ArticleCard.kt`.

*Found by the owner's wave-E walkthrough, 2026-09-20. Branch: `feat/023-android-card-swipe-motion`.*

---

## Parked

Deliberate non-goals, recorded so they are not rediscovered as oversights.

- **Background and periodic refresh.** No `WorkManager`, `JobScheduler`, alarms, or push. The app fetches
  on cold start and on request, and never while closed. (*004 §3*)
- ~~**Instrumented tests in CI.**~~ **Un-parked and shipped, PR #32, 2026-09-01.** `android.yml` now runs
  `connectedDebugAndroidTest` on a `reactivecircus/android-emulator-runner` emulator in a second job, with
  KVM enabled and test reports uploaded; the first job still runs the JVM suite, `assembleDebug` and
  `assembleDebugAndroidTest`. 002 slice 4's decision stood for eleven items and was reversed on the
  exposure 013 documented. Kept here, struck through, because it was a recorded non-goal and reversing one
  is worth seeing.
- **A second dataset endpoint.** One compile-time HTTPS URL: no user-editable address, no environment
  switching, no mirror, no publisher fetching (`08-security-dependencies.md` §52). (*004 §3*)
- **Delta or partial dataset updates.** Whole dataset or nothing. (*004 §3*)

---

## Debt

Not items. Things a future item should absorb when it touches the same ground.

- **`DiscoverScreen` carries three scroll effects, and one of them was implicated in a real defect.**
  The reset on category and state-class change, D11's scroll-to-end when an article becomes opened, and
  D12's scroll-the-card-into-view when the deck advances. **This is no longer a tidiness note.** Item 013
  proved that D12's `animateScrollTo` — roughly 380 ms of ancestor scroll after every head-article change,
  including the Undo restore — consumes the pointer DOWN in the Initial pass, which silently ate every
  swipe that landed inside it. 013 fixed it at the card (`requireUnconsumed = false`) rather than by
  touching the scroll, deliberately: the scroll is behaving correctly and it is item 012's ground.

  **Item 012 should know this before it moves the header.** 012 largely subsumes D12 — with the
  operational block below the card there is little left for it to correct — so the reconciliation is
  likelier to be a *deletion* than a merge. If D12 goes, the window 013 fixed stops occurring at all, and
  013's fix becomes belt-and-braces rather than the only thing holding. Do not read that as licence to
  revert it. (*008 D11/D12; `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md`*)
- **The thirteen legacy token names are still the first thirteen fields of `ui/theme/Tokens.kt`.** Item 017
  kept them alive deliberately — 17 files held 205 call sites at the time — and scoped that to wave E's
  duration. For as long as they remain, two names exist for several colours. **This is wave-close work that
  came due**, not open-ended debt; it is listed here so it survives the close if the close slips.
  (*017 `spec.md` §6; 021 `evidence.md`*)
- **~160 `dp` literals remain in the components.** Item 017 gave them a scale to move to and deliberately
  did not move them. Absorb them per file, when something next edits that file. (*017 `spec.md` §6*)
- **A release-signed APK on the emulator makes every later `connectedDebugAndroidTest` fail** with
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — debug and release carry different certificates. It cost one
  implementer session a wasted run during 022. **Uninstall the package after any release-build
  walkthrough.** (*022 `evidence.md`*)
- **The recovery notice still says *"Reset local data in Settings to recover."*** Import is now also a
  recovery path and the copy does not say so. Left alone deliberately: changing it means authoring copy
  the specification does not provide. (*009 §Outstanding*)
- **`SettingsSheet`'s body sits one indent level shallower than its nesting** after the status message was
  pinned outside the scrollable column. Cosmetic, and no formatter gate exists to catch it; fix it when
  something next edits that file. (*009 s3 walkthrough fix*)

- **`AppViewModel.adoptDataset()` reads back its own published UI state** to apply D8
  (`uiState.value.discover as? DiscoverUiState.Card`). Correct and covered — the D8 test fails if the
  cast stops matching — but it is inverted data flow coupling a domain decision to a screen-level type.
  Restructure if a fourth writer of that state appears. (*004 §Outstanding*)
- **`DiscoverUiState.Card.isOpened` tests status only**, where the browser also requires
  `openedAt !== null` (`js/app.js:118-122`). Equivalent today. Tighten it when a record can arrive from a
  path other than a transition. (*002 slice 2, observation 7*)
- **`DatasetPhase.Error` carries no error code**, so the validator's `UNSUPPORTED_SCHEMA`-versus-malformed
  distinction cannot reach the UI. Matches the browser, which renders one panel for all four codes and is
  forbidden from leaking payload text. (*002 slice 2, observation 8*)

---

## Verification debt

**Wave E was walked through on 2026-09-20, on a signed release build, and 022 was walked through after
it.** That pass is what produced items 022 and 023; its record is in
`specs/022-android-appearance-switch-motion/evidence.md`. Wave E's five items have no *individual*
walkthroughs — the wave was walked as a whole, which is what a redesign wave admits.

Owner walkthroughs — `spec.md` §5 in each item — performed for **003**, **004**, wave A's **010**, and
wave B's **008** and **009**, all driven over `adb` by the orchestrator and recorded in each item's
`evidence.md`; open for **001** and **002**. Item **007** has no walkthrough of its own by design — its
surface was unreachable until 008 landed the trigger, so **008's walkthrough is Undo's**, which closes
that gap. **011** has none by design; its validator scenarios cannot occur in a dataset the pipeline
emits.

**002's debt is partly retired.** Its four unobserved checks were Discover's Loading and Error states,
History's Yesterday and Earlier groups, three-tag row truncation, and a positive known-reading-time
aggregate. During wave B's close the emulator lost network mid-session, which surfaced **Discover's
Loading state** (*"Gathering a thoughtful queue…"*) and **its Error state** (*"Discover is unavailable
right now"*, with the retry control correctly disabled while a refresh was in flight) on the device for
the first time. The other three remain open.

001's tooling blocker is **partly cleared**: a Python 3.14.5 venv built from `requirements-dev.txt` runs
`python -m pytest` (144 passed) and `python -m pipeline.main --validate-config` cleanly, so the
pinned-3.13 concern turns out not to block the suite. What remains missing for 001 is
`data/articles.json` locally.

**Open from wave B, recorded rather than written off:**

- **A real Storage Access Framework round trip on hardware for 009** — export to Drive or Files, reboot,
  import back. Emulator provider behaviour differs from a real one, and that difference is the whole
  point of the check. It is an owner checkpoint in `waves/wave-b.md`.
- **A TalkBack gesture pass on both items.** The accessibility *tree* was inspected and is correct on both
  surfaces; TalkBack navigation itself was not driven.
- **008's mid-drag cue frame** was never photographed. The cue is verified by code and by the
  committed-swipe path, not mid-gesture.
- **009's walkthrough was performed on the pre-rebase build.** The merged build was not re-walked — the
  emulator had lost network. The rebase added one production line, covered by a unit test.
- **The owner's judgement on whether the swipe motion feels right** (`06-ui-ux.md` §44: tactile, quiet,
  controlled). Reported as "smooth and nice" during testing, but that was before the landing defects were
  fixed, so it is worth one more pass. **Still open after 013**, and now more clearly worth doing: 013
  changed when a gesture is *adopted*, not how it animates, but it is the third item in a row to touch
  the swipe surface. **Item 023 is the first item that changes how it animates**, and answering this is
  part of its walkthrough rather than a separate pass.

**Added by 013, 2026-08-28:**

- ~~**The instrumented suite is now three classes and four tests, and still out of CI.**~~ **Closed
  2026-09-01 by PR #32.** The exposure this entry described — a local `connectedDebugAndroidTest` run being
  the only thing that exercised any guard — is gone; `android.yml` runs the suite on an emulator. The suite
  has since grown 4 → **20** tests. The original reasoning is kept above, struck through, because it is
  the argument that eventually won.

**Added by wave E's close, 2026-09-22:**

- **The hosted instrumented job failed twice on a head whose app code was byte-identical to a passing
  one**, during 022. The environmental case is strong — emulator boot warnings, three `adb` exit-code-1
  retries, seven prior consecutive successes on `android.yml` — but not closed, and both failures belong to
  the same workflow run, so they are **not two independent samples**. Removing the theme animation was not
  shown to fix anything and was not attempted. **Recorded as an open question. If it recurs, it becomes an
  item.** (*022 `evidence.md`*)
- **The three wave-E screens have no individual walkthrough record**, only the whole-wave pass of
  2026-09-20 that found 022 and 023. Once 023 lands, the swipe surface will have changed for the fifth
  time and a Discover-specific pass is worth one sitting.
- **Discover's triage controls lost their visible text labels in item 019, and that is an open owner
  decision, not a finding.** *"Not interested"* and *"Save for later"* now travel only as
  `accessibleName` content descriptions, so a **sighted** reader sees bare `←` and `→`. §76.5 authorises
  icon-only controls carrying accessible names and those names are present and asserted; §35's *"must not
  replace the labelled semantic understanding of the action"* reads against it. It is also **plausibly
  load-bearing for the fold fix** — the labels cost vertical space at exactly the width that was tight, and
  *whether the rail would still fit at 360 dp with them restored is unmeasured.* 019 raised it for the
  owner rather than deciding it at review. (*019 `evidence.md` §7*)

- **A residual Undo-window measurement, recorded as a measurement and not as intended behaviour.** After
  013's fix every delay from 0.05 s to 1.2 s commits against the restored article. At a nominal delay of
  **0** the swipe still lands on the *outgoing* article — that is item **015**, a real defect with its own
  entry, not a tolerance. Nothing here refuses input at any delay (`013 design.md` D9, D11); there is no
  window in which the card declines a touch.
