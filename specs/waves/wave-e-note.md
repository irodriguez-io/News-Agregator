# Wave E — what it actually cost

Written on 2026-09-30, after item 025 shipped, per `execution-model.md` §4.6. Wave E was five items: **017
design tokens**, then **018 shared components**, then **019 Discover** ∥ **020 Read Later and History**, then
**021 motion**. This note scores the wave's predictions against what happened, and then follows the wave
into the three items its sign-off walkthrough set off (022, 023, 024).

**Scope, and how late this is.** The note was due at 021's merge on 2026-09-19 and is eleven days late. It
is written from each item's `evidence.md`, from git, and from what `gh` still holds. **The repository was
rebuilt on 2026-09-18** (`backlog.md` §"On PR numbers"), and the hosted-CI history of every run before then
went with it. Where a number cannot be recovered, this note says so instead of reconstructing it.

| Item | Merge | PR | JVM tests | Instrumented | Push-triggered CI on the merge commit |
|---|---|---|---|---|---|
| Amendment 9, fonts, design sweep | `75d0f65`, `4bdb54a`, `47885e4` | #27, #28, #29 | — | — | lost in the rebuild |
| 017 design tokens | `98d4d4e` | #30 | 297 → 315 | 5 | lost in the rebuild |
| 018 shared components | `33dae99` | #31 | 315 → 343 | 5 | lost in the rebuild |
| Instrumented tests in CI (not an item) | `9c0dd7f` | #32 | — | 5 | lost in the rebuild |
| 019 Discover | `6e2b10d` | #33 | 343 → 362 | 5 → 8 | lost in the rebuild |
| 020 Read Later and History | `e251886` | #34 | → 381 | 8 → 12 | lost in the rebuild |
| **021 motion** | **`15e082c`** | **#3** | **381 → 385** | **12 → 17** | **Android `35478655367`, Test `35478655465`, Pages `35478655382`** |

The instrumented column is counted from git (`@Test` under `androidTest` at each merge), not taken from the
evidence files — see §7 for why. 020's own branch finished at 362 JVM; `main` reads 381 because 019's 19
landed first. **`main` closed the wave at 385 JVM and 17 instrumented**; at 025's merge it is 403 and 28.

---

## 1. The wave's structural predictions held

The brief made four structural bets, and every one paid.

- **One amendment, written before 017 was designed.** Amendment 9 merged at 10:45 on 2026-09-01 (`75d0f65`);
  017's design commit followed at 11:17. Every item cited it, and no item amended `06-ui-ux.md` on its own
  branch. The amendment's own design pass caught three contrast failures *"by computing contrast rather than
  looking"* (`wave-e-amendment.md` §5).
- **The dark scheme as an owner checkpoint, not an implementer decision.** The owner chose an Oklch
  derivation from the same seeds on 2026-09-01, against a rendered specimen of every candidate palette in
  both schemes (`wave-e-amendment.md` §4). An independent Python Oklch implementation matched all 26 derived
  values (`017/evidence.md` §6).
- **Fonts as assets, not a dependency.** Bundled in `res/font/` at PR #28; `googlefonts` was never needed.
- **019 ∥ 020 shared nothing.** Their diffs have no file in common, their commits interleave on the evening of
  2026-09-01, and 020 rebased onto 019's merge with no conflict and merged seven minutes after it. **The
  concurrency cost one rebase.** It is the cheapest concurrent pair this programme has run, and the reason
  is the design sweep's correction of the collision matrix in PR #29: it found that four of
  `ui/components/**`'s eleven files were 020's, by asking *who calls each composable* rather than *which
  directory is it in* (`wave-e.md` §"Collisions and order", Fault 1).

