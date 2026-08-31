# Intentional Reading — V1 Cross-Agent Contracts

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/contracts.md`\
**Role:** Authoritative shared contract specification\
**Applies to:** Content Pipeline, State/Ranking, Frontend UI, Integration

---

## 1. Purpose

This document freezes the data structures, identifiers, state semantics, module interfaces, and cross-workstream assumptions required for independent parallel implementation.

After this document is marked **Approved**, feature agents must not alter these contracts without supervisor approval.

If implementation reveals that a contract is insufficient or contradictory, the responsible agent must stop, document the issue, and escalate it rather than silently inventing a replacement.

---

# 2. Contract Versioning

V1 uses two independently versioned persisted formats:

```text
Generated article dataset: schemaVersion = 1
Browser local state:       schemaVersion = 1
```

These versions are independent.

Future changes to one do not automatically require changing the other.

---

# 3. Canonical Category IDs

Internal category IDs are exact and stable:

```text
science
technology
literature
history
weightlifting
iam
identity_automation
```

The synthetic Discover filter:

```text
all
```

is a UI/ranking filter and is **not** an article category.

Display labels are:

```text
science              → Science
technology           → Technology
literature           → Literature
history              → History
weightlifting        → Weightlifting
iam                   → IAM
identity_automation  → Identity Automation
```

Implementation agents must not invent alternate IDs such as:

```text
identity-automation
identityAutomation
tech
weights
```

---

# 4. Generated Dataset Contract

The Python pipeline generates:

```text
data/articles.json
```

with this top-level structure:

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-08-16T23:00:00Z",
  "pipeline": {
    "enabledSourceCount": 20,
    "successfulSourceCount": 19,
    "failedSourceCount": 1,
    "articleCount": 214
  },
  "articles": []
}
```

## Required dataset fields

### `schemaVersion`

Integer.

V1 value:

```text
1
```

### `generatedAt`

UTC ISO-8601 timestamp.

### `pipeline`

Informational build metadata.

Required fields:

```text
enabledSourceCount
successfulSourceCount
failedSourceCount
articleCount
```

The frontend may display degraded-data information based on this metadata but must not infer detailed source failures that are not present in the contract.

### `articles`

Array of valid Article objects defined below.

---

# 5. Article Contract

Every generated article must conform conceptually to:

```json
{
  "id": "60bca89ea70f36ddc822",

  "title": "OAuth 2.1 Authorization Framework Moves Forward",
  "url": "https://example.org/article",

  "source": {
    "id": "ietf_oauth",
    "name": "IETF OAuth WG"
  },

  "category": "iam",

  "publishedAt": "2026-08-15T18:30:00Z",
  "author": "Example Author",

  "excerpt": "A concise normalized plain-text description of the article.",

  "readingTimeMinutes": null,

  "tags": [
    {
      "id": "oauth",
      "label": "OAuth"
    },
    {
      "id": "authorization",
      "label": "Authorization"
    }
  ],

  "contentType": {
    "id": "standards_update",
    "label": "Standards Update"
  },

  "score": {
    "base": 91,
    "sourceQuality": 50,
    "contentType": 20,
    "freshness": 13,
    "topicSignal": 6,
    "metadata": 2
  }
}
```

---

# 6. Article Field Requirements

## `id`

Required string.

The pipeline generates it from the canonicalized article identity.

V1 stable-ID algorithm:

```text
canonical article URL
        ↓
UTF-8 encode
        ↓
SHA-256
        ↓
first 20 lowercase hexadecimal characters
```

Example:

```text
60bca89ea70f36ddc822
```

A generated V1 Article must have a usable canonical external URL.

If normalization cannot produce a usable external HTTP or HTTPS URL, the
source entry must be rejected and no Article object may be emitted.

No GUID-, title-, or publication-date-based fallback article identity is used
in V1.

Once calculated, the article ID must be deterministic for the same canonical
article URL.

---

## `title`

Required non-empty plain-text string.

Remote HTML markup must not survive normalization.

---

## `url`

Required canonical external article URL after normalization.

Tracking parameters and fragments should be removed according to `04-taxonomy-scoring.md`.

The frontend treats this value as untrusted external input.

---

## `source`

Required object:

```json
{
  "id": "ietf_oauth",
  "name": "IETF OAuth WG"
}
```

