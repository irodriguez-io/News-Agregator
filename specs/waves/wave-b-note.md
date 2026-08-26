# Wave B — what it actually cost

Written 2026-08-26, immediately after 009 merged. Companion to `wave-b.md`, which is left as the
historical record of what was believed at dispatch. This is what happened, so wave C is planned against
evidence rather than against that document's expectations.

**Outcome: both items merged to `main` the same day, in the planned order, with the owner testing the
build on the emulator between the two merges and finding defects the reviewer had missed.**

| Item | PR | Merge commit | Rounds |
|---|---|---|---|
| 008 Swipe gestures | #12 | `f3ced7e` | 3 slices, 4 follow-ups (2 of them owner-reported) |
| 009 Import/export | #13 | `833f0fe` | 3 slices, 4 follow-ups (1 of them the rebase seam) |

| Gate | At wave A close | At wave B close |
|---|---|---|
| Android JVM tests | 163 | **198** |
| Web tests | 114 | 114 (untouched) |

---

## 1. The concurrency worked again, and again it was not where the cost was

Both items ran genuinely in parallel through their first two slices, and neither collided. The hub-file
matrix in `execution-model.md` §2 predicted the two contact points exactly — `AppViewModel.kt` and
`IntentionalReadingApp.kt` — and both turned out to be additive.

**Wave A concluded that review is the ceiling. Wave B agrees but sharpens it: the ceiling is
*verification*, of which code review is only the cheap half.** Seven review rounds across the two items
found real defects, and every one was about *shape* rather than function — a nullable field on a shared
type, raw bytes written where a canonical encoding belonged, a lock held across a document write, a
shipped item's scenario quietly absorbed into a new test. Gates were green in all seven cases.

But the four most valuable findings of the wave did not come from reading diffs at all. Two came from the
owner using the app, one came from re-gating a rebased head, and one came from driving the emulator far
enough to notice a message nobody could see.

## 2. Two defects the owner found that the reviewer had walked straight past

This is the wave's most uncomfortable lesson and the one most worth carrying.

1. **Returning from the publisher left Mark read below the fold.** The reviewer hit this *during item
   009's walkthrough*, scrolled up to reach the button, and carried on — treating a workaround as a step.
2. **A committed swipe returned the screen to the top**, leaving the incoming card half hidden. The
   reviewer drove a dozen swipes through item 008's own walkthrough without registering it.

Both were reported by the owner within minutes of first touching the build.

**The failure was in the questions being asked.** Every check the reviewer ran was of the form *did the
state change correctly* — records written, counts updated, files untouched, announcements raised. Neither
defect is a state question. Both are *is the right thing in front of the reader*, and no step in either
walkthrough asked it. Screenshots were being read to confirm assertions, not looked at the way a reader
would look at them.

**Recommendation for wave C:** every walkthrough step gets a second question after the assertion —
*and is what the reader needs now actually on screen?* It costs nothing and it is the only thing that
would have caught either of these.

## 3. The rebase caught a defect nothing else could have

`execution-model.md` §4 requires both gates re-run on the rebased head, with pre-rebase numbers discarded.
That rule reads like bookkeeping. It is not.

After 008 merged and 009 rebased onto it, the rebased head **failed to compile**, and behind that sat a
genuine cross-item defect:

- Item 007's D3 required an import to clear the undo **slot**, and 009 did.
- Item 008 introduced the visible undo **offer** as deliberately independent of the slot, because the
  browser's toast expires while `undoManager.peek()` stays populated.

Both correct alone. Together, an import emptied the slot and left the offer standing — the app offering an
Undo button it already knew would fail with `UNDO_UNAVAILABLE`. **Neither branch could show it:** 008 had
no import, 009 had no offer. Recorded as `specs/009-android-import-export/design.md` D11.

**Nothing automated would have caught it.** Both branches were green. Both PRs had green hosted CI. Git
found nothing to conflict about, because the two items' edits touched different lines of the same files.

**Corollary for wave C:** 005 runs alone, so there is no seam of this kind — but it collides with every
other Android item, which means *it* is the branch that will be rebased against whatever lands first. The
same rule applies with the same force.

## 4. The implementer's escalations were the right ones

Wave A's note named a pattern: the implementer optimises for the letter of the instruction. Wave B's
briefs were written against that — stating the *purpose* of each constraint alongside the constraint — and
the behaviour changed measurably.

