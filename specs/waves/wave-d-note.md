# Wave D — what it actually cost

Written at 016's close, per `execution-model.md` §4.6. Wave D was four items: **015 undo swipe
attribution** and **012 Discover card first** concurrently, then **014 undo offer surfaces**, then **016
widen what is reversible**. This note scores the wave's predictions against what happened.

**Scope.** 015, 012 and 014 were dispatched and closed in an earlier session; their numbers are in their own
`evidence.md` files and are not re-scored here. What follows is written from those evidence files, from
016's run, and from `wave-d.md`'s handoff section. Where a judgement needs data only the earlier session
had, it says so instead of inventing it.

| Item | Merge | PR | Push-triggered CI on the merge commit |
|---|---|---|---|
| Amendments 7 and 8 + all four designs | `6c857a61` | #19 | Test `33422480339`, Pages `33422480328` |
| 015 undo swipe attribution | `88e71b7c` | #20 | Android `33424850723`, Test `33424850747`, Pages `33424850705` |
| 012 Discover card first | `05657ed6` | #21 | Android `33442192566`, Test `33442192352`, Pages `33442192215` |
| 014 undo offer surfaces | `cc2a6084` | #22 | Android `33456164733`, Test `33456164726`, Pages `33456164705` |
| handoff docs | `eb4742a` | #23 | — |
| **016 widen what is reversible** | **`d249bc0`** | **#24** | **Android `33462547085`, Test `33462547211`, Pages `33462546918`** |

`main` closes the wave at **297 tests, 0 failures**, both Gradle gates green. Tests went 284 → 297 across
the wave. No worktrees, no branches outstanding.

---

## 1. The wave's thesis held; its estimate of where the cost would land did not

`wave-d.md` says three of the four items are one subject — *Undo does not work where a reader expects it
to* — and that 012 rides along because it collides with nothing. **Both held exactly.** 015 ∥ 012 ran
concurrently with no interference, and the three undo items composed into a coherent whole: Undo now means
the same thing on every surface of the app.

The brief also predicted where the difficulty would be: **016's arithmetic**, called "the long pole of the
wave", to be designed "on paper, before dispatching any implementer."

**That prediction was right and it worked.** `spec.md` §2's table was settled at the design pass, ratified
by the owner, and carried into Amendment 8 — and it survived implementation without a single row being
questioned. The `MARK_UNREAD` double negative, which the brief named in advance as "the obvious place to get
the arithmetic wrong", was got right the first time because the design pass had already found that
`UndoRecord` could not express it and added `preferenceReapplication`.

**The cost landed somewhere the brief did not look: in the slice plans, not the specifications.** Every
specification in this wave survived contact. Two of the four slice plans did not.

## 2. The seam that cost the wave: plans drawn by who writes a file

This is the transferable lesson and the reason to read this note. It is also the third wave in a row to
learn it, which is the actual finding.

`execution-model.md` §2's collision matrix orders hub files by **who writes them**. That is correct for
deciding which waves can run concurrently — it is what the matrix was built for. Slice plans have been
borrowing its shape, and a slice plan needs two more questions the matrix never asks: **who asserts against
this file**, and **what receives its output at runtime.**

Three items have now failed on that gap, once per edge:

1. **Item 006, wave C — an assertion edge.** 005 asserted §58's ordering through `DiscoverDeck.build()`;
   006 exists to reorder it; 006's plan froze every existing assertion. Unsatisfiable, and it surfaced at
   dispatch, weeks after plan-mode approval.
2. **Item 014 — a compile edge.** Slice 1 removed a parameter from `AppViewModel` while excluding
   `AppViewModelTest.kt` and its 28 calls. The slice could not have built its own test sources.
3. **Item 016 — a runtime edge.** Slice 1 widened `reversibleActions`, and the records it began producing
   reached `raiseUndoOffer`'s `error("Only Save and Dismiss can raise an Undo offer")` — a different file,
   owned by slice 2. Measured at the gate: **292 tests, 7 failures**, every one that exception.

`wave-c-note.md` §7 named this gap and **nothing changed**, which is why it caught two more items. It is now
fixed: `execution-model.md` §2.1 was amended at this wave's close (`550816f`) with the three edges, the five
rules that follow, and these three cases as the evidence.