`id` is the stable source identifier from `config/sources.json`.

`name` is the user-facing publisher/source name.

---

## `category`

Required canonical category ID from Section 3.

---

## `publishedAt`

UTC ISO-8601 timestamp or `null`.

If the source does not provide a trustworthy publication date:

```json
"publishedAt": null
```

The pipeline must not invent a publication date.

---

## `author`

Plain-text author string or `null`.

No synthetic author should be generated.

---

## `excerpt`

Required plain-text string.

It may be empty when the source provides no useful summary:

```json
"excerpt": ""
```

Publisher-supplied HTML must be normalized to safe text before dataset generation.

---

## `readingTimeMinutes`

Positive integer or `null`.

V1 must not invent reading time from unavailable full article bodies.

If enough source-supplied content exists to calculate a meaningful estimate, the pipeline may populate it.

Otherwise:

```json
"readingTimeMinutes": null
```

---

# 7. Topic Tag Contract

`tags` is always an array.

Each tag is:

```json
{
  "id": "passkeys_webauthn",
  "label": "Passkeys / WebAuthn"
}
```

Rules:

- `id` must exist in `config/topics.json`;
- `label` comes from the same taxonomy entry;
- order represents descending detected relevance;
- organically detected tags are limited to five;
- duplicate topic IDs are prohibited.

The frontend must use `tag.id` for personalization and `tag.label` for display.

It must not reverse-engineer labels from IDs.

---

# 8. Content-Type Contract

`contentType` is:

```json
{
  "id": "engineering_deep_dive",
  "label": "Engineering Deep Dive"
}
```

The pipeline owns content-type assignment.

The frontend:

- displays `label`;
- may use `id` for semantic classes or filtering;
- must not independently reclassify content.

The exact approved content-type vocabulary is defined in `03-content-sources.md`.

---

# 9. Base Score Contract

Article base score consists of exactly:

```text
sourceQuality    0–50
contentType      0–20
freshness        0–15
topicSignal      0–10
metadata         0–5
─────────────────────
base             0–100
```

Invariant:

```text
base =
    sourceQuality
  + contentType
  + freshness
  + topicSignal
  + metadata
```

All generated score components are integers in V1.

The browser may calculate additional personalized components but must never mutate the persisted/generated base score.

---

# 10. Source Configuration Contract

`config/sources.json` contains an array of source objects.

Conceptual structure:

```json
{
  "sources": [
    {
      "id": "ietf_oauth",
      "name": "IETF OAuth WG",
      "category": "iam",

      "adapter": "atom",
      "url": "https://datatracker.ietf.org/group/oauth/documents/feed/?significant=1",

      "quality": 50,

      "contentType": {
        "id": "standards_update",
        "label": "Standards Update",
        "score": 20
      },

      "enabled": true,

      "minTopicMatches": 0,

      "admissionTopicIds": [],

      "forcedTags": [
        "oauth"
      ]
    }
  ]
}
```

---

# 11. Source Configuration Required Fields

Every source must define:

```text
id
name
category
adapter
url
quality
contentType
enabled
minTopicMatches
admissionTopicIds
forcedTags
```

Allowed `adapter` values in V1:

```text
rss
atom
rss_autodiscovery
html_listing
```

`quality` must be an integer:

```text
0–50
```

`contentType.score` must be an integer:

```text
0–20
```

`admissionTopicIds` is an array of topic IDs allowed to satisfy source-specific admission filtering.

An empty array means normal category-scoped topic matching is used where a minimum is configured.

`forcedTags` is an array of approved topic IDs.

Forced tags do not count toward `minTopicMatches` and do not artificially increase organic `topicSignal`.

---

# 12. Topic Configuration Contract

`config/topics.json` contains:

```json
{
  "topics": [
    {
      "id": "passkeys_webauthn",
      "label": "Passkeys / WebAuthn",

      "categories": [
        "iam"
      ],

      "aliases": [
        "passkey",
        "passkeys",
        "webauthn",
        "fido2",
        "phishing-resistant authentication"
      ]
    }
  ]
}
```

Required fields:

```text
id
label
categories
aliases
```

A topic may participate in more than one category.

Example:

```json
{
  "id": "scim",
  "label": "SCIM",
  "categories": [
    "iam",
    "identity_automation"
  ],
  "aliases": [
    "scim",
    "system for cross-domain identity management"
  ]
}
```