Codex stopped and asked **three times**, each time correctly:

- twice on item 008 slice 1, before writing any code, on two arithmetic errors in the reviewer's own slice
  plan (the threshold boundary, and an intent-lock example where 9 × 1.15 = 10.35 made the stated pair
  wrong);
- once at the rebase, refusing to reshape a test that was failing on behaviour at an integration seam, and
  correcting the reviewer's expected test count while it was at it.

**Design errors caught before any code is written are the cheapest defects in the programme.** Two of the
three cost a single message each.

## 5. Where a brief cost a round trip, again

Item 009's export result shape. The brief said *"carrying the encoded bytes on success is fine — choose
the shape that reads naturally next to the other four."* The implementer chose a nullable `encodedBytes`
on the shared `LocalStateResult.Success`, which is built in six places, five of which cannot populate it —
and then needed four `assertNotNull` calls in the tests to restore the guarantee the type had just lost.

That is precisely the pattern wave A's note §3 recorded from item 007's nullable `Applied.record`, and
wave A's §4 already said *offering a choice is not neutral*. **The lesson was written down and then
repeated anyway, in the very next wave, by the person who wrote it down.** Wave C's briefs state
decisions; they do not offer shapes.

## 6. Two Compose-only slices, declared in advance, and it held

Both items' slice 3 carried no JVM test, and both slice plans said so **at design time** rather than
discovering it at review. That framing worked: neither slice was argued about, and both were verified by
`assembleDebug` plus a walkthrough instead.

Item 009's slice 3 passed first time with no findings, and the reason is worth naming: **the IO-dispatch
hazard exposed by reviewing slice 2 was written into slice 3's brief before it was dispatched.** Review of
one slice fed the next slice's brief rather than a later findings round. That is cheaper than any
follow-up and should be deliberate in wave C.

## 7. Tooling notes for wave C

- **A count read from disk after a failed build is a lie.** A failed `testDebugUnitTest` leaves the
  *previous* run's XML in place, and the reviewer's summing command happily reported "179 tests, 0
  failures" for a build that had not compiled. Delete `app/build/test-results/testDebugUnitTest` before
  re-gating, and read the `BUILD SUCCESSFUL` line, not just the counts.
- **A PR with no checks is not a PR that passed.** PR #12 initially produced *zero* workflow runs — not
  even `test.yml`, which has no path filter and fires on every PR. Read "no checks" as "never ran", and
  re-trigger with a close/reopen, which preserves the head SHA.
- **`gh` had two accounts authenticated** and the active one lacked push rights to this repo. `gh auth
  switch --user irodriguez-io` before pushing.
- **A Codex session can come up behind a hooks-review overlay that swallows the prompt**, reporting
  `done` while having received nothing. A pane reporting done with no commits on the branch means read the
  pane, not the status.
- Briefs go to Codex through a quoted heredoc and `"$(cat file)"`. Sent inline, backticks in the prompt
  are shell-interpreted and identifiers are silently stripped before the implementer sees them.
- The emulator lost network after a resume and could not refetch a dataset, which is why 009's walkthrough
  was performed on the pre-rebase build and the merged build was not re-walked.

## 8. For wave C specifically

Wave C is **005 preference learning, then 006 deck diversity**, and `execution-model.md` §5 already calls
005 "one review larger than all of wave A combined". Nothing in wave B contradicts that.

- **005 must reconcile records that arrived by import**, not only records the device created, and in both
  directions. `specs/009-android-import-export/design.md` D1 sets out the mirror case: an Android
  `MARK_UNREAD` on an imported browser record clears the flag while leaving the weight stranded. Wave B's
  walkthrough produced a real exported document containing a record claiming `"read": true` with
  `preferences` empty, which is the hazard made concrete.
- **008 shipped Undo's producer**, so `contracts.md` §23's reversal guard is now reachable from the UI.
  005 introduces the weight arithmetic behind it. The undo path currently asserts `preferences` is
  byte-identical across an undo; that assertion becomes the thing 005 has to change deliberately.
- **Item 012 is queued** — moving Discover's operational header below the card — and it needs an amendment
  to `06-ui-ux.md` §21. It is small but it is a specification change, so it gets designed, not folded in.
