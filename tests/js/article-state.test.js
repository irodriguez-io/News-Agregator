import test from "node:test";
import assert from "node:assert/strict";

import {
  commitArticleAction,
  createUndoManager,
  transitionArticle,
  undoArticleAction,
} from "../../js/state/article-state.js";
import {
  getHistoryAggregate,
  getNavigationCounts,
  getReadLaterAggregate,
  isDiscoverEligible,
  selectHistory,
  selectReadLater,
} from "../../js/state/selectors.js";
import { createDefaultState } from "../../js/state/storage.js";
import { makeArticle, MemoryStorage, TIMES } from "./helpers.js";

function recordFor(result, article = makeArticle()) {
  return result.state.articles[article.id];
}

function assertSignal(state, event, expectedSource, expectedTopic) {
  assert.equal(state.preferences.sources.source_one.weight, expectedSource);
  assert.equal(state.preferences.sources.source_one.interactions, 1);
  assert.equal(state.preferences.topics.distributed_systems.weight, expectedTopic);
}

test("UNSEEN transitions persist complete snapshots, exact status metadata, and only their signal", () => {
  const cases = [
    ["open", "opened", "openedAt", "opened", 0.1, 0.05],
    ["save", "saved", "savedAt", "saved", 0.45, 0.3],
    ["dismiss", "dismissed", "dismissedAt", "dismissed", -0.35, -0.2],
    ["mark_read", "read", "readAt", "read", 0.25, 0.2],
  ];
  for (const [action, status, timestamp, signal, sourceDelta, topicDelta] of cases) {
    const article = makeArticle();
    const result = transitionArticle(createDefaultState(), article, action, { now: TIMES.first });
    const record = recordFor(result, article);
    assert.equal(result.ok, true);
    assert.equal(record.status, status);
    assert.equal(record.firstSeenAt, TIMES.first);
    assert.equal(record[timestamp], TIMES.first);
    assert.deepEqual(record.article, article);
    assert.notEqual(record.article, article);
    assert.deepEqual(record.signalsApplied, {
      opened: signal === "opened",
      saved: signal === "saved",
      dismissed: signal === "dismissed",
      read: signal === "read",
    });
    assertSignal(result.state, signal, sourceDelta, topicDelta);
  }
});

test("OPENED transitions preserve Open and add only Save, Dismiss, or Read", () => {
  const opened = transitionArticle(createDefaultState(), makeArticle(), "open", { now: TIMES.first }).state;
  for (const [action, status, signal] of [
    ["save", "saved", "saved"],
    ["dismiss", "dismissed", "dismissed"],
    ["mark_read", "read", "read"],
  ]) {
    const result = transitionArticle(opened, makeArticle(), action, { now: TIMES.second });
    const record = recordFor(result);
    assert.equal(record.status, status);
    assert.equal(record.firstSeenAt, TIMES.first);
    assert.equal(record.openedAt, TIMES.first);
    assert.equal(record.signalsApplied.opened, true);
    assert.equal(record.signalsApplied[signal], true);
  }
});

test("SAVED → READ clears queue timestamps, applies Read once, and preserves prior signals", () => {
  let state = transitionArticle(createDefaultState(), makeArticle(), "open", { now: TIMES.first }).state;
  state = transitionArticle(state, makeArticle(), "save", { now: TIMES.second }).state;
  const result = transitionArticle(state, makeArticle(), "mark_read", { now: TIMES.third });
  const record = recordFor(result);
  assert.equal(record.status, "read");
  assert.equal(record.readAt, TIMES.third);
  assert.equal(record.savedAt, null);
  assert.equal(record.dismissedAt, null);
  assert.deepEqual(record.signalsApplied, { opened: true, saved: true, dismissed: false, read: true });
});

