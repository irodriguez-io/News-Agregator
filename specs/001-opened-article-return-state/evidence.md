# 001 — Evidence

**Item:** Opened Article Return State\
**Branch:** `feat/001-opened-article-return-state`\
**Base:** `main` @ `f42335c` (hosted CI green on that SHA, verified before dispatch)

---

## Commits

| SHA | Commit |
| --- | --- |
| `bd060ff` | `docs(spec): design opened article return state` |
| `6971d1c` | `test: cover opened article return state` (slice 1 RED) |
| `4226473` | `feat: hold opened article in Discover` (slice 1 GREEN) |
| `a5f6964` | `docs(spec): mark slice 1 done` |
| `1e23447` | `test: cover opened Discover card actions` (slice 2 RED) |
| `abd033e` | `feat: acknowledge opened Discover articles` (slice 2 GREEN) |

Implementer: Codex, one fresh session per slice. Reviewer: Claude (non-author) at each slice gate.

## Definition of Done

### Slice 1 — view model carries opened state, opened card held
- [x] Discover view model exposes a derived `opened` boolean from the displayed article's record
      (`status === "opened"` and `openedAt !== null`) — `js/app.js:118`, `:225`.
- [x] After a successful Open the same article is still the displayed card — `js/app.js:213-220`.
- [x] Hold released when the record leaves `opened`, when the article drops out of the deck
      (`js/app.js:219`), and on category change (`js/app.js:326`).
- [x] Failed Open persistence exposes no opened state (guarded by `result.ok`, `js/app.js:283-288`).
- [x] No new persisted field; `state.session` unchanged; `js/state/storage.js` invariants untouched.
- [x] `buildDeck` ordering unchanged — the hold only selects among already-ranked eligible candidates.
- [x] No new window/document listeners; `hashchange` remains the only one.
- [x] `isDiscoverEligible` imported from `js/state/selectors.js`, no algorithm duplicated
      (`docs/v1/workstreams/integration.md` §13).

### Slice 2 — acknowledgment and Mark read control
- [x] Opened card renders a visible acknowledgment (`data-opened="true"` plus an "Opened" text label,
      not colour alone) — `js/ui/discover.js:139`, `:164-172`.
- [x] `Mark read` offered alongside `Save for later` and `Not interested` — `js/ui/discover.js:203-209`.
- [x] `Mark read` dispatches `mark_read` and produces the full §27 transition: `status: "read"`,
      `readAt` set, `openedAt` preserved, `savedAt` / `dismissedAt` cleared.
- [x] Article leaves Discover, appears in History under Today, History count updates immediately.
- [x] No Undo affordance for `mark_read`; accessible status announcement instead
      (`05-personalization-state.md` lines 818-822).
- [x] A card with no opened record renders exactly as before (asserted explicitly).
- [x] Colours are token-only via `color-mix(in oklch, ...)` per `06-ui-ux.md` §4; reuses the existing
      `.button-quiet` class; no animation (§44).
- [x] No new keyboard shortcut (§49); `Read article` retained.

## Gate results

Local gate `npm test` (Node 24.x), run by the reviewer independently at each SHA:

| SHA | tests | pass | fail |
| --- | --- | --- | --- |
| `6971d1c` (slice 1 RED) | 101 | 95 | 6 |
| `4226473` (slice 1 GREEN) | 101 | 101 | 0 |
| `1e23447` (slice 2 RED) | 105 | 101 | 4 |
| `abd033e` (slice 2 GREEN) | 105 | 105 | 0 |

Failing-first was confirmed genuine at both RED commits — the new scenario-named tests failed for
missing behavior while every pre-existing test still passed. No test was edited, skipped, or deleted;
`git diff` over `tests/` shows zero removed lines across both slices.

Coverage went from 95 to 105 tests (+10).

## Scenario traceability

| Scenario (`spec.md` §4) | Authority | Covered by |
| --- | --- | --- |
| opened article is acknowledged on return | `06-ui-ux.md` §51 | `tests/js/app.test.js`, `tests/js/discover.test.js` |
| mark read from opened reaches History | `05-personalization-state.md` §27, `06-ui-ux.md` §3.5 | `tests/js/discover.test.js` |
| opened stays Discover-eligible until resolved | `05-personalization-state.md` §32 | `tests/js/app.test.js` |
| open persistence failure claims nothing | `06-ui-ux.md` §50 | `tests/js/app.test.js` |
| mark read offers no Undo | `05-personalization-state.md` lines 818-822 | `tests/js/discover.test.js` |

## Test infrastructure added

`tests/js/dom-fixture.js` — a dependency-free minimal DOM (real tree semantics, `textContent`
composition, event dispatch with bubbling, `dataset`, `click()`) so the real `js/ui/discover.js` can be
exercised without jsdom. Required because `AGENTS.md` forbids frontend and test runtime dependencies.
Reviewed for vacuity: the tests drive the actual production modules, not a stand-in.

## Reviewer observation (not a finding)

`actionResult` in `js/ui/discover.js:119` changed from `async` to sync-returning-when-the-handler-returns
a non-thenable. All four paths (sync throw, rejected promise, plain value, `undefined`) were checked and
are semantics-preserving; the change exists so the failure-restoration path is deterministically
testable. It touches a shared helper, which brushes against `AGENTS.md`'s "no unrelated refactoring", but
it is in an owned file, in the exact function the new control's failure path flows through, and weakens
no test.

## Outstanding — owner verification

The `spec.md` §5 browser walkthrough was **not** performed by the agent and remains open:

```sh
python3.13 -m venv .venv && .venv/bin/python -m pip install -r requirements-dev.txt
.venv/bin/python -m pytest
.venv/bin/python -m pipeline.main
.venv/bin/python scripts/build_pages.py
.venv/bin/python -m http.server 4173 --directory .build/pages
```

Then click `Read article`, switch to the publisher tab, return, and confirm the acknowledgment, the
`Mark read` control, and the resulting History entry.

Reason it was not run: no `.venv` and no `data/articles.json` exist locally, only Python 3.14.5 is
installed against the project's pinned Python 3.13, and generating the dataset requires live network
fetches across 20 sources (two of which are known-failing under Amendment 4). Running that on the wrong
interpreter would have produced ambiguous evidence. The Python pipeline is untouched by this item, and
`scripts/build_pages.py` allowlists `css` and `js` wholesale, so the changed files need no build change.
The genuine tab-switch is a human step regardless.
