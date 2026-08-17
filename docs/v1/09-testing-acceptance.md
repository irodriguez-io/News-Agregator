# Intentional Reading — V1 Testing and Acceptance Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/09-testing-acceptance.md`\
**Role:** Authoritative automated testing, integration verification, manual acceptance, release gates, and V1 completion criteria

---

## 1. Purpose

This document defines how Intentional Reading V1 is verified before implementation work is considered complete.

Testing must prove:

- deterministic content processing;
- stable shared contracts;
- correct article-state transitions;
- correct preference learning;
- correct ranking;
- safe persistence;
- graceful source failures;
- secure handling of untrusted data;
- accurate UI state;
- responsive behavior;
- accessibility;
- production deployment readiness.

V1 testing should remain proportional to the application's architecture.

The project must not introduce a large browser-testing framework solely to satisfy artificial coverage targets.

---

# 2. Testing Principles

V1 follows these principles:

```text
test deterministic logic automatically
        +
test contracts explicitly
        +
test remote parsers with local fixtures
        +
test integrated user flows
        +
verify visual behavior manually
        +
keep live-network validation separate
```

The test suite should answer:

> Does our code behave according to the approved specification?

It should not depend unnecessarily on:

> Is every publisher online right now?

---

# 3. Test Layers

V1 uses five verification layers:

```text
1. Unit tests
2. Contract tests
3. Integration tests
4. Manual UI/accessibility acceptance
5. Production/deployment verification
```

Not every requirement belongs in every layer.

---

# 4. Test Ownership

Each workstream owns tests for the behavior it implements.

## Content Pipeline

Owns:

```text
tests/pipeline/**
```

including:

- normalization;
- URL handling;
- source configuration;
- taxonomy;
- admission;
- deduplication;
- scoring;
- adapter fixtures;
- retention;
- dataset generation.

---

## State and Ranking

Owns:

```text
tests/js/**
```

for:

- dataset loading;
- local-state logic;
- preference learning;
- status transitions;
- persistence validation;
- import/export;
- personalization;
- exploration;
- deck sequencing.

---

## Frontend UI

Owns verification for:

- rendering;
- interactions;
- swipe behavior;
- navigation;
- responsive presentation;
- accessibility;
- visual conformity to the approved design.

Automated DOM testing is not required if the same acceptance can be reliably verified through browser/manual integration testing without adding unnecessary dependencies.

---

## Integration

Owns:

- cross-module wiring;
- end-to-end state flows;
- production artifact verification;
- full release-gate execution.

---

# 5. Python Test Framework

Python tests use:

```text
pytest
```

Preferred invocation:

```text
python -m pytest
```

The pipeline test suite must not require internet access unless a test is explicitly classified as live validation.

---

# 6. JavaScript Test Framework

Preferred V1 framework:

```text
Node.js built-in node:test
```

Preferred invocation:

```text
node --test tests/js
```

or an equivalent deterministic path supported by the repository.

Third-party test frameworks such as:

```text
Vitest
jsdom
```

may be introduced only under the exception policy defined in:

```text
08-security-dependencies.md
```

---

# 7. Test Isolation

Automated tests must not depend on:

- the user's real browser localStorage;
- the live production dataset;
- current publisher availability;
- current date/time without controlled injection;
- external AI services;
- random ranking;
- production GitHub Pages state.

Tests should inject or fixture these inputs explicitly.

---

# 8. Controlled Time

Logic involving:

```text
freshness
generatedAt
savedAt
readAt
openedAt
dismissedAt
```

must be testable with controlled timestamps.

Tests must not assume the real current clock.

Functions should accept a time input or isolate clock access sufficiently for deterministic testing.

---

# 9. No Random Test Ordering Dependency

Because V1 ranking is deterministic, tests must not rely on random shuffling.

A repeated test using the same:

```text
dataset
state
category
time
```

must produce identical results.

---

# 10. Pipeline Fixture Strategy

Remote formats are represented through local fixtures under:

```text
tests/pipeline/fixtures/
```

Fixtures should contain only enough publisher material to test parser behavior.

Examples:

```text
quanta.xml
science_aaas.xml
ietf_oauth.atom
ietf_scim.atom
anthropic_engineering.html
barbell_medicine.html
okta_workflows.html
```

