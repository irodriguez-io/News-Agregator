# Intentional Reading — V1 Architecture Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/02-architecture.md`\
**Role:** Authoritative system architecture specification

---

## 1. Purpose

This document defines the technical architecture, component boundaries, runtime model, repository structure, and dependency direction for Intentional Reading V1.

The architecture must support:

- deterministic multi-source article ingestion;
- static GitHub Pages hosting;
- local-only personalization;
- independent implementation workstreams;
- testable component boundaries;
- graceful source failures;
- no backend application server;
- no database;
- no external AI API dependency;
- no frontend runtime framework.

This document defines **how major parts of the system are separated**.

Detailed schemas and algorithms are defined in their respective authoritative specifications.

---

## 2. Architectural Principles

### 2.1 Static application first

The deployed application must remain a static website.

Production runtime consists of:

```text
HTML
CSS
vanilla JavaScript
static JSON
browser localStorage
```

There is no continuously running application backend.

---

### 2.2 Build-time ingestion, browser-time personalization

Responsibilities are deliberately separated:

```text
BUILD TIME
Python
    ↓
fetch + extract + normalize + classify/admit + force tags
    ↓
metadata confidence + deduplicate + full score + retain + validate
    ↓
data/articles.json


BROWSER TIME
JavaScript
    ↓
load static articles
    ↓
combine with local preferences/state
    ↓
build personalized Discover deck
```

The Python pipeline does not know the user's swipe history.

The browser does not know how RSS/Atom/HTML sources are fetched.

---

### 2.3 Source complexity stays out of the frontend

The browser must not contain:

- RSS URLs;
- Atom parsing;
- source-specific HTML parsers;
- feed adapters;
- source-quality configuration;
- content admission rules;
- source-fetch retry logic.

All of those belong to the Python ingestion layer.

---

### 2.4 Personal data stays out of the pipeline

The build pipeline must not receive:

- swipe history;
- Read Later contents;
- History;
- source preference weights;
- topic preference weights;
- appearance preference;
- exported local-state backups.

Personalization is exclusively browser-local in V1.

---

### 2.5 Dependencies point inward through stable contracts

Components communicate through explicit interfaces.

The system should avoid modules reaching into another subsystem's implementation details.

Conceptually:

```text
External Publishers
        ↓
   Python Adapters
        ↓
 Normalized Articles
        ↓
 Taxonomy + Scoring
        ↓
 articles.json
        ↓
 js/data/articles.js
        ↓
 State + Ranking
        ↓
       UI
```

No layer should bypass the layer immediately responsible for the contract it consumes.

---

## 3. Runtime and Build-Time Architecture

The complete V1 architecture is:

```text
                         GITHUB ACTIONS
                               │
                               ▼
                      Python ingestion pipeline
                               │
             ┌─────────────────┼──────────────────┐
             ▼                 ▼                  ▼
          RSS/Atom        HTML listings       Release notes
             │                 │                  │
             └─────────────────┼──────────────────┘
                               ▼
                         adapter extraction
                               │
                               ▼
                    plain-text normalization
                               │
                               ▼
              URL canonicalization and validation
                               │
                               ▼
               stable Article ID / normalized date
                               │
                               ▼
                  organic taxonomy matching
                               │
                               ▼
                source-specific admission filtering
                               │
                               ▼
                    forced tag application
                               │
                               ▼
                metadata confidence calculation
                               │
                               ▼
                         deduplication
                               │
                               ▼
                    full Article base scoring
                               │
                               ▼
                            retention
                               │
                               ▼
                   final dataset validation
                               │
                               ▼
                      data/articles.json
                               │
                               ▼
                     GitHub Pages artifact
                               │
                 ┌─────────────┴─────────────┐
                 │                           │
                 ▼                           ▼
           Static frontend             Browser storage
                 │                           │
                 └─────────────┬─────────────┘
                               ▼
                       personalized deck
                               │
                               ▼
                Read Later / Discover / History
```

---

## 4. Technology Stack

### Production frontend

Required:

- semantic HTML;
- CSS;
- vanilla JavaScript using ES modules;
- browser `fetch`;
- browser `localStorage`.

