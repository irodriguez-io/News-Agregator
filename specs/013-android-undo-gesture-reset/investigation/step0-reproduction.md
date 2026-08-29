# 013 Step 0 — the no-Undo reproduction (2026-08-27)

`spec.md` §5.1 required the defect to reproduce with **no Undo involved** before any fix was written,
on the grounds that §1.2's diagnosis predicts it must. It does. The diagnosis survives its own
falsifiable test.

Run on `Pixel_10` (`emulator-5554`) against branch head `f1f381b`, product code unchanged.

```sh
adb -s emulator-5554 shell \
  "input swipe 800 1300 180 1300 100; sleep 0.35; input swipe 800 1300 180 1300 100"
```

- Head card: "Are We Thinking Correctly About AI Intelligence?" (`quanta`), true bounds
  `[47,731][1033,1986]` from `uiautomator dump`.
- Incoming card: a `w3c_webauthn` article, settled bounds `[47,482][1033,1986]`.
- Both swipes used `y=1300`, inside both cards' bounds. Coordinates were read from the dump rather than
  fixed, per §5.4.

Confirmed against the pulled state document, not by the card leaving the deck:

| | before | after |
| --- | --- | --- |
| records | 13 | 14 |
| `sources.quanta` | 0.90 / 2 | 0.55 / 3 |
| `topics.physics_quantum` | 0.30 / 1 | 0.10 / 2 |
| `sources.w3c_webauthn` | 0.45 / 1 | **0.45 / 1** |

Exactly one record was added (`d20d4d04831977ce501e`, dismissed). The second card's source weight and
interaction count did not move at all. **The second swipe was discarded.**

Artifacts in this directory: `step0-state-before.json`, `step0-state-after.json`,
`step0-uiautomator-before.xml`, `step0-uiautomator-after.xml`.

## Why the item stopped here

§5.3's instrumented RED could not be obtained. Seven pre-fix harness variations — paused `mainClock`,
raw `MotionEvent` injection, the real `DiscoverScreen` scroll path, and composition- and layout-bound
injection — all **passed** against unchanged production code, so none of them was a genuine fail-first.
Per §5.3 and D3 the implementer stopped and reported rather than weakening the test; no fix was written
and nothing was committed.

The likely reason is structural: the window exists because the pointer handler's launch is starved by a
real frame clock contending with the D12 scroll, and `ComposeTestRule` is built to synchronize exactly
that away — it idles until composition and its coroutines settle before injecting. §5.3's assumption
that `mainClock` "makes the timing deterministic" is what makes the window unobservable. That is a flaw
in the verification design, not in the diagnosis.

Owner decisions of 2026-08-27, taken on this evidence:

1. **Amend §5.3 to assert the cause rather than the symptom** — the instrumented RED becomes "the
   pointer handler is not re-launched when the head article changes", which does fail first today.
   Symptom coverage moves to this reproduction plus §5.4's walkthrough.
2. **Split the "button press immediately after the deck changes" scenario into its own item.** §1.3
   records that Save for later fails in the same window without ever consulting the gesture state, so
   D1's persistent-handler fix would not address it; it has a different mechanism and gets diagnosed on
   its own terms.

Both are specification changes, so the item returns to design before implementation resumes.
