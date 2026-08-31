# Intentional Reading — V1 Personalization and Local State Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/05-personalization-state.md`\
**Role:** Authoritative browser persistence, article-state transitions, preference learning, personalized ranking, exploration, diversity sequencing, Undo, and local backup specification

---

## 1. Purpose

This document defines how Intentional Reading V1 learns from user behavior while keeping all personal state local to the browser.

V1 personalization is deliberately:

- deterministic;
- explainable;
- local-only;
- bounded;
- reversible where required;
- independent of external AI services.

The browser personalization layer answers:

> Given the approved article corpus, which eligible article should be shown next to this user?

It must not change the pipeline's editorial base score.

---

# 2. Core Separation

The generated dataset represents editorial/content judgment:

```text
data/articles.json
        ↓
base score
source
topics
category
```

Browser state represents personal judgment:

```text
localStorage
        ↓
source preferences
topic preferences
saved/read/dismissed state
```

The final Discover ordering combines them without modifying either source of truth.

---

# 3. Persistent Storage Key

All V1 persistent application state is stored under exactly:

```text
intentionalReading:v1
```

No UI, ranking, or utility module may create additional persistent application-state keys.

`js/state/storage.js` is the only module permitted to access browser `localStorage` directly.

---

# 4. Local State Schema

The V1 root state is:

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

The exact shared contract is authoritative in:

```text
docs/v1/contracts.md
```

---

# 5. Default State

When no local state exists:

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

No article records are created merely because articles were loaded or displayed.

---

# 6. Untouched Articles Are Not Persisted

An article remains represented only by `articles.json` until the user performs a persistent interaction.

Displaying an article in Discover does not create a local record.

A local article record is created when the user first:

- opens;
- saves;
- dismisses;
- marks the article read.

This prevents localStorage from becoming a duplicate of the generated corpus.

---

# 7. Complete Article Snapshots

When an article first becomes persistent, store a complete snapshot conforming to the Article contract.

The snapshot preserves:

- ID;
- title;
- canonical URL;
- source;
- category;
- publication date;
- author;
- excerpt;
- reading time;
- tags;
- content type;
- base score.

This is required because generated articles eventually age out of `articles.json`.

Read Later and History must continue working after that occurs.

---

# 8. Logical Article States

Product-level states are:

```text
UNSEEN
OPENED
SAVED
DISMISSED
READ
```

Persisted statuses are exactly:

```text
opened
saved
dismissed
read
```

`UNSEEN` means:

```text
no persisted article record exists
```

---

# 9. Timestamp Semantics

Each persisted article record may contain:

```text
firstSeenAt
openedAt
savedAt
dismissedAt
readAt
```

All populated timestamps are UTC ISO-8601 strings.

`firstSeenAt` is set once when the first persistent article record is created and never changes.

Other timestamps reflect the current meaningful state/event history described below.

---

# 10. Learning Signals

Preference learning is tracked independently from current article status.

Each persisted article includes:

```json
{
  "signalsApplied": {
    "opened": false,
    "saved": false,
    "dismissed": false,
    "read": false
  }
}
```

This prevents repeat actions from repeatedly training the system.

---

# 11. Exact Preference Deltas

V1 uses these exact learning values:

| User event | Source weight | Each article topic |
|---|---:|---:|
| Not Interested | -0.35 | -0.20 |
| Save for Later | +0.45 | +0.30 |
| First Open | +0.10 | +0.05 |
| Mark Read | +0.25 | +0.20 |

These are preference updates, not changes to the article's base score.

---

# 12. Preference Bounds

Every source/topic weight is clamped to:

```text
-5.0 ≤ weight ≤ +5.0
```

No interaction may push a weight outside that interval.

This prevents long-term usage from producing unbounded ranking influence.

---

# 13. Preference Interaction Counters

Preference entries contain:

```json
{
  "weight": 1.45,
  "interactions": 7
}
```

`interactions` counts currently applied learning events affecting that preference.

When a learning signal is applied:

```text
interactions += 1
```

When that signal is formally reversed:

```text
interactions -= 1
```

Interaction count must never fall below:

```text
0
```

---

# 14. Missing Preferences

If a source or topic has no preference entry, treat it as:

