# 020 — design note

Five decisions, two of which are refusals to decide.

---

## D1 — The null reading-time rule is not decided here. It is preserved.

**Decision.** Leave the omission logic exactly as it is, and assert it.

**Why.** `wave-e.md` asks this item to decide the null presentation. §29 and §54 already decided it — omit,
never `0 min`, never estimate — and `StatBand.kt:75–77` already implements it. There is nothing to decide.

**Why it still gets a scenario.** A re-layout is exactly when a working omission rule gets replaced with a
tidier-looking formatted zero. The scenario is a guard, not a specification.

---

## D2 — Whether this is a two-gate item is decided, and the answer is no

**Decision.** One gate surface, `android.yml`. **Conditional on no new user-facing string.**

**Why.** `wave-e.md` defers the call to this design pass. Every string §63's high-fidelity empty state needs
already exists — `history_empty_title`, `history_empty_copy`, `go_to_discover`, and the Read Later pair.
§63's "high fidelity" is visual weight applied to existing copy.

**The condition is load-bearing.** §75.2 makes any new string shared copy, which pulls in `js/**` and its
validators. So `res/values/strings.xml` appearing in this item's diff is a **stop**, and §5.1's diff check
is written to surface it.

---

## D3 — The four shared-directory files are this item's

**Decision.** This item owns `ArticleRow.kt`, `EditorialHeader.kt`, `EmptyStatePanel.kt` and `StatBand.kt`,
against the wave brief's matrix.

**Why.** All four are called only from Read Later and History. Leaving them with item 018 would mean 018
restyling four components it cannot see in context, and this item re-laying out two screens around
components it may not touch.

Recorded on both sides: `018/spec.md` §1.2 excludes them, this spec §1.1 claims them.

---

## D4 — The toast overlap is fixed with an inset, not by touching the toast

**Decision.** Add bottom content inset to both lists so the last row clears a showing Undo offer. **Do not
touch `UndoToast.kt` or its hosting in `IntentionalReadingApp.kt`.**

**Why.** The toast is hosted globally so that an offer raised on one destination survives a destination
change (§70, Amendment 8). Moving or re-parenting it to solve a list-layout problem would put this item
inside item 021's file and inside wave D's ground, and would risk the cross-destination guarantee.

The defect is that the list ends underneath the toast. The list owns its own bottom inset.

**Stop condition if this proves wrong:** if the overlap cannot be fixed from inside these screens, report
rather than reaching into the toast.

---

## D5 — The row spends the thumbnail's width on type

**Decision.** No thumbnail, no reserved region; the freed width goes to the headline and metadata, and the
row is laid out so a media slot could be introduced later without a re-layout.

**Why.** §74.2, and the same obligation §74.2 places on item 019's card. As there, "reads as deliberate
rather than as missing" is not testable and is an explicit owner judgment at walkthrough.

---

## What this note does not decide

**Any value.** Every count, sum, topic and date is unchanged; every colour, radius and type style comes from
017. **Any behaviour.** Mark read, Remove, Reopen, Mark unread, their undo paths and their count updates are
untouched.
