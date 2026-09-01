# Wave D — Undo everywhere, and the Discover header

**Items:** 015, 012 concurrently · then 014 · then 016
**Prerequisite:** wave C merged (`5dd2753`); Amendments 7 and 8 committed on `main`
**Cut from:** `main`
**Design pass:** 2026-08-31 — see *Design pass outcome*, which corrects this brief in four places
**Status:** 015, 012 and 014 merged; **016 remains** — see *Where this wave stands*, written for a fresh session

Self-contained brief. Read `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`,
`specs/execution-model.md`, then this file. Read `specs/waves/wave-c-note.md` §5 before designing 014 or
016 — it holds the observations from the app that these two items exist to act on.

---

## What this wave is

Three of the four items are the same subject seen from three angles: **Undo does not work where a reader
expects it to.** The fourth, 012, is unrelated and rides along because it collides with nothing.

This is the first wave with no parity gap in it. Three of the items are defects the owner found by using the
app. **014 is not** — the design pass established that the behaviour it changes is specified, and the item
reverses a decision rather than repairing a gap. See *Design pass outcome* 1.

---

## Design pass outcome — 2026-08-31

**All four items were designed in one session, in implementation order, as `execution-model.md` §4.1
requires.** Four things below were wrong or unknown when this brief was written and are corrected here.
Where this section and an earlier one disagree, this section wins. Specifications:
`specs/015-android-undo-swipe-attribution/`, `specs/012-android-discover-card-first/`,
`specs/014-android-undo-offer-surfaces/`, `specs/016-android-reversible-actions/`. Amendments:
`specs/waves/wave-d-amendments.md`.

**1. 014 needs an amendment after all, and it shares 016's.** This brief and `backlog.md` both said 014 was
amendment-free because nothing about *what is reversible* changes. But Undo is scoped to the **swipe
gesture** in five places — `contracts.md` §31, `05-personalization-state.md` §36, `06-ui-ux.md` §70,
`01-product.md` §14, `09-testing-acceptance.md` §50 — and items 007 and 008 made the labelled buttons
non-undoable **deliberately**, with the rationale recorded in `007/spec.md` §1.1 and `008/design.md` D8
(*Undo recovers a mis-trigger; a press on a button labelled "Save for later" is not one*). The browser does
the same at `js/ui/discover.js:211`. So the *Save for later* button raising no offer is **specified
behaviour, not a defect**, and 014 reverses a decision rather than fixing a gap. The owner decided on
2026-08-31 to proceed with the reversal.

**One amendment covers both items — Amendment 8** — because §31, §36 and §70 each state the surface rule and
the action rule in the same sentence, and editing them twice would leave `docs/v1/**` self-contradictory in
between. It is **permissive**: it authorises the wider scope without obliging the browser to follow, so 014
and 016 stay Android-only and fire `android.yml` alone. It is written before 014's branch is cut, which
means **the wave's one amendment conversation happens before the first undo-surface item, not before the
last.**

**2. The `?` cell is resolved: 015 does not touch `ArticleCard.kt`.** The fix is an identity guard in
`AppViewModel.onArticleAction` — an action declared as coming from the Discover card is refused when its
article is not the published Discover head. It is mechanism-independent, it is assertable in
`AppViewModelTest` by article id, and it needs no change to the gesture, the deck or the header
(`015/design.md` D1, D5, D6). **015 ∥ 012 stands.** 015 does touch `IntentionalReadingApp.kt` — four lambdas
gain one argument — which 014 and 016 also edit, but they are sequential, so it is a rebase note and both
of their slice plans carry it as a checkable assumption.

**3. 012 got wider than "move the header", and 016 got narrower.** 012 must re-aim the became-opened scroll:
it targets `scrollState.maxValue` today because the card is the last thing in the column, and after the
reorder that scrolls the **Mark read** button back off screen — re-opening the wave B defect the owner found
by hand (`wave-b-note.md` §9). 012 also compacts the masthead, by owner decision: eyebrow and title stay
above the card, the purpose copy moves below it, because a full masthead still pushes the card's action rail
below the fold at 360 dp. 016, meanwhile, **does not touch `ui/screens/readlater/**` or
`ui/screens/history/**` at all** — those screens already call the actions; only the state machine, the undo
record, `raiseUndoOffer`, `UiStateMapper` and three strings change.

