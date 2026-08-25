# Wave B — Swipe gestures and import/export

**Items:** 008, 009 · **Prerequisite:** wave A merged, 007 in particular · **Cut from:** `main`

Self-contained brief. Read `AGENTS.md`, `docs/v1/README.md`, `specs/backlog.md`,
`specs/execution-model.md`, then this file.

---

## Why these two together, and why after A

Disjoint from each other — 008 is the Discover gesture surface, 009 is Settings plus the state file. Both
need 007 merged: **008 because a mis-swipe with no Undo is worse than no swipe**, and 009 because it
edits `AppViewModel` after 007 has.

**Dispatch order:** both at once. **Sequential fallback:** 009 → 008 (009 is larger).
**Merge order:** 008 → 009.

---

## 008 — Swipe gestures

Port `js/ui/swipe.js`: the 90px threshold, the intent lock, the rotation.

**Deferred by** 002 §3, restated by 004 §3.

**This is enrichment, not a compliance gap.** The labeled buttons are the only triage affordance on
Android today, which is the compliant direction — `DESIGN.md:8` requires a labeled equivalent for every
gesture, never the reverse. Nothing is broken without it. Scope it accordingly and do not let it grow.

**Expected surface:** `ui/components/ArticleCard.kt`, `ui/screens/discover/DiscoverScreen.kt`, a new
gesture module under `ui/`.

**Design-time questions:**

- **The 90px threshold is a CSS pixel value.** Android's equivalent is `dp`, and they are not the same
  thing. Decide whether parity means the number or the physical distance, and record why.
- **Compose gesture handling versus the existing scroll.** 002 already hit a Discover card scroll defect
  on the emulator; a horizontal drag detector inside a vertically scrollable card is exactly where that
  class of bug lives.
- **Accessibility.** Every swipe keeps its labeled button. TalkBack must not lose an action, and the
  gesture must not become the only path to anything.
- **Undo's reach.** Every swipe-triggered action must be undoable through 007's slot. If it is not, the
  swipe should not exist.

**Verification is substantially emulator-only.** A JVM test can cover the threshold arithmetic; it cannot
tell you the gesture feels right or that it fights the scroll.

---

## 009 — Import and export

Port `exportState`/`importState` (`js/state/storage.js:303-345`) with the Storage Access Framework
surface: a document picker and creator, the 5 MiB cap (`05-personalization-state.md:1023-1048`), atomic
replacement (§49), replacement-not-merge (§50), and the import validator's own error surface.

**Deferred by** 002 §3 and 003 §3, restated by 004 §3. `future-items.md` §"The item that ports import and
export" is required reading before designing this — it is the long-form version of the hazard below.

**Cheaper than it looks:** `LocalStateValidator` is already ported and `LocalStateFile` already writes
atomically. This item is largely the wrapper and the SAF surface.

**Expected surface:** `ui/screens/settings/SettingsSheet.kt` (the local-data section, mirroring
`js/ui/settings.js:221-282`), `MainActivity.kt` or a launcher composable for
`ActivityResultContracts.OpenDocument` / `CreateDocument`, new IO under `data/`,
`res/values/strings.xml`.

**The inherited hazard, which the spec must address explicitly:** an Android-read article exported and
imported into the web client, then marked unread, decrements weights the browser never incremented —
because 003's records carry derived `signalsApplied` flags with no deltas behind them. It requires a
deliberate user action and cannot happen while the two stores stay separate, which they do today. This
item is what makes them not separate. Decide and record: warn, block, sanitize on export, or accept.

**Non-negotiables from the specification, not the implementer's judgment:** the 5 MiB cap, atomic
replacement, and **replacement, not merge**. An import that merges is a defect even if it looks kinder.

**Also design deliberately:** what a failed import leaves behind. `08-security-dependencies.md:629-640`
makes explicit reset the only recovery path out of corrupt stored state, and an import that half-lands
manufactures exactly that state.

---

## Gates

Per `execution-model.md` §8. Both items fire `android.yml` only.

---

## Owner checkpoints

1. **The 009 export/import hazard decision** — warn, block, sanitize, or accept. A product call, not an
   engineering one. Ask during design; it shapes the scenarios.
2. **A device pass on 008** — whether the swipe feels right, and whether it fights the card scroll.
   `adb` can drive a synthetic swipe; it cannot judge one.
3. **A real SAF round trip on 009** — export to Drive or Files, reboot, import back. Worth doing on
   hardware rather than only on the emulator, since SAF provider behaviour differs.
4. **Wave sign-off** against merged `main`.

---

## Definition of wave done

Both merged to `main`; `evidence.md` per item; walkthroughs run against merged `main` and recorded;
`backlog.md` updated; a wave note. **Wave C is 005 running alone** — confirm before starting it that
nothing in this wave left `AppViewModel.kt` in a state 005 has to unpick first.
