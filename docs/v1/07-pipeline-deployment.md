# Intentional Reading — V1 Pipeline and Deployment Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/07-pipeline-deployment.md`\
**Role:** Authoritative ingestion execution, source failure handling, dataset retention, build validation, GitHub Actions, and GitHub Pages deployment specification

---

## 1. Purpose

This document defines how Intentional Reading V1:

1. retrieves approved content sources;
2. produces `data/articles.json`;
3. tolerates individual source failures;
4. prevents catastrophic datasets from replacing a healthy deployment;
5. bounds the discovery corpus;
6. builds the static production artifact;
7. deploys automatically to GitHub Pages.

The production application remains static.

Python is a build-time tool, not a production service.

---

# 2. Deployment Model

The production flow is:

```text
Approved publishers
        ↓
GitHub Actions
        ↓
Python ingestion pipeline
        ↓
data/articles.json
        ↓
static site staging directory
        ↓
GitHub Pages artifact
        ↓
GitHub Pages
```

There is:

```text
no backend server
no production Python process
no database
no content-update commit bot
```

---

# 3. Pipeline Entry Point

The primary pipeline entry point is:

```text
python -m pipeline.main
```

Default behavior:

After loading and validating configuration and capturing the run-wide `generatedAt`, process enabled sources and the combined candidate set in this exact conceptual order:

1. fetch;
2. adapter extraction;
3. plain-text normalization;
4. URL canonicalization and required-field validation;
5. stable Article ID and date normalization;
6. organic taxonomy matching;
7. source-specific admission filtering;
8. forced tag application;
9. compute metadata confidence needed for duplicate-winner selection;
10. deduplication;
11. full base scoring;
12. retention;
13. final dataset validation;
14. write `data/articles.json`;
15. emit a concise execution summary.

Metadata confidence is calculated before deduplication because duplicate-winner selection requires it. This pre-dedup metadata calculation is not full base scoring. The complete Article base score is calculated after deduplication.

An output override may be supported:

```text
python -m pipeline.main --output <path>
```

This is useful for:

- tests;
- temporary builds;
- development validation.

---

# 4. Configuration Validation Mode

The pipeline should support:

```text
python -m pipeline.main --validate-config
```

This validates:

```text
config/sources.json
config/topics.json
```

without performing publisher network requests.

Validation includes all rules defined in:

```text
03-content-sources.md
04-taxonomy-scoring.md
contracts.md
```

Configuration failure is fatal.

---

# 5. Live Source Validation Mode

The pipeline must support:

```text
python -m pipeline.main --validate-sources
```

This performs live source retrieval and parsing validation.

Expected output is conceptually:

```text
✓ quanta
✓ science_aaas
✓ acm_queue
✓ anthropic_engineering
...
✗ example_source
  HTTP timeout
```

This mode:

- performs real network access;
- validates all enabled sources;
- reports failures individually;
- does not replace fixture-based unit tests;
- does not deploy anything.

If any enabled source fails validation:

```text
process exit code ≠ 0
```

This command is primarily an operational/developer diagnostic and is not itself the scheduled deployment gate.

## Amendment 4 — initial-release live validation

The initial V1 release may accept a non-zero `--validate-sources` result only for these specifically investigated upstream conditions:

- `n8n_release_notes`: the configured canonical page returns HTTP 200, advertises `https://docs.n8n.io/changelog/release-notes/rss.xml`, that advertised feed returns HTTP 404, and no alternative RSS/Atom feed is advertised;
- `ietf_scim`: the approved endpoint returns HTTP 200 and valid Atom 1.0 without a parse error, but contains zero `<entry>` elements.

Both sources remain enabled and must remain reported as failed while those exact conditions persist. Release acceptance additionally requires the normal catastrophic dataset gates to pass and no evidence of an implementation regression. The investigated diagnostic baseline was 20 of 22 sources successful with 221 retained Articles.

This is not a general allowance for two failed sources. Any additional source failure, or any materially changed failure mode for either listed source, requires investigation and is not covered automatically. Upstream recovery should restore normal ingestion without another amendment.

---

# 6. Generation Timestamp

At the start of a normal pipeline generation run, capture exactly one:

```text
generatedAt
```

UTC timestamp.

That value is used consistently for:

