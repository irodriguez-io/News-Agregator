# Intentional Reading — V1 Security and Dependency Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/08-security-dependencies.md`\
**Role:** Authoritative security boundaries, dependency policy, browser hardening, content trust rules, CI supply-chain controls, and vulnerability-management specification

---

## 1. Purpose

This document defines the security posture for Intentional Reading V1.

The application has a deliberately small attack surface:

- static frontend;
- no user authentication;
- no backend application server;
- no database;
- no application secrets;
- no generative-AI API keys;
- public publisher ingestion at build time;
- browser-local personal state.

Security should preserve that simplicity rather than introduce infrastructure disproportionate to the application's risk.

The primary V1 risks are:

1. untrusted publisher content;
2. dependency/supply-chain vulnerabilities;
3. unsafe DOM rendering;
4. malicious or malformed imported local-state backups;
5. compromised or malformed publisher URLs;
6. GitHub Actions supply-chain risk;
7. accidental introduction of secrets or telemetry.

---

# 2. Security Principles

V1 follows these principles:

```text
minimize dependencies
        +
treat remote content as untrusted
        +
keep personal state local
        +
use least-privilege CI
        +
avoid secrets entirely
        +
fail safely
```

The preferred security control is often:

> Do not introduce the capability.

Examples:

```text
No backend
→ no backend credential exposure

No AI API
→ no AI API key exposure

No runtime npm framework
→ smaller browser dependency surface

No article HTML rendering
→ reduced XSS surface

No analytics
→ reduced privacy surface
```

---

# 3. Trust Boundaries

V1 has four explicit trust boundaries.

## Boundary A — Repository-controlled code/configuration

Examples:

```text
HTML
CSS
JavaScript
Python
sources.json
topics.json
GitHub Actions
```

These are trusted only after normal code review and CI.

---

## Boundary B — Remote publisher content

Examples:

```text
RSS
Atom
publisher HTML listings
article titles
article excerpts
authors
external URLs
publication metadata
```

All remote publisher data is untrusted input.

Publisher whitelisting does not make publisher content safe for direct HTML execution.

---

## Boundary C — Imported browser backup

An imported JSON backup is untrusted input even when the user believes they created it previously.

It may be:

- malformed;
- manually edited;
- corrupted;
- unexpectedly large;
- from an incompatible version.

It must be completely validated before replacing active local state.

---

## Boundary D — Third-party development/build dependencies

Examples:

```text
Python packages
npm development packages, if any
GitHub Actions
```

These execute with developer or CI privileges and therefore require explicit versioning, auditing, and update controls.

---

# 4. No Application Secrets

V1 must not require or contain:

```text
OpenAI API key
Anthropic API key
publisher credentials
GitHub personal access token
database credentials
OAuth client secrets
service-account credentials
analytics tokens
private API keys
```

No secret-like placeholder should be added simply in anticipation of a future feature.

Future functionality requiring secrets requires an explicit architecture/security revision.

---

# 5. No Secret Material in Frontend

The deployed artifact is public.

Nothing under production frontend paths may be treated as confidential.

Specifically:

```text
index.html
css/**
js/**
data/articles.json
```

must contain no secret material.

Environment-variable substitution into browser JavaScript is not a security mechanism.

---

# 6. Runtime Third-Party Network Boundary

During normal application operation, the frontend should require network access only to:

```text
the GitHub Pages origin itself
```

for application resources such as:

```text
HTML
CSS
JavaScript
data/articles.json
```

External publisher navigation occurs only after an explicit user action.

V1 must not make background runtime requests to:

- rss2json;
- OpenAI;
- Anthropic;
- Google Analytics;
- advertising networks;
- third-party fonts;
- CDNs for JavaScript libraries;
- recommendation APIs;
- telemetry services.

---

# 7. System Fonts Only

The approved design system uses system/fallback font stacks.

V1 must not introduce remote web-font services.

This avoids:

- additional runtime dependencies;
- privacy leakage;
- render-blocking third-party requests;
- CSP complexity.

---

# 8. Remote Article Content Is Plain Text

The pipeline normalizes publisher-provided:

