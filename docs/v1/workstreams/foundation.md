# Intentional Reading — V1 Foundation Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/foundation.md`\
**Workstream type:** Supervisor / foundation preparation\
**Implementation work:** Prohibited

---

## 1. Mission

Prepare and freeze the complete V1 specification foundation before any feature implementation begins.

This workstream exists to ensure that:

- all authoritative V1 specifications are present;
- approved design references are present;
- shared contracts are internally consistent;
- repository instructions are clear;
- future feature branches begin from exactly the same commit;
- no implementation agent must invent cross-workstream behavior.

This is a specification and repository-structure task.

It is **not** a coding workstream.

---

## 2. Starting Point

The foundation work begins from:

```text
branch: v1-foundation
bootstrap commit:
24b9d8d0074222925c42181abbf570bee4d2dab9
```

The bootstrap commit contains the documentation skeleton only.

The existing production application must remain functionally unchanged during foundation preparation.

---

## 3. Required Inputs

The supervisor must have the approved versions of:

```text
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/03-content-sources.md
docs/v1/04-taxonomy-scoring.md
docs/v1/05-personalization-state.md
docs/v1/06-ui-ux.md
docs/v1/07-pipeline-deployment.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
```

and:

```text
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
```

The approved `contracts.md` must include Contract Amendment 1:

> Every generated V1 Article must have a usable canonical HTTP/HTTPS URL. If normalization cannot produce one, reject the source entry. V1 has no GUID-, title-, or publication-date-derived fallback Article identity.

---

## 4. Required Reading

Before changing foundation files, the supervisor must read:

```text
AGENTS.md
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/03-content-sources.md
docs/v1/04-taxonomy-scoring.md
docs/v1/05-personalization-state.md
docs/v1/06-ui-ux.md
docs/v1/07-pipeline-deployment.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
design-reference/DESIGN.md
```

Do not populate files blindly without first understanding the complete specification package.

---

## 5. Foundation Deliverables

The completed foundation must contain:

```text
docs/v1/
├── README.md
├── 01-product.md
├── 02-architecture.md
├── contracts.md
├── 03-content-sources.md
├── 04-taxonomy-scoring.md
├── 05-personalization-state.md
├── 06-ui-ux.md
├── 07-pipeline-deployment.md
├── 08-security-dependencies.md
├── 09-testing-acceptance.md
└── workstreams/
    ├── foundation.md
    ├── content-pipeline.md
    ├── state-ranking.md
    ├── frontend-ui.md
    ├── integration.md
    └── final-review.md
```

and:

```text
design-reference/
├── DESIGN.md
├── intentional-reading-prototype.png
└── README.md
```

---

## 6. Permitted Changes

This workstream may modify:

```text
AGENTS.md
docs/v1/**
design-reference/**
```

It may make small repository housekeeping changes only when necessary to support the frozen specification package.

Examples:

- correcting documentation paths;
- fixing Markdown links;
- updating design-reference README;
- correcting filename casing.

---

## 7. Forbidden Changes

The foundation workstream must not implement V1 application behavior.

Do not modify application code such as:

```text
index.html
script.js
style.css
js/**
css/**
pipeline/**
config/**
tests/**
.github/workflows/**
requirements*.txt
package.json
package-lock.json
```

unless the supervisor explicitly determines that an existing documentation-only bootstrap file requires a path correction and the change contains no implementation.

No dependencies may be installed or introduced during foundation preparation.

---

## 8. Existing Application Preservation

The legacy application must continue to exist unchanged after the foundation commit.

Do not:

- remove `script.js`;
- remove `style.css`;
- rewrite `index.html`;
- replace current RSS behavior;
- add V1 UI;
- add Python ingestion;
- add localStorage state;
- add Actions workflows.

Those belong to later implementation/integration stages.

---

## 9. Specification Integrity Review

Before declaring the foundation frozen, the supervisor must inspect the entire package for contradictions.

At minimum verify consistency of:

### Category IDs

```text
science
technology
literature
history
weightlifting
iam
identity_automation
```

Synthetic filter:

```text
all
```

---

### Primary UI terminology

```text
Read Later
Discover
History
```

---

### Article identity

```text
usable canonical HTTP/HTTPS URL
→ SHA-256
→ first 20 lowercase hexadecimal characters
```

No fallback identity.

---

### Dataset schema

```text
schemaVersion = 1
```

---

### Local state

```text
localStorage key:
intentionalReading:v1

schemaVersion = 1
```

---

### Persisted statuses

```text
opened
saved
dismissed
read
```

---

### Preference deltas

```text
Open:
source +0.10
topic  +0.05

Save:
source +0.45
topic  +0.30

Dismiss:
source -0.35
topic  -0.20

Read:
source +0.25
topic  +0.20
```

---

### Ranking

```text
personalizedScore =
base
+ sourcePreference
+ topicPreference
+ exploration
```

Bounds:

```text
sourcePreference: [-5,+5]
topicPreference:  [-6,+6]
exploration:       [0,+3]
```

---

### Diversity

```text
same immediate source:
-8 temporary penalty

third consecutive category in All:
-5 temporary penalty
```

---

### Source catalog

Exactly:

```text
22 configured V1 sources
```

unless an approved specification amendment changes the count.

---

### Retention

```text
45-day known-date maximum age
40 articles/source
500 articles total
```

---

### Catastrophic dataset gates

```text
>=20 retained articles
>=50% enabled sources operationally successful
```

---

### UI interactions

