# 011 — Design note

Short, because two of the three changes are one expression each. It exists for D1, which corrects the
brief this item was dispatched from, and for D3, which is the reason this item spans two trees.

## Workstream role

This is the only item since 002 that is **not** `android-client`. It owns `js/**` and `tests/js/**`,
plus exactly one line of `android/…/ui/format/Labels.kt` and the assertion that pins it.

Forbidden: `pipeline/**`, `config/**`, `docs/v1/**`, `css/**`, `index.html`, `scripts/**`, and every
Android path except `Labels.kt` and its test. `AGENTS.md`'s rule that the runtime frontend stays HTML,
CSS, and vanilla JavaScript binds here directly, and nothing in this item comes close to it.

## D1 — The tag cap is six, and the dispatching brief said five

`specs/backlog.md` §011 and `specs/waves/wave-a.md` §011 both derive a five-tag cap from
`contracts.md` §7. The derivation drops the qualifier "organically detected".

The pipeline reads §7 correctly and in two steps:

```python
# pipeline/taxonomy.py:38-40   — the §7 limit, applied to organic matches
article["tags"] = [ {...} for topic, _, _ in matches[:5] ]

# pipeline/taxonomy.py:58-70   — forced tags appended on top
for topic_id in source["forcedTags"]:
    if topic_id in present: continue
    article["tags"].append({"id": topic_id, "label": topic["label"]})
```

Hence `pipeline/validation.py:95`:

```python
if len(tag_ids) != len(set(tag_ids)) or len(tag_ids) > 6:
```

Six is 5 + 1, and the 1 is bounded by configuration rather than by convention:
`pipeline/configuration.py:183-185` compares the whole forced-tag map to a frozen constant, and
`config/sources.json` carries three sources with one forced tag each against seventeen with none. A
source cannot acquire a second forced tag without failing config validation.

**So the client cap is six.** Implementing the brief's five would refuse a document the pipeline can
legitimately publish — trading a permissive validator for an incorrect one. The brief's instruction to
"check first, then decide" is what surfaced this; the check is recorded in `spec.md` §1.2 so the next
reader of `backlog.md` does not re-derive five.

**Follow-on for the backlog, not for this item:** `backlog.md` §011's third bullet should be corrected
to say six when this item is marked Shipped. That is bookkeeping at wave close, not a scope change.

## D2 — Both validator changes are single-expression, and neither invents an error shape

`js/data/validation.js` already has the vocabulary for both:

```js
// :144-145  — expectInteger's third argument is the minimum
readingTimeMinutes = expectInteger(candidate.readingTimeMinutes, `${path}.readingTimeMinutes`, 1)
//                                                                                             ^ becomes 2

// :148      — the array check gains a length guard alongside it
if (!Array.isArray(candidate.tags)) fail(`${path}.tags`, "must be an array");
//                                  ^ a sibling fail(...) for length, in the same style
```

`fail(path, message)` is the established failure shape and both changes use it verbatim. No new error
code, no new message vocabulary, no change to `DatasetError` or to how `js/data/articles.js` surfaces a
refusal. `contracts.md` §13's dataset-loader contract is unaffected: a refused dataset is refused the
same way it always was.

The duplicate-id check at `:150-153` stays exactly as it is. The length guard is additional, not a
replacement.

## D3 — One string, two clients, two frozen-copy assertions

The browser builds the sentence inline (`js/ui/discover.js:330`):

```js
`${Math.trunc(remaining - 1)} more ${remaining - 1 === 1 ? "choice" : "choices"} wait quietly behind this one.`
```

The noun is already pluralised correctly; only the verb is wrong, and only in the singular case. The
fix has to move the verb into the same conditional as the noun. Android's `Labels.kt:44-48` is already
shaped that way — two separate branches — so its change is one literal.

**The two counts are equivalent and there is no off-by-one to worry about.** The browser passes
`remaining - 1` where `remaining` is `deck.length` (`js/app.js:229`), and Android's
`DiscoverDeck.remainingCount` is `eligible.size - 1` (`DiscoverDeck.kt:29`). Both branch on the
already-decremented value, and both render nothing at zero. Verified rather than assumed, because a
copy fix that silently rested on an off-by-one would be worse than the grammar error.

**The Android assertion stays an exact-string assertion.** Item 004 established the pattern at
`bcc18f1` and it survives in `UiStateMapperTest.kt:309`:

```kotlin
assertEquals("Some sources were unavailable when this content was gathered.", Labels.DEGRADED_NOTICE)
```

It gets a new exact string, not a looser matcher. A regex, a `contains`, or a "starts with the count"
assertion would defeat the entire purpose — the assertion exists so that changing the copy on one
client and not the other fails a build.

The browser needs the equivalent. `tests/js/discover.test.js` is where it goes, and it asserts the
rendered text of the side note for both the singular and the plural case, as exact strings.

## D4 — Sequencing inside wave A

This item is first in the wave's merge order (`waves/wave-a.md`), and it is genuinely independent:
`js/**` and `tests/js/**` collide with nothing else in flight, and `Labels.kt:46` is a line no other
wave-A item reads or writes — 007 touches `AppViewModel` and `ArticleStateMachine`, 010 touches
resources and `AppContainer`. Merging it first costs the other two branches one rebase each against a
diff that cannot conflict with theirs.

It is also the only wave-A item that fires `test.yml` as well as `android.yml`, so it is the one whose
final review has two green workflows to confirm rather than one.