**The brief also bet that 017 could ship a theme and change no consumer.** It did: all thirteen legacy token
names were kept with new values, and 205 call sites in 17 files compiled unchanged (`017/design.md` D1). The
cost was accepted in writing — *"one colour under two names"* — and scoped to the wave. **It came due at the
close and is still owed**: Road to done row 3. *(Correction, 2026-10-01: four of the thirteen were owed — the four that duplicated another colour. The other nine have no twin; D1 never scheduled them. Retired by item 026, PR #13.)*

## 2. Where the cost landed: in the orchestrator's own words

Wave D's cost landed in slice plans drawn by who writes a file. Wave E's landed one step earlier, **in the
prescriptions the orchestrator wrote into briefs and designs**. Three of the wave's findings were the
brief's or the design's own text, and two evidence files say so in those words:

1. **017, slice 1 — a RED that could only fail to compile.** The brief asked for a failing-first commit on a
   purely additive change, so the RED failed at Kotlin compilation rather than on a value. *"Weak, and the
   brief's fault"* (`017/evidence.md` §3). The reviewer compensated by perturbation and the plan was amended
   to require value-failing REDs.
2. **018, slice 1 — a ban satisfied to the letter.** The brief said *no colour, radius, dimension or font
   literal*. The implementation obeyed by deriving 52, 48, 56 and 1.5 dp from the spacing scale: *"Every
   value numerically right, every expression semantically false"*, and the consequential case was the
   48 dp touch-target floor expressed as spacing arithmetic (`018/evidence.md` §4, *"The one finding, and it was
   the brief's fault"*). Fixed in one `refactor(android):` commit, test count unchanged.
3. **019, at dispatch — a design resting on tests that did not exist.** `design.md` D3 assumed item 012's
   composition-order tests were there to protect 012's placement rule. They were not. Caught by the
   orchestrator's Step 0.4 reconciliation before the implementer started (`019/evidence.md` §1). The guard
   had not existed since Amendment 7 was written, and slice 3 was amended to build it.

A fourth was a specification gap rather than a prescription. **020's StatBand applied §76.6's "numerals use
`stat-num`" to all three slots**, but only the queue count is a numeral; the other two are `"Unavailable"`
and a tag label, and a 28 sp display face broke both mid-word in a ~137 dp column. *"Every gate was green
and every assertion passed"* (`020/evidence.md` §9). Found by the orchestrator's walkthrough over `adb`,
fixed before merge.

**The pattern:** a rule written in a brief gets implemented as written. When the rule is wrong, a diligent
implementer produces a diligent violation of what was meant. Wave D's §7 says the implementer refusing to
build a contradiction is where the defects are found; **a ban is not a contradiction, so nobody refuses it.**

## 3. The gate that arrived mid-wave, and what it could and could not see

PR #32 put the instrumented suite into hosted CI between 018 and 019. That is the most consequential change
the wave made, and it is not a numbered item.

**It earned itself twice inside the wave:**

- **On its first run it failed merged code.** 018's `CategoryChipRowLayoutTest` asserted a 360 dp row on the
  runner's 320 dp default AVD and got 320. It *"passed locally only because the device happened to be wide
  enough"* (`execution-model.md` §8.3). Code that every review had passed, failed by a gate. 018's `evidence.md`, written before PR #32, still says *"Nothing regressed"*.
- **On 021's head `4f7b241`** both new Settings-sheet tests failed, 15 of 17, after passing 17 of 17 locally
  (`021/evidence.md` §10).

**What it did not see:** 020's StatBand overflow; the appearance flash (022); the card's exit and entrance
(023); the half-second gap (024); both tabs' labels showing at once. Every one of those is a defect of
legibility or of character. **Both of the gate's catches were defects of structure** — a width, a node that
could not be found. That is the line between what this suite can see and what it cannot, and three more
items after the wave did not move it.

**It also brought the project its longest-running open question.** 021's
`reducedMotionComposesDestinationAndBackResultImmediately` failed on 022's evidence-only head `29c90cd` and
again on `bf34fd7`, on code that passed unchanged. It stayed open from 2026-09-20 until 025 traced it to
`Espresso.pressBack()`, the suite's only focus-dependent call, and shipped on 2026-09-30. **A gate that has to
be re-run cannot prove anything** (`definition-of-done.md` §2.2), so a flaky test that wave E introduced stayed
open for ten days and became row 1 of the Road to done.

## 4. The headline: the owner found what mattered, and the wave left two of them

**The wave sign-off walkthrough, on 2026-09-20 on a signed release build, found two defects: 022 and 023.**
The backlog calls them *"the two defects it shipped"*. That is accurate about when they shipped and
inaccurate about where they came from, and the difference is the lesson.

- **022, the appearance flash, predates the wave.** `MainActivity` declared no `android:configChanges`, so
  choosing a scheme recreated the Activity. Item 010's `design.md` D5 considered the fix and declined it *"on
  scope, not on merit"* (`022/spec.md` §1.3). *"Nothing in the theming code is wrong."* Wave E did not cause
  it; it made the app polished enough that a white flash read, in the owner's words, as *"an unfinished
  product."*
- **023, the card's exit, is a gap the wave's allocation made.** Item 008 shipped the card's motion before
  the second edition split §44 by surface. The wave gave `ArticleCard.kt` to 019, which restyled it, and
  motion to 021, whose scope was destinations and the sheet. *"The card fell between the two"* (`023/spec.md`
  §1.2). So the Android card ran the browser's 280 ms curve, never faded, and had no entrance at all.

**The JVM suite did not merely miss 023. It pinned it.** `SwipeGestureTest.kt:139` asserted
`assertEquals(280, SwipeGesture.EXIT_DURATION_MS)` — §44's first-edition value — so the gate was green
*because* the code was non-compliant (`023/evidence.md` §1, stop 3). An assertion on a literal defends the literal,
not the requirement.

**The chain did not stop at the wave.** 023's walkthrough on 2026-09-22 passed every mechanical check and
the owner still found 024's half-second gap — *"enough for my brain to doubt whether a new card will
arrive."* The same walkthrough found the tab-label overlap, which is 021's ground, and the owner later added
it to the Road to done by name. *"Items 022, 023 and 024 were each found by the one before"*
(`definition-of-done.md` §1), and that sentence is why the definition of done exists.

This is the **fourth wave running** in which the owner using the app found the defects no gate did
(`wave-c-note.md` §5, `wave-d-note.md` §7). **Wave E is the clearest case, because it is the first wave that
had the instrumented suite in CI** — and the suite caught two width-and-presence defects while the owner
caught four that a reader feels.

**The transferable finding** is in 023's allocation, not in its code: **a collision matrix allocates files,
and a requirement can live in a file nobody was asked to re-read.** 019 owned `ArticleCard.kt` and restyled
it; nothing asked whether its motion met the edition the wave was implementing. Road to done row 7, the
second-edition sweep, exists to check whether any other §80 split fell through the same way.

## 5. The owner checkpoints the brief set, and the ones that did not happen

`wave-e.md` set five owner checkpoints and said of the fourth: *"the wave's own note should be able to say
the owner saw each screen."* **It cannot.**

| Checkpoint | What happened |
|---|---|
| 1. The amendment, before 017 | **Done.** Merged before 017's design (§1). |
| 2. The dark scheme | **Done**, with the palette, 2026-09-01. |
| 3. Fonts | **Not needed** — bundled, no dependency proposed. |
| 4. An owner walkthrough at each merged item | **Not done for any of the five.** 017 and 018 were walked by the orchestrator over `adb`, with *"the owner rules on what `adb` cannot settle"*; no ruling is recorded. 019, 020 and 021 record it as *"outstanding"* at merge. 021 merged *"on the owner's explicit authorisation"* with checkpoints 4 and 5 both open. |
| 5. Wave sign-off on a device, light and dark | **Performed on 2026-09-20**, on a signed release build, against `15e082c`. It found 022 and 023. **Its record is folded into `022/evidence.md` rather than kept as a wave record**, and no line states a sign-off in both schemes; that the dark scheme was looked at is known only because a tonal-container difference was flagged in it. |

**The checkpoint that was skipped is the one that would have found the defects earliest.** 017's own
walkthrough recorded that the 360 dp fold regressed to a five-line headline and offered the owner the
option to hold 017 until 019 (`017/evidence.md` §10.4). No ruling is recorded; 017 merged, and 019's clamp
fixed it. That worked out. It is still the pattern: **a checkpoint the merge does not wait on does not
happen.**

**Two owner judgements the wave posed are still not on the record:**

- **021's *"does the motion read as expressive or as busy?"*** — posed in its `spec.md` §5.5 as the one
  judgement only the owner can make. No verdict is recorded anywhere in `specs/`, and it is **not on the
  Road to done list**.
- **019's triage labels** — the owner kept them icon-only on 2026-09-02 and deferred the decision to wave
  close (`021/evidence.md` §9). That is Road to done row 5.

## 6. Why the wave took eighteen days instead of two

All of the wave's implementation work happened on 2026-09-01 and 2026-09-02: 017 through 020 merged inside
seventeen hours, and 021's slices and its CI fix landed the same morning 020 merged. **021 then waited sixteen days**
with one open final-review finding — a file-level `@Suppress` — and a *"Resume here"* note
(`021/evidence.md` §11–§12). No file records why. What the record does show is that the repository moved,
was rebuilt under a new remote, and took two release-signing PRs on 2026-09-18; 021 merged forward from
`7a70636` the next day, its review finding was found to be wrong in kind (seven internal reaches, not two)
and fixed, and it merged at 18:23 on 2026-09-19.

The rebuild is why the CI column in this note's table is mostly empty, and why 021's evidence still names
PR #35 for what is PR #3.

## 7. The record: what the evidence files say that git does not

Written down here so they are not trusted later. **None of these has been corrected in place**; correcting
another item's evidence is not this note's job.

- **019 and 020 both report *"+2 instrumented"*. Git says +3 and +4.** 019's `DiscoverScreenLayoutTest` has
  three tests; 020 added `ArticleRowLayoutTest` (1) and `ReadingListLayoutTest` (3).
- **020's evidence cites seven commit SHAs that no longer exist** — its pre-rebase slice commits (`d484feb`,
  `0ea9245`, `daaefb7`, `8ceda93`, `2643559`, `1d5f529`, `6510697`). The merged equivalents are `27c6da1`,
  `4e295a9`, `fe1aaef`, `85f6feb`, `03ecb92`, `1f25fc6` and `193f1e2`. No re-run of 020's gates after the
  rebase is recorded.
- **021's evidence names PR #35 and CI runs `33656289696` and `33656289670`**, all of which now return 404.
  The merged PR is #3; its merge-commit runs are in this note's table.
- **018's evidence says *"Nothing regressed"*** and was not updated after PR #32's gate failed its width test.
- **`wave-e-amendment.md` §4 says *"All four"* decisions over a table of six.**
- **017's `slices.md` gives `main` as `e126cbb`**, which does not resolve.
- **CI run IDs for 017 to 020 were never recorded.** Each evidence file says *"Recorded at PR time"*; that
  record was the PR, and the PR went with the rebuild.

**The lesson for the next wave is narrow and cheap:** an evidence file written before merge goes stale at
merge. The merge commit's SHA and its push-triggered run IDs belong in the file, and only the repository
itself is durable enough to hold them.

## 8. On the credit side

- **021's implementer took the *"test looks wrong"* branch for the first time in the wave** — refused to
  edit a test it believed wrong, reported, and was granted authority narrowly (`021/evidence.md` §5).
- **023's implementer stopped four times, and *"every one was right"*** (`023/evidence.md` §1) — two of
  those stops corrected false claims the design made about files, which had been *"believed twice — once by
  the design pass, once by the orchestrator who copied it into a brief."*
- **`LaunchBackgroundTest` survived a total palette change unedited**, because item 010 had already applied
  wave C's comparator lesson: it asserts that the launch background equals the token, not what colour the
  token is (`017/evidence.md` §4).
- **018's one finding was fixed in a single commit with the test count unchanged and no test edited.**
- **The amendment's contrast work was computed, not eyeballed**, and it caught three failures and two
  derivation bugs before an implementer saw a palette.

## 9. What the wave leaves

On the Road to done (`backlog.md`), and owed by this wave:

- **Row 3** — retire the 13 legacy token names. 017's accepted cost; due since 2026-09-19. *(Four, not thirteen; shipped as 026, PR #13.)*
- **Row 4** — both tabs' labels at once. 021's ground; needs a §79.1 amendment.
- **Row 5** — 019's triage labels, an owner decision. Whether the rail fits at 360 dp with labels restored is
  unmeasured.
- **Row 7** — the second-edition sweep. The direct consequence of §4's finding.
- **Row 8** — the final acceptance pass, which is also the first time each wave-E screen gets the individual
  look checkpoint 4 asked for.

Not on the list, and the owner's to place: **021's *expressive or busy* judgement** (§5). Item 012's 360 dp
limitation still stands after 019 and is recorded, not owed (`backlog.md`).

With this note written, **wave E's close is two pieces short instead of three**: the token names and
`backlog.md`'s own close of the wave.
