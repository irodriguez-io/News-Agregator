import test from "node:test";
import assert from "node:assert/strict";

import {
  LOCAL_STORAGE_KEY,
  StateStorageError,
  createDefaultState,
  exportState,
  importState,
  loadState,
  migrateState,
  resetState,
  saveState,
  validateState,
} from "../../js/state/storage.js";
import { commitArticleAction, transitionArticle } from "../../js/state/article-state.js";
import { makeArticle, MemoryStorage, TIMES } from "./helpers.js";

test("default state and key exactly match Local State v1", () => {
  assert.equal(LOCAL_STORAGE_KEY, "intentionalReading:v1");
  assert.deepEqual(createDefaultState(), {
    schemaVersion: 1,
    preferences: { sources: {}, topics: {} },
    articles: {},
    settings: { appearance: "system" },
    session: { lastCategory: "all" },
  });
  assert.deepEqual(loadState({ storage: new MemoryStorage() }), {
    ok: true,
    state: createDefaultState(),
    source: "default",
  });
});

test("valid state saves and loads through only the exact storage key", () => {
  const storage = new MemoryStorage();
  const state = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first }).state;
  const saved = saveState(state, { storage });
  assert.equal(saved.ok, true);
  assert.deepEqual(storage.calls.map((call) => call.slice(0, 2)), [["setItem", LOCAL_STORAGE_KEY]]);
  assert.deepEqual(loadState({ storage }).state, state);
});

test("malformed and unsupported stored state return recoverable errors and preserve raw bytes", async (t) => {
  for (const [name, raw, code] of [
    ["malformed", "{not json", "MALFORMED_JSON"],
    ["unsupported", JSON.stringify({ ...createDefaultState(), schemaVersion: 2 }), "UNSUPPORTED_SCHEMA"],
  ]) {
    await t.test(name, () => {
      const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: raw });
      const result = loadState({ storage });
      assert.equal(result.ok, false);
      assert.equal(result.error instanceof StateStorageError, true);
      assert.equal(result.error.code, code);
      assert.equal(result.rawValue, raw);
      assert.equal(storage.values.get(LOCAL_STORAGE_KEY), raw);
      assert.equal(storage.calls.some(([operation]) => operation !== "getItem"), false);
    });
  }
});

test("corrupt startup state rejects ordinary application mutations without replacing raw bytes", async (t) => {
  for (const [name, raw] of [
    ["malformed", "{not json"],
    ["unsupported", JSON.stringify({ ...createDefaultState(), schemaVersion: 2 })],
  ]) {
    await t.test(name, () => {
      const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: raw });
      const loaded = loadState({ storage });

      const result = commitArticleAction({
        state: loaded.state,
        article: makeArticle(),
        action: "save",
        now: TIMES.first,
        storage,
      });

      assert.equal(result.ok, false);
      assert.equal(result.persisted, false);
      assert.equal(result.error.code, "RECOVERY_REQUIRED");
      assert.equal(result.state, loaded.state);
      assert.equal(storage.values.get(LOCAL_STORAGE_KEY), raw);
    });
  }
});

test("only explicit Reset or a valid replacement import releases corrupt-state recovery", async (t) => {
  await t.test("valid replacement import", () => {
    const raw = "{not json";
    const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: raw });
    const loaded = loadState({ storage });

    const invalidImport = importState("{", { storage });
    assert.equal(invalidImport.ok, false);
    assert.equal(storage.values.get(LOCAL_STORAGE_KEY), raw);

    const replacement = createDefaultState();
    const imported = importState(JSON.stringify(replacement), { storage });
    assert.equal(imported.ok, true);
    assert.deepEqual(loadState({ storage }), { ok: true, state: replacement, source: "storage" });

    const saved = commitArticleAction({
      state: imported.state,
      article: makeArticle(),
      action: "save",
      now: TIMES.first,
      storage,
    });
    assert.equal(saved.ok, true);
    assert.equal(saved.persisted, true);
    assert.equal(saved.state.articles[makeArticle().id].status, "saved");
    assert.equal(loaded.ok, false);
  });

  await t.test("Reset", () => {
    const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: "{not json" });
    const loaded = loadState({ storage });
    assert.equal(loaded.ok, false);

    const reset = resetState({ storage });
    assert.equal(reset.ok, true);

    const saved = commitArticleAction({
      state: reset.state,
      article: makeArticle(),
      action: "save",
      now: TIMES.first,
      storage,
    });
    assert.equal(saved.ok, true);
    assert.equal(saved.persisted, true);
  });
});

test("migration is centralized and refuses unsupported paths", () => {
  const state = createDefaultState();
  assert.deepEqual(migrateState(state, 1, 1), state);
  assert.throws(() => migrateState(state, 2, 1), (error) => error.code === "UNSUPPORTED_SCHEMA");
});

test("export emits only the exact validated V1 state and round-trips through replacement import", () => {
  const current = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first }).state;
  const serialized = exportState(current);
  assert.deepEqual(Object.keys(JSON.parse(serialized)), ["schemaVersion", "preferences", "articles", "settings", "session"]);

  const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: JSON.stringify(createDefaultState()) });
  const imported = importState(serialized, { storage });
  assert.equal(imported.ok, true);
  assert.deepEqual(imported.state, current);
  assert.deepEqual(loadState({ storage }).state, current);
});

