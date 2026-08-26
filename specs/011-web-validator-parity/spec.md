# 011 — Web validator parity and shared copy

**Status:** draft (awaiting plan gate)\
**Workstream role:** web runtime + one Android string (see `design.md` §Workstream role)\
**Authority:** `docs/v1/contracts.md` §§6/7/13, `docs/v1/07-pipeline-deployment.md`,
`docs/v1/09-testing-acceptance.md`\
**Wave:** A (`specs/waves/wave-a.md`) · **Branch:** `feat/011-web-validator-parity` → `main`

---

## 1. Problem

Three defects on the browser side, all found by Android ports reading the source they were porting, and
none of them Android's to fix. Item 002 recorded them as outstanding rather than reaching across the
tree to correct them, which was right; this item is where they get corrected.

Two are validator gaps, one is a grammar error in shared copy.

### 1.1 The client validator is looser than the pipeline that feeds it

`js/data/validation.js` is the browser's trust boundary: it is what stands between a fetched
`data/articles.json` and the application state. In two places it admits documents the pipeline's own
validator would refuse.

| Field | `js/data/validation.js` | `pipeline/validation.py` |
|---|---|---|
| `readingTimeMinutes` | `>= 1` (`:145`) | `>= 2` (`:82`) |
| `tags` length | no limit (`:148-163`) | `<= 6` (`:95`) |

A client validator being *looser* than the producer is backwards. The producer's constraints are the
ones that describe the data; the client's job is to refuse anything that does not look like what the
producer emits, precisely because the client cannot assume the file it fetched came from the producer.

### 1.2 The tag limit is six, not five — the backlog and the wave brief both say five, and both are wrong

**This is the one place where the brief must not be implemented as written.** `specs/backlog.md` §011
and `specs/waves/wave-a.md` §011 both cite `contracts.md` §7's *"organically detected tags are limited
to five"* and infer a five-tag cap for the client validator. The inference drops a word.

`contracts.md` §7 limits **organically detected** tags to five, and the pipeline implements exactly
that: `pipeline/taxonomy.py:38-40` takes `matches[:5]`. But `apply_forced_tags`
(`pipeline/taxonomy.py:58-70`) then **appends** the source's configured forced tags on top of the
organic five. Five organic plus one forced is six, and that is why `pipeline/validation.py:95` caps the
total at six rather than five.

The ceiling is structural, not incidental. `pipeline/configuration.py:183-185` freezes the forced-tag
catalog against a constant — `config/sources.json` today has exactly three sources carrying exactly one
forced tag each, and seventeen carrying none, so no source can contribute a second forced tag without a
configuration change the loader would reject.

**A five-tag cap in the client validator would reject legitimate datasets.** It would replace a
too-loose validator with a wrong one, which is worse. The client cap is **six**, matching
`pipeline/validation.py:95`.

### 1.3 Both validator gaps are defence-in-depth, not live bugs

The brief asks this to be checked rather than assumed, so it was, against the dataset the pipeline
actually published — 205 articles, `generatedAt` `2026-08-26T01:57:30Z`:

| Check | Observed |
|---|---|
| `readingTimeMinutes` non-null | 44 of 205; **minimum 2**; zero instances of 1 |
| `tags` length | maximum **3**; distribution 0:120, 1:62, 2:21, 3:2 |

And neither value is reachable by construction, not merely absent today.
`pipeline/normalize.py:171` returns `max(2, ceil(word_count / READING_WORDS_PER_MINUTE))` and returns
`None` below the word-count floor, so 1 cannot be produced. The tag ceiling is the five-plus-forced
arithmetic above.

**So both are defence-in-depth: they harden the trust boundary against a dataset that did not come from
this pipeline.** That is a real thing to fix — the whole point of a client-side validator is the case
where the file is not what it claims to be — but it is not a defect a reader is experiencing today, and
the scenarios below are written as validator-contract scenarios rather than as bug reproductions.

### 1.4 The copy is ungrammatical in both clients

`js/ui/discover.js:330` renders `"1 more choice wait quietly behind this one."` — singular noun, plural
verb. The plural branch of the same expression is correct: `"4 more choices wait quietly behind this
one."`

Item 002 ported the string verbatim to `android/…/ui/format/Labels.kt:46` rather than silently
correcting it, which was the right call — a port is not the place to invent copy. That decision is what
makes this a two-client fix rather than an Android one.

