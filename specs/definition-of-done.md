# Definition of done

**Adopted:** on merge of this document — owner-approved at the PR.
**Binds:** what this project still builds. Not `docs/v1/**`: this is a tracking rule, like `backlog.md`, and it
invents no product requirement. Its acceptance criteria are the ones `docs/v1/09-testing-acceptance.md`
already states.

---

## 1. Why this exists

The browser client has an end state: `09-testing-acceptance.md` §82 (release gate) and §83 (V1 completion).
**The Android client never got one.** Amendment 6 admitted it as *"delivered incrementally under numbered
specification items"* and stopped there, so every walkthrough that found something produced another item,
and nothing said when to stop. Items 022, 023 and 024 were each found by the one before. This document is
the end state.

## 2. The project is done when

1. **Every item on the closed list** in `backlog.md` §"Road to done" is shipped or decided, and nothing else
   is on it.
2. **The hosted gate is trustworthy.** `android.yml` and `test.yml` pass on `main` without a re-run, on the
   last three merges in a row. (§82: *automated deterministic tests pass*. A gate that needs re-running is not
   deterministic — see item 025.)
3. **§82's release gate holds for the Android client**, evidenced once, at the end, on a signed release
   build:
   - the §73 responsive matrix at all five Android widths (`360 390 430 600 768` dp);
   - §72 touch targets, §75 reduced motion, §76 live status and a **TalkBack pass** for the accessibility
     acceptance;
   - §81 manual product acceptance — editorial, text-first, finite, intentional; truthful counts; opening
     content leaves for the publisher.
4. **§83 holds for the browser client**, confirmed by the existing gates on `main`, not re-audited.
5. **Every owner decision on the list has been made and written down**, whether or not it is built.
6. **The known limitations are written down** in one place — the final acceptance evidence — rather than
   left as open debt.

When all six hold, the owner declares done. Work after that is a new version, not a continuation.

## 3. The freeze rule — how the list stays closed

From adoption, **nothing joins the "Road to done" list** unless it is a **severity-blocking defect**: it
breaks a rule `docs/v1/**` already states, or it loses or corrupts the reader's data. Everything else — polish,
a walkthrough finding that is not a spec violation, a better idea, debt — goes to **After done**, and is not
worked.

**The test for a new finding is one question: does a binding section of `docs/v1/**` already say this is
wrong?** If yes, it is a defect, and it joins the list. If it would need an amendment to become a
requirement, it is a new requirement, and it waits.

The owner may override the rule by name at any time — *"add X to the list"* — and the override is recorded
beside the item. Claude does not add items on its own judgement.

## 4. What "done" does not mean

- Not that `backlog.md`'s Debt section is empty. Debt is absorbed when something next edits the same file;
  after done, it is After-done work.
- Not that every walkthrough is perfect. It means the final acceptance pass found no severity-blocking
  defect.
- Not Play Store publication, which the owner declined.
