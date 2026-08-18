# Intentional Reading — V1 Taxonomy and Base Scoring Specification

**Status:** Approved for V1 foundation\
**Document:** `docs/v1/04-taxonomy-scoring.md`\
**Role:** Authoritative normalization, taxonomy, tagging, deduplication, admission, and deterministic base-scoring specification

---

## 1. Purpose

This document defines how raw publisher entries become normalized, tagged, admitted, deduplicated, and scored V1 Articles.

The entire process is deterministic.

V1 must not use:

- generative AI;
- embeddings;
- semantic-vector search;
- remote classification APIs;
- probabilistic AI-content detection;
- machine-learning recommendation models.

For identical:

```text
source configuration
topic configuration
raw input
generation time
```

the pipeline should produce identical normalized article metadata and base scores.

---

# 2. Processing Order

For each enabled source, processing occurs conceptually in this order:

```text
fetch source
    ↓
adapter extraction
    ↓
plain-text normalization
    ↓
URL canonicalization
    ↓
required-field validation
    ↓
stable article ID
    ↓
date normalization
    ↓
organic taxonomy matching
    ↓
source-specific admission filtering
    ↓
forced tag application
    ↓
compute metadata confidence needed for duplicate winner selection
    ↓
deduplication
    ↓
full base scoring
    ↓
dataset retention rules
    ↓
final dataset validation
    ↓
output
```

Metadata confidence is computed before deduplication because duplicate-winner selection requires it. This pre-dedup metadata calculation is not full base scoring.

The full Article base score is calculated after deduplication:

```text
base =
    sourceQuality
  + contentType
  + freshness
  + topicSignal
  + metadata
```

Retention and deployment thresholds are defined in:

```text
07-pipeline-deployment.md
```

---

# 3. Raw Source Entry

Adapters may extract source-specific raw metadata including:

```text
title
url
guid
author
published date
summary
description
content
```

Not every source supplies every field.

Adapters must not manufacture missing publisher metadata merely to satisfy the Article contract.

Normalization decides whether enough valid information exists to emit an Article.

---

# 4. Plain-Text Normalization

Remote text is untrusted.

Fields expected to become plain text must be normalized before output.

Applicable fields include:

```text
title
author
excerpt
source-provided text used for taxonomy matching
```

Normalization should conceptually perform:

1. decode HTML entities;
2. remove HTML elements;
3. remove script/style content if encountered;
4. Unicode-normalize;
5. collapse repeated whitespace;
6. trim leading/trailing whitespace;
7. preserve human-readable punctuation where useful.

No publisher HTML is emitted as trusted article markup.

---

# 5. Unicode Normalization

For matching and normalization, use:

```text
Unicode NFKC
```

before case normalization.

For case-insensitive taxonomy matching, use Unicode-aware case folding where practical.

The display text itself should preserve appropriate original capitalization after HTML/plain-text cleanup.

---

# 6. Title Rules

A normalized title must:

- be plain text;
- contain at least one non-whitespace character;
- not consist solely of punctuation;
- remain bounded against pathological source input.

Recommended V1 maximum:

```text
500 characters
```

If a normalized title exceeds the maximum, truncate safely and append:

```text
…
```

The full publisher article body must never be used as a title fallback.

An entry without a usable title is rejected.

---

# 7. Author Rules

Author may be:

```text
string
null
```

Normalize source-provided author metadata to plain text.

Recommended maximum:

```text
200 characters
```

Do not synthesize:

```text
Editorial Staff
Unknown
IETF
Okta
```

when the source did not provide an author.

Publisher/source identity is already represented separately.

---

# 8. Excerpt Rules

The excerpt is derived only from publisher-supplied feed/listing summary material.

Preferred input order:

```text
explicit summary
    ↓
description
    ↓
short source-provided content representation
    ↓
empty string
```

Do not generate excerpts with AI.

Do not scrape the full article merely to produce an excerpt.

The normalized excerpt:

