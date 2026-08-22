# 001 — Design note

## Workstream role

This item runs under the **`integration`** workstream, not `frontend-ui`.

`js/app.js` is a forbidden path for both `frontend-ui` (`docs/v1/workstreams/frontend-ui.md` §5) and
`state-ranking` (`docs/v1/workstreams/state-ranking.md` §5), and `tests/js/**` is forbidden to
`frontend-ui`. `docs/v1/workstreams/integration.md` §7 grants `js/app.js`, `css/**`, `js/**`, and
`tests/**` for integration wiring and contract-compatible corrective fixes, and §11-12 make view-model
construction an `app.js` responsibility while forbidding algorithm duplication there.

Implementers must therefore not self-block on `frontend-ui`'s forbidden paths. Slice boundaries in
`slices.md` still keep each run narrow.

## Decisions

### The acknowledgment is derived, never stored

The opened marker and the `Mark read` control render from the displayed article's existing record —
`status === "opened"` and `openedAt`. No new persisted field is introduced.

This is a hard constraint, not a preference: `js/state/storage.js:105-113` enforces
`signalsApplied.opened === (openedAt !== null)` and `signalsApplied.read === (status === "read")`, so an
invented persisted flag would make `loadState` reject the whole blob as corrupt. `state.session` is
exact-key validated against `["lastCategory"]` (`js/state/storage.js:166`) and is left untouched.

No state-layer change is needed at all — `mark_read` from `opened` is already an allowed transition
(`js/state/article-state.js:70-77`) that sets `status` and `readAt` (`:116-122`), and the dispatcher
already routes it (`js/app.js:287`).

### Card stability is session-only, and non-persisted

`js/app.js` keeps an in-memory id of the opened-but-unresolved card and presents it instead of `deck[0]`
while it remains Discover-eligible and present in the current deck.

This is required for the fix to be reliable. `buildDeck` recomputes on every `render()`
(`js/ranking/deck.js:47`), and the Open preference signal (`js/state/preferences.js:1-6` —
`open: { source: 0.1, topic: 0.05 }`, plus exploration-bonus decay in `js/ranking/personalize.js:5-17`)
can change the ordering, so the top card can silently swap and the acknowledgment would often never be
seen.

Deck *ordering* rules are untouched — base scoring, exploration, and diversity sequencing all remain as
specified. Only the choice of which eligible candidate is presented first changes, and only while an open
is unresolved.

Cleared when the record leaves `opened`, on category change, and on reload. After a reload the
acknowledgment still renders whenever the opened article surfaces, since it derives from the record.

**The specifications are silent on card stability.** This is a design decision, explicitly approved at
the plan gate rather than inferred.

### No tab-return listener

`render()` already runs after a successful Open, so the acknowledgment is in the DOM before the tab is
backgrounded and is simply present on return. No `visibilitychange`, `focus`, `pagehide`,
`BroadcastChannel`, `storage`, or service-worker surface is introduced. The application's only window
listener remains `hashchange` (`js/app.js:370-375`).

### Nothing else invented

- No `Mark read` keyboard shortcut — `06-ui-ux.md` §49 enumerates exactly Left Arrow, Right Arrow, `Z`.
- No Undo toast for `mark_read` — V1 Undo covers only the most recent Discover `dismiss` or `save`
  (`05-personalization-state.md`, lines 818-822). `Mark read` gets an accessible status announcement.
- `Read article` stays on the card. Reopening is spec-supported and idempotent: a second Open preserves
  the original `openedAt` (`05-personalization-state.md` §"If already opened"; implemented at
  `js/state/article-state.js:81-86`).
- Presentation uses the existing six authored tokens with `color-mix(in oklch, ...)` per `06-ui-ux.md`
  §4, following the sibling `.article-card[data-*]` state pattern at `css/app.css:418-430`.

## Reuse

| Need | Existing code |
| --- | --- |
| deck construction | `buildDeck` — `js/ranking/deck.js` |
| eligibility test | `isDiscoverEligible` — `js/state/selectors.js:3` |
| view-model call site | `ui.renderDiscover({...})` — `js/app.js:207-219` |
| `OPENED → READ` transition | `commitArticleAction` — `js/state/article-state.js:116-122` |
| action dispatch from a card | `perform()` — `js/ui/discover.js:186-231` |
| DOM/button helpers | `element`, `button` — `js/ui/dom.js` |
| accessible status | `announceStatus` — `js/ui/toast.js` / `js/ui/index.js` |
| card state styling pattern | `.article-card[data-*]` states — `css/app.css:418-430` |
| test doubles | `MemoryStorage`, `createUiRecorder`, `createWindowStub`, `makeArticle`, `makeDataset` — `tests/js/helpers.js` |

## Regression boundary

These existing tests encode contracts this item must not break:

- `tests/js/article-state.test.js:167` — `isDiscoverEligible({ status: "opened" }) === true`.
- `tests/js/app.test.js:204` — Open validates the URL, attempts persistence first, and still navigates
  after a persistence failure.
- `tests/js/app.test.js:243-266` — `window.open` receives `_blank`, `noopener,noreferrer`, and
  `opener` is nulled.
- `tests/js/app.test.js:99-124` — History populates only after an explicit `mark_read`.

No ADR is warranted: no new dependency, no integration, no security-relevant mechanism.