---

# 13. Dataset Loader Contract

Frontend module:

```text
js/data/articles.js
```

must export a function conceptually equivalent to:

```javascript
async function loadArticleDataset(
  url = "./data/articles.json"
)
```

Resolved value:

```javascript
{
  schemaVersion,
  generatedAt,
  pipeline,
  articles
}
```

The implementation may use named exports/classes internally, but the workstream must provide one clear public dataset-loading function.

The loader must:

- fetch only the static dataset;
- reject unsupported schema versions;
- reject structurally unusable datasets;
- return predictable errors;
- perform no personalization;
- perform no localStorage mutation.

---

# 14. Browser Local-State Root

All V1 persistent browser state exists under one logical localStorage entry:

```text
intentionalReading:v1
```

The serialized root structure is:

```json
{
  "schemaVersion": 1,

  "preferences": {
    "sources": {},
    "topics": {}
  },

  "articles": {},

  "settings": {
    "appearance": "system"
  },

  "session": {
    "lastCategory": "all"
  }
}
```

No other module may create independent persistent application-state keys without a specification revision.

Temporary browser mechanisms unrelated to application state are outside this contract.

---

# 15. Preference Entry Contract

Source and topic preference entries use the same structure:

```json
{
  "weight": 1.45,
  "interactions": 7
}
```

Rules:

```text
weight minimum = -5.0
weight maximum = +5.0
interactions minimum = 0
```

`interactions` represents currently applied learning signals affecting that source/topic.

If an Undo or corrective state transition reverses a learning signal, the associated interaction count is reversed as well.

---

# 16. Persisted Article Record

The `articles` map is keyed by article ID.

Example:

```json
{
  "60bca89ea70f36ddc822": {
    "article": {
      "...": "complete Article snapshot"
    },

    "status": "saved",

    "firstSeenAt": "2026-08-16T18:21:00Z",
    "openedAt": null,
    "savedAt": "2026-08-16T18:22:12Z",
    "dismissedAt": null,
    "readAt": null,

    "signalsApplied": {
      "opened": false,
      "saved": true,
      "dismissed": false,
      "read": false
    }
  }
}
```

The `article` field stores a complete Article snapshot conforming to the generated Article contract.

This ensures Read Later and History survive removal of the article from future generated datasets.

---

# 17. Local Article Status Contract

Persisted `status` is exactly one of:

```text
opened
saved
dismissed
read
```

An untouched/unseen article has **no persisted record**.

Therefore `unseen` is a logical state but is not a persisted status value.

---

# 18. Status Semantics

## `opened`

The user opened an article from Discover without subsequently saving, dismissing, or marking it read.

An `opened` article remains eligible for Discover.

---

## `saved`

The article belongs to Read Later.

An opened saved article remains:

```text
status = saved
```

with `openedAt` populated.

---

## `dismissed`

The article is excluded from Discover and is not in Read Later or History.

---

## `read`

The article belongs to History.

---

# 19. Timestamp Contract

Article interaction timestamps are:

```text
firstSeenAt
openedAt
savedAt
dismissedAt
readAt
```

All non-null timestamp values are UTC ISO-8601 strings.

`null` means the corresponding event has not occurred or is not currently applicable.

`firstSeenAt` is set when the first persistent interaction record is created and does not change afterward.

---

# 20. Learning Signal Contract

Applied learning signals are tracked independently of current status:

```json
{
  "opened": false,
  "saved": false,
  "dismissed": false,
  "read": false
}
```

This prevents duplicate preference updates.

Status does not imply that every same-named signal must exist.

Example:

An article may become:

```text
status = saved
signalsApplied.saved = false
```

when a previously read article is marked unread and returned to Read Later.

That transition changes queue state but is not itself a new Save-for-Later preference signal.

---

# 21. V1 Preference Deltas

Exact V1 learning deltas are:

| Event | Source | Each topic |
|---|---:|---:|
| Not Interested | -0.35 | -0.20 |
| Save for Later | +0.45 | +0.30 |
| First Open | +0.10 | +0.05 |
| Mark Read | +0.25 | +0.20 |

Weights are clamped to:

```text
[-5.0, +5.0]
```

These values are authoritative for V1.

---

