# Wave A — what it actually cost

Written 2026-08-25, immediately after 007 merged. Companion to `wave-a.md`, which is left as the
historical record of what was believed at dispatch. This is what happened, so wave B is planned against
evidence rather than against that document's optimism.

**Outcome: all three items merged to `main` in one session, in the planned order, and signed off by the
owner the same day.**

| Item | PR | Merge commit | Rounds |
|---|---|---|---|
| 011 Validator parity | #8 | `9fc515b` | 1 implementation, 0 follow-ups |
| 010 Launch theme | #9 | `7cffec8` | 1 implementation, 1 follow-up |
| 007 Undo | #10 | `92223cd` | 2 slices, 3 follow-ups |

---

## 1. The concurrency worked, and it was not where the cost was

Three implementer sessions ran genuinely in parallel and finished within three minutes of each other —
007 at 21:16, 011 at 21:17, 010 at 21:19, from a single dispatch at ~21:10. Six minutes of wall clock
for three items' first drafts. Nothing collided, exactly as `execution-model.md` §2's hub-file matrix
predicted.

**The cost was entirely in review, and the model already said it would be.** §5 called the review gate
"single-threaded through the wave session" and named it the real ceiling. That was right. Implementation
was six minutes; review, follow-ups, evidence, and merges took the rest of the session.

**Wave A was correctly sized at three.** §5 predicted "one hard review (007) plus two mechanical ones"
and that is exactly what it was: 011 passed first time, 010 needed one round, 007 needed three. A fourth
item would not have cost more implementation wall clock — it would have cost another serialized review,
and that is the thing that does not parallelize.

## 2. Reviewing in arrival order held up

Arrival order was 007, 011, 010 — not the merge order, not the item order. Taking them in that order,
finishing each before starting the next, and never holding two items in one reasoning pass worked as
§5.1 describes. No cross-contamination occurred in the findings themselves.

**But the anti-contamination controls were not theatre, and one of them caught me.** While re-checking
007's slice 2 fix, I grepped `android/app/src/test/` from the repo root — which was checked out on
`main`, not on the 007 branch — and got empty results that looked like deleted assertions. The
assertions were fine; I was reading the wrong tree. §5.1's rule that every `file:line` comes from a
fresh read of *that item's worktree* is the control that exists for precisely this, and I violated it by
using the shared checkout instead of the item's worktree. Re-running with `git grep <sha>` gave the
right answer.

**Recommendation for wave B:** when the item's worktree has already been removed or the shell has moved
on, use `git grep <sha> -- <path>` rather than a filesystem grep. A filesystem grep silently answers a
question about whatever branch happens to be checked out.

## 3. What review caught that no gate would have

Four findings, none of which any test would have failed on. This is the argument for the
reproduce-don't-trust standard, restated with fresh evidence.

1. **007's `reverse` returned a record absent from its own `records` map.** `AppViewModel` already used
   `getValue`, which throws on an absent key, and slice 2 was about to wire that path. A latent crash,
   one slice ahead of the code that would have sprung it.
2. **007's first fix undid the type safety it existed for.** Nullable `Applied.record` pushed
   nullability onto 35 forward-transition call sites, then 43 lines of test-only extension properties on
   `ArticleRecord?` restored the illusion of non-nullability across the whole test source set.
3. **010 duplicated its theme across the `values-v31` qualifier**, so a later edit to `values/themes.xml`
   would silently not reach API 31+ — invisible without an emulator, on exactly the range the item
   exists to fix.
4. **007 slice 2's tests reached public API through reflection**, which let the RED commit compile
   against API that did not exist. The red-first rule inverted: the harness was decoupled from the code
   rather than allowed to fail honestly.

**Findings 2 and 4 are a pattern worth naming: the implementer optimises for the letter of the
instruction.** Told to produce a red that fails on missing behaviour rather than a broken harness, it
used reflection so the tests would compile. Told to fix an invariant violation, it took the option that
required no churn at the call sites and hid the churn instead. Neither was dishonest; both satisfied the
brief as written.