**The rule that would have caught both of this wave's instances at design time, in one sentence:** *a slice
boundary is valid only if the slice can reach a green gate on its own — state how, when the plan is
written.* Neither 014's slice 1 nor 016's slice 1 could, and neither plan was asked.

## 3. A plan can document its own contradiction and read it as a feature

016's `slices.md` said, of slice 3: *"slice 2's three new `PendingUndoMessage` cases make
`IntentionalReadingApp.kt` fail to compile until this slice lands. That is the RED, and it is stated rather
than dressed up."*

It was stated rather than dressed up. It was also a report that the cut was wrong, and it was filed as
evidence of rigour. **A slice cannot both require a green gate and be the next slice's failing-first
evidence.** The plan held both claims, in writing, and the contradiction survived a design pass and
plan-mode approval because it was phrased as a virtue.

This is a different failure from §2's — not a missing axis, but a *known* dependency that got relabelled
instead of resolved. Worth its own rule, now `execution-model.md` §2.1 rule 3: **a slice's failing-first
evidence must be its own tests failing for the intended reason, never the next slice's compile error.**

## 4. The fix that generalises: land the consumers before the producer

016 was unblocked by inverting its order. The original cut was domain → offer → copy, by layer. The
replacement is **sinks first, then source**:

- **Slice A** landed the three `PendingUndoMessage` cases, `raiseUndoOffer`'s mapping, `UiStateMapper`'s
  availability rule, the three strings and the toast mapping — all of it **green on the old tree**, because
  `reversibleActions` was still narrow and nothing produced the new values.
- **Slice B** widened the set, and everything downstream already handled it.

Each slice reached a green gate on its own with a real failing-first commit, and slice A's RED was genuine:
three mapper cases red against `UiStateMapper.kt:70`'s hardcoded `SAVE || DISMISS`.

> **Extending an enum, a set, or a returned type is safe while nothing emits the new values.** When a change
> widens what a producer emits, the consumer slice is green on the old tree and the producer slice turns the
> whole thing on. This is `execution-model.md` §2.1 rule 4 and it is the most reusable thing this wave
> produced.

## 5. An assertion enumeration expires when the item beneath it merges

016's `design.md` D5 opened with *"Complete as the tree stands today."* It was — against the tree the design
pass read. Item 014 then created `reversible actions carry undo state without caller eligibility`, whose
loop asserts that `MARK_READ` carries no undo record, which Amendment 8 overturns.

**Caught at slice 1's preflight, before a single file was edited** — the earliest catch in the programme so
far. The implementer reported it and stopped rather than absorbing it, and the case's real subject (caller
eligibility) turned out to survive untouched.

D5 was accurate when written and stale when used, and **no gate distinguishes the two.** The protocol that
caught it is the one wave D's own lesson 3 established: name cases with reasons, never freeze them, and
require an unlisted failure to be reported before it is edited. It has now paid for itself twice.

A smaller instance of the same decay: D5 said `PreferenceLearningTest` had 11 cases. The file holds two
classes and the class has 10; the 11 counted `@Test` annotations across the file. Nothing was skipped, but a
wrong count is exactly what a future freeze rule leans on. Corrected at `83bb38a`.

## 6. The two lessons the earlier session banked, unchanged

**A scenario must assert what the item controls.** Item 012's first scenario required the Discover card's
full action rail visible at 360 dp. Card height is unbounded in the article's *title* — `ArticleCard` sets
`maxLines` on the excerpt and none on the title, and `06-ui-ux.md` §25 forbids clamping it — so no header
arrangement could deliver it. The scenario also over-reached past its own Amendment 7, which binds ordering
only, and past §71, which requires actions be **reachable**, not visible. The implementer built the item,
drove the emulator, and refused to commit. Corrected in place; the 360 dp case goes to wave E item 019 with
its measurements in `012/spec.md` §1.4.