# 22. Idempotency Rules

Each learning signal may be applied at most once at a time for a given article.

Examples:

Opening an article five times applies:

```text
First Open = once
```

not five times.

Saving an already-saved article does not reapply the Save signal.

Marking an already-read article read does not reapply the Read signal.

---

# 23. Reversible Learning Rules

The following corrective actions reverse signals:

### Undo Not Interested

Reverse:

```text
dismissed signal
```

and decrement corresponding interaction counts.

### Undo Save for Later

Reverse:

```text
saved signal
```

and decrement corresponding interaction counts.

### Mark Unread

If a Read signal had previously been applied:

```text
reverse read signal
signalsApplied.read = false
```

The article moves to:

```text
status = saved
```

This does **not** apply a new Save signal.

### Undo Mark Read

If the Mark Read action applied a Read signal:

```text
reverse read signal
```

and decrement corresponding interaction counts. If it applied none, reverse nothing.

The exact previous record is restored, including `signalsApplied.read`.

### Undo Mark Unread

Mark Unread is itself a corrective action. If it reversed a Read signal, undoing it **re-applies** that signal:

```text
apply read signal
signalsApplied.read = true
```

and increments corresponding interaction counts. If Mark Unread reversed nothing, undoing it applies nothing.

The article returns to:

```text
status = read
```

This is the only reversal in V1 that applies a signal rather than reversing one.

### Undo Remove from Read Later

Remove applies no preference signal (§24), so undoing it applies and reverses **nothing**.

The exact previous record is restored:

```text
status = saved
```

*Amended by Amendment 8.*

---

# 24. Remove from Read Later

Removing an article from Read Later changes:

```text
status → dismissed
```

but does **not** apply the negative Not Interested preference signal.

Reason:

Removing an item from a backlog does not necessarily mean the topic/source is unwanted.

Any previously applied Save/Open signals remain applied.

---

# 25. Opening Behavior

First open:

1. derive the Open-state update, including the article snapshot if necessary;
2. set `openedAt` if not already set;
3. apply First Open signal if not previously applied;
4. preserve existing `saved` or `read` status where applicable;
5. attempt local persistence;
6. navigate to the external publisher whether persistence succeeds or fails.

If persistence succeeds, retain the Open interaction normally before navigation. If persistence fails, report a local-state warning and do not claim that the Open interaction was persisted, but still proceed to the publisher. Reading must not become unavailable merely because browser storage is unavailable.

This navigation exception applies only to Open/external reading. Persistent queue/state actions such as Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset must not be presented as successful when persistence fails.

If the article was previously unseen:

```text
status = opened
```

If it was already saved:

```text
status remains saved
```

If reopened from History:

```text
status remains read
```

---

# 26. Discover Eligibility Contract

An article is eligible for Discover when:

```text
no persisted record exists
```

or:

```text
status = opened
```

An article is excluded from Discover when:

```text
status = saved
status = dismissed
status = read
```

This rule is authoritative for `js/ranking/deck.js`.

---

# 27. Read Later Eligibility Contract

Read Later contains exactly persisted article records where:

```text
status = saved
```

Default ordering:

```text
savedAt descending
```

If `savedAt` is null because an article reached Saved through Mark Unread, the transition must set `savedAt` to the current timestamp.

---

# 28. History Eligibility Contract

History contains exactly persisted article records where:

```text
status = read
```

Default ordering:

```text
readAt descending
```

---

# 29. Appearance Contract

Persistent appearance value is exactly:

```text
light
dark
system
```

Default:

```text
system
```

No other theme identifiers are valid in V1.

---

# 30. Session Contract

V1 persists:

```json
{
  "lastCategory": "all"
}
```

Valid values:

```text
all
science
technology
literature
history
weightlifting
iam
identity_automation
```

Other ephemeral session state does not need persistence unless explicitly added to a specification.

---

# 31. Undo Contract

Undo state is **not persisted to localStorage**.

It exists only in active application memory.

Conceptually:

```javascript
{
  articleId,
  action,
  previousRecord,
  preferenceReversalData
}
```

Only the most recent eligible action must be retained, regardless of which surface or destination performed it. The reversible set is defined in §23.

*Amended by Amendment 8.*

Reloading the page clears Undo availability.

---

# 32. Storage Module Contract

