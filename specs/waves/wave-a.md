# Wave A — Undo, launch theme, web validator parity

**Items:** 007, 010, 011 · **Prerequisite:** none — 004 is merged · **Cut from:** `main`

Self-contained brief. A fresh session should be able to run this wave from `AGENTS.md`,
`docs/v1/README.md`, `specs/backlog.md`, `specs/execution-model.md`, and this file, with no prior
conversation.

---

## Why these three together

Three disjoint file columns (`execution-model.md` §2): 007 is `AppViewModel` plus the Discover surface,
010 is the launch theme and manifest, 011 is `js/**` plus one Kotlin string. Nothing to coordinate
between them beyond the merge order.

**007 is the long pole and the reason this wave is first.** `contracts.md` §23 makes Undo Not Interested
and Undo Save read the `signalsApplied` reversal guard that 005 will introduce. Built now, while
`preferences` is always empty, Undo is pure state-machine inversion. Built after 005, it is the hardest
item in the backlog.

**Dispatch order:** 007 first, so it gets the most runway; 010 and 011 alongside it.
**Sequential fallback order** (`execution-model.md` §5): 007 → 010 → 011.
**Merge order:** 011 → 010 → 007. The two small ones land first so 007 rebases once, late, rather than
twice.

---

## 007 — Undo

Port the single-slot undo manager and its toast, `js/state/article-state.js:154-231`. No Discover action
on Android offers Undo today.

**Deferred by** 002 §3, restated by 004 §3.

**Expected surface:** `ui/AppViewModel.kt` (the undo slot),
`domain/state/ArticleStateMachine.kt` (inverse transitions), the announcement plumbing already in
`ui/IntentionalReadingApp.kt:80-104`, `res/values/strings.xml`.

**Design-time questions the spec must settle, not the implementer:**

- **Which actions offer Undo, and from which surfaces.** The browser is the authority; do not invent.
- **How Undo interacts with `_heldArticleId`.** Undoing a dismiss from Discover has to put the reader
  back on a card. `clearHeldArticleIfNeeded()` (`AppViewModel.kt:410-415`) releases the pin the moment a
  status leaves `OPENED`, and an Undo that re-enters `OPENED` has to re-establish it or the reader is
  bounced to a different article.
- **Whether the undo slot survives process death.** It is in-memory in the browser. Say so explicitly
  either way; do not leave it to be discovered.
- **Whether the existing 6-second announcement timer** (`IntentionalReadingApp.kt:80-87`) is the right
  toast vehicle, or whether Undo needs an actionable surface of its own. This is the one place where 007
  might legitimately need new UI rather than reuse.

**Out of scope:** swipe gestures (that is 008), any preference-weight reversal (there are no weights yet
— `signalsApplied` stays derived exactly as 003 left it).

---

## 010 — Launch theme

Android paints the launch window before Compose starts, so the pre-Compose frame can flash light while
the stored appearance is Dark. D8's no-flash gate covers every *composed* frame; this is the frame before
them.

**Recorded by** 003 §Outstanding, untouched by 004.

**Expected surface:** `res/values/themes.xml`, `AndroidManifest.xml:20`, `MainActivity.kt`.

**Resolve at design time, before the implementer is dispatched:** a proper launch theme probably wants
`androidx.core:core-splashscreen`. `AGENTS.md` and 004 §3 both bar new Android dependencies without
explicit approval, and `android/gradle/libs.versions.toml` was deliberately untouched by 004. **Either
get the owner's approval or specify a no-dependency solution.** Do not let this reach the implementer
unresolved.

The hard part is not the plumbing, it is that no static `windowBackground` can be correct when the stored
appearance is unknown at launch. If the chosen approach cannot actually be correct in all three
appearance settings, say so in the design note and scope it to what it can guarantee.

**Verification is emulator-only.** No JVM test observes a pre-Compose frame. Budget the walkthrough
accordingly: cold start in each of the three appearance settings, against a system theme set the other
way.

---

## 011 — Web validator parity and shared copy

Three defects found by Android ports reading the browser source, none of them Android's to fix.

**Raised by** 002 §Outstanding and slice 2 observation 6.

- `js/data/validation.js:145` accepts `readingTimeMinutes >= 1`; `pipeline/validation.py:81-83` requires
  ≥ 2. The pipeline is stricter than its own client validator. Verified still present 2026-08-25.
- `js/data/validation.js:148-163` enforces no `tags` length limit, where `contracts.md` §7 limits
  organically detected tags to five.
- `js/ui/discover.js:330` renders "1 more choice wait quietly behind this one." — singular noun, plural
  verb. Ported verbatim to `Labels.kt:46` for parity, which was correct.

**Fix the copy in both clients in this item or in neither.** A unilateral divergence invents a
requirement, and the whole reason the bug is in `Labels.kt` is that 002 refused to invent one.

**Expected surface:** `js/data/validation.js`, `js/ui/discover.js`, `tests/js/**`,
`ui/format/Labels.kt` (one line) and its frozen-copy assertion. Note that `Labels.DEGRADED_NOTICE` has a
frozen-copy assertion pattern already (004, `bcc18f1`); the same discipline applies here — the assertion
stays an exact-string assertion, it just gets a new exact string.

**Check first, then decide:** if `readingTimeMinutes: 1` or a six-tag article cannot occur in a dataset
the pipeline actually emits, this is a defence-in-depth fix rather than a live bug. Say which it is in
the spec. It does not change whether to fix it; it changes how the scenarios are written.

**Out of scope:** `pipeline/**` and `config/**`. The pipeline is the correct one of the two.

---

## Gates

Per `execution-model.md` §8. 007 and 010 fire `android.yml`; 011 fires both workflows. Green hosted CI on
the exact final head is required before any final-mode review merges.

---

## Owner checkpoints

The orchestrator drives the emulator over `adb` (`execution-model.md` §6). The owner is needed for:

1. **The 010 dependency decision** — approve `androidx.core:core-splashscreen`, or require a
   no-dependency approach. **Blocks 010's dispatch**, so ask early, in the design pass.
2. **The 011 copy fix** — the corrected sentence is authored copy in a product with a deliberate voice.
   Propose the wording; let the owner settle it.
3. **A visual pass on 010** — whether the launch frame reads as seamless is a judgment `adb` cannot make.
4. **Wave sign-off** against merged `main`.

---

## Definition of wave done

All three merged to `main`; `evidence.md` written per item; walkthroughs run against merged `main` and
recorded; `backlog.md` updated — items moved to Shipped, 010 struck from Debt, 011's three defects struck
from the queue; and a wave note recording what the concurrency actually cost, so wave B can be planned
against evidence instead of this document's optimism.