```text
title
author
excerpt
```

into plain text.

The frontend must still treat every Article field as untrusted text.

Safe rendering uses:

```text
textContent
createElement
setAttribute for validated non-code attributes
```

Remote text must not be interpreted as HTML.

---

# 9. Unsafe HTML Sinks

Application code must not use remote/persisted Article content with:

```text
innerHTML
outerHTML
insertAdjacentHTML
document.write
```

V1 should preferably avoid these APIs entirely in application runtime code.

If an implementation believes one is necessary for static repository-authored markup, it must first demonstrate why equivalent DOM construction is impractical and obtain supervisor approval.

No such use is expected in V1.

---

# 10. Dynamic Code Execution

V1 application code must not use:

```text
eval()
new Function()
setTimeout("string code")
setInterval("string code")
```

No content from:

- publishers;
- localStorage;
- imported backups;
- URLs

may become executable JavaScript.

---

# 11. URL Validation — Generated Articles

The pipeline emits only Article URLs with:

```text
http:
https:
```

schemes.

The frontend must not assume an imported/persisted URL is safe merely because generated datasets normally satisfy this invariant.

Before external navigation, the browser layer must validate:

```text
protocol == http: OR https:
```

Invalid URLs must not be opened.

---

# 12. External Navigation Safety

Publisher links:

- open only after explicit user action;
- open in a new tab/window;
- use `noopener`;
- use `noreferrer`;
- visibly communicate external navigation.

The application must not expose the originating window through `window.opener`.

---

# 13. No Publisher URL Construction

The frontend must use the canonical:

```text
article.url
```

provided by the Article snapshot/dataset.

It must not build publisher URLs by concatenating:

```text
source ID
title
slug
query strings
```

This avoids inconsistent navigation and unsafe string construction.

---

# 14. Publisher HTML Parsing

Build-time HTML parsers may extract metadata but must never execute remote scripts.

The parser must treat:

```text
<script>
<style>
event-handler attributes
embedded widgets
```

as data to ignore, not executable content.

Only approved metadata is normalized into the Article contract.

---

# 15. XML Parser Safety

Feed processing must not intentionally enable:

- external entity resolution;
- remote DTD retrieval;
- arbitrary XML entity expansion.

V1 feed parsing should remain within the behavior of the approved feed-processing library.

Do not introduce an XML parser configured to retrieve external entities.

---

# 16. Source URL Validation

Configured publisher URLs must:

- use HTTP or HTTPS;
- contain a valid hostname;
- satisfy source configuration validation.

Other schemes are prohibited.

Examples rejected:

```text
file:
ftp:
javascript:
data:
ssh:
```

---

# 17. SSRF Protection for Derived URLs

Some pipeline URLs are derived from remote publisher-controlled content, particularly:

```text
RSS autodiscovery
HTTP redirects
```

Before requesting a derived URL, reject targets resolving to non-public destinations.

At minimum reject:

```text
localhost
loopback addresses
private network addresses
link-local addresses
unspecified addresses
reserved/non-public IP ranges
```

Use Python standard-library network/IP facilities where practical.

Validation should occur before requesting an autodiscovered endpoint and again when following a redirect to a materially different host.

The goal is to prevent publisher-controlled metadata from turning the GitHub Actions runner into a request proxy toward internal network services.

---

# 18. Allowed Derived URL Schemes

RSS autodiscovery and redirects may resolve only to:

```text
http
https
```

No adapter may follow a discovered:

```text
file:
data:
ftp:
```

resource.

---

# 19. Source Response Limits

The response-size, timeout, redirect, and retry boundaries in:

```text
07-pipeline-deployment.md
```

are security controls as well as reliability controls.

Implementation must not remove them merely because a publisher works without the limits during local testing.

---

# 20. Imported Backup Size

Before reading/parsing an imported local-state backup, enforce a V1 maximum file size of:

```text
5 MiB
```

A larger file must be rejected before replacing application state.

This is a defensive bound, not a normal expected backup size.

---

# 21. Imported Backup Validation

Import validation is all-or-nothing.