- must be plain text;
- may be empty;
- should collapse whitespace;
- should remove obvious feed boilerplate when safely identifiable;
- should be bounded.

Recommended maximum:

```text
800 characters
```

If truncation is necessary:

1. prefer a sentence boundary before the limit;
2. otherwise truncate on a word boundary;
3. append `…`.

---

# 9. URL Requirement

Per **Contract Amendment 1**, every emitted V1 Article must have a usable external URL.

Allowed schemes:

```text
http
https
```

A URL is unusable when, after parsing, it:

- uses another scheme;
- has no hostname;
- is syntactically unusable;
- resolves to an empty value after normalization.

If no usable HTTP/HTTPS URL exists:

```text
REJECT SOURCE ENTRY
```

No GUID-, title-, or date-derived fallback Article is emitted.

---

# 10. URL Canonicalization

Canonicalization should be conservative.

The goal is stable identity and tracking removal without changing article meaning.

Perform:

### Scheme

Lowercase:

```text
HTTPS → https
HTTP  → http
```

### Hostname

Lowercase hostname.

Example:

```text
EXAMPLE.COM → example.com
```

Do not automatically remove:

```text
www.
```

because hostnames remain publisher-controlled identity.

### Default ports

Remove:

```text
http:80
https:443
```

Preserve non-default ports if otherwise valid.

### Fragment

Remove the entire fragment:

```text
#comments
#section
```

### Tracking parameters

Remove recognized tracking parameters.

V1 must remove parameter names matching:

```text
utm_*
fbclid
gclid
dclid
msclkid
mc_cid
mc_eid
igshid
vero_id
oly_anon_id
oly_enc_id
```

Matching of parameter names is case-insensitive.

Do **not** remove generic query parameters such as:

```text
id
page
article
version
lang
ref
```

unless explicitly added to the approved tracking list.

### Remaining query parameters

Preserve all remaining parameters and values.

Sort remaining parameter pairs deterministically by:

```text
parameter name
then value
```

to stabilize equivalent URLs.

### Path

Preserve the publisher path.

Do not:

- lowercase arbitrary paths;
- collapse semantically meaningful path segments;
- decode/re-encode path content unnecessarily.

A trailing slash on a non-root path may be removed for canonical identity.

Root:

```text
/
```

must remain valid.

---

# 11. Stable Article ID

After URL canonicalization:

```text
canonical URL
    ↓
UTF-8 bytes
    ↓
SHA-256
    ↓
first 20 lowercase hexadecimal characters
```

Example shape:

```text
60bca89ea70f36ddc822
```

The Article ID is derived only from the canonical URL in V1.

The same canonical URL must always produce the same ID.

---

# 12. Publication Date Normalization

If a source provides a parseable publication timestamp:

1. parse source timezone when present;
2. convert to UTC;
3. serialize as ISO-8601 UTC.

Preferred output:

```text
2026-08-16T18:30:00Z
```

If the date is missing or not reliably parseable:

```json
"publishedAt": null
```

Do not substitute:

- fetch time;
- build time;
- first-seen time.

---

# 13. Future Publication Dates

Minor future skew may occur because of publisher timezone or clock differences.

If:

```text
publishedAt <= generatedAt + 6 hours
```

treat negative age as:

```text
0 hours
```

for freshness scoring.

If a publication time is more than six hours in the future:

```text
publishedAt = null
```

for normalized/scoring purposes and log a source-entry warning.

The pipeline must not assign maximum freshness to implausibly future-dated entries.

---

# 14. Reading-Time Estimation

V1 does not scrape full article bodies to calculate reading time.

`readingTimeMinutes` may be populated only when the source itself provides a sufficiently substantial text representation.

Minimum normalized word count:

```text
400 words
```

If at least 400 words of legitimate source-provided article content are available during ingestion:

```text
readingTimeMinutes =
    ceil(wordCount / 225)
```

Minimum emitted value:

```text
2
```

The full source-provided body used for this calculation does not need to be retained in `articles.json`.

If insufficient text is available:

```json
"readingTimeMinutes": null
```

