# 002 — design

## Workstream role

`android-client` — a new role created by Amendment 6, owning exactly:

- `android/**`
- `.github/workflows/android.yml`
- `specs/002-android-client-foundation/**`

Forbidden to this role: `pipeline/**`, `config/**`, `js/**`, `css/**`, `index.html`, `scripts/**`,
`tests/**`, `package.json`, `requirements*.txt`, `.github/workflows/test.yml`,
`.github/workflows/deploy.yml`, and every file under `docs/v1/**` except the Amendment 6 paragraph and
the §28 cross-reference that authorize this item.

The role exists because none of the five V1 workstreams fits. `frontend-ui` is defined against the
vanilla-JS runtime; `state-ranking` owns `js/state/**` and `js/ranking/**`; `content-pipeline` owns
Python. An Android client that borrowed any of those roles would inherit forbidden-path rules written for
a different runtime.

## Decisions

### `/android` is its own Gradle root, not a module of a parent build

There is no root Gradle build to include it in, and creating one would put a `settings.gradle.kts` at the
repository root — a build file governing a repository that is 90% Python and vanilla JS. A self-contained
build under `/android` makes the isolation mechanical: `scripts/build_pages.py:14-19` allowlists exactly
`index.html`, `css`, `js`, and `data/articles.json`, so no Android file can reach the deployed artifact
regardless of what is added later. Single `:app` module; the package layering below is the same seam and
is extractable when a second *consumer* appears (widget, Wear), not when a second *layer* appears.

### compileSdk 37, targetSdk 36, minSdk 26

`minSdk 26` is a timekeeping decision, not a hand-wave about modern devices: API 26 provides `java.time`
with no core-library desugaring configuration at all, and this dataset is timestamp-dense — `publishedAt`
parsing, relative-age rendering, and the local-calendar day bucketing of `js/ui/format.js:51-62`. Compose
itself needs only 21.

`compileSdk 37` is forced by the pinned AndroidX stack, which declares a minimum compileSdk of 37 in its
AAR metadata. This was set to 36 at design time on the incorrect assumption that a missing
`platforms/android-37` and absent `cmdline-tools` made 37 unreachable locally; AGP installs the platform
itself against the already-accepted licence, which slice 1's review verified end to end.

`targetSdk` deliberately stays at **36**. It satisfies the Play requirement that took effect
2026-08-31, and moving to 37 means inheriting Android 17 behaviour changes — that belongs in its own
slice with its own testing, not in a foundation.

### The bundled dataset is a snapshot of production bytes, not a hand-authored fixture

`articles.json` is gitignored (`.gitignore:1`) and CI never commits it, but the six-hourly deploy
publishes it, so real bytes are retrievable:

```sh
curl -fsS -o android/app/src/main/assets/sample_articles.json \
  https://irodriguez.io/News-Agregator/data/articles.json
```

Those bytes already passed `pipeline/validation.py:134-178` before publication, so conformance is free
rather than a matter of author discipline. Hand-authoring would have required replaying the freshness
ladder (`pipeline/scoring.py:24-40`), the metadata rules (`pipeline/scoring.py:11-21`), and per-source
quality and content-type scores from `config/sources.json` by hand, and would still have produced a
fixture less demanding than reality. The current snapshot (166 articles, 169,736 bytes) exercises
`readingTimeMinutes: null` in 73% of articles, zero tags in 122 of 166, 70 null authors, 33 empty
excerpts, 25 titles containing non-ASCII characters, and `failedSourceCount: 5`.

The asset is named `sample_articles.json`, not `articles.json`, so it can never be confused with the
generated file. It is snapshotted whole — trimming would break `pipeline.articleCount` against
`articles.length`.

### The validator mirrors `js/data/validation.js` rule for rule, and returns a result