**4. 016's arithmetic is settled on paper, and one part of it needs a new field.** `REMOVE` has **no**
preference arithmetic in either direction (`contracts.md` §24; no `preferenceEvents` entry) and its previous
record is always `SAVED` (`allowedFrom[REMOVE] = {SAVED}`), so restoring `previousRecord` is sufficient.
Undo of `MARK_READ` reverses the Read signal iff the forward transition applied it. **Undo of `MARK_UNREAD`
must *re-apply* the Read signal** — `transition`'s `MARK_UNREAD` branch reverses it and leaves
`preferenceSignalApplied` false, so `UndoRecord.preferenceReversal` cannot express what undoing it has to
do, and widening the set alone would restore a record claiming `signalsApplied.read = true` with the weight
still subtracted. Silently. That is the exact place this brief predicted the arithmetic would be got wrong.
`016/spec.md` §2 is the table; `016/design.md` D1 is the field.

---

## Where this wave stands — handoff, 2026-08-31

**Read this section first.** It is written so a fresh session can resume without this session's history, per
`execution-model.md` §7. If it turns out to be insufficient, fix it rather than reconstructing from git.

### Merged

| Item | Merge commit | PR | Push-triggered CI on the merge commit |
|---|---|---|---|
| Amendments 7 and 8 + all four designs | `6c857a61` | #19 | Test `33422480339`, Pages `33422480328` |
| **015** undo swipe attribution | `88e71b7c` | #20 | Android `33424850723`, Test `33424850747`, Pages `33424850705` |
| **012** Discover card first | `05657ed6` | #21 | Android `33442192566`, Test `33442192352`, Pages `33442192215` |
| **014** undo offer surfaces | `cc2a6084` | #22 | Android `33456164733`, Test `33456164726`, Pages `33456164705` |

`main` is at `cc2a6084`, clean, **286 tests, 0 failures**, `:app:assembleDebug` green. One worktree, no
branches outstanding, no implementer sessions running.

### What is left

1. **Item 016** — the only unshipped item. See *Dispatching 016* below.
2. **The wave-end walkthrough** (`execution-model.md` §4.5, §6), batched for 015, 014 and 016 against
   merged `main`. **012's was already driven at its slice 2 and is recorded** in
   `specs/012-android-discover-card-first/walkthrough/` — it is not repeated, because that item's only
   evidence *is* the walkthrough and deferring it would have meant merging with none.
3. **`wave-d-note.md`** per `execution-model.md` §4.6. Its content is largely written already — see
   *Lessons* below.
4. **`backlog.md`** — 015, 012 and 014 are already moved to Shipped. 016 remains under Queued.
5. **One amendment to `execution-model.md` §2** — see *Lessons*, item 1. This is a governance change and is
   deliberately left for wave close rather than made mid-wave.

### Dispatching 016

Everything it needs is written. `specs/016-android-reversible-actions/` has `spec.md` (§2 is the settled
arithmetic table), `design.md` (D1 is the `UndoRecord` change) and `slices.md` (three slices).

**Do this in order:**

```sh
cd "/Users/isidro.rodriguez/Documents/VS Code/news-agregator"
git worktree add -b feat/016-android-reversible-actions ../news-agregator-016 main
```

Then re-check **all eight of `spec.md` §7's assumptions** against `cc2a6084` before briefing slice 1.
`/feature-implementation` Step 0.4 is not a formality on this item — it is the step that would have caught
item 006's failure, and it caught 014's.

**Assumption 2 is already resolved and it is the one that changes the plan's shape.** Item 014 **deleted**
the `undoable` parameter rather than adding more `undoable = true` arguments, so:

- `ArticleStateMachine.transition` builds an undo record on `action in reversibleActions` alone;
- `AppViewModel.persistArticleTransition` raises the offer from `transition.undoRecord` unconditionally;
- therefore **016's slice 2 stays small and needs no call-site audit** — widening `reversibleActions` makes
  Read Later's *Mark read* and *Remove*, History's *Mark unread* and Discover's *Mark read* all raise the
  offer with no changes at their call sites. `slices.md`'s *If assumption 2 is false* section does **not**
  apply and can be ignored.

**The two things most likely to go wrong**, both stated in `spec.md` §2 and worth restating to the
implementer:

- **Undoing `MARK_UNREAD` must *re-apply* the Read signal, not reverse anything.** `transition`'s
  `MARK_UNREAD` branch reverses the Read signal and leaves `preferenceSignalApplied` false, so
  `UndoRecord.preferenceReversal` cannot express what undoing it has to do. Widening `reversibleActions`
  alone would restore a record claiming `signalsApplied.read = true` while the weight stays subtracted —
  **silently, with nothing in the app noticing.** `design.md` D1 adds `preferenceReapplication` for exactly
  this, with an `init` require that the two fields are never both set.
- **`REMOVE` moves no weight in either direction** (`contracts.md` §24) and its previous record is always
  `SAVED` (`allowedFrom[REMOVE] = {SAVED}`), so restoring `previousRecord` is sufficient. Do not add a
  `preferenceReversals` entry for it.

`design.md` D5 lists the assertions that change. **Treat that list the way 014's had to be treated** — as
named cases with reasons, not as a complete enumeration, and see *Lessons* item 3 for the class it is most
likely to miss.

### Lessons, for `wave-d-note.md`

Three, and the wave has not finished paying for them yet. **All three were found by an implementer
stopping rather than proceeding — none by a gate, none by reading a diff.** That is now the fourth
consecutive wave in which the most valuable findings came from somewhere other than the review gate
(`wave-b-note.md`, `wave-c-note.md` §5).

**1. `execution-model.md` §2's collision matrix has no axis for who *asserts against* a file, and that gap
has now failed two items at dispatch.** Item 006 froze an assertion its predecessor had made unfreezable.
Item 014 split a slice across a compile boundary: slice 1 removed a parameter from `AppViewModel` while
excluding `AppViewModelTest.kt` and its 28 calls, so the slice could not have built its own test sources.
Same root, different costume — *the plan was drawn by who writes a file, never by who asserts against it.*
`wave-c-note.md` §7 already named this and nothing changed. **Amend §2 at wave close: every hub-file row
needs a second axis listing the test files that assert against it, and a signature change must be assumed
to reach every one of them.**

**2. A scenario must assert what the item controls.** Item 012's first scenario required the Discover
card's full action rail to be visible at 360 dp. Card height is unbounded in the article's *title* —
`ArticleCard` sets `maxLines` on the excerpt and none on the title, and `06-ui-ux.md` §25 forbids clamping
it — so no header arrangement could deliver it. The scenario also over-reached past its own Amendment 7,
which binds ordering only, and past §71, which requires actions be **reachable**, not visible. The
implementer built the item, drove the emulator, and refused to commit. Corrected in place; the 360 dp case
is handed to wave E item 019 with its measurements in `specs/012-android-discover-card-first/spec.md` §1.4.

**3. When a design pass removes a capability, ask which *fixtures use* it — not only which assertions claim
it.** Item 014's `design.md` D5 reasoned carefully about every test that asserted the old rule and missed
`a refused Undo announces its failure and keeps the offer`, which used `SAVE` with `undoable = false` as a
way to persist *without* claiming the undo slot. That is a fixture, not an assertion, and it is invisible
to a line-number enumeration. Repaired by switching the setup action to `OPEN`; name and all six assertions
unchanged.

**Also worth banking, on the credit side.** Both implementers independently applied item 015's lesson about
the `article()` helper's shared `oauth` tag — overriding `tags` with distinct ids so that "this topic must
not move" cannot be vacuously true when another article moves the same topic. And 014's slice 2 reported
two scenarios as *already covered* rather than manufacturing duplicate tests for them.

### Owner checkpoints still outstanding

