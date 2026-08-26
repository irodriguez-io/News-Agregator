# 011 — slice plan

Sized **XS → 1 slice**. One item branch (`feat/011-web-validator-parity`), one PR targeting `main`.
The slice closes as a failing-first test commit plus an implementation commit.

Scenario names refer to `spec.md` §4.

One slice, not two. The three changes are one expression, one guard, and one string; splitting them
would produce three commits' worth of ceremony around about six lines of product code. They also share
a single reason to exist — the browser being less careful than the pipeline and than its own port.

## Slice 1: the browser matches the pipeline, and both clients say the same sentence

- **Scenarios:** all ten in `spec.md` §4.
- **Files:**
  - `js/data/validation.js` — the `readingTimeMinutes` minimum (`:145`), and a `tags` length guard
    beside the array check (`:148`)
  - `js/ui/discover.js` — the singular verb in the side note (`:330`)
  - `tests/js/articles.test.js` — the seven validator scenarios
  - `tests/js/discover.test.js` — the two copy scenarios, as exact-string assertions
  - `tests/js/helpers.js` — only if `makeArticle`/`makeDataset` genuinely cannot express a six-tag or
    seven-tag article without a change; prefer building the fixture in the test
  - `android/app/src/main/kotlin/io/irodriguez/intentionalreading/ui/format/Labels.kt` — line 46, and
    nothing else in the file
  - `android/app/src/test/kotlin/**` — the Android half of the cross-client scenario, as an exact-string
    assertion in the `Labels.DEGRADED_NOTICE` style
- **Must not touch:** `pipeline/**`, `config/**`, `docs/v1/**`, `css/**`, `index.html`, `scripts/**`,
  `data/**`, and every Android path except `Labels.kt` and its test. No dependency is added to
  `package.json` or `android/gradle/libs.versions.toml`.
- **Fixed decisions — do not re-open mid-implementation:**
  - **The tag cap is SIX, not five.** `pipeline/validation.py:95` is the authority, because
    `contracts.md` §7's limit of five is on *organically detected* tags and
    `pipeline/taxonomy.py:58-70` appends forced tags on top. `backlog.md` and `waves/wave-a.md` both
    say five and are both wrong (`design.md` D1). A five-tag cap would reject legitimate datasets.
  - **The corrected string is exactly** `1 more choice waits quietly behind this one.` — owner-settled,
    verb only. The plural branch is not touched.
  - **Both clients change, or neither does.** A commit that fixes one is incomplete, not partial.
  - Reuse `fail(path, message)` for the tag-length refusal. No new error code, no new message
    vocabulary, no change to `DatasetError` (`design.md` D2).
  - The existing duplicate-tag-id check stays; the length guard is additional.
  - The Android assertion stays an **exact-string** assertion with a new exact string — not a regex,
    not `contains`, not a prefix match (`design.md` D3).
- **Definition of done:**
  - `npm test` green — 105/105 at 004, plus this slice's additions.
  - `python -m pytest` and `python -m pipeline.main --validate-config` green, as proof the pipeline was
    not touched.
  - `cd android && ./gradlew :app:testDebugUnitTest` and `:app:assembleDebug` green.
  - A test proving `readingTimeMinutes: 1` is refused and `2` is accepted, and that `null` still is.
  - A test proving seven tags are refused and **six are accepted** — the six-accepted case is the one
    that would catch a five-cap regression, so it is not optional.
  - A test proving duplicate tag ids are still refused, i.e. the existing check was not displaced.
  - Exact-string assertions on both the singular and plural side note in `tests/js/discover.test.js`,
    and an exact-string assertion on the Android singular and plural labels.
  - `git diff --stat` shows no file under `pipeline/`, `config/`, or `docs/v1/`.
  - No assertion from either existing suite deleted.
- **Status:** pending

## Bookkeeping this item creates for wave close

Not slice work — recorded here so it is not lost when `evidence.md` is written:

- `specs/backlog.md` §011's third bullet says the taxonomy "limits organically detected tags to five"
  and implies a five-tag client cap. Correct it to six with the forced-tag reasoning when 011 is moved
  to Shipped (`design.md` D1).
- `specs/waves/wave-a.md` §011 carries the same error and should be annotated rather than rewritten —
  the wave brief is a historical record of what was believed at dispatch.