Do not estimate reading time from title plus short excerpt.

---

# 15. Matching Text

Taxonomy matching uses only:

```text
normalized title
+
normalized excerpt
```

Full remote article bodies are not required for taxonomy classification.

Title and excerpt are evaluated separately because title evidence receives greater weight.

---

# 16. Taxonomy-Matching Normalization

Taxonomy matching uses a separate comparison representation.

For both article text and aliases:

1. Unicode NFKC;
2. case fold;
3. convert punctuation/separators to spaces;
4. collapse whitespace;
5. trim.

Examples:

```text
OAuth 2.1
→ oauth 2 1

FIDO-2
→ fido 2

AI/ML
→ ai ml
```

This permits deterministic token-sequence comparison while avoiding unsafe raw substring matching.

---

# 17. Whole-Token / Whole-Phrase Matching

Aliases must match complete normalized token sequences.

Example alias:

```text
ai
```

matches:

```text
AI agents
```

but must not match:

```text
mail
chair
said
```

Alias:

```text
programming
```

matches the token:

```text
programming
```

but not arbitrary substrings.

Multi-token aliases match contiguous normalized token sequences.

---

# 18. Category Scope

Only topics whose configured:

```text
categories
```

contain the article's configured category are eligible for matching.

Example:

Weightlifting topic:

```text
programming
```

must not tag an ACM Queue article merely because the technology article uses the word "programming."

Category scoping is mandatory.

---

# 19. Organic Topic Evidence

For each eligible topic:

### Title match

If one or more aliases for that topic match the title:

```text
+3 evidence
```

Title evidence is applied at most once per topic.

Multiple synonymous aliases in the same title do not stack.

### Excerpt match

If one or more aliases match the excerpt:

```text
+1 evidence
```

Excerpt evidence is applied at most once per topic.

Therefore one topic has organic evidence of:

```text
0
1
3
4
```

A topic with:

```text
0
```

is not organically detected.

---

# 20. Organic Tag Ordering

Organically detected topics are ordered by:

1. evidence descending;
2. title-match presence;
3. topic label alphabetically;
4. topic ID as final deterministic tie-break.

Retain at most:

```text
5 organically detected topics
```

in the Article contract.

---

# 21. Topic Signal Score

The article's `topicSignal` component is:

```text
sum of organic evidence across ALL organically detected eligible topics
```

before the five-tag output limit.

Clamp:

```text
0–10
```

Examples:

### One title + excerpt match

```text
topic A = 4

topicSignal = 4
```

### Two title matches

```text
topic A = 3
topic B = 3

topicSignal = 6
```

### Three strong topics

```text
4 + 4 + 3 = 11

topicSignal = 10
```

Forced tags never contribute to `topicSignal`.

---

# 22. Admission-Match Counting

Source admission filtering uses **distinct organically detected topic IDs**, not evidence points.

Example source:

```json
{
  "minTopicMatches": 1,
  "admissionTopicIds": [
    "ai_ml",
    "software_architecture"
  ]
}
```

An article organically matching:

```text
ai_ml
```

has:

```text
admission match count = 1
```

even if `ai_ml` matched both title and excerpt.

---

# 23. Admission Topic Scope

When:

```text
admissionTopicIds = []
```

and:

```text
minTopicMatches > 0
```

all organically detected topics valid for the source category may satisfy admission.

When `admissionTopicIds` contains values:

only organically detected topics in that allowlist count.

Forced tags never count toward admission.

---

# 24. V1 Filtered Sources

Per `03-content-sources.md`, V1 applies topic admission filtering to exactly:

```text
barbell_medicine
entra_releases
```

### Barbell Medicine

Require at least one organic Weightlifting topic.

### Microsoft Entra Releases

Require at least one organic match from the approved identity-automation admission allowlist defined in `03-content-sources.md`.

Under Amendment 5, `openai_release_notes` is deferred to V2 and has no active V1 admission rule.

---

# 25. Forced Tags

After admission filtering, apply configured forced tags.