```json
{
  "weight": 0,
  "interactions": 0
}
```

Entries may be created lazily when a signal first affects them.

---

# 15. Topics Used for Preference Learning

Every canonical tag in:

```text
article.tags
```

participates in preference learning.

This includes:

- organically detected tags;
- approved forced tags.

Forced tags are excluded from pipeline base-score topic evidence but are valid personalization signals because their source context makes the topic authoritative.

Duplicate topic IDs must never cause repeated preference application.

---

# 16. Category Preferences Are Prohibited

V1 does not store or learn category preference weights.

There must be no structure such as:

```json
{
  "preferences": {
    "categories": {
      "technology": 4.2
    }
  }
}
```

The user deliberately chose the seven categories.

Learning must happen at the more specific:

```text
source
topic
```

levels.

---

# 17. First Open Transition

When an unseen Discover article is opened:

```text
UNSEEN → OPENED
```

Perform atomically:

1. store complete Article snapshot;
2. set `status = "opened"`;
3. set `firstSeenAt = now`;
4. set `openedAt = now`;
5. leave `savedAt`, `dismissedAt`, and `readAt` null;
6. apply First Open preference signal;
7. set `signalsApplied.opened = true`;
8. persist state;
9. open the external publisher URL.

The external page must not be opened until the local state update has been successfully attempted.

---

# 18. Reopening an Opened Article

If:

```text
status = opened
signalsApplied.opened = true
```

opening again:

- does not alter source preference;
- does not alter topic preferences;
- does not increment interaction counts;
- does not replace the original `openedAt`.

The original first-open timestamp remains authoritative.

---

# 19. Opening a Saved Article

When a saved article is opened:

```text
status remains saved
```

If it has never previously been opened:

- set `openedAt = now`;
- apply First Open preference signal;
- set `signalsApplied.opened = true`.

If already opened:

- do not reapply the signal;
- preserve original `openedAt`.

Opening does not remove an item from Read Later.

---

# 20. Reopening a Read Article

When an article is reopened from History:

```text
status remains read
```

If the article somehow lacks a previously applied Open signal:

- set `openedAt` if necessary;
- apply First Open once.

Otherwise no preference learning occurs.

Reopening History does not make the article unread.

---

# 21. Save-for-Later Transition from Unseen

When an unseen article is saved:

```text
UNSEEN → SAVED
```

Perform:

1. persist Article snapshot;
2. set `status = "saved"`;
3. set `firstSeenAt = now`;
4. set `savedAt = now`;
5. leave `openedAt`, `dismissedAt`, and `readAt` null;
6. apply Save preference signal;
7. set `signalsApplied.saved = true`;
8. persist.

Read Later count increases immediately after successful persistence.

---

# 22. Save-for-Later Transition from Opened

When:

```text
OPENED → SAVED
```

perform:

- preserve `firstSeenAt`;
- preserve `openedAt`;
- preserve Open signal;
- set `status = "saved"`;
- set `savedAt = now`;
- set `dismissedAt = null`;
- set `readAt = null`;
- apply Save signal if it has not already been applied;
- persist.

---

# 23. Saving an Already-Saved Article

If:

```text
status = saved
```

a duplicate Save action is a no-op for preference learning.

It must not:

- add another +0.45 source adjustment;
- add another +0.30 topic adjustment;
- increment interaction counters;
- replace the original current `savedAt` merely due to duplicate input.

---

# 24. Dismiss Transition from Unseen

When an unseen article receives Not Interested:

```text
UNSEEN → DISMISSED
```

Perform:

1. persist Article snapshot;
2. set `status = "dismissed"`;
3. set `firstSeenAt = now`;
4. set `dismissedAt = now`;
5. leave unrelated state timestamps null;
6. apply Not Interested signal;
7. set `signalsApplied.dismissed = true`;
8. persist.

The article leaves Discover.

---

# 25. Dismiss Transition from Opened

When:

```text
OPENED → DISMISSED
```

perform:

- preserve `firstSeenAt`;
- preserve `openedAt`;
- preserve previously applied Open signal;
- set `status = "dismissed"`;
- set `dismissedAt = now`;
- clear `savedAt`;
- clear `readAt`;
- apply Not Interested if not already applied;
- persist.

