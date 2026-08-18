import {
  APPEARANCE_IDS,
  ARTICLE_ID_PATTERN,
  ARTICLE_STATUSES,
  CATEGORY_FILTER_IDS,
  IDENTIFIER_PATTERN,
  ValidationError,
  assertNoDangerousKeys,
  isPlainObject,
  isUtcTimestamp,
  validateArticle,
} from "../data/validation.js";

export const LOCAL_STORAGE_KEY = "intentionalReading:v1";
export const LOCAL_STATE_SCHEMA_VERSION = 1;
export const MAX_IMPORT_BYTES = 5 * 1024 * 1024;

const ROOT_KEYS = ["schemaVersion", "preferences", "articles", "settings", "session"];
const RECORD_KEYS = [
  "article", "status", "firstSeenAt", "openedAt", "savedAt", "dismissedAt", "readAt", "signalsApplied",
];
const SIGNAL_KEYS = ["opened", "saved", "dismissed", "read"];
const recoveryLockedStorage = new WeakSet();

export class StateStorageError extends Error {
  constructor(code, message) {
    super(message);
    this.name = "StateStorageError";
    this.code = code;
    this.recoverable = true;
  }
}

function storageError(code, message) {
  return new StateStorageError(code, message);
}

function cloneJson(value) {
  return JSON.parse(JSON.stringify(value));
}

function expectObject(value, path) {
  if (!isPlainObject(value)) throw new ValidationError("INVALID_STATE", `${path} must be an object`);
}

function expectExactKeys(value, expected, path) {
  expectObject(value, path);
  const actualKeys = Object.keys(value).sort();
  const expectedKeys = [...expected].sort();
  if (actualKeys.length !== expectedKeys.length || actualKeys.some((key, index) => key !== expectedKeys[index])) {
    throw new ValidationError("INVALID_STATE", `${path} has an invalid structure`);
  }
}

function expectNullableTimestamp(value, path) {
  if (value !== null && !isUtcTimestamp(value)) {
    throw new ValidationError("INVALID_STATE", `${path} must be null or a UTC ISO-8601 timestamp`);
  }
  return value;
}

function validatePreferenceMap(candidate, path) {
  expectObject(candidate, path);
  const validated = {};
  for (const [key, entry] of Object.entries(candidate)) {
    if (!IDENTIFIER_PATTERN.test(key)) throw new ValidationError("INVALID_STATE", `${path} contains an invalid key`);
    expectExactKeys(entry, ["weight", "interactions"], `${path}.${key}`);
    if (!Number.isFinite(entry.weight) || entry.weight < -5 || entry.weight > 5) {
      throw new ValidationError("INVALID_STATE", `${path}.${key}.weight is outside V1 bounds`);
    }
    if (!Number.isInteger(entry.interactions) || entry.interactions < 0) {
      throw new ValidationError("INVALID_STATE", `${path}.${key}.interactions is invalid`);
    }
    validated[key] = { weight: entry.weight, interactions: entry.interactions };
  }
  return validated;
}

function validateRecord(candidate, articleId, path) {
  expectExactKeys(candidate, RECORD_KEYS, path);
  const article = validateArticle(candidate.article, `${path}.article`);
  if (article.id !== articleId) throw new ValidationError("INVALID_STATE", `${path}.article.id does not match its map key`);
  if (!ARTICLE_STATUSES.includes(candidate.status)) throw new ValidationError("INVALID_STATE", `${path}.status is invalid`);
  if (!isUtcTimestamp(candidate.firstSeenAt)) throw new ValidationError("INVALID_STATE", `${path}.firstSeenAt is invalid`);

  const record = {
    article,
    status: candidate.status,
    firstSeenAt: candidate.firstSeenAt,
    openedAt: expectNullableTimestamp(candidate.openedAt, `${path}.openedAt`),
    savedAt: expectNullableTimestamp(candidate.savedAt, `${path}.savedAt`),
    dismissedAt: expectNullableTimestamp(candidate.dismissedAt, `${path}.dismissedAt`),
    readAt: expectNullableTimestamp(candidate.readAt, `${path}.readAt`),
    signalsApplied: {},
  };

  expectExactKeys(candidate.signalsApplied, SIGNAL_KEYS, `${path}.signalsApplied`);
  for (const signal of SIGNAL_KEYS) {
    if (typeof candidate.signalsApplied[signal] !== "boolean") {
      throw new ValidationError("INVALID_STATE", `${path}.signalsApplied.${signal} must be boolean`);
    }
    record.signalsApplied[signal] = candidate.signalsApplied[signal];
  }

  if (record.signalsApplied.opened !== (record.openedAt !== null)) {
    throw new ValidationError("INVALID_STATE", `${path} has inconsistent Open signal metadata`);
  }
  if (record.signalsApplied.dismissed && record.status !== "dismissed") {
    throw new ValidationError("INVALID_STATE", `${path} has an inconsistent Dismiss signal`);
  }
  if (record.signalsApplied.read !== (record.status === "read")) {
    throw new ValidationError("INVALID_STATE", `${path} has an inconsistent Read signal`);
  }

  const requiredTimestamp = {
    opened: "openedAt",
    saved: "savedAt",
    dismissed: "dismissedAt",
    read: "readAt",
  }[record.status];
  if (record[requiredTimestamp] === null) throw new ValidationError("INVALID_STATE", `${path}.${requiredTimestamp} is required`);
  if (record.status !== "saved" && record.savedAt !== null) throw new ValidationError("INVALID_STATE", `${path}.savedAt is not currently applicable`);
  if (record.status !== "dismissed" && record.dismissedAt !== null) throw new ValidationError("INVALID_STATE", `${path}.dismissedAt is not currently applicable`);
  if (record.status !== "read" && record.readAt !== null) throw new ValidationError("INVALID_STATE", `${path}.readAt is not currently applicable`);
  const firstSeenTime = Date.parse(record.firstSeenAt);
  for (const field of ["openedAt", "savedAt", "dismissedAt", "readAt"]) {
    if (record[field] !== null && Date.parse(record[field]) < firstSeenTime) {
      throw new ValidationError("INVALID_STATE", `${path}.${field} predates firstSeenAt`);
    }
  }
  return record;
}