V1:

```text
ietf_oauth
    → oauth

w3c_webauthn
    → passkeys_webauthn

ietf_scim
    → scim
```

If a forced topic was already organically detected:

- retain one tag object;
- preserve its organic ranking position;
- do not duplicate it.

If it was not organic:

- append it after organic tags;
- use its canonical taxonomy label.

Because the contract limits **organic** tags rather than total tags, a forced tag may cause the Article's internal `tags` array to contain more than five entries.

Normal Discover presentation still displays no more than five.

---

# 26. Topic Alias Validation

At configuration-load time:

- topic IDs must be unique;
- labels must be non-empty;
- aliases must be non-empty after matching normalization;
- category IDs must be valid;
- aliases within the same topic must not duplicate after normalization.

Within a single category, identical normalized aliases assigned to multiple topics should be treated as a configuration error unless explicitly approved by a future specification revision.

This prevents ambiguous deterministic classification.

The same alias may exist in different categories where category scoping prevents ambiguity.

---

# 27. Science Taxonomy

## `physics_quantum`

**Label:** Physics & Quantum

**Categories:** `science`

Aliases:

```text
physics
quantum
quantum physics
quantum mechanics
particle physics
particle physicist
condensed matter
quantum field theory
```

---

## `space_cosmology`

**Label:** Astronomy & Cosmology

**Categories:** `science`

Aliases:

```text
astronomy
astronomer
astrophysics
astrophysicist
cosmology
cosmologist
black hole
black holes
exoplanet
exoplanets
galaxy
galaxies
neutron star
neutron stars
```

---

## `mathematics`

**Label:** Mathematics

**Categories:** `science`

Aliases:

```text
mathematics
mathematician
mathematicians
theorem
topology
geometry
number theory
combinatorics
algebra
mathematical proof
```

---

## `biology_evolution`

**Label:** Biology & Evolution

**Categories:** `science`

Aliases:

```text
biology
biologist
evolution
evolutionary
genetics
genetic
genome
genomics
ecology
microbiology
microbe
microbes
```

---

## `neuroscience`

**Label:** Neuroscience & Cognition

**Categories:** `science`

Aliases:

```text
neuroscience
neuroscientist
neuron
neurons
cognition
cognitive science
brain science
```

---

## `earth_climate`

**Label:** Earth & Climate

**Categories:** `science`

Aliases:

```text
climate science
climate scientist
geology
geologist
oceanography
oceanographer
atmosphere
atmospheric science
earth science
paleoclimate
```

---

## `scientific_method`

**Label:** Scientific Method

**Categories:** `science`

Aliases:

```text
reproducibility
replication
peer review
scientific method
research methodology
meta analysis
systematic review
```

---

# 28. Technology Taxonomy

## `software_architecture`

**Label:** Software Architecture

**Categories:** `technology`

Aliases:

```text
software architecture
system architecture
software design
architectural pattern
codebase architecture
application architecture
```

---

## `distributed_systems`

**Label:** Distributed Systems

**Categories:** `technology`

Aliases:

```text
distributed system
distributed systems
consensus algorithm
replication
distributed database
distributed computing
raft consensus
paxos
```

---

## `cloud_infrastructure`

**Label:** Cloud & Infrastructure

**Categories:** `technology`

Aliases:

```text
cloud infrastructure
cloud computing
serverless
edge computing
cloud platform
infrastructure as code
```

---

## `networking`

**Label:** Networking

**Categories:** `technology`

Aliases:

```text
networking
network protocol
network protocols
dns
bgp
tcp
quic
http 2
http 3
routing protocol
```

---

## `cybersecurity`

**Label:** Cybersecurity

**Categories:** `technology`

Aliases:

```text
cybersecurity
cyber security
vulnerability
vulnerabilities
exploit
exploits
cryptography
cryptographic
zero trust
security architecture
supply chain attack
```

---

## `ai_ml`

**Label:** AI & Machine Learning

**Categories:** `technology`

Aliases:

```text
ai
artificial intelligence
machine learning
ml
large language model
large language models
llm
llms
neural network
neural networks
foundation model
foundation models
generative ai
```

Because matching is whole-token based, the aliases `ai`, `ml`, and `llm` must not behave as substring matches.

---

## `programming_languages`

**Label:** Languages & Runtimes

**Categories:** `technology`

Aliases:

```text
programming language
programming languages
compiler
compilers
runtime
runtimes
webassembly
wasm
garbage collector
garbage collection
```

---

## `data_systems`

**Label:** Databases & Data Systems

**Categories:** `technology`

Aliases:

```text
database
databases
data storage
query engine
query engines
sql
data platform
data platforms
database engine
```

---

## `devops_sre`

**Label:** DevOps & SRE

**Categories:** `technology`

Aliases:

```text
devops
site reliability
site reliability engineering
sre
observability
incident response
production incident
production outage
reliability engineering
```

---

## `hardware`

**Label:** Hardware & Semiconductors

**Categories:** `technology`

Aliases:

```text
semiconductor
semiconductors
processor
processors
cpu
gpu
chip architecture
microarchitecture
integrated circuit
```

---

# 29. Literature Taxonomy

## `fiction`

**Label:** Fiction

**Categories:** `literature`

Aliases:

```text
fiction
novel
novels
novelist
novelists
short story
short stories
```

---

## `poetry`

**Label:** Poetry

**Categories:** `literature`

Aliases:

```text
poetry
poem
poems
poet
poets
```

---

## `literary_criticism`

**Label:** Literary Criticism

**Categories:** `literature`

Aliases:

```text
literary criticism
literary critic
literary critics
close reading
critical essay
```

---

## `writing_craft`

**Label:** Writing Craft

**Categories:** `literature`

Aliases:

```text
writing craft
narrative technique
prose style
storytelling craft
craft of writing
narrative voice
```

---

## `author_interviews`

**Label:** Author Interviews

**Categories:** `literature`

Aliases:

```text
author interview
author interviews
writer interview
writer interviews
writers at work
conversation with
```

---

## `translation`

**Label:** Translation

**Categories:** `literature`

Aliases:

```text
literary translation
translated literature
translator
translators
translation
```

---

## `literary_history`

**Label:** Literary History

**Categories:** `literature`

Aliases:

```text
literary history
literary movement
literary movements
literary tradition
literary traditions
history of literature
```

---

# 30. History Taxonomy

## `ancient_history`

**Label:** Ancient History

**Categories:** `history`

Aliases:

```text
ancient history
antiquity
ancient rome
roman empire
roman republic
ancient greece
mesopotamia
ancient egypt
ancient world
```

---

## `medieval_history`

**Label:** Medieval History

**Categories:** `history`

Aliases:

```text
medieval
medieval history
middle ages
middle age
```

---

## `early_modern_history`

**Label:** Early Modern

**Categories:** `history`

Aliases:

```text
early modern
renaissance
reformation
sixteenth century
seventeenth century
```

---

## `modern_history`

**Label:** Modern History

**Categories:** `history`

Aliases:

```text
modern history
nineteenth century
twentieth century
19th century
20th century
```

---

## `archaeology`

**Label:** Archaeology

**Categories:** `history`

Aliases:

```text
archaeology
archaeological
archaeologist
archaeologists
excavation
excavations
```

---

## `social_cultural_history`

**Label:** Social & Cultural History

**Categories:** `history`

Aliases:

```text
social history
cultural history
history of everyday life
everyday life
material culture
```

---

## `science_technology_history`

**Label:** History of Science & Technology

**Categories:** `history`

Aliases:

```text
history of science
history of technology
industrial revolution
scientific revolution
technological history
```

---

## `economic_history`

**Label:** Economic & Trade History

**Categories:** `history`

Aliases:

```text
economic history
trade history
history of trade
commerce
labor history
labour history
history of labor
history of labour
```

---

## `military_history`

**Label:** Military History

**Categories:** `history`

Aliases:

