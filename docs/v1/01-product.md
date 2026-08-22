# Intentional Reading — V1 Product Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/01-product.md`\
**Role:** Authoritative product specification

---

## 1. Purpose

Intentional Reading is a personal reading-discovery and triage application designed to replace passive doomscrolling during short periods of downtime with deliberate, high-value reading.

The application is not intended to maximize engagement, session duration, article consumption, or return frequency.

Its primary purpose is to help the user answer one question quickly:

> Is this article worth my attention?

The application should make that decision easy, preserve worthwhile material for later, and then allow the user to leave.

---

## 2. Product Problem

Short periods of downtime—such as waiting in line or waiting for an appointment—are easily consumed by passive social-media or news-feed behavior.

Traditional aggregators often recreate the same problem by emphasizing:

- volume;
- novelty;
- recency;
- infinite feeds;
- engagement;
- sensational headlines;
- low-quality secondary content;
- AI-generated or low-effort articles.

Intentional Reading should instead provide a deliberately constrained stream of high-quality material from curated sources.

The product should optimize for:

- source quality;
- relevance;
- intentional selection;
- useful discovery;
- intellectual variety;
- low cognitive overhead;
- short, purposeful sessions.

---

## 3. Target User and Usage Model

V1 is a **single-user personal application**.

Primary usage is expected to occur:

- on one personal device;
- during short downtime sessions;
- frequently on a mobile phone;
- with occasional desktop use.

Cross-device synchronization is not required.

No account, login, identity system, cloud profile, or remote personalization store is required.

User preferences, reading history, and queue state remain local to the device.

---

## 4. Product Philosophy

### 4.1 Attention is finite

The application should help the user:

1. evaluate an article;
2. save or reject it;
3. read something worthwhile;
4. leave.

The product must not deliberately encourage extended engagement.

V1 must avoid:

- infinite-scroll mechanics;
- streaks;
- reward animations;
- gamification;
- engagement scores;
- artificial urgency;
- autoplay behavior;
- celebratory effects;
- social comparison;
- recommendations designed primarily to prolong sessions.

---

### 4.2 Text is the primary content surface

Article quality should be evaluated through:

- source;
- title;
- excerpt;
- topic;
- content type;
- publication metadata.

Article imagery is not part of V1.

The application must not use:

- article thumbnails;
- fallback stock photography;
- generated images;
- random imagery;
- decorative images intended to manufacture relevance.

---

### 4.3 Editorial rather than administrative

The application should feel like a curated reading product rather than:

- an analytics dashboard;
- an enterprise administration interface;
- a generic SaaS product;
- a social-media feed.

Detailed visual rules are defined separately in `06-ui-ux.md` and `design-reference/DESIGN.md`.

---

### 4.4 Gestures are shortcuts, not requirements

Swipe gestures may provide fast article triage, but every gesture must have an explicit equivalent control.

The user must always be able to operate the core application without swiping.

---

### 4.5 Queue state must be honest

Counts displayed for Read Later and History must reflect actual local state immediately after a state-changing action.

The product must not use inflated, delayed, estimated, or engagement-oriented counts.

---

## 5. V1 Categories

Discover supports the following top-level categories:

1. Science
2. Technology
3. Literature
4. History
5. Weightlifting
6. IAM
7. Identity Automation

An additional **All** view combines eligible articles from every category.

Category assignment originates from configured sources and is not dynamically inferred by the personalization system.

Detailed source and taxonomy definitions are authoritative in:

- `03-content-sources.md`
- `04-taxonomy-scoring.md`

---

## 6. Primary Navigation

V1 has exactly three primary user destinations:

```text
Read Later | Discover | History
```

On mobile:

- Discover occupies the center position.
- Discover is the primary visual anchor.
- Read Later is on the left.
- History is on the right.

The underlying data model may refer internally to articles with a `read` state, but the user-facing destination is named **History**, not **Read**.

Settings is a secondary utility surface and is not a fourth primary navigation destination.

---

## 7. Core Product Concepts

### 7.1 Discover

Discover answers:

> What is worth my attention?

It presents one primary article at a time.

The user may:

- dismiss it;
- save it for later;
- open the original article;
- change category;
- undo the most recent swipe action.

