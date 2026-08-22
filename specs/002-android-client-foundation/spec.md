# 002 — Android Client Foundation

**Status:** approved (plan gate passed)\
**Workstream role:** `android-client` (see `design.md` §Workstream role)\
**Authority:** `docs/v1/README.md` Amendment 6, `docs/v1/contracts.md` §ArticleDataset,
`docs/v1/01-product.md` §27, `docs/v1/05-personalization-state.md` §§27/32,
`docs/v1/06-ui-ux.md` §§3/18/51

---

## 1. Problem

Intentional Reading has exactly one client: the static browser application. Amendment 6 authorizes a
native Android client as an additional read-only consumer of the frozen `ArticleDataset v1` contract, but
no Android code, build, or contract port exists — the repository contains no Gradle, Java, or Kotlin
anything.

The product is mobile-first by specification (`01-product.md:700-702` — "V1 is mobile-first but must
remain fully usable on desktop"), and its mobile presentation is already fully described:
`06-ui-ux.md:502` §18 fixes a bottom navigation bar, and `design-reference/DESIGN.md:92,119,125` fixes
the tab order Read Later / Discover / History with Discover centered. A native client is therefore not a
redesign; it is a second rendering of behavior that is already frozen.

Two properties of the existing system make this milestone tractable rather than speculative:

- **The dataset is a static, validated, publicly served artifact.** `pipeline/main.py:152-162` builds it,
  `pipeline/validation.py:134-178` proves it, and CI publishes it to Pages every six hours without
  committing it (`.github/workflows/deploy.yml`, `.gitignore:1`). It is fetchable today at
  `https://irodriguez.io/News-Agregator/data/articles.json`.
- **The article state machine and the screen semantics are specified to the transition, not the
  intention.** `js/state/article-state.js:70-86` is a complete transition table with idempotent no-ops,
  and `js/state/selectors.js` fixes eligibility, ordering, and counts.

This item delivers the smallest foundation that renders the real product against real data. It does not
deliver feature parity, and §3 fences off everything it deliberately omits.

## 2. Story

As a reader, I want the Intentional Reading experience as a native Android application, so that my
finite reading queue is reachable from my phone's launcher with the same three destinations and the same
one-card-at-a-time discipline as the web client.

## 3. Out of scope

- **Networking.** The client reads a committed snapshot of the published dataset from app assets. No
  `INTERNET` permission, no HTTP client, no refresh. Amendment 6 permits networking; this milestone
  defers it so the UI can be exercised and tested without it.
- **Persistence.** In-memory state only. It survives configuration change and does **not** survive
  process death; every launch begins a fresh queue. This is a stated limitation, not a defect.
- **Preference learning and personalization.** `js/ranking/personalize.js` and the
  `INTERACTION_DELTAS` table (`js/state/preferences.js:1-6`) are not ported. Discover renders in dataset
  file order, which is legitimate because the pipeline emits a total order
  (`pipeline/retention.py:12-20`).
- **Deck diversity sequencing.** The −8 same-source and −5 third-consecutive-category penalties
  (`js/ranking/deck.js:26-38`) are not ported, so the Android head article may differ from the web
  client's for the same dataset.
- **Undo.** The single-slot undo manager and toast (`js/state/article-state.js:154-231`) are omitted, so
  no Discover action offers Undo in this milestone.
- **Swipe gestures.** `js/ui/swipe.js` (90px threshold, intent lock, rotation) is not ported; the labeled
  buttons are the only triage affordance. `DESIGN.md:8` requires a labeled equivalent for every gesture,
  never the reverse.
- **Export, import, and reset.** The local-data section of the settings dialog
  (`js/ui/settings.js:221-282`) is omitted; there is nothing persisted to export.
- **Any change to the browser client, the Python pipeline, the source catalog, or the taxonomy.**
  Amendment 6 confines this item to `/android` plus its own CI workflow.
- **Visual identity of the design reference.** Iowan Old Style and Avenir do not exist on Android; see
  `design.md` §Divergences.
- **Tablet and foldable type scaling.** CSS `clamp()` has no Compose analogue; the phone end of each
  clamp is used.

## 4. Scenarios

### Scenario: the bundled dataset loads and validates
Given the application starts with no prior state\
When the bundled dataset snapshot is read from app assets\
Then it is accepted only if it satisfies the frozen `ArticleDataset v1` contract\
And `schemaVersion` must equal 1, `pipeline.successfulSourceCount + pipeline.failedSourceCount` must
equal `pipeline.enabledSourceCount`, `pipeline.articleCount` must equal the number of articles, article
IDs must be unique, and every `score.base` must equal the sum of its five components\
And the parsed articles are exposed in dataset file order.

*(contracts.md §ArticleDataset; mirrors `js/data/validation.js:197-225` and `pipeline/validation.py:134-169`)*

### Scenario: Discover offers exactly one article
Given a validated dataset with more than one eligible article in the selected category\
When the reader views Discover\
Then exactly one article card is presented\
And the header states how many articles are available in the selected category\
And a side note states how many further choices wait behind the presented card\
And no list, feed, scroll of articles, or pagination is presented.

*(01-product.md §27; 06-ui-ux.md §3; DESIGN.md:5 — "Attention is finite")*

### Scenario: a category with no articles reaches the empty state
Given the dataset contains no articles in a category\
When the reader selects that category's chip\
Then Discover presents the empty state rather than an error or a blank body\
And the empty state grants permission to leave without implying anything was missed\
And Read Later and History remain reachable.

*(06-ui-ux.md §3; `js/ui/discover.js:301-310`; 01-product.md:558-575 — no urgency or guilt messaging)*

### Scenario: a degraded dataset is disclosed without alarm
Given a validated dataset whose `pipeline.failedSourceCount` is greater than zero\
When the reader views Discover\
Then a notice discloses that some sources were unavailable during the latest refresh\
And the available articles are presented normally\
And no per-source failure detail is inferred or displayed.

*(contracts.md:133 — the client "must not infer detailed source failures that are not present in the
contract"; `js/app.js:229`)*

### Scenario: dismissing advances the deck
Given a Discover card is presented and further eligible articles exist\
When the reader activates `Not interested`\
Then the article's record becomes `status = "dismissed"` with `dismissedAt` set and `savedAt` / `readAt`
cleared\
And the article is no longer offered in Discover\
And the next article in dataset order becomes the presented card\
And the available and remaining counts decrease immediately.

*(05-personalization-state.md §§27/32; `js/state/article-state.js:70-77`, `:109-115`)*

### Scenario: saving reaches Read Later
Given a Discover card is presented\
When the reader activates `Save for later`\
Then the record becomes `status = "saved"` with `savedAt` set and `dismissedAt` / `readAt` cleared\
And the article leaves Discover\
And it appears at the top of Read Later, which orders by most recently saved\
And the Read Later navigation count increases immediately.

*(05-personalization-state.md §27; `js/state/article-state.js:102-108`; `js/state/selectors.js:12-16`)*

### Scenario: an opened article is acknowledged and held
Given a Discover card for an article with no persisted record\
When the reader activates `Read article`\
Then the publisher URL is handed to the system browser\
And the record becomes `status = "opened"` with `openedAt` set\
And the same card remains presented, visibly marked as opened\
And the card additionally offers `Mark read`\
And the article is absent from both Read Later and History.

*(06-ui-ux.md §51; 05-personalization-state.md §32; `specs/001-opened-article-return-state/spec.md` §4;
01-product.md:334 — `opened ≠ read`)*

### Scenario: marking read reaches History under Today
Given a card is presented, opened or not\
When the reader activates `Mark read`\
Then the record becomes `status = "read"` with `readAt` set, `openedAt` preserved, and `savedAt` /
`dismissedAt` cleared\
And the article leaves Discover\
And it appears in History grouped under Today\
And the History navigation count increases immediately\
And no Undo affordance is offered.

*(05-personalization-state.md §27; `js/state/article-state.js:116-122`; `js/ui/format.js:51-62`)*

### Scenario: a contract-violating dataset claims nothing
Given a dataset that violates the frozen contract in any single respect\
When the application attempts to load it\
Then Discover presents its error state\
And no article from that dataset is presented\
And an unsupported `schemaVersion` is distinguished from a malformed dataset\
And Read Later and History remain reachable.

*(contracts.md §ArticleDataset; `js/data/articles.js:11-13` failure taxonomy; `js/app.test.js:107-137`)*

### Scenario: three destinations and a modal settings surface
Given the application is running\
When the reader inspects the navigation\
Then exactly three destinations are offered, ordered Read Later, Discover, History, with Discover
centered\
And navigation counts are shown for Read Later and History only\
And Settings is presented as a modal surface and is never a fourth destination\
And the reader can choose Light, Dark, or System appearance.

*(06-ui-ux.md §18; DESIGN.md:92,94,119,125; `js/app.js:32-37`; `js/ui/navigation.js:9-46`)*

## 5. Verification

Unit gate, from `android/`:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The unit suite validates the **shipped asset bytes** with the production validator, so the snapshot
cannot drift out of contract without failing the build.

The existing web and pipeline gates must remain green and untouched, because this item modifies no code
they cover:

```sh
.venv/bin/python -m pytest
.venv/bin/python -m pipeline.main --validate-config
.venv/bin/python -m pip_audit -r requirements.txt
npm test
```

Manual acceptance on a device or emulator, after `./gradlew :app:installDebug`: walk scenarios 2, 3, 4,
5, 6, 7, 8, and 10 in order.

Scenarios 3 and 4 depend on the shape of the committed snapshot rather than on authored fixtures, so the
snapshot's profile is recorded in `evidence.md` and the walkthrough is driven from it: pick any category
the profile reports as empty to reach scenario 3, and expect the degraded notice of scenario 4 whenever
the profile reports a non-zero failed-source count. The snapshot verified while planning this item
(`generatedAt` `2026-08-22T12:59:34Z`, 166 articles) reports five failed sources, no `weightlifting` and
no `identity_automation` articles, and twenty-five titles containing non-ASCII characters — confirm that
non-ASCII titles render correctly too, since asset decoding is load-bearing. A snapshot taken later may
differ in every one of those numbers without being wrong; only the contract is fixed.

Cross-check once against the browser client serving the same bytes per `README.md`. Read Later and
History counts must match after an identical sequence of actions. The Discover head article is expected
to differ — that is the documented ranking divergence in `design.md` §Divergences, not a defect.