test("reset removes persistent state and yields an exact default; failures are explicit", () => {
  const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: "old" });
  assert.deepEqual(resetState({ storage }), { ok: true, state: createDefaultState() });
  assert.equal(storage.values.has(LOCAL_STORAGE_KEY), false);

  const failing = new MemoryStorage({ [LOCAL_STORAGE_KEY]: "old" });
  failing.failRemove = true;
  const result = resetState({ storage: failing });
  assert.equal(result.ok, false);
  assert.equal(result.error.code, "WRITE_FAILED");
  assert.equal(failing.values.get(LOCAL_STORAGE_KEY), "old");
});

test("storage read/write failures are explicit and do not claim a committed state", () => {
  const readFailure = new MemoryStorage();
  readFailure.failGet = true;
  assert.equal(loadState({ storage: readFailure }).error.code, "READ_FAILED");

  const writeFailure = new MemoryStorage();
  writeFailure.failSet = true;
  const result = saveState(createDefaultState(), { storage: writeFailure });
  assert.equal(result.ok, false);
  assert.equal(result.error.code, "WRITE_FAILED");
  assert.equal(result.state, null);
});

test("import rejects all required unsafe candidates atomically", async (t) => {
  const existing = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first }).state;
  const base = transitionArticle(createDefaultState(), makeArticle(), "save", { now: TIMES.first }).state;
  const record = base.articles[makeArticle().id];
  const cases = new Map([
    ["malformed JSON", "{"],
    ["unsupported version", { ...base, schemaVersion: 2 }],
    ["invalid root", { ...base, extra: true }],
    ["invalid Article ID", { ...base, articles: { bad: record } }],
    ["key ID mismatch", { ...base, articles: { "00000000000000000002": record } }],
    ["unsafe URL", { ...base, articles: { [makeArticle().id]: { ...record, article: { ...record.article, url: "data:text/html,bad" } } } }],
    ["invalid status", { ...base, articles: { [makeArticle().id]: { ...record, status: "unseen" } } }],
    ["invalid category", { ...base, articles: { [makeArticle().id]: { ...record, article: { ...record.article, category: "tech" } } } }],
    ["invalid appearance", { ...base, settings: { appearance: "sepia" } }],
    ["high preference", { ...base, preferences: { ...base.preferences, sources: { x: { weight: 5.01, interactions: 1 } } } }],
    ["low preference", { ...base, preferences: { ...base.preferences, sources: { x: { weight: -5.01, interactions: 1 } } } }],
    ["negative interactions", { ...base, preferences: { ...base.preferences, topics: { x: { weight: 0, interactions: -1 } } } }],
    ["invalid timestamp", { ...base, articles: { [makeArticle().id]: { ...record, firstSeenAt: "today" } } }],
    ["invalid signal", { ...base, articles: { [makeArticle().id]: { ...record, signalsApplied: { ...record.signalsApplied, saved: 1 } } } }],
    ["missing Read signal", (() => {
      const readState = transitionArticle(createDefaultState(), makeArticle(), "mark_read", { now: TIMES.first }).state;
      const readRecord = readState.articles[makeArticle().id];
      return { ...readState, articles: { [makeArticle().id]: { ...readRecord, signalsApplied: { ...readRecord.signalsApplied, read: false } } } };
    })()],
    ["inconsistent Open signal", { ...base, articles: { [makeArticle().id]: { ...record, openedAt: TIMES.second, signalsApplied: { ...record.signalsApplied, opened: false } } } }],
    ["timestamp before first seen", { ...base, articles: { [makeArticle().id]: { ...record, savedAt: "2026-08-17T11:00:00.000Z" } } }],
    ["constructor", JSON.stringify(createDefaultState()).replace('"sources":{}', '"sources":{"constructor":{"weight":0,"interactions":0}}')],
    ["prototype", JSON.stringify(createDefaultState()).replace('"sources":{}', '"sources":{"prototype":{"weight":0,"interactions":0}}')],
    ["__proto__", JSON.stringify(createDefaultState()).replace('"sources":{}', '"sources":{"__proto__":{"weight":0,"interactions":0}}')],
  ]);

  for (const [name, candidate] of cases) {
    await t.test(name, () => {
      const existingRaw = JSON.stringify(existing);
      const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: existingRaw });
      const result = importState(typeof candidate === "string" ? candidate : JSON.stringify(candidate), { storage });
      assert.equal(result.ok, false);
      assert.equal(storage.values.get(LOCAL_STORAGE_KEY), existingRaw);
    });
  }
});

test("import enforces the 5 MiB defensive size boundary before parsing", () => {
  const storage = new MemoryStorage({ [LOCAL_STORAGE_KEY]: JSON.stringify(createDefaultState()) });
  const result = importState(" ".repeat((5 * 1024 * 1024) + 1), { storage });
  assert.equal(result.ok, false);
  assert.equal(result.error.code, "IMPORT_TOO_LARGE");
  assert.deepEqual(loadState({ storage }).state, createDefaultState());
});

test("complete validation enforces appearance/category enums and excludes category preferences", () => {
  for (const appearance of ["light", "dark", "system"]) {
    for (const lastCategory of ["all", "science", "technology", "literature", "history", "weightlifting", "iam", "identity_automation"]) {
      assert.equal(validateState({ ...createDefaultState(), settings: { appearance }, session: { lastCategory } }).settings.appearance, appearance);
    }
  }
  assert.throws(() => validateState({ ...createDefaultState(), preferences: { sources: {}, topics: {}, categories: {} } }));
});
