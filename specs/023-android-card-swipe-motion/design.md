# 023 — design note

Warranted: this item authors motion the specification is silent about, and it is the fourth consecutive item
on a surface whose defects have historically been invisible to the JVM gate. D2 is the load-bearing
decision and is the reason this item is small.

---

## D1 — The M3 equivalent duration is `300ms`, and it is recorded here because §44.2 delegates it

§44.2 names the easing — Material 3 Emphasized — and says *"at the M3 equivalent duration"* without giving a
number. That delegation has to be discharged somewhere, and an implementer is the wrong place.

**`300ms`.** Reasons, in order:

1. §79.1 already uses `300ms` on M3 Emphasized for the destination transition. One Emphasized duration
   across the client is the consistency §44.2 asks for by name (*"for consistency with the rest of the
   Android motion system (§79)"*).
2. It is an M3 duration token (`medium2`), which `280ms` is not.
3. It is 20 ms longer than the browser's, which is within the tolerance §44's character constraint cares
   about and moves in the direction of *controlled* rather than *hurried*.

The exit **distance** does not change (`spec.md` §4), so a 300 ms exit is very slightly slower per pixel
than today's. That is the intended direction.

## D2 — No `AnimatedContent`, because the exit and the entrance are already sequential

The obvious reading of this defect is "wrap the card in `AnimatedContent` so the outgoing and incoming
overlap." **That is the wrong fix here, and the reason is item 015.**

Read `ArticleCard.kt:145-155`. The commit is launched *after* the exit animation has completed:

```kotlin
restoreScope.launch {
    gesture.animateToGestureState()          // exit runs to completion
    currentOnSwipeCommit(gesture.article, articleAction) { … }
}
```

So the head article does not change until the outgoing card has finished leaving. There is never a moment
when both articles are on screen, and the replacement's `remember(article.id, …)` rebuild happens after the
old card is gone.

`AnimatedContent` would *create* the overlap that does not currently exist, and with it two composed cards
each carrying a live `pointerInput`. That reopens exactly the question item 015 was raised to settle — which
article a touch belongs to — and item 013's *"there is no window in which the card declines a touch"* would
have to be re-established against a card that is mid-exit. **A fix that reintroduces two of this surface's
three historical defects to solve a cosmetic one is a bad trade.**

The entrance is therefore an ordinary property animation on the single card that is already composed. One
card on screen, one gesture owner, 013 and 015 untouched by construction.

## D3 — The entrance is `alpha` and `translationY` on the card's existing `graphicsLayer`

`ArticleCard.kt:178-181` already has a `graphicsLayer` block writing `translationX` and `rotationZ`. The
entrance adds `alpha` and `translationY` to the same block, driven by one `Animatable` that starts at `0f`
and animates to `1f` when the composable is keyed onto a new `article.id`.

```text
entrance   300ms, Material 3 Emphasized Decelerate
opacity    0 → 1
rise       12dp below resting position → 0
lateral    none
scale      none
rotation   none
```

**Decelerate, not the exit's curve.** The card is arriving and settling; §44's *controlled* reads as
something that comes to rest rather than something that is thrown. §79.2 already uses a decelerated curve
for the same reason on the sheet.

**`12dp` is deliberately small.** §23 forbids deck gamification and §47 forbids continuous card movement;
a large rise would read as a card being dealt. The owner chose "rise and fade in place" over the lateral
and promote-from-depth options precisely to stay on this side of that line.

**It rides the existing `remember(article.id, …)` key.** That key is what currently causes the replacement
to be constructed at rest; the same key is the signal that a new article has arrived. No new state plumbing.

## D4 — The entrance must not gate input, and that is a test, not a comment

Item 013's settled behaviour is that a card accepts a swipe as soon as it is on screen. The entrance runs on
`graphicsLayer` properties only, which are **draw-time** transforms: they change what is painted, not what
is hit-tested or composed. The `pointerInput` modifier is live from the first composition regardless of
`alpha`.

So the property is satisfied by construction — **and it is still tested**, instrumented, because
`backlog.md` records that this surface's defects have repeatedly been ones a green JVM gate could not see,
and "satisfied by construction" is the kind of claim that stops being true after an unrelated refactor.

One consequence to hold: a card at `alpha = 0f` on its first entrance frame is invisible but swipeable. That
is correct under 013 and is not a defect to be fixed by delaying input.

## D5 — The exit fade is one line in the block that already exists

`alpha` in the same `graphicsLayer`, animated to `0f` across the exit alongside the existing translate and
rotate, matching `js/ui/swipe.js:114`. It joins `animateToGestureState()`'s existing `coroutineScope`
launch set; the exit remains one coordinated animation with one completion point, which is what keeps D2's
sequencing intact.

## D6 — Reduced motion, matching 021 and 022

`reducedMotion` is already threaded and already branches in this file — `ArticleCard.kt:103-110` selects a
`0`-duration tween today. The entrance takes the same shape: `snap()` under the preference, so the card is
opaque and at rest on its first frame.

Under reduced motion the exit already sets `exitTranslationX = 0f` (`SwipeGesture.kt`), and the fade must
follow the same branch — no fade, not a fast fade. §48 says *effectively immediate*.

## D7 — The total gesture is now about 600 ms, and the walkthrough judges it

**Corrected 2026-09-23, after the walkthrough. This decision's prediction was tested and did not survive
it**, and the original text is kept below so the correction is visible rather than silent — the treatment
D8 in this file already received.

**What still stands.** Exit `300ms`, then the state action, then entrance `300ms`, sequential by D2, so they
add. §43 says the active card *"exits briefly"* and sets no budget for the whole sequence. Recording a
budget here so that a "feels slow" finding has a named cause rather than a re-design was the right instinct.

**What was wrong: the named lever.** D7 predicted that a sluggish verdict would point at the **entrance
duration**. The walkthrough returned a sluggish verdict on 2026-09-22 and **the entrance duration is not the
lever.** Neither animation is too long. The cost is the roughly half a second of *nothing* between them:
`ArticleCard.kt:146-150` awaits `animateToGestureState()`, and only then does `onSwipeCommit` run the
transition, `saveLocalState` — a disk write — and `adoptPersistedState`'s full deck re-rank, all on
`Dispatchers.Main.immediate`. The head article changes after that, and the entrance starts after that.

**Acting on D7 as written would make the defect worse.** A shorter entrance leaves the dead gap untouched
while making it a larger share of the total wait, and weakens the only motion that tells the eye a card
arrived. The real lever is the commit path's sequencing — which is **D2**, this item's central constraint,
so it is a design pass and not a value change. `evidence.md` §4 carries the full finding.

**Why this correction exists at all.** D8 was wrong about a file and was corrected at implementation. D7 was
wrong about a prediction and was *not* corrected at the walkthrough, so for one merge it stood as live
guidance to the next item on this surface. That is the same failure `evidence.md` §1 names as this item's
transferable lesson — **a claim written when true and stale when used, with no gate that distinguishes the
two** — reappearing in the one file that had already learned it.

---

### The original text, superseded

> Exit `300ms`, then the state action, then entrance `300ms`. Sequential by D2, so they add.
>
> §43 says the active card *"exits briefly"* and sets no budget for the whole sequence. 600 ms is longer
> than today's 280 ms-plus-nothing, and it is the price of the replacement arriving rather than
> materialising. **This is the first thing to look at if the walkthrough finds the swipe sluggish**, and the
> lever is the entrance duration, not the exit — the exit is fixed by §44.2 and D1.
>
> Recorded here so that a "feels slow" finding has a named cause and a named lever rather than a re-design.

## D8 — No new dependency, and no new constant file

**Corrected 2026-09-22, at implementation. Both of this decision's factual claims were wrong, and the
original text is kept below so the correction is visible rather than silent.**

The decision itself stands: no new dependency, no new file, and the two new values (entrance duration,
entrance rise) join `SwipeGesture`'s constant block, which is the JVM-testable home for this surface's
numbers. Cite §79.5.

**What was wrong:**

1. *"`Animatable`, `tween`, `snap` and the M3 easing set are already imported in this file."*
   `ArticleCard.kt:3-7` imported `Animatable`, `AnimationSpec`, `AnimationVector1D`, `CubicBezierEasing`
   and `tween` — **not `snap`, and no M3 easing of any kind.** Slice 1 added the `snap` import and slice 2
   added `LinearOutSlowInEasing`.
2. *"…already carries a citation per constant."* `SwipeGesture.kt:6-13` was **eight bare `const val`
   declarations with no comments at all.** There was no citation style to match, so slice 1 established one
   from `ui/theme/Theme.kt:60-61`, the closest precedent in the codebase.

**Neither error changed the decision, and both cost a dispatch.** The first implementer session refused to
start on them and was right to. **The transferable lesson is in `evidence.md` §1:** a design note's factual
claims about a file are exactly the claims that go stale between the design pass and the implementation, and
no gate distinguishes a claim that was true when written from one that was never true.

**What D8 did not anticipate, and slice 1 had to decide:** §44.2 names "Material 3 Emphasized" and this
codebase had no shared definition of it. Item 021's `PathEasing` at `ui/IntentionalReadingApp.kt:271-279` is
the only one, and duplicating it into `SwipeGesture.kt` was chosen over editing 021's file, which is outside
this item's boundary. **The easing is therefore defined twice, deliberately and disclosed** — see
`evidence.md` §2.

---

### The original text, superseded

> `Animatable`, `tween`, `snap` and the M3 easing set are already imported in this file. The two new values
> (entrance duration, entrance rise) join `SwipeGesture`'s existing constant block, which is already the
> JVM-testable home for this surface's numbers and already carries a citation per constant. Cite §79.5.
