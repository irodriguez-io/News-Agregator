# Intentional Reading — V1 Content Pipeline Workstream

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/workstreams/content-pipeline.md`\
**Workstream type:** Feature implementation\
**Primary branch:** `feat/content-pipeline`\
**Primary ownership:** `pipeline/**`, `config/**`, `tests/pipeline/**`

---

## 1. Mission

Implement the complete build-time content ingestion pipeline for Intentional Reading V1.

This workstream converts the approved source catalog into a deterministic, validated `ArticleDataset v1` compatible with the frozen cross-agent contracts.

The workstream owns:

- source configuration;
- topic configuration;
- source adapters;
- normalization;
- canonical URLs;
- Article IDs;
- taxonomy matching;
- source admission filtering;
- forced tags;
- deduplication;
- base scoring;
- retention;
- dataset validation;
- pipeline tests.

The workstream does **not** own:

- browser state;
- personalization;
- UI;
- navigation;
- localStorage;
- `js/app.js`;
- production integration.

---

## 2. Starting Point

Create this branch/worktree from exactly:

```text
FOUNDATION_SHA
```

Do not branch from:
main
v1-foundation bootstrap commit
another feature branch
The supervisor will provide the exact frozen SHA.
3. Required Reading
Before implementation, read:
AGENTS.md
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/03-content-sources.md
docs/v1/04-taxonomy-scoring.md
docs/v1/07-pipeline-deployment.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
docs/v1/workstreams/content-pipeline.md
Also inspect:
docs/v1/05-personalization-state.md
only as needed to understand what must remain browser-owned.
Do not reinterpret shared contracts from memory.
4. Owned Paths
This workstream may create or modify:
pipeline/**
config/**
tests/pipeline/**
requirements.txt
requirements-dev.txt
It may also make the minimum necessary change to:
.gitignore
for generated pipeline output if required by the approved deployment specification.
data/articles.json is permitted as generated operational output of the Content Pipeline workstream. It is not a committed owned source path; it must remain ignored/untracked during feature work. The agent may generate, inspect, overwrite, and delete this local output while testing.
This does not grant ownership of arbitrary `data/**` source files.
Any change outside these paths requires supervisor approval.
5. Forbidden Paths
Do not modify:
index.html
css/**
style.css
script.js
js/**
tests/js/**
design-reference/**
docs/v1/**
.github/workflows/**
package.json
package-lock.json
Do not implement deployment workflows in this branch.
Deployment integration belongs to the integration/supervisor phase unless explicitly reassigned.
6. Shared Contract Constraint
The pipeline must emit exactly the frozen:
ArticleDataset v1
Article v1
contracts from:
docs/v1/contracts.md
Do not independently alter:
schemaVersion;
Article property names;
category IDs;
source object structure;
tag object structure;
content-type structure;
score component names;
Article ID format.
If implementation reveals a contract problem:
STOP
→ document issue
→ report supervisor
Do not silently revise the schema.
7. Target Pipeline Layout
Implement approximately:
pipeline/
├── __init__.py
├── main.py
├── fetch.py
├── normalize.py
├── deduplicate.py
├── taxonomy.py
├── scoring.py
├── output.py
└── adapters/
    ├── __init__.py
    ├── rss.py
    ├── atom.py
    ├── rss_autodiscovery.py
    └── html_listing.py
Minor additional modules are allowed where they improve separation.
Examples:
validation.py
network.py
models.py
retention.py
Do not collapse the entire pipeline into main.py.
8. Main Orchestrator Responsibility
pipeline/main.py should coordinate stages rather than implement them all.
Conceptually:
load configuration
validate configuration
capture generatedAt

for each enabled source:
    fetch
    adapter extraction
    plain-text normalization
    URL canonicalization and required-field validation
    stable Article ID and date normalization
    organic taxonomy matching
    source-specific admission filtering
    forced tag application
    compute metadata confidence needed for duplicate winner selection

deduplicate
full base scoring
retain
validate dataset
write output
report summary

Metadata confidence is calculated before deduplication because duplicate-winner selection requires it. This pre-dedup metadata calculation is not full base scoring. The complete Article base score is calculated after deduplication.
The exact function names are not contractual.
The boundaries are.
9. Configuration Files
Create:
config/sources.json
config/topics.json
using the exact approved source catalog and taxonomy from:
03-content-sources.md
04-taxonomy-scoring.md
contracts.md
Do not invent new source IDs, topics, aliases, or score values.
10. Source Catalog Requirement
config/sources.json must contain exactly:
20 enabled V1 sources
unless the authoritative source specification is formally amended.

Amendment 5 is that approved amendment: `openai_release_notes` and `okta_workflows` are deferred to V2 and must be absent from V1 configuration and runtime processing.
The source definitions must reproduce exactly:
source ID;
display name;
category;
adapter;
URL;
quality;
content type;
enabled state;
minimum topic matches;
admission topic IDs;
forced tags.
11. Source Adapters
Implement the approved adapter types:
rss
atom
rss_autodiscovery
html_listing
Adapters produce raw article records for normalization.
They must not:
calculate personalized ranking;
know about browser state;
render frontend HTML;
modify source configuration at runtime.
12. RSS / Atom
Feed adapters should use the approved feed parser dependency.
They must extract, where available:
title
url
author
published date
summary/description/content metadata
Do not scrape publisher article bodies after parsing feed entries.
13. RSS Autodiscovery
The autodiscovery adapter must:
fetch the configured canonical page;
locate publisher-declared RSS/Atom alternates;
resolve relative URLs safely;
reject unrelated or invalid candidates;
validate derived targets against the security rules;
fetch and parse the selected feed.
Do not use public-web search or third-party RSS-generation services.
14. HTML Listing Sources
Implement narrow source-specific parsing for exactly the approved V1 HTML-listing sources:
anthropic_engineering
barbell_medicine
Do not build a generalized arbitrary-site scraper.
The implementation may dispatch on source ID inside the HTML adapter or use source-specific helper functions/modules.
15. Network Safety
Implement the HTTP behavior from:
07-pipeline-deployment.md
08-security-dependencies.md
Required:
connect timeout: 10 seconds
read timeout: 20 seconds
one retry for approved transient failures
~2 second retry delay
maximum 5 redirects
maximum 10 MiB per feed/listing response
descriptive User-Agent
No request may wait indefinitely.
16. Derived URL / SSRF Protection
Before requesting URLs derived from:
autodiscovery
redirects to materially different hosts
reject:
localhost;
loopback;
RFC1918/private;
link-local;
unspecified;
reserved/non-public network destinations.
Use standard-library mechanisms where practical.
Only:
http
https
are permitted.
17. Source Failure Isolation
Normal source failures must be isolated.
Examples:
timeout
HTTP failure
malformed feed
parser failure
0 raw parsed entries
These should mark the source failed and allow remaining sources to continue.
Fatal pipeline/configuration errors remain fatal.
18. Source Success Semantics
A source is successful when:
fetch succeeds
adapter parses
>=1 raw entry exists before admission filtering
A source may be successful with:
0 accepted Articles
when admission filtering intentionally rejects all parsed entries.
Do not treat filtered-to-zero as source failure.
19. Normalization
Implement exactly the rules from 04-taxonomy-scoring.md.
Required areas include:
NFKC normalization;
HTML-to-plain-text cleanup;
whitespace normalization;
title bounds;
author bounds;
excerpt bounds;
publication-date normalization;
reading-time rules.
Remote publisher markup must not survive as trusted frontend markup.
20. Article URL Requirement
Every emitted Article must have a usable canonical:
http://
https://
URL.
Per Contract Amendment 1:
no usable URL
→ reject source entry
Do not create fallback Article IDs from:
GUID;
title;
publication date.
21. URL Canonicalization
Implement approved behavior for:
lowercase scheme/host;
default-port removal;
fragment removal;
tracking-parameter removal;
meaningful query preservation;
deterministic query ordering;
conservative trailing-slash normalization.
Do not aggressively rewrite publisher URLs.
22. Article IDs
Article ID:
SHA-256(canonical URL UTF-8)
→ first 20 lowercase hexadecimal characters
Tests must assert known expected values.
23. Publication Dates
Normalize valid source timestamps to UTC ISO-8601.
Missing/invalid dates remain:
null
Do not substitute fetch time.
Future dates:
<= generatedAt + 6 hours
→ age treated as 0 for freshness

> generatedAt + 6 hours
→ normalized publication date treated as unknown
→ log warning
24. Reading Time
Only calculate when at least:
400 words
of legitimate source-provided content are available.
Formula:
ceil(wordCount / 225)
minimum emitted value:
2
Otherwise:
null
Do not fetch article bodies merely to calculate reading time.
25. Taxonomy Matching
Implement deterministic category-scoped matching using:
title weight = 3
excerpt weight = 1
Evidence applies once per topic per field.
Use whole-token / whole-phrase matching.
Do not use raw substring matching.
26. Topic Scope
Only evaluate topics whose:
categories
contain the Article category.
This must prevent cross-category false positives such as:
Technology "programming"
≠ Weightlifting training-programming tag
27. Topic Output
Persist:
up to 5 organic tags
ordered according to the approved evidence rules.
Forced tags may extend the internal tag array beyond five.
Every tag must use:
{
  "id": "...",
  "label": "..."
}
from config/topics.json.
28. Topic Signal
Calculate:
sum organic evidence across all organically detected eligible topics
before five-tag output truncation.
Clamp:
0–10
Forced tags contribute:
0
to topicSignal.
29. Admission Filtering
Implement source-specific admission exactly for:
barbell_medicine
entra_releases
Admission counts distinct organically detected topic IDs.
Forced tags never satisfy admission.
Do not filter Anthropic Engineering.
30. Forced Tags
Apply exactly:
ietf_oauth
→ oauth

w3c_webauthn
→ passkeys_webauthn

ietf_scim
→ scim
If already organic:
do not duplicate
Forced tags are valid final Article tags but not organic scoring evidence.
31. Deduplication
Implement:
Exact canonical URL
Always collapse.
Same-source near duplicate
When both valid publication dates exist:
normalized title similarity >= 0.92
AND publication dates within 14 days
→ duplicate
If either publication date is unavailable:
normalized title similarity >= 0.97
→ duplicate
Use Python standard-library difflib.SequenceMatcher.
Cross-source
Only deduplicate when normalized titles are exactly equal, both valid publication timestamps exist, and timestamps are within 72 hours.
If either publication date is unavailable, do not perform cross-source title-only deduplication.
Be conservative.
Do not implement semantic event clustering.
32. Duplicate Winner
Use approved priority:
source quality
metadata score
newer valid publication date
richer non-empty excerpt
deterministic source/article-ID tie-break
Do not use content-type score as a duplicate-winner criterion.
Never merge different publishers into a synthetic Article.
33. Base Scoring
Implement exact components:
sourceQuality  0–50
contentType    0–20
freshness      0–15
topicSignal    0–10
metadata       0–5
Invariant:
base =
sourceQuality
+ contentType
+ freshness
+ topicSignal
+ metadata
All generated components are integers.
34. Freshness
Implement exact approved boundaries:
<=1 day      15
<=3 days     13
<=7 days     10
<=14 days     7
<=30 days     4
>30 days      1
unknown       5
Use the run-wide captured generatedAt.
35. Metadata Score
Implement exactly:
valid non-null publication date
+2

excerpt >=80 chars
+2

excerpt 1–79 chars
+1

excerpt 0 chars
+0

non-empty author
+1

readingTimeMinutes
+0
Maximum:
5
36. Retention
Apply exactly in this order:
deduplicated/scored candidates
→ remove known-date articles >45 days
→ max 40/source
→ max 500 total
→ deterministic final order
Unknown-date Articles are not removed solely by age.
37. Catastrophic Dataset Gates
A normal generation run is deployable only if:
retained articleCount >= 20
and:
successfulSourceCount >= ceil(enabledSourceCount * 0.50)
For 20 enabled sources:
>=10 successful
A failing gate must result in non-zero process exit.
38. Dataset Contract
Generate:
data/articles.json
compatible with:
schemaVersion = 1
Top-level:
schemaVersion
generatedAt
pipeline
articles
Pipeline metadata:
enabledSourceCount
successfulSourceCount
failedSourceCount
articleCount
39. Dataset Validation
Before successful output, validate:
schema version;
metadata counts;
Article contract;
unique IDs;
valid categories;
known source IDs;
known topic IDs;
known content types;
HTTP/HTTPS URLs;
score invariant;
timestamps/nulls.
Never emit a structurally invalid "successful" dataset.
40. Atomic Output
Write output using:
temporary file
→ successful serialization
→ close/flush
→ atomic replace
Do not leave truncated data/articles.json on failure.
41. Generated Output Tracking
Per the approved deployment architecture, scheduled current-feed output is not committed repeatedly.
Add the minimum .gitignore handling needed so normal generated:
data/articles.json
does not become a routine repository modification.
Do not implement Git commit/push behavior.
42. Pipeline Commands
Support:
python -m pipeline.main
Normal generation.
Support:
python -m pipeline.main --validate-config
No network access.
Support:
python -m pipeline.main --validate-sources
Live validation of all enabled sources.
Optional:
--output <path>
for test/development use.
43. Exit-Code Semantics
Normal generation:
0
→ valid deployable dataset

non-zero
→ fatal/config/sanity failure
--validate-config:
0 valid
non-zero invalid
--validate-sources:
0 all enabled sources validate
non-zero one or more fail
44. Logging
Provide concise source-level operational output.
Report conceptually:
raw
normalized
accepted
rejected
failure reason
and final:
enabled sources
successful
failed
raw entries
accepted
duplicates
expired
retained
Do not dump full remote feeds/HTML.
45. Python Dependencies
Start with the approved minimal runtime set:
feedparser
requests
beautifulsoup4
python-dateutil
Use standard library for:
hashlib
difflib
ipaddress
json
where practical.
Do not add another runtime package merely for convenience.
46. Dependency Files
Create exact-version-pinned manifests consistent with:
08-security-dependencies.md
Expected:
requirements.txt
requirements-dev.txt
Development dependencies may include:
pytest
pip-audit
Do not use unbounded direct versions.
47. Unit Test Requirements
Implement the required pipeline tests from:
09-testing-acceptance.md
At minimum cover:
configuration validation;
URL canonicalization;
stable ID;
text normalization;
dates;
reading time;
whole-token taxonomy;
category scope;
topic evidence;
topic score;
forced tags;
admission filters;
deduplication;
metadata score;
freshness;
base score;
retention;
source failures;
catastrophic gates;
atomic output;
dataset contract;
SSRF target validation.
48. Adapter Fixtures
Create representative local fixtures.
At minimum include useful coverage for:
Quanta RSS
IETF OAuth Atom
IETF SCIM Atom
Anthropic Engineering HTML
Barbell Medicine HTML
Additional feed fixtures are encouraged when parsing behavior materially differs.
Do not make unit tests depend on live internet.
49. Test Commands
Before completion, run:
python -m pytest
and:
python -m pipeline.main --validate-config
Both must pass.
50. Live Validation
Also run when network access is available:
python -m pipeline.main --validate-sources
Report the result exactly.
A temporary publisher failure does not justify modifying the frozen source catalog without review.
If an endpoint/specification appears genuinely stale:
STOP
→ report exact source
→ report observed failure
→ propose correction
Do not silently substitute an unofficial source.
51. Security Audit
Run the approved Python dependency audit before reporting completion.
Conceptually:
pip-audit
Known vulnerabilities must be reported and resolved according to:
08-security-dependencies.md
Do not auto-run destructive dependency upgrade commands.
52. No Frontend Implementation
Do not create:
js/data/articles.js
even though it consumes the pipeline output.
That belongs to State/Ranking.
Do not create mock UI merely to demonstrate the dataset.
Pipeline correctness is demonstrated by tests and generated output.
53. No Workflow Implementation
Do not implement:
.github/workflows/test.yml
.github/workflows/deploy.yml
in this branch unless the supervisor explicitly reassigns them.
This avoids shared-path conflicts during parallel work.
The integration phase will connect tested pipeline commands to Actions.
54. Scope Discipline
Do not add:
full article scraping;
image extraction;
AI summarization;
embedding generation;
search indexing;
database storage;
category inference;
generalized crawler framework;
arbitrary source discovery;
machine-learning topic classification.
Implement only approved V1 behavior.
55. Contract Conflict Procedure
If implementation cannot satisfy a frozen contract without contradiction:
stop affected implementation;
identify exact spec sections;
explain why they conflict;
identify affected interfaces/tests;
report to supervisor.
Do not edit docs/v1/** from the feature branch.
56. Completion Gate
The Content Pipeline workstream is complete only when:
config/sources.json exists and matches the Amendment 5 approved 20-source catalog

openai_release_notes and okta_workflows are absent from V1 configuration, adapters, constants, fixtures, validation, and generation

config/topics.json exists and matches approved taxonomy

pipeline implementation exists

deterministic unit tests pass

configuration validation passes

dependency audit passes

live validation result is reported

no forbidden paths changed

changes are committed
57. Completion Report
Report exactly:
Workstream:
Content Pipeline

Branch:
feat/content-pipeline

Commit SHA:
<full SHA>

Owned paths changed:
<summary>

python -m pytest:
PASS / FAIL
<test count>

python -m pipeline.main --validate-config:
PASS / FAIL

python -m pipeline.main --validate-sources:
PASS / FAIL / NOT RUN
<source summary>

pip-audit:
PASS / FAIL

Generated dataset sanity:
<article count / successful-source count if run>

Known limitations:
NONE / details

Shared contract changes:
NONE

Forbidden paths changed:
NONE
If either shared contracts or forbidden paths were changed, do not declare completion.
58. Commit and Stop
After all owned gates pass:
inspect git diff;
commit only this workstream's changes;
capture the full commit SHA;
report completion;
stop.
Do not:
merge into integration;
cherry-pick other feature branches;
implement frontend wiring;
begin deployment work.
The supervisor owns integration.
Related Authoritative Documents
docs/v1/README.md
docs/v1/01-product.md
docs/v1/02-architecture.md
docs/v1/contracts.md
docs/v1/03-content-sources.md
docs/v1/04-taxonomy-scoring.md
docs/v1/07-pipeline-deployment.md
docs/v1/08-security-dependencies.md
docs/v1/09-testing-acceptance.md
This workstream produces the deterministic static ArticleDataset consumed by the rest of V1. It must not implement browser behavior.