Discover is a triage experience, not a conventional scrolling news feed.

---

### 7.2 Read Later

Read Later answers:

> What have I already decided is worth reading?

It is a compact reading queue.

Articles arrive in Read Later primarily when the user:

- swipes right; or
- activates the explicit Save for Later control.

Read Later is not presented as a swipe deck.

The user may:

- open an article;
- mark it read;
- remove it;
- return to Discover.

---

### 7.3 History

History answers:

> What have I already read?

It provides a chronological record of articles explicitly marked as read.

The user may:

- reopen an article;
- mark it unread.

Marking an article unread moves it to Read Later rather than returning it to Discover.

---

## 8. Discover Article Presentation

A Discover article should expose enough information to make a decision without requiring the full article to load inside the application.

A card may contain:

- source name;
- category;
- publication date or relative age;
- article title;
- concise excerpt;
- up to five detected topic tags;
- content-type label;
- estimated reading time when available;
- explicit Read Article action.

The application does not render or mirror full publisher article bodies.

The original publisher remains the reading destination.

---

## 9. Triage Semantics

The two primary triage decisions are deliberately simple.

### Swipe left / Not Interested

Meaning:

> This is not worth my attention.

Result:

- article leaves Discover;
- article becomes dismissed;
- relevant personalization signals are updated negatively;
- article does not normally reappear in Discover.

---

### Swipe right / Save for Later

Meaning:

> This is worth my time, but I am not necessarily reading it now.

Result:

- article leaves Discover;
- article enters Read Later;
- Read Later count increases immediately;
- relevant personalization signals are updated positively.

Swipe right does **not** mean that the article was read.

---

## 10. Reading an Article

The application provides an explicit **Read Article** action.

Selecting it:

- attempts to record that the article was opened before navigation;
- applies the appropriate personalization signal only once;
- preserves the local article snapshot;
- opens the original publisher URL.

If local persistence fails, the application warns that the Open interaction was not persisted but still opens the publisher URL. Reading must remain available when browser storage is unavailable. Other persistent queue/state actions must not be presented as successful when persistence fails.

Opening an article does not automatically mean the article was completed.

Therefore:

```text
opened ≠ read
```

The user must explicitly mark an article as read.

---

## 11. Article State Model

At the product level, articles may occupy these meaningful states:

```text
UNSEEN
OPENED
SAVED
DISMISSED
READ
```

Typical transitions include:

```text
UNSEEN
  ├── Not interested ──────────→ DISMISSED
  ├── Save for later ──────────→ SAVED
  └── Read article ────────────→ OPENED
```

From an opened article:

```text
OPENED
  ├── Save for later ──────────→ SAVED
  └── Mark read ───────────────→ READ
```

From Read Later:

```text
SAVED
  ├── Read article ────────────→ SAVED + opened metadata
  ├── Mark read ───────────────→ READ
  └── Remove ──────────────────→ DISMISSED
```

From History:

```text
READ
  └── Mark unread ─────────────→ SAVED
```

Detailed persistence semantics are authoritative in `05-personalization-state.md`.

---

## 12. Read Later Count

The Read Later count represents the number of articles currently in the saved reading queue.

It must:

- increase when an article is saved;
- decrease when a saved article is marked read;
- decrease when a saved article is removed;
- increase when a History article is marked unread;
- update immediately after each action.

Opening a saved article without marking it read does not remove it from Read Later.

---

## 13. History Count

The History count represents the number of locally retained articles explicitly marked as read.

It must:

- increase when an article is marked read;
- decrease if that article is subsequently marked unread;
- update immediately.

Opening an article is insufficient to increase History.

---

## 14. Undo

V1 supports undo for the **most recent swipe action**.

Undo may reverse:

- a dismissal;
- a Save for Later action.

Undo must:

- restore the affected article appropriately;
- reverse the corresponding personalization signal;
- update visible counts;
- restore the article to the active discovery flow where applicable.

Only the most recent eligible action needs to be undoable.

Persistent multi-action undo history is outside V1 scope.

---

## 15. Personalization Philosophy

The recommendation system exists to prioritize useful articles, not maximize engagement.

V1 personalization learns from:

- source preference;
- topic preference.

