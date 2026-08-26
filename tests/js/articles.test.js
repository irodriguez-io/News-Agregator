import test from "node:test";
import assert from "node:assert/strict";

import { DatasetError, loadArticleDataset } from "../../js/data/articles.js";
import { ValidationError, validateArticleDataset } from "../../js/data/validation.js";
import { makeArticle, makeDataset } from "./helpers.js";

function response(body, { ok = true, status = 200, jsonError = null } = {}) {
  return {
    ok,
    status,
    async json() {
      if (jsonError) throw jsonError;
      return body;
    },
  };
}

test("dataset loader returns a valid empty or populated ArticleDataset v1 without storage access", async () => {
  const dataset = makeDataset();
  let requestedUrl;
  const loaded = await loadArticleDataset("/data/articles.json", {
    fetchImpl: async (url) => {
      requestedUrl = url;
      return response(dataset);
    },
  });

  assert.equal(requestedUrl, "/data/articles.json");
  assert.deepEqual(loaded, dataset);
  assert.notEqual(loaded, dataset);

  const empty = makeDataset([]);
  assert.deepEqual(
    await loadArticleDataset(undefined, { fetchImpl: async () => response(empty) }),
    empty,
  );
});

test("dataset loader normalizes network, HTTP, and invalid JSON failures", async (t) => {
  await t.test("network", async () => {
    await assert.rejects(
      loadArticleDataset(undefined, { fetchImpl: async () => { throw new Error("offline payload"); } }),
      (error) => error instanceof DatasetError && error.code === "FETCH_FAILED" && !error.message.includes("payload"),
    );
  });

  await t.test("HTTP", async () => {
    await assert.rejects(
      loadArticleDataset(undefined, { fetchImpl: async () => response({}, { ok: false, status: 503 }) }),
      (error) => error instanceof DatasetError && error.code === "FETCH_FAILED",
    );
  });

  await t.test("JSON", async () => {
    await assert.rejects(
      loadArticleDataset(undefined, { fetchImpl: async () => response(null, { jsonError: new Error("bad raw") }) }),
      (error) => error instanceof DatasetError && error.code === "INVALID_JSON" && !error.message.includes("bad raw"),
    );
  });
});

test("dataset loader rejects unsupported schemas distinctly", async () => {
  await assert.rejects(
    loadArticleDataset(undefined, {
      fetchImpl: async () => response({ ...makeDataset(), schemaVersion: 2 }),
    }),
    (error) => error instanceof DatasetError && error.code === "UNSUPPORTED_SCHEMA",
  );
});

test("dataset loader rejects structurally unusable top-level and Article data", async (t) => {
  const invalid = [
    null,
    { ...makeDataset(), generatedAt: "yesterday" },
    { ...makeDataset(), pipeline: { ...makeDataset().pipeline, articleCount: 4 } },
    makeDataset([{ ...makeArticle(), url: "javascript:alert(1)" }]),
    makeDataset([{ ...makeArticle(), id: "not-an-id" }]),
    makeDataset([{ ...makeArticle(), source: { id: "constructor", name: "Unsafe" } }]),
    makeDataset([{ ...makeArticle(), publishedAt: "2026-02-30T12:00:00Z" }]),
    makeDataset([{ ...makeArticle(), score: { ...makeArticle().score, base: 89 } }]),
  ];

  for (const candidate of invalid) {
    await t.test(String(invalid.indexOf(candidate)), async () => {
      await assert.rejects(
        loadArticleDataset(undefined, { fetchImpl: async () => response(candidate) }),
        (error) => error instanceof DatasetError && error.code === "INVALID_DATASET",
      );
    });
  }
});

test("Scenario: a reading time of one minute is refused", () => {
  // Given
  const dataset = makeDataset([makeArticle({ readingTimeMinutes: 1 })]);

  // When / Then
  assert.throws(
    () => validateArticleDataset(dataset),
    (error) => error instanceof ValidationError && error.message.includes("readingTimeMinutes"),
  );
});

test("Scenario: the smallest reading time the pipeline can emit is accepted", () => {
  // Given
  const dataset = makeDataset([makeArticle({ readingTimeMinutes: 2 })]);

  // When
  const validated = validateArticleDataset(dataset);

  // Then
  assert.equal(validated.articles[0].readingTimeMinutes, 2);
});

test("Scenario: an absent reading time is still accepted", () => {
  // Given
  const dataset = makeDataset([makeArticle({ readingTimeMinutes: null })]);

  // When
  const validated = validateArticleDataset(dataset);

  // Then
  assert.equal(validated.articles[0].readingTimeMinutes, null);
});

test("Scenario: a seventh tag is refused", () => {
  // Given
  const tags = Array.from({ length: 7 }, (_, index) => ({
    id: `tag_${index + 1}`,
    label: `Tag ${index + 1}`,
  }));
  const dataset = makeDataset([makeArticle({ tags })]);

  // When / Then
  assert.throws(
    () => validateArticleDataset(dataset),
    (error) => error instanceof ValidationError && error.message.includes("dataset.articles[0].tags"),
  );
});

test("Scenario: six tags are accepted, because the pipeline can emit six", () => {
  // Given
  const tags = Array.from({ length: 6 }, (_, index) => ({
    id: `tag_${index + 1}`,
    label: `Tag ${index + 1}`,
  }));
  const dataset = makeDataset([makeArticle({ tags })]);

  // When
  const validated = validateArticleDataset(dataset);

  // Then
  assert.deepEqual(validated.articles[0].tags, tags);
});

test("Scenario: an article with no tags is still accepted", () => {
  // Given
  const dataset = makeDataset([makeArticle({ tags: [] })]);

  // When
  const validated = validateArticleDataset(dataset);

  // Then
  assert.deepEqual(validated.articles[0].tags, []);
});

test("Scenario: duplicate tag ids are still refused", () => {
  // Given
  const dataset = makeDataset([makeArticle({
    tags: [
      { id: "duplicate_tag", label: "First label" },
      { id: "duplicate_tag", label: "Second label" },
    ],
  })]);

  // When / Then
  assert.throws(
    () => validateArticleDataset(dataset),
    (error) => error instanceof ValidationError
      && error.message.includes("dataset.articles[0].tags[1].id"),
  );
});
