# 019 — design note

Four decisions.

---

## D1 — The clamp is the mechanism that closes item 012's finding

**Decision.** Apply §13.2's three-line headline clamp and two-line excerpt clamp, and treat the 360 dp
above-the-fold measurement as this item's definition of done rather than as a nice-to-have.

**Why.** Item 012 proved that no arrangement of the *surrounding layout* fixes the fold problem, because
the card's height is unbounded in its title. Only bounding the title bounds the card. 012 said so, said it
would need an amendment against §25, and handed the finding here; §13.2 is that amendment.

**What is given up, stated plainly:** a reader no longer sees the whole of a very long title on the card.
The full title is on the publisher's page, which is one tap away and is where they are going anyway (§50).

**Assert the clamp on the text element, not through the screen.** Item 006's lesson: a property asserted
through the algorithm that consumes it produces a test the next item cannot satisfy.

---

## D2 — Adopt 018's controls; do not restyle the inline ones

**Decision.** Replace the three inline action-rail treatments with calls to item 018's shared controls.

**Why.** 018 shipped them unconsumed precisely so this item could adopt them. Restyling the inline versions
instead would leave two treatments of the same control in the tree permanently, rather than for the length
of one item.

**Risk this creates, and the mitigation.** This item depends on 018's API. 018 is designed and its
signatures are known, but it has not been implemented when this plan is written. **Do not freeze 018's
parameter names in this plan** — `slices.md` states the dependency and requires it to be re-read at
preflight, per §2.1 rule 5.

---

## D3 — Amendment 7 is preserved by asserting it, not by remembering it

**Decision.** Keep item 012's composition-order tests green rather than re-writing the layout and
re-checking by eye.

**Why.** Amendment 7 grants a redesign the licence to re-lay out Discover *provided the card still leads
the viewport*. The failure mode is not disagreement — it is a re-layout that quietly reverts the ordering
while nobody is asserting it. 012's tests are the guard, and this item's job is to leave them green.

---

## D4 — The headline occupies the media slot; nothing is reserved

**Decision.** No image area, no placeholder, no reserved height. The Playfair headline starts where the
design's 16:9 slot would have been.

**Why.** §74.2. `ArticleDataset v1` has no image field and adding one is a frozen-contract change.

**Obligation this carries** (also §74.2): lay the card out so a media slot could be introduced later
without a re-layout. And because "reads as deliberate" is not testable, it is an explicit owner judgment at
walkthrough.

---

## What this note does not decide

**Any value** — all from 017. **Any behaviour** — swipe, threshold, cues, commitment, undo, deck order and
counts are untouched; only presentation changes.