- freshness scoring;
- future-date handling;
- dataset metadata.

Do not call the current clock independently for every article's freshness calculation.

This improves deterministic behavior within a generation run.

---

# 7. Source Processing Order

Sources should be processed in the order defined by:

```text
config/sources.json
```

V1 does not require parallel source fetching.

Sequential retrieval is preferred because:

- there are only 22 configured sources;
- implementation is simpler;
- logs remain easier to interpret;
- it avoids unnecessary request bursts toward publishers.

A future optimization may introduce bounded concurrency through an explicit specification revision.

---

# 8. HTTP Client Behavior

External source retrieval should use one reusable HTTP client/session for the pipeline run.

Requests must:

- use HTTP or HTTPS only;
- follow ordinary redirects;
- use explicit timeouts;
- identify the application with a reasonable User-Agent;
- avoid aggressive retry behavior.

V1 must not attempt to disguise itself as a logged-in interactive browser.

---

# 9. User-Agent

Use a descriptive User-Agent conceptually similar to:

```text
IntentionalReading/1.0 (+https://irodriguez.io/News-Agregator/)
```

Minor formatting variation is acceptable.

The User-Agent should identify the application rather than impersonate Chrome, Safari, or another browser.

---

# 10. Request Timeouts

Use explicit network timeouts.

Recommended V1 values:

```text
connect timeout: 10 seconds
read timeout:    20 seconds
```

A request must never be allowed to wait indefinitely.

Timeout failure is handled as a source failure.

---

# 11. Retry Policy

V1 allows at most:

```text
1 retry
```

after the initial request for transient failures.

Eligible transient failures include:

```text
408
429
500
502
503
504
network connection interruption
read timeout
```

Do not retry ordinary permanent client failures such as:

```text
400
401
403
404
410
```

unless the source adapter has an explicitly documented reason.

---

# 12. Retry Delay

Use a short bounded backoff.

Conceptually:

```text
initial failure
    ↓
wait approximately 2 seconds
    ↓
single retry
```

If a valid `Retry-After` value is supplied and is reasonably short, it may be honored.

Do not make a scheduled GitHub Action sleep for long publisher throttling windows.

If throttling cannot be resolved quickly:

```text
mark source failed
continue pipeline
```

---

# 13. Redirect Policy

Normal HTTP redirects may be followed.

Recommended maximum:

```text
5 redirects per retrieval
```

Redirect loops or excessive redirect chains are source failures.

A runtime redirect does not automatically rewrite:

```text
config/sources.json
```

Permanent catalog changes require review.

---

# 14. Response-Size Boundary

V1 feed/listing retrieval should reject unexpectedly large individual responses.

Recommended upper bound:

```text
10 MiB per fetched feed/listing resource
```

This protects the scheduled pipeline from:

- accidental giant responses;
- malformed publishers;
- runaway memory use.

A source exceeding the boundary is treated as failed and logged.

The pipeline does not retrieve complete article media or image assets.

---

# 15. No Article-Body Crawl

The pipeline fetches only what is necessary for approved source ingestion.

For feed-based sources:

```text
fetch feed
```

For RSS autodiscovery:

```text
fetch canonical listing page
        ↓
discover publisher feed
        ↓
fetch feed
```

For HTML listing adapters:

```text
fetch approved listing/release page
```

V1 must not recursively fetch every article page merely to:

- improve excerpts;
- calculate reading time;
- enrich metadata;
- inspect images.

If sufficient metadata is not supplied by the approved ingestion surface:

```text
use null/empty contract values
```

rather than expanding into a crawler.

---

# 16. Source Success Definition

A source counts as operationally successful when:

1. its configured resource can be retrieved;
2. the adapter parses successfully;
3. at least one raw source entry is produced before source-specific topic admission filtering.

A source may therefore be successful even when:

```text
accepted article count = 0
```

if all valid entries were intentionally rejected by an admission filter.

Example:

```text
OpenAI feed retrieved successfully
8 raw release entries parsed
0 match approved technical admission topics

source status = successful
accepted = 0
```

This is not a source failure.

---

# 17. Empty Parsed Source

If retrieval technically succeeds but the adapter produces:

```text
0 raw entries
```

the source is considered failed.

For a configured publication/feed, an empty parse is more likely to indicate:

