# 001 — slice plan

Sized **M → 2 ordered slices**. One item branch (`feat/001-opened-article-return-state`), one PR.
Each slice closes as a failing-first test commit plus an implementation commit, and must fit one fresh
implementer context window.

Scenario names refer to `spec.md` §4.

## Slice 1: the Discover view model carries opened state and holds the opened card

- **Scenarios:** "opened article is acknowledged on return" (stability half); "opened stays
  Discover-eligible until resolved"; "open persistence failure claims nothing".
- **Files:** `js/app.js`, `tests/js/app.test.js`.
- **Must not touch:** `js/ui/**`, `css/**`, `js/state/**`, `js/ranking/**`, `js/data/**`, `docs/v1/**`,
  `pipeline/**`, `config/**`, `index.html`, `package.json`.
- **Reuse:** `buildDeck` (`js/ranking/deck.js`), `isDiscoverEligible` (`js/state/selectors.js:3`), the
  existing `ui.renderDiscover` call site (`js/app.js:207-219`), and `createUiRecorder` /
  `MemoryStorage` / `createWindowStub` / `makeDataset` from `tests/js/helpers.js` — the recorder captures
  view models, so this slice is observable without any UI change.
- **Definition of done:**
  - the Discover view model exposes the displayed article's opened state, derived from its record;
  - after a successful Open, the same article is still the displayed card on the next render;
  - the held card is released when its record leaves `opened` and when the category changes;
  - a failed Open persistence yields no opened state in the view model;
  - no new persisted state; `state.session` unchanged;
  - `npm test` green, with the four regression tests in `design.md` §Regression boundary untouched
    and passing.
- **Status:** done (gate: PASS, slice review)

## Slice 2: opened acknowledgment and `Mark read` control on the card

- **Scenarios:** "opened article is acknowledged on return" (presentation half); "mark read from opened
  reaches History"; "mark read offers no Undo".
- **Files:** `js/ui/discover.js`, `css/app.css`, `tests/js/` (Discover render and action coverage).
- **Must not touch:** `js/app.js`, `js/state/**`, `js/ranking/**`, `js/data/**`, `docs/v1/**`,
  `pipeline/**`, `config/**`.
- **Reuse:** the existing `perform()` helper and the three current controls
  (`js/ui/discover.js:186-231`), `element` / `button` from `js/ui/dom.js`, `announceStatus`, the
  `mark_read` action already accepted by the dispatcher (`js/app.js:287`), and the
  `.article-card[data-*]` CSS pattern at `css/app.css:418-430`.
- **Definition of done:**
  - a card whose record is `opened` renders a visible opened acknowledgment;
  - that card offers `Mark read` alongside `Save for later` and `Not interested`;
  - `Mark read` dispatches `mark_read`, the article leaves Discover, appears in History under Today, and
    the History navigation count updates immediately;
  - no Undo affordance is offered for `mark_read`; an accessible status announcement is made instead;
  - a card with no opened record renders exactly as it does today;
  - styling uses only the authored tokens with `color-mix(in oklch, ...)`; no new keyboard shortcut;
  - `npm test` green.
- **Status:** pending

## Gates

`npm test` (`node --test tests/js/*.test.js`, Node 24.x) for every slice.

The Python gates are untouched by this item but run in CI on the PR and must stay green:
`pytest`, `python -m pipeline.main --validate-config`, `python -m pip_audit -r requirements.txt`.
