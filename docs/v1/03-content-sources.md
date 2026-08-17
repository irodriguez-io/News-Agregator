# Intentional Reading — V1 Content Sources

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/03-content-sources.md`\
**Role:** Authoritative source catalog, source metadata, adapters, content types, and source-specific admission rules

---

## 1. Purpose

This document defines the complete V1 source whitelist.

Only sources explicitly defined here may enter the V1 Discover corpus.

Source selection is intentionally conservative.

The objective is not broad news coverage.

The objective is:

> A small set of consistently high-value sources whose content is worth competing for the user's finite attention.

V1 contains **22 configured sources** across seven categories.

---

## 2. Source Selection Principles

A V1 source should satisfy most of the following:

- identifiable publisher or standards body;
- meaningful editorial or technical authority;
- original reporting, research, engineering, standards, or expert analysis;
- relatively high signal-to-noise ratio;
- stable canonical URLs;
- mechanically ingestible without circumventing access controls;
- useful material beyond headline-level consumption.

Source quantity is deliberately constrained.

A source should not be added merely because it publishes frequently.

---

## 3. Curated-Source Boundary

The ingestion pipeline may ingest only sources in:

```text
config/sources.json
```

and every enabled source must correspond to an approved entry in this document.

The pipeline must not:

- crawl arbitrary linked websites;
- discover new publishers automatically;
- ingest search-engine results;
- ingest social-media feeds;
- treat linked third-party articles as implicitly approved sources.

Adding a new publisher requires an explicit source-catalog revision.

---

## 4. Source Quality Scores

`quality` is an editorial configuration value owned by this project.

It is not an external rating.

V1 uses:

```text
0–50
```

General interpretation:

```text
50  Primary standards / authoritative technical bodies
48–49 Exceptional specialist or primary technical sources
45–47 High-quality specialist/editorial sources
40–44 Strong secondary reporting/analysis
<40 Not expected for initial V1 whitelist
```

Source quality is intentionally the largest component of the base score.

---

# 5. Content-Type Vocabulary

V1 sources use the following configured content types.

| ID | Label | Score |
|---|---|---:|
| `standards_update` | Standards Update | 20 |
| `official_release_notes` | Official Release Notes | 19 |
| `research_reporting` | Research & Science | 19 |
| `reported_science` | Reported Science | 18 |
| `engineering_deep_dive` | Engineering Deep Dive | 18 |
| `evidence_based_training` | Evidence-Based Training | 17 |
| `historical_essay` | Historical Essay | 16 |
| `engineering_journalism` | Engineering Journalism | 16 |
| `literary_essay` | Literary Essay | 15 |
| `reported_journalism` | Reported Journalism | 14 |

Content type is source-configured in V1.

The pipeline does not use an AI classifier to infer content type.

---

# 6. Science Sources

## 6.1 Quanta Magazine

```json
{
  "id": "quanta",
  "name": "Quanta Magazine",
  "category": "science",
  "adapter": "rss",
  "url": "https://www.quantamagazine.org/feed/",
  "quality": 48,
  "contentType": {
    "id": "reported_science",
    "label": "Reported Science",
    "score": 18
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

Quanta is intended to provide accessible but substantive reporting across mathematics and basic science.

---

## 6.2 Science / AAAS

```json
{
  "id": "science_aaas",
  "name": "Science / AAAS",
  "category": "science",
  "adapter": "rss",
  "url": "https://feeds.science.org/rss/science.xml",
  "quality": 49,
  "contentType": {
    "id": "research_reporting",
    "label": "Research & Science",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

---

# 7. Technology Sources

## 7.1 ACM Queue

```json
{
  "id": "acm_queue",
  "name": "ACM Queue",
  "category": "technology",
  "adapter": "rss",
  "url": "https://queue.acm.org/rss/feeds/queuecontent.xml",
  "quality": 49,
  "contentType": {
    "id": "engineering_deep_dive",
    "label": "Engineering Deep Dive",
    "score": 18
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

---

## 7.2 IEEE Spectrum

```json
{
  "id": "ieee_spectrum",
  "name": "IEEE Spectrum",
  "category": "technology",
  "adapter": "rss_autodiscovery",
  "url": "https://spectrum.ieee.org/",
  "quality": 46,
  "contentType": {
    "id": "engineering_journalism",
    "label": "Engineering Journalism",
    "score": 16
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Use RSS autodiscovery rather than encoding an unnecessarily brittle feed path.

Admission:

```text
all normalized feed entries
```

---

## 7.3 Ars Technica Features

```json
{
  "id": "ars_features",
  "name": "Ars Technica",
  "category": "technology",
  "adapter": "rss_autodiscovery",
  "url": "https://arstechnica.com/features/",
  "quality": 44,
  "contentType": {
    "id": "reported_journalism",
    "label": "Reported Journalism",
    "score": 14
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

The Features/long-form stream is preferred over the general high-volume Ars news feed.

Admission:

```text
all normalized feature entries
```

---

## 7.4 Cloudflare Blog

```json
{
  "id": "cloudflare_blog",
  "name": "Cloudflare Blog",
  "category": "technology",
  "adapter": "rss",
  "url": "https://blog.cloudflare.com/rss/",
  "quality": 45,
  "contentType": {
    "id": "engineering_deep_dive",
    "label": "Engineering Deep Dive",
    "score": 18
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

---

## 7.5 Anthropic Engineering

```json
{
  "id": "anthropic_engineering",
  "name": "Anthropic Engineering",
  "category": "technology",
  "adapter": "html_listing",
  "url": "https://www.anthropic.com/engineering",
  "quality": 47,
  "contentType": {
    "id": "engineering_deep_dive",
    "label": "Engineering Deep Dive",
    "score": 18
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

This adapter is source-specific.

It must parse only the Anthropic Engineering listing and must not become a generalized Anthropic crawler.

Admission:

```text
all valid Engineering publication entries
```

---

## 7.6 OpenAI Release Notes

```json
{
  "id": "openai_release_notes",
  "name": "OpenAI Release Notes",
  "category": "technology",
  "adapter": "rss",
  "url": "https://openai.com/products/release-notes/rss.xml",
  "quality": 46,
  "contentType": {
    "id": "official_release_notes",
    "label": "Official Release Notes",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 1,
  "admissionTopicIds": [
    "ai_ml",
    "software_architecture",
    "cybersecurity",
    "devops_sre"
  ],
  "forcedTags": []
}
```

OpenAI's release stream covers a wider product surface than the intended Technology reading feed.

Therefore V1 admits an OpenAI release-note entry only when it organically matches at least one approved Technology topic in:

```text
ai_ml
software_architecture
cybersecurity
devops_sre
```

Forced tags must not be used to satisfy this admission rule.

This filter is intended to reduce generic product/UI announcement noise while retaining technically relevant AI and platform material.

---

# 8. Literature Sources

## 8.1 The Paris Review

```json
{
  "id": "paris_review",
  "name": "The Paris Review",
  "category": "literature",
  "adapter": "rss",
  "url": "https://www.theparisreview.org/blog/feed/",
  "quality": 47,
  "contentType": {
    "id": "literary_essay",
    "label": "Literary Essay",
    "score": 15
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

---

## 8.2 Public Books

```json
{
  "id": "public_books",
  "name": "Public Books",
  "category": "literature",
  "adapter": "rss_autodiscovery",
  "url": "https://www.publicbooks.org/",
  "quality": 46,
  "contentType": {
    "id": "literary_essay",
    "label": "Literary Essay",
    "score": 15
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized primary publication entries
```

The adapter should ignore discovered podcast-specific feeds if multiple feed alternatives are present and prefer the site's primary article feed.

---

# 9. History Sources

## 9.1 JSTOR Daily

```json
{
  "id": "jstor_daily",
  "name": "JSTOR Daily",
  "category": "history",
  "adapter": "rss",
  "url": "https://daily.jstor.org/feed/",
  "quality": 47,
  "contentType": {
    "id": "historical_essay",
    "label": "Historical Essay",
    "score": 16
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

---

## 9.2 The Public Domain Review

```json
{
  "id": "public_domain_review",
  "name": "The Public Domain Review",
  "category": "history",
  "adapter": "rss_autodiscovery",
  "url": "https://publicdomainreview.org/",
  "quality": 46,
  "contentType": {
    "id": "historical_essay",
    "label": "Historical Essay",
    "score": 16
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all valid primary editorial entries
```

---

# 10. Weightlifting Sources

## 10.1 Stronger by Science

```json
{
  "id": "stronger_by_science",
  "name": "Stronger by Science",
  "category": "weightlifting",
  "adapter": "rss_autodiscovery",
  "url": "https://www.strongerbyscience.com/",
  "quality": 47,
  "contentType": {
    "id": "evidence_based_training",
    "label": "Evidence-Based Training",
    "score": 17
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized primary article entries
```

---

## 10.2 Barbell Medicine Training

```json
{
  "id": "barbell_medicine",
  "name": "Barbell Medicine",
  "category": "weightlifting",
  "adapter": "html_listing",
  "url": "https://www.barbellmedicine.com/articles/articles-training/",
  "quality": 46,
  "contentType": {
    "id": "evidence_based_training",
    "label": "Evidence-Based Training",
    "score": 17
  },
  "enabled": true,
  "minTopicMatches": 1,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

The training-specific listing is preferred to the publisher's broader medical article surface.

The source-specific adapter must remain constrained to the approved training listing.

Admission:

```text
at least 1 organically detected Weightlifting topic
```

This provides an additional safeguard against admitting unrelated medical material.

---

# 11. IAM Sources

## 11.1 IETF OAuth Working Group

```json
{
  "id": "ietf_oauth",
  "name": "IETF OAuth WG",
  "category": "iam",
  "adapter": "atom",
  "url": "https://datatracker.ietf.org/group/oauth/documents/feed/?significant=1",
  "quality": 50,
  "contentType": {
    "id": "standards_update",
    "label": "Standards Update",
    "score": 20
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": [
    "oauth"
  ]
}
```

Use the **Significant** document-change feed rather than the All Changes feed.

This reduces administrative and low-value document revision noise.

The forced `oauth` tag:

- appears as article taxonomy metadata;
- does not count toward admission;
- does not artificially increase organic topic-signal scoring.

---

## 11.2 OpenID Foundation Specs

```json
{
  "id": "openid_specs",
  "name": "OpenID Foundation",
  "category": "iam",
  "adapter": "rss",
  "url": "https://openid.net/category/specs/feed/",
  "quality": 50,
  "contentType": {
    "id": "standards_update",
    "label": "Standards Update",
    "score": 20
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized Specs-category entries
```

---

## 11.3 W3C Web Authentication Working Group

```json
{
  "id": "w3c_webauthn",
  "name": "W3C Web Authentication WG",
  "category": "iam",
  "adapter": "rss_autodiscovery",
  "url": "https://www.w3.org/groups/wg/webauthn/",
  "quality": 50,
  "contentType": {
    "id": "standards_update",
    "label": "Standards Update",
    "score": 20
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": [
    "passkeys_webauthn"
  ]
}
```

Use the feed advertised by the official working-group page.

The forced WebAuthn topic tag follows the same non-organic-scoring rules as other forced tags.

---

## 11.4 Okta Identity Engine API Releases

```json
{
  "id": "okta_identity_engine",
  "name": "Okta Identity Engine",
  "category": "iam",
  "adapter": "rss",
  "url": "https://developer.okta.com/rss/identity-engine.xml",
  "quality": 48,
  "contentType": {
    "id": "official_release_notes",
    "label": "Official Release Notes",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

Admission:

```text
all normalized feed entries
```

Do not force an `authentication` tag simply because the source is Okta.

Source identity and article topic are separate concepts.

---

# 12. Identity Automation Sources

## 12.1 Okta Workflows Production Releases

```json
{
  "id": "okta_workflows",
  "name": "Okta Workflows",
  "category": "identity_automation",
  "adapter": "html_listing",
  "url": "https://help.okta.com/wf/en-us/Content/Topics/ReleaseNotes/Workflows/production.htm",
  "quality": 49,
  "contentType": {
    "id": "official_release_notes",
    "label": "Official Release Notes",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

The adapter must parse only the official Production release-history page.

It must not crawl unrelated Okta Help content.

Admission:

```text
all distinct production-release entries
```

---

## 12.2 n8n Release Notes

```json
{
  "id": "n8n_release_notes",
  "name": "n8n Release Notes",
  "category": "identity_automation",
  "adapter": "rss_autodiscovery",
  "url": "https://docs.n8n.io/changelog/release-notes",
  "quality": 48,
  "contentType": {
    "id": "official_release_notes",
    "label": "Official Release Notes",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": []
}
```

The official documentation states that the same release-note entries are published as RSS.

V1 should discover the publisher-declared RSS endpoint from the canonical Release Notes page rather than depend on an unverified guessed URL.

Admission:

```text
all normalized release-note entries
```

V1 deliberately does **not** require an identity-specific topic match for n8n.

Learning how the automation platform itself evolves is considered useful within the Identity Automation category.

### Amendment 4 upstream condition

The canonical page currently returns HTTP 200 and advertises:

```text
https://docs.n8n.io/changelog/release-notes/rss.xml
```

That publisher-advertised feed currently returns HTTP 404, and the canonical page advertises no alternative RSS/Atom endpoint. This exact investigated condition is accepted as a publisher-side V1 limitation for the initial release.

While it persists, `n8n_release_notes` remains enabled and is reported as a failed source. Do not disable, remove, substitute, or change it to `html_listing` merely to make live validation green. If the feed recovers, normal ingestion should recover automatically. A materially different failure is not covered by Amendment 4.

---

## 12.3 Microsoft Entra Releases

```json
{
  "id": "entra_releases",
  "name": "Microsoft Entra",
  "category": "identity_automation",
  "adapter": "rss",
  "url": "https://learn.microsoft.com/api/search/rss?search=%22Release+notes+-+Azure+Active+Directory%22&locale=en-us",
  "quality": 47,
  "contentType": {
    "id": "official_release_notes",
    "label": "Official Release Notes",
    "score": 19
  },
  "enabled": true,
  "minTopicMatches": 1,
  "admissionTopicIds": [
    "provisioning",
    "scim",
    "lifecycle_jml",
    "workflow_orchestration",
    "access_requests",
    "identity_governance",
    "identity_sources",
    "agent_identity",
    "agent_automation",
    "connectors_integrations"
  ],
  "forcedTags": []
}
```

The Microsoft Entra release stream covers substantially more than identity-process automation.

Therefore V1 admits an Entra item only when it organically matches at least one approved admission topic.

Forced tags must not satisfy this filter.

---

## 12.4 IETF SCIM Working Group

```json
{
  "id": "ietf_scim",
  "name": "IETF SCIM WG",
  "category": "identity_automation",
  "adapter": "atom",
  "url": "https://datatracker.ietf.org/group/scim/documents/feed/?significant=1",
  "quality": 50,
  "contentType": {
    "id": "standards_update",
    "label": "Standards Update",
    "score": 20
  },
  "enabled": true,
  "minTopicMatches": 0,
  "admissionTopicIds": [],
  "forcedTags": [
    "scim"
  ]
}
```

Use the **Significant** document-change feed.

The forced `scim` tag does not count as an organic taxonomy match.

### Amendment 4 upstream condition

The approved endpoint currently returns HTTP 200 and valid Atom 1.0 with no parse error, but contains zero `<entry>` elements. This exact investigated condition is accepted as a publisher-side V1 limitation for the initial release.

While it persists, `ietf_scim` remains enabled and remains a failed source under the frozen empty-parse success semantics. Do not fabricate success or disable, remove, or substitute it. If entries return upstream, normal ingestion should recover automatically. A materially different failure is not covered by Amendment 4.

---

# 13. V1 Source Summary

| Category | Source Count |
|---|---:|
| Science | 2 |
| Technology | 6 |
| Literature | 2 |
| History | 2 |
| Weightlifting | 2 |
| IAM | 4 |
| Identity Automation | 4 |
| **Total** | **22** |

---

# 14. Admission Rule Summary

Most sources admit all valid normalized entries.

V1 source-specific admission filtering applies to exactly:

```text
openai_release_notes
barbell_medicine
entra_releases
```

### OpenAI Release Notes

Requires:

```text
≥1 approved technical Technology topic
```

### Barbell Medicine

Requires:

```text
≥1 Weightlifting topic
```

### Microsoft Entra

Requires:

```text
≥1 approved identity-automation admission topic
```

No other V1 source requires a minimum topic match.

---

# 15. Forced Tag Rules

Forced tags exist only where source context makes the topic inherent.

V1 forced tags are:

```text
ietf_oauth       → oauth
w3c_webauthn     → passkeys_webauthn
ietf_scim        → scim
```

Forced tags:

- appear in the final Article tag set;
- use the canonical taxonomy label;
- do not count toward `minTopicMatches`;
- do not increase organic `topicSignal`;
- remain available for browser personalization after the article is presented.

No vendor name is automatically converted into a topic.

For example:

```text
Okta source ≠ authentication topic
n8n source  ≠ workflow topic
OpenAI source ≠ AI/ML topic
```

Topic classification must remain content-driven unless explicitly forced above.

---

# 16. RSS Autodiscovery Rules

For `rss_autodiscovery`, the adapter:

1. retrieves the configured canonical HTML page;
2. inspects publisher-declared alternate-feed metadata;
3. considers only RSS/Atom alternatives;
4. prefers a feed semantically associated with the primary article surface;
5. rejects unrelated podcast, comment, and category feeds where a primary publication feed is available;
6. resolves relative URLs against the canonical page;
7. retrieves and parses the selected feed.

Autodiscovery is confined to the configured publisher page. It must not become general web discovery.

---

# 17. HTML Listing Adapter Rules

`html_listing` is source-specific, not a universal scraper.

Each implementation must:

- target the approved canonical listing;
- use selectors/structures covered by local fixtures;
- extract only article or release entries represented on that listing;
- produce canonical publisher URLs;
- fail safely when the expected structure changes;
- avoid following pagination or unrelated links unless explicitly specified.

The approved HTML-listing sources are:

```text
anthropic_engineering
barbell_medicine
okta_workflows
```

---

# 18. RSS and Atom Adapter Rules

RSS/Atom adapters normalize common entry fields without trusting embedded markup. Feed GUIDs may assist diagnostics or parsing but never replace the V1 canonical-URL identity rule.

Entries without a usable HTTP/HTTPS article URL are rejected under Contract Amendment 1.

---

# 19. Redirect and URL Boundaries

Adapters may follow ordinary HTTP redirects within configured fetch limits. A redirect does not authorize crawling the destination site. The emitted Article URL must still pass canonicalization and scheme validation.

---

# 20. Source Failure Behavior

A failed source is isolated. The pipeline:

- records the source ID and bounded failure code;
- does not fabricate replacement Articles;
- continues with other sources;
- reports success/failure counts to deployment sanity checks.

Publisher response bodies and arbitrary feed contents must not be dumped into logs.

---

# 21. Configuration Validation

Before any network retrieval, configuration validation must reject:

- duplicate source IDs;
- unknown categories, adapters, content types, or topic IDs;
- out-of-range quality/content-type scores;
- invalid URLs;
- contradictory admission settings;
- forced tags outside the source category;
- enabled sources not approved by this catalog.

---

# 22. Source-Catalog Completion Criteria

The source catalog is correctly implemented when all 22 entries are represented exactly once, the three filtered sources enforce their approved organic-topic admission rules, the three forced tags retain their non-scoring semantics, and every adapter is verified with local fixtures plus bounded live diagnostics.