test("Open, Save, and Read are idempotent and preserve their first timestamps", () => {
  let state = transitionArticle(createDefaultState(), makeArticle(), "open", { now: TIMES.first }).state;
  state = transitionArticle(state, makeArticle(), "open", { now: TIMES.second }).state;
  assert.equal(state.articles[makeArticle().id].openedAt, TIMES.first);
  assert.equal(state.preferences.sources.source_one.interactions, 1);

  state = transitionArticle(state, makeArticle(), "save", { now: TIMES.second }).state;
  state = transitionArticle(state, makeArticle(), "save", { now: TIMES.third }).state;
  assert.equal(state.articles[makeArticle().id].savedAt, TIMES.second);
  assert.equal(state.preferences.sources.source_one.interactions, 2);

  state = transitionArticle(state, makeArticle(), "mark_read", { now: TIMES.third }).state;
  const again = transitionArticle(state, makeArticle(), "mark_read", { now: "2026-08-17T15:00:00.000Z" }).state;
  assert.equal(again.articles[makeArticle().id].readAt, TIMES.third);
  assert.equal(again.preferences.sources.source_one.interactions, 3);
});

test("opening saved/read records preserves status and only applies a missing Open signal", () => {
  let saved = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first }).state;
  saved = transitionArticle(saved, makeArticle(), "open", { now: TIMES.second }).state;
  assert.equal(saved.articles[makeArticle().id].status, "saved");
  assert.equal(saved.articles[makeArticle().id].openedAt, TIMES.second);
  assert.equal(saved.articles[makeArticle().id].signalsApplied.opened, true);

  let read = transitionArticle(saved, makeArticle(), "mark_read", { now: TIMES.third }).state;
  read = transitionArticle(read, makeArticle(), "open", { now: "2026-08-17T15:00:00.000Z" }).state;
  assert.equal(read.articles[makeArticle().id].status, "read");
  assert.equal(read.articles[makeArticle().id].openedAt, TIMES.second);
});

test("Mark Unread reverses Read only, preserves Open/Save, and creates a current savedAt", () => {
  let state = transitionArticle(createDefaultState(), makeArticle(), "open", { now: TIMES.first }).state;
  state = transitionArticle(state, makeArticle(), "save", { now: TIMES.first }).state;
  state = transitionArticle(state, makeArticle(), "mark_read", { now: TIMES.second }).state;
  const before = state.preferences.sources.source_one;
  const result = transitionArticle(state, makeArticle(), "mark_unread", { now: TIMES.third });
  const record = recordFor(result);
  assert.equal(record.status, "saved");
  assert.equal(record.readAt, null);
  assert.equal(record.savedAt, TIMES.third);
  assert.deepEqual(record.signalsApplied, { opened: true, saved: true, dismissed: false, read: false });
  assert.equal(result.state.preferences.sources.source_one.weight, before.weight - 0.25);
  assert.equal(result.state.preferences.sources.source_one.interactions, before.interactions - 1);
});

test("Remove is queue management with no negative preference and no Save reversal", () => {
  let state = transitionArticle(createDefaultState(), makeArticle(), "open", { now: TIMES.first }).state;
  state = transitionArticle(state, makeArticle(), "save", { now: TIMES.second }).state;
  const preferences = structuredClone(state.preferences);
  const result = transitionArticle(state, makeArticle(), "remove", { now: TIMES.third });
  const record = recordFor(result);
  assert.equal(record.status, "dismissed");
  assert.equal(record.dismissedAt, TIMES.third);
  assert.equal(record.savedAt, null);
  assert.deepEqual(record.signalsApplied, { opened: true, saved: true, dismissed: false, read: false });
  assert.deepEqual(result.state.preferences, preferences);
});

test("invalid corrective transitions return explicit failures without mutation", () => {
  const state = createDefaultState();
  for (const action of ["mark_unread", "remove"]) {
    const result = transitionArticle(state, makeArticle(), action, { now: TIMES.first });
    assert.equal(result.ok, false);
    assert.equal(result.code, "INVALID_TRANSITION");
    assert.equal(result.state, state);
  }
});