- source-format change;
- broken selector;
- feed-discovery failure;
- parser regression.

Log the issue explicitly.

---

# 18. Per-Source Operational Record

During a run, track for each source:

```text
source ID
status
fetched/raw count
normalized count
accepted count
rejected count
duplicate contribution
error summary if failed
```

These detailed operational records are primarily for logs.

The generated dataset exposes only the pipeline metadata required by the shared dataset contract.

---

# 19. Source Failure Isolation

One failed source must not terminate normal generation.

Example:

```text
quanta                  ✓
science_aaas            ✓
anthropic_engineering   ✗
cloudflare_blog         ✓
ietf_oauth              ✓
```

Processing continues.

Final deployment eligibility is determined by dataset sanity checks, not by requiring 100% source availability.

---

# 20. Fatal Configuration Failure

The following are fatal before publisher processing:

- malformed JSON configuration;
- duplicate source IDs;
- duplicate topic IDs;
- invalid category IDs;
- invalid adapter IDs;
- invalid quality/content scores;
- unknown forced-tag IDs;
- unknown admission topic IDs;
- taxonomy ambiguity prohibited by `04-taxonomy-scoring.md`.

Configuration failure means:

```text
pipeline exits non-zero
no production dataset written
no deployment
```

---

# 21. Unexpected Pipeline Exceptions

A source-specific exception should be caught at the source boundary when possible.

A truly pipeline-wide programming error such as:

- failed output serialization;
- corrupted in-memory dataset structure;
- uncaught scoring failure;
- impossible schema invariant;

is fatal.

Do not swallow arbitrary exceptions merely to force deployment.

---

# 22. Dataset Metadata

The generated output must contain:

```json
{
  "schemaVersion": 1,
  "generatedAt": "...",
  "pipeline": {
    "enabledSourceCount": 22,
    "successfulSourceCount": 21,
    "failedSourceCount": 1,
    "articleCount": 214
  },
  "articles": []
}
```

The counts describe the current generation run.

`articleCount` must equal:

```text
len(articles)
```

after retention.

---

# 23. Degraded Dataset

A dataset is operationally degraded when:

```text
failedSourceCount > 0
```

but all catastrophic sanity checks still pass.

A degraded dataset may deploy.

The frontend may use pipeline metadata to indicate that some current sources were unavailable.

Read Later and History remain independent of this condition.

---

# 24. Retention Philosophy

The generated dataset is:

```text
current discovery corpus
```

not:

```text
permanent content archive
```

Local interacted-article snapshots provide personal archival durability.

Therefore the global generated corpus is intentionally bounded.

---

# 25. Maximum Article Age

For articles with a valid:

```text
publishedAt
```

retain only articles no older than:

```text
45 days
```

relative to `generatedAt`.

Articles older than 45 days are removed before final output.

---

# 26. Unknown Publication Dates

An Article with:

```text
publishedAt = null
```

cannot be reliably age-filtered.

It remains eligible for retention but is still constrained by:

- per-source cap;
- global dataset cap;
- deterministic scoring/order.

Its freshness score remains the approved unknown-date value from `04-taxonomy-scoring.md`.

---

# 27. Per-Source Cap

After deduplication and scoring, retain at most:

```text
40 Articles per source
```

When a source has more than 40 eligible articles, retain its highest-ranked articles using:

1. base score descending;
2. publication date descending, unknown last;
3. Article ID ascending.

This allows freshness to influence retention through the base score without making age the only criterion.

---

# 28. Global Dataset Cap

After age filtering and per-source limits, retain at most:

```text
500 Articles total
```

If more remain, retain by:

1. base score descending;
2. publication date descending, unknown last;
3. source ID ascending;
4. Article ID ascending.

The browser's personalization and diversity logic subsequently controls user-facing ordering.

---

# 29. Retention Order

Apply dataset retention in exactly this conceptual order:

```text
deduplicated/scored articles
        ↓
45-day known-date cutoff
        ↓
40-per-source limit
        ↓
500-total limit
        ↓
final deterministic ordering
```

Do not apply the total cap independently inside each adapter.

---

# 30. No Category Quota

V1 does not impose artificial:

```text
equal articles per category
```

quotas.

The curated source list and browser-side diversity logic are responsible for variety.

A future version may add category floors only if real usage demonstrates a need.

