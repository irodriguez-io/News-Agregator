# Wave E — specification amendment 9

One amendment for the whole wave, drafted at wave E's design pass on 2026-09-01. It is **documents work,
owned by the orchestrator**; no implementer edits `docs/v1/**` (`AGENTS.md`; `docs/v1/README.md` §14).

`docs/v1/README.md` §14 requires a specification change to (1) identify the conflict, (2) identify affected
documents, (3) define the new decision, (4) update all affected specifications, (5) inform all affected
workstreams, (6) **be committed before dependent implementation continues.**

| Amendment | Must be committed before | Blocks |
|---|---|---|
| **9** — Android Client Visual Direction: Material 3 Expressive | item 017's branch is cut | **017, 018, 019, 020, 021** — the whole wave |

Step 4 is **already performed**: `docs/v1/06-ui-ux.md` has been rewritten as its second edition in the same
change as this file. This file is steps 1–3, 5, and the record of the owner decisions the design pass
produced.

---

# Amendment 9 — Android Client Visual Direction: Material 3 Expressive

## 1. The conflict

`06-ui-ux.md` was a single visual specification binding **both** V1 delivery surfaces. It was authored for
the browser runtime; Amendment 6 then admitted a native Android client as a second surface **without giving
it a visual direction of its own.** Wave E introduces one, and it contradicted the shipped document in
fifteen places.

Six of those were genuine contradictions rather than value changes:

| Shipped rule | The Android design sources | Why it is a contradiction |
|---|---|---|
| §13 "never force article titles to one line"; §25 no "arbitrary hard two-line clamp" | headlines clamp at 3 lines | the rule forbids exactly what the design requires |
| §26 excerpt "up to 4 lines at normal mobile widths" | descriptions clamp at 2 lines | same |
| §15, §57 Read Later/History rows use **rules, not floating-card containers** | Queue Row is a 16dp tonal-filled container | the row both is and is not a container |
| §18 Discover sits "approximately 7px higher" | M3 Expressive bar, uniform baseline, pill indicator | the lift and the pill are alternative ways to mark the same thing |
| §8 "navigation selection uses primary ink rather than accent colour" | active nav pill is a tonal container | same |
| §37 pressed controls move ~1px downward | pressed is a 12% overlay with a 0.95 scale-down | same |

The remaining nine were value replacements: the colour system and its tokens (§4–§7), accent usage (§8),
typography and its scale (§11–§12), spacing (§14), radii (§15), shadows (§16), the Settings control's form
(§20), control treatments (§32–§35), and motion values (§44, §46).

### 1.1 The design sources also disagree with each other, and one disagrees with itself

Neither Android source could be cited as-is. `m3-expressive-DESIGN.md` carries a YAML token block **and** a
prose section naming different values, and `m3-expressive-PRD.md` §8 carries a third, smaller set. Three
colours the prose relies on for its most visible components — the tonal fill, the nav pill indicator, the
chip outline, the StatBand — **appear in no token block at all.**

Without a precedence rule, item 017 could not define a token set and items 018–020 would each have resolved
the same conflict differently. The rule is now fixed in `06-ui-ux.md` §2.2 and the prose colours are mapped
in §77.3.

### 1.2 What was never in conflict, and was being treated as though it were

- **§24's "no reserved image area exists"** and **§74's imagery prohibition** already agreed with this
  wave's imagery decision. The design sources' media slots are the deviation.
- **§29 and §54 already settled** the null reading-time question `wave-e.md` asked item 020 to decide.
  Item 020 cites them; it does not decide them.
- **§53 already authorised** a three-part overview band, so the StatBand is not the "dashboard metric grid"
  §74 prohibits.
- **§39–§43, §45, §48–§51** carry behaviour and nothing in this wave touches them.

## 2. Affected documents

- `docs/v1/README.md` — the amendment record and the `06-ui-ux.md` descriptor, §3 below.
- `docs/v1/06-ui-ux.md` — **rewritten as its second edition.**
- `docs/v1/09-testing-acceptance.md` — **three acceptance criteria scoped by surface.** See 2.1.

**Not affected.** `contracts.md` — no contract, schema, status, signal, action or storage change.
`01-product.md`, `05-personalization-state.md` — no state, ranking, count, learning or undo change;
**Amendments 7 and 8 survive intact.** `03-content-sources.md`, `04-taxonomy-scoring.md`,
`07-pipeline-deployment.md` — untouched. `docs/v1/workstreams/frontend-ui.md` — it restates browser visual
values, and its `Primary ownership` line (`index.html`, `css/**`, `js/ui/**`) already scopes it to the
browser unambiguously, so no change is needed. **`js/**`, `css/**` and every web asset — no change is
authorised or required.**