```text
military history
warfare
battlefield
battlefields
history of war
military campaign
military campaigns
```

---

## `primary_sources`

**Label:** Archives & Primary Sources

**Categories:** `history`

Aliases:

```text
primary source
primary sources
archive
archives
archival
manuscript
manuscripts
historical letters
historical diary
historical diaries
```

---

# 31. Weightlifting Taxonomy

## `strength`

**Label:** Strength

**Categories:** `weightlifting`

Aliases:

```text
strength training
maximal strength
maximum strength
one rep max
1rm
strength adaptation
```

---

## `hypertrophy`

**Label:** Hypertrophy

**Categories:** `weightlifting`

Aliases:

```text
hypertrophy
muscle growth
muscle mass
muscular hypertrophy
```

---

## `programming`

**Label:** Training Programming

**Categories:** `weightlifting`

Aliases:

```text
periodization
training volume
training frequency
training intensity
training program
training programming
programming
```

This topic is scoped only to Weightlifting.

---

## `technique`

**Label:** Technique

**Categories:** `weightlifting`

Aliases:

```text
lifting technique
exercise technique
range of motion
lifting form
exercise form
bar path
```

---

## `powerlifting`

**Label:** Powerlifting

**Categories:** `weightlifting`

Aliases:

```text
powerlifting
powerlifter
powerlifters
squat
bench press
deadlift
```

---

## `olympic_weightlifting`

**Label:** Olympic Weightlifting

**Categories:** `weightlifting`

Aliases:

```text
olympic weightlifting
weightlifting
snatch
clean and jerk
clean & jerk
weightlifter
```

Because the article category is already Weightlifting, the generic token `weightlifting` is acceptable within this scoped topic.

---

## `recovery`

**Label:** Recovery & Fatigue

**Categories:** `weightlifting`

Aliases:

```text
recovery
fatigue management
training fatigue
detraining
deload
deloading
```

---

## `nutrition`

**Label:** Sports Nutrition

**Categories:** `weightlifting`

Aliases:

```text
sports nutrition
protein intake
energy balance
calorie intake
caloric intake
nutrition for strength
nutrition for hypertrophy
```

---

## `injury_rehab`

**Label:** Injury & Rehab

**Categories:** `weightlifting`

Aliases:

```text
injury management
injury rehabilitation
rehabilitation
rehab
tendinopathy
return to training
pain management
```

---

## `conditioning`

**Label:** Conditioning

**Categories:** `weightlifting`

Aliases:

```text
conditioning
aerobic training
cardiovascular training
cardio
work capacity
aerobic fitness
```

---

## `research_methods`

**Label:** Exercise Science Research

**Categories:** `weightlifting`

Aliases:

```text
systematic review
meta analysis
randomized trial
randomised trial
randomized controlled trial
randomised controlled trial
effect size
exercise science research
```

---

# 32. IAM Taxonomy

## `authentication`

**Label:** Authentication

**Categories:** `iam`

Aliases:

```text
authentication
authenticator
authenticators
sign in
signin
user authentication
```

---

## `authorization`

**Label:** Authorization

**Categories:** `iam`

Aliases:

```text
authorization
authorisation
access control
policy enforcement
authorization policy
permission model
```

---

## `oauth`

**Label:** OAuth

**Categories:** `iam`

Aliases:

```text
oauth
oauth 2
oauth 2 0
oauth 2 1
pkce
dpop
token exchange
authorization code flow
client credentials
```

---

## `oidc`

**Label:** OpenID Connect

**Categories:** `iam`

Aliases:

```text
oidc
openid connect
id token
id tokens
userinfo endpoint
```

---

## `saml`

**Label:** SAML

**Categories:** `iam`

Aliases:

```text
saml
security assertion markup language
saml 2
saml 2 0
```

---

## `passkeys_webauthn`

**Label:** Passkeys / WebAuthn

**Categories:** `iam`

Aliases:

```text
passkey
passkeys
webauthn
web authentication
fido2
fido 2
phishing resistant authentication
```