No frontend framework is required or permitted in V1 unless an authoritative specification is explicitly revised.

Specifically, V1 must not introduce runtime dependencies on:

- React;
- Vue;
- Angular;
- Svelte;
- jQuery;
- UI component frameworks;
- gesture libraries;
- state-management frameworks.

Swipe behavior must be implementable using browser pointer/touch APIs and standard JavaScript.

---

### Ingestion pipeline

Required language:

```text
Python 3
```

Expected dependencies should remain deliberately small.

Likely runtime dependencies include:

- `feedparser`;
- `requests`;
- `beautifulsoup4`;
- `python-dateutil`.

The final approved dependency set is authoritative in:

`08-security-dependencies.md`

Near-duplicate matching should use the Python standard library where practical before adding another dependency.

---

### Development/test tooling

JavaScript test dependencies may be introduced as development-only dependencies.

Expected tools:

- Vitest;
- jsdom.

They must not become frontend runtime dependencies.

Python testing should use a conventional test framework, expected to be `pytest`.

Exact dependency and version policies are authoritative in:

`08-security-dependencies.md`

---

## 5. Repository Structure

V1 should evolve toward this structure:

```text
.
├── AGENTS.md
├── README.md
├── index.html
│
├── css/
│   └── app.css
│
├── js/
│   ├── app.js
│   │
│   ├── data/
│   │   └── articles.js
│   │
│   ├── ranking/
│   │   ├── personalize.js
│   │   └── deck.js
│   │
│   ├── state/
│   │   ├── storage.js
│   │   └── preferences.js
│   │
│   ├── ui/
│   │   ├── discover.js
│   │   ├── swipe.js
│   │   ├── read-later.js
│   │   ├── history.js
│   │   ├── navigation.js
│   │   └── settings.js
│   │
│   └── utils/
│       ├── dates.js
│       └── text.js
│
├── pipeline/
│   ├── __init__.py
│   ├── main.py
│   ├── fetch.py
│   ├── normalize.py
│   ├── deduplicate.py
│   ├── taxonomy.py
│   ├── scoring.py
│   ├── output.py
│   │
│   └── adapters/
│       ├── __init__.py
│       ├── rss.py
│       ├── atom.py
│       ├── rss_autodiscovery.py
│       └── html_listing.py
│
├── config/
│   ├── sources.json
│   └── topics.json
│
├── data/
│   └── articles.json
│
├── tests/
│   ├── pipeline/
│   │   ├── fixtures/
│   │   ├── test_normalize.py
│   │   ├── test_deduplicate.py
│   │   ├── test_taxonomy.py
│   │   └── test_scoring.py
│   │
│   └── js/
│       ├── storage.test.js
│       ├── preferences.test.js
│       └── ranking.test.js
│
├── docs/
│   └── v1/
│       └── ...
│
├── design-reference/
│   ├── DESIGN.md
│   └── intentional-reading-prototype.png
│
├── requirements.txt
├── package.json
├── package-lock.json
│
└── .github/
    ├── dependabot.yml
    └── workflows/
        ├── test.yml
        └── deploy.yml
```

Minor file additions are permitted when they preserve the architecture and ownership boundaries defined here.

Implementation agents must not reorganize the architecture unilaterally.

---

## 6. Legacy File Migration

The existing repository currently contains:

```text
index.html
style.css
script.js
```

V1 implementation may replace the internals of the existing application, but migration should happen deliberately.

Target layout:

```text
style.css
    ↓
css/app.css

script.js
    ↓
modular js/**
```

`index.html` remains the static application entry point.

The old `script.js` and `style.css` should be removed only after their V1 replacements are integrated and verified.

No implementation agent should delete legacy files merely because new modules exist.

Removal occurs during integration when the replacement paths are confirmed functional.

---

## 7. Python Pipeline Boundary

The Python subsystem owns all operations needed to convert configured publishers into the normalized article dataset.

It owns:

```text
external HTTP retrieval
feed parsing
source-specific parsing
URL normalization
stable article IDs
excerpt cleanup
date normalization
taxonomy matching
source admission rules
deduplication
base scoring
retention
output generation
pipeline metadata
sanity checks
```

It must not own:

```text
personalized ranking
swipe history
Read Later
History
appearance state
browser interactions
UI rendering
```

---

## 8. Pipeline Orchestration

`pipeline/main.py` acts as orchestration rather than containing the implementation of every stage.

Its conceptual flow is:

```python
load_configuration()

for source in enabled_sources:
    response = fetch_source(source)
    entries = extract_entries(response, source)
    entries = normalize_plain_text(entries)
    entries = canonicalize_urls_and_validate_required_fields(entries)
    entries = assign_article_ids_and_normalize_dates(entries)
    entries = match_organic_taxonomy(entries)
    entries = apply_admission_rules(entries, source)
    entries = apply_forced_tags(entries, source)
    candidates += compute_metadata_confidence(entries)

articles = deduplicate(candidates)
articles = compute_full_base_scores(articles)
articles = apply_retention_rules(articles)
validate_dataset(articles)
write_output(articles)
```

Metadata confidence is calculated before deduplication because duplicate-winner selection requires it. This calculation is not full base scoring; the complete Article base score is calculated only after deduplication.

The exact function names are not contractual.

The separation of responsibilities is contractual.

Large monolithic ingestion scripts are prohibited.

---

## 9. Adapter Architecture

External content is accessed through source adapters.

Each adapter must present a common conceptual interface:

```text
SourceConfig
     ↓
adapter.fetch(...)
     ↓
list of raw article records
```

V1 adapter classes/types are:

```text
rss
atom
rss_autodiscovery
html_listing
```

### `rss`

Parses RSS feeds.

### `atom`

Parses Atom feeds.

### `rss_autodiscovery`

Loads a configured canonical page, discovers a declared RSS/Atom feed, and then parses that feed.

This should be used when the publisher exposes a feed but maintaining a hardcoded feed URL is unnecessarily brittle.

### `html_listing`

Used only for explicitly approved sources that do not expose a suitable feed.

It is not a generalized website scraper.

Every HTML listing parser must be source-specific and deliberately scoped.

---

## 10. Adapter Failure Isolation

One source failure must not normally stop the entire ingestion pipeline.

For each source, the pipeline should record:

```text
success
failure
fetched count
accepted count
rejected count
error summary
```

A failed source should emit an actionable warning and allow remaining sources to continue.

Catastrophic dataset validation is handled separately.

---

## 11. Configuration Boundary

Publisher and topic behavior is configuration-driven.

### `config/sources.json`

Owns source-level configuration such as:

- source ID;
- display name;
- category;
- adapter;
- canonical URL/feed URL;
- quality score;
- content type;
- enabled state;
- admission requirements;
- forced tags where explicitly allowed.

### `config/topics.json`

Owns taxonomy configuration such as:

- topic ID;
- display label;
- allowed categories;
- aliases;
- deterministic matching metadata.

Implementation code should not duplicate source lists or topic dictionaries that belong in these configuration files.

---

## 12. Generated Dataset Boundary

The build-time contract between Python and JavaScript is:

```text
data/articles.json
```

This is a static generated artifact.

It is not a network API.

The browser performs a standard static-file request such as:

```javascript
fetch("./data/articles.json")
```

No server-side endpoint processes this request.

---

## 13. `js/data/articles.js`

`js/data/articles.js` is the frontend data-access boundary for the generated dataset.

It owns:

- loading `data/articles.json`;
- validating supported dataset schema version;
- performing lightweight structural validation;
- returning normalized article records to browser modules;
- reporting dataset-loading failures through a predictable error interface.

It must not:

- fetch publishers directly;
- fetch RSS;
- call `rss2json`;
- call OpenAI;
- call Anthropic;
- contain source-ranking configuration;
- modify localStorage;
- calculate user preference weights;
- render UI.

The directory is named `data`, not `api`, specifically because V1 does not expose or consume an application backend API.

---

## 14. Browser State Boundary

All direct `localStorage` access belongs to:

```text
js/state/storage.js
```