It does not learn a top-level category preference.

The user has already deliberately selected the available subject areas, so repeatedly rejecting articles within Technology, for example, must not cause the system to infer that Technology itself is unwanted.

The system should instead learn more specific patterns such as:

```text
distributed systems        ↑
security architecture      ↑
AI product announcements   ↓
```

Detailed ranking behavior is authoritative in:

- `04-taxonomy-scoring.md`
- `05-personalization-state.md`

---

## 16. Exploration and Intellectual Variety

Personalization must not collapse the application into a narrow echo chamber.

Articles from:

- unfamiliar sources;
- underexplored topics;
- less frequently shown categories

should retain reasonable opportunities to appear.

The application should maintain diversity without relying on opaque engagement optimization.

The user should continue encountering material outside their strongest learned preferences.

---

## 17. Content Quality Philosophy

V1 uses a curated source whitelist.

The primary content strategy is:

```text
curated sources
        +
primary/authoritative material
        +
deterministic scoring
        +
personal preference learning
```

The product does not attempt to determine whether an article was AI-written.

There is no AI-content detector in V1.

Quality is managed primarily by:

- source selection;
- source-quality weighting;
- primary-source preference;
- content-type weighting;
- topic relevance;
- user feedback.

---

## 18. No External AI Dependency

V1 requires no generative-AI API.

Specifically, V1 does not require:

- OpenAI API;
- Anthropic API;
- GPT classification;
- embedding APIs;
- hosted recommendation models.

All V1 ranking and personalization behavior must be deterministic and locally understandable.

Future AI enrichment may be considered separately, but the V1 architecture must not depend on it.

---

## 19. Short-Session Experience

The application should function well in sessions shorter than five minutes.

A successful session may consist of:

1. opening Discover;
2. evaluating two or three articles;
3. dismissing one;
4. saving one;
5. opening one useful article;
6. leaving the application.

The product should not imply that more interactions equal greater success.

A session in which the user discovers and reads one worthwhile article is considered successful.

A session in which the user decides that nothing is worth reading and leaves is also valid.

---

## 20. Read Later Is Not an Infinite Backlog

Read Later should provide awareness of queue size.

V1 does not impose a hard limit on saved articles.

The product may display:

- current queue count;
- estimated cumulative reading time where available.

V1 must not:

- prevent saving after an arbitrary threshold;
- shame the user for queue size;
- use urgent or guilt-oriented messaging.

A future version may explore soft backlog guidance if needed.

---

## 21. Settings

Settings is a compact secondary surface.

V1 Settings contains:

### Appearance

- Light
- Dark
- System

Appearance preference is stored locally.

System mode follows the device/browser color preference.

### Local data management

- Export local data
- Import local data
- Reset all data

The application must make it possible to back up and restore the local personalization and reading state without introducing remote synchronization.

---

## 22. Local-Only Data

V1 stores personalization and reading state locally.

This includes, as applicable:

- dismissed articles;
- saved articles;
- read history;
- source preferences;
- topic preferences;
- interaction timestamps;
- appearance preference.

No analytics or telemetry is required.

The application must not introduce:

- Google Analytics;
- tracking pixels;
- behavioral telemetry;
- remote user profiles;
- advertising identifiers.

---

## 23. Article Persistence

Articles that the user interacts with must remain meaningful even after they disappear from the current generated feed.

Therefore, interacted articles must retain sufficient local metadata for:

- Read Later;
- History;
- preference processing;
- export/import.

The generated feed is not itself the permanent reading-history database.

Detailed local persistence contracts are defined in `05-personalization-state.md`.

---

## 24. Loading, Empty, and Failure Behavior

The product must behave intentionally when content is unavailable.

### Loading

Communicate that the reading queue is being prepared without using high-energy or engagement-oriented animation.

### No new articles

The interface should explicitly communicate that there is nothing requiring attention.

The user should be free to leave.

Read Later may be offered as a secondary destination.

### Feed degradation or failure

Failure to load new Discover content must not prevent access to locally stored:

- Read Later;
- History;
- Settings/data export.

Feed problems should not imply that locally stored reading data has been lost.

---

## 25. Accessibility at the Product Level

Core functionality must never depend solely on gestures.