A laxer second implementation of a frozen contract is how contracts rot. Every rule is ported: exact key
sets, the `^[0-9a-f]{20}$` article-ID pattern, the `^[a-z0-9][a-z0-9_]{0,99}$` identifier pattern, the
seven category IDs, the ten content-type IDs, the UTC timestamp pattern *and* its real-calendar-date
check (`js/data/validation.js:80-93` — the rule that rejects `2026-02-30T12:00:00Z`), score component
ranges, `base == sum`, and the dataset-level count arithmetic.

Two deliberate omissions: the `__proto__` / `prototype` / `constructor` key rejection
(`js/data/validation.js:49-55`) is JavaScript prototype-pollution defence with no JVM analogue, and the
defensive-copy contract of `js/data/articles.js` is unnecessary for immutable Kotlin data classes.

Validation returns a result type, never an exception. This is not stylistic: when a later milestone
fetches live bytes, one bad article from a newer pipeline must not blank the client, and a lenient
drop-and-count mode has to be addable without restructuring the call sites. For the same reason
`schemaVersion != 1` is a *separately coded* failure — "this app is out of date" is a different message
from "this dataset is malformed", exactly as `js/data/articles.js` distinguishes `UNSUPPORTED_SCHEMA`
from `INVALID_DATASET`.

### Closed enumerations are Kotlin enums resolved inside the validator, with no `UNKNOWN` member

`Category`, `ContentTypeId`, `ArticleStatus`, and `ArticleAction` are closed sets of values with no
per-case payload, so enums rather than sealed hierarchies; `entries` then supplies the category chip row
and the test tables for free.

They are resolved by an explicit `fromId` lookup in the validator rather than by `@Serializable enum`
decoding, because a decoding failure raises a `SerializationException` that cannot say *which* article
was at fault, whereas the validator can report `articles[3].category`. No `UNKNOWN` member exists:
`schemaVersion` 1 is frozen, so an unknown category is an upstream defect, and a tolerant fallback would
silently construct a half-valid domain object. `ArticleStatus` carries the wire strings from
`js/data/validation.js:14` so a future persisted export can interoperate with the browser's local state
shape.

### `ArticleRecord` stores the full Article snapshot now, before anything needs it

The browser stores the whole validated article inside each record (`js/state/article-state.js:26-35`,
`:91-93`), and `tests/js/article-state.test.js:215-228` locks that the persisted snapshot — not a later
dataset copy — is authoritative. The reason is retention: articles age out after 45 days
(`pipeline/retention.py:40`), and Read Later and History must survive that. If the Android record held
only an article ID, the persistence milestone would *silently shrink* both lists on every dataset
refresh. Nothing in this milestone needs the snapshot, and it is the one modelling decision that is
genuinely expensive to reverse, so it lands now.

### One ViewModel, and per-screen state derived by pure functions

The three screens are three projections of one record map — precisely the browser's single `state`
object. Three ViewModels would require a shared repository singleton on day one to avoid divergent
copies. Per-screen `UiState` is computed by pure functions with no Android imports, so the four mutually
exclusive Discover body states are encoded as a sealed type and their precedence is enforced by the
compiler rather than by the ordering of an `if` chain, as in `js/ui/discover.js:282-333`.

`domain/**` must not import `android.*` or `androidx.*`. That single rule is what keeps the suite a fast
JVM suite and keeps a future module extraction mechanical.

### The clock is injected

`nowProvider: () -> Instant` and a time-zone supplier are constructor parameters of the ViewModel and the
state mapper. The Today / Yesterday / Earlier bucketing (`js/ui/format.js:51-62`) and the relative-age
ladder (`js/ui/format.js:24-37`) are calendar-sensitive, and an `Instant.now()` inside a composable is
the single most common way this layer becomes untestable.

### No navigation library

