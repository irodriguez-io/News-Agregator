import assert from "node:assert/strict";
import test from "node:test";

import { backupFilename, createApplication, destinationFromHash } from "../../js/app.js";
import { transitionArticle } from "../../js/state/article-state.js";
import { createDefaultState, exportState, saveState } from "../../js/state/storage.js";
import { makeArticle, makeDataset, MemoryStorage, TIMES } from "./helpers.js";

function createUiRecorder() {
  const calls = {
    appearance: [],
    announcements: [],
    discover: [],
    history: [],
    navigation: [],
    readLater: [],
    settings: [],
    destroyDiscover: 0,
  };
  return {
    calls,
    ui: {
      announceStatus: (message) => calls.announcements.push(message),
      applyAppearance: (appearance) => calls.appearance.push(appearance),
      destroyDiscover: () => { calls.destroyDiscover += 1; },
      renderDiscover: (viewModel) => calls.discover.push(viewModel),
      renderHistory: (viewModel) => calls.history.push(viewModel),
      renderNavigation: (viewModel) => calls.navigation.push(viewModel),
      renderReadLater: (viewModel) => calls.readLater.push(viewModel),
      renderSettings: (viewModel) => calls.settings.push(viewModel),
    },
  };
}

function createWindowStub() {
  const listeners = new Map();
  return {
    addEventListener(type, listener) {
      listeners.set(type, listener);
    },
    dispatch(type) {
      listeners.get(type)?.();
    },
  };
}

function stateWith(article, action) {
  return transitionArticle(createDefaultState(), article, action, { now: TIMES.first }).state;
}

test("routing and backup naming use stable V1 values", () => {
  assert.equal(destinationFromHash("#read-later"), "read_later");
  assert.equal(destinationFromHash("#history"), "history");
  assert.equal(destinationFromHash("#unknown"), "discover");
  assert.equal(
    backupFilename("2026-08-17T14:05:09.123Z"),
    "intentional-reading-backup-20260817-140509Z.json",
  );
});

test("startup renders local state independently, then renders the loaded personalized deck", async () => {
  let resolveDataset;
  const pendingDataset = new Promise((resolve) => { resolveDataset = resolve; });
  const article = makeArticle();
  const storage = new MemoryStorage();
  const { calls, ui } = createUiRecorder();
  const locationObject = { hash: "#discover", search: "?debug=1" };
  const app = createApplication({
    storage,
    ui,
    locationObject,
    windowObject: createWindowStub(),
    loadDataset: () => pendingDataset,
    now: () => TIMES.second,
  });

  const starting = app.start();
  assert.equal(calls.appearance[0], "system");
  assert.equal(calls.discover.at(-1).loading, true);
  assert.equal(calls.navigation.at(-1).readLaterCount, 0);

  resolveDataset(makeDataset([article]));
  const result = await starting;
  assert.equal(result.ok, true);
  assert.equal(calls.discover.at(-1).article.id, article.id);
  assert.equal(calls.discover.at(-1).debug.base, article.score.base);
  assert.equal(calls.discover.at(-1).debug.detectedTagCount, article.tags.length);
});

test("dataset failure leaves persisted Read Later and History routes available", async () => {
  const saved = makeArticle();
  const read = makeArticle({ id: "00000000000000000002", title: "Already read" });
  let state = stateWith(saved, "save");
  state = transitionArticle(state, read, "mark_read", { now: TIMES.second }).state;
  const storage = new MemoryStorage();
  assert.equal(saveState(state, { storage }).ok, true);
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => { throw new Error("unavailable"); },
    now: () => TIMES.third,
  });

  const result = await app.start();
  assert.equal(result.ok, false);
  assert.equal(calls.discover.at(-1).error, true);
  assert.deepEqual(calls.navigation.at(-1), {
    activeDestination: "discover",
    readLaterCount: 1,
    historyCount: 1,
  });

  assert.equal(app.handleAction({ action: "navigate", destination: "read_later" }).ok, true);
  assert.equal(calls.readLater.at(-1).items[0].article.id, saved.id);
  assert.equal(app.handleAction({ action: "navigate", destination: "history" }).ok, true);
  assert.equal(calls.history.at(-1).items[0].article.id, read.id);
});

test("persistent actions commit only after storage succeeds and only eligible actions create Undo", async () => {
  const article = makeArticle();
  const storage = new MemoryStorage();
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([article]),
    now: () => TIMES.first,
  });
  await app.start();

  storage.failSet = true;
  const failed = app.handleAction({ action: "save", articleId: article.id, undoable: true });
  assert.equal(failed.ok, false);
  assert.deepEqual(app.getSnapshot().state.articles, {});
  assert.equal(calls.discover.at(-1).article.id, article.id);

  storage.failSet = false;
  const saved = app.handleAction({ action: "save", articleId: article.id, undoable: false });
  assert.equal(saved.ok, true);
  assert.equal(app.getSnapshot().state.articles[article.id].status, "saved");
  assert.equal(app.getSnapshot().undoAvailable, false);
});

