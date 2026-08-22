import assert from "node:assert/strict";
import test from "node:test";

import { createApplication } from "../../js/app.js";
import { transitionArticle } from "../../js/state/article-state.js";
import { createDefaultState, saveState } from "../../js/state/storage.js";
import { destroyDiscover, renderDiscover } from "../../js/ui/discover.js";
import { renderHistory } from "../../js/ui/history.js";
import {
  MemoryStorage,
  TIMES,
  createUiRecorder,
  createWindowStub,
  makeArticle,
  makeDataset,
} from "./helpers.js";
import { findAll, findButton, findByClass, installDomFixture } from "./dom-fixture.js";

function renderModel(overrides = {}, handlers = {}) {
  return renderDiscover({
    article: makeArticle(),
    opened: false,
    category: "all",
    remainingCount: 1,
    now: new Date(TIMES.second),
    ...overrides,
  }, handlers);
}

function buttonLabels(root) {
  return findAll(root, (node) => node.tagName === "BUTTON")
    .map((node) => node.getAttribute("aria-label"))
    .filter(Boolean);
}

test("Scenario: opened article is acknowledged on return (presentation half)", (t) => {
  const dom = installDomFixture();
  t.after(() => {
    destroyDiscover();
    dom.restore();
  });

  renderModel({ opened: true });

  const card = findByClass(dom.appView, "article-card");
  const acknowledgment = findByClass(card, "opened-acknowledgment");
  assert.equal(card.dataset.opened, "true");
  assert.match(acknowledgment.textContent, /opened/i);
  assert.deepEqual(buttonLabels(card), [
    "Not interested",
    "Read article in a new tab",
    "Save for later",
    "Mark read",
  ]);

  renderModel({ opened: false });
  const unopenedCard = findByClass(dom.appView, "article-card");
  assert.equal(unopenedCard.dataset.opened, undefined);
  assert.equal(findByClass(unopenedCard, "opened-acknowledgment"), null);
  assert.equal(findButton(unopenedCard, "Mark read"), null);
  assert.deepEqual(buttonLabels(unopenedCard), [
    "Not interested",
    "Read article in a new tab",
    "Save for later",
  ]);
});

test("Scenario: mark read from opened reaches History", async (t) => {
  const dom = installDomFixture();
  t.after(() => {
    destroyDiscover();
    dom.restore();
  });
  const article = makeArticle();
  const openedState = transitionArticle(
    createDefaultState(),
    article,
    "open",
    { now: TIMES.first },
  ).state;
  const storage = new MemoryStorage();
  assert.equal(saveState(openedState, { storage }).ok, true);
  const { calls, ui } = createUiRecorder();
  const app = createApplication({
    storage,
    ui: {
      ...ui,
      destroyDiscover,
      renderDiscover,
      renderHistory,
    },
    locationObject: { hash: "#discover", search: "" },
    windowObject: createWindowStub(),
    loadDataset: async () => makeDataset([article]),
    now: () => TIMES.second,
  });
  await app.start();

  const markRead = findButton(dom.appView, "Mark read");
  assert.ok(markRead, "opened card should render the Mark read control");
  markRead.click();
  await Promise.resolve();

  const record = app.getSnapshot().state.articles[article.id];
  assert.equal(record.status, "read");
  assert.equal(record.readAt, TIMES.second);
  assert.equal(record.openedAt, TIMES.first);
  assert.equal(record.savedAt, null);
  assert.equal(record.dismissedAt, null);
  assert.equal(findByClass(dom.appView, "article-card"), null);
  assert.equal(calls.navigation.at(-1).historyCount, 1);

  assert.equal(app.handleAction({ action: "navigate", destination: "history" }).ok, true);
  assert.equal(findByClass(dom.appView, "history-group-heading").textContent, "Today1 article");
  assert.match(dom.appView.textContent, new RegExp(article.title));
});

test("Scenario: mark read offers no Undo", async (t) => {
  const dom = installDomFixture();
  t.after(() => {
    destroyDiscover();
    dom.restore();
  });
  const actions = [];
  renderModel({ opened: true }, {
    onAction(detail) {
      actions.push(detail);
      return { ok: true };
    },
  });

  const markRead = findButton(dom.appView, "Mark read");
  assert.ok(markRead, "opened card should render the Mark read control");
  markRead.click();
  await Promise.resolve();

  assert.deepEqual(actions, [{
    action: "mark_read",
    articleId: makeArticle().id,
    undoable: false,
  }]);
  assert.match(dom.status.textContent, /marked read/i);
  assert.equal(dom.toast.childElementCount, 0);
  assert.doesNotMatch(dom.document.textContent, /undo/i);
});

test("mark read persistence failure restores its control and announces the failure", async (t) => {
  const dom = installDomFixture();
  t.after(() => {
    destroyDiscover();
    dom.restore();
  });
  renderModel({ opened: true }, {
    onAction: () => ({ ok: false, message: "The article could not be moved to History." }),
  });
  const markRead = findButton(dom.appView, "Mark read");
  assert.ok(markRead, "opened card should render the Mark read control");

  markRead.click();
  await Promise.resolve();

  assert.equal(markRead.disabled, false);
  assert.match(dom.status.textContent, /could not be moved to History/);
  assert.ok(findByClass(dom.appView, "article-card"));
});
