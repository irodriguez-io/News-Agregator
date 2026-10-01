# 027 — design

One page. Two decisions; the first is the owner's.

## D1 — The incoming destination covers the outgoing one (owner, 2026-10-01)

**Chosen:** give the incoming destination an opaque background, so it slides *over* the outgoing one.
**Why:** it is the smallest change that makes overlapping text impossible rather than fainter, and it keeps
§79.1's character — one page arriving over another that recedes. It adds one rule to §79.1 and changes none
of its values.

**Rejected:**

| Option | What the reader would see | Why not |
|---|---|---|
| Outgoing fades to `0` instead of `0.8` | Pages stay see-through; the old text fades out as the new slides in | Text still overlaps mid-fade, as a ghost. Changes a §79.1 value. |
| Both | Opaque incoming, and the uncovered strip fades away too | Two rule changes where one removes the overlap. Revisit only if the walkthrough finds the receding strip itself reads as "both at once". |
| Material shared axis | Both pages slide together; old fades out early, new fades in late | Replaces §79.1's motion wholesale rather than correcting it. |

**The background is `LocalIntentionalReadingTokens.current.bg`, read inside the `AnimatedContent` child**,
not a captured colour. That is the same value the `Scaffold`'s `containerColor` reads (`:216`), so at rest
the two are indistinguishable, and during an appearance change both follow §79.4's blended tokens in the
same frame. A literal or a `MaterialTheme.colorScheme` colour would break that and is not acceptable.

**Where it goes:** on the `AnimatedContent` child — one `Box(Modifier.fillMaxSize().background(bg))` around
the `when (targetDestination)` — not inside the three screens. The screens are out of scope (`spec.md` §4),
and one wrapper means a fourth destination cannot forget it.

**Z-order needs no change.** `AnimatedContent` draws the target content after the initial content, so the
incoming destination is already on top; it was simply transparent. If the implementer finds otherwise on
device, that is a stop condition, not a `zIndex` to add silently.

## D2 — Extract the transition so the test can see through it

A pixel test against the real app has to tell Discover's text from Read Later's text, which is fragile. So
the `AnimatedContent` block moves, unchanged, into an `internal @Composable` in `ui/DestinationTransition.kt`
(beside `destinationSlideDirection`, which it already uses) that takes `destination`, `reducedMotion` and a
`content: @Composable (Destination) -> Unit` slot. `IntentionalReadingApp` calls it with today's `when`.

The test then hosts it with synthetic content: the outgoing destination is a full-size solid magenta box,
the incoming one is empty. Part-way through, any magenta inside the area the incoming destination has slid
over is bleed-through. Before the fix that pixel is magenta at 80% over `bg`; after, it is `bg`. The host
takes `reducedMotion = { false }` directly, so the test needs no `animator_duration_scale` change — which
matters because CI runs with animations disabled (`android.yml:83`).

**The extraction is behaviour-neutral and lands in the RED commit** with the test, so RED compiles and fails
on the assertion, not on a missing symbol. The GREEN commit is then the background alone, and its diff
shows that.
