# 011 — Web validator parity and shared copy — evidence

**Branch:** `feat/011-web-validator-parity` → `main`\
**Wave:** A (`specs/waves/wave-a.md`), first in the merge order\
**Implementer:** Codex, one session\
**Reviewer:** Claude, this session — authored the spec, design note, slice plan and this file, and
wrote no product or test code

---

## Commit chain

| Commit | Kind | Contents |
|---|---|---|
| `ddcdccc` | `docs(spec)` | `spec.md`, `design.md`, `slices.md` |
| `8cbedd6` | RED | nine scenario tests across `tests/js/articles.test.js`, `tests/js/discover.test.js`, and `UiStateMapperTest.kt` |
| `6191a49` | GREEN | `js/data/validation.js`, `js/ui/discover.js`, `Labels.kt` |

Sized XS, one slice, no follow-up round was needed — the slice passed review first time.

## Gates

Every number below was reproduced by the reviewer in a throwaway detached worktree with
`--rerun-tasks`, not read from the implementer's report.

**RED at `8cbedd6`** — the failures are the three defects and nothing else:

```text
npm test
  ✖ Scenario: a reading time of one minute is refused
  ✖ Scenario: a seventh tag is refused
  ✖ Scenario: the browser writes the singular sentence in correct English
  ℹ tests 114  ℹ pass 111  ℹ fail 3

./gradlew :app:testDebugUnitTest --rerun-tasks
  UiStateMapperTest > Scenario - the Android client says exactly what the browser says FAILED
  136 tests completed, 1 failed
```

These are genuine assertion failures, not compilation errors. Note that
*"six tags are accepted, because the pipeline can emit six"* **passes** at RED: it is a regression
guard against a five-cap, not a new behaviour, and it is the assertion that would have caught the
error described below.

**GREEN at `6191a49`:**

| Gate | Result |
|---|---|
| `npm test` | 114 tests, 114 pass, 0 fail (105 at 004) |
| `python -m pytest` | 144 passed |
| `python -m pipeline.main --validate-config` | `Configuration valid: sources=20 topics=72` |
| `./gradlew :app:testDebugUnitTest --rerun-tasks` | **136** tests, 0 failures, 0 errors, 0 skipped (135 at 004) |
| `./gradlew :app:assembleDebug --rerun-tasks` | `BUILD SUCCESSFUL` |

The pytest and `--validate-config` runs are not because this item changes the pipeline. They are the
proof that it did not.

## What the design pass caught that the backlog had wrong

**The tag cap is six, not five, and both `specs/backlog.md` §011 and `specs/waves/wave-a.md` §011 said
five.** Both derive the number from `contracts.md` §7 — *"organically detected tags are limited to
five"* — and drop the qualifier.

The pipeline reads §7 correctly in two steps. `pipeline/taxonomy.py:38-40` caps organic matches at five
with `matches[:5]`; `apply_forced_tags` (`pipeline/taxonomy.py:58-70`) then appends the source's
configured forced tags on top. Five organic plus one forced is six, which is why
`pipeline/validation.py:95` caps the total at six.

The ceiling is structural rather than incidental. `pipeline/configuration.py:183-185` compares the whole
forced-tag map against a frozen constant, and `config/sources.json` carries three sources with exactly
one forced tag each against seventeen with none — so no source can contribute a second without failing
configuration validation.

**Implementing the brief as written would have shipped a validator that rejects documents the pipeline
legitimately publishes** — replacing a too-loose validator with a wrong one, which is worse. The wave
brief's own instruction to "check first, then decide" is what surfaced it.

## Both defects are defence-in-depth, not live bugs

Also checked rather than assumed, against the dataset the pipeline had actually published at design
time — 205 articles, `generatedAt` `2026-08-26T01:57:30Z`:

| Check | Observed |
|---|---|
| `readingTimeMinutes` non-null | 44 of 205; **minimum 2**; zero instances of 1 |
| `tags` length | maximum **3**; distribution 0:120, 1:62, 2:21, 3:2 |

Neither value is reachable by construction either, not merely absent on the day.
`pipeline/normalize.py:171` returns `max(2, ceil(word_count / READING_WORDS_PER_MINUTE))` and returns
`None` below the word-count floor, so 1 cannot be produced; the tag ceiling is the arithmetic above.

So this item hardens the client trust boundary against a dataset that did not come from this pipeline —
which is the entire purpose of a client-side validator — rather than fixing something a reader is
experiencing. The scenarios are written as validator-contract scenarios accordingly, and there is no
owner walkthrough for the item.

## Decisions taken during design

- **`fail(path, message)` is reused for the tag-length refusal, with the existing `"is invalid"`
  message vocabulary** already used by the category check. No new error code, no change to
  `DatasetError`, and no change to how `js/data/articles.js` surfaces a refusal, so `contracts.md` §13
  is untouched.
- **The client validator's tag checking was NOT widened beyond length.** The pipeline also verifies each
  tag id against `config/topics.json`, the label against the taxonomy entry, and the topic against the
  article's category. The browser has no taxonomy to check against, and giving it one is a different
  item.
- **The corrected copy is a verb change only** — `1 more choice waits quietly behind this one.` — settled
  by the owner on 2026-08-25. The plural branch was already correct and is untouched.
- **Verified there is no off-by-one hiding under the grammar error.** The browser passes `remaining - 1`
  where `remaining` is `deck.length` (`js/app.js:229`); Android's `DiscoverDeck.remainingCount` is
  `eligible.size - 1` (`DiscoverDeck.kt:29`). Both clients branch on the already-decremented value and
  both render nothing at zero. A copy fix silently resting on an off-by-one would have been worse than
  the grammar error.

## What review checked

- Boundaries: six files touched, none under `pipeline/`, `config/`, `docs/v1/`, `css/`, `scripts/`,
  `data/`, `index.html`. No change to `package.json` or `android/gradle/libs.versions.toml`.
- Test files are **additions only** — zero deleted lines, so no existing assertion was weakened.
- Both frozen-copy assertions are **exact-string** assertions, per the discipline item 004 established
  at `bcc18f1`. A regex or a `contains` would defeat their purpose, which is to fail the build when one
  client's copy changes and the other's does not.
- `fail()` throws (`js/data/validation.js:45-47`), so reading `candidate.tags.length` after the
  `Array.isArray` guard is safe.
- The six-accepted test asserts order preservation via `assert.deepEqual`, not just length.

## Known behaviour this item introduces

A dataset carrying `readingTimeMinutes: 1` or a seventh tag is now refused by the browser where it was
previously accepted. Neither can be produced by this pipeline, so no dataset the project publishes is
affected. A hand-edited or substituted `articles.json` that previously loaded may now be refused, which
is the intended tightening.

## Outstanding

- **`specs/backlog.md` §011's third bullet still says five.** It must be corrected to six with the
  forced-tag reasoning when this item is moved to Shipped. Bookkeeping at wave close, not scope.
- **`specs/waves/wave-a.md` §011 carries the same error** and should be annotated rather than rewritten —
  a wave brief is a historical record of what was believed at dispatch.
- **The two validators still diverge on tag *identity*.** The pipeline checks ids, labels and category
  membership against the taxonomy; the browser checks structure, uniqueness and now length. Recorded as
  a deliberate non-goal above, not as debt to absorb silently.

## Reviewer independence

All product and test code in this item was written by the implementer agent (Codex) in one session. The
reviewer authored the specification, design note, slice plan and this evidence file, and wrote no
product or test code. Every gate result quoted here was reproduced by the reviewer with `--rerun-tasks`
in a throwaway worktree rather than accepted from the implementer's report.