**When a design pass removes a capability, ask which *fixtures use* it, not only which assertions claim
it.** Item 014's D5 reasoned carefully about every test asserting the old rule and missed `a refused Undo
announces its failure and keeps the offer`, which used `SAVE` with `undoable = false` to persist *without*
claiming the undo slot. A fixture, not an assertion, and invisible to a line-number enumeration. Repaired by
switching the setup action to `OPEN`; name and all six assertions unchanged.

## 7. Where the findings came from, five waves running

**Every significant defect in this wave was found by an implementer refusing to build a contradiction.
None by a gate. None by reading a diff.** That is now the fourth consecutive wave with that shape
(`wave-b-note.md`, `wave-c-note.md` §5).

| Item | Found by the implementer stopping | When |
|---|---|---|
| 012 | the 360 dp scenario is unsatisfiable | after building it and driving the emulator |
| 014 | the slice split could not compile; a fixture depended on the removed flag | at dispatch, and at slice review |
| 016 | D5's enumeration was stale | **at preflight, before any edit** |
| 016 | the slice boundary could not be green | after implementing and measuring the gate |

The trend inside the wave is worth naming: the catches got **earlier**. 012's cost a full build and an
emulator run; 014's cost a dispatch; 016's first cost nothing at all. The report-before-editing protocol is
what moved it, and it is cheap enough to keep.

**One consequence for how this programme is run:** the review gate has never been where the expensive
defects are found, and four waves of evidence say it will not become so. Its value is confirming that the
work matches the specification. **The specification's correctness is established by an implementer trying to
build it** — so the cheapest possible dispatch of a doubtful plan beats another pass of reading it.

## 8. On the credit side

- **Both implementers applied item 015's `article()` helper lesson unprompted** — overriding `tags` with
  distinct ids so "this topic must not move" cannot be vacuously true when another article moves the same
  topic. 016's cross-pane test went further and added unrelated source and topic controls.
- **014's slice 2 reported two scenarios as *already covered*** rather than manufacturing duplicate tests.
- **016's slice B left six of seven known consumers unedited** and *strengthened* the seventh — `MARK_READ`
  moved from asserting no offer to asserting the specific `MARKED_READ` message, with `OPEN`'s branch
  intact. Coverage went up, not down, in a slice whose whole job was changing what those tests observe.
- **Every rewrite in this wave carries its reason in the commit message**, so a reviewer reading the diff
  alone can see that an amendment overturned the assertion rather than that it was inconvenient.

## 9. Closed out

Written before the walkthrough ran; this section records how it finished.

**The batched walkthrough for 015, 014 and 016 was driven on 2026-08-31** against merged `main` at
`d249bc0`, **against real accumulated history** — 35 articles and 31 preference entries carried over, app
data preserved through `adb install -r`, ending at 51 articles.
`specs/016-android-reversible-actions/walkthrough/` has it. 012's was driven at its slice 2 and was not
repeated.

**All eight of `016/spec.md` §6.3's steps passed**, plus item 014's *Save for later* offer and item 015's
race. **Step 3 — the re-application, the step this wave repeatedly named as the one most likely to be
wrong — is correct on the device.** Undoing `MARK_UNREAD` sent `openid_specs` from −0.15/4 back to 0.10/5
and both topics back up, entry for entry, with the record byte-identical.

**Item 015 was driven seven times at ~40–50 ms and produced zero misattributions**, scored by article id
every time.

**Both remaining owner checkpoints are settled:** the three toast strings were approved as written, and the
wave was signed off against `d249bc0`.

### The one thing the walkthrough found that no gate could

Not a defect against any specification, and visible only from the device: **the Undo toast overlaps the
bottom row's action rail** on Read Later and History while it is showing. It clears in 4.5 s and no action
is lost, but a reader aiming at the bottom row during the window hits the toast instead. Carried to wave E
item 020, which re-lays out both panes.

That is a modest finding by this programme's standards — and it is the point. **Every prior wave's
walkthrough found something substantial.** This one found almost nothing, because the defects had already
been caught earlier: four of them by implementers refusing to build a contradiction, one of those before a
single file was edited. §7's trend held all the way to the end of the wave.

### A method finding worth more than it looks

The walkthrough's *Method notes* add three items to `wave-c-note.md` §6. The one that matters:

> **The Undo toast's tap target moves with the message width** — measured at x≈692 for `Not interested`
> through x≈802 for `Removed from Read Later`. A fixed coordinate silently misses, **and a missed Undo
> looks exactly like a passing run unless you score by article id.**

That is item 015's trap wearing a third costume, now in the driver rather than in the code. It cost three
retries in this run and would have produced a false pass in a walkthrough scored by counts.
