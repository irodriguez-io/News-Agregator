# 006 — Evidence

Item 006 ports the browser's deck diversity sequencing — a −8 same-source penalty and a −5
third-consecutive-category penalty, applied per selection step — to `DiscoverDeck`. One slice, one
implementer session that produced code, and three plan defects caught before or at review.

**Nothing this item adds is visible at today's surface** (`spec.md` §1.1). The walkthrough below is a
regression check and says so at every step; no step demonstrates a penalty, because none can.

## 1. Gates

Run by the orchestrator on the reviewed head `439a116`, not read from the implementer's report
(`spec.md` §5.1):

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd android
rm -rf app/build/test-results/testDebugUnitTest
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:assembleDebug
```

| Gate | Baseline (`c27d87e`) | This item (`439a116`) |
| --- | --- | --- |
| `:app:testDebugUnitTest` | 258 tests, 0 failures | **275 tests, 0 failures, 0 errors, 0 skipped** |
| `:app:assembleDebug` | BUILD SUCCESSFUL | **BUILD SUCCESSFUL** |
| `npm test` (parity cross-check) | 114 pass, 0 fail | **114 pass, 0 fail — unchanged** |

**The baseline is the post-merge head, not a number carried from any 005 slice report**
(`waves/wave-b-note.md` §7). `main` was merged in at `c27d87e` and the gates were re-run there before
anything was dispatched; the merge, not the slice, is what changed the tree.

`npm test` unchanged at exactly 114 is the evidence that `js/**` was read as the specification and never
edited.

## 2. Commits

| SHA | What |
| --- | --- |
| `c27d87e` | merge `main` into the item branch (46 commits behind: 005's merge and all of 013) |
| `e600559` | `docs(spec)`: D8 — the frozen five-key assertion |
| `a0d0470` | `docs(spec)`: correct D8's recorded penalty from −8.0 to 0.0 |
| `b3d35af` | **failing-first tests** |
| `5d6f02e` | implementation |
| `421a9de` | `docs(spec)`: §4.4's no-local-state-write is structural, not assertable |
| `439a116` | `test(android)`: remove the vacuous write assertion |

**The failing-first commit was verified independently**, not accepted on report: `b3d35af` was checked
out into a throwaway worktree and `:app:testDebugUnitTest` run there. It fails at
`compileDebugUnitTestKotlin` with `Unresolved reference 'sequencing'` at 19 call sites and
`Cannot access 'candidateComparator': it is private`. Missing behaviour, not a broken harness.

## 3. What the plan got wrong, and who caught it

Three defects. **All three were in the plan; none reached shipped code.** Two of the three were caught by
the implementer refusing to build a contradiction, which is the escalation rule paying for itself.

### 3.1 The frozen five-key assertion — caught by the implementer, first dispatch

`slices.md` froze every existing assertion. Item 005's
`DiscoverDeckTest.kt`'s *"candidate order follows all five keys with a deliberate collision at each key"*
asserts the ordered `deck.candidates` list ending `sourceAFirst, sourceASecond, sourceB` — two `source-a`
articles adjacent **on purpose**, so §58's key 5 has a collision to resolve. This item's −8 must break
that adjacency. No implementation satisfies both.

The first session read the brief, found this, wrote nothing, and reported. Resolved by `design.md` D8:
`candidateComparator` becomes `internal`, 005's assertion sorts with it directly — same fixture, same
eight IDs, same order, same name — and this item adds a test for that fixture's *sequenced* order so the
coverage moved off `build()` is replaced. Net assertions go up.

**Root cause worth carrying forward:** `js/**` asserts `compareCandidates` on a sorted array
(`tests/js/ranking.test.js:60-74`) and asserts `buildDeck` separately. Item 005 routed the comparator
assertion through `DiscoverDeck.build()`, and this item's plan froze that coupling. **An assertion routed
through an algorithm that a later item reorders will be frozen by that item's plan and block it.**

### 3.2 The recorded penalty was wrong — caught by the implementer, second dispatch

D8's new test was specified as asserting `sourceASecond` records `sameSourcePenalty == -8.0`. It records
**0.0**. The −8 decides *step 7*, where `sourceASecond` scores 82 against `sourceB`'s 90 and loses; by
step 8 the previous card is `sourceB` and D1 recomputes from scratch. The reordering is the observable
proof the penalty fired; the recorded number is zero.

`spec.md` §4.1 already said exactly this — *"the third card's same-source penalty is 0"* — so the
amendment contradicted an authoritative scenario two files away. Corrected in `a0d0470`. An arithmetic
error in the plan, of the same class `waves/wave-b-note.md` §4 says is cheapest to catch before code.

### 3.3 A vacuous assertion — caught at slice review

The implementer's `sequencing modifies nothing` contained:

```kotlin
var localStateWriteCount = 0
// ... build(...) ...
assertEquals(0, localStateWriteCount)
```

Declared, never touched, asserted equal to its own initializer. It cannot fail under any implementation,
and it was the only thing answering the DoD's *"and that no local-state write occurs"*.

`DiscoverDeck.build` is a pure `domain/` function with no persistence dependency and **no writer to
observe**, so there is no honest assertion available at this layer — the property is enforced by the
signature and by the no-`android.*`-imports invariant. The DoD was corrected to say so (`421a9de`) and
the two lines were deleted (`439a116`). The value-equality half of that test was always real and stands.

## 4. Slice review

One slice, reviewed against `spec.md` §4 and the slice plan, not against the brief.

- All 17 new tests map to §4 scenarios by name; no `@Ignore`, `@Disabled`, or `assumeTrue` anywhere.
- The greedy loop matches `js/ranking/deck.js:26-72`, including the `selectedCategory == null` all-view
  gate and the two-already-selected-cards category condition.
- `minWithOrNull` falls through to the existing five-key comparator rather than restating it (D2).
- **The only deleted lines in the entire test tree are the eight belonging to the authorized D8
  retargeting.** The other four `DiscoverDeckTest` tests are byte-identical.
- Verdict: one finding (§3.3), fixed, then **PASS** on `439a116`.

## 5. Owner walkthrough — `spec.md` §5.2

Driven over `adb` on `Pixel_10` (`emulator-5554`, API 37) by the orchestrator, against the 006 build
installed **over** the pre-006 build (`f06a32b` = 005 merged + 013) without resetting, exactly as §5.2
requires. Artifacts in `walkthrough/`.

**Result: all six steps pass. Two deviations from the script are recorded below rather than smoothed
over.**

### Step 1 — the head card did not move: **passes, byte-identically**

| | pre-006 build | 006 build installed over it |
| --- | --- | --- |
| available count | 215 available in All | 215 available in All |
| head card | *Web Authentication: An API for accessing Public Key Credentials Level 3…* (W3C Web Authentication WG) | identical |

The screenshot and the `uiautomator` dump are **the same file by MD5** across the two builds
(`4228b595…` and `4961c229…`). This is D3's claim confirmed in the running app, and it is what pins
`spec.md` §1.1 and item 005's `design.md` D12 outside of prose.

### Step 2 — a swipe still advances correctly: **passes**

Three consecutive dismissals: **215 → 214 → 213 → 212**, one per swipe. Four distinct articles, no
repeats, each incoming card fully composed before the next swipe.

### Step 3 — the category selector still filters: **passes, on both builds**

Read on the 006 build, then with the pre-006 APK swapped back over the same data, then swapped again:

| view | pre-006 | 006 |
| --- | --- | --- |
| All | 212 available, head *Notice of Vote to Approve…* | identical |
| Science | 33 available, head *Science is based on assumptions…* | identical |
| All (returned) | 212 available, same head | identical |

### Step 4 — the held card still holds: **passes**

*Read article* opened `com.android.chrome/…ChromeTabbedActivity`. On `KEYCODE_BACK` the same article is
the card on screen, now showing `OPENED` and *"This article is ready for your next decision."*, with
**`Mark read` at y=1829 on a 2424-px screen — reachable without scrolling**.

### Step 5 — Undo still returns the card: **passes**

Swipe to dismiss, 0.4 s, tap Undo, all inside one on-device `adb shell` — per
`013/investigation/step0-undo-window.md` §158, tapping Undo from the host after a `uiautomator dump`
misses the 4.5 s window every time. Confirmed here: the toast is invisible to a dump and plainly visible
in a `screencap` at 0.35 s (`walkthrough/step5-undo-toast-at-0.35s.png`).

Read from the pulled state document, not from the screen:

| | before | after swipe + Undo |
| --- | --- | --- |
| available | 206 | **206** |
| head card | *In Hilbert Space, All Things Are Quantumly Possible* | **identical** |
| records | 9 — 5 dismissed, 4 saved | **9 — 5 dismissed, 4 saved** |

**Deviation, and it is not this item's:** §5.2 step 5 says *"Save an article, undo from the toast"*. The
**Save for later button commits the save but raises no Undo offer at all** — screencaps at 0.2 s, 1.0 s
and 2.0 s show none, while the swipe path raises one at 0.35 s. **This reproduces identically on the
pre-006 build**, so it is pre-existing on `main` and nothing 006 did causes or worsens it. The Undo
mechanism itself was therefore exercised through the swipe path, which is the same reversal.

This is adjacent to **item 014** but is not quite what 014 describes. 014 says the card buttons fail
*inside the Undo window*; what is observed here is that the Save button **succeeds** — the record is
written, Read Later increments — and simply never offers the reversal that
`ArticleStateMachine.reversibleActions` makes available for `SAVE`. **Item 014's designer should start
from this observation rather than from 014's current wording**, and should not assume the two share a
mechanism — that assumption cost item 013 two of its four passes.

### Step 6 — no jank at full dataset size: **measured; owner judgement outstanding**

Ten swipes (**206 → 196**, exactly ten), `dumpsys gfxinfo` reset immediately before each run, same
device, same session:

| | pre-006 (`f06a32b`) | 006 (`439a116`) |
| --- | --- | --- |
| 50th percentile | 17 ms | **17 ms** |
| 90th percentile | 19 ms | **19 ms** |
| 95th percentile | 21 ms | **20 ms** |
| 99th percentile | 34 ms | **34 ms** |
| Janky frames | 26 (9.15 %) | **22 (8.33 %)** |
| Missed Vsync | 0 | **0** |

**Indistinguishable.** The greedy loop's cost does not show above the noise of the two runs.

**Deviation, stated plainly:** §5.2 step 6 specifies *"a full 500-article dataset"*. The cached dataset
is **215 articles** — that is what the live feed carries today, and fabricating a 500-article dataset
would mean touching `pipeline/**`, which §3 forbids this item. D4's worst case is therefore exercised at
about **18 %** of its cost: ~23,000 candidate evaluations per rebuild against ~125,000 at n = 500. The
numbers above are real and the comparison is sound, but **they do not measure the case D4 actually
argues about.** Anyone reading this as proof that n = 500 is fine is reading more than it says.

## 6. Outstanding — what the owner is asked for, and only this

`spec.md` §5.2 reserves exactly two judgements. Both are open:

1. **Step 1 — that the head card genuinely did not move.** The evidence is byte-identical screenshots and
   dumps; what is asked is your confirmation that this is the same card you expect to see, not merely the
   same pixels.
2. **Step 6 — responsiveness at full dataset size.** The measurement says 006 costs nothing detectable at
   n = 215. It does not say what happens at n = 500. If you want that answered before this ships, it
   needs a dataset that does not exist today.

If step 6 ever does show a stall, `design.md` D4 already says the fix is **not** a cache: only the head
card is consumed at today's surface, so the sequenced tail can be computed lazily. That is a follow-up
item with a real design question, not a mid-implementation decision.
