# Intentional Reading — V1 Specification Index

**Status:** Approved for V1 foundation\
**Audience:** Supervisor, implementation agents, integration agent, final-review agent\
**Purpose:** Entry point and authority map for the Intentional Reading V1 implementation

---

## 1. Purpose

This directory contains the authoritative implementation specifications for Intentional Reading V1.

Intentional Reading is a personal, static, text-first reading discovery and triage application designed to replace passive doomscrolling with deliberate high-value reading.

Implementation agents must treat these documents as requirements, not suggestions.

The project intentionally uses a specification-first, multi-agent implementation model.

Before implementation begins, the complete V1 specification package must exist on the shared foundation commit from which all feature branches/worktrees are created.

---

## 2. Authority Model

The V1 documentation is divided into:

1. authoritative product and technical specifications;
2. shared cross-agent contracts;
3. approved visual design references;
4. bounded implementation workstream packets.

The authoritative specifications define **what must be true**.

The workstream packets define **what each implementation agent is responsible for doing**.

Workstream documents must not redefine or override the authoritative specifications.

---

## 3. Required Reading Order

Every supervisor/integration agent must read the V1 package in this order:

```text
01-product.md
02-architecture.md
contracts.md
03-content-sources.md
04-taxonomy-scoring.md
05-personalization-state.md
06-ui-ux.md
07-pipeline-deployment.md
08-security-dependencies.md
09-testing-acceptance.md
design-reference/DESIGN.md
```

Implementation agents may focus more heavily on their owned specifications, but they must still understand:

```text
01-product.md
02-architecture.md
contracts.md
```

before modifying code.

---

## 4. Specification Map

### `01-product.md`

Defines:

- product purpose;
- V1 scope;
- primary navigation;
- Discover / Read Later / History behavior;
- article lifecycle at the product level;
- intentional-reading philosophy;
- explicit V1 non-goals.

This is the highest-level behavioral contract.

---

### `02-architecture.md`

Defines:

- static GitHub Pages architecture;
- build-time vs browser-time responsibilities;
- repository/module boundaries;
- Python pipeline architecture;
- frontend data/state/ranking/UI boundaries;
- parallel implementation model;
- worktree/integration model.

---

### `contracts.md`

Defines the shared cross-workstream contracts.

This includes:

- category IDs;
- generated dataset schema;
- Article schema;
- source configuration schema;
- topic configuration schema;
- local state schema;
- status values;
- learning signals;
- semantic UI actions;
- module interface expectations.

Shared contracts are frozen for feature implementation.

Contract Amendment 1 is incorporated:

> Every emitted Article must have a usable canonical HTTP/HTTPS URL. No GUID-, title-, or publication-date-derived fallback identity exists in V1.

---

### `03-content-sources.md`

Defines:

- the complete V1 publisher whitelist;
- source IDs;
- source categories;
- adapters;
- source-quality scores;
- content types;
- source-specific admission rules;
- OpenAI technical-content filtering;
- Anthropic Engineering inclusion;
- forced tags;
- source-catalog policies.

V1 contains exactly 22 configured sources unless this specification is formally revised.

---

### `04-taxonomy-scoring.md`

Defines:

- normalization;
- canonical URLs;
- stable Article IDs;
- topic aliases;
- topic matching;
- category scoping;
- admission filtering;
- forced-tag behavior;
- deduplication;
- metadata confidence;
- freshness;
- deterministic `0–100` base scoring.

---

### `05-personalization-state.md`

Defines:

- localStorage persistence;
- Article state transitions;
- snapshots;
- preference learning;
- preference reversals;
- Undo;
- import/export/reset;
- personalized ranking;
- deterministic exploration;
- diversity sequencing.

All personal reading/preference state remains browser-local.

---

### `06-ui-ux.md`

Defines:

- approved visual direction;
- color system;
- typography;
- spacing;
- Discover card;
- swipe behavior;
- Read Later;
- History;
- Settings;
- responsive behavior;
- accessibility;
- keyboard interaction;
- reduced motion.

Visual implementation must also follow:

```text
design-reference/DESIGN.md
design-reference/intentional-reading-prototype.png
```

---

### `07-pipeline-deployment.md`

Defines:

- source retrieval;
- retries/timeouts;
- failure isolation;
- dataset retention;
- catastrophic sanity gates;
- dataset generation;
- GitHub Actions;
- Pages artifact assembly;
- scheduled refresh;
- deployment behavior.

Scheduled refreshes do not create Git commits.

---

### `08-security-dependencies.md`

Defines:

- remote-content trust boundaries;
- frontend DOM safety;
- local import validation;
- SSRF protections;
- dependency policy;
- Python/npm audits;
- GitHub Action pinning;
- Dependabot;
- workflow permissions;
- privacy requirements.

---

### `09-testing-acceptance.md`

Defines:

- unit testing;
- contract testing;
- integration testing;
- UI/manual verification;
- security gates;
- responsive matrix;
- accessibility acceptance;
- production artifact verification;
- initial live-source validation;
- final V1 Definition of Done.

---

## 5. Approved Design References

The design references live under:

```text
design-reference/
```

Expected contents:

```text
DESIGN.md
intentional-reading-prototype.png
README.md
```

`DESIGN.md` is authoritative for visual-system details.

The prototype screenshot is a visual reference.

Prototype article content is not authoritative source-catalog content.

The design references must not override:

- product semantics;
- security rules;
- state contracts;
- architecture boundaries.

If a conflict exists, escalate it rather than silently resolving it.

---

## 6. Workstream Documents

Implementation task packets live under:

```text
docs/v1/workstreams/
```

Expected files:

```text
foundation.md
content-pipeline.md
state-ranking.md
frontend-ui.md
integration.md
final-review.md
```

These files define:

- agent mission;
- required reading;
- path ownership;
- forbidden paths;
- expected outputs;
- test gates;
- completion report.

They do not replace the authoritative specifications.

---

## 7. Path Ownership During Parallel Implementation

After the foundation is frozen, three primary feature workstreams run independently.

At most these three feature worktrees may run in parallel.

### Content Pipeline

Owns:

```text
pipeline/**
config/**
tests/pipeline/**
```

---

### State / Ranking

Owns:

```text
js/data/**
js/state/**
js/ranking/**
tests/js/**
```

---

### Frontend UI

Owns:

```text
index.html
css/**
js/ui/**
```

---

### Integration ownership

During parallel implementation:

```text
js/app.js
```

belongs to integration/supervisor ownership.

Agents must not modify another workstream's owned paths unless explicitly authorized.

---

## 8. Feature Branch / Worktree Model

All feature branches must start from the same frozen foundation commit.

Conceptually:

```text
main
  │
  └── v1-foundation
          │
          ├── feat/content-pipeline
          ├── feat/state-ranking
          └── feat/frontend-ui
```

Integration occurs through:

```text
integration/v1
```

Feature agents:

1. implement;
2. test;
3. commit;
4. report their SHA;
5. stop.

Feature agents do not merge their own branches.

---

## 9. Contract Freeze Rule

After the final foundation commit is declared:

```text
FOUNDATION_SHA
```

feature agents must not independently change:

- Article schema;
- dataset schema;
- source/topic schemas;
- category IDs;
- local-state schema;
- semantic UI action names;
- ranking formulas;
- preference deltas;
- shared path ownership.

If a contract issue is discovered:

```text
STOP
  ↓
document exact conflict
  ↓
report to supervisor
  ↓
supervisor decides whether specification amendment is required
```

---

## 10. Implementation Discipline

Agents must:

- prefer deterministic implementations;
- keep functions/modules bounded;
- respect architecture boundaries;
- use tests for owned logic;
- avoid speculative features;
- avoid unrelated refactoring;
- avoid adding dependencies without specification justification;
- treat remote publisher data as untrusted;
- preserve the static/no-backend architecture.

Agents must not interpret:

> "while I am here"

as permission to expand scope.

---

## 11. Dependency Policy

V1 targets:

```text
frontend runtime npm dependencies: 0
```

JavaScript testing should prefer:

```text
Node.js built-in node:test
```

Python runtime dependencies must remain small and reviewed.

Dependency and supply-chain requirements are authoritative in:

```text
08-security-dependencies.md
```

---

## 12. Implementation Completion

No workstream is complete merely because code exists.

Completion requires:

```text
implementation
+
owned tests
+
manual checks where applicable
+
clean commit
+
reported SHA
+
known-limitations report
```

The supervisor must integrate workstreams one at a time and run relevant gates after each integration.

---

## 13. Final Release Gate

V1 may merge to `main` only after:

- all three feature streams are integrated;
- automated tests pass;
- security audits pass;
- source validation is reviewed;
- responsive UI matrix passes;
- accessibility acceptance passes;
- production artifact passes local verification;
- repository security settings are verified;
- no unresolved specification conflicts remain.

The authoritative completion requirements live in:

```text
09-testing-acceptance.md
```

---

## 14. Specification Change Policy

During implementation, specifications may be changed only through deliberate supervisor-controlled amendments.

A specification change must:

1. identify the conflict/problem;
2. identify affected documents/contracts;
3. define the new decision;
4. update all affected specifications;
5. inform all affected workstreams;
6. be committed before dependent implementation continues.

Silent contract drift is prohibited.

---

## 15. Guiding Constraint

When implementation choices are ambiguous, prefer the solution that best preserves:

```text
intentional attention
determinism
clarity
small attack surface
local privacy
low dependency count
testability
static architecture
```

Do not add complexity merely because a more elaborate architecture is technically possible.