No UI or ranking module may directly call:

```javascript
localStorage.getItem(...)
localStorage.setItem(...)
localStorage.removeItem(...)
```

Instead, browser modules use functions exported by `storage.js`.

This creates one boundary for:

- serialization;
- versioning;
- migrations;
- validation;
- import/export;
- corruption recovery.

---

## 15. Preference-Learning Boundary

`js/state/preferences.js` owns the rules for translating user actions into preference changes.

It owns:

- source preference updates;
- topic preference updates;
- weight clamping;
- interaction counters;
- idempotent learning signals;
- reversing signals during Undo where applicable.

It must not:

- render UI;
- fetch articles;
- directly manipulate DOM;
- directly access localStorage except through the storage abstraction;
- reorder the Discover deck by itself.

---

## 16. Personalized Ranking Boundary

`js/ranking/personalize.js` owns calculation of article-specific personalized ranking components.

Conceptually:

```text
base score
+
source preference
+
topic preference
+
exploration adjustment
=
personalized candidate score
```

The module should expose the components used to derive the final score for debugging and tests.

Ranking calculations must remain deterministic for identical:

- article input;
- preference state;
- interaction state.

---

## 17. Deck Construction Boundary

`js/ranking/deck.js` owns construction of the final Discover sequence.

It is responsible for:

1. excluding articles whose state makes them ineligible for Discover;
2. applying the selected category filter;
3. requesting personalized scores;
4. sorting candidates;
5. applying diversity sequencing;
6. returning the Discover deck.

Diversity sequencing is a separate sequencing concern rather than an irreversible change to an article's persisted base score.

---

## 18. UI Boundary

Modules under:

```text
js/ui/
```

own browser presentation and user interaction.

They may:

- construct DOM elements;
- render application states;
- listen for user input;
- call state/ranking functions;
- announce state changes;
- request navigation changes.

They must not independently implement:

- preference algorithms;
- source scoring;
- taxonomy matching;
- persistence serialization;
- article feed parsing.

---

## 19. UI Module Responsibilities

### `discover.js`

Owns:

- Discover-screen rendering;
- active article-card presentation;
- category-selector integration;
- loading/empty/error states;
- triggering triage actions.

### `swipe.js`

Owns:

- pointer/touch drag behavior;
- swipe threshold handling;
- swipe visual feedback;
- keyboard-equivalent swipe input;
- reduced-motion-compatible transition behavior.

It must return semantic actions such as:

```text
dismiss
save
```

rather than directly changing persistence or preference data.

### `read-later.js`

Owns Read Later presentation and its user controls.

### `history.js`

Owns History presentation and its user controls.

### `navigation.js`

Owns primary navigation presentation and active-destination behavior.

### `settings.js`

Owns:

- Settings dialog;
- theme selection;
- export initiation;
- import initiation;
- reset confirmation.

Settings must remain a secondary surface rather than a primary application destination.

---

## 20. Application Coordinator

`js/app.js` is the frontend composition root.

It owns wiring modules together.

Conceptually it coordinates:

```text
load dataset
load local state
initialize theme
determine route/view
construct Discover deck
initialize UI
listen for semantic UI actions
delegate state updates
refresh affected views
```

`app.js` must not become a second implementation of:

- ranking;
- storage;
- swipe mechanics;
- feed parsing;
- taxonomy;
- source scoring.

It should coordinate rather than duplicate subsystem logic.

---

## 21. Application Routing

V1 does not require a routing framework.

Primary destinations may be represented through:

- view state;
- URL hash;
- query state;

provided browser navigation remains predictable.

The exact mechanism may be chosen during implementation, but:

- no SPA framework may be added for routing;
- primary views must remain directly reachable through application navigation;
- switching views must not reload publisher feeds;
- locally available Read Later and History must remain accessible if Discover dataset loading fails.

---

## 22. Theme Architecture

Appearance state supports:

```text
light
dark
system
```

Theme preference is stored locally.

`system` observes:

```text
prefers-color-scheme
```

and follows changes when practical.

Theme implementation must rely on the approved design tokens rather than duplicating independent color definitions across components.