Three peer destinations, no arguments, no deep links, and a back stack that is never deeper than one —
the browser implements exactly this with a three-entry hash map (`js/app.js:32-37`). `androidx.navigation`
2.9.8 is in published maintenance mode (critical fixes only), so adopting it starts in debt.
`androidx.navigation3` 1.1.6 is stable and is the right eventual answer, but its value is the back stack
as observable data, scene strategies, and per-entry ViewModel scoping, none of which this milestone has.
It is declared in the version catalog and left unused so the decision is recorded rather than
re-litigated. Migration trigger: the first in-app article reader, share-intent deep link, or adaptive
list-detail pane. Because the destination is one field in one state class read by one `when`, that
migration is a two-file change.

### The `Read article` action hands the URL to the system browser

`Intent(ACTION_VIEW, uri)`, not a Custom Tab: `androidx.browser` would be a new runtime dependency, and
the browser client's own behavior is to leave for an isolated external context
(`window.open(url, "_blank", "noopener,noreferrer")` with `opener` nulled, `js/app.js:84-89`). Validate
the URL scheme before dispatching, mirroring the `INVALID_ARTICLE` refusal at
`tests/js/article-state.test.js:277-288`.

### Six authored colour tokens, plus a thin Material 3 scheme

`DESIGN.md:13` authors six OKLCH values per theme (`css/app.css:3-8`, `:35-41`) and derives everything
else with `color-mix(in oklch, …)`. Mapping six values onto Material 3's thirty-odd semantic roles would
invent the other twenty-four and would let tonal elevation tint `surface`, which `DESIGN.md:36-40`
forbids — the accent is reserved for the content-type badge and the primary read action, and navigation
selection uses ink, not accent. So the tokens are a dedicated immutable type behind a
`staticCompositionLocalOf`, with derived values computed from the six rather than hand-tuned.

A Material 3 `ColorScheme` is still constructed, because `NavigationBar`, `ModalBottomSheet`, ripples,
and focus indication read from it and would otherwise render in default Material purple. Dynamic colour
is disabled: the palette is authored, not derived from wallpaper.

Compose has no OKLCH literal, so the six values are converted to sRGB hex with the OKLCH triple retained
in a comment beside each. The conversion self-validates: the light accent `oklch(0.322424 0.12543
262.24)` round-trips to `#0B2D72`, the hex `DESIGN.md:36` requires.

### No image loading dependency, ever

`index.html:9` sets `img-src 'none'` and `01-product.md` lists article images as a non-goal.
`DESIGN.md:6` makes text the visual material. Coil, Glide, and any equivalent are out of scope
permanently, not merely deferred — worth stating so that nobody adds one "for the source favicon".

## Reuse

| Need | Existing definition to port |
|---|---|
| Dataset contract, field by field | `js/data/validation.js:119-225`, `pipeline/validation.py:21-131` |
| Frozen category and content-type IDs | `pipeline/constants.py:3-24`, `js/data/validation.js:3-26` |
| Canonical article ordering | `pipeline/retention.py:12-20` |
| Article status values | `js/data/validation.js:14` |
| Status transition table and idempotent no-ops | `js/state/article-state.js:70-86` |
| Per-action timestamp side effects | `js/state/article-state.js:96-138` |
| Discover eligibility | `js/state/selectors.js:3-5` |
| Read Later and History ordering | `js/state/selectors.js:12-23` |
| Navigation badge counts | `js/state/selectors.js:25-34` |
| Overview band aggregates | `js/state/selectors.js:36-58` |
| Relative age, reading time, history bucketing | `js/ui/format.js:24-66` |
| Category chip list and labels | `js/ui/format.js:1-10` |
| Discover body-state precedence and copy | `js/ui/discover.js:276-336` |
| Card anatomy and action set | `js/ui/discover.js:134-274` |
| Degraded-mode disclosure | `js/app.js:229`, `js/ui/discover.js:312-333` |
| Appearance values | `js/data/validation.js:13`, `js/ui/theme.js:12-29` |
| Colour tokens and derivations | `css/app.css:1-42` |
| Type scale and editorial roles | `design-reference/DESIGN.md:45-53` |
| Sample dataset bytes | `https://irodriguez.io/News-Agregator/data/articles.json` |
| Source quality and content-type scores, topic IDs | `config/sources.json`, `config/topics.json` |
| CI action pinning house style | `.github/workflows/test.yml:16,18,33` |