test("Discover eligibility, selectors, ordering, counts, and aggregates use persisted snapshots", () => {
  const articles = [
    makeArticle({ id: "00000000000000000001", readingTimeMinutes: 5, tags: [{ id: "oauth", label: "OAuth" }] }),
    makeArticle({ id: "00000000000000000002", readingTimeMinutes: null, tags: [] }),
    makeArticle({ id: "00000000000000000003", readingTimeMinutes: 3, tags: [{ id: "scim", label: "SCIM" }] }),
  ];
  let state = transitionArticle(createDefaultState(), articles[0], "save", { now: TIMES.first }).state;
  state = transitionArticle(state, articles[1], "save", { now: TIMES.second }).state;
  state = transitionArticle(state, articles[2], "mark_read", { now: TIMES.third }).state;

  assert.equal(isDiscoverEligible(null), true);
  assert.equal(isDiscoverEligible({ status: "opened" }), true);
  for (const status of ["saved", "dismissed", "read"]) assert.equal(isDiscoverEligible({ status }), false);
  assert.deepEqual(selectReadLater(state).map((entry) => entry.article.id), [articles[1].id, articles[0].id]);
  assert.deepEqual(selectHistory(state).map((entry) => entry.article.id), [articles[2].id]);
  assert.deepEqual(getNavigationCounts(state), { readLater: 2, history: 1 });
  assert.deepEqual(getReadLaterAggregate(state), {
    count: 2,
    knownReadingTimeMinutes: 5,
    unknownReadingTimeCount: 1,
    firstAvailableTopic: "oauth",
  });
  assert.deepEqual(getHistoryAggregate(state), {
    count: 1,
    knownReadingTimeMinutes: 3,
    unknownReadingTimeCount: 0,
    newestAvailableTopic: "scim",
  });
});

test("Save and Dismiss Undo exactly restore unseen/opened state and preferences", () => {
  for (const action of ["save", "dismiss"]) {
    for (const prior of ["unseen", "opened"]) {
      let state = createDefaultState();
      if (prior === "opened") state = transitionArticle(state, makeArticle(), "open", { now: TIMES.first }).state;
      const before = structuredClone(state);
      const acted = transitionArticle(state, makeArticle(), action, { now: TIMES.second, undoable: true });
      const undone = undoArticleAction(acted.state, acted.undo);
      assert.equal(undone.ok, true);
      assert.deepEqual(undone.state, before);
    }
  }
});

test("Undo manager is single-action and in-memory only", () => {
  const manager = createUndoManager();
  assert.equal(manager.peek(), null);
  const first = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first, undoable: true }).undo;
  const second = transitionArticle(createDefaultState(), makeArticle(), "dismiss", { now: TIMES.second, undoable: true }).undo;
  manager.replace(first);
  manager.replace(second);
  assert.deepEqual(manager.peek(), second);
  manager.clear();
  assert.equal(manager.peek(), null);
  assert.equal(createUndoManager().peek(), null);
});

test("transaction wrapper commits UI state only on persistence success; Open may still navigate on failure", () => {
  const state = createDefaultState();
  const failingStorage = new MemoryStorage();
  failingStorage.failSet = true;

  for (const action of ["save", "dismiss", "mark_read"]) {
    const result = commitArticleAction({ state, article: makeArticle(), action, now: TIMES.first, storage: failingStorage });
    assert.equal(result.ok, false);
    assert.equal(result.persisted, false);
    assert.equal(result.allowNavigation, false);
    assert.equal(result.state, state);
  }
  const open = commitArticleAction({ state, article: makeArticle(), action: "open", now: TIMES.first, storage: failingStorage });
  assert.equal(open.ok, false);
  assert.equal(open.allowNavigation, true);
  assert.equal(open.state, state);
  assert.equal(open.attemptedState.articles[makeArticle().id].status, "opened");

  const storage = new MemoryStorage();
  const manager = createUndoManager();
  const saved = commitArticleAction({ state, article: makeArticle(), action: "save", now: TIMES.first, storage, undoable: true, undoManager: manager });
  assert.equal(saved.ok, true);
  assert.equal(saved.persisted, true);
  assert.equal(saved.state.articles[makeArticle().id].status, "saved");
  assert.equal(manager.peek().action, "save");
});