Validate at minimum:

```text
schema version
root object structure
preference maps
preference weight bounds
interaction counts
article ID format
Article snapshot structure
status enums
category enums
appearance enum
timestamp/null values
external URL protocols
signal flags
```

No invalid candidate state may partially replace current state.

---

# 22. Article ID Validation During Import

Persisted article-map keys and snapshot IDs must:

- match one another;
- conform to V1 Article ID format.

Expected pattern:

```text
20 lowercase hexadecimal characters
```

Conceptually:

```text
^[0-9a-f]{20}$
```

Invalid Article IDs reject the import.

---

# 23. Preference Key Validation During Import

Source/topic preference-map keys must be bounded plain identifiers.

They must not be used as executable/property-path expressions.

Implementation should reject dangerous property names such as:

```text
__proto__
prototype
constructor
```

and avoid unsafe deep-merge behavior.

Import should construct validated state deliberately rather than recursively merging arbitrary JSON into application objects.

---

# 24. Imported String Bounds

Imported Article snapshots must enforce reasonable field bounds consistent with pipeline normalization.

At minimum:

```text
title       <= 500 characters
author      <= 200 characters when non-null
excerpt     <= 800 characters
```

Labels and source identifiers must also be reasonably bounded.

Imported content must not bypass the safety constraints applied to generated content.

---

# 25. Import Does Not Execute Content

Imported:

```text
titles
excerpts
source names
tag labels
content-type labels
```

remain text.

An imported value containing:

```html
<script>alert(1)</script>
```

must display, if retained at all, as inert text rather than executable markup.

---

# 26. LocalStorage Is Not a Security Boundary

Browser localStorage is used for convenience and persistence, not confidentiality.

The application must assume that a user with access to the browser profile can inspect or modify local state.

V1 does not attempt to encrypt localStorage.

No secret or credential is stored there.

---

# 27. Corrupt LocalStorage

Malformed or incompatible localStorage data must not be silently overwritten.

The application should:

1. detect the problem;
2. preserve the existing raw value;
3. surface a recoverable error;
4. permit explicit reset if the user chooses.

This preserves the possibility of manual recovery/export diagnosis.

---

# 28. Content Security Policy

V1 should deploy a restrictive Content Security Policy through repository-controlled HTML where compatible with GitHub Pages hosting.

The intended policy should allow:

```text
application scripts from self
application styles from self
application fetches to self
```

and prohibit unnecessary third-party resource classes.

Target posture:

```text
default-src 'self'
script-src 'self'
connect-src 'self'
object-src 'none'
base-uri 'none'
```

Because V1 uses no article imagery or remote fonts, corresponding directives should be kept restrictive.

The exact final CSP must be tested against:

- swipe interaction;
- theme behavior;
- local data export/import;
- the approved UI.

Do not weaken `script-src` with:

```text
'unsafe-eval'
```

or arbitrary third-party origins.

---

# 29. Inline Script Policy

Application JavaScript belongs in repository-controlled module files.

Avoid inline executable `<script>` blocks.

`index.html` should load:

```text
./js/app.js
```

as the application module entry point.

This supports a restrictive script policy and clearer code ownership.

---

# 30. No Third-Party Runtime JavaScript

V1 production HTML must not load JavaScript from:

```text
unpkg
jsDelivr
cdnjs
Google
third-party widgets
analytics services
```

All application runtime JavaScript is repository-controlled and deployed with the static artifact.

---

# 31. No Third-Party Runtime CSS

V1 must not load:

- CDN stylesheets;
- remote UI frameworks;
- Google Fonts stylesheets.

Production CSS is repository-controlled.

---

# 32. Frontend Runtime Dependency Policy

V1 target:

```text
third-party npm runtime dependencies = 0
```

`package.json` must not contain production `dependencies` without a formal specification revision.

Vanilla browser APIs are sufficient for V1 runtime functionality.

---

# 33. JavaScript Test Dependency Preference

Prefer:

```text
Node.js built-in node:test
```

for V1 State/Ranking/Data unit tests.

The core JavaScript modules are intentionally designed to be testable without a DOM framework.