## Regression boundary

No existing test may be modified, and no existing gate may change behavior. This item adds no Python, no
JavaScript, no CSS, and no HTML. The following must remain green and untouched:

- `npm test` — `node --test tests/js/*.test.js`. Baseline verified on the item base SHA `9e524eb`:
  **105 tests, 105 pass, 0 fail**.
- `python -m pytest` — 86 test functions across `tests/pipeline/`. Counted statically; no local
  virtual environment exists in this working copy, so the authoritative run is hosted CI.
- `python -m pipeline.main --validate-config`
- `python -m pip_audit -r requirements.txt`

Two invariants outside `/android` must hold after this item and are worth asserting explicitly, because
they are cheap to check and expensive to lose:

1. `ALLOWED_PATHS` in `scripts/build_pages.py:14-19` is unchanged, so `/android` cannot enter the Pages
   artifact.
2. `.github/workflows/test.yml` and `.github/workflows/deploy.yml` are unchanged, so web and pipeline
   pull requests do not begin paying for a Gradle build.

## Divergences from the browser client

Each is deliberate, and each is recorded here so that a reviewer does not file it as a defect.

1. **Typography.** Android has neither Iowan Old Style nor Avenir, and Avenir is licensed and may not be
   bundled. The client uses the platform serif, sans, and monospace families, which is the same
   fallback-chain strategy as `css/app.css:19-21` but resolves to different faces. The Android client
   therefore reads in a different voice than the web client. Revisitable by bundling licence-clear faces
   and re-tuning the scale; that is a milestone of its own, not a detail of this one.
2. **Discover ordering.** Dataset file order, not personalized re-sequencing. On a fresh install the
   difference is near-invisible because personalization collapses toward `score.base` with no
   interactions recorded, but the head article may differ from the browser's for the same bytes.
3. **No persistence across process death.** State survives configuration change only.
4. **`readingTimeMinutes` accepted at `>= 1`.** `contracts.md` states a minimum of 2 and
   `pipeline/validation.py:81-83` enforces it, but the shipped browser validator accepts `>= 1`
   (`js/data/validation.js:145`). The Android client matches the shipped validator rather than being
   stricter than the generator. Empirically the pipeline never emits 1 — the minimum non-null value in
   the current snapshot is 2 — so this is a tie-break with no observable consequence, and the
   documentation-versus-code disagreement is reported rather than resolved unilaterally.
5. **`tags` capped at 6.** `pipeline/validation.py:94-95` enforces a maximum of six, and
   `taxonomy.py:38-40` plus the forced-tag rules explain why. `validateArticle` in
   `js/data/validation.js:148-163` has **no length check at all**. The Android client enforces the
   contract's number; the gap in the browser validator is reported to the web owner and is explicitly
   not fixed by this item.
6. **No swipe, no undo, no toast.** Triage is by labeled button only. `DESIGN.md:8` requires a labeled
   keyboard equivalent for every gesture, so shipping buttons before gestures is the correct order.

## Decision record

Amendment 6 in `docs/v1/README.md` is the durable record of the authorization; this document is the
durable record of the technical decisions, which is the repository's established pattern — item 001
recorded its decisions here and explicitly declined a separate ADR, and no `docs/decisions.md` exists.

Dependencies introduced (all first-party Android or JetBrains, all under `/android`, none reaching the
web runtime): Android Gradle Plugin, Kotlin, Compose via the Compose BOM, Material 3,
`androidx.core:core-ktx`, `androidx.activity:activity-compose`, `androidx.lifecycle` ViewModel and
runtime Compose artifacts, `kotlinx.serialization`, and test-only `kotlin-test`,
`kotlinx-coroutines-test`, `androidx.test.ext:junit`, and Espresso. Versions are pinned in
`android/gradle/libs.versions.toml`, which is the single place they are declared.