export function createDefaultState() {
  return {
    schemaVersion: 1,
    preferences: { sources: {}, topics: {} },
    articles: {},
    settings: { appearance: "system" },
    session: { lastCategory: "all" },
  };
}

export function validateState(candidate) {
  assertNoDangerousKeys(candidate, "state");
  expectExactKeys(candidate, ROOT_KEYS, "state");
  if (candidate.schemaVersion !== LOCAL_STATE_SCHEMA_VERSION) {
    throw new ValidationError("UNSUPPORTED_SCHEMA", "Unsupported Local State schema version");
  }

  expectExactKeys(candidate.preferences, ["sources", "topics"], "state.preferences");
  const preferences = {
    sources: validatePreferenceMap(candidate.preferences.sources, "state.preferences.sources"),
    topics: validatePreferenceMap(candidate.preferences.topics, "state.preferences.topics"),
  };

  expectObject(candidate.articles, "state.articles");
  const articles = {};
  for (const [articleId, record] of Object.entries(candidate.articles)) {
    if (!ARTICLE_ID_PATTERN.test(articleId)) throw new ValidationError("INVALID_STATE", "state.articles contains an invalid Article ID");
    articles[articleId] = validateRecord(record, articleId, `state.articles.${articleId}`);
  }

  expectExactKeys(candidate.settings, ["appearance"], "state.settings");
  if (!APPEARANCE_IDS.includes(candidate.settings.appearance)) throw new ValidationError("INVALID_STATE", "state.settings.appearance is invalid");
  expectExactKeys(candidate.session, ["lastCategory"], "state.session");
  if (!CATEGORY_FILTER_IDS.includes(candidate.session.lastCategory)) throw new ValidationError("INVALID_STATE", "state.session.lastCategory is invalid");

  return {
    schemaVersion: 1,
    preferences,
    articles,
    settings: { appearance: candidate.settings.appearance },
    session: { lastCategory: candidate.session.lastCategory },
  };
}

export function migrateState(state, fromVersion, toVersion = LOCAL_STATE_SCHEMA_VERSION) {
  if (fromVersion === 1 && toVersion === 1) return validateState(state);
  throw storageError("UNSUPPORTED_SCHEMA", `No Local State migration exists from version ${fromVersion} to ${toVersion}`);
}

function resolveStorage(storage) {
  if (storage) return storage;
  if (typeof globalThis.localStorage !== "undefined") return globalThis.localStorage;
  throw storageError("STORAGE_UNAVAILABLE", "Browser local storage is unavailable");
}

function recoveryRequired(adapter) {
  recoveryLockedStorage.add(adapter);
}

function persistValidatedState(validated, adapter, { explicitRecovery = false } = {}) {
  if (recoveryLockedStorage.has(adapter) && !explicitRecovery) {
    return {
      ok: false,
      state: null,
      error: storageError(
        "RECOVERY_REQUIRED",
        "Stored local state must be reset or replaced by a valid import before changes can be persisted",
      ),
    };
  }

  try {
    adapter.setItem(LOCAL_STORAGE_KEY, JSON.stringify(validated));
  } catch {
    return { ok: false, state: null, error: storageError("WRITE_FAILED", "Local state could not be persisted") };
  }
  if (explicitRecovery) recoveryLockedStorage.delete(adapter);
  return { ok: true, state: validated };
}