---

## `mfa`

**Label:** MFA

**Categories:** `iam`

Aliases:

```text
mfa
2fa
multi factor authentication
multifactor authentication
two factor authentication
two factor
```

---

## `federation`

**Label:** Federation

**Categories:** `iam`

Aliases:

```text
identity federation
federated identity
federation
identity provider
relying party
federation protocol
```

---

## `identity_proofing`

**Label:** Identity Proofing

**Categories:** `iam`

Aliases:

```text
identity proofing
identity verification
identity assurance
proofing
identity evidence
```

---

## `identity_governance`

**Label:** Identity Governance

**Categories:** `iam`, `identity_automation`

Aliases:

```text
identity governance
iga
entitlement management
access certification
access certifications
access review
access reviews
entitlement review
entitlement reviews
```

This topic intentionally participates in both IAM and Identity Automation.

---

## `privileged_access`

**Label:** Privileged Access

**Categories:** `iam`

Aliases:

```text
privileged access
pam
privileged access management
just in time access
jit access
privileged account
privileged accounts
```

---

## `identity_security`

**Label:** Identity Security

**Categories:** `iam`

Aliases:

```text
identity security
identity threat
identity threats
credential attack
credential attacks
compromised credential
compromised credentials
credential theft
device assurance
```

---

## `workload_identity`

**Label:** Workload & Machine Identity

**Categories:** `iam`

Aliases:

```text
workload identity
workload identities
machine identity
machine identities
service account
service accounts
non human identity
non human identities
nhi
```

---

## `agent_identity`

**Label:** Agent Identity

**Categories:** `iam`, `identity_automation`

Aliases:

```text
agent identity
agent identities
ai agent identity
ai agent identities
agent authorization
agent authorisation
agent delegation
agent credential
agent credentials
```

---

## `tokens_jwt`

**Label:** Tokens & JWT

**Categories:** `iam`

Aliases:

```text
jwt
json web token
json web tokens
access token
access tokens
refresh token
refresh tokens
security event token
security event tokens
```

---

# 33. Identity Automation Taxonomy

## `provisioning`

**Label:** Provisioning

**Categories:** `identity_automation`

Aliases:

```text
provisioning
deprovisioning
account provisioning
account deprovisioning
user provisioning
user deprovisioning
```

---

## `scim`

**Label:** SCIM

**Categories:** `iam`, `identity_automation`

Aliases:

```text
scim
system for cross domain identity management
scim protocol
scim provisioning
```

---

## `lifecycle_jml`

**Label:** Joiner / Mover / Leaver

**Categories:** `identity_automation`

Aliases:

```text
joiner mover leaver
joiner
mover
leaver
jml
identity lifecycle
user lifecycle
employee lifecycle
lifecycle management
```

---

## `workflow_orchestration`

**Label:** Workflow Orchestration

**Categories:** `identity_automation`

Aliases:

```text
workflow
workflows
workflow orchestration
orchestration
identity orchestration
automation workflow
automation workflows
```

---

## `api_automation`

**Label:** API Automation

**Categories:** `identity_automation`

Aliases:

```text
api automation
rest api
api request
api requests
api workflow
api workflows
http request
http requests
```

---

## `connectors_integrations`

**Label:** Connectors & Integrations

**Categories:** `identity_automation`

Aliases:

```text
connector
connectors
integration
integrations
app integration
app integrations
integration builder
```

---

## `event_driven`

**Label:** Event-Driven Automation

**Categories:** `identity_automation`

Aliases:

```text
event driven
event driven automation
webhook
webhooks
event hook
event hooks
event trigger
event triggers
```

---

## `access_requests`

**Label:** Access Requests & Approval

**Categories:** `identity_automation`

Aliases:

```text
access request
access requests
entitlement request
entitlement requests
approval workflow
approval workflows
access approval
access approvals
```

---

## `data_transformation`

**Label:** Data Transformation

**Categories:** `identity_automation`

Aliases:

```text
data mapping
data transformation
data transformations
json transformation
json transformations
field mapping
attribute mapping
```

