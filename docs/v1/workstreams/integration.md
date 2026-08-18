# Intentional Reading — V1 Integration Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/integration.md`\
**Workstream type:** Supervisor / integration implementation\
**Primary branch:** `integration/v1`\
**Primary ownership:** shared composition, workflows, legacy migration, cross-workstream wiring

---

## 1. Mission

Integrate the three completed V1 feature workstreams into one production-ready static application while preserving all frozen contracts.

This workstream connects:

```text
Content Pipeline
+
State / Ranking
+
Frontend UI
```

into the complete V1 application.

Integration owns the shared surfaces that feature agents intentionally avoided, including:

- `js/app.js`;
- cross-module wiring;
- GitHub Actions workflows;
- production artifact assembly;
- final package manifests where shared;
- legacy `script.js` / `style.css` cleanup;
- integration defects;
- end-to-end behavior;
- full release-gate execution before final review.

Integration is not permission to redesign subsystems.

The preferred strategy is:

> compose tested feature implementations with minimal corrective changes.

---

## 2. Starting Point

Create:

```text
integration/v1
```

from exactly:

```text
FOUNDATION_SHA
```

Do not create integration from:

```text
main
bootstrap SHA
one feature branch tip
```

The integration branch must share the same frozen base as all feature branches.

---

## 3. Required Feature Inputs

Before integration begins, the supervisor must have completion reports and full commit SHAs for:

```text
feat/content-pipeline
feat/state-ranking
feat/frontend-ui
```

Record them as:

```text
PIPELINE_SHA
STATE_RANKING_SHA
FRONTEND_UI_SHA
```

Do not integrate an incomplete feature branch merely because its code appears usable.

Each feature branch must have passed its own workstream completion gate.

---

## 4. Required Reading