---

# 26. Mark Read from Saved

Normal completion flow:

```text
SAVED → READ
```

Perform:

- preserve Article snapshot;
- preserve `firstSeenAt`;
- preserve `openedAt` if present;
- set `status = "read"`;
- set `readAt = now`;
- clear `savedAt`;
- clear `dismissedAt`;
- apply Read preference signal if not already applied;
- set `signalsApplied.read = true`;
- persist.

Results:

```text
Read Later count -1
History count +1
```

---

# 27. Mark Read from Opened

An article may be read immediately without first entering Read Later.

Transition:

```text
OPENED → READ
```

Perform:

- preserve Open metadata/signal;
- set `status = "read"`;
- set `readAt = now`;
- clear `savedAt`;
- clear `dismissedAt`;
- apply Read signal once;
- persist.

No Save signal is implied.

---

# 28. Mark Read from Unseen

UI flows do not normally expose Mark Read for an untouched Discover article without opening it first.

However, state logic may safely support:

```text
UNSEEN → READ
```

if invoked by a valid future integration path.

In that case:

- create snapshot;
- set `firstSeenAt = now`;
- set `status = "read"`;
- set `readAt = now`;
- apply only the Read signal;
- do not manufacture Open or Save signals.

The normal V1 UI is not required to expose this transition.

---

# 29. Mark Unread

History provides:

```text
Mark unread
```

Transition:

```text
READ → SAVED
```

Perform:

1. reverse Read preference signal if applied;
2. set `signalsApplied.read = false`;
3. decrement affected source/topic interaction counters accordingly;
4. set `status = "saved"`;
5. clear `readAt`;
6. set `savedAt = now`;
7. preserve `openedAt`;
8. preserve previously applied Save signal if one existed;
9. do not apply a new Save signal;
10. persist.

Results:

```text
History count -1
Read Later count +1
```

---

# 30. Remove from Read Later

Read Later provides:

```text
Remove
```

Transition:

```text
SAVED → DISMISSED
```

This is a queue-management action, not a statement that the source/topic is unwanted.

Therefore:

- do not apply Not Interested;
- do not reverse the prior Save signal;
- preserve Open signal if present;
- preserve Save signal if previously applied;
- set `status = "dismissed"`;
- set `dismissedAt = now`;
- clear `savedAt`;
- clear `readAt`;
- persist.

`signalsApplied.dismissed` remains false unless an actual Not Interested event was previously applied.

---

# 31. Why Remove Is Not Negative Training

Removing an article may mean:

- the queue is too long;
- the user changed priorities;
- the article became stale;
- the user read it elsewhere;
- the user no longer wants that specific item.

None necessarily means:

> Show me less from this source/topic.

Only explicit Not Interested carries the negative learning signal.

---

# 32. Discover Eligibility

An article is Discover-eligible when:

```text
no persisted record exists
```

or:

```text
status = opened
```

Exclude when:

```text
status = saved
status = dismissed
status = read
```

An opened-but-unresolved article remains eligible.

---

# 33. Read Later Membership

Read Later contains exactly:

```text
status = saved
```

Default sort:

```text
savedAt descending
```

All entries are rendered from their local Article snapshot.

Do not depend on the article still existing in the generated dataset.

---

# 34. History Membership

History contains exactly:

```text
status = read
```

Default sort:

```text
readAt descending
```

All entries are rendered from their local Article snapshot.

---

# 35. Navigation Counts

Read Later count:

```text
count(status == "saved")
```

History count:

```text
count(status == "read")
```

Counts are derived from current logical state.

Do not maintain separate persistent counters that can drift from article state.

---

# 36. Undo Scope

V1 Undo supports the most recent successful **eligible action**, from any surface that performs it:

```text
save
dismiss
mark read
mark unread
remove from read later
```

The trigger does not determine reversibility. A swipe, a labelled control, and a keyboard shortcut are equivalent for this purpose.

Undo is **not** available for:

- Open;
- import;
- reset;
- appearance changes.

Reversal arithmetic for each eligible action is in `contracts.md` §23.

*Amended by Amendment 8.*

---

# 37. Undo Persistence

Undo data exists only in active JavaScript memory.

It is not stored in localStorage.

