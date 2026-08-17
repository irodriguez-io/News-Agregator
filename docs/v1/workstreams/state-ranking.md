# Intentional Reading — V1 State and Ranking Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/state-ranking.md`\
**Workstream type:** Feature implementation\
**Primary branch:** `feat/state-ranking`\
**Primary ownership:** `js/data/**`, `js/state/**`, `js/ranking/**`, `tests/js/**`

---

## 1. Mission

Implement the complete browser-side data-loading, persistence, article-state, preference-learning, personalization, and Discover deck logic for Intentional Reading V1.

This workstream owns:

- loading and validating `data/articles.json`;
- local state persistence;
- state schema/version handling;
- Article snapshots;
- article status transitions;
- preference updates and reversals;
- Undo state support logic;
- import/export/reset logic;
- personalized scoring;
- exploration;
- deterministic deck construction;
- Discover eligibility;
- Read Later ordering;
- History ordering;
- state/ranking automated tests.

This workstream does **not** own:

- DOM rendering;
- CSS;
- swipe gesture rendering;
- primary navigation rendering;
- Settings dialog presentation;
- publisher ingestion;
- GitHub Actions;
- `js/app.js`.

---

## 2. Starting Point

Create this branch/worktree from exactly:

```text
FOUNDATION_SHA
```