Before merging/cherry-picking feature work, read:

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
docs/v1/workstreams/integration.md
```

Also review all three feature completion reports before touching shared files.

---

## 5. Integration Order

Integrate one feature stream at a time.

Recommended order:

```text
1. Content Pipeline
2. State / Ranking
3. Frontend UI
4. Shared composition / deployment
```

After each feature integration:

1. inspect diff/conflicts;
2. run relevant tests;
3. verify shared contracts;
4. fix only clearly integration-owned issues;
5. stop and return feature defects to owning branch when appropriate.

Do not merge all three and debug the resulting system as one undifferentiated change.

---

## 6. Feature Integration Method

Use normal Git integration from the completed feature SHAs.

Acceptable approaches include:

```text
git merge --no-ff
```

or deliberate cherry-pick of the feature's bounded commit set if the supervisor has a clear reason.

Prefer preserving each workstream's history when practical.

Do not copy feature files manually unless resolving a documented conflict.

---

## 7. Ownership During Integration

Integration may modify shared/application-wide surfaces including:

```text
js/app.js
.github/workflows/**
.github/dependabot.yml
.gitignore
README.md
package.json
package-lock.json
requirements*.txt
index.html
css/**
js/**
pipeline/**
config/**
tests/**
```

but this broader permission exists only for:

- integration wiring;
- release tooling;
- contract-compatible corrective fixes;
- legacy cleanup;
- cross-module issues.

It is not permission for broad refactoring.

---

## 8. Feature Defect Rule

When integration discovers a defect clearly owned by one feature subsystem:

Examples:

```text
pipeline scoring wrong
state transition wrong
UI swipe threshold wrong
```

preferred process:

```text
report defect to owning feature worktree
→ fix there
→ run owned tests
→ commit fix
→ integrate new feature SHA/fix commit
```

This preserves ownership and avoids integration silently becoming a fourth feature branch.

---

## 9. Integration-Owned Fixes

Integration may fix small defects directly when they are genuinely cross-module, such as:

- mismatched callback wiring;
- import/export boundary mismatch;
- missing module export needed exactly as specified;
- asset path errors;
- CSP composition;
- workflow command wiring;
- removal of legacy references;
- route/view coordination.

Keep such fixes small and documented.

---

## 10. Contract Changes Prohibited

Do not change frozen shared contracts during normal integration.

Examples:

```text
Article schema
local-state schema
status values
category IDs
preference deltas
ranking formula
semantic action names
source catalog
```

If integration proves a frozen contract is actually defective:

```text
STOP
→ document exact conflict
→ create supervisor-controlled spec amendment
→ update affected workstreams
→ resume only after approval
```

---

## 11. js/app.js Ownership

Integration creates:

```text
js/app.js
```

as the application composition root.

Its job is to coordinate modules.

It must not duplicate their algorithms.

---

## 12. app.js Responsibilities

Conceptually:

```text
initialize
    ↓
load local state
    ↓
apply appearance
    ↓
load ArticleDataset
    ↓
derive current destination/category
    ↓
build view models
    ↓
render UI
    ↓
handle semantic actions
    ↓
delegate state/ranking work
    ↓
persist
    ↓
rerender affected views
```

---

## 13. app.js Non-Responsibilities

Do not reimplement inside `app.js`:

- taxonomy;
- base scoring;
- URL normalization;
- preference delta math;
- exploration;
- diversity sequencing;
- localStorage serialization;
- swipe mechanics;
- DOM styling.

If `app.js` starts becoming a monolith, move behavior back into its owning module.

---

## 14. Dataset Startup

At startup:

1. load local state independently;
2. apply local appearance/navigation state;
3. attempt to load current `data/articles.json`.

Important:

```text
local state availability
≠
current dataset availability
```

Read Later, History, and Settings must remain usable even if dataset loading fails.

---

## 15. Discover Startup

When the dataset is valid:

```text
load articles
→ build deck from current state/category
→ render Discover
```

When dataset loading fails:

```text
render feed error
→ preserve local Read Later / History
```

Do not wipe state.

---

## 16. Action Orchestration

Canonical semantic actions:

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

Integration maps them to state and UI behavior.

---

## 17. Save Flow

Conceptually:

```text
UI emits save
→ derive SAVED transition
→ apply Save preference signal
→ attempt persistence
→ on success:
     store Undo
     rebuild deck
     update counts
     rerender
     show success toast
→ on failure:
     keep/restore prior visible state
     show storage error
```

UI must not be told success before persistence succeeds.

---

## 18. Dismiss Flow

Conceptually:

```text
UI emits dismiss
→ derive DISMISSED transition
→ apply Dismiss preference
→ persist
→ on success:
     store Undo
     rebuild deck
     rerender
     show status
```

Same transactional rule applies.

---

## 19. Open Flow

For `open`:

1. derive/apply first-open transition when needed;
2. attempt local persistence;
3. after that attempt, initiate external navigation whether persistence succeeded or failed;
4. preserve current Article status semantics;
5. do not mark read.

If persistence succeeds, retain the Open interaction normally before navigation. If persistence fails, surface a local-state warning, do not claim that the Open interaction was persisted, and still open the publisher Article. Reading must not become unavailable merely because browser storage is unavailable.

This exception applies specifically to Open/external reading navigation. Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset must not be rendered as successful state transitions when persistence fails.

---

## 20. Mark Read Flow

```text
state transition
→ apply Read signal once
→ persist
→ update Read Later/History
→ rerender
```

Opening and reading remain separate.

---

## 21. Mark Unread Flow

```text
reverse Read signal
→ READ → SAVED
→ new savedAt
→ persist
→ update counts
→ rerender
```

Do not create a new Save learning signal.

---

## 22. Remove Flow

```text
SAVED → DISMISSED
→ preserve prior Save/Open signals
→ no negative Dismiss preference
→ persist
→ rerender Read Later
```

---

## 23. Undo Flow

Undo is available only for the most recent successful:

```text
save
dismiss
```

Integration must:

- call appropriate state/preference reversal behavior;
- persist restored state;
- rebuild Discover/counts;
- rerender;
- clear/replace Undo state as required.

Undo remains in memory only.

---

## 24. Navigation

Integration owns current destination:

```text
read_later
discover
history
```

Settings is modal/secondary.

Navigation must not cause publisher refetching.

---

## 25. Routing Choice

A lightweight routing mechanism may be used.

Preferred options:

```text
application view state
URL hash
```

No routing framework.

If hashes are used, keep them stable and simple.

Example:

```text
#discover
#read-later
#history
```

The exact hash strings are integration implementation details, not new shared contracts.

---

## 26. Category Change

When UI emits:

```text
category_change
```

integration:

1. validates category;
2. updates `session.lastCategory`;
3. persists state;
4. rebuilds Discover;
5. rerenders category selection/card.

No preference update.

---

## 27. Appearance Change

When UI emits:

```text
appearance_change
```

integration:

1. validates enum;
2. updates local state;
3. persists;
4. applies UI theme.

No ranking/state-learning effect.

---

## 28. Export Flow

Integration requests current valid state from storage and produces the approved JSON backup.

Recommended filename format:

```text
intentional-reading-backup-YYYYMMDD-HHMMSSZ.json
```

No network request.

---

## 29. Import Flow

UI selects file and enforces the approved size pre-check.

Integration/state:

```text
read text
→ validate entire candidate
→ if invalid:
     keep current state
     show error
→ if valid:
     replace persisted state atomically
     clear in-memory Undo
     reload state
     apply appearance
     rebuild deck
     rerender all destinations/counts
```

No partial merge.

---

## 30. Reset Flow

After explicit UI confirmation:

```text
reset persisted state
→ clear Undo
→ apply System appearance
→ category = All
→ rebuild deck from current dataset
→ rerender
```

Current static dataset remains unchanged.

---

## 31. Debug Mode

Integration detects:

```text
?debug=1
```

or equivalent approved mechanism.

It passes actual production ranking breakdowns to the UI.

Debug mode must not alter:

- ranking;
- persistence;
- deck order;
- interaction behavior.

---

## 32. Read Later View Model

Integration obtains saved items from State/Ranking and passes:

- local Article snapshots;
- queue count;
- known reading-time aggregate;
- next topic;
- History count.

Do not reconstruct saved items from current dataset.

---

## 33. History View Model

Integration obtains read items from State/Ranking and passes:

- local Article snapshots;
- History count;
- relevant overview values;
- Read Later count.

History grouping is presentation-oriented and may be performed by UI using supplied `readAt`.

---

## 34. Legacy script.js Migration

After the complete ES-module application functions correctly:

```text
script.js
```

may be removed.

Before deletion, verify:

- `index.html` no longer references it;
- no V1 functionality depends on it;
- no useful logic remains only there.

Do not retain duplicated legacy functionality "just in case."

---

## 35. Legacy style.css Migration

After `css/app.css` fully replaces legacy styles:

```text
style.css
```

may be removed.

Verify `index.html` and production artifact reference only the approved V1 stylesheet(s).

---

## 36. rss2json Removal

Final integrated frontend must contain no runtime references to:

```text
rss2json
```

Search the repository.

Expected production runtime usage:

```text
NONE
```

---

## 37. LoremFlickr / Images Removal

Final V1 must contain no runtime:

```text
LoremFlickr
random image
fallback thumbnail
article image
```

behavior.

Search and remove obsolete legacy references where integration owns them.

---

## 38. Unsafe DOM Review

Search integrated frontend for:

```text
innerHTML
outerHTML
insertAdjacentHTML
document.write
```

Remote Article metadata must not flow through unsafe HTML sinks.

Preferred outcome:

```text
no application runtime usage
```

Any static-only exception requires explicit security review.

---

## 39. Runtime Dependency Review

Final frontend must have:

```text
0 npm runtime dependencies
```

If feature integration accidentally introduced one:

```text
STOP
→ remove or request formal spec change
```

---

## 40. package.json Integration

Combine JavaScript test/tooling configuration without changing runtime architecture.

Preferred:

```text
Node 24 LTS
node:test
zero third-party npm packages
```

If approved dev dependencies exist, preserve exact pins and lockfile.

---

## 41. Python Dependency Integration

Preserve Pipeline's exact reviewed manifests.

Do not casually update versions during integration.

Dependency changes require normal security review and audits.

---

## 42. GitHub Workflow Files

Integration owns creation of:

```text
.github/workflows/test.yml
.github/workflows/deploy.yml
.github/dependabot.yml
```

according to:

```text
07-pipeline-deployment.md
08-security-dependencies.md
09-testing-acceptance.md
```

---

## 43. test.yml

Configure code-change validation for appropriate:

```text
pull_request
push to main
```

gates.

It must run applicable:

- Python tests;
- JS tests;
- configuration validation;
- dependency audits.

Avoid live-publisher dependency in deterministic test jobs.

---

## 44. deploy.yml Triggers

Configure:

```text
push to main
workflow_dispatch
schedule
```

Scheduled cron:

```text
17 */6 * * *
```

---

## 45. Push / Manual Deployment Gate

For code-triggered or manual release:

```text
checkout
→ setup Python/Node
→ install reviewed dependencies
→ tests
→ security audits
→ generate live dataset
→ sanity validate
→ assemble Pages artifact
→ deploy
```

No failing gate may proceed to production deployment.

---

## 46. Scheduled Refresh Path

Scheduled content refresh:

```text
checkout default branch
→ setup Python
→ install pipeline runtime dependencies
→ validate config
→ generate live dataset
→ sanity validate
→ assemble artifact
→ deploy
```

Do not rerun unrelated frontend tests every six hours when application code did not change.

---

## 47. Pages Artifact

Assemble a clean directory such as:

```text
.build/pages/
```

Allowed production content:

```text
index.html
css/**
js/**
data/articles.json
approved runtime static assets
```

---

## 48. Production Artifact Exclusions

Do not publish:

```text
pipeline/**
tests/**
docs/**
design-reference/**
config/**
.github/**
requirements*.txt
package.json
package-lock.json
AGENTS.md
```

unless required at runtime by a formally approved change.

---

## 49. Project Subpath

Verify static paths work under:

```text
/News-Agregator/
```

style GitHub Pages hosting.

Use relative application-owned resource paths.

Do not assume domain-root deployment.

---

## 50. Pages Permissions

Use least privilege.

Build normally:

```text
contents: read
```

Deployment requires only approved Pages permissions, including:

```text
pages: write
id-token: write
```

No:

```text
contents: write
```

for scheduled feed refresh.

---

## 51. GitHub Action Pinning

Every external:

```yaml
uses:
```

reference must use a full immutable commit SHA.

Include readable version comments.

Do not use floating:

```text
@main
@master
@v4
```

references.

---

## 52. Dependabot

Configure weekly monitoring for:

```text
pip
npm
github-actions
```

No automatic merge requirement.

---

## 53. No Automated Dataset Commits

Workflow must not run:

```text
git add data/articles.json
git commit
git push
```

Current feed JSON belongs to deployment artifact generation.

---

## 54. Concurrency

Pages deployment should use a shared concurrency group such as:

```text
pages
```

with:

```text
cancel-in-progress: true
```

where appropriate.

A failed/canceled new deployment must not delete the last successful site.

---

## 55. Integration Test Sequence

After Pipeline integration:

```text
python -m pytest
python -m pipeline.main --validate-config
```

After State/Ranking integration:

```text
python tests still pass
JS tests pass
```

After Frontend integration:

```text
all automated tests
manual smoke checks
```

After shared composition:

```text
full release-gate suite
```

---

## 56. Full Automated Gate

Before final review run:

```text
python -m pytest
node --test tests/js
python -m pipeline.main --validate-config
pip-audit
```

and, if npm dependencies exist:

```text
npm audit --audit-level=high
```

Use exact repository scripts where provided.

---

## 57. Initial Live Source Validation

Before final V1 release:

```text
python -m pipeline.main --validate-sources
```

Review all 20 Amendment 5 V1 sources and confirm `openai_release_notes` and `okta_workflows` are absent from configuration and runtime processing.

A broken source at initial release must be investigated rather than ignored simply because degraded deployment thresholds would permit it.

Under approved Amendment 4, the initial-release decision may proceed with failures for `n8n_release_notes` and/or `ietf_scim` only when each still matches the exact investigated upstream condition documented in `03-content-sources.md` and `07-pipeline-deployment.md`, normal catastrophic dataset gates pass, and there is no implementation regression. Keep both sources enabled and report the non-zero live-validation result truthfully. Do not extend the amendment to another source or a materially changed failure mode without investigation and an approved amendment.

---

## 58. Live Dataset Generation

Generate a current real dataset.

Record:

```text
enabled source count
successful source count
failed source count
retained Article count
```

Confirm catastrophic thresholds pass.

---

## 59. Production Artifact Local Test

Build the exact static Pages artifact.

Serve that artifact through local HTTP.

Do not test only source-tree files.

Verify:

- index loads;
- CSS loads;
- ES modules load;
- JSON dataset loads;
- paths work from a project subpath;
- primary user flow works.

---

## 60. Core Integrated Scenario

Verify:

```text
Discover
→ Save
→ Read Later count +
→ Undo
→ restored
→ Save again
→ Read Later
→ Open
→ remains saved
→ Mark read
→ History
→ Mark unread
→ back to Read Later
```

Counts and persistence must remain correct.

---

## 61. Immediate Open Scenario

Verify:

```text
unseen Article
→ Open
→ opened signal once
→ reload
→ status remains opened
→ still Discover-eligible
```

Opening must not imply Read.

---

## 62. Remove Scenario

Verify:

```text
saved
→ remove
→ dismissed
```

with:

```text
no negative Dismiss preference
Save signal preserved
```

---

## 63. Import / Export Scenario

Verify:

```text
accumulate state
→ export
→ reset
→ import
→ exact state restored
```

Then attempt invalid import and confirm existing restored state remains unchanged.

---

## 64. Dataset Failure Scenario

Prevent `articles.json` from loading.

Verify:

- Discover shows error;
- Read Later works;
- History works;
- Settings works;
- Export works;
- local state remains.

---

## 65. No-New-Articles Scenario

Make every dataset Article ineligible.

Verify:

```text
Nothing needs your attention right now.
```

or approved equivalent.

No Article resurrection.

---

## 66. Degraded Dataset Scenario

Use a valid dataset with:

```text
failedSourceCount > 0
```

Verify Discover remains usable and degradation is subtle.

---

## 67. Responsive Matrix

Verify integrated application at:

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

No horizontal scrolling.

---

## 68. Accessibility Integration Gate

Verify:

- keyboard-only navigation;
- Left/Right/Z behavior;
- visible focus;
- touch targets;
- live status;
- Settings dialog;
- contrast;
- reduced motion;
- external-link semantics.

---

## 69. Theme Gate

Verify:

```text
Light
Dark
System
```

including:

- persistence;
- System tracking;
- reset to System.

---

## 70. Runtime Network Review

Use browser devtools.

Normal app activity should request only:

```text
same-origin application resources
data/articles.json
```

until the user explicitly opens a publisher Article.

Expected background requests to:

```text
rss2json
OpenAI API
Anthropic API
analytics
Google Fonts
CDNs
```

must be:

```text
NONE
```

---

## 71. Security Search Review

Search repository/frontend for:

```text
innerHTML
rss2json
loremflickr
eval(
new Function
javascript:
unsafe-eval
```

Review every match.

Expected application-runtime prohibited usage:

```text
NONE
```

---

## 72. Production Artifact Inspection

Inspect final artifact contents explicitly.

Confirm no specification/source files leaked.

Do not assume the workflow allowlist is correct merely because deployment succeeded.

---

## 73. Repository Settings Checklist

Integration/final release preparation should note that the repository owner must verify:

```text
Dependabot alerts enabled
Dependabot security updates enabled
Pages source = GitHub Actions
appropriate Actions permissions
```

These settings may not be configurable solely through repository files.

---

## 74. README Update

Update root project `README.md` only as needed to reflect the new V1 application and local development commands.

Do not copy the entire specification set into root README.

It should remain a useful project entry point.

---

## 75. Legacy Cleanup

Only after integrated behavior passes:

- remove obsolete `script.js`;
- remove obsolete `style.css`;
- remove obsolete legacy references;
- remove obsolete random-image/runtime RSS logic.

Run full tests again afterward.

---

## 76. No Spec Drift Cleanup

Do not "simplify" code by changing approved semantics.

Examples prohibited during cleanup:

```text
mark Open as Read automatically
drop opened state
change preference deltas
remove exploration
change source count
relax import validation
```

Cleanup changes structure, not product meaning.

---

## 77. Full Integration Gate

Integration is complete only when:

```text
all three feature workstreams integrated

js/app.js complete

GitHub workflows complete

Dependabot config complete

legacy runtime removed

Python tests pass

JS tests pass

config validation passes

dependency audits pass

live source validation reviewed

real dataset generated

production artifact built

production artifact tested locally

responsive matrix passes

accessibility checks pass

security review passes

no unresolved contract drift
```

---

## 78. Integration Commit Strategy

Integration may contain multiple commits, especially:

```text
merge pipeline
merge state/ranking
merge UI
wire application
add workflows
fix integration
legacy cleanup
```

Keep commits understandable.

The final integration tip SHA becomes:

```text
INTEGRATION_SHA
```

for final review.

---

## 79. Integration Report

Before handing off to Final Review, report:

```text
Workstream:
Integration

Branch:
integration/v1

Foundation SHA:
<FULL SHA>

Pipeline SHA:
<FULL SHA>

State/Ranking SHA:
<FULL SHA>

Frontend UI SHA:
<FULL SHA>

Integration SHA:
<FULL SHA>

Automated tests:
<commands/results>

Dependency audits:
<results>

Live source validation:
<20-source summary>

Generated dataset:
<source success / Article count>

Production artifact:
PASS / FAIL

Responsive matrix:
PASS / FAIL

Accessibility:
PASS / FAIL

Security review:
PASS / FAIL

Legacy script.js removed:
YES / NO

Legacy style.css removed:
YES / NO

Known limitations:
NONE / details

Specification amendments during integration:
NONE / details
```

---

## 80. Stop Condition

After producing a clean integration report and recording:

```text
INTEGRATION_SHA
```

stop feature/integration development.

Do not merge to:

```text
main
```

yet.

The branch now moves to:

```text
Final Review
```

for an independent adversarial/specification audit.

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
- `docs/v1/workstreams/content-pipeline.md`
- `docs/v1/workstreams/state-ranking.md`
- `docs/v1/workstreams/frontend-ui.md`

This workstream integrates independently tested subsystems into the complete production application. It must preserve their frozen semantics rather than redesign them.