### 2.1 `09-testing-acceptance.md` was not unaffected, and the first draft of this amendment said it was

The draft claimed no acceptance criterion changed. That was wrong, and it was caught by checking the claim
rather than restating it. Three criteria were written with the browser's numbers and would have **accepted
an Android build that violates the new specification**:

| Criterion | The defect | Now |
|---|---|---|
| §72 Touch-Target Acceptance | accepts a `44px` compact action; §72.2 requires `48dp` for every Android element without exception, so the weaker criterion would have passed a non-compliant build | scoped per surface, plus a new §72.1 requiring the control-boundary contrast floor to be **measured** |
| §73 Responsive Matrix | lists ten browser widths including four the Android client does not ship, and does not mark 360 as non-optional | scoped per surface; 360 marked non-optional on Android with item 012's finding cited |
| §74 Theme Acceptance | did not say both schemes are mandatory on both surfaces | states it |

**An acceptance document that is weaker than the specification is worse than one that contradicts it**, because
the contradiction gets argued and the weakness just passes.

## 3. The decision

> **Approved Amendment 9, `Android Client Visual Direction: Material 3 Expressive`,** gives the native
> Android client a visual direction of its own, distinct from the browser runtime's. Amendment 6 admitted
> the Android client as a separate delivery surface consuming the frozen dataset contract read-only; this
> amendment completes that split on the visual axis.
>
> **Reach.** The new direction binds the **Android client only.** The browser runtime's visual
> specification is unchanged in substance, remains binding on it, and is **not** made non-compliant by this
> amendment. No `js/**` or `css/**` change is authorised or required. The web client is not scheduled for
> redesign; if it ever is, that is its own amendment.
>
> **Mechanism.** `06-ui-ux.md` is **rewritten as a second edition** that scopes every rule at the point
> where it is stated. Each section opens with a `Binds:` line naming the surfaces it governs; sections
> where the two surfaces genuinely differ carry both treatments side by side. **Section numbers are
> unchanged**, so every existing citation of the document continues to resolve to its subject, and §80
> records what changed inside each section. Sections 76–79 are new and carry Android presentation the first
> edition had no home for.
>
> **Behaviour is never scoped.** Every rule about what the application does — state transitions, counts,
> signals, action semantics, gesture outcomes, keyboard bindings, live status, undo, accessibility — binds
> both surfaces without exception. Only presentation is ever split. **No behaviour change is authorised:**
> not a state transition, status value, learning signal, delta, clamp, count, ranking, deck ordering, undo
> path, undo arithmetic, gesture semantic, keyboard binding, or authored string. **Amendment 7's placement
> rule and Amendment 8's undo scope both remain binding.** A wave-E item that finds itself changing
> behaviour has a scope defect, not a licence.
>
> **Precedence among the Android design sources** is fixed in §2.2: the DESIGN token block for values, the
> PRD for component states, motion and IA, DESIGN's prose illustrative only. Two seed values are settled by
> owner decision as named exceptions, recorded in §77.2.
>
> **Colour is authored as ten seeds and derived in Oklch** (§77, §78), widening the browser's six-seed rule
> in count and keeping it in kind. **No component outside the theme package may name a colour.**
>
> **The dark scheme is derived from the same seeds**, not separately authored (§77.5), and was approved with
> the palette. `06-ui-ux.md` §64 and item 010 shipped a working Light/Dark/System switch; a light-only
> palette would have regressed it.
>
> **Fonts are bundled as `res/font/` assets** (§11.2). This adds no Gradle dependency and fetches nothing at
> runtime. `androidx.compose.ui.text.googlefonts` is **not** authorised.
>
> **The information architecture does not change:** three destinations — Read Later, Discover, History, in
> that order — plus a Settings modal overlay that is not a fourth tab.
>
> **Imagery stays out** (§74.1, §74.2). `ArticleDataset v1` carries no image field; adding one is a
> frozen-contract change with its own workstream ahead of it. Components are laid out so imagery could be
> added later without a re-layout.
>
> **Reduced motion continues to bind** (§48): every animation this wave introduces must honour it, and a
> test must assert it.

## 4. Owner decisions recorded at this design pass

All four were taken on 2026-09-01 after review of a rendered specimen of every candidate palette at
390 dp in both schemes.