Additional fixtures may be added where source behavior materially differs.

---

# 11. Fixture Safety

Fixtures must not contain unnecessary complete copyrighted article bodies.

Prefer:

- abbreviated feed entries;
- representative HTML structures;
- minimal excerpts;
- fabricated or shortened article text where adequate.

Fixtures exist to test parsing structure, not mirror publisher content.

---

# 12. Configuration Tests

Automated tests must verify that valid V1 configuration loads successfully.

Invalid fixtures must test rejection of:

- duplicate source IDs;
- duplicate topic IDs;
- invalid category IDs;
- unknown adapter IDs;
- out-of-range source quality;
- out-of-range content-type scores;
- unknown forced tags;
- unknown admission topic IDs;
- empty topic aliases;
- duplicate normalized aliases;
- ambiguous same-category aliases.

Configuration errors must fail before article generation.

---

# 13. URL Canonicalization Tests

Required cases include:

### Tracking removal

Input:

```text
https://example.com/article?utm_source=rss&utm_campaign=test
```

Expected:

```text
https://example.com/article
```

### Fragment removal

Input:

```text
https://example.com/article#comments
```

Expected:

```text
https://example.com/article
```

### Query preservation

Input:

```text
https://example.com/article?id=123&lang=en
```

must preserve meaningful parameters.

### Stable query order

Equivalent parameter orderings must canonicalize identically.

### Default ports

```text
https://example.com:443/article
```

must normalize to the default-port-free form.

### Unsafe protocol

```text
javascript:
data:
file:
ftp:
```

must be rejected.

---

# 14. Article ID Tests

Tests must verify:

```text
same canonical URL
→ same 20-character ID
```

and:

```text
different canonical URL
→ different expected identity
```

Expected IDs should be asserted against known fixtures rather than merely checking length.

---

# 15. Plain-Text Normalization Tests

Representative hostile publisher input:

```html
<script>alert(1)</script>
<strong>OAuth Update</strong>
```

must normalize to inert text without executable markup.

Tests must cover:

- HTML entities;
- repeated whitespace;
- embedded script/style elements;
- Unicode normalization;
- excessive title length;
- excessive excerpt length.

---

# 16. Publication Date Tests

Required cases:

- valid timestamp with timezone;
- valid UTC timestamp;
- missing date;
- invalid date;
- date within six hours in the future;
- date more than six hours in the future.

Expected results must follow `04-taxonomy-scoring.md` exactly.

---

# 17. Reading-Time Tests

Tests must verify:

```text
<400 source-supplied words
→ null
```

and:

```text
>=400 source-supplied words
→ ceil(words / 225)
```

The minimum emitted estimate remains:

```text
2 minutes
```

Short excerpt text must not produce a fabricated reading-time estimate.

---

# 18. Taxonomy Matching Tests

Required behavior:

### Whole token

Alias:

```text
ai
```

matches:

```text
AI agents
```

but does not match:

```text
mail
said
chair
```

### Whole phrase

```text
openid connect
```

must match the normalized phrase but not disjoint words.

### Case independence

```text
OAuth
oauth
OAUTH
```

must classify equivalently.

### Punctuation normalization

```text
OAuth 2.1
```

must match the approved normalized alias.

---

# 19. Category Scope Tests

Required example:

Technology article:

```text
Programming language design
```

must not receive the Weightlifting:

```text
programming
```

topic.

Likewise, category-specific aliases must remain scoped.

---

# 20. Topic Evidence Tests

For one topic:

```text
title match only
→ evidence = 3

excerpt match only
→ evidence = 1

title + excerpt
→ evidence = 4
```

Multiple aliases of the same topic within one field must not stack.

---

# 21. Topic Signal Tests

Examples must verify:

```text
4 + 3
→ topicSignal 7
```

and:

```text
4 + 4 + 3
→ raw 11
→ topicSignal 10
```

Forced tags must not alter `topicSignal`.

---

# 22. Forced Tag Tests

Required:

```text
ietf_oauth
→ oauth
```

```text
w3c_webauthn
→ passkeys_webauthn
```

```text
ietf_scim
→ scim
```

Forced tags must:

- appear in final Article tags;
- not satisfy topic admission;
- not raise `topicSignal`;
- not duplicate an organically detected tag.

