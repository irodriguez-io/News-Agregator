# Intentional Reading — V1 Final Review Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/final-review.md`\
**Workstream type:** Independent review / release gate\
**Primary input:** `integration/v1` at `INTEGRATION_SHA`\
**Primary output:** APPROVE / REJECT with actionable findings\
**Feature implementation:** Prohibited except explicitly authorized review fixes

---

## 1. Mission

Perform an independent final audit of the integrated Intentional Reading V1 implementation before merge to `main`.

The purpose of this workstream is not to improve or redesign the application.

It is to answer:

> Does the integrated implementation actually satisfy the approved V1 specifications, contracts, security requirements, tests, and release criteria?

The reviewer should assume that subtle regressions or contract drift may exist even when all automated tests pass.

---

## 2. Starting Point

Review exactly:

```text
integration/v1
at the supervisor-provided:
INTEGRATION_SHA
```

Do not review an uncommitted working tree as the release candidate.
The candidate must have a clean Git status before review begins.
3. Required Inputs
Before final review begins, obtain:
FOUNDATION_SHA
PIPELINE_SHA
STATE_RANKING_SHA
FRONTEND_UI_SHA
INTEGRATION_SHA
and the Integration completion report.
Record these in the final review report.
4. Required Reading
Before evaluating code, read:
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
docs/v1/workstreams/final-review.md
design-reference/DESIGN.md
Also inspect:
design-reference/intentional-reading-prototype.png
for visual intent.
Do not begin with implementation assumptions. Begin from the specifications.
5. Review Philosophy
Final Review is:
specification-driven
adversarial
evidence-based
Do not accept:
"the code looks fine"
"tests passed"
"it works on my machine"
as sufficient evidence by themselves.
Look specifically for:
silent contract drift;
missing edge cases;
duplicated business logic;
UI/state inconsistencies;
unsafe content handling;
production artifact leakage;
dependency/supply-chain regressions;
accessibility gaps;
behavior that technically works but violates product intent.
6. Review Scope
Review the complete integrated repository, including:
index.html
css/**
js/**
pipeline/**
config/**
tests/**
.github/**
requirements*.txt
package*.json
README.md
.gitignore
and the generated production artifact/workflow behavior where available.
7. Feature Coding Is Not the Goal
The reviewer must not use Final Review as an opportunity to refactor the project.
Do not:
redesign modules;
rename shared concepts;
change algorithms for preference;
add new functionality;
optimize prematurely;
modernize dependencies without need.
Findings should be reported back to the appropriate owner/integration path.
8. Permitted Review Fixes
A reviewer may make a very small direct fix only when all of the following are true:
the defect is unambiguous;
it does not alter a frozen contract;
it is low-risk;
it is clearly review/integration-owned;
rerunning all affected gates is practical.
Examples:
typo in user-facing copy
missing rel=noopener
incorrect artifact exclusion
broken README command
For substantive defects, report them instead of silently repairing them.
9. Specification Authority Review
Confirm implementation follows:
01-product.md
for product semantics.
Explicitly verify:
product is intentional reading, not engagement maximization;
navigation is Read Later / Discover / History;
Discover is triage, not infinite feed;
opening is not equivalent to reading;
no article images;
no login/backend/cloud sync;
no AI API dependency;
no analytics/gamification.
10. Architecture Review
Confirm:
static frontend
+
build-time Python ingestion
+
browser-local personalization
is preserved.
Reject unexpected:
backend server;
database;
runtime Python service;
frontend publisher fetching;
server-side personalization;
framework runtime dependency.
11. Contract Review — Dataset
Inspect generated dataset/sample fixtures and code.
Verify exactly:
schemaVersion = 1
and Article shape matches contracts.md.
Check:
id;
title;
url;
source;
category;
publishedAt;
author;
excerpt;
readingTimeMinutes;
tags;
contentType;
score.
No undocumented shared fields should be required for normal operation.
12. Contract Review — Article Identity
Verify:
canonical HTTP/HTTPS URL
→ SHA-256
→ first 20 lowercase hexadecimal chars
Contract Amendment 1 must be implemented.
Reject any fallback identity based on:
GUID;
title;
publication date.
Entries without usable URLs must be rejected.
13. Contract Review — Category IDs
Verify internal IDs remain exactly:
science
technology
literature
history
weightlifting
iam
identity_automation
and synthetic filter:
all
No alternate casing/hyphenation should silently appear.
14. Contract Review — Local State
Verify:
intentionalReading:v1
schemaVersion = 1
and exact logical root:
preferences
articles
settings
session
No fragmented persistent application keys should exist.
15. Contract Review — Statuses
Verify only:
opened
saved
dismissed
read
are persisted.
Do not accept persisted:
unseen
removed
archived
or alternate status names.
16. Contract Review — Preference Deltas
Verify exact values in production code:
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
No UI duplication of these values.
17. Contract Review — Ranking Formula
Verify exact production formula:
personalizedScore =
base
+ sourcePreference
+ topicPreference
+ exploration
Bounds:
sourcePreference [-5,+5]
topicPreference  [-6,+6]
exploration      [0,+3]
No hidden category preference.
No random noise.
Verify the deterministic candidate pre-sort before diversity sequencing is exactly:
personalized total descending;
base score descending;
publication date descending, unknown last;
source ID ascending;
Article ID ascending.
18. Contract Review — Diversity
Verify:
same previous source
→ -8 temporary penalty

third same category in All
→ -5 temporary penalty
These must remain temporary deck-selection effects only.
No persistent mutation.
19. Product-State Transition Review
Explicitly test:
UNSEEN → OPENED
UNSEEN → SAVED
UNSEEN → DISMISSED
OPENED → SAVED
OPENED → READ
SAVED → READ
READ → SAVED
SAVED → DISMISSED
Verify every transition preserves/reverses the correct signals.
20. Opening Is Not Read
This is a critical product contract.
Verify:
open Article
does not:
remove from Read Later;
add to History;
apply Read signal;
set readAt.
Reject implementation that auto-completes an article on click.
Verify Open attempts local persistence first. On success, the Open interaction is retained and publisher navigation proceeds. On persistence failure, an accessible local-state warning is shown, no persisted success is claimed, and publisher navigation still proceeds.
Verify persistent queue/state actions such as Save, Dismiss, Mark Read, Mark Unread, Remove, Import, and Reset are not presented as successful when persistence fails.
21. Mark Unread Review
Verify:
READ → SAVED
does all of:
reverses Read signal;
clears readAt;
sets new savedAt;
preserves Open;
preserves prior Save signal;
does not apply a new Save signal.
22. Remove Review
Verify:
SAVED → DISMISSED
from Remove:
applies no negative Dismiss preference;
does not reverse Save;
preserves Open;
removes from queue.
This is a likely drift risk and should be tested explicitly.
23. Undo Review
Verify Undo:
only supports most recent Save/Dismiss swipe;
is in-memory only;
reverses corresponding learning signal;
restores previous record exactly;
preserves earlier Open signal;
disappears after reload.
24. Source Catalog Review
Verify exactly 22 V1 sources exist in config/sources.json.
Compare IDs, categories, adapters, URLs, quality scores, content types, admission rules, and forced tags to 03-content-sources.md.
No unauthorized publisher additions.
25. OpenAI Filter Review
Verify OpenAI Release Notes require at least one organic match from:
ai_ml
software_architecture
cybersecurity
devops_sre
Forced tags must not satisfy admission.
26. Anthropic Filter Review
Verify Anthropic Engineering remains unfiltered beyond normal source validity.
Do not accidentally apply the OpenAI admission model to Anthropic.
27. Broad-Source Filter Review
Verify:
barbell_medicine
entra_releases
use the approved organic topic admission rules.
28. Forced Tags Review
Verify exactly:
ietf_oauth → oauth
w3c_webauthn → passkeys_webauthn
ietf_scim → scim
and forced tags do not increase organic topicSignal.
29. Taxonomy Review
Spot-check topic aliases and category scope.
Specifically test false-positive-sensitive tokens such as:
ai
ml
programming
Whole-token/phrase matching must be evident.
No raw substring classification.
30. Deduplication Review
Verify:
exact URL dedupe;
same-source similarity >=0.92 with both valid dates within 14 days;
same-source similarity >=0.97 when either date is unavailable;
cross-source exact normalized title with both valid timestamps within 72 hours;
no cross-source title-only deduplication when either date is unavailable;
winner order: source quality, metadata score, newer valid publication date, richer non-empty excerpt, deterministic source/article-ID tie-break.
Content-type score must not participate in duplicate-winner selection.
Reject semantic/event-level clustering that is not specified.
31. Base Score Review
Verify exact component formula:
sourceQuality
contentType
freshness
topicSignal
metadata
No hidden multipliers.
All generated components integers.
Verify metadata scoring exactly:
valid non-null publication date +2;
excerpt >=80 characters +2;
excerpt 1–79 characters +1;
excerpt 0 characters +0;
non-empty author +1;
readingTimeMinutes +0;
maximum 5.
32. Freshness Review
Spot-check exact thresholds:
<=1 day 15
<=3     13
<=7     10
<=14     7
<=30     4
>30      1
unknown  5
33. Retention Review
Verify order:
45-day known-date cutoff
→ 40/source
→ 500 total
Do not accept reordering that changes semantics without documented reason.
34. Dataset Health Gates
Verify deployment blocks when:
articleCount < 20
or:
successful sources < 50% enabled
For 22 enabled:
minimum = 11
35. Frontend Trust Boundary
Inspect render paths for Article-derived:
title
source.name
author
excerpt
tag.label
contentType.label
Confirm they are rendered as text.
Reject unsafe flow through:
innerHTML
insertAdjacentHTML
document.write
36. Unsafe Code Execution Review
Search for:
eval(
new Function
setTimeout("...
setInterval("...
javascript:
Expected prohibited runtime usage:
NONE
37. External URL Review
Verify article navigation accepts only:
http
https
including imported snapshots.
Confirm:
new-tab/window behavior;
noopener;
noreferrer;
no URL construction from titles/source IDs.
38. SSRF Review
Inspect autodiscovery/redirect network target validation.
Verify non-public destinations are rejected.
At minimum test representative:
localhost
127.0.0.1
::1
RFC1918
link-local
unspecified
No actual internal requests should be performed.
39. Import Security Review
Verify invalid imports are rejected atomically.
Test:
malformed JSON;
unsupported schema;
5 MiB;


invalid Article ID;
key/ID mismatch;
unsafe protocol;
bad enum;
preference out of bounds;
negative interactions;
dangerous keys.
Existing state must remain unchanged.
40. Dependency Review
Confirm production frontend has:
0 npm runtime dependencies
If npm development dependencies exist:
exact direct versions;
lockfile;
audit passes.
Review Python manifests for exact direct pins.
41. Node/Python Baseline Review
Verify CI/tooling baseline matches approved:
Node 24 LTS
Python 3.13
unless a formally approved compatibility amendment exists.
42. Dependency Audit Review
Verify latest candidate gate results include:
pip-audit
and, when applicable:
npm audit --audit-level=high
Review any exceptions explicitly.
No blanket ignored audit failure.
43. GitHub Actions Pin Review
Inspect every external:
uses:
reference.
It must be pinned to a full immutable commit SHA.
No:
@main
@master
@v4
floating refs.
44. Workflow Permission Review
Confirm least privilege.
Reject unnecessary:
contents: write
for content refresh.
Pages deployment may use only approved:
pages: write
id-token: write
plus required read permissions.
45. Workflow Trigger Review
Confirm deploy workflow supports:
push to main
workflow_dispatch
17 */6 * * *
and scheduled refresh does not execute unnecessary frontend test work.
46. No Automated Dataset Commits
Search workflows for:
git add
git commit
git push
related to current feed generation.
Expected scheduled dataset-commit behavior:
NONE
47. Dependabot Review
Confirm .github/dependabot.yml covers:
pip
npm
github-actions
with approved review-based update approach.
48. Production Artifact Review
Build/inspect exact Pages artifact.
Allowed:
index.html
css/**
js/**
data/articles.json
approved runtime assets
Reject leakage of:
docs/**
design-reference/**
tests/**
pipeline/**
config/**
.github/**
requirements*.txt
package*.json
AGENTS.md
49. Project-Subpath Review
Serve production artifact in a way that simulates a project path such as:
/News-Agregator/
Verify no domain-root assumptions.
50. Runtime Network Review
Use browser developer tools.
Normal background runtime requests should be limited to same-origin application assets/data.
Reject background calls to:
rss2json
OpenAI
Anthropic
analytics
Google Fonts
CDNs
publishers
Publisher network occurs only after explicit article navigation.
51. Legacy Removal Review
Confirm obsolete:
script.js
style.css
are removed only if fully replaced.
Also search for legacy:
rss2json
LoremFlickr
image fallback
behavior.
Expected V1 runtime:
NONE
52. Discover UX Review
Verify:
one primary Article at a time;
text-first hierarchy;
no images;
visible source/title/excerpt/tags/content type;
explicit Dismiss/Read/Save controls;
swipe equivalents;
finite queue feel;
no infinite scroll.
53. Swipe Review
Verify exactly:
left → Not interested
right → Save for later
threshold = 90px
and below-threshold release produces no state change.
54. Keyboard Review
Verify:
Left Arrow  → Dismiss
Right Arrow → Save
Z           → Undo
and shortcuts do not interfere with dialogs/form controls.
55. Read Later Review
Verify:
chronological savedAt desc;
compact editorial rows;
local snapshots;
Read;
Mark read;
Remove;
truthful count;
truthful aggregate reading time.
No swipe deck.
56. History Review
Verify:
readAt desc;
Today / Yesterday / Earlier presentation;
Reopen;
Mark unread;
local snapshots.
No personalization reordering.
57. Settings Review
Verify:
Light
Dark
System
Export
Import
Reset
Settings remains secondary/modal.
No account/cloud settings.
Verify each rendered application view has one application masthead/header, with no duplicated `Intentional Reading` masthead or duplicated Settings control. Treat any apparent repetition in the prototype PNG as an illustrative stitching/reference artifact, not a product requirement.
58. Theme Review
Verify all themes and persistence.
System mode must follow OS/browser preference where practical.
Reset returns to System.
59. Responsive Review
Manually inspect all required widths:
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
Record each result.
No horizontal page scrolling.
60. Accessibility Review
Perform keyboard-only pass.
Verify:
visible focus;
semantic controls;
no gesture-only behavior;
touch target sizes;
live announcements;
dialog behavior;
reduced motion;
contrast;
external-link indication.
61. Reduced Motion Review
Enable:
prefers-reduced-motion: reduce
Verify swipe/motion effects become minimal/immediate without loss of meaning.
62. Design Fidelity Review
Compare integrated application to:
DESIGN.md
intentional-reading-prototype.png
Look for unauthorized drift in:
palette;
typography;
spacing;
navigation;
card composition;
editorial character.
Do not fail harmless differences caused by real content/system fonts.
Do report material redesign.
63. Debug Mode Review
Verify:
?debug=1
can expose actual:
base;
source preference;
topic preference;
exploration;
final score;
tag count.
Normal mode must hide ranking numbers.
Debug mode must not alter ordering/state.
64. Degraded Dataset Review
Use dataset metadata with failed sources.
Verify Discover remains usable and messaging is subtle.
65. Dataset Failure Review
Prevent current dataset load.
Verify:
Discover error;
Read Later available;
History available;
Settings/export available;
local data retained.
66. No-New-Articles Review
Make all Articles ineligible.
Verify product treats this as a valid successful state.
No automatic resurrection.
No pressure/engagement loop.
67. Product Acceptance Scenario Review
Execute the full scenarios from 09-testing-acceptance.md, especially:
Micro-Triage
Read Lifecycle
Immediate Reading
Queue Management
Backup and Restore
Offline Personal Data
Personalization
Intentional Exit
Record pass/fail for each.
68. Automated Tests
Run complete automated suite from a clean candidate checkout/environment.
At minimum:
python -m pytest
node --test tests/js
python -m pipeline.main --validate-config
and dependency audits.
Do not rely solely on Integration's reported results.
69. Live Source Validation
Run/review:
python -m pipeline.main --validate-sources
for initial release.
Record all 22 sources.
Any failure must be explicitly resolved or accepted via a source-spec amendment before first V1 release.
70. Real Dataset Generation
Generate the release-candidate dataset and record:
enabled sources
successful sources
failed sources
retained Articles
Verify dataset health gates.
71. Final Artifact Functional Test
Build the exact production artifact.
Serve it over local HTTP.
Perform a concise end-to-end flow against that artifact.
Do not substitute source-tree testing.
72. Repository Settings Verification
Confirm or request owner confirmation for:
Dependabot alerts
Dependabot security updates
GitHub Pages source = Actions
appropriate Actions permissions
If not directly inspectable, mark as:
REQUIRES OWNER VERIFICATION
rather than assuming.
73. Finding Severity
Classify findings as:
BLOCKER
Violates a frozen contract, security requirement, or core product behavior.
Examples:
Open marks Read;
unsafe remote innerHTML;
wrong Article schema;
broken persistence;
runtime API secret.
HIGH
Release-quality defect materially affecting required V1 behavior.
Examples:
broken import;
one required responsive range unusable;
Actions deployment fundamentally incorrect.
MEDIUM
Required behavior is incomplete but localized.
LOW
Minor polish/documentation issue not materially affecting V1 semantics.
74. Approval Rule
Final Review may approve merge to main only when:
BLOCKER findings = 0
HIGH findings = 0
and all required release gates pass.
MEDIUM findings must either:
be resolved; or
be explicitly accepted as a documented known limitation only if they do not contradict V1 Definition of Done.
Core required behavior cannot be waived merely by relabeling it a limitation.
75. Rejection Rule
If final review fails:
DO NOT MERGE TO MAIN
Report each finding with:
severity
spec reference
observed behavior
expected behavior
likely owning workstream
recommended corrective action
Return defects to the appropriate feature/integration path.
76. Retest Rule
After fixes:
rerun affected tests;
rerun full release gates for any material change;
review the new integration tip SHA.
Do not approve the original stale SHA after fixes.
Record:
NEW INTEGRATION_SHA
when applicable.
77. Final Review Report
Produce:
Workstream:
Final Review

Foundation SHA:
<FULL SHA>

Pipeline SHA:
<FULL SHA>

State/Ranking SHA:
<FULL SHA>

Frontend UI SHA:
<FULL SHA>

Integration SHA reviewed:
<FULL SHA>

Automated tests:
PASS / FAIL
<details>

Dependency/security audits:
PASS / FAIL
<details>

Live source validation:
PASS / FAIL
<22-source summary>

Generated dataset:
<successful sources / retained Articles>

Production artifact:
PASS / FAIL

Project subpath:
PASS / FAIL

Responsive matrix:
PASS / FAIL

Accessibility:
PASS / FAIL

Security review:
PASS / FAIL

Product acceptance scenarios:
PASS / FAIL
<scenario summary>

Repository settings:
VERIFIED / OWNER VERIFICATION REQUIRED

Findings:
<BLOCKER/HIGH/MEDIUM/LOW table or NONE>

Known limitations:
NONE / details

Final decision:
APPROVE FOR MAIN
or
REJECT
78. APPROVE FOR MAIN Meaning
APPROVE FOR MAIN means the reviewer has sufficient evidence that:
the integrated code matches the frozen V1 specs;
security gates pass;
required product flows work;
deployment artifact is valid;
no blocking contract drift remains.
It does not mean the application can never contain bugs.
It means the agreed V1 release standard has been met.
79. Main Merge
Final Review itself does not need to perform the merge unless the supervisor explicitly owns that action.
After:
Final decision: APPROVE FOR MAIN
the supervisor may merge:
integration/v1
→ main
using the repository's chosen normal merge strategy.
80. Post-Merge Verification
After main merge/deployment, perform a final production smoke check:
site loads
Discover loads
Read Later navigation works
History navigation works
Settings works
current dataset visible
If a production-only defect appears:
treat as release defect
→ fix/revert through normal Git flow
Do not manually patch deployed Pages files.
81. Review Non-Goals
Final Review does not introduce:
V2 features;
reference shelf;
search;
notifications;
sync;
AI summaries;
native app;
design redesign;
new source catalog expansion.
Any attractive enhancement discovered during review belongs in future planning.
82. Stop Condition
After either:
APPROVE FOR MAIN
or:
REJECT
and the complete report is produced:
STOP
Do not begin unrelated feature work.
Related Authoritative Documents
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
This workstream independently determines whether the integrated V1 release candidate satisfies the approved specification and is fit to merge to main.