| Decision | Chosen | Where it landed |
|---|---|---|
| Reach of the new direction | **Android only**; the browser keeps its direction | §0, §3 above |
| Precedence among the design sources | DESIGN tokens > PRD > DESIGN prose | §2.2 |
| Shape of the change to `06-ui-ux.md` | **Full rewrite** with numbering preserved, rather than a supersession table | §0, §80 |
| Dark-scheme method | Derived from the same seeds by Oklch, not hand-authored | §77.5 |
| `primary` seed | **`#1B2CC1`** over `#00129A` | §77.2 — a **named exception** to the precedence rule |
| `tonal` seed | **`#ABD2FA`** over `#7692FF` | §77.2 — a **named exception**, and the better value on contrast |

**The two seed exceptions are exceptions, not a repeal.** The precedence order in §2.2 continues to govern
every value it does not name.

### 4.1 Why the rewrite needed a replacement control

A supersession table would have kept the change additive, which would have made "no behaviour changed"
checkable from the diff alone. A full rewrite moves every line and loses that check —
`execution-model.md` §2.1 exists because three items shipped defects that no diff read caught.

**`06-ui-ux.md` §80 is the replacement control.** It records, section by section, whether the rule is
unchanged, split by surface, widened, or moved. A reviewer verifies behaviour invariance against that table
rather than against the diff. Every one of the 26 sections cited by another document was checked to still
carry its original subject before the rewrite was installed.

## 5. Three defects the design pass found by computing contrast rather than looking

All three are the same shape — **a control whose only boundary is a low-contrast outline** — and all three
looked acceptable on screen. WCAG SC 1.4.11 requires 3:1 for a boundary that identifies a control.

| Component | Candidate value | Measured | Resolution |
|---|---|---|---|
| 56 dp triage buttons, 1.5 dp ring | `#7692FF`, the design's own prose value | **2.9:1 — fails** | the `secondary` seed `#3856BF` at 6.5:1 (§35.2) |
| unselected category chip, light | a hairline one lightness step off the card | **1.6:1 — fails** | the `border` seed at 4.5:1 (§78.3) |
| unselected category chip, dark | the `border` seed | **1.4:1 — fails**, the seed is darker than the card | the `muted` seed at 6.6:1 (§78.3) |

The fix is not three values but **one rule**: `06-ui-ux.md` §73.1 states the floor, and §78.3 splits the
single outline token into a **decorative hairline** exempt from it and a **control boundary** derived to
clear it in each scheme independently.

**Two derivation bugs were also found and fixed in the same pass**, both of which would otherwise have
reached `Tokens.kt`:

1. **Mixing a saturated blue toward pure white in Oklch produced olive.** White's hue is a rounding
   artefact; interpolating toward it drags the result off-hue. The content-type badge came out `#E4E6D3`
   instead of a pale blue. §78.1 now requires a near-achromatic endpoint to adopt the other endpoint's hue.
2. **The shipped `mix(fg 30%, border)` rule does not survive the palette change.** It was correct for the
   old light `#CFD6E2` border seed and *darkens* the new `#757686` seed into a heavy rule. Replaced by
   §78.3's symmetric lightness-step derivation.

## 6. Informing the workstreams — step 5

| Item | Cites | Owner checkpoint it carries |
|---|---|---|
| 017 | §11.2, §12.2, §14.2, §15.2, §16.2, §76.1, §77, §78 | palette and dark scheme — **already approved**; 017 implements the recorded values |
| 018 | §18.2, §20.2, §22.2, §32.2–§35.2, §37.2, §72.2, §76.3–§76.5 | walkthrough at merge |
| 019 | §13.2, §24, §26.2, §27.2, §28.2, §76.2; **Amendment 7**; item 012 §1.4's 360 dp finding | walkthrough at merge |
| 020 | §52.2, §56.1, §58.2, §63, §76.6; §54/§55/§61's null rules — **already settled** | walkthrough at merge; whether shared copy makes it a two-gate item |
| 021 | §79; §44's character constraint; §47; §48 | walkthrough at merge |

**No wave-E item amends `docs/v1/**`.** An item that believes it needs to reports to the supervisor.

**§78.3's control-boundary derivation must be asserted by a test**, not implemented by eye. It is the one
piece of 017's output that three separate candidate values got wrong.

## 7. What this amendment deliberately does not resolve

**Whether item 020 is a two-gate item.** It depends on whether History's empty state (§63) needs a new
shared string, and `wave-e.md` requires that decided at 020's design pass. §75.2 states the constraint and
leaves the call there.