---

# 23. Admission Filter Tests

### OpenAI

Article with approved technical topic:

```text
PASS
```

Article with only unrelated product announcement content:

```text
REJECT
```

### Barbell Medicine

Training-related article:

```text
PASS
```

Unrelated medical article:

```text
REJECT
```

### Entra

Relevant lifecycle/provisioning/governance article:

```text
PASS
```

Unrelated Entra announcement:

```text
REJECT
```

---

# 24. Deduplication Tests

Required cases:

### Exact canonical URL duplicate

Must collapse.

### Same-source near duplicate

When both valid publication dates exist:

```text
normalized title similarity >= 0.92
AND
publication dates within 14 days
→ collapse
```

If either publication date is unavailable:

```text
normalized title similarity >= 0.97
→ collapse
```

The recommended implementation is Python standard-library `difflib.SequenceMatcher`.

### Same-source outside date window

When both dates exist and are more than 14 days apart:

```text
retain both
```

### Similar but below threshold

```text
retain both
```

### Cross-source exact normalized title within 72 hours

When both valid publication timestamps exist:

```text
collapse
```

using approved winner rules.

### Cross-source exact title outside 72 hours

```text
retain both
```

### Cross-source similar but non-identical normalized titles

```text
retain both
```

### Unknown cross-source date

Do not perform title-only cross-publisher deduplication.

---

# 25. Duplicate Winner Tests

Verify winner priority:

```text
source quality
→ metadata score
→ newer valid publication date
→ richer non-empty excerpt
→ deterministic source/article-ID tie-break
```

Content-type score must not participate in duplicate-winner selection.

The implementation must never synthesize a combined article from duplicate candidates.

---

# 26. Metadata Score Tests

Required exact cases:

```text
valid non-null date  +2
excerpt >=80         +2
excerpt 1–79         +1
excerpt 0            +0
non-empty author     +1
readingTimeMinutes   +0
------------------------
maximum               5
```

and boundary tests:

```text
excerpt length 0
excerpt length 79
excerpt length 80
```

Expected excerpt contributions are `+0`, `+1`, and `+2`, respectively. `readingTimeMinutes` contributes `0` metadata points whether it is null or populated.

---

# 27. Freshness Tests

Exact boundaries must be tested for:

```text
1 day
3 days
7 days
14 days
30 days
unknown publication date
```

Expected scores:

```text
15
13
10
7
4
1
5 for unknown
```

according to interval rules in `04-taxonomy-scoring.md`.

---

# 28. Base Score Tests

Known fixture values must verify:

```text
base =
sourceQuality
+ contentType
+ freshness
+ topicSignal
+ metadata
```

Score invariant:

```text
0 <= base <= 100
```

must hold.

A generated Article whose component sum does not equal `base` must fail dataset validation.

---

# 29. Retention Tests

Required cases:

### Age

Known-date article older than 45 days:

```text
removed
```

### Unknown date

Not removed solely by age.

### Per-source cap

Source with 45 eligible items:

```text
retain 40
```

using required ordering.

### Global cap

Dataset with >500 articles:

```text
retain 500
```

using required deterministic ordering.

---

# 30. Source Failure Tests

Pipeline tests must simulate:

- timeout;
- HTTP failure;
- malformed feed;
- empty parsed source;
- successful source with zero admitted articles;
- source-specific parser exception.

Expected distinction:

```text
empty parse
→ source failed

parsed entries but admission removes all
→ source successful
```

---

# 31. Catastrophic Dataset Tests

Required cases:

```text
19 retained articles
→ generation fails
```

```text
20 retained articles
→ article-count gate passes
```

For 22 enabled sources:

```text
10 successful
→ fails

11 successful
→ passes source-ratio gate
```

assuming other gates pass.

---

# 32. Atomic Output Test

A simulated output-write failure must not leave a truncated:

```text
data/articles.json
```

in place of a previously valid file.

Where practical, test the temporary-file/replace behavior directly.

---

# 33. Dataset Contract Tests

Generated dataset must validate:

- `schemaVersion = 1`;
- valid `generatedAt`;
- pipeline counts;
- `articleCount == len(articles)`;
- unique Article IDs;
- valid Article schema;
- canonical categories;
- valid source IDs;
- valid tag IDs;
- valid content types;
- score invariants;
- valid URLs.