V1 requires equivalent accessible controls for:

- dismiss;
- save;
- open;
- undo;
- navigation;
- Settings.

State changes must be perceivable without relying solely on color.

Detailed interaction, keyboard, contrast, and motion requirements are authoritative in:

- `06-ui-ux.md`
- `design-reference/DESIGN.md`

---

## 26. Responsive Product Behavior

V1 is mobile-first but must remain fully usable on desktop.

Mobile and desktop may use different navigation presentation, but they must preserve the same product semantics.

The desktop experience must not become a separate dashboard product.

The same core destinations remain:

```text
Read Later
Discover
History
```

Detailed responsive behavior belongs to `06-ui-ux.md`.

---

## 27. V1 Functional Scope

V1 includes:

- curated multi-source article ingestion;
- seven subject categories;
- All-category discovery;
- text-first Discover cards;
- deterministic article scoring;
- deterministic topic tagging;
- swipe-left dismissal;
- swipe-right Save for Later;
- explicit equivalent triage buttons;
- explicit Read Article action;
- Read Later queue;
- explicit Mark Read;
- History;
- Mark Unread;
- most-recent-action undo;
- source/topic preference learning;
- diversity sequencing;
- exploration bonuses;
- local persistence;
- export/import/reset;
- Light/Dark/System appearance;
- debug ranking mode;
- responsive mobile and desktop UI;
- graceful degraded-feed behavior.

---

## 28. Explicit V1 Non-Goals

The following are outside V1 scope:

- user accounts;
- authentication;
- multi-user support;
- cloud synchronization;
- cross-device synchronization;
- backend application server;
- database;
- OpenAI API integration;
- Anthropic API integration;
- generative-AI summaries;
- AI article generation;
- AI-content detection;
- embeddings;
- machine-learning recommendation models;
- social features;
- comments;
- likes;
- sharing feeds between users;
- follower systems;
- article images;
- full article scraping;
- full article mirroring;
- paywall bypass;
- browser extension;
- native mobile application;
- notifications;
- push notifications;
- email digests;
- telemetry;
- advertising;
- monetization;
- streaks;
- gamification;
- infinite scroll;
- automatic cross-device backups.

`native mobile application` above scopes the V1 web deliverable. Approved Amendment 6, **Native Android Client Authorization** (see `README.md`), permits a separate native Android client that consumes the frozen `ArticleDataset v1` contract read-only and is delivered incrementally under numbered specification items. It changes no other non-goal in this section, no browser runtime rule, and no V1 web requirement.

---

## 29. Product Success Criteria

V1 succeeds if the user can reliably:

1. open the application during a short downtime session;
2. encounter articles from deliberately curated sources;
3. quickly understand what each article is about;
4. reject irrelevant material with minimal effort;
5. save worthwhile material without claiming it has been read;
6. open the original publisher when ready to read;
7. explicitly record completed reading;
8. revisit saved and previously read material;
9. observe the queue adapt gradually to source/topic preferences;
10. retain enough variety to discover unfamiliar material;
11. preserve all personal reading state locally;
12. use the application without accounts, API keys, backend infrastructure, or external AI services.

The product should feel successful because it helps the user spend attention intentionally—not because it maximizes time spent inside the application.

---

## 30. Authoritative Related Specifications

This document defines **what V1 is and how it should behave as a product**.

Implementation details are defined elsewhere:

- `02-architecture.md` — system architecture and module boundaries
- `03-content-sources.md` — approved publishers, feeds, adapters, and admission rules
- `04-taxonomy-scoring.md` — taxonomy, tagging, normalization, and base scoring
- `05-personalization-state.md` — local state, preferences, ranking, and article transitions
- `06-ui-ux.md` — detailed UI, visual, responsive, and interaction specification
- `07-pipeline-deployment.md` — ingestion pipeline and GitHub Pages deployment
- `08-security-dependencies.md` — security boundaries and dependency policy
- `09-testing-acceptance.md` — automated tests and final acceptance requirements
- `design-reference/DESIGN.md` — approved visual design system

If a lower-level implementation document conflicts with a product requirement in this document, the conflict must be escalated to the supervisor rather than silently resolved by an implementation agent.