If the acceptance suite can be implemented with built-in Node tooling:

```text
third-party npm development dependencies = 0
```

is preferred.

---

# 34. Vitest/jsdom Exception

`02-architecture.md` permits development-only test dependencies such as Vitest and jsdom.

They may be added only if:

1. a required V1 automated test cannot reasonably be implemented with built-in Node tooling;
2. the agent documents the concrete need;
3. dependencies remain development-only;
4. exact versions are recorded;
5. `package-lock.json` is committed;
6. dependency audits pass.

They must never become browser runtime dependencies.

---

# 35. Node.js Version

V1 JavaScript tooling targets:

```text
Node.js 24 LTS
```

The chosen major version should be recorded consistently in:

- local developer guidance;
- CI setup;
- package engine metadata where used.

Do not run CI on an EOL Node release.

Patch/minor updates within the approved LTS line are expected over time.

---

# 36. npm Installation Policy

When third-party npm packages exist, CI must use:

```text
npm ci
```

rather than an unconstrained install that rewrites dependency resolution.

`package-lock.json` must be committed whenever package dependencies exist.

The lockfile is authoritative for CI dependency resolution.

---

# 37. npm Version Specifiers

If npm development dependencies are introduced, direct dependency declarations should use exact versions rather than permissive:

```text
^
~
*
latest
```

ranges.

Dependency updates should be explicit and reviewable.

---

# 38. npm Vulnerability Audit

When npm dependencies exist, CI must run:

```text
npm audit --audit-level=high
```

Result policy:

```text
HIGH vulnerability      → fail gate
CRITICAL vulnerability  → fail gate
```

Lower-severity findings remain visible for review but do not automatically block V1 release unless separately judged material.

Do not run:

```text
npm audit fix --force
```

automatically in CI.

Security updates must remain reviewable code/dependency changes.

---

# 39. Python Version

V1 pipeline tooling should use one explicitly supported Python 3 version consistently in CI.

Target V1 baseline:

```text
Python 3.13
```

Local development may use another currently supported Python version when compatible, but CI is the reproducibility baseline.

---

# 40. Python Runtime Dependencies

Initial expected direct runtime dependencies remain limited to:

```text
feedparser
requests
beautifulsoup4
python-dateutil
```

A new Python runtime dependency requires justification.

Before adding a dependency, prefer:

```text
Python standard library
```

when it provides a clear and maintainable solution.

Examples where the standard library is intentionally preferred:

```text
hashlib       → Article IDs
difflib       → near-duplicate title comparison
ipaddress     → network target validation
json          → configuration/output
```

---

# 41. Python Version Pinning

Python direct dependencies must use exact reviewed versions in the committed requirements manifest.

Conceptually:

```text
package==X.Y.Z
```

Avoid unbounded declarations such as:

```text
package
package>=X
package~=X
```

in the CI installation manifest.

Dependency version changes must appear clearly in Git diffs.

---

# 42. Python Development Dependencies

Development/security tools such as:

```text
pytest
pip-audit
```

may be maintained separately from pipeline runtime dependencies.

A separate:

```text
requirements-dev.txt
```

is permitted and recommended.

It may reference the runtime requirements where appropriate.

Development dependencies must also use reviewed versions.

---

# 43. Python Vulnerability Audit

CI must run `pip-audit` against the installed/resolved Python dependency environment or reviewed requirements.

Known vulnerable Python dependencies must cause the security gate to fail.

V1 does not automatically invoke:

```text
pip-audit --fix
```

Dependency remediation is performed through an explicit reviewed update.

Because Python advisory sources do not always provide a consistent severity suitable for a simple HIGH/CRITICAL threshold, V1 intentionally uses the stricter policy:

```text
known Python dependency vulnerability
→ fail security gate
```

unless the specification is explicitly amended.

---

# 44. Vulnerability Scanner Limitations

Dependency scanners identify known advisories.

They do not prove that:

```text
all dependencies are safe
all malicious packages are detected
application code is vulnerability-free
```

Therefore V1 combines:

- minimal dependencies;
- exact direct pins;
- dependency auditing;
- Dependabot;
- safe coding boundaries;
- tests.

No audit tool is treated as a complete security proof.

---

# 45. Dependabot Configuration

The repository must contain:

```text
.github/dependabot.yml
```

covering all applicable package ecosystems:

```text
pip
npm
github-actions
```

Use a low-noise update cadence:

```text
weekly
```

Dependency updates arrive through pull requests rather than direct automatic changes to `main`.

---

# 46. Dependabot Alerts

Dependabot vulnerability alerts must be enabled in repository security settings where available.

An alert is not silently ignored. Resolution may be an upgrade, removal, replacement, or a documented time-bounded exception approved by the maintainer.

---

# 47. GitHub Actions Pinning

Third-party and official actions must be pinned to full-length immutable commit SHAs.

Human-readable version comments may accompany the SHA, but tags alone are not authoritative pins.

Dependabot should monitor the `github-actions` ecosystem so pinned references can be updated through reviewable pull requests.

---

# 48. Workflow Permissions

Use top-level read-only permissions where practical and grant narrower job-specific permissions only where required. Pull-request validation must not receive Pages deployment privileges.

---

# 49. Secrets

V1 requires no application API keys, publisher credentials, user accounts, database credentials, or client secrets.

Secrets must not appear in:

- frontend JavaScript;
- generated JSON;
- repository files;
- workflow logs;
- test fixtures;
- downloadable state exports.

If a future source requires authentication, that is an architecture and security revision, not an environment-variable shortcut.

---

# 50. Untrusted URL Handling

Only normalized HTTP/HTTPS Article URLs may reach navigation. New-tab navigation must isolate the opener with `noopener` and `noreferrer` semantics.

Article titles, authors, excerpts, tags, source names, and URLs remain untrusted even when the publisher is whitelisted.

---

# 51. DOM Rendering Boundary

Remote text is rendered with `textContent` or equivalent DOM construction. Publisher HTML must not be assigned to `innerHTML`.

The application must not create inline event handlers or execute markup-derived scripts, styles, or URLs.

---

# 52. Fetch Boundaries

The browser fetches only its own static assets and dataset. It never fetches publisher feeds at runtime.

Pipeline fetches enforce approved schemes, timeouts, redirect limits, response-size limits, and source isolation. Remote responses are data only.

---

# 53. Import Security

Local-state import is parsed as data and validated completely before replacement. It must not evaluate JavaScript, hydrate prototypes from arbitrary values, or accept unsupported schema versions.

Invalid import leaves current state unchanged.

---

# 54. Export Security

Export includes only the application state defined by `contracts.md`. It must not include browser metadata, unrelated localStorage entries, credentials, cookies, or browsing history.

---

# 55. Logging and Privacy

Logs may contain source IDs, status codes, bounded error categories, counts, and timing. They must not contain raw feed bodies, arbitrary excerpts, local user preferences, Read Later/History contents, or imported backup contents.

---

# 56. Content Security Policy

Where supported by the static hosting surface, use a restrictive policy consistent with a self-contained static site:

- scripts and styles from self;
- no object embedding;
- no frame ancestors;
- no broad remote image dependency;
- connections limited to self at browser runtime.

The exact deployable policy must be verified against the implementation rather than copied without testing.

---

# 57. Security Gate

The release gate includes:

- deterministic tests;
- configuration and dataset validation;
- `npm audit` at the approved severity threshold;
- `pip-audit` with failure on known Python dependency vulnerabilities;
- verification that Actions references are full SHAs;
- checks for prohibited runtime dependencies and accidental secret files.

---

# 58. Exception Policy

A security exception must identify the advisory/control, affected dependency or boundary, actual exposure, compensating control, owner, and expiry/review date. Exceptions do not silently weaken automated tests.

---

# 59. Security Completion Criteria

V1 is security-complete when remote content is plain text at the DOM boundary, no browser secrets or runtime publisher fetches exist, state import is atomic and non-executable, dependencies and Actions are pinned/audited, least-privilege workflows deploy only validated artifacts, and security failures block release without replacing the last known good site.