Authoritative visual tokens are defined in:

```text
design-reference/DESIGN.md
```

and incorporated into:

```text
06-ui-ux.md
```

---

## 23. External Article Navigation

The application does not proxy article bodies.

Selecting Read Article or Reopen navigates to the publisher's canonical URL.

External article links:

- are visibly identifiable as external;
- open in a new browser tab/window;
- use safe external-link attributes where applicable.

Opening a publisher URL is separate from marking an article as read.

---

## 24. Trust Boundary for Remote Content

All external article metadata must be treated as untrusted input.

Examples include:

- title;
- author;
- excerpt;
- source-provided markup;
- URLs;
- feed metadata.

The frontend must not directly inject remote publisher markup through unsafe DOM operations.

Where article fields are expected to contain text, rendering should use:

```text
textContent
DOM element construction
```

rather than assigning remote content directly to:

```text
innerHTML
```

If HTML cleanup is required during ingestion, the pipeline should normalize content into safe text before generating `articles.json`.

No implementation should trust publisher-provided markup merely because the source is whitelisted.

Detailed security requirements belong to:

`08-security-dependencies.md`

---

## 25. No `rss2json` Runtime Dependency

The current application's `rss2json` usage is legacy behavior.

V1 removes this runtime dependency.

The browser must never depend on `rss2json` for application operation.

Source ingestion occurs during the Python/GitHub Actions build pipeline.

---

## 26. No Random Image Dependency

The current LoremFlickr fallback behavior is legacy behavior.

V1 removes:

- LoremFlickr;
- article-image extraction;
- random image fallbacks;
- article thumbnails.

No image-fetching subsystem exists in V1.

---

## 27. Build-Time Pipeline Boundary

The Python pipeline owns:

- fetching approved sources;
- source-specific parsing;
- normalization and canonical URLs;
- deterministic IDs;
- taxonomy matching and admission;
- deduplication and base scoring;
- dataset validation and atomic output.

The browser does not repeat or reinterpret those operations.

---

## 28. Failure Isolation

One source failure must not abort collection from unrelated sources. The pipeline records bounded diagnostics, continues other sources, and applies the deployment sanity thresholds defined in `07-pipeline-deployment.md`.

The browser must continue to operate from the last successfully deployed static artifact when a later refresh fails.

---

## 29. Testing Boundaries

Deterministic pipeline and browser modules are unit tested. Remote parsers use local fixtures. Integrated browser behavior is verified against a local static dataset. Live-network checks remain separate from deterministic tests.

The authoritative gate and acceptance matrix is `09-testing-acceptance.md`.

---

## 30. Dependency Boundary

Production runtime dependencies are minimized. The frontend remains static and client-side; Python dependencies exist only for build-time ingestion. Dependency pinning, auditing, and GitHub Actions controls are defined in `08-security-dependencies.md`.

---

## 31. Deployment Architecture

GitHub Actions produces one static GitHub Pages artifact containing the application and a validated `data/articles.json`. No application server, database, secret-bearing browser service, or runtime feed proxy is introduced.

---

## 32. Workstream Ownership

### Content Pipeline

Owns `pipeline/`, source/topic configuration validation, fixtures, and generated dataset semantics.

### State/Ranking

Owns `js/state/` and `js/ranking/`, including persistence, transitions, personalization, Undo, and deterministic deck sequencing.

### Frontend UI

Owns presentation, navigation, gestures, keyboard behavior, accessibility, and responsive styling.

### Integration/Supervisor

Owns application coordination, contract enforcement, cross-workstream integration, release gates, and deployment wiring.

Workstreams may not change shared contracts without supervisor approval.

---

## 33. Architectural Completion Criteria

V1 architecture is complete when:

- approved sources produce a validated static dataset;
- the browser loads that dataset without a runtime feed/API dependency;
- all application persistence is isolated behind the storage module;
- ranking is deterministic and side-effect free;
- UI modules issue semantic actions rather than mutating state;
- remote content remains plain text at the rendering boundary;
- a static artifact can be tested and deployed through the approved workflows.