---

## `observability_audit`

**Label:** Automation Observability

**Categories:** `identity_automation`

Aliases:

```text
execution history
workflow history
audit log
audit logs
system log
system logs
telemetry
workflow telemetry
execution log
execution logs
```

---

## `identity_sources`

**Label:** Identity Sources

**Categories:** `identity_automation`

Aliases:

```text
identity source
identity sources
source of truth
authoritative source
hris
hr source
human resources system
```

---

## `agent_automation`

**Label:** Agent Automation

**Categories:** `identity_automation`

Aliases:

```text
ai agent
ai agents
agent workflow
agent workflows
agent orchestration
mcp
model context protocol
agent automation
```

---

## Cross-Category V1 Topics

The following V1 topics intentionally span categories:

```text
identity_governance
    → iam
    → identity_automation

agent_identity
    → iam
    → identity_automation

scim
    → iam
    → identity_automation
```

Their topic IDs and labels remain identical across both contexts.

Do not duplicate them as separate category-specific topic IDs.

---

# 34. Deduplication

Deduplication is deterministic.

## Exact canonical-URL deduplication

Articles sharing a canonical URL are duplicates.

## Same-source near-duplicate detection

For Articles from the same source, compare normalized titles using Python standard-library `difflib.SequenceMatcher` or an equivalent deterministic implementation.

When both Articles have valid publication dates, they are duplicates only when:

```text
normalized title similarity >= 0.92
AND
publication dates are within 14 days
```

If either publication date is unavailable, the same-source Articles are duplicates only when:

```text
normalized title similarity >= 0.97
```

## Cross-source near-duplicate detection

Articles from different sources are duplicates only when:

```text
normalized titles are exactly equal
AND
both valid publication timestamps exist
AND
timestamps are within 72 hours
```

If either publication date is unavailable, do not perform cross-source title-only deduplication.

## Duplicate winner

When a duplicate group exists, retain in order of:

1. higher configured source quality;
2. higher metadata score;
3. newer valid publication date;
4. richer non-empty excerpt;
5. deterministic source/article-ID tie-break.

Content-type score is not a duplicate-winner criterion.

---

# 35. Source-Quality Score

`sourceQuality` is copied from the approved source configuration and must be an integer from `0` through `50`.

---

# 36. Content-Type Score

`contentType` is copied from the approved content-type configuration and must be an integer from `0` through `20`.

---

# 37. Freshness Score

Age is calculated from `generatedAt` and normalized `publishedAt`.

```text
<= 1 day        15
<= 3 days       13
<= 7 days       10
<= 14 days       7
<= 30 days       4
> 30 days        1
unknown          5
```

Freshness is deliberately bounded so newness cannot overpower source quality.

---

# 38. Metadata Score

Metadata confidence is deterministic and capped at five points. V1 awards:

```text
valid non-null publication date  +2
excerpt >= 80 characters         +2
excerpt 1–79 characters          +1
excerpt 0 characters             +0
non-empty author                 +1
readingTimeMinutes               +0
```

Missing optional metadata is not an error and never receives fabricated replacements.

---

# 39. Base Score

```text
base =
    sourceQuality
  + contentType
  + freshness
  + topicSignal
  + metadata
```

Component ranges:

```text
sourceQuality  0–50
contentType    0–20
freshness      0–15
topicSignal    0–10
metadata       0–5
base           0–100
```

All components are stored so ranking decisions remain inspectable.

---

# 40. Output Ordering

The generated dataset is ordered by:

1. base score descending;
2. publication time descending, with unknown last;
3. source ID;
4. Article ID.

Browser personalization may reorder eligible Articles without mutating these generated values.

---

# 41. Taxonomy and Scoring Completion Criteria

The implementation is complete when canonicalization and IDs satisfy Contract Amendment 1, all topic aliases validate without same-category ambiguity, admission and forced-tag semantics are distinct, duplicate selection is deterministic, and every emitted score equals the sum of its stored components.
