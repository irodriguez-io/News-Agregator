# Wave D — specification amendments 7 and 8

Two amendments, drafted at wave D's design pass on 2026-08-31 and **approved at the plan gate the same
day**. They are
**documents work, owned by the orchestrator**; no implementer edits `docs/v1/**`
(`AGENTS.md`; `docs/v1/README.md` §14).

`docs/v1/README.md` §14 requires a specification change to (1) identify the conflict, (2) identify affected
documents, (3) define the new decision, (4) update all affected specifications, (5) inform all affected
workstreams, (6) **be committed before dependent implementation continues.** This file is steps 1–3 and the
exact text for step 4. Commit order:

| Amendment | Must be committed before | Blocks |
|---|---|---|
| **7** — Discover Composition Ordering | item 012's branch is cut | 012 |
| **8** — Undo Scope: Reversible Actions and Offer Surfaces | item 014's branch is cut | 014 **and** 016 |

Both land in the same `docs(spec)` commit as this wave's four item specifications, on `docs/wave-d-e-plan`
(PR #19), so that `main` carries them before any wave-D branch exists.

---

# Amendment 7 — Discover Composition Ordering

## 1. The conflict

`06-ui-ux.md` §21 opens *"Discover begins with an editorial header area"*, which is an ordering rule. The
owner's requirement, from testing wave B's build on 2026-08-26, is the opposite ordering: **the card should
be the first thing in the viewport, not the thing you scroll to.**

§21 already lists the category selector and the available-article context among what the header *may*
include, so which widgets exist is not in dispute. Only their position is.

## 2. Affected documents

- `docs/v1/README.md` — the amendment record.
- `docs/v1/06-ui-ux.md` §21.

Not affected: `contracts.md` (no state, contract, status, signal or action changes), `06-ui-ux.md` §23
(one-primary-card rule, untouched), §22 (the selector's own options and control height, untouched), and every
other destination's header.

## 3. The decision

> **Approved Amendment 7, `Discover Composition Ordering`,** changes the ordering rule in `06-ui-ux.md` §21
> so that Discover's decision surface leads the viewport. A compact masthead — a small metadata eyebrow and
> the screen title — remains the first thing on Discover. The operational block — concise purpose copy, the
> refresh affordance, content-age and failed-refresh disclosure, available-article context, and the category
> selector — follows the article card rather than preceding it.
>
> **The intent is binding and the widget order is illustrative: on Discover, the first thing in the viewport
> is the article card, not the controls that describe it.** A later redesign may re-lay out Discover freely
> provided that intent is preserved.
>
> This amendment changes no contract, no state, no status, no learning behaviour, no copy and no action
> semantics. It does not alter §23's one-primary-card rule, it does not change what the header may contain,
> and it applies to Discover only — Read Later and History keep their full editorial headers. It is a
> placement rule and nothing else.

## 4. The text

`06-ui-ux.md` §21, replacing the section body between the heading and the `---`:

```markdown
# 21. Discover Header

Discover's header is split, and the article card sits between the two halves.

The **masthead** comes first and is deliberately compact:

- small metadata/eyebrow;
- application/screen title.

The **operational block** follows the article card:

- concise purpose copy;
- refresh affordance and content-age context;
- failed-refresh disclosure;
- available/current article context;
- category selector.

The binding rule is the ordering intent, not the widget list:

```text
the first thing in the viewport on Discover is the article card,
not the controls that describe it
```

The masthead should feel like publication masthead/content framing rather than dashboard chrome, and framing
placed below its subject is still framing. A redesign may re-lay out either half provided the card still
leads the viewport.

*Amended by Amendment 7.*
```

---

# Amendment 8 — Undo Scope: Reversible Actions and Offer Surfaces

## 1. The conflict

Two conflicts, in the same sentences.

**(a) Surfaces.** Undo is scoped to the *swipe gesture* in five places: `contracts.md` §31
("only the most recent eligible **swipe** action must be retained"), `05-personalization-state.md` §36
("only the most recent successful Discover **swipe** action"), `06-ui-ux.md` §70 ("offered **only** for the
most recent eligible Discover **swipe**"), `01-product.md` §14 ("the **most recent swipe action**") and
`09-testing-acceptance.md` §50. Items 007 and 008 implemented that faithfully and recorded the rationale —
Undo recovers a mis-*trigger*, and a press on a button labelled "Save for later" is not one
(`specs/007-android-undo/spec.md` §1.1; `specs/008-android-swipe-gestures/design.md` D8).

The owner's finding, from the running app, is that this makes reversibility depend on which finger movement
started an action rather than on what the action does — so the same save is reversible by swipe and
irreversible by button, one control apart on the same card.

**(b) Actions.** `05-personalization-state.md` §36 says Undo is *not required* for Open, Mark Read, Mark
Unread, Remove, import, reset or appearance changes, and its first line scopes it to `dismiss` and `save`
only. The owner reproduced by hand on 2026-08-31 that Read Later's *Mark read* and *Remove* and History's
*Mark unread* offer no reversal, and asked for them.

These are one conflict, not two, because §31, §36 and §70 each state both the surface rule and the action
rule in a single sentence. Editing them twice would leave `docs/v1/**` self-contradictory in between.

## 2. Affected documents

- `docs/v1/README.md` — the amendment record.
- `docs/v1/contracts.md` §23 (reversible learning rules — three rules added) and §31 (Undo Contract).
- `docs/v1/05-personalization-state.md` §36 (Undo Scope) and §41 (Undo Lifetime, one clause).
- `docs/v1/06-ui-ux.md` §70 (Toast and Live Status, one sentence). §45's toast strings are labelled
  *"Examples"* and remain illustrative, so new toast copy needs no amendment.
- `docs/v1/01-product.md` §14 (Undo).
- `docs/v1/09-testing-acceptance.md` §48, §49 (the swipe-only framing of the Save and Dismiss undo tests)
  and §50 (Undo Scope Test).
- `docs/v1/01-product.md` §5's Discover capability list, one bullet — *"undo the most recent swipe action"*.
- `docs/v1/workstreams/state-ranking.md` §42, `docs/v1/workstreams/integration.md` §23 and
  `docs/v1/workstreams/final-review.md` §23 — historical browser implementation briefs. Each gains a
  one-line pointer to this amendment rather than a rewrite (§14 step 5, *inform all affected workstreams*).

Not affected: `contracts.md` §21 (the deltas and the clamp are unchanged), §22 (idempotency unchanged), §24
(Remove applies no negative signal — this amendment depends on it and does not change it), §25 (opening
behaviour), and `05-personalization-state.md` §37–§40 (persistence and the Save/Dismiss reversal rules, all
unchanged).

## 3. The decision

> **Approved Amendment 8, `Undo Scope: Reversible Actions and Offer Surfaces`,** widens V1 Undo along two
> axes and states the arithmetic for the widened set.
>
> **Surfaces.** An eligible action may raise the undo offer from **any surface that performs it**. The
> trigger — a swipe, a labelled control, or a keyboard shortcut where one exists — does not determine
> reversibility; the action does. This supersedes the swipe-scoped wording in `contracts.md` §31,
> `05-personalization-state.md` §36, `06-ui-ux.md` §70, `01-product.md` §14 and
> `09-testing-acceptance.md` §50, and supersedes the conclusions recorded in `specs/007-android-undo/spec.md`
> §1.1 and `specs/008-android-swipe-gestures/design.md` D8.
>
> **Actions.** The reversible set becomes:
>
> ```text
> save
> dismiss
> mark read
> mark unread
> remove from read later
> ```
>
> **Open is not reversible** and does not become so. Import, reset and appearance changes are not reversible
> and continue to clear the undo record.
>
> **Arithmetic.** Reversing an action restores the exact pre-action record
> (`05-personalization-state.md` §38) and:
>
> - **Undo Remove** applies **no** preference change in either direction, because Remove applies none
>   (§24). The restored record is the `saved` record it replaced.
> - **Undo Mark Read** reverses the Read signal **only if the forward action applied it**, and restores
>   `signalsApplied.read` from the restored record.
> - **Undo Mark Unread re-applies** the Read signal that Mark Unread reversed, and restores
>   `signalsApplied.read = true`. This is the only place in V1 where reversing an action applies a signal
>   rather than reversing one, and it exists because Mark Unread is itself a corrective action (§23).
>
> **Scope of the record.** Still exactly one undo record, still memory only, still cleared by reload, import
> and reset (§31, `05-personalization-state.md` §37). Any eligible action replaces it, regardless of which
> destination performed it. Because the record names an article, an offer raised on one destination remains
> valid if the reader changes destination inside the offer's lifetime.
>
> **Reach.** This amendment is **permissive, not obligatory**. It authorises the wider scope; it does not
> require every client to implement it. The browser's existing scope — swipe and keyboard triggers, `save`
> and `dismiss` only — remains compliant, and no `js/**` change is required by this amendment. The Android
> client implements the wider scope under items 014 and 016.
>
> No delta, clamp, status value, eligibility rule, idempotency rule or storage contract changes.

## 4. The text

### 4.1 `contracts.md` §23 — three rules appended before the section's `---`

```markdown
### Undo Mark Read

If the Mark Read action applied a Read signal:

```text
reverse read signal
```

and decrement corresponding interaction counts. If it applied none, reverse nothing.

The exact previous record is restored, including `signalsApplied.read`.

### Undo Mark Unread

Mark Unread is itself a corrective action. If it reversed a Read signal, undoing it **re-applies** that
signal:

```text
apply read signal
signalsApplied.read = true
```

and increments corresponding interaction counts. If Mark Unread reversed nothing, undoing it applies
nothing.

The article returns to:

```text
status = read
```

This is the only reversal in V1 that applies a signal rather than reversing one.

### Undo Remove from Read Later

Remove applies no preference signal (§24), so undoing it applies and reverses **nothing**.

The exact previous record is restored:

```text
status = saved
```

*Amended by Amendment 8.*
```

### 4.2 `contracts.md` §31 — one line replaced

```diff
-Only the most recent eligible swipe action must be retained.
+Only the most recent eligible action must be retained, regardless of which surface or destination
+performed it. The reversible set is defined in §23.
```

### 4.3 `05-personalization-state.md` §36 — section body replaced

```markdown
# 36. Undo Scope

V1 Undo supports the most recent successful **eligible action**, from any surface that performs it:

```text
save
dismiss
mark read
mark unread
remove from read later
```

The trigger does not determine reversibility. A swipe, a labelled control and a keyboard shortcut are
equivalent for this purpose.

Undo is **not** available for:

- Open;
- import;
- reset;
- appearance changes.

Reversal arithmetic for each eligible action is in `contracts.md` §23.

*Amended by Amendment 8.*
```

### 4.4 `05-personalization-state.md` §41 — one clause

```diff
-- another eligible swipe replaces it;
+- another eligible action replaces it;
```

### 4.5 `06-ui-ux.md` §70 — one sentence

```diff
-Undo is offered only for the most recent eligible Discover swipe and remains available for approximately the approved toast duration.
+Undo is offered for the most recent eligible action, from whichever surface performed it, and remains available for approximately the approved toast duration. The eligible set is in `contracts.md` §23.
```

### 4.6 `01-product.md` §14 — two lines

```diff
-V1 supports undo for the **most recent swipe action**.
+V1 supports undo for the **most recent eligible action**, however the reader triggered it.

 Undo may reverse:

 - a dismissal;
 - a Save for Later action;
+- a Mark Read action;
+- a Mark Unread action;
+- a removal from Read Later.
```

and, after *"Only the most recent eligible action needs to be undoable."*, unchanged.

### 4.7 `09-testing-acceptance.md` §50 — section body replaced

```markdown
# 50. Undo Scope Test

Only the most recent eligible:

```text
save
dismiss
mark read
mark unread
remove
```

action must be undoable, and it must be undoable from whichever surface performed it.

Open must not be undoable.

Reload/session reconstruction must not preserve Undo.

*Amended by Amendment 8.*
```

### 4.8 `01-product.md` §5, `09-testing-acceptance.md` §48 and §49 — the residual swipe-only framing

```diff
-- undo the most recent swipe action.
+- undo the most recent eligible action.
```

```diff
-After Save swipe then Undo:
+After a Save then Undo, from any surface that performed it:
-After Dismiss swipe then Undo:
+After a Dismiss then Undo, from any surface that performed it:
```

The assertions in §48 and §49 themselves are unchanged — only the trigger they name.

### 4.9 Workstream pointers

One line appended to `workstreams/state-ranking.md` §42, `workstreams/integration.md` §23 and
`workstreams/final-review.md` §23:

```markdown
> Amendment 8 widens the eligible set and removes the swipe-only restriction. The browser's existing
> swipe-and-keyboard scope remains compliant; see `contracts.md` §23 and §31.
```

---

## 5. What these amendments deliberately do not do

- **They do not require the browser to change.** Amendment 8 is permissive by owner decision, 2026-08-31.
  Web parity, if it is ever wanted, is a separate numbered item.
- **They do not touch the deltas, the clamp, idempotency, eligibility or the storage contract.** The
  arithmetic in §23 is derived from `contracts.md` §21, §22 and §24 as they stand.
- **They do not make Open reversible**, and they do not make import, reset or appearance changes reversible.
- **They do not add a keyboard shortcut to Android.** 008 D8 declined it for a device with no hardware
  keyboard and that stands.
- **Amendment 7 does not survive as a layout.** It survives as one sentence, on purpose, because wave E
  re-lays out Discover.
