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

Approved Amendment 4, **Initial Release Upstream Source Failure Acceptance**, is also incorporated. It permits the initial V1 release to proceed while the specifically investigated upstream conditions for `n8n_release_notes` and `ietf_scim` persist, provided those conditions still match their documented failure modes, normal catastrophic dataset gates pass, and there is no implementation regression. Both sources remain enabled and continue to be reported as failed; this amendment does not make `--validate-sources` pass or accept unrelated or changed failures.

Approved Amendment 5, **OpenAI and Okta Workflows Deferral to V2**, removes `openai_release_notes` and `okta_workflows` from the V1 source catalog after initial-release investigation found that OpenAI retrieval is intermittently blocked by a managed challenge and Okta publishes no usable entry-specific URLs. V1 contains exactly 20 configured sources. Neither deferred source may be fetched, emitted, counted, or treated as an accepted V1 failure. Reintroducing either source requires a future V2 source-catalog amendment with contract-compatible retrieval and canonical Article identity.

Approved Amendment 6, **Native Android Client Authorization**, permits a native Android client to be built inside this repository as an additional read-only consumer of the frozen `ArticleDataset v1` contract. The client is a separate delivery surface, not a change to the V1 web application: the vanilla HTML, CSS, and JavaScript rule continues to bind the browser runtime, the Python pipeline remains the sole source of Article data, and this amendment introduces no backend, no authentication, no telemetry, and no pipeline, source-catalog, or taxonomy change. All Android code is confined to `/android`, which is outside the deployed Pages allowlist and therefore cannot affect the web artifact. The Android client must consume the dataset contract as frozen; it may not widen, reinterpret, or fork the Article schema, category IDs, content types, status values, or learning signals, and any contract change it needs requires its own amendment. Feature parity with the web client is explicitly not required and is delivered incrementally under numbered specification items.

Approved Amendment 7, **Discover Composition Ordering**, changes the ordering rule in `06-ui-ux.md` §21 so that Discover's decision surface leads the viewport. A compact masthead — a small metadata eyebrow and the screen title — remains the first thing on Discover; the operational block — concise purpose copy, the refresh affordance, content-age and failed-refresh disclosure, available-article context, and the category selector — follows the article card rather than preceding it. The intent is binding and the widget order is illustrative: **on Discover, the first thing in the viewport is the article card, not the controls that describe it.** A later redesign may re-lay out Discover freely provided that intent is preserved. This amendment changes no contract, no state, no status, no learning behaviour, no copy, and no action semantics; it does not alter §23's one-primary-card rule, it does not change what the header may contain, and it applies to Discover only — Read Later and History keep their full editorial headers. It is a placement rule and nothing else.

