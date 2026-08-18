import { loadArticleDataset } from "./data/articles.js";
import { APPEARANCE_IDS, CATEGORY_FILTER_IDS, isSafeHttpUrl } from "./data/validation.js";
import { buildDeck } from "./ranking/deck.js";
import { commitArticleAction, commitUndo, createUndoManager } from "./state/article-state.js";
import {
  getHistoryAggregate,
  getNavigationCounts,
  getReadLaterAggregate,
  selectHistory,
  selectReadLater,
} from "./state/selectors.js";
import {
  cloneState,
  exportState,
  importState,
  loadState,
  resetState,
  saveState,
} from "./state/storage.js";
import {
  announceStatus,
  applyAppearance,
  destroyDiscover,
  renderDiscover,
  renderHistory,
  renderNavigation,
  renderReadLater,
  renderSettings,
} from "./ui/index.js";

const DESTINATIONS = Object.freeze(["discover", "read_later", "history"]);
const HASHES = Object.freeze({
  discover: "#discover",
  read_later: "#read-later",
  history: "#history",
});

const DEFAULT_UI = Object.freeze({
  announceStatus,
  applyAppearance,
  destroyDiscover,
  renderDiscover,
  renderHistory,
  renderNavigation,
  renderReadLater,
  renderSettings,
});

const FAILURE_MESSAGES = Object.freeze({
  dismiss: "Not interested could not be saved. The article remains available.",
  save: "The article could not be saved to Read Later.",
  mark_read: "The article could not be moved to History.",
  mark_unread: "The article could not be returned to Read Later.",
  remove: "The article could not be removed from Read Later.",
  undo: "Undo could not be completed.",
  category_change: "The category could not be changed.",
  appearance_change: "Appearance could not be changed.",
  import_data: "Import was not completed. Current local data was not changed.",
  reset_data: "Local data could not be reset.",
});

export function destinationFromHash(hash = "") {
  const match = Object.entries(HASHES).find(([, value]) => value === hash);
  return match?.[0] ?? "discover";
}

export function backupFilename(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) throw new TypeError("Backup time must be a valid date");
  const [day, time] = date.toISOString().split("T");
  return `intentional-reading-backup-${day.replaceAll("-", "")}-${time.replaceAll(":", "").replace(/\.\d{3}Z$/, "Z")}.json`;
}

function topicLabel(records, topicId) {
  if (!topicId) return null;
  for (const record of records) {
    const tag = record.article.tags.find((candidate) => candidate.id === topicId);
    if (tag) return tag.label;
  }
  return null;
}

function defaultNavigateExternal(url, windowObject = globalThis.window) {
  if (!windowObject || typeof windowObject.open !== "function") return false;
  const opened = windowObject.open(url, "_blank", "noopener,noreferrer");
  if (opened) opened.opener = null;
  return true;
}

function defaultDownload(
  serialized,
  filename,
  documentObject = globalThis.document,
  urlApi = globalThis.URL,
  revokeLater = (callback) => globalThis.setTimeout(callback, 0),
) {
  if (!documentObject || !urlApi || typeof urlApi.createObjectURL !== "function") return false;
  const blobUrl = urlApi.createObjectURL(new Blob([serialized], { type: "application/json" }));
  try {
    const link = documentObject.createElement("a");
    link.href = blobUrl;
    link.download = filename;
    link.rel = "noopener noreferrer";
    link.click();
  } catch (error) {
    urlApi.revokeObjectURL(blobUrl);
    throw error;
  }
  revokeLater(() => urlApi.revokeObjectURL(blobUrl));
  return true;
}

function resultFailure(action) {
  return { ok: false, message: FAILURE_MESSAGES[action] ?? "The action could not be completed." };
}