test("Open validates the URL, attempts persistence first, and still navigates after persistence failure", async () => {
  const article = makeArticle();
  const storage = new MemoryStorage();
  const navigated = [];
  const scheduled = [];
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([article]),
    navigateExternal: (url) => { navigated.push(url); return true; },
    schedule: (callback) => scheduled.push(callback),
    now: () => TIMES.first,
  });
  await app.start();
  storage.failSet = true;

  const result = app.handleAction({ action: "open", articleId: article.id });
  assert.deepEqual(result, { ok: true, persisted: false, navigationOpened: true });
  assert.deepEqual(navigated, [article.url]);
  assert.deepEqual(app.getSnapshot().state.articles, {});
  scheduled[0]();
  assert.match(calls.announcements.at(-1), /not saved locally/);

  const unsafeArticle = makeArticle({ id: "00000000000000000002", url: "javascript:alert(1)" });
  const unsafeApp = createApplication({
    storage: new MemoryStorage(),
    ui: createUiRecorder().ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([unsafeArticle]),
    navigateExternal: (url) => navigated.push(url),
  });
  await unsafeApp.start();
  assert.equal(unsafeApp.handleAction({ action: "open", articleId: unsafeArticle.id }).ok, false);
  assert.equal(navigated.length, 1);
});

test("default external navigation uses a new isolated browsing context", async () => {
  const article = makeArticle();
  const openCalls = [];
  const openedWindow = { opener: "original" };
  const windowObject = createWindowStub();
  windowObject.open = (...args) => {
    openCalls.push(args);
    return openedWindow;
  };
  const app = createApplication({
    storage: new MemoryStorage(),
    ui: createUiRecorder().ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject,
    loadDataset: async () => makeDataset([article]),
    now: () => TIMES.first,
  });
  await app.start();

  assert.equal(app.handleAction({ action: "open", articleId: article.id }).ok, true);
  assert.deepEqual(openCalls, [[article.url, "_blank", "noopener,noreferrer"]]);
  assert.equal(openedWindow.opener, null);
});

test("successful swipe actions can be undone and preference changes are persisted", async () => {
  const article = makeArticle();
  const storage = new MemoryStorage();
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([article]),
    now: () => TIMES.first,
  });
  await app.start();

  assert.equal(app.handleAction({ action: "dismiss", articleId: article.id, undoable: true }).ok, true);
  assert.equal(app.getSnapshot().undoAvailable, true);
  assert.equal(app.handleAction({ action: "undo" }).ok, true);
  assert.deepEqual(app.getSnapshot().state.articles, {});

  assert.equal(app.handleAction({ action: "category_change", category: "technology" }).ok, true);
  assert.equal(app.getSnapshot().state.session.lastCategory, "technology");
  assert.equal(app.handleAction({ action: "appearance_change", appearance: "dark" }).ok, true);
  assert.equal(app.getSnapshot().state.settings.appearance, "dark");
  assert.equal(calls.appearance.at(-1), "dark");
});

test("export, replacement import, and reset use the exact validated local-state schema", async () => {
  const article = makeArticle();
  const storage = new MemoryStorage();
  const downloads = [];
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui,
    locationObject: { hash: "#read-later", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([article]),
    download: (serialized, filename) => { downloads.push({ serialized, filename }); return true; },
    now: () => TIMES.third,
  });
  await app.start();

  const importedState = stateWith(article, "save");
  assert.equal(app.handleAction({ action: "import_data", serialized: exportState(importedState) }).ok, true);
  assert.equal(calls.readLater.at(-1).items.length, 1);
  assert.equal(app.handleAction({ action: "export_data" }).ok, true);
  assert.equal(downloads[0].filename, "intentional-reading-backup-20260817-140000Z.json");
  assert.deepEqual(JSON.parse(downloads[0].serialized), importedState);

  assert.equal(app.handleAction({ action: "reset_data" }).ok, true);
  assert.deepEqual(app.getSnapshot().state, createDefaultState());
  assert.equal(calls.readLater.at(-1).items.length, 0);
});

test("default export triggers a JSON download before revoking its object URL", async () => {
  const links = [];
  const revoked = [];
  const scheduled = [];
  const documentObject = {
    createElement(tagName) {
      assert.equal(tagName, "a");
      const link = { clickCalled: false, click() { this.clickCalled = true; } };
      links.push(link);
      return link;
    },
  };
  const urlApi = {
    createObjectURL(blob) {
      assert.equal(blob.type, "application/json");
      return "blob:test-backup";
    },
    revokeObjectURL(url) {
      revoked.push(url);
    },
  };
  const app = createApplication({
    storage: new MemoryStorage(),
    ui: createUiRecorder().ui,
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    documentObject,
    urlApi,
    schedule: (callback) => scheduled.push(callback),
    loadDataset: async () => makeDataset(),
    now: () => TIMES.first,
  });
  await app.start();

  assert.equal(app.handleAction({ action: "export_data" }).ok, true);
  assert.deepEqual(links[0], {
    clickCalled: true,
    download: "intentional-reading-backup-20260817-120000Z.json",
    href: "blob:test-backup",
    rel: "noopener noreferrer",
    click: links[0].click,
  });
  assert.deepEqual(revoked, []);
  scheduled[0]();
  assert.deepEqual(revoked, ["blob:test-backup"]);
});