Refreshing/reopening the application removes Undo availability.

---

# 38. Undo Record

An Undo record must contain enough information to restore the exact pre-action state.

Conceptually:

```javascript
{
  articleId,
  action,
  previousRecord,
  preferenceSignal
}
```

If no local article record existed before the swipe:

```text
previousRecord = null
```

---

# 39. Undo Save

For the most recent Save swipe:

1. restore the previous Article record exactly;
2. if previous record was absent, remove the newly created local record;
3. reverse the Save preference signal;
4. decrement affected interaction counters;
5. set `signalsApplied.saved` according to restored state;
6. restore queue count;
7. rebuild Discover.

Undo must not reverse a prior Open signal that existed before the Save.

---

# 40. Undo Dismiss

For the most recent Not Interested swipe:

1. restore the previous Article record exactly;
2. if previous record was absent, remove the newly created local record;
3. reverse the Dismiss preference signal;
4. decrement affected interaction counters;
5. restore Discover eligibility.

Undo must not reverse an Open signal that existed before the dismissal.

---

# 41. Undo Lifetime

The UI exposes Undo for approximately the design-specified toast duration:

```text
4.5 seconds
```

The in-memory record may remain available for the current action until:

- another eligible action replaces it;
- the page reloads;
- the application explicitly clears it.

The UI controls actual visible Undo availability.

---

# 42. Transactional State Changes

A user action that changes persistent state should be treated as one logical transaction:

```text
derive next state
    ↓
apply preference/state changes
    ↓
attempt persistence
    ↓
if persistence succeeds:
    update visible UI
else:
    preserve/restore previous logical UI state
    surface error
```

The UI must not claim:

```text
Saved
Read
Dismissed
```

when the state could not be persisted.

---

# 43. Storage Failure

Examples include:

- browser storage unavailable;
- storage quota exceeded;
- serialization failure.

On persistence failure:

- do not silently discard the error;
- do not falsely update navigation counts;
- do not clear existing valid stored state;
- surface a recoverable local-storage error.

Open/external reading is the specific exception to the normal visual-commit boundary:

1. attempt the Open-state local persistence first;
2. if persistence succeeds, retain the Open interaction normally and proceed to publisher navigation;
3. if persistence fails, surface a local-state warning, do not claim the Open interaction was persisted, and still proceed to open the publisher Article.

Reading must not become unavailable merely because local browser storage is unavailable.

Persistent queue/state actions including Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset must not visually claim a successful state transition when persistence fails.

Detailed user-facing error presentation belongs to `06-ui-ux.md`.

---

# 44. State Loading

`loadState()` behavior:

### No key exists

Return default V1 state.

### Valid V1 state exists

Return parsed current state.

### Malformed JSON

Return a recoverable storage error.

Do not automatically overwrite the malformed value.

### Unsupported schema version

Return a recoverable compatibility error.

Do not silently reset or reinterpret it.

---

# 45. State Migration Architecture

V1 implements:

```text
schemaVersion = 1
```

No historical migration is required yet.

However, storage code must have a clear migration entry point conceptually like:

```javascript
migrateState(state, fromVersion, toVersion)
```

Future migrations must remain centralized in the storage subsystem.

---

# 46. Import / Export Format

Export uses the exact V1 local-state root object.

No second proprietary backup schema is required.

Exported file content therefore begins conceptually:

```json
{
  "schemaVersion": 1,
  "preferences": {},
  "articles": {},
  "settings": {},
  "session": {}
}
```

Recommended filename:

```text
intentional-reading-backup-YYYYMMDD-HHMMSSZ.json
```

---

# 47. Export Behavior

Export must:

- serialize the current valid local state;
- include all persisted Article snapshots;
- include source/topic preferences;
- include Settings;
- include last selected category;
- perform no network request.

Export does not contain the current global `articles.json` corpus unless an article already exists as a persisted local snapshot.

---

# 48. Import Validation

Import must validate the entire candidate state before replacing active state.

Validate at minimum:

- parseable JSON;
- `schemaVersion = 1`;
- valid root structure;
- valid preference entries;
- valid article IDs;
- valid persisted statuses;
- valid Article snapshot structures;
- valid category IDs;
- valid appearance enum;
- valid timestamp/null shapes.