export function createApplication(options = {}) {
  const ui = { ...DEFAULT_UI, ...(options.ui ?? {}) };
  const loadDataset = options.loadDataset ?? loadArticleDataset;
  const storage = options.storage;
  const windowObject = options.windowObject ?? globalThis.window;
  const documentObject = options.documentObject ?? globalThis.document;
  const locationObject = options.locationObject ?? globalThis.location;
  const urlApi = options.urlApi ?? globalThis.URL;
  const now = options.now ?? (() => new Date());
  const schedule = options.schedule ?? ((callback) => globalThis.setTimeout(callback, 0));
  const undoManager = options.undoManager ?? createUndoManager();
  const navigateExternal = options.navigateExternal
    ?? ((url) => defaultNavigateExternal(url, windowObject));
  const download = options.download
    ?? ((serialized, filename) => defaultDownload(
      serialized,
      filename,
      documentObject,
      urlApi,
      schedule,
    ));

  let state = null;
  let dataset = null;
  let datasetError = null;
  let datasetLoading = false;
  let started = false;
  let activeDestination = destinationFromHash(locationObject?.hash ?? "");

  const actionHandlers = {
    onAction: (detail) => handleAction(detail),
    onRetry: () => refreshDataset(),
  };

  function currentDate() {
    const value = typeof now === "function" ? now() : now;
    return value instanceof Date ? value : new Date(value);
  }

  function articleForAction(articleId) {
    if (activeDestination === "discover") {
      return dataset?.articles.find((article) => article.id === articleId)
        ?? state.articles[articleId]?.article
        ?? null;
    }
    return state.articles[articleId]?.article
      ?? dataset?.articles.find((article) => article.id === articleId)
      ?? null;
  }

  function render() {
    const counts = getNavigationCounts(state);
    ui.renderNavigation({
      activeDestination,
      readLaterCount: counts.readLater,
      historyCount: counts.history,
    }, actionHandlers);
    ui.renderSettings({ appearance: state.settings.appearance }, actionHandlers);

    if (activeDestination !== "discover") ui.destroyDiscover();
    if (activeDestination === "read_later") {
      const items = selectReadLater(state);
      const aggregate = getReadLaterAggregate(state);
      ui.renderReadLater({
        items,
        readLaterCount: aggregate.count,
        aggregateReadingTimeMinutes: aggregate.knownReadingTimeMinutes,
        nextTopic: topicLabel(items, aggregate.firstAvailableTopic),
        now: currentDate(),
      }, actionHandlers);
      return;
    }
    if (activeDestination === "history") {
      const items = selectHistory(state);
      const aggregate = getHistoryAggregate(state);
      ui.renderHistory({
        items,
        historyCount: aggregate.count,
        aggregateReadingTimeMinutes: aggregate.knownReadingTimeMinutes,
        latestTopic: topicLabel(items, aggregate.newestAvailableTopic),
        now: currentDate(),
      }, actionHandlers);
      return;
    }

    const deck = dataset
      ? buildDeck({ articles: dataset.articles, state, category: state.session.lastCategory })
      : [];
    const first = deck[0] ?? null;
    const debugMode = new URLSearchParams(locationObject?.search ?? "").get("debug") === "1";
    ui.renderDiscover({
      article: first?.article ?? null,
      category: state.session.lastCategory,
      remainingCount: dataset ? deck.length : null,
      now: currentDate(),
      degraded: Boolean(dataset?.pipeline.failedSourceCount),
      debug: debugMode && first
        ? { ...first.score, detectedTagCount: first.article.tags.length }
        : null,
      loading: datasetLoading,
      error: datasetError,
      undoAvailable: Boolean(undoManager.peek()),
    }, actionHandlers);
  }

  function persistStateChange(action, mutator) {
    const candidate = cloneState(state);
    mutator(candidate);
    const result = saveState(candidate, { storage });
    if (!result.ok) return resultFailure(action);
    state = result.state;
    render();
    return { ok: true, state };
  }

  function navigate(destination) {
    if (!DESTINATIONS.includes(destination)) return resultFailure("navigate");
    activeDestination = destination;
    if (locationObject && locationObject.hash !== HASHES[destination]) {
      locationObject.hash = HASHES[destination];
    }
    render();
    return { ok: true };
  }

  function handleArticleAction(detail) {
    const article = articleForAction(detail.articleId);
    if (!article) return resultFailure(detail.action);
    if (detail.action === "open" && !isSafeHttpUrl(article.url)) {
      return { ok: false, message: "This publisher URL cannot be opened safely." };
    }

    const result = commitArticleAction({
      state,
      article,
      action: detail.action,
      now,
      storage,
      undoable: Boolean(detail.undoable),
      undoManager,
    });

    if (detail.action === "open" && result.allowNavigation) {
      let opened = false;
      try {
        opened = navigateExternal(article.url);
      } catch {
        opened = false;
      }
      if (result.ok) {
        state = result.state;
        render();
      }
      if (!opened) return { ok: false, message: "The publisher could not be opened in a new tab." };
      if (!result.ok) {
        schedule(() => ui.announceStatus(
          "The publisher opened, but this Open interaction was not saved locally.",
        ));
        return { ok: true, persisted: false, navigationOpened: true };
      }
      return { ok: true, persisted: true, navigationOpened: true };
    }

    if (!result.ok) return resultFailure(detail.action);
    state = result.state;
    render();
    return { ok: true, persisted: result.persisted, changed: result.changed };
  }

  function handleAction(detail = {}) {
    if (["dismiss", "save", "open", "mark_read", "mark_unread", "remove"].includes(detail.action)) {
      return handleArticleAction(detail);
    }
    switch (detail.action) {
      case "undo": {
        const result = commitUndo({ state, undoManager, storage });
        if (!result.ok) return resultFailure("undo");
        state = result.state;
        render();
        ui.announceStatus("Undo completed.");
        return { ok: true, persisted: true };
      }
      case "navigate":
        return navigate(detail.destination);
      case "category_change":
        if (!CATEGORY_FILTER_IDS.includes(detail.category)) return resultFailure(detail.action);
        return persistStateChange(detail.action, (candidate) => {
          candidate.session.lastCategory = detail.category;
        });
      case "appearance_change":
        if (!APPEARANCE_IDS.includes(detail.appearance)) return resultFailure(detail.action);
        {
          const result = persistStateChange(detail.action, (candidate) => {
            candidate.settings.appearance = detail.appearance;
          });
          if (result.ok) ui.applyAppearance(detail.appearance);
          return result;
        }
      case "export_data": {
        let serialized;
        try {
          serialized = exportState(state);
          if (!download(serialized, backupFilename(currentDate()))) return resultFailure(detail.action);
        } catch {
          return resultFailure(detail.action);
        }
        return { ok: true };
      }
      case "import_data": {
        const result = importState(detail.serialized, { storage });
        if (!result.ok) return resultFailure(detail.action);
        state = result.state;
        undoManager.clear();
        ui.applyAppearance(state.settings.appearance);
        render();
        return { ok: true };
      }
      case "reset_data": {
        const result = resetState({ storage });
        if (!result.ok) return resultFailure(detail.action);
        state = result.state;
        undoManager.clear();
        ui.applyAppearance(state.settings.appearance);
        render();
        return { ok: true };
      }
      default:
        return resultFailure(detail.action);
    }
  }

  async function refreshDataset() {
    datasetLoading = true;
    datasetError = null;
    render();
    try {
      dataset = await loadDataset();
    } catch {
      dataset = null;
      datasetError = true;
    }
    datasetLoading = false;
    render();
    if (dataset) ui.announceStatus("Discover is ready.");
    return dataset ? { ok: true, dataset } : { ok: false };
  }

  async function start() {
    if (started) return dataset ? { ok: true, dataset } : { ok: false };
    started = true;
    const loaded = loadState({ storage });
    state = loaded.state;
    ui.applyAppearance(state.settings.appearance);
    if (windowObject?.addEventListener) {
      windowObject.addEventListener("hashchange", () => {
        activeDestination = destinationFromHash(locationObject?.hash ?? "");
        render();
      });
    }
    render();
    if (!loaded.ok) {
      ui.announceStatus("Local reading data could not be loaded. A temporary empty state is in use.");
    }
    return refreshDataset();
  }

  return Object.freeze({
    start,
    refreshDataset,
    handleAction,
    getSnapshot: () => ({
      state: cloneState(state),
      dataset,
      datasetError,
      datasetLoading,
      activeDestination,
      undoAvailable: Boolean(undoManager.peek()),
    }),
  });
}

if (typeof document !== "undefined") {
  createApplication().start().catch(() => {
    announceStatus("Intentional Reading could not start.");
  });
}