**Corrected string, settled with the owner 2026-08-25:**

```text
1 more choice waits quietly behind this one.
```

Verb only. The digit, the adverb, and the rest of the sentence are unchanged, and the plural branch is
untouched.

**Both clients change in this item, or neither does.** A unilateral correction on one side invents a
requirement, which is exactly what 002 refused to do.

## 2. Story

As a reader, I want the browser to refuse a dataset the pipeline could not have produced, and to write
the sentence about my remaining choices in correct English, so that the client I trust with my reading
is as careful as the pipeline that fills it.

## 3. Out of scope

- **`pipeline/**` and `config/**`.** The pipeline is the correct one of the two and is not touched. If
  the implementer believes the pipeline is wrong, that is a report to the supervisor.
- **Widening the client validator's tag checking beyond length.** The pipeline also verifies each tag
  id against `config/topics.json`, checks the label against the taxonomy entry, and checks the topic
  against the article's category (`pipeline/validation.py:87-93`). The browser does none of that and
  this item does not add it — the client has no taxonomy to check against, and inventing one is a
  different item. Duplicate-id rejection already exists (`js/data/validation.js:150-153`) and is
  unchanged.
- **Any other divergence between the two validators.** Only the two the ports found are in scope.
  A survey is a separate item.
- **The plural branch of the remaining-choices copy**, which is already correct.
- **Any change to `contracts.md` or `docs/v1/**`.** §7 is being read correctly, not amended.
- **Any Android change beyond the one string and its assertion.** No behaviour, no layout, no logic.
- **New dependencies** in either tree.

## 4. Scenarios

### Scenario: a reading time of one minute is refused

Given a dataset whose article carries `readingTimeMinutes` of 1\
When the browser validates it\
Then the dataset is refused\
And the failure names the `readingTimeMinutes` field

### Scenario: the smallest reading time the pipeline can emit is accepted

Given a dataset whose article carries `readingTimeMinutes` of 2\
When the browser validates it\
Then the dataset is accepted\
And the value is carried through unchanged

### Scenario: an absent reading time is still accepted

Given a dataset whose article carries `readingTimeMinutes` of `null`\
When the browser validates it\
Then the dataset is accepted

### Scenario: a seventh tag is refused

Given a dataset whose article carries seven tags with distinct ids\
When the browser validates it\
Then the dataset is refused\
And the failure names the `tags` field

### Scenario: six tags are accepted, because the pipeline can emit six

Given a dataset whose article carries six tags with distinct ids\
When the browser validates it\
Then the dataset is accepted\
And all six tags are carried through in order

### Scenario: an article with no tags is still accepted

Given a dataset whose article carries an empty `tags` array\
When the browser validates it\
Then the dataset is accepted

### Scenario: duplicate tag ids are still refused

Given a dataset whose article carries two tags with the same id\
When the browser validates it\
Then the dataset is refused\
And the failure names the duplicated tag

### Scenario: the browser writes the singular sentence in correct English

Given Discover is showing an article with exactly one further choice behind it\
When the side note is rendered\
Then it reads `1 more choice waits quietly behind this one.`

### Scenario: the plural sentence is unchanged

Given Discover is showing an article with more than one further choice behind it\
When the side note is rendered\
Then it reads `<n> more choices wait quietly behind this one.`

### Scenario: the Android client says exactly what the browser says

Given the Android singular remaining-choices label\
When it is compared to the browser's singular string\
Then the two are character for character identical\
And the plural labels are likewise identical

## 5. Verification

This item touches both trees, so **both** CI workflows fire and both must be green on the exact final
head before the final review merges (`execution-model.md` §8).

Web and pipeline gates:

```sh
npm test
python -m pytest
python -m pipeline.main --validate-config
```

`python -m pytest` and `--validate-config` are run not because this item changes the pipeline — it does
not — but because they are the proof that it did not.

Android gates:

```sh
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

All ten scenarios are automated. There is **no owner walkthrough for this item**: the validator
scenarios are unreachable from the interface by construction (§1.3 — the pipeline cannot emit either
value), and the copy change is a single word verified by two exact-string assertions. Wave A's batched
walkthrough against merged `main` covers it incidentally — Discover with exactly two eligible articles
shows the corrected sentence on both clients — and that observation is recorded in `evidence.md`.
