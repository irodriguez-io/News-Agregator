import { ValidationError, isUtcTimestamp, validateArticle } from "../data/validation.js";
import { applyInteraction, reverseInteraction } from "./preferences.js";
import { cloneState, saveState } from "./storage.js";

const SIGNAL_FOR_ACTION = Object.freeze({
  open: "opened",
  save: "saved",
  dismiss: "dismissed",
  mark_read: "read",
});
const EVENT_FOR_ACTION = Object.freeze({
  open: "open",
  save: "save",
  dismiss: "dismiss",
  mark_read: "read",
});

function resolveNow(now) {
  const value = typeof now === "function" ? now() : (now ?? new Date());
  const timestamp = value instanceof Date ? value.toISOString() : value;
  if (!isUtcTimestamp(timestamp)) throw new TypeError("Transition time must be a UTC ISO-8601 timestamp");
  return timestamp;
}

function makeRecord(article, timestamp) {
  return {
    article: validateArticle(article),
    status: "opened",
    firstSeenAt: timestamp,
    openedAt: null,
    savedAt: null,
    dismissedAt: null,
    readAt: null,
    signalsApplied: { opened: false, saved: false, dismissed: false, read: false },
  };
}

function invalidTransition(state, action, status = "unseen") {
  return {
    ok: false,
    changed: false,
    code: "INVALID_TRANSITION",
    error: new Error(`Action ${action} is invalid from ${status}`),
    state,
    undo: null,
  };
}

function applySignal(next, record, article, action) {
  const signal = SIGNAL_FOR_ACTION[action];
  if (!signal || record.signalsApplied[signal]) return false;
  next.preferences = applyInteraction(next.preferences, article, EVENT_FOR_ACTION[action]);
  record.signalsApplied[signal] = true;
  return true;
}

export function transitionArticle(state, articleCandidate, action, { now, undoable = false } = {}) {
  let article;
  try {
    article = validateArticle(articleCandidate);
  } catch (error) {
    if (error instanceof ValidationError) {
      return { ok: false, changed: false, code: "INVALID_ARTICLE", error, state, undo: null };
    }
    throw error;
  }

  const existing = state.articles[article.id] ?? null;
  const status = existing?.status ?? "unseen";
  const allowed = {
    open: ["unseen", "opened", "saved", "read"],
    save: ["unseen", "opened", "saved"],
    dismiss: ["unseen", "opened", "dismissed"],
    mark_read: ["unseen", "opened", "saved", "read"],
    mark_unread: ["read"],
    remove: ["saved"],
  };
  if (!allowed[action]) throw new TypeError(`Unsupported Article action: ${action}`);
  if (!allowed[action].includes(status)) return invalidTransition(state, action, status);

  if ((action === "save" && status === "saved")
    || (action === "dismiss" && status === "dismissed")
    || (action === "mark_read" && status === "read")
    || (action === "open" && existing?.signalsApplied.opened)) {
    return { ok: true, changed: false, state, undo: null };
  }

  const timestamp = resolveNow(now);
  const next = cloneState(state);
  const previousRecord = existing ? cloneState(existing) : null;
  const record = existing ? cloneState(existing) : makeRecord(article, timestamp);
  const snapshot = record.article;
  next.articles[article.id] = record;
  let preferenceSignalApplied = false;

  switch (action) {
    case "open":
      if (!existing) record.status = "opened";
      if (record.openedAt === null) record.openedAt = timestamp;
      preferenceSignalApplied = applySignal(next, record, snapshot, action);
      break;
    case "save":
      record.status = "saved";
      record.savedAt = timestamp;
      record.dismissedAt = null;
      record.readAt = null;
      preferenceSignalApplied = applySignal(next, record, snapshot, action);
      break;
    case "dismiss":
      record.status = "dismissed";
      record.dismissedAt = timestamp;
      record.savedAt = null;
      record.readAt = null;
      preferenceSignalApplied = applySignal(next, record, snapshot, action);
      break;
    case "mark_read":
      record.status = "read";
      record.readAt = timestamp;
      record.savedAt = null;
      record.dismissedAt = null;
      preferenceSignalApplied = applySignal(next, record, snapshot, action);
      break;
    case "mark_unread":
      if (record.signalsApplied.read) {
        next.preferences = reverseInteraction(next.preferences, snapshot, "read");
        record.signalsApplied.read = false;
      }
      record.status = "saved";
      record.readAt = null;
      record.savedAt = timestamp;
      record.dismissedAt = null;
      break;
    case "remove":
      record.status = "dismissed";
      record.dismissedAt = timestamp;
      record.savedAt = null;
      record.readAt = null;
      break;
  }

  const undo = undoable && (action === "save" || action === "dismiss")
    ? {
        articleId: article.id,
        action,
        previousRecord,
        preferenceSignal: preferenceSignalApplied ? EVENT_FOR_ACTION[action] : null,
      }
    : null;
  return { ok: true, changed: true, state: next, undo };
}

export function undoArticleAction(state, undoRecord) {
  if (!undoRecord || !["save", "dismiss"].includes(undoRecord.action)) {
    return { ok: false, changed: false, code: "UNDO_UNAVAILABLE", state };
  }
  if (!state.articles[undoRecord.articleId]) {
    return { ok: false, changed: false, code: "UNDO_STALE", state };
  }

  const next = cloneState(state);
  const currentRecord = next.articles[undoRecord.articleId];
  if (undoRecord.preferenceSignal) {
    next.preferences = reverseInteraction(next.preferences, currentRecord.article, undoRecord.preferenceSignal);
  }
  if (undoRecord.previousRecord === null) delete next.articles[undoRecord.articleId];
  else next.articles[undoRecord.articleId] = cloneState(undoRecord.previousRecord);
  return { ok: true, changed: true, state: next };
}

export function createUndoManager() {
  let active = null;
  return Object.freeze({
    replace(record) {
      active = record ? cloneState(record) : null;
    },
    peek() {
      return active ? cloneState(active) : null;
    },
    take() {
      const record = active ? cloneState(active) : null;
      active = null;
      return record;
    },
    clear() {
      active = null;
    },
  });
}

export function commitArticleAction({
  state,
  article,
  action,
  now,
  storage,
  undoable = false,
  undoManager,
}) {
  const transition = transitionArticle(state, article, action, { now, undoable });
  if (!transition.ok) return { ...transition, persisted: false, allowNavigation: false };
  if (!transition.changed) {
    return {
      ...transition,
      persisted: true,
      allowNavigation: action === "open",
    };
  }

  const persistence = saveState(transition.state, { storage });
  if (!persistence.ok) {
    return {
      ok: false,
      changed: false,
      persisted: false,
      allowNavigation: action === "open",
      state,
      attemptedState: transition.state,
      error: persistence.error,
      undo: null,
    };
  }
  if (undoManager && transition.undo) undoManager.replace(transition.undo);
  return {
    ok: true,
    changed: true,
    persisted: true,
    allowNavigation: action === "open",
    state: persistence.state,
    undo: transition.undo,
  };
}

export function commitUndo({ state, undoManager, storage }) {
  const record = undoManager?.peek();
  const transition = undoArticleAction(state, record);
  if (!transition.ok) return { ...transition, persisted: false };
  const persistence = saveState(transition.state, { storage });
  if (!persistence.ok) {
    return { ok: false, changed: false, persisted: false, state, attemptedState: transition.state, error: persistence.error };
  }
  undoManager.clear();
  return { ok: true, changed: true, persisted: true, state: persistence.state };
}