---

# 34. Dataset Loader Tests

`js/data/articles.js` tests must cover:

### Valid dataset

Returns expected structure.

### Unsupported schema

Rejects predictably.

### Structurally unusable dataset

Rejects predictably.

### Network/fetch failure

Returns/throws the normalized failure expected by integration.

It must not modify local state during any of these tests.

---

# 35. Storage Default-State Tests

When no state exists:

```text
loadState()
```

returns exactly the logical V1 default state.

No articles or preferences should be created implicitly.

---

# 36. Storage Malformed-State Tests

Malformed localStorage JSON:

- must not be silently overwritten;
- must produce a recoverable error;
- must preserve the raw stored value.

Unsupported `schemaVersion` follows the same preservation principle.

---

# 37. State Transition Tests

Every supported transition must have explicit tests.

At minimum:

```text
UNSEEN → OPENED
UNSEEN → SAVED
UNSEEN → DISMISSED
OPENED → SAVED
OPENED → DISMISSED
OPENED → READ
SAVED → READ
READ → SAVED
SAVED → DISMISSED
```

Verify:

- resulting status;
- timestamps;
- signals;
- preference changes;
- counts.

---

# 38. Open Idempotency Tests

Opening an article twice must apply:

```text
First Open
```

exactly once.

Expected:

```text
source +0.10 once
topic +0.05 once
```

`openedAt` remains the original first-open timestamp.

---

# 39. Save Idempotency Tests

Repeated Save on an already-saved article:

- must not reapply preference signal;
- must not increment interaction counts;
- must not unexpectedly change `savedAt`.

---

# 40. Read Idempotency Tests

Repeated Mark Read on an already-read article:

- must not reapply Read signal;
- must not increment interaction counts.

---

# 41. Mark Unread Tests

Required lifecycle:

```text
READ → SAVED
```

Verify:

- Read signal reversed;
- Open signal preserved;
- prior Save signal preserved if it existed;
- no new Save signal;
- `readAt = null`;
- `savedAt = now`;
- Read Later count +1;
- History count -1.

---

# 42. Remove Tests

Required:

```text
SAVED → DISMISSED
```

Verify:

- no Not Interested signal applied;
- existing Save signal preserved;
- existing Open signal preserved;
- item leaves Read Later;
- item does not enter History.

---

# 43. Preference Clamp Tests

Repeated positive interaction simulation:

```text
weight never > +5
```

Repeated negative interaction simulation:

```text
weight never < -5
```

Tests must cover both sources and topics.

---

# 44. Preference Counter Tests

Applying a signal:

```text
interactions +1
```

Reversing it:

```text
interactions -1
```

must never produce a negative counter.

---

# 45. Category Preference Absence Test

The V1 state model must not create:

```text
preferences.categories
```

through any normal interaction.

Ranking tests must not depend on category weights.

---

# 46. Discover Eligibility Tests

Expected:

```text
unseen     → eligible
opened     → eligible
saved      → excluded
dismissed  → excluded
read       → excluded
```

---

# 47. Navigation Count Tests

Counts must be derived from state:

```text
Read Later = status == saved
History    = status == read
```

Transitions must immediately produce the expected counts.

No separate persisted counters may be required.

---

# 48. Undo Save Tests

After Save swipe then Undo:

- previous state restored exactly;
- Save preference signal reversed;
- interaction counters reversed;
- Read Later count restored;
- article returns to Discover eligibility where appropriate;
- prior Open signal remains if it existed before Save.

---

# 49. Undo Dismiss Tests

After Dismiss swipe then Undo:

- previous state restored exactly;
- negative signal reversed;
- article restored to Discover;
- prior Open signal remains if applicable.

---

# 50. Undo Scope Test

Only the most recent eligible:

```text
save
dismiss
```

swipe must be undoable.

Reload/session reconstruction must not preserve Undo.

---

# 51. Export Tests

Exported JSON must contain:

- schema version;
- preferences;
- persisted articles;
- settings;
- session.

It must not require:

- network access;
- current global dataset inclusion;
- backend services.

---

# 52. Import Validation Tests

Required invalid cases:

- malformed JSON;
- file >5 MiB;
- unsupported schema version;
- invalid Article ID;
- mismatched map key/snapshot ID;
- invalid URL scheme;
- invalid status;
- invalid category;
- invalid appearance;
- preference outside bounds;
- negative interaction count;
- invalid timestamp;
- dangerous object keys such as `__proto__`.

Every invalid import must leave current state unchanged.

---

# 53. Valid Import Test

A valid exported V1 state must import successfully and restore:

- preferences;
- Read Later;
- History;
- dismissals;
- article snapshots;
- appearance;
- last category.

---

# 54. Reset Tests

Reset must produce default state:

```text
preferences empty
articles empty
appearance system
lastCategory all
```

Undo state must also be cleared at the application layer.

---

# 55. Personalized Score Tests

Known values must verify:

```text
personalizedScore =
base
+ sourcePreference
+ topicPreference
+ exploration
```

The original Article must remain unmodified.

---

# 56. Topic Preference Cap Tests

Examples:

```text
raw topic sum = +9
→ +6
```

```text
raw topic sum = -8
→ -6
```

---

# 57. Exploration Tests

### Source

```text
0 interactions → +3
1              → +2
2              → +1
3+             → 0
```

### Topic

Lowest article-topic interaction count:

```text
0 → +2
1 → +1
2 → +0.5
3+ → 0
```

Final exploration:

```text
min(3, source + topic)
```

must be verified exactly.

---

# 58. Personalized Score Range Test

The final personalized score may exceed:

```text
100
```

Tests must not incorrectly clamp it to a percentage range.

---

# 59. Ranking Determinism Test

Given identical:

- dataset;
- state;
- category;
- time-independent inputs;

two calls to deck construction must return identical Article ordering.

No random value may affect the result.

---

# 60. Same-Source Diversity Test

Candidate ordering should demonstrate:

```text
same previous source
→ temporary -8
```

A stronger article may still remain next when its advantage exceeds eight points.

This proves the rule is a penalty, not a prohibition.

---

# 61. Category Diversity Test

In:

```text
All
```

a candidate that would create a third consecutive category receives:

```text
-5
```

temporary sequencing penalty.

In a category-specific view:

```text
no category penalty
```

---

# 62. Diversity Persistence Test

After deck construction:

- base scores unchanged;
- source preferences unchanged;
- topic preferences unchanged;
- local state unchanged.

Diversity is pure sequencing behavior.

---

# 63. Read Later Order Test

Read Later contains exactly `saved` records ordered by `savedAt` descending. Article snapshots remain available after removal from a later generated dataset.

---

# 64. History Order Test

History contains exactly `read` records ordered by `readAt` descending. Mark Unread moves the Article to Read Later, sets the required save timestamp, reverses only an applied Read signal, and does not apply a new Save signal.

---

# 65. Queue Count Tests

Read Later and History counts must update immediately after successful transitions and remain unchanged after rejected operations. Counts are derived from committed state.

---

# 66. Dataset Loader Integration Tests

Verify:

- supported schema loads;
- unsupported schema rejects;
- structurally unusable dataset rejects;
- empty-but-valid dataset produces a truthful empty state;
- loader performs no personalization or storage mutation.

---

# 67. Application Flow Tests

Using a local static dataset, verify:

1. Discover renders the highest sequenced eligible Article;
2. Save moves it to Read Later and offers Undo;
3. Dismiss excludes it and offers Undo;
4. Open applies its first-open signal once and preserves saved/read status;
5. Mark Read moves a saved Article to History;
6. Mark Unread returns it to Read Later;
7. Remove excludes it without a negative preference signal;
8. reload reconstructs queues from persisted snapshots.

---

# 68. External Navigation Tests

Only HTTP/HTTPS Article URLs may open. The navigation path uses a new context with opener isolation. Invalid schemes must be rejected without applying a successful Open transition.

For a valid publisher URL, verify that Open-state persistence is attempted first. When persistence succeeds, retain the Open interaction and navigate. When persistence fails, show a local-state warning, do not claim the Open interaction was persisted, and still navigate to the publisher. Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset must not be presented as successful when persistence fails.

---

# 69. UI State Tests

Verify distinct, truthful presentation for:

- loading;
- populated Discover;
- category empty;
- all Articles handled;
- dataset unavailable/invalid;
- populated and empty Read Later;
- populated and empty History;
- storage failure.

---

# 70. Keyboard Acceptance

All functionality must be available without pointer gestures. Verify Tab/Shift+Tab order, Enter/Space activation, Escape for dialogs, arrow/swipe equivalents where specified, and:

```text
Z → Undo
```

when Undo is available and focus is not in a text-entry control.

---

# 71. Focus Acceptance

Every interactive control shows visible focus in light and dark themes. Opening/closing Settings manages focus correctly, and state changes do not unexpectedly move focus or trap keyboard users.

---

# 72. Touch-Target Acceptance

Verify design-specified target sizes, including at least `44px` usable compact actions, `48px` primary/card controls where specified, and `54px` mobile-navigation targets.

---

# 73. Responsive Matrix

Manually verify at:

```text
360
390
430
600
768
820
1024
1366
1440
1920
```

pixels wide.

At every width verify no horizontal scrolling, readable long titles, reachable actions, stable fixed/sticky navigation, usable dialogs, and no clipped focus indicators.

---

# 74. Theme Acceptance

Verify Light, Dark, and System; persistence across reload; reaction to system preference while `system` is selected; and sufficient contrast for text, borders, focus, selected states, and controls.

---

# 75. Reduced-Motion Acceptance

With `prefers-reduced-motion: reduce`, remove or substantially reduce card drag/exit and ornamental transitions while keeping state changes understandable. Functionality must remain complete.

---

# 76. Live-Status Acceptance

Loading, save, dismiss, read/unread, import, reset, errors, and Undo availability are announced appropriately without stealing focus or producing repeated/noisy announcements.

---

# 77. Content Safety Tests

Fixtures containing scripts, event-handler attributes, HTML elements, encoded markup, malicious URL schemes, and oversized text must normalize/render as inert data. The frontend must not assign publisher content to `innerHTML`.

---

# 78. Workflow Tests

Validate workflow syntax and policy:

- approved triggers only;
- full release gates for push/manual deployment;
- approved reduced scheduled-refresh path;
- least-privilege permissions;
- full-SHA Action pins;
- Pages artifact assembled only after validation;
- failed gates cannot deploy.

---

# 79. Dependency Security Gates

The release gate must pass the npm audit threshold and must fail for any known Python dependency vulnerability reported by the approved audit tool, unless an explicit current exception exists.

---

# 80. Live-Network Validation

Live checks are separate from deterministic tests. They verify that enabled endpoints remain reachable and parsable and report source-specific changes. A publisher outage does not justify weakening local parser fixtures or acceptance tests.

For initial-release review, Amendment 4 permits the release decision to remain acceptable when live validation reports failures only for `n8n_release_notes` and/or `ietf_scim`, but only after verifying each still-present failure exactly matches its documented upstream condition in `03-content-sources.md` and `07-pipeline-deployment.md`. The command result remains a failure and must be reported truthfully; the release decision is evaluated separately.

Normal catastrophic dataset gates must pass, and the reviewer must find no implementation regression. Any other failed source or materially different failure mode is outside Amendment 4 and requires investigation. The previously observed diagnostic state was 20 of 22 successful sources and 221 retained Articles; it is evidence, not a permanently required count.

---

# 81. Manual Product Acceptance

Confirm that the product feels editorial, text-first, finite, and intentional rather than dashboard-like or engagement-driven; gestures have visible and keyboard equivalents; counts are truthful; no article imagery or fabricated metadata appears; and opening content clearly leaves for the publisher.

---

# 82. Release Gate

A release is eligible only when:

- automated deterministic tests pass;
- configuration and dataset validation pass;
- security/dependency checks pass;
- required integration flows pass;
- the responsive/accessibility acceptance matrix is completed;
- the static artifact is successfully assembled;
- no unresolved severity-blocking defect remains.

---

# 83. V1 Completion Criteria

V1 is complete when every approved specification has corresponding evidence, all shared contracts are honored, all enabled sources have fixture coverage, article/state/ranking behaviors are deterministic and tested, the UI passes the required widths and accessibility checks, and the validated static artifact can deploy without secrets or runtime feed dependencies.