---

# 31. Final Dataset Ordering

After retention, output Articles in the deterministic ordering already defined by `04-taxonomy-scoring.md`:

1. base score descending;
2. publication date descending, unknown last;
3. source ID ascending;
4. Article ID ascending.

This is not the final Discover ordering.

Browser personalization reorders eligible articles locally.

---

# 32. Catastrophic Sanity Check: Minimum Articles

A generated dataset may deploy only when:

```text
articleCount >= 20
```

If:

```text
articleCount < 20
```

generation is considered catastrophically unhealthy.

Result:

```text
pipeline exits non-zero
deployment blocked
```

---

# 33. Catastrophic Sanity Check: Source Success Ratio

At least:

```text
50% of enabled sources
```

must succeed operationally.

Calculate:

```text
successfulSourceCount >= ceil(enabledSourceCount * 0.50)
```

For 22 enabled V1 sources:

```text
minimum successful sources = 11
```

If this threshold fails:

```text
deployment blocked
```

---

# 34. No Mandatory Per-Category Threshold

A temporary outage affecting an entire small category does not automatically prevent deployment if the global sanity thresholds pass.

The resulting dataset is degraded.

This is intentional because:

- other categories may still provide substantial current value;
- existing Read Later/History remain available;
- requiring every category would make the deployment unnecessarily fragile.

Operational logs should make category/source outages visible.

---

# 35. Dataset Structural Validation

Before deployment, validate:

- top-level schema version;
- required metadata fields;
- `articleCount` consistency;
- every Article contract;
- unique Article IDs;
- valid categories;
- valid source IDs;
- valid content types;
- valid topic IDs;
- exact score-sum invariant;
- valid URLs;
- valid timestamp/null fields.

A structurally invalid final dataset is fatal.

---

# 36. Dataset Score Validation

Every article must satisfy:

```text
base =
    sourceQuality
  + contentType
  + freshness
  + topicSignal
  + metadata
```

and:

```text
0 <= base <= 100
```

A score invariant violation blocks deployment.

---

# 37. Atomic Dataset Write

Do not write directly over the final output while serialization is in progress.

Preferred pattern:

```text
serialize complete dataset
        ↓
write temporary file beside destination
        ↓
flush/close
        ↓
atomically replace data/articles.json
```

A failed generation must not leave a truncated partial JSON file.

---

# 38. JSON Encoding

Write:

```text
UTF-8
no BOM
```

Use human-readable JSON formatting.

Recommended:

```text
2-space indentation
```

Do not escape ordinary Unicode text unnecessarily.

Output property ordering should be stable enough to aid debugging and reproducibility.

---

# 39. Generated File Version-Control Policy

Scheduled content refreshes must not create commits.

`data/articles.json` is a generated production artifact.

The repository should prevent routine accidental tracking of generated current-feed output, for example through:

```text
/data/articles.json
```

in `.gitignore`.

Tests should use explicit fixtures rather than depending on a committed live dataset.

---

# 40. Pipeline Logging

Normal pipeline logs should summarize rather than flood CI.

Example:

```text
[quanta]
Raw:        15
Normalized: 15
Accepted:   15

[barbell_medicine]
Raw:        20
Normalized: 19
Accepted:    8
Rejected:   11
  topic admission: 10
  invalid URL:      1

[anthropic_engineering]
FAILED
Reason: listing parse produced 0 entries

--------------------------------------------------
Enabled sources:     22
Successful:          21
Failed:               1
Raw entries:        287
Accepted:           238
Duplicates:          21
Expired:              7
Retained:           210
```

Exact formatting is not contractual.

The information content is.

---

# 41. Logging Safety

Do not dump:

- complete remote HTML;
- complete feed bodies;
- full article bodies;
- imported browser data;
- secrets.

There are no V1 application secrets, but logging should still remain bounded and intentional.

Error messages should identify:

```text
source ID
operation
concise failure
```

---

# 42. GitHub Actions Workflows

V1 contains:

```text
.github/workflows/test.yml
.github/workflows/deploy.yml
```

Their responsibilities are separate.

Detailed test matrices and security gates are defined in:

```text
08-security-dependencies.md
09-testing-acceptance.md
```

---

# 43. Deploy Workflow Triggers

`deploy.yml` runs on:

```text
push to main
schedule every 6 hours
workflow_dispatch
```

Use a non-top-of-hour scheduled minute to avoid unnecessary synchronization with large numbers of unrelated scheduled workflows.

Recommended cron:

```text
17 */6 * * *
```

This gives four ingestion runs per day.

The exact minute `17` has no product significance.

---

# 44. Push-to-Main Deployment

When V1 code is pushed/merged to:

```text
main
```

the deployment workflow must:

1. check out the repository;
2. install required Python dependencies;
3. install JS development dependencies;
4. run the full release-gate test/security suite required by `09-testing-acceptance.md`;
5. generate the live article dataset;
6. run catastrophic dataset validation;
7. assemble the Pages artifact;
8. deploy only if every gate passes.

A failing test must prevent production deployment.

---

# 45. Manual Deployment

`workflow_dispatch` follows the same quality gates as a code-triggered deployment.

Manual execution is not a bypass around:

- tests;
- security checks;
- dataset validation.

It exists for intentional reruns or operational recovery.

---

# 46. Scheduled Refresh

For scheduled six-hour refreshes, application source code has not changed since the last accepted deployment.

Therefore a scheduled run does not need to reinstall/run the entire frontend development test suite merely to refresh content.

Scheduled path:

```text
checkout current main
        ↓
setup Python
        ↓
install Python runtime dependencies
        ↓
validate configuration
        ↓
generate live dataset
        ↓
validate dataset/sanity
        ↓
assemble static artifact
        ↓
deploy
```

The pipeline generation itself exercises live source retrieval.

Full frontend tests remain code-change gates rather than six-hour content-refresh work.

---

# 47. Scheduled Refresh Security

Scheduled ingestion must use current default-branch code only.

It must not:

- pull implementation code from arbitrary branches;
- execute scripts supplied by remote publishers;
- install dependencies declared by feed content;
- checkout source repositories linked from Articles.

Remote publisher data is input, never executable instruction.

---

# 48. Source Failure Isolation

Each source produces an explicit success or failure result. A failure does not erase successful results from unrelated sources and does not authorize fabricated fallback content.

Retries are bounded. Timeouts and maximum response sizes prevent one publisher from occupying the workflow indefinitely.

---

# 49. Last-Known-Good Behavior

If generation, validation, sanity checks, security checks, artifact assembly, or deployment fails, the currently deployed GitHub Pages version remains the last known good version.

A failed refresh must never replace production with an empty, structurally invalid, or catastrophically reduced dataset.

---

# 50. Workflow Permissions

Workflows use least privilege.

The Pages deployment job requires only the permissions needed by the official deployment flow, including:

```text
pages: write
id-token: write
contents: read
```

Other jobs must not inherit write access unnecessarily.

---

# 51. Concurrency

Pages deployments use a concurrency group so overlapping refreshes do not race to publish different artifacts. A newer queued deployment may supersede an older queued run, but an in-progress production deployment must be handled according to the approved Pages workflow behavior.

---

# 52. Artifact Contents

The deployable static artifact contains only required site assets, configuration-independent frontend files, and the validated generated dataset.

It must not contain:

- repository history;
- local backups;
- test fixtures;
- environment files;
- caches;
- pipeline logs;
- secrets.

---

# 53. Deployment Environment

The deployment job targets the repository's GitHub Pages environment and exposes the resulting page URL through the workflow environment metadata.

No custom application server or runtime ingestion service is created.

---

# 54. Observability

Workflow summaries report:

- enabled/successful/failed source counts;
- admitted/rejected/deduplicated Article counts;
- validation and sanity-check outcomes;
- artifact/deployment outcome.

Diagnostics remain bounded and must not include raw publisher bodies or local user data.

---

# 55. Operational Recovery

An authorized maintainer may use `workflow_dispatch` to rerun the approved workflow after a transient publisher or GitHub failure. Manual execution uses the same validation, security, sanity, and deployment gates; it is never a bypass.

---

# 56. Deployment Completion Criteria

The pipeline/deployment work is complete when push/manual runs execute full release gates, scheduled runs perform the approved content-refresh path, source failures remain isolated, invalid/catastrophic datasets cannot deploy, JSON writes are atomic, permissions are least-privilege, and the deployed artifact contains the validated static application only.