`js/state/storage.js` is the only application module that directly accesses `localStorage`.

It must provide public capabilities conceptually equivalent to:

```javascript
loadState()
saveState(state)
resetState()

exportState()
importState(serializedState)
```

It may expose additional helper functions if useful, but other modules must not bypass it.

`loadState()` must always return a valid current-version logical state or a recoverable error/default-state outcome.

---

# 33. Preferences Module Contract

`js/state/preferences.js` must expose semantic preference operations rather than requiring callers to manipulate weights.

Conceptual public operations:

```javascript
applyInteraction(preferences, article, event)
reverseInteraction(preferences, article, event)
```

Valid learning events are:

```text
open
save
dismiss
read
```

The module owns:

- exact delta tables;
- clamping;
- interaction counts;
- duplicate-signal protection support;
- reversal math.

It must not own article status transitions.

---

# 34. Personalized Score Contract

`js/ranking/personalize.js` returns a breakdown conceptually equivalent to:

```javascript
{
  total: 94.4,
  base: 87,
  sourcePreference: 2.4,
  topicPreference: 3.0,
  exploration: 2.0
}
```

The exact V1 formula is finalized in `05-personalization-state.md`.

Mandatory properties of the function:

- deterministic;
- side-effect free;
- does not mutate Article;
- does not mutate preferences;
- exposes score components for debug mode.

Diversity is intentionally **not** part of this per-article score object.

---

# 35. Diversity Contract

Diversity belongs to deck sequencing, not persisted scoring.

`js/ranking/deck.js` receives ranked candidates and constructs an ordered sequence.

It may temporarily prefer:

- a different source from the previous card;
- category variety in the All view.

It must not modify:

```text
Article.score.base
persistent source preferences
persistent topic preferences
```

Diversity adjustments are session/deck sequencing behavior only.

---

# 36. Deck Builder Contract

Conceptual public interface:

```javascript
buildDeck({
  articles,
  state,
  category
})
```

Returns an ordered array of candidate Article objects or deck entries.

Responsibilities:

1. apply Discover eligibility;
2. apply category filter;
3. compute personalized score;
4. sort;
5. sequence for diversity;
6. return deterministic deck for identical inputs.

If deck entries include score metadata for debug mode, the original Article object must remain logically immutable.

---

# 37. Semantic UI Action Contract

UI modules communicate intent to the integration/application coordinator using semantic actions.

Canonical action names:

```text
dismiss
save
open
mark_read
mark_unread
remove
undo
navigate
category_change
appearance_change
export_data
import_data
reset_data
```

UI modules must not mutate persistent state directly.

---

# 38. State Transition Result Contract

State-changing operations return a result that makes success or failure explicit and provides the current logical state needed for rendering. Failed operations must not leave partially updated preferences, article records, counts, or timestamps.

Queue and History counts are derived from the committed result; they are never decorative or estimated.

---

# 39. External Navigation Contract

Opening an Article uses its canonical external `url` in a new browsing context with opener isolation equivalent to:

```text
target="_blank"
rel="noopener noreferrer"
```

Only normalized HTTP and HTTPS URLs are permitted.

The application attempts Open-state persistence before navigation. A persistence failure produces a local-state warning and no false persisted-success claim, but it must not block navigation to the valid publisher URL.

---

# 40. Error Contract

Public data/state modules must distinguish recoverable user-facing failures from programmer/configuration errors. Errors must not expose feed payloads, arbitrary remote markup, local-state contents, or secrets.

The UI must provide truthful loading, empty, degraded, and unavailable states without fabricating Articles.

---

# 41. Contract Ownership

No workstream may unilaterally change:

- Article fields;
- category or content-type identifiers;
- localStorage root/schema;
- persisted status values;
- preference deltas;
- Discover eligibility;
- action names;
- scoring component meanings.

Any conflict must be escalated to the supervisor and resolved by an explicit specification amendment.

---

# 42. Contract Completion Criteria

The contracts are satisfied when:

- pipeline output validates against the Article and dataset contracts;
- browser state round-trips through storage without semantic loss;
- ranking remains deterministic and side-effect free;
- UI actions map to the defined state transitions;
- Read Later and History use persisted Article snapshots;
- malformed or unsupported input fails safely;
- Contract Amendment 1 is enforced for every emitted Article URL and ID.
