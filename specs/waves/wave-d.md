# Wave D — Undo everywhere, and the Discover header

**Items:** 015, 012 concurrently · then 014 · then 016
**Prerequisite:** wave C merged (`5dd2753`)
**Cut from:** `main`

Self-contained brief. Read `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`,
`specs/execution-model.md`, then this file. Read `specs/waves/wave-c-note.md` §5 before designing 014 or
016 — it holds the observations from the app that these two items exist to act on.

---

## What this wave is

Three of the four items are the same subject seen from three angles: **Undo does not work where a reader
expects it to.** The fourth, 012, is unrelated and rides along because it collides with nothing.

This is the first wave with no parity gap in it. Every item here is either a defect the owner found by
using the app or an affordance the app promises and does not deliver.

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
| **The action is reversible; the offer is never requested.** `undoable = true` is passed at exactly one call site in the whole app — Discover's `onSwipeCommit`, `IntentionalReadingApp.kt:290`. Everything else takes the `undoable = false` default. | `SAVE`, `DISMISS` | raise the offer at the remaining call sites | **No** |
| **The action is not reversible at all.** `reversibleActions = setOf(SAVE, DISMISS)`, `ArticleStateMachine.kt:254`. `transition` builds no `UndoRecord`, and `undo` returns `UNDO_UNAVAILABLE`. | `MARK_READ`, `MARK_UNREAD`, `REMOVE` | widen `reversibleActions`, then raise the offer | **Yes** — `contracts.md` §23 |

Discover's card buttons span both rows: *Not interested* and *Save for later* are the first, ***Mark
read* is the second**. So the old 014 carried a hidden dependency on the old 016's amendment, and
whichever ran second would have inherited a half-widened `reversibleActions`.

Cut on the reversibility line instead, and the dependency disappears and the amendment is written once:

- **014 — raise the offer where the reversal already exists.** No `docs/v1/**` change.
- **016 — widen what is reversible, then wire every surface.** One amendment, one §23 conversation.

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
3. **016** — after 014 merges. Its amendment can be drafted while 014 is being implemented; the
   amendment is a documents task and blocks only 016's own design pass.

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
016. Splitting it out is what makes this item amendment-free.

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

1. **016's amendment.** Blocks 016's design. What becomes reversible, and what reversing it does to the
   preference weights.
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