1. **The three new toast strings** in `specs/016-android-reversible-actions/spec.md` §5 — *Marked as read*,
   *Returned to Read Later*, *Removed from Read Later* — judged on screen at 016's walkthrough. `06-ui-ux.md`
   §45 labels its toast strings *"Examples"*, so no amendment is needed for new copy.
2. **A walkthrough of the widened Undo against real accumulated history, not fresh state** (checkpoint 3
   below). Every wave so far has found its most valuable defects this way and none of them by a gate.
3. **Wave sign-off** against merged `main`.

Checkpoints 1 and 2 from the original list are **settled**: Amendment 8 was approved and committed, and
012's placement rule was approved along with the §1.4 limitation.

---

## The re-cut: 014 and 016 were drawn on the wrong line

**014 and 016 have been re-scoped in place.** Their numbers, and every citation to them — including
`specs/013-android-undo-gesture-reset/spec.md` §1.8 — still resolve. What changed is the boundary
between them.

They were cut **by pane**: 014 owned Discover's card buttons, 016 owned Read Later and History. That
boundary cuts across the real one. The code has exactly two reasons an action does not offer Undo, and
they need different work:

| Reason | Actions | Fix | Amendment? |
|---|---|---|---|
| **The action is reversible; the offer is never requested.** `undoable = true` is passed at exactly one call site in the whole app — Discover's `onSwipeCommit`, `IntentionalReadingApp.kt:290`. Everything else takes the `undoable = false` default. | `SAVE`, `DISMISS` | raise the offer at the remaining call sites | **Yes** — see *Design pass outcome* 1; corrected 2026-08-31 |
| **The action is not reversible at all.** `reversibleActions = setOf(SAVE, DISMISS)`, `ArticleStateMachine.kt:254`. `transition` builds no `UndoRecord`, and `undo` returns `UNDO_UNAVAILABLE`. | `MARK_READ`, `MARK_UNREAD`, `REMOVE` | widen `reversibleActions`, then raise the offer | **Yes** — `contracts.md` §23 |

Discover's card buttons span both rows: *Not interested* and *Save for later* are the first, ***Mark
read* is the second**. So the old 014 carried a hidden dependency on the old 016's amendment, and
whichever ran second would have inherited a half-widened `reversibleActions`.

Cut on the reversibility line instead, and the dependency disappears and the amendment is written once:

- **014 — raise the offer where the reversal already exists.** Needs Amendment 8, shared with 016
  (*Design pass outcome* 1, which corrects this).
- **016 — widen what is reversible, then wire every surface.** The same amendment, one §23
  conversation, written once and before 014.

**Scope correction carried into 016:** `UndoToast` is already hosted globally in
`IntentionalReadingApp`, outside the destination `when` and gated only on `!settingsOpen`, so it renders
on Read Later and History today. **There is no per-pane affordance to build** — only an offer to raise.
016's original title overstates its UI work.

---

## Collisions and order

`execution-model.md` §2's matrix is a record of how waves A–C fell out. This is the same read for wave D:

| Hub file | 015 | 012 | 014 | 016 |
|---|---|---|---|---|
| `ui/AppViewModel.kt` | ● | | | |
| `ui/components/ArticleCard.kt` | **?** | | ● | |
| `ui/screens/discover/DiscoverScreen.kt` | | ● | | |
| `ui/IntentionalReadingApp.kt` | | | ● | ● |
| `domain/state/ArticleStateMachine.kt` | | | | ● |
| `ui/screens/readlater/**`, `ui/screens/history/**` | | | | ● |
| needs a `docs/v1/**` amendment | | ● | | ● |

**Corrected by the design pass** — see *Design pass outcome* 2 and 3:

| Hub file | 015 | 012 | 014 | 016 |
|---|---|---|---|---|
| `ui/AppViewModel.kt` | ● | | ● | ● |
| `ui/IntentionalReadingApp.kt` | ● | | ● | ● |
| `ui/components/ArticleCard.kt` | **no** | | **no** | |
| `ui/screens/discover/DiscoverScreen.kt` (+ two new files there) | | ● | | |
| `ui/components/EditorialHeader.kt` | | ● | | |
| `domain/state/ArticleStateMachine.kt` | | | ● | ● |
| `domain/state/UndoRecord.kt`, `ui/AppUiState.kt`, `ui/state/UiStateMapper.kt`, `res/values/strings.xml` | | | | ● |
| `ui/screens/readlater/**`, `ui/screens/history/**` | | | | **no** |
| needs a `docs/v1/**` amendment | | ● 7 | ● 8 | ● 8 |

**015 and 012 run concurrently.** `AppViewModel.kt` against `DiscoverScreen.kt`, nothing shared.

**014 and 016 cannot.** They share `IntentionalReadingApp.kt`'s action wiring, and 016 also moves the
`reversibleActions` set that 014's Discover work reads.

**015 leads.** It is a defect *in* the undo path, and 014 and 016 both multiply the surfaces where a
reader can reach it. Fixing the race after widening means re-verifying every new path against it.

**The one uncertainty in this plan is the `?` cell.** 015's surface is a design-pass output, not a known
quantity — it is a publish-ordering defect and the fix may land in `AppViewModel.kt` or in
`ArticleCard.kt`'s article-identity capture. **If 015's design pass puts the fix in `ArticleCard.kt`,
it collides with 014 and the concurrency claim above must be rechecked before 014 is dispatched.**
Say so in 015's design note either way, so the next session does not have to re-derive it.

### The order

1. **015 ∥ 012** — concurrent, both from `main`.
2. **014** — after 015 merges.
3. **016** — after 014 merges. **Corrected:** the amendment is shared with 014 and therefore blocks 014's
   dispatch, not just 016's design pass. It is drafted at the design pass and lands on `main` before any
   wave-D branch is cut (`specs/waves/wave-d-amendments.md`). See *Design pass outcome* 1.

---

## Designing this wave — and the wave-C trap

`execution-model.md` §4.1 says design all of the wave's items first, in one session, before any
implementation starts. Do that. Design in the **implementation order** — 015, 012, 014, 016 — so each
later design pass can read the surfaces the earlier ones declared.

**But wave C's three plan defects all came from one thing, and two of this wave's four items are exposed
to it:** item 006 was designed against a tree that did not exist yet. It assumed item 005's
`DiscoverDeck` shape without being able to see 005's *tests*, and its slice plan then froze an assertion
that 005's merge had made unfreezable. The plan was approved in plan mode and only failed at dispatch,
weeks later.

> **A design pass that depends on an unmerged item's output is a forecast, not a design.** Write it as
> one.

Which items are exposed here:

| Item | Cut against | Exposure |
|---|---|---|
| 015 | merged `main` | none |
| 012 | merged `main` | none |
| 014 | **015 unmerged** | 015's fix may land in `ArticleCard.kt`, which is 014's ground |
| 016 | **014 unmerged** | 016 rewires the same `undoable` call sites 014 will have just changed |

**Three rules for 014's and 016's slice plans specifically:**

1. **Do not freeze an existing assertion.** State which tests you expect to exist and what they should
   still prove, but never write "no existing assertion may be edited" — that is the exact sentence that
   made 006 unimplementable. If an assertion must change, the design pass says so and says why.
2. **State assumptions as assumptions, with the file and the fact each depends on.** "Assumes 015 leaves
   `ArticleCard.kt` untouched" is checkable at dispatch. A silent assumption is not.
3. **Every DoD bullet must be assertable at the layer it names.** 006 shipped a bullet demanding proof
   that "no local-state write occurs" from a pure function with no writer to observe, and got a vacuous
   assertion in return. If a bullet cannot be tested where it sits, say what enforces it instead.

`/feature-implementation` Step 0.4 re-reconciles the slice plan against anything decided since design.
For 014 and 016 that step is not a formality — it is where these forecasts get checked against the tree
that actually exists.

**Two owner checkpoints will block design inside the session**, so expect the design pass to stop and
ask rather than guess:

- **012's amendment** — the narrow `06-ui-ux.md` ordering change.
- **016's amendment** — what becomes reversible and what reversing it does to the preference weights.
  This is the long pole of the wave. It can be drafted before 016's design pass begins, and probably
  should be.

---

## 015 — A swipe immediately after Undo is attributed to the outgoing article

Found by item 013's slice 1, deliberately left unfixed, and the reason 013 spent a whole pass chasing a
mystery that did not exist.

At a very short delay after Undo — under roughly one frame — the pointer DOWN is adopted against the
article that was *leaving*, not the one Undo restored. `awaitFirstDown RETURNED article=8c80f6f9…` fires
**22 ms before** the restored card composes, and the state document records the wrong source. Three runs
at delay 0 gave `ietf_oauth`, `ietf_oauth`, `science_aaas` — **a race, not a constant.**

The reader sees a card, swipes it, and trains a preference for a different article.

**It is a publish-ordering defect, not a consumption one.** 013's `requireUnconsumed` fix neither
addresses it nor makes it worse; that was verified in 013's walkthrough and is not worth re-deriving.

**The trap, and it is the whole reason this item is dangerous:** scored by "did a weight move", these
runs count as **passes**. That is exactly what made 013's failure window look like a band open on both
sides and cost it a pass. **Score by which article moved.** Any test, any walkthrough step, any log line
that does not name the article id is not evidence here.

*Raised by `specs/013-android-undo-gesture-reset/investigation/step0-undo-window.md` §2.*

---

## 012 — Discover header below the card

Move Discover's **operational** header below the article card: Refresh, content age, the failed-refresh
disclosure, the available count, and the category selector. The masthead — eyebrow, title, purpose copy —
stays on top. The card should be the first thing in the viewport, not the thing you scroll to.

**Needs a `docs/v1/**` amendment.** `06-ui-ux.md:570` says *"Discover begins with an editorial header
area"*, which is an ordering rule. §21 then lists the category selector and the available-article context
among what the header *may* include, so this is a narrow change rather than a rewrite — but it is still
`docs/v1/**`, which no feature workstream may change silently (`AGENTS.md`; `docs/v1/README.md` §14).

**It largely subsumes item 008's D12**, the fix that scrolls the incoming card into view after a swipe.
With the operational block below the card there is little left for it to correct.

**Known and accepted at planning time:** wave E's redesign re-lays out Discover wholesale and will
rewrite the amendment this item writes. The owner chose to take the fix now rather than wait for E
(2026-08-31). Write 012's amendment so it states the *intent* — the card leads the viewport — and not
just the widget order, so E inherits a rule rather than a layout.

*Raised by the owner, 2026-08-26, after testing wave B's build.*

---

## 014 — Raise the undo offer where the reversal already exists

**Re-scoped 2026-08-31; see the re-cut section above.**

`SAVE` and `DISMISS` are already in `reversibleActions`. Every surface that performs them **except**
Discover's swipe fails to ask for the offer, because `undoable = true` is passed at exactly one call
site.

**Confirmed in the app, on both the 006 and pre-006 builds** (`006/evidence.md` §5 step 5): the *Save for
later* button **commits the save** — the record is written, Read Later increments — and simply never
offers the reversal. Screencaps at 0.2 s, 1.0 s and 2.0 s show no toast, while the swipe path raises one
at 0.35 s.

**In scope:** Discover's *Not interested* and *Save for later* buttons, and any other `SAVE`/`DISMISS`
call site a design pass finds.

**Out of scope:** *Mark read*, anywhere. It is `MARK_READ`, it is not reversible today, and it belongs to
016. Splitting it out keeps this item to one dimension — **it does not make it amendment-free, which is what
this brief originally claimed; see *Design pass outcome* 1.**

**It inherits no cause from 013.** 013's mechanism is an ancestor scroll consuming the pointer DOWN
before `ArticleCard`'s gesture handler adopts it. A `Button` is not a `pointerInput` gesture and does not
use `awaitFirstDown`. **Do not start from 013's diagnosis** — that assumption cost 013 two of its four
passes, and the evidence now says the button is not failing at all, it is succeeding silently.

