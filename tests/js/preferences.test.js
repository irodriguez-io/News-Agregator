import test from "node:test";
import assert from "node:assert/strict";

import {
  INTERACTION_DELTAS,
  applyInteraction,
  reverseInteraction,
} from "../../js/state/preferences.js";
import { makeArticle } from "./helpers.js";

const expected = {
  open: [0.1, 0.05],
  save: [0.45, 0.3],
  dismiss: [-0.35, -0.2],
  read: [0.25, 0.2],
};

test("preference events use the exact V1 deltas for source and every unique topic", () => {
  assert.deepEqual(INTERACTION_DELTAS, {
    open: { source: 0.1, topic: 0.05 },
    save: { source: 0.45, topic: 0.3 },
    dismiss: { source: -0.35, topic: -0.2 },
    read: { source: 0.25, topic: 0.2 },
  });

  for (const [event, [source, topic]] of Object.entries(expected)) {
    const preferences = applyInteraction(
      { sources: {}, topics: {} },
      makeArticle({ tags: [{ id: "oauth", label: "OAuth" }, { id: "oauth", label: "OAuth" }] }),
      event,
    );
    assert.deepEqual(preferences.sources.source_one, { weight: source, interactions: 1 });
    assert.deepEqual(preferences.topics.oauth, { weight: topic, interactions: 1 });
    assert.equal("categories" in preferences, false);
  }
});

test("preference operations are immutable and create missing entries lazily", () => {
  const before = { sources: {}, topics: {} };
  const after = applyInteraction(before, makeArticle(), "save");
  assert.deepEqual(before, { sources: {}, topics: {} });
  assert.notEqual(after, before);
});

test("source and topic weights clamp to both V1 bounds", () => {
  const high = { sources: { source_one: { weight: 4.9, interactions: 8 } }, topics: {
    distributed_systems: { weight: 4.9, interactions: 8 },
    software_architecture: { weight: 4.9, interactions: 8 },
  } };
  const low = { sources: { source_one: { weight: -4.9, interactions: 8 } }, topics: {
    distributed_systems: { weight: -4.9, interactions: 8 },
    software_architecture: { weight: -4.9, interactions: 8 },
  } };
  assert.equal(applyInteraction(high, makeArticle(), "save").sources.source_one.weight, 5);
  assert.equal(applyInteraction(high, makeArticle(), "save").topics.distributed_systems.weight, 5);
  assert.equal(applyInteraction(low, makeArticle(), "dismiss").sources.source_one.weight, -5);
  assert.equal(applyInteraction(low, makeArticle(), "dismiss").topics.distributed_systems.weight, -5);
});

test("reversal restores deltas and counters without going negative", () => {
  const article = makeArticle({ tags: [{ id: "oauth", label: "OAuth" }] });
  const applied = applyInteraction({ sources: {}, topics: {} }, article, "read");
  assert.deepEqual(reverseInteraction(applied, article, "read"), { sources: {}, topics: {} });
  assert.deepEqual(reverseInteraction({ sources: {}, topics: {} }, article, "read"), { sources: {}, topics: {} });
});

test("unsupported preference events fail as programmer errors", () => {
  assert.throws(() => applyInteraction({ sources: {}, topics: {} }, makeArticle(), "category"), /Unsupported interaction/);
});
