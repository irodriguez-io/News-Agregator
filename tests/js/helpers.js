export const TIMES = {
  first: "2026-08-17T12:00:00.000Z",
  second: "2026-08-17T13:00:00.000Z",
  third: "2026-08-17T14:00:00.000Z",
};

export function makeArticle(overrides = {}) {
  const article = {
    id: "00000000000000000001",
    title: "A useful article",
    url: "https://example.com/article",
    source: { id: "source_one", name: "Source One" },
    category: "technology",
    publishedAt: "2026-08-16T12:00:00Z",
    author: "Writer",
    excerpt: "A concise, plain-text description.",
    readingTimeMinutes: 4,
    tags: [
      { id: "distributed_systems", label: "Distributed Systems" },
      { id: "software_architecture", label: "Software Architecture" },
    ],
    contentType: { id: "engineering_deep_dive", label: "Engineering Deep Dive" },
    score: {
      base: 90,
      sourceQuality: 48,
      contentType: 18,
      freshness: 13,
      topicSignal: 7,
      metadata: 4,
    },
  };

  return {
    ...article,
    ...overrides,
    source: { ...article.source, ...(overrides.source ?? {}) },
    contentType: { ...article.contentType, ...(overrides.contentType ?? {}) },
    score: { ...article.score, ...(overrides.score ?? {}) },
    tags: overrides.tags ?? article.tags.map((tag) => ({ ...tag })),
  };
}

export function makeDataset(articles = [makeArticle()]) {
  return {
    schemaVersion: 1,
    generatedAt: "2026-08-17T12:00:00Z",
    pipeline: {
      enabledSourceCount: 20,
      successfulSourceCount: 20,
      failedSourceCount: 0,
      articleCount: articles.length,
    },
    articles,
  };
}

export class MemoryStorage {
  constructor(initial = {}) {
    this.values = new Map(Object.entries(initial));
    this.failGet = false;
    this.failSet = false;
    this.failRemove = false;
    this.calls = [];
  }

  getItem(key) {
    this.calls.push(["getItem", key]);
    if (this.failGet) throw new Error("get failed");
    return this.values.has(key) ? this.values.get(key) : null;
  }

  setItem(key, value) {
    this.calls.push(["setItem", key, value]);
    if (this.failSet) throw new Error("set failed");
    this.values.set(key, String(value));
  }

  removeItem(key) {
    this.calls.push(["removeItem", key]);
    if (this.failRemove) throw new Error("remove failed");
    this.values.delete(key);
  }
}

export function createUiRecorder() {
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

export function createWindowStub() {
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