Worth knowing: `DiscoverScreen.kt:74-87`'s article-change `animateScrollTo` runs for roughly 380 ms after
the head article changes.

---

## 016 — Widen what is reversible

**Re-scoped 2026-08-31; see the re-cut section above.**

Widen `ArticleStateMachine.reversibleActions` beyond `SAVE`/`DISMISS` to cover `MARK_READ`,
`MARK_UNREAD` and `REMOVE`, then raise the offer at every call site that performs them.

**Confirmed by the owner from the running app, 2026-08-31.** In **Read Later**, *Mark read* and *Remove*
raise no undo offer. In **History**, *Mark unread* raises none. Plus Discover's own *Mark read*, moved
here from 014 by the re-cut.

**This is the item that needs the amendment, and it is not a formality.** `contracts.md` §23 ties Undo to
the `signalsApplied` reversal guard item 005 introduced. Widening what is reversible changes which
preference deltas can be reversed and when — the coupling is the reason item 007 led the whole programme
and it is the reason this cannot be bolted on. **Design the amendment and the reversal arithmetic
together, on paper, before dispatching any implementer.**

Specifically to settle at design time:

- **What `REMOVE` reverses to.** It is its own `ArticleAction`, not a `DISMISS`. Its previous state may
  be `SAVED`, and the reversal has to restore that, not a generic "unremoved".
- **Whether `MARK_READ` and `MARK_UNREAD` carry preference deltas at all**, and if so what reversing
  each does to `signalsApplied`. `MARK_UNREAD` is already a reversal-shaped action; undoing it is a
  double negative and is the obvious place to get the arithmetic wrong.
- **Whether an undo offer raised in Read Later or History should survive a destination change.** The
  toast is global; the reader can switch panes inside the 4.5 s window.

---

## Gates

Per `execution-model.md` §8. All four items fire `android.yml` only; none touches `js/**`.

Two extra evidence obligations for this wave:

- **Every item that raises an undo offer must prove the reversal by the article id**, in the state
  document, not by a count and not by a weight moving. 015's trap applies to 014 and 016 as well.
- **A test that a reader who undoes in one pane and then acts in another does not double-apply or
  double-reverse a delta.** The toast is global; nothing today stops the reader moving.

---

## Walkthrough method — read this before driving any emulator step

Both learned the hard way in wave C (`wave-c-note.md` §6):

- **`uiautomator dump` cannot see the Undo toast at all.** A `screencap` at 0.35 s shows it plainly.
  Drive the whole sequence inside **one on-device `adb shell`** and score from `screencap` plus the
  pulled state document. `013/investigation/step0-undo-window.md` §158 recorded the weaker form of this;
  wave C confirmed the stronger one.
- **Card action buttons sit roughly 150 px above their text labels.** Tapping the centre of the label's
  bounds misses the button. Read the clickable node's bounds, not the label's.
- **The category chip row scrolls.** Chip coordinates go stale after anything that moves the page.
  Re-locate before every tap.

---

## Owner checkpoints

1. **Amendment 8.** Now blocks **014 as well as 016**, and it is drafted:
   `specs/waves/wave-d-amendments.md`. What becomes reversible, which surfaces may offer it, and what
   reversing each action does to the preference weights. Four sub-decisions were taken on 2026-08-31:
   proceed with 014's reversal; keep the amendment permissive rather than binding on the browser; compact
   Discover's masthead; and have undo of `MARK_UNREAD` re-apply the Mark Read delta.
2. **012's placement rule**, once the amendment is drafted — that the intent is stated in a form wave E
   can inherit.
3. **A walkthrough of the widened Undo against real accumulated history**, not fresh state. Every wave
   so far has had its most valuable defects found this way and none of them by a gate.
4. **Wave sign-off** against merged `main`.

---

## Definition of wave done

All four merged; `evidence.md` per item; walkthroughs recorded; `backlog.md` updated; `wave-d-note.md`
written per `execution-model.md` §4.6. At that point Undo means the same thing everywhere in the app,
and the backlog holds only wave E.