export function loadState({ storage } = {}) {
  let adapter;
  try {
    adapter = resolveStorage(storage);
  } catch (error) {
    return { ok: false, state: createDefaultState(), error };
  }

  let rawValue;
  try {
    rawValue = adapter.getItem(LOCAL_STORAGE_KEY);
  } catch {
    return { ok: false, state: createDefaultState(), error: storageError("READ_FAILED", "Local state could not be read") };
  }
  if (rawValue === null) return { ok: true, state: createDefaultState(), source: "default" };

  let parsed;
  try {
    parsed = JSON.parse(rawValue);
  } catch {
    recoveryRequired(adapter);
    return {
      ok: false,
      state: createDefaultState(),
      rawValue,
      error: storageError("MALFORMED_JSON", "Stored local state is malformed"),
    };
  }

  if (!isPlainObject(parsed)) {
    recoveryRequired(adapter);
    return {
      ok: false,
      state: createDefaultState(),
      rawValue,
      error: storageError("INVALID_STATE", "Stored local state is structurally invalid"),
    };
  }
  if (parsed.schemaVersion !== LOCAL_STATE_SCHEMA_VERSION) {
    recoveryRequired(adapter);
    return {
      ok: false,
      state: createDefaultState(),
      rawValue,
      error: storageError("UNSUPPORTED_SCHEMA", "Stored local state uses an unsupported schema"),
    };
  }

  try {
    return { ok: true, state: migrateState(parsed, parsed.schemaVersion), source: "storage" };
  } catch (error) {
    recoveryRequired(adapter);
    return {
      ok: false,
      state: createDefaultState(),
      rawValue,
      error: error instanceof StateStorageError
        ? error
        : storageError("INVALID_STATE", "Stored local state is structurally invalid"),
    };
  }
}

export function saveState(state, { storage } = {}) {
  let validated;
  try {
    validated = validateState(state);
  } catch {
    return { ok: false, state: null, error: storageError("INVALID_STATE", "Local state is structurally invalid") };
  }

  let adapter;
  try {
    adapter = resolveStorage(storage);
  } catch {
    return { ok: false, state: null, error: storageError("WRITE_FAILED", "Local state could not be persisted") };
  }
  return persistValidatedState(validated, adapter);
}

export function resetState({ storage } = {}) {
  let adapter;
  try {
    adapter = resolveStorage(storage);
    adapter.removeItem(LOCAL_STORAGE_KEY);
  } catch {
    return { ok: false, state: null, error: storageError("WRITE_FAILED", "Local state could not be reset") };
  }
  recoveryLockedStorage.delete(adapter);
  return { ok: true, state: createDefaultState() };
}

export function exportState(state) {
  return JSON.stringify(validateState(state), null, 2);
}

export function importState(serializedState, { storage } = {}) {
  if (typeof serializedState !== "string") {
    return { ok: false, state: null, error: storageError("INVALID_IMPORT", "Imported local state must be serialized JSON") };
  }
  if (new TextEncoder().encode(serializedState).byteLength > MAX_IMPORT_BYTES) {
    return { ok: false, state: null, error: storageError("IMPORT_TOO_LARGE", "Imported local state exceeds 5 MiB") };
  }

  let candidate;
  try {
    candidate = JSON.parse(serializedState);
  } catch {
    return { ok: false, state: null, error: storageError("MALFORMED_JSON", "Imported local state is malformed") };
  }
  if (!isPlainObject(candidate)) {
    return { ok: false, state: null, error: storageError("INVALID_IMPORT", "Imported local state is structurally invalid") };
  }
  if (candidate.schemaVersion !== LOCAL_STATE_SCHEMA_VERSION) {
    return { ok: false, state: null, error: storageError("UNSUPPORTED_SCHEMA", "Imported local state uses an unsupported schema") };
  }

  let validated;
  try {
    validated = validateState(candidate);
  } catch {
    return { ok: false, state: null, error: storageError("INVALID_IMPORT", "Imported local state is structurally invalid") };
  }

  let adapter;
  try {
    adapter = resolveStorage(storage);
  } catch {
    return { ok: false, state: null, error: storageError("WRITE_FAILED", "Local state could not be persisted") };
  }
  return persistValidatedState(validated, adapter, { explicitRecovery: true });
}

export function cloneState(state) {
  return cloneJson(state);
}