An invalid import must be rejected atomically.

---

# 49. Atomic Import

Import sequence:

```text
read selected file
    ↓
parse
    ↓
validate complete candidate state
    ↓
if invalid:
    reject
    keep existing state untouched

if valid:
    persist complete candidate state
    reload logical state
    rebuild counts/deck/theme
```

Never partially merge an invalid backup.

---

# 50. Import Is Replacement, Not Merge

V1 import replaces the current local application state.

It does not attempt to merge:

- two histories;
- conflicting preference weights;
- duplicate saved queues.

This keeps backup behavior predictable.

A future version may introduce merge semantics if required.

---

# 51. Reset

Reset:

1. requires explicit user confirmation;
2. removes the `intentionalReading:v1` stored state;
3. creates the default logical V1 state;
4. resets appearance to `system`;
5. resets category to `all`;
6. clears preferences;
7. clears Read Later;
8. clears History;
9. clears dismissals;
10. clears current Undo state.

Reset does not:

- modify `articles.json`;
- change configured sources;
- trigger pipeline execution.

---

# 52. Appearance State

Allowed values:

```text
light
dark
system
```

Default:

```text
system
```

`system` follows:

```text
prefers-color-scheme
```

where supported.

Appearance changes are not personalization signals.

---

# 53. Last Category

V1 persists:

```text
session.lastCategory
```

Allowed:

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

Selecting a category:

- updates `lastCategory`;
- rebuilds the Discover deck;
- does not modify preference weights.

---

# 54. Personalized Candidate Score

For every Discover-eligible candidate:

```text
personalizedScore =
    base
  + sourcePreference
  + topicPreference
  + exploration
```

The result is not clamped to `0–100`; it is a ranking value, not a percentage.

---

# 55. Source Preference Component

```text
sourcePreference = current local preference weight for article.source.id
```

This component ranges from:

```text
-5 to +5
```

An unseen source has weight `0`.

---

# 56. Topic Preference Component

Sum the current preference weight for every Article tag, then clamp:

```text
topicPreference = clamp(sum(topic weights), -6, +6)
```

Unknown topics contribute zero. Duplicate tag IDs must not be counted twice.

---

# 57. Exploration Component

Exploration depends on interaction counts, not randomness.

### Source exploration

```text
0 interactions  +3
1               +2
2               +1
3+               0
```

### Topic exploration

Use the lowest interaction count among the Article's topics:

```text
0  +2
1  +1
2  +0.5
3+  0
```

If the Article has no topics, topic exploration is zero.

Final exploration:

```text
min(3, source exploration + topic exploration)
```

---

# 58. Personalized Ranking Order

Candidate sort order is:

1. personalized total descending;
2. base score descending;
3. publication date descending, unknown last;
4. source ID ascending;
5. Article ID ascending.

Identical inputs must always produce identical ordering.

---

# 59. Same-Source Diversity

During sequential deck construction, a candidate from the same source as the previously selected card receives a temporary:

```text
-8
```

penalty for that selection step.

The penalty is not persisted and is not a prohibition; a sufficiently stronger candidate may remain next.

---

# 60. Category Diversity

In the `all` view, a candidate that would create a third consecutive card from the same category receives a temporary:

```text
-5
```

sequencing penalty.

Category diversity is disabled in category-specific views.

---

# 61. Diversity Purity

Diversity sequencing must not modify:

- Article base scores;
- preferences;
- interaction counts;
- persisted article records;
- the dataset.

---

# 62. Deck Rebuilds

Rebuild Discover after:

- category change;
- successful save/dismiss/read transition affecting eligibility;
- Undo;
- valid state import;
- reset;
- dataset reload.

Rebuilding does not create learning signals.

---

# 63. Queue Ordering

Read Later is ordered by `savedAt` descending. History is ordered by `readAt` descending. These screens do not use personalized ranking or diversity penalties.

---

# 64. State and Ranking Completion Criteria

The implementation is complete when every transition is atomic, signals are idempotent and reversible where specified, Article snapshots survive dataset removal, import replaces state only after full validation, ranking is deterministic, exploration follows the exact tables, and diversity remains a pure deck-sequencing concern.
