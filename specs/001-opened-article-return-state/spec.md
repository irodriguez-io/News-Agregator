# 001 — Opened Article Return State

**Status:** approved (plan gate passed)\
**Workstream role:** `integration` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/05-personalization-state.md` §27, `docs/v1/06-ui-ux.md` §51

---

## 1. Problem

A reader activates `Read article`, reads in the new tab, and returns to the application tab. Nothing
acknowledges the open, and the article never reaches History.

The Open itself is recorded correctly. `handleArticleAction` persists `status = "opened"` before
navigation (`js/app.js:242-278`, `js/state/article-state.js:96-101`), and absence from History is the
frozen contract — `docs/v1/01-product.md:334` states `opened ≠ read`, and History selects
`status === "read"` (`js/state/selectors.js:20`).

The defect is that two authoritative sections were never implemented in the UI:

- **`05-personalization-state.md` §27 — Mark Read from Opened.** Requires the `OPENED → READ`
  transition: "An article may be read immediately without first entering Read Later."
- **`06-ui-ux.md` §51 — Opened Article Return State.** On return, the application may visibly
  acknowledge that the article has been opened, and should support the next choices `Mark read`,
  `Save for later`, `Not interested`.

The state layer already satisfies §27 (`js/state/article-state.js:70-77`, `:116-122`) and the dispatcher
already accepts `mark_read` (`js/app.js:287`), but **no Discover UI path reaches it**. The card exposes
only Not interested / Read article / Save for later, and no opened-state presentation exists in
`css/app.css`. A reader who opens directly from Discover can therefore reach `read` only by first saving
to Read Later.

## 2. Story

As a reader, I want the application to acknowledge an article I have opened and let me mark it read from
Discover, so that my History reflects what I actually finished without routing everything through Read
Later.

## 3. Out of scope

- Changing `isDiscoverEligible` — an opened-but-unresolved article remains eligible
  (`05-personalization-state.md` §32).
- Automatically promoting `opened → read`. `opened ≠ read` is frozen; the reader marks read explicitly.
- Any amendment to `docs/v1/**`.
- Read Later and History screen layouts.
- The Python pipeline, source catalog, and deployment.
- A `Mark read` keyboard shortcut — `06-ui-ux.md` §49 enumerates exactly Left Arrow, Right Arrow, `Z`.

## 4. Scenarios

### Scenario: opened article is acknowledged on return
Given a Discover card for an article with no persisted record\
When the reader activates `Read article` and the Open persists successfully\
Then the publisher opens in a new isolated tab (`_blank`, `noopener,noreferrer`, unchanged)\
And the same card remains on screen, visibly marked as opened\
And the card offers `Mark read`, `Save for later`, and `Not interested`.

*(06-ui-ux.md §51)*

### Scenario: mark read from opened reaches History
Given an opened-but-unresolved card is displayed on Discover\
When the reader activates `Mark read`\
Then the record becomes `status = "read"` with `readAt` set, `openedAt` preserved, and `savedAt` /
`dismissedAt` cleared\
And the article leaves Discover\
And it appears in History grouped under Today\
And the History navigation count increases immediately.

*(05-personalization-state.md §27; 06-ui-ux.md §3.5)*

### Scenario: opened stays Discover-eligible until resolved
Given an opened-but-unresolved article\
When the reader reloads the application\
Then the article is still offered in Discover\
And it is absent from both History and Read Later.

*(05-personalization-state.md §32 — "An opened-but-unresolved article remains eligible")*

### Scenario: open persistence failure claims nothing
Given local persistence is failing\
When the reader activates `Read article`\
Then the publisher still opens\
And the accessible status states that the Open interaction was not saved locally\
And no opened acknowledgment and no `Mark read` control is presented.

*(06-ui-ux.md §50, lines 1170-1174 — do not claim an unpersisted transition)*

### Scenario: mark read offers no Undo
Given the reader has just marked an opened article read\
Then an accessible status announcement is made\
And no Undo affordance is offered.

*(05-personalization-state.md lines 818-822 — V1 Undo supports only the most recent Discover `dismiss`
or `save`)*

## 5. Verification

Unit gate: `npm test` (`node --test tests/js/*.test.js`, Node 24.x).

End-to-end, against the real Pages artifact per `README.md`:

```sh
.venv/bin/python -m pipeline.main
.venv/bin/python scripts/build_pages.py
.venv/bin/python -m http.server 4173 --directory .build/pages
```

Then in a browser: walk scenario 1 including a genuine tab switch and return, confirm the acknowledgment
and the `Mark read` control, activate it, and confirm the article appears in History under Today with the
navigation count incremented. Reload and confirm scenario 3.