Do not branch from:
main
bootstrap SHA
pipeline branch
frontend branch
All feature streams must start from the same frozen foundation commit.
3. Required Reading
Before implementation, read:
AGENTS.md
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/05-personalization-state.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
docs/v1/workstreams/state-ranking.md
Also read the relevant pipeline output contracts in:
docs/v1/03-content-sources.md
docs/v1/04-taxonomy-scoring.md
The implementation consumes the frozen Article/Dataset contracts and must not reinterpret them.
4. Owned Paths
This workstream may create or modify:
js/data/**
js/state/**
js/ranking/**
tests/js/**
package.json
package-lock.json
package.json and package-lock.json are owned here only for JavaScript test tooling and scripts.
The preferred outcome is still:
zero third-party npm dependencies
if Node's built-in test tooling is sufficient.
5. Forbidden Paths
Do not modify:
index.html
css/**
style.css
script.js
js/ui/**
js/app.js
pipeline/**
config/**
tests/pipeline/**
docs/v1/**
design-reference/**
.github/workflows/**
requirements*.txt
Any required change outside owned paths must be escalated to the supervisor.
6. Shared Contract Constraint
This workstream must consume exactly the frozen:
ArticleDataset v1
Article v1
Local State v1
contracts from:
docs/v1/contracts.md
Do not alter:
Article field names;
dataset schema version;
localStorage root key;
local-state structure;
status values;
semantic action names;
category IDs;
preference delta values;
ranking component names.
If implementation reveals a contradiction:
STOP
→ report exact conflict
→ do not edit docs
7. Target JavaScript Layout
Implement approximately:
js/
├── data/
│   └── articles.js
│
├── state/
│   ├── storage.js
│   ├── preferences.js
│   └── article-state.js
│
└── ranking/
    ├── personalize.js
    └── deck.js
article-state.js is recommended to centralize transitions.
Minor additional modules are allowed if they preserve boundaries.
Examples:
validation.js
constants.js
import-export.js
Do not introduce a general frontend framework.
8. Data Loader
Implement:
js/data/articles.js
with a public function conceptually equivalent to:
loadArticleDataset(url = "./data/articles.json")
Responsibilities:
fetch the static dataset;
validate schemaVersion = 1;
validate required top-level structure;
perform lightweight Article structural validation;
return a normalized logical dataset;
produce predictable errors.
It must not:
access localStorage;
personalize;
render DOM;
fetch publishers;
call external APIs.
9. Dataset Failure Behavior
The data loader must distinguish:
network/fetch failure
unsupported schema
structurally unusable dataset
through predictable errors/results.
It must not:
erase local state;
modify preferences;
fabricate Articles.
Integration/UI will decide how errors are displayed.
10. LocalStorage Boundary
Only:
js/state/storage.js
may directly call:
localStorage.getItem(...)
localStorage.setItem(...)
localStorage.removeItem(...)
No other JavaScript module may bypass this abstraction.
Tests should provide a storage substitute/fake where needed.
11. Storage Key
Use exactly:
intentionalReading:v1
No additional persistent application-state keys.
12. Default State
Implement exactly:
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
Untouched Articles must not be persisted.
13. State Versioning
storage.js owns version detection and migration entry points.
V1 requires:
schemaVersion = 1
Implement a clear migration boundary even though there are no historical migrations yet.
Conceptually:
migrateState(state, fromVersion, toVersion)
Unsupported versions must not be silently reinterpreted.
14. Corrupt Stored State
Malformed stored JSON must:
produce a recoverable storage error;
preserve the original raw value;
not silently reset/overwrite it.
Integration/UI may offer Reset.
The state module itself must not destroy potentially recoverable data automatically.
15. Article Snapshot Persistence
When an Article first becomes persistent, store the complete Article snapshot.
Do not persist untouched feed Articles.
The local snapshot is authoritative for:
Read Later
History
after that Article disappears from future generated datasets.
16. Persisted Article Statuses
Use exactly:
opened
saved
dismissed
read
unseen means:
no persisted Article record
Do not persist "unseen".
17. Timestamps
Support:
firstSeenAt
openedAt
savedAt
dismissedAt
readAt
Use UTC ISO-8601 strings.
firstSeenAt is immutable after first persistent interaction.
Other timestamps follow the transition semantics in:
05-personalization-state.md
18. Signal Tracking
Persist:
{
  "opened": false,
  "saved": false,
  "dismissed": false,
  "read": false
}
under:
signalsApplied
Signal state must be independent of current Article status.
This is required for idempotency and reversals.
19. Preference Structure
Source and topic entries are:
{
  "weight": 1.45,
  "interactions": 7
}
Missing entries logically mean:
weight = 0
interactions = 0
Create entries lazily.
20. Preference Deltas
Implement exact V1 values:
OPEN
source +0.10
topic  +0.05

SAVE
source +0.45
topic  +0.30

DISMISS
source -0.35
topic  -0.20

READ
source +0.25
topic  +0.20
Do not duplicate these values in UI code.
They belong to:
js/state/preferences.js
21. Preference Bounds
Clamp all source/topic weights to:
[-5.0, +5.0]
interactions must never fall below:
0
22. Preference Topics
Use every unique:
article.tags[].id
for topic preference learning.
This includes approved forced tags.
Do not use:
category as a preference;
contentType as a preference;
source display name as a topic.
23. Category Preferences Prohibited
Do not create:
preferences.categories
through any code path.
Category selection is explicit user intent and only affects deck filtering.
24. Preference Module API
Provide semantic operations conceptually equivalent to:
applyInteraction(preferences, article, event)
reverseInteraction(preferences, article, event)
Supported events:
open
save
dismiss
read
The module owns:
deltas;
clamping;
counter updates;
reversals.
It must not own Article status transitions.
25. Article State Module
Centralize state transitions under:
js/state/**
Prefer a dedicated:
article-state.js
or equivalent.
UI/integration must not need to manually manipulate low-level status/timestamps/signals.
26. Unseen → Opened
Implement atomically:
persist snapshot
status = opened
firstSeenAt = now
openedAt = now
apply Open signal once
signalsApplied.opened = true
No Save/Read/Dismiss signal implied.
27. Open Idempotency
Repeated opening:
does not reapply Open signal
does not increment interaction counters
does not replace openedAt
This applies whether status is:
opened
saved
read
28. Unseen → Saved
Implement:
snapshot
status = saved
firstSeenAt = now
savedAt = now
apply Save once
signalsApplied.saved = true
No Open signal implied.
29. Opened → Saved
Preserve:
firstSeenAt
openedAt
Open signal
Then:
status = saved
savedAt = now
apply Save once
30. Duplicate Save
If already:
status = saved
duplicate Save must not:
apply preference again;
increment interactions;
rewrite current savedAt.
31. Unseen → Dismissed
Implement:
snapshot
status = dismissed
firstSeenAt = now
dismissedAt = now
apply Dismiss once
32. Opened → Dismissed
Preserve:
Open metadata/signal
Then:
status = dismissed
dismissedAt = now
apply Dismiss once
33. Saved → Read
Implement:
status = read
readAt = now
savedAt = null
dismissedAt = null
apply Read once
signalsApplied.read = true
Preserve prior:
Open signal
Save signal
where present.
34. Opened → Read
Implement:
status = read
readAt = now
apply Read once
Do not fabricate Save.
35. Unseen → Read
Support safely in state logic even if normal UI does not expose it.
Apply only:
Read
Do not fabricate:
Open
Save
36. Read → Saved (Mark Unread)
Implement exactly:
reverse Read signal if applied
signalsApplied.read = false

status = saved
readAt = null
savedAt = now
Preserve:
Open signal
prior Save signal
Do not apply a new Save signal.
37. Saved → Dismissed (Remove)
Remove from Read Later is queue management.
Implement:
status = dismissed
dismissedAt = now
savedAt = null
readAt = null
Do not:
apply Dismiss signal
reverse Save signal
Preserve previous positive signals.
38. Discover Eligibility
Eligible:
no record
opened
Excluded:
saved
dismissed
read
Centralize this logic so UI does not duplicate it.
39. Read Later Selection
Read Later contains exactly:
status = saved
Sort:
savedAt descending
Return/use local snapshots.
Do not depend on current feed presence.
40. History Selection
History contains exactly:
status = read
Sort:
readAt descending
Use local snapshots.
41. Counts
Derive dynamically:
Read Later count =
number of status == saved

History count =
number of status == read
Do not persist independent counters.
42. Undo Logic
Undo supports only the most recent successful Discover:
save
dismiss
Store Undo in memory only.
Do not persist it.
43. Undo Record
Support enough data conceptually equivalent to:
{
  articleId,
  action,
  previousRecord,
  preferenceSignal
}
previousRecord may be:
null
if the Article was previously unseen.
44. Undo Save
Undo must:
restore exact previous Article record;
remove newly created record if previously unseen;
reverse Save signal;
restore interaction counters;
preserve any prior Open signal;
restore Discover eligibility/counts appropriately.
45. Undo Dismiss
Undo must:
restore exact prior Article record;
reverse negative Dismiss signal;
preserve prior Open signal;
restore eligibility.
46. Undo Lifetime
Do not persist Undo.
Application reload clears it.
The UI's visual 4.5-second presentation is not owned here, but state logic must support a single active Undo record.
47. Transactional State Changes
Implement logical state changes so integration can:
derive next state
→ attempt persistence
→ commit UI only on success
Avoid APIs that irreversibly mutate global state before persistence success can be determined.
Prefer functions that can return a new/next state.
Open/external reading is the specific exception: attempt Open-state persistence first; on success retain the interaction normally; on failure return an explicit local-state warning result without claiming persistence, while allowing Integration to continue publisher navigation.
Persistent queue/state actions such as save, dismiss, mark_read, mark_unread, remove, import, and reset must not return a visually successful transition when persistence fails.
48. Storage API
Provide capabilities conceptually equivalent to:
loadState()
saveState(state)
resetState()
exportState()
importState(serializedState)
Additional helpers are allowed.
Other modules must not bypass this layer.
49. Export
Export exactly the V1 state structure.
No alternate backup schema.
Include:
schemaVersion;
preferences;
persisted Article snapshots;
settings;
session.
No network operation.
50. Import Size / Validation Boundary
UI owns file selection, but state/storage code must provide validation for imported content.
The 5 MiB file-size check may occur at UI/integration boundary before parsing.
Once serialized content is handed to state code, validate the complete candidate structure.
51. Import Validation
Reject invalid:
JSON;
schema version;
root structure;
Article IDs;
key/snapshot ID mismatch;
Article snapshots;
URLs;
status values;
category values;
appearance;
timestamps;
preference bounds;
negative interaction counts;
signal flags;
dangerous keys.
No partial import.
52. Prototype Pollution Defense
Reject dangerous property names such as:
__proto__
prototype
constructor
in untrusted import maps.
Do not recursively merge arbitrary imported objects into application state.
Construct validated objects explicitly.
53. Import Replacement
Valid import:
replaces current local state
It does not merge histories/preferences.
This behavior is frozen for V1.
54. Reset
Reset produces exact default logical state.
It clears persistent:
preferences;
persisted Articles;
appearance;
lastCategory.
Integration owns clearing in-memory Undo at the same time.
55. Appearance State
Allowed:
light
dark
system
Default:
system
State module only persists/validates this value.
UI owns actual theme rendering.
56. Last Category
Allowed:
all
science
technology
literature
history
weightlifting
iam
identity_automation
Default:
all
Category selection changes no preference weights.
57. Personalized Scoring Module
Implement:
js/ranking/personalize.js
as a deterministic side-effect-free function/module.
Exact formula:
personalizedScore =
base
+ sourcePreference
+ topicPreference
+ exploration
58. Base
Use:
article.score.base
unaltered.
Do not mutate generated scoring data.
59. Source Preference Contribution
Use current source weight for:
article.source.id
Range:
[-5,+5]
Missing:
0
60. Topic Preference Contribution
Sum weights for unique:
article.tags[].id
Then clamp:
[-6,+6]
No tags:
0
Do not average instead of sum; the frozen formula is sum then clamp.
61. Source Exploration
Map source interaction count:
0 → +3
1 → +2
2 → +1
3+ → 0
62. Topic Exploration
Use the lowest interaction count among Article tags:
0 → +2
1 → +1
2 → +0.5
3+ → 0
No tags:
0
63. Final Exploration
Calculate:
min(3, sourceExploration + topicExploration)
Range:
[0,+3]
No randomness.
64. Personalized Score Output
Expose conceptually:
{
  total,
  base,
  sourcePreference,
  topicPreference,
  exploration
}
This result is consumed by:
deck logic;
debug mode;
tests.
Do not add diversity penalty into this object as a persisted score component.
65. Final Score Range
Do not clamp final personalized score to:
0–100
Values above 100 are valid ordering values.
66. Candidate Pre-Sort
Before diversity:
personalized total descending;
base descending;
publication date descending, unknown last;
source ID ascending;
Article ID ascending.
Must be deterministic.
67. Deck Builder
Implement:
js/ranking/deck.js
conceptually:
buildDeck({
  articles,
  state,
  category
})
Responsibilities:
eligibility filtering;
category filtering;
personalized scoring;
pre-sort;
diversity sequencing;
deterministic output.
No DOM rendering.
68. Category Filtering
If:
category = all
all eligible Article categories participate.
Otherwise:
article.category == selected category
Do not infer alternate categories.
69. Same-Source Diversity
At each greedy selection position:
same source as immediately previous selected Article
→ temporary -8
This is not persistent and not a hard exclusion.
70. Category Diversity
Only in:
All
If candidate would create a third consecutive Article from the same category:
temporary -5
Disable entirely in category-specific views.
71. Sequencing Score
Temporary:
sequencingScore =
personalizedScore
+ sameSourcePenalty
+ categoryPenalty
Penalties are used only during deck construction.
Do not write them back to Article/state/preferences.
72. Diversity Tie-Breaks
For equal temporary sequencing score:
higher personalized score;
higher base;
newer publication date;
source ID ascending;
Article ID ascending.
73. Ranking Determinism
Do not use:
Math.random()
random shuffle
timestamp noise
Identical:
dataset
state
category
must produce identical deck order.
74. Opened Article Ranking
status = opened remains eligible.
Do not:
pin it to the front;
suppress it;
give a hidden opened penalty.
It participates through the normal score after its Open preference signal.
75. Debug Support
Ranking modules must expose enough real production values for debug mode:
base
sourcePreference
topicPreference
exploration
total
Debug mode itself is integrated later.
Do not build a second ranking implementation for debug purposes.
76. Read Later Aggregate Helpers
You may provide pure helper functions for:
saved Article selection;
saved count;
summed known readingTimeMinutes;
first available queue topic.
Do not render them.
Unknown reading time must not be treated as zero-duration content.
77. History Aggregate Helpers
You may provide pure helpers for:
History selection;
History count;
newest available topic;
known reading-time totals if integration/UI needs them.
Chronology remains:
readAt descending
78. JavaScript Test Strategy
Prefer:
Node.js built-in node:test
and built-in assertions.
Avoid third-party packages unless a required test genuinely cannot be implemented reasonably without them.
79. package.json
Create the minimum JavaScript tooling manifest required for tests/scripts.
Suggested scripts:
{
  "scripts": {
    "test": "node --test tests/js"
  }
}
Exact syntax may vary based on ESM/package configuration.
Do not add runtime dependencies.
80. ES Modules
Production JavaScript should use ES modules.
Configure tests so production modules can be imported directly rather than creating separate CommonJS copies.
Do not duplicate logic solely for testability.
81. Data Loader Tests
Cover:
valid dataset;
unsupported schema;
malformed/structurally invalid dataset;
fetch failure;
no storage side effects.
Use mocked/injected fetch behavior rather than live network.
82. Storage Tests
Cover:
default state;
valid state load/save;
malformed JSON;
unsupported version;
raw corrupt value preservation;
reset;
import;
export;
dangerous keys.
83. State Transition Tests
Cover all required transitions:
UNSEEN → OPENED
UNSEEN → SAVED
UNSEEN → DISMISSED
OPENED → SAVED
OPENED → DISMISSED
OPENED → READ
SAVED → READ
READ → SAVED
SAVED → DISMISSED
Verify:
status;
timestamps;
snapshots;
signals;
preference changes.
84. Idempotency Tests
Explicitly test:
Open twice
Save twice
Read twice
No duplicated learning.
85. Mark Unread Tests
Verify:
Read signal reversed
no new Save signal
prior Save/Open preserved
savedAt reset to now
readAt cleared
86. Remove Tests
Verify:
status saved → dismissed
no Dismiss preference
Save signal preserved
Open preserved
87. Undo Tests
Test both:
Save → Undo
Dismiss → Undo
with prior states:
unseen
opened
Verify exact restoration and signal reversal.
88. Preference Tests
Cover:
exact deltas;
source clamping;
topic clamping;
interaction counters;
reversal;
missing entries;
no category preferences.
89. Personalized Ranking Tests
Cover:
base preservation;
source weight;
topic sum + clamp;
exploration values;
final score above 100;
deterministic breakdown.
90. Exploration Tests
Exact source mapping:
0/1/2/3+
Exact topic mapping:
0/1/2/3+
Final cap:
3
91. Deck Tests
Cover:
eligibility;
category filtering;
deterministic pre-sort;
same-source penalty;
category penalty;
category penalty disabled outside All;
strong Article still winning despite penalty;
no persistent mutation.
92. Read Later / History Ordering Tests
Verify:
Read Later → savedAt descending
History    → readAt descending
No personalization reranking.
93. Import Security Tests
Cover at minimum:
malformed JSON
unsupported version
invalid Article ID
key/ID mismatch
unsafe URL scheme
invalid status
invalid category
invalid appearance
preference >5 or <-5
negative interaction count
invalid timestamp
__proto__
constructor
prototype
Every invalid candidate must leave existing state unchanged.
94. Controlled Time
Do not hardwire new Date() throughout transition logic.
Provide an injectable/current-time boundary so tests can assert exact timestamps.
Implementation style may vary.
The behavior must be deterministic under controlled time.
95. No Browser DOM Dependency
State/ranking modules should not require:
document
HTMLElement
CSS
DOM rendering
This keeps them directly testable under Node.
If a module needs browser-specific APIs such as localStorage or fetch, isolate them through boundaries/injection.
96. Security Boundaries
Treat imported and dataset content as untrusted data.
State/ranking code must not:
use eval;
interpret strings as code;
generate HTML;
construct unsafe deep merges.
Validate Article external URLs as HTTP/HTTPS where required.
97. No Pipeline Logic Duplication
Do not reimplement:
taxonomy classification;
canonical URL normalization;
source scoring;
content-type inference;
base score calculation.
The browser consumes the generated Article contract.
98. No UI Logic
Do not create:
card markup;
toast DOM;
navigation DOM;
Settings modal;
swipe gestures;
CSS.
Expose functions/data that integration/UI can consume.
99. No app.js
Do not create or modify:
js/app.js
during this workstream.
Cross-module orchestration belongs to integration.
100. Scope Discipline
Do not add:
backend sync;
IndexedDB;
Service Worker sync;
cloud backup;
embeddings;
AI ranking;
random exploration;
analytics;
category learning;
notifications.
Implement only frozen V1 behavior.
101. Test Command
Before completion, run:
node --test tests/js
or the approved equivalent if the repository test script is used.
All owned tests must pass.
102. npm Audit
If there are npm dependencies:
npm audit --audit-level=high
must pass according to 08-security-dependencies.md.
If there are genuinely zero third-party npm packages, report that explicitly rather than pretending an audit provided meaningful coverage.
103. Contract Conflict Procedure
If frozen state/ranking behavior cannot be implemented consistently:
stop;
identify exact spec sections;
explain conflict;
identify affected tests/interfaces;
report supervisor.
Do not edit docs/v1/**.
104. Completion Gate
The State/Ranking workstream is complete only when:
Article dataset loader implemented

localStorage abstraction implemented

state schema/version handling implemented

Article transitions implemented

preference learning/reversal implemented

Undo support logic implemented

import/export/reset implemented

personalized scoring implemented

exploration implemented

deck sequencing implemented

all owned tests pass

no forbidden paths changed

changes committed
105. Completion Report
Report exactly:
Workstream:
State / Ranking

Branch:
feat/state-ranking

Commit SHA:
<full SHA>

Owned paths changed:
<summary>

JavaScript test command:
<command>

Test result:
PASS / FAIL
<test count>

Third-party npm dependencies:
NONE / list

npm audit:
PASS / FAIL / NOT APPLICABLE

LocalStorage direct access outside storage.js:
NONE

Shared contract changes:
NONE

Known limitations:
NONE / details

Forbidden paths changed:
NONE
If shared contracts or forbidden paths changed, do not declare completion.
106. Commit and Stop
After owned gates pass:
inspect the branch diff;
commit only owned implementation/tests;
capture full SHA;
report completion;
stop.
Do not:
merge;
modify UI;
wire app.js;
consume pipeline feature branch;
implement Actions.
Integration owns composition.
Related Authoritative Documents
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/04-taxonomy-scoring.md
docs/v1/05-personalization-state.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
This workstream produces the deterministic browser-local state and ranking engine consumed by integration and UI. It must remain independent of DOM rendering and publisher ingestion.
