# Wave D — batched walkthrough for items 015, 014 and 016

Driven 2026-08-31 against **merged `main` at `d249bc0`** (the `android/` tree at the head of
`docs/wave-d-close` is byte-identical to `d249bc0`; verified with `git diff --stat d249bc0 95f5c38 -- android/`,
empty). Emulator `Pixel_10`, APK `:app:assembleDebug` installed with `adb install -r` so **app data was
preserved**.

Item 012's walkthrough was driven at its slice 2 and is recorded in
`specs/012-android-discover-card-first/walkthrough/`. It is not repeated here.

## Against real accumulated history, not fresh state

`waves/wave-d.md` owner checkpoint 3. The device carried state from the day's earlier walkthroughs and it
was **not** reset:

| | Articles | Statuses | Source weights | Topic weights |
|---|---|---|---|---|
| At the start | 35 | 29 dismissed, 4 saved, 2 read | 11 | 20 |
| At the end | 51 | 45 dismissed, 2 saved, 4 read | 13 | 22 |

`state-00-baseline.json` and `state-99-final.json` are the endpoints. Every step below was scored by
**pulling the state document and comparing entry for entry, keyed by article id, source id and topic id** —
never by a count, never by "a weight moved" (`waves/wave-d.md`; item 015's trap).

## Results

| # | Step (`016/spec.md` §6.3) | Result |
|---|---|---|
| 1 | Read Later → **Remove**, then Undo | **PASS** — `3024289593e2fb5b7909` back to `saved`, record **byte-identical**, whole preferences map unchanged in both directions |
| 2 | Read Later → **Mark read**, then Undo | **PASS** — `3024289593e2fb5b7909` back to `saved`, record byte-identical, `science_aaas` back to `-1.9 / 13` |
| 3 | History → **Mark unread**, then Undo | **PASS — the re-application is correct.** See below. |
| 4 | Discover → **Mark read** on an opened card, then Undo | **PASS** — `d5e7959aa0306d0c6343` back to `opened`, **First Open still applied**, `cloudflare_blog` and both topics unchanged |
| 5 | **Cross-pane** | **PASS** — see below |
| 6 | Let an offer **expire**, Read Later and History | **PASS** — both actions stood, nothing reversed, no weight moved |
| 7 | Read the three new **toast strings** on screen | **All three rendered.** Copy is the owner's call — see *Open for the owner* |
| 8 | *And is what the reader needs next on screen?* | See *Observations* |

Plus the two items batched in:

| Item | Check | Result |
|---|---|---|
| **014** | Discover's **Save for later** button raises an undo offer | **PASS** — `21-014-saveforlater.png`. This is the defect 014 fixed: the button used to commit the save silently. |
| **015** | A swipe immediately after Undo is attributed to the article the reader saw | **PASS**, 4 clean race runs — see below |

### Step 3 — the re-application, the step most likely to be wrong

`016/spec.md` §2.2. Undoing `MARK_UNREAD` must **re-apply** the Read signal, not reverse anything. It is the
only place in V1 where reversing an action applies a signal, and if it were wrong the record would claim
`signalsApplied.read = true` while the weight stayed subtracted — silently.

Article `e5a5c441204348ce65b7`, source `openid_specs`, topics `federation` and `oidc`:

| | status | `read` | `openid_specs` | `federation` | `oidc` |
|---|---|---|---|---|---|
| armed | `read` | true | 0.10 / 5 | 0.55 / 3 | 0.15 / 5 |
| after **Mark unread** | `saved` | false | −0.15 / 4 | 0.35 / 2 | −0.05 / 4 |
| after **Undo** | `read` | **true** | **0.10 / 5** | **0.55 / 3** | **0.15 / 5** |

Record restored **byte-identical**; the whole sources map and the whole topics map identical, entry for
entry. **The weights went back up.** The forward deltas also match `contracts.md` §21 exactly: source
+0.25, each topic +0.20.

### Step 5 — cross-pane

Mark **A** read in Read Later → switch to History → mark **B** unread → Undo. A and B carry distinct
sources and topics.

- **B** `e5a5c441204348ce65b7` (`openid_specs`, `federation`, `oidc`) — restored byte-identical, every weight
  back to its pre-run value.
- **A** `6fac3b29bd2f156fe56d` (`ieee_spectrum`) — stayed `read`; `ieee_spectrum` moved **−0.95 / 5 →
  −0.70 / 6**, i.e. **+0.25 and one interaction, exactly once**. The undo did not touch it.
- **Only `ieee_spectrum` moved at all.** No topic moved. No double-apply, no double-reverse.

The sibling scenario was also driven separately: **the offer survives the reader changing destination** —
A marked read in Read Later, destination switched to History, Undo tapped there, A restored byte-identical.

### Item 015 — the race

Swipe → Undo → swipe again at **~40–50 ms**, which is inside the window 013 measured. Seven attempts; in the
four where the Undo tap landed (runs 1, 2, 5, 7), **exactly one article changed and it was the head that was
on screen** — the second swipe hit the article Undo restored, not the one that was leaving. In the three
where the Undo tap missed, two articles were dismissed and **each dismissal was still attributed to whatever
card was showing**.

**Zero misattributions in seven attempts.** Scored by article id every time, per the wave's rule that
anything not naming the id is not evidence here.

## Observations

**The layout that 012 delivered holds up.** The Discover card leads the viewport at every step and the
operational block sits below it; the action rail was reachable without scrolling on this device throughout.

**Step 8, honestly answered: yes, with one exception.** After Undo the restored card is on screen with its
actions reachable, and after each Read Later or History action the pane re-renders with the next row in
place. The exception is that **the Undo toast overlaps the bottom row's action controls** while it is
showing — on Read Later and History the toast sits directly over the last visible row's Read/Mark read/Remove
rail. It clears itself in 4.5 s and no action is lost, but a reader aiming at the bottom row during the
window will hit the toast instead. Not a defect against any specification; noted because it is only visible
from the device.

## Open for the owner

1. **The three toast strings**, all confirmed rendering (screencaps in this directory):
   - `Marked as read` — `09-step2-toast.png`
   - `Returned to Read Later` — `13-step3-toast.png`
   - `Removed from Read Later` — `03-remove-035.png`

   `06-ui-ux.md` §45 labels its toast strings *"Examples"*, so new copy needs no amendment. **The wording is
   your call.** One note in their favour: *Returned to Read Later* describes what actually happened —
   `MARK_UNREAD` moves the article to `SAVED` (`contracts.md` §23) — rather than restating the button label.

2. **Wave sign-off** against merged `main` at `d249bc0`.

## Method notes, for the next wave

Confirming `wave-c-note.md` §6 and adding to it:

- **`uiautomator dump` cannot see the Undo toast at all.** `screencap` at 0.35 s shows it plainly. Every
  toast in this run was captured that way, with the tap and the capture inside **one on-device `adb shell`**.
- **The toast's Undo target moves with the message width.** Measured on this device: `Not interested`
  x≈692, `Marked as read` x≈738, `Returned to Read Later` x≈773, `Removed from Read Later` x≈802, all at
  y≈2087. A fixed coordinate will silently miss, and a missed Undo looks exactly like a passing run unless
  you score by article id.
- **Re-locate before every tap — including after an action, not just after a scroll.** This cost three
  retries here. A card's controls moved from y=1554 to y=1412 after an unrelated action, and the cross-pane
  sequence failed twice on coordinates that were correct one screen earlier.
- **Card and row action controls sit ~110–150 px above their text labels**, and the nav bar's clickable
  nodes are `[0,2151][346,2361]` and `[734,2151][1080,2361]` — nowhere near the label bounds.
- **Allow ~1.4 s after an action before changing destination.** At 0.4 s the pane switch raced the
  recomposition and the action never committed.