```text
Left swipe / Left Arrow
→ Not Interested

Right swipe / Right Arrow
→ Save for Later

Z
→ Undo
```

Swipe threshold:

```text
90px
```

---

## 10. Cross-Document Conflict Rule

If the supervisor discovers a genuine contradiction:

```text
STOP
```

Do not silently choose one interpretation.

Report:

1. conflicting documents/sections;
2. exact conflicting requirements;
3. affected future workstreams;
4. recommended resolution.

A specification amendment must be approved before affected implementation begins.

---

## 11. Source Endpoint Review

Before the foundation is declared implementation-ready, inspect `03-content-sources.md` for:

- malformed URLs;
- missing source IDs;
- duplicated IDs;
- mismatch with taxonomy admission IDs;
- inconsistency with adapter types.

Do not modify approved source semantics merely because a different implementation would be easier.

Live source validation belongs to the Pipeline workstream.

---

## 12. Taxonomy Contract Review

Verify every:

```text
forcedTags
admissionTopicIds
```

value in the source specification exists in the approved taxonomy.

Verify cross-category topics remain exactly as specified.

Do not create vendor-name topics such as:

```text
okta
openai
anthropic
n8n
```

unless formally added through a future taxonomy amendment.

---

## 13. UI / State Contract Review

Ensure UI action semantics align with state transitions.

Examples:

```text
save
→ saved
→ Read Later

mark_read
→ read
→ History

mark_unread
→ saved
→ Read Later

remove from Read Later
→ dismissed
→ NO negative preference signal
```

Opening must remain separate from reading.

---

## 14. Design Reference Review

Verify that:

```text
design-reference/DESIGN.md
```

matches the approved V1 UI specification for:

- light/dark palette;
- typography;
- spacing;
- card anatomy;
- navigation;
- responsive breakpoints;
- swipe behavior;
- motion;
- accessibility.

Prototype article/source content is illustrative only.

The design screenshot must not alter `03-content-sources.md`.

---

## 15. AGENTS.md Role

Root `AGENTS.md` must remain concise.

It should instruct implementation agents to:

- read the V1 specification index;
- respect workstream ownership;
- preserve frozen contracts;
- avoid unrelated changes;
- run required tests;
- commit and report SHA;
- never self-merge.

It should not duplicate hundreds of lines from the specifications.

---

## 16. No Premature Worktrees

Do not create:

```text
feat/content-pipeline
feat/state-ranking
feat/frontend-ui
```

worktrees until:

1. all specification files are populated;
2. all workstream packets are populated;
3. contract review passes;
4. foundation changes are committed;
5. the final commit SHA is recorded as `FOUNDATION_SHA`.

---

## 17. Foundation Preflight

Before the final foundation commit, the supervisor must produce a preflight report containing:

### Documentation status

Confirm every required file exists.

### Contract summary

Summarize the major frozen shared contracts.

### Dependency graph

Describe dependencies among:

```text
Pipeline
State/Ranking
Frontend UI
Integration
```

### Path ownership matrix

Confirm owned and forbidden paths for each feature stream.

### Planned branches/worktrees

Expected:

```text
feat/content-pipeline
feat/state-ranking
feat/frontend-ui
integration/v1
```

### Test gates

Summarize workstream completion gates.

### Ambiguities/conflicts

List any unresolved ambiguity.

If any material contradiction remains:

```text
DO NOT START IMPLEMENTATION
```

---

## 18. Final Foundation Commit

After successful preflight:

1. ensure repository status is clean except intended foundation changes;
2. inspect the diff;
3. commit the complete foundation package;
4. record the full commit SHA.

That SHA becomes:

```text
FOUNDATION_SHA
```

All three parallel feature branches/worktrees must start from exactly this SHA.

---

## 19. Foundation SHA Rule

The previously completed bootstrap commit:

```text
24b9d8d0074222925c42181abbf570bee4d2dab9
```

is **not** the final implementation foundation SHA.

It is only the skeleton/bootstrap base.

The new commit containing the complete approved specifications and workstream packets becomes the true:

```text
FOUNDATION_SHA
```

---

## 20. Completion Report

When the foundation workstream is complete, report:

```text
Foundation branch:
v1-foundation

Foundation SHA:
<full SHA>

Files populated:
<summary>

Contract review:
PASS / FAIL

Unresolved specification conflicts:
NONE / details

Application implementation changed:
NO

Repository status:
CLEAN
```

If implementation files changed unexpectedly, foundation completion must not be declared until that change is explained or reverted.

---

## 21. Stop Condition

After recording `FOUNDATION_SHA`:

```text
STOP
```

Do not begin feature implementation in the foundation worktree.

The next step is supervisor orchestration:

1. create three branches/worktrees from `FOUNDATION_SHA`;
2. assign workstream packets;
3. launch bounded feature agents.

---

## Related Authoritative Documents

- `docs/v1/README.md`
- `docs/v1/01-product.md`
- `docs/v1/02-architecture.md`
- `docs/v1/contracts.md`
- `docs/v1/03-content-sources.md`
- `docs/v1/04-taxonomy-scoring.md`
- `docs/v1/05-personalization-state.md`
- `docs/v1/06-ui-ux.md`
- `docs/v1/07-pipeline-deployment.md`
- `docs/v1/08-security-dependencies.md`
- `docs/v1/09-testing-acceptance.md`
- `design-reference/DESIGN.md`

This workstream prepares the implementation foundation. It must not implement the product.
