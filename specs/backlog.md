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

Last reviewed: 2026-09-29, after **024 shipped** and **`definition-of-done.md` was adopted**. The work left
is the closed list in **Road to done** below, and nothing else.

**On PR numbers.** The repository moved from `~/Documents/VS Code/` to `~/Documents/Repos/` and its remote
changed to `irodriguez-io` on 2026-09-18, which restarted PR numbering at #1. PRs #30–#34 and PRs #1–#7
therefore both exist and are not out of order — anything numbered below #8 is later than anything numbered
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
| 023 | The card leaves, and the next one arrives | Android | PR #6, 2026-09-23 |
| 024 | The next card arrives without a pause | Android | PR #9, 2026-09-29 |
| 025 | The instrumented suite does not depend on window focus | Android | PR #11, 2026-09-30 |
| 026 | One name for each colour — four duplicate token names retired | Android | PR #13, 2026-10-01 |

Each has `spec.md`, `design.md`, `slices.md`, and `evidence.md` under `specs/<n>-<slug>/`.

---

## Road to done

**The closed list.** `definition-of-done.md` §2 says when the project is done; this is everything that stands
between here and there. **Nothing joins it except a severity-blocking defect** — one that breaks a rule
`docs/v1/**` already states, or loses the reader's data — **or an item the owner adds by name** (§3). Everything
else goes to **After done**.

Ordered. Numbers are allocated at design time, so only the next one is named.

**Done §2.2 is met** at `8602be0`: clean merges 1–3 are `5c1ba62` (025), `2397ec4` (026) and `8602be0`
(027), each green on `android.yml` and `test.yml` at attempt 1. Docs-only merges run no Android gate and do
not count.