Approved Amendment 8, **Undo Scope: Reversible Actions and Offer Surfaces**, widens V1 Undo along two axes and states the arithmetic for the widened set. **Surfaces:** an eligible action may raise the undo offer from any surface that performs it — the trigger, whether a swipe, a labelled control, or a keyboard shortcut where one exists, does not determine reversibility; the action does. This supersedes the swipe-scoped wording previously carried by `contracts.md` §31, `05-personalization-state.md` §36, `06-ui-ux.md` §70, `01-product.md` §14, and `09-testing-acceptance.md` §50. **Actions:** the reversible set becomes save, dismiss, mark read, mark unread, and remove from Read Later. Open is not reversible and does not become so; import, reset, and appearance changes are not reversible and continue to clear the undo record. **Arithmetic:** reversing an action restores the exact pre-action record, and in addition — Undo Remove applies no preference change in either direction, because Remove applies none (§24); Undo Mark Read reverses the Read signal only if the forward action applied it; and Undo Mark Unread **re-applies** the Read signal that Mark Unread reversed, restoring `signalsApplied.read = true`, which is the only place in V1 where reversing an action applies a signal rather than reversing one. **Scope of the record:** still exactly one undo record, still memory-only, still cleared by reload, import, and reset. Any eligible action replaces it regardless of which destination performed it, and because the record names an article, an offer raised on one destination remains valid if the reader changes destination inside the offer's lifetime. **Reach:** this amendment is permissive, not obligatory. It authorizes the wider scope without requiring every client to implement it; the browser's existing scope — swipe and keyboard triggers, save and dismiss only — remains compliant, and no `js/**` change is required by this amendment. No delta, clamp, status value, eligibility rule, idempotency rule, or storage contract changes.
Approved Amendment 9, **Android Client Visual Direction: Material 3 Expressive**, gives the native Android client a visual direction of its own, distinct from the browser runtime's. Amendment 6 admitted the Android client as a separate delivery surface consuming the frozen dataset contract read-only; this amendment completes that split on the visual axis. **Reach:** the new direction binds the Android client only. The browser runtime's visual specification is unchanged in substance, remains binding on it, and is not made non-compliant by this amendment; no `js/**` or `css/**` change is authorized or required, and the web client is not scheduled for redesign. **Mechanism:** `06-ui-ux.md` is rewritten as a second edition that scopes every rule at the point where it is stated — each section opens with a `Binds:` line naming the surfaces it governs, and sections where the two surfaces genuinely differ carry both treatments side by side. Section numbers are unchanged, so every existing citation continues to resolve to its subject; `06-ui-ux.md` §80 records what changed inside each section, and §§76–79 are new. **Behaviour is never scoped:** every rule about what the application does — state transitions, counts, signals, action semantics, gesture outcomes, keyboard bindings, live status, undo, accessibility — binds both surfaces without exception, and only presentation is ever split. **No behaviour change is authorized** — not a state transition, status value, learning signal, delta, clamp, count, ranking, deck ordering, undo path, undo arithmetic, gesture semantic, keyboard binding, or authored string; Amendment 7's placement rule and Amendment 8's undo scope both remain binding. Android colour is authored as ten seeds and derived in Oklch, widening the browser's six-seed rule in count and keeping it in kind, and no component outside the theme package may name a colour. The dark scheme is derived from the same seeds rather than separately authored, because Settings offers Light/Dark/System on both surfaces and a light-only palette would regress a shipped control. The two contested seed values are settled by owner decision as named exceptions to the design sources' precedence order, which continues to govern every value it does not name. Fonts ship as bundled `res/font/` assets, which adds no Gradle dependency and fetches nothing at runtime; `androidx.compose.ui.text.googlefonts` is not authorized. The information architecture does not change. Article imagery remains prohibited on both surfaces: `ArticleDataset v1` carries no image field, and adding one is a frozen-contract change requiring its own amendment. No contract, schema, status, signal, action, storage, source-catalog or taxonomy change.

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
- Anthropic Engineering inclusion;
- forced tags;
- source-catalog policies.

V1 contains exactly 20 configured sources under Amendment 5.

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

Defines, for **both** delivery surfaces:

- approved visual direction, per surface;
- colour systems and their token architectures;
- typography;
- spacing, radii, shadows;
- navigation and information architecture;
- Discover card;
- swipe behavior;
- Read Later;
- History;
- Settings;
- responsive behavior;
- accessibility;
- keyboard interaction;
- reduced motion.

Since **Amendment 9** the document is in its **second edition** and is explicitly two-surface. Every section
opens with a `Binds:` line stating which surfaces it governs. **Behaviour sections bind both surfaces
without exception; only presentation is ever scoped.** Section numbers are unchanged from the first edition,
and §80 records what changed inside each one.

Visual implementation must follow the source for the surface being built, and neither source may be cited
against the other surface:

```text
browser   design-reference/DESIGN.md
          design-reference/intentional-reading-prototype.png

Android   specs/design/m3-expressive-DESIGN.md
          specs/design/m3-expressive-PRD.md
```

The two Android sources disagree with each other; their precedence order is fixed in `06-ui-ux.md` §2.2.

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