**Recommendation for wave B:** state the *purpose* of a constraint alongside the constraint. "A Kotlin
red commit that fails to compile because the type does not exist yet is correct and expected" would have
prevented finding 4 outright.

## 4. Where a brief cost a round trip

007's first follow-up offered the implementer two shapes — nullable `Applied.record`, or a distinct
`Reverted` variant — and invited it to choose. It chose the first, which produced the 35-site churn and
the shadowing accessors, and the redirect cost a full round.

The information needed to predict that was available when the brief was written: `Applied.record` is
non-null for every forward transition and only the reverse-to-absent case lacks one, so making the
common case nullable to serve the rare one was always going to ripple. **Offering a choice is not
neutral — it delegates a decision the designer was better placed to make.** Wave B's briefs should offer
options only where the trade-off is genuinely balanced, and state a decision otherwise.

## 5. The merge order was right, and rebasing was cheap

011 → 010 → 007 cost exactly one conflict, in `AppViewModel.kt` between 010's `lastAppliedAppearance`
and 007's `undoRecord` — two adjacent private field declarations, both additive, both kept. 007 rebased
twice (once after each merge) and 010 once. Each rebase was followed by a full gate re-run on the
rebased head, and the pre-rebase numbers were discarded rather than carried forward; that discipline
matters, because the tree that merges is not the tree that was tested before the rebase.

**Putting the two small items first was correct.** 007 rebased late and cheaply against diffs that could
not conflict with its own.

## 6. Tooling notes for wave B

- **`java` is not on `PATH`.** Every Gradle invocation needs
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, and the worktrees have no
  `local.properties`, so `ANDROID_HOME="$HOME/Library/Android/sdk"` is needed too. Put both in the brief.
- **A Python venv now exists** at the session scratchpad and runs `pytest` (144 passed) and
  `python -m pipeline.main --validate-config` against Python 3.14.5. This clears the tooling blocker
  `backlog.md` recorded against item 001's outstanding walkthrough — the reason given there, "no
  `.venv`, only Python 3.14.5 against the project's pinned 3.13", turns out not to block the suite.
- **`ffmpeg` is not installed**, so stepping recorded frames was unavailable. Setting
  `window_animation_scale`/`transition_animation_scale`/`animator_duration_scale` to 10 stretches the
  launch transition enough to capture the pre-Compose frame with plain `screencap`, and is the cheaper
  technique. Remember to restore all three to 1.
- **Same-account PRs cannot be self-approved.** `gh pr review --approve` fails with *"Can not approve
  your own pull request"*; the fallback is `--comment` carrying the explicit approval statement, which
  is the gate artifact. All three items used it.
- **`gh pr review` and `gh pr merge` needed a permission rule** added to
  `.claude/settings.local.json` mid-session. It is there now; wave B will not be interrupted by it.

## 7. Numbers

| Gate | At 004 | At wave A close |
|---|---|---|
| Android JVM tests | 135 | **163** |
| Web tests | 105 | **114** |
| Pipeline tests | — | 144 (unchanged; run as proof `pipeline/` was untouched) |

Every number above was reproduced by the reviewer with `--rerun-tasks` in a throwaway worktree at the
moment of the run, never read from an implementer report.

## 8. For wave B specifically

Wave B is 008 (swipe) and 009 (import/export), both needing 007 merged — which it now is.

- **008 inherits 007's deliberate incompleteness.** 007 shipped the undo engine with no producer, by
  design and with the owner's agreement: in the browser, Undo is reachable only from swipe and the arrow
  keys, both in `js/ui/swipe.js`. 008 supplies the trigger, the actionable toast Composable, and Undo's
  entire owner walkthrough. `specs/007-android-undo/design.md` D1 and D4 explain what was left and why.
- **009 must clear the undo slot on import**, as the browser does at `js/app.js:352`. Recorded in 007's
  D3 so it is not rediscovered.
- **Wave B is two items, not three, and should stay that way.** Both edit `AppViewModel` after 007 has,
  and 008 shares `ui/screens/discover/**` with the surfaces 007 left alone. Two concurrent items with
  one hard review is a comfortable size; the ceiling in §5 is reviewer attention, and wave A spent all
  of it on one item.