| # | Work | Kind | Why it is on the list |
|---|---|---|---|
| 1 | **Shipped — PR #11, 2026-09-30.** ~~**025 — make the hosted instrumented job deterministic.** `DestinationTransitionInstrumentedTest.reducedMotionComposesDestinationAndBackResultImmediately` fails with `RootViewWithoutFocusException` on code that passes unchanged. Seen during 022 (twice, one run) and again on `bf34fd7`, whose tree is identical to the passing PR head.~~ Cause: `Espresso.pressBack()`, the suite's only focus-dependent call; see `025/evidence.md`. Done §2.2 now counts merges from here. | item | Done §2.2 needs three unrerun green merges. A gate that has to be re-run cannot prove it. |
| 2 | **Shipped — PR #12, 2026-09-30.** ~~**Close wave E, part 1: write `waves/wave-e-note.md`.** Every other wave has one; E's has never existed in git history.~~ | docs | Wave-close work that came due on 2026-09-19. |
| 3 | **Shipped — PR #13, 2026-10-01.** ~~**Close wave E, part 2: retire the 13 legacy token names** — still the first fields of `ui/theme/Tokens.kt` (`:15-27`).~~ Item 026. **Four retired, not thirteen:** `accent`, `accentSoft`, `quietInk`, `strongBorder`, the only names that duplicated another colour (`017/design.md` D1). The other nine have no twin and D1 marks them *"never"*; owner's decision, 2026-09-30. | item | Item 017 scoped their life to the wave; it ended. Two names for one colour is how a theme regresses. |
| 4 | **Shipped — PR #14, 2026-10-01.** ~~**Stop destination transitions showing both tabs' text at once.** Moving between Read Later, Discover and History shows the outgoing and incoming labels together. **Not a defect under the freeze test** — §79.1 specifies the outgoing destination's *"fade to 0.8 opacity"* while the incoming one slides in — so the fix **needs an amendment to §79.1** before it can be built. Item 021's ground: `AnimatedContent` at `ui/IntentionalReadingApp.kt:280-310`.~~ Item 027, Amendment 13: **the incoming destination is opaque and covers the outgoing one**; §79.1's values unchanged (owner's choice, 2026-10-01). Cause: no destination drew a background (`027/spec.md` §1.2). | item + amendment | **Added by the owner by name, 2026-09-29** (`definition-of-done.md` §3). Found by 023's walkthrough. |
| 5 | **Shipped — PR #15, 2026-10-01.** Item 028, Amendment 14: restored as short inline labels. ~~**Owner decision — 019's triage labels:** stay icon-only, or restore the visible text? §76.5 allows icon-only; §35 reads against it. If *restore*, it becomes an item, and whether the rail still fits at 360 dp must be measured.~~ One row, `← Skip` · `Read article ↗` · `Save →`; borderless, because full names or outlines do not fit at 360 dp (`028/spec.md` §1.3). Dismiss's accessible name becomes *"Skip, not interested"* so it contains the visible word (owner's choice). | decision → item + amendment | Deferred to wave close since 019. Done §2.5. |
| 6 | **Decided 2026-10-01: not carried.** ~~**Owner decision — should the exit carry the swipe's release velocity?** Reachable, but §44.2 fixes the curve and duration, so building it needs an amendment. **Default if undecided: After done.**~~ The owner's words: the swipe card *"doesn't need to carry the swipe's speed"*. The exit stays §44.2's fixed-duration tween. There is no item and no amendment, and it is not deferred to After done. | decision | Done §2.5. |
| 7 | **Done 2026-10-01.** ~~**The second-edition sweep, bounded to one sitting.** §80 records sections *"split"* for the second edition; check whether any besides §44 was left unimplemented on Android. Findings go through the freeze test.~~ Five parallel read-only audits covered every split or widened section, and Claude checked each finding against the code; the card shadow was also checked on the emulator. **Seven breaks** joined as rows 7a and 7b. The owner's rulings: §65's generic refusal message *counts as telling why*; §38's current focus indication is *accepted as it stands, regardless of the section's wording*; §27's five tags stay at narrow widths (production on 2026-10-02 had 205 articles, none with more than 3 tags); §16.2's sheet elevation *means a drop shadow*, so it is a break and joins 7b, with a pre-approved drop if it cannot be made to render. Everything else is in After done. | audit | Raised by 023. The card's curve was a real compliance gap nobody owned; there were four more. |
| 7a | **Accessibility: headings, and readable disabled text.** (1) §73 requires *"semantic landmarks and headings"*, but no Android screen marks a heading; there is no `heading()` anywhere in `src/main`. (2) §37 requires disabled text the reader is meant to read to meet §73's contrast. *"Refreshing…"* (`DiscoverHeader.kt:71-78`) and *"Try again"* (`DiscoverScreen.kt:215-222`) use Material's default disabled colour, about 2.4:1 against 4.5:1. That figure is computed from the M3 default and has not been measured on screen. | item | Road 7. No item ever claimed either rule. |
| 7b | **Presentation and motion: the toast, two shadows, the content cap, one test.** (1) §45.2: the toast fades in with a slight slide; today it appears instantly (`IntentionalReadingApp.kt:346-351`). §48 requires a reduced-motion branch and a test for it. (2) §16.2/§78.4: the card's `tertiary` shadow at about 10% renders at about 2% because the colour's alpha is multiplied again, and it shows no visible shadow on the emulator. The only test checks source text (`ArticleCardTest.kt:240-251`). (3) §16.2: the Settings sheet has no drop shadow (*"8dp+ elevation"*; owner: a drop shadow). Material's bottom sheet has no shadow setting and draws in its own window. **Pre-approved drop (owner, 2026-10-01): if a test shows the shadow does not render, this part is dropped without coming back to the owner, and it is recorded in After done with that evidence.** (4) §14.2: the 680 dp content cap and 24 dp tablet margin are defined (`Spacing.kt:26`) but never applied. (5) §48: the Discover auto-scroll (`DiscoverScreen.kt:82-107`) has no reduced-motion test. | item | Road 7. (1) belonged to nobody in wave E, the same way §44 did. (2)–(4) were defined and never applied. |
| 8 | **Final acceptance pass**, on a signed release build: §73 at all five widths, §72, §75, §76, a **TalkBack** pass over Discover, Read Later, History, Settings and import/export (closes wave B's debt), §81, 002's three unobserved History checks, and 001's browser walkthrough (fetch `data/articles.json` from production first). SAF round trip on real hardware if a device is available; if not, recorded as a known limitation. | acceptance | Done §2.3, §2.4, §2.6. The last thing on the list. |

**Guardrails for 7a and 7b** (owner's concern, 2026-10-01: fixing these must not create new breaks):
- **Each fix ships a test of the rendered result, not the source text.** 019's shadow test read the source and passed while no shadow was visible.
- **Every animation a fix adds ships its §48 reduced-motion branch and test in the same slice.** This is the most likely way a fix creates a new break.
- **The 680 dp cap is verified at 360 dp, to show nothing changed there, as well as at 600 and 768 dp.**
- **Each item names the files it may touch and the files it may not, and its diff check enforces both.**
- **Road 7 was the last audit.** Walkthrough findings from 7a, 7b and row 8 go through the freeze test, and anything that is not a stated-rule break goes to After done.

**Superseded by this list:** the Reminders item *"Watch for the byte-identical CI instrumented failure"* is now
row 1.

## After done

Recorded so they are not rediscovered as oversights; **not worked** until the owner opens a new version.

- **Landscape and viewports above ~615 dp keep the old exit-to-entrance timing** (024 `spec.md` §4).
- **R8 and a baseline profile**, the untried levers against first-use jank.
- **Road 7's cosmetic and ambiguous findings** (2026-10-01): §37.2's pressed treatment on chips, outlined buttons, row actions and Settings buttons; §36's Reset *"active: ink fill"*; §46.2's control-transition values (instant pressed state; library controls on M3's default springs); §76.6's StatBand showing a truncated *"Unavail…"* (§54 allows *"unavailable"*); §64.2's *"surface-card toggles"*, invisible in light because `card` equals `surface`; off-scale 12 dp radii (`ResetConfirmation`, `ImportConfirmation`, `UndoToast`, `LocalStateMessages`); spacing literals outside `Spacing.kt`; the `28.sp` close glyph; `#FCFEFF` in `ic_launcher_foreground.xml`, a retired seed; raw `border` used as an outline in four pre-wave-E places; §27's first-3-tags rule at narrow widths (owner: accepted at 5); and unasserted §73.1 cases (row action on `container`, 3.91:1 light, passes but is untested).
- **The Amendment Record at the end of `06-ui-ux.md` stops at Amendment 9**; 10–14 are recorded only in `docs/v1/README.md:136-144`. Docs only.
- **Everything in Debt below**, absorbed only when something next edits the same file.
- Everything already in **Parked**.

---

## Queued — history

*Kept as the record of how each item was scoped. The live queue is Road to done above.*

**One item, and a wave close.** Wave E's five items are implemented and merged, and so are **022** and
**023**, the two defects the owner's wave-E walkthrough found. The queue holds **024** — the defect 023's
own walkthrough found — and the wave-close bookkeeping 021's evidence itemised.

**Wave E's close is outstanding, and it was three distinct pieces of work.** `waves/wave-e-note.md` is
written (PR #12, 2026-09-30); this document's wave row is the only record that the wave finished, and **the thirteen legacy
token names item 017 kept alive for the wave's duration have not been retired** — they are still the first
thirteen fields of `ui/theme/Tokens.kt`. That debt was scoped to the wave and came due at its close.
*(021 `evidence.md`, §Wave-close work that outlives this item.)*

**The walkthrough itself was performed**, on a signed release build, on 2026-09-20. It found two defects,
both motion and both on Android: the appearance flash (**022**, shipped) and the card swipe exit and
entrance (**023**, shipped). That is the fourth wave running in which the owner using the app found
the defects no gate did — see `waves/wave-c-note.md` and `waves/wave-d-note.md` for the same headline.
**023's own walkthrough, on 2026-09-22, repeated it:** every mechanical check passed and the owner still
found **024**.

**Item 012's 360 dp finding carried into 019 and still stands after it.** The Discover card's full action
rail cannot be visible at 360 dp under any header arrangement, because card height is unbounded in the
article title. The measurements and the limitation are in
`specs/012-android-discover-card-first/spec.md` §1.4. The redesign did not change that and was not expected
to.

**All five waves are done.** `waves/wave-b-note.md` records what wave B cost and `waves/wave-c-note.md`
what wave C cost; `waves/wave-e-note.md` records what wave E cost (PR #12). Their shared headline
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
`ui/components/ArticleCard.kt`, `ui/gesture/SwipeGesture.kt` and `ui/screens/discover/**`. **Item 024 also
runs outside the waves**, and nothing else is in flight for it to collide with. Its surface turned out
narrower than 023's — `ArticleCard.kt` and `SwipeGesture.kt` only.

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

### ~~023 — The card leaves, and the next one arrives~~  ·  **Shipped**

Reported 2026-09-20 in the same walkthrough: a swiped card *"moves horizontally and when it reaches the
border suddenly disappears and a new card appears in the center of Discovery, without any transition."*

**The first diagnosis was wrong and is recorded so it is not re-derived.** The exit *distance* was not the
fault — `js/ui/swipe.js:108`'s `Math.max(window.innerWidth * 0.82, 620)` and item 008's Android port agree
closely in viewport-relative terms. Three other things were, and this item fixed all three:

1. **The Android exit ran the browser's curve.** §44.2 requires M3 Emphasized easing on Android; item 008
   shipped §44.1's curve at 280 ms, before the second edition, and wave E's collision matrix gave motion to
   021, so the card fell between the two. It now runs Emphasized at 300 ms.
2. **The card never faded.** It now fades as it leaves, alongside translate and rotate.
3. **Nothing specified the entrance.** This item authored **Amendment 11** (§79.5): the next card rises and
   fades in place, with no lateral movement, as the owner chose on 2026-09-20. The entrance does not gate
   input — a card accepts a swipe from its first frame, including while still transparent.

Two slices. 391 → **400** JVM tests, 20 → **23** instrumented. `ArticleCardGestureTest` and
`ArticleCardScrollGestureTest`, the guards items 013 and 015 left, survived untouched.

**Its walkthrough, 2026-09-22 on a signed release build, did not pass.** Every mechanical step held; the
character judgement did not, and the reason is **024**. **023 did not create that gap — it made it
legible:** before it, the replacement simply materialised, so there was nothing to wait for.

*Found by the owner's wave-E walkthrough, 2026-09-20.*
*Evidence:* `specs/023-android-card-swipe-motion/evidence.md` — §4 is the walkthrough.

### ~~024 — The next card arrives without a pause~~  ·  **Shipped**

Found by 023's walkthrough, 2026-09-22. **Roughly half a second of nothing sits between the card leaving and
the replacement arriving** — in the owner's words, *"enough for my brain to doubt whether a new card will
arrive."* **Undo is the benchmark**: it reads as immediate, and the owner called it optimal.

**The gap has two parts, and the first is most of it.** `ArticleCard.kt:147-155` requests the commit only
after the exit's full `300ms` curve. That curve is Material 3 Emphasized, which front-loads its travel, so on a
phone the card is off-screen **55–60 ms** into the exit and the remaining ~245 ms draws nothing. Then comes
the save — an `fsync`'d write on `Dispatchers.IO`, then `publish()`'s re-rank — which undo pays too.

**Corrected at design, 2026-09-29.** This entry previously said the write and the re-rank ran on
`Dispatchers.Main.immediate` and that the re-rank was in `adoptPersistedState`; neither is right. It also
named the fix as starting the save alongside the exit, which removes only the second, smaller part.

**The owner chose to commit on departure:** the save starts the moment the card is no longer visible, not
when its curve ends. **Amendment 12** says so in §79.5. One slice, in `ArticleCard.kt` and `SwipeGesture.kt`
only — **narrower** than 023's, not wider; the view model and `ui/screens/discover/**` are untouched. 023's
D2 is reopened in one respect — the head changes once the card is gone rather than once its curve ends — so
items **013** and **015** are re-proved with instrumented tests. Landscape and viewports above ~615 dp keep
today's timing (`spec.md` §4).

**Shipped as PR #9 (`bf34fd7`), 2026-09-29, in one slice**, 400 → 403 JVM tests, 23 → 28 instrumented. **The owner walkthrough
passed the same day** on a signed release build: *"no pause between new card and old card"*, and against
undo, *"is the same lapse."* The design's prediction held — what remains is the save, and it reads like undo.

*Branch: `feat/024-android-card-arrival-gap`. Evidence:* `specs/024-android-card-arrival-gap/evidence.md`.

### Also found by 023's walkthrough, and not part of 024

Neither is numbered yet; numbers are allocated at design time.

- **The exit discards the gesture's release velocity.** *Declined by the owner on 2026-10-01 (Road to done row 6): not wanted, so the exit stays as it is.* It is a fixed-duration `tween`, so a slow drag
  released at the threshold snaps to full speed; the owner wants *"a smooth movement for the card
  vanishing."* Reachable with `Animatable.animateDecay` or `animateTo` with an `initialVelocity`, but
  **§44.2 fixes the exit's curve and duration, so this needs an amendment** — an owner decision before it
  is an item. The browser does the same, which is why item 008 ported it that way.
- **Destination transitions show both tabs' text at once.** Moving between Read Later, Discover and History
  renders the outgoing and incoming labels together for an instant. That is item **021**'s ground —
  `AnimatedContent` at `ui/IntentionalReadingApp.kt:280-310` — and it was in no scenario.

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
- **The M3 Emphasized easing is defined twice** — `SwipeGesture.ExitEmphasizedEasing` (023) and item 021's
  `IntentionalReadingApp.kt:271-279`. Deliberate and disclosed; absorb it when something next touches
  `IntentionalReadingApp.kt`. (*023 `evidence.md` §5*)
- **`ArticleCardTest` asserts against source-file text in many places** — eleven such assertions on `main`
  before 023, which followed the convention and extended it. It breaks on refactors that change no
  behaviour. (*023 `evidence.md` §5*)
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
  part of its walkthrough rather than a separate pass. **Answered 2026-09-22: not yet.** 023's walkthrough
  found the swipe still does not read as *controlled*, and for the first time named the cause — the
  exit→entrance gap, now item **024**. This stays open until 024's walkthrough. **024's walkthrough,
  2026-09-29, found the gap gone and nothing else wrong, and the owner closed the judgement:** *"i consider
  it passed."* **Closed**, after five items on this surface.

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
  item.** (*022 `evidence.md`*) **Two more clean passes since**, both during 023 — Android runs
  `35817066156` on `9cc69ee` and `35817630661` on `3d0cb7c`, 23 instrumented tests, no `adb` retries on
  either.
- **The three wave-E screens have no individual walkthrough record**, only the whole-wave pass of
  2026-09-20 that found 022 and 023. 023 has landed and was walked on 2026-09-22, but that pass looked at
  the swipe, not the whole screen. **024 will change the swipe surface for the sixth time**, so a
  Discover-specific pass after it is worth one sitting.
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
