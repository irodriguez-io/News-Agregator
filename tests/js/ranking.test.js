import test from "node:test";
import assert from "node:assert/strict";

import { buildDeck, compareCandidates } from "../../js/ranking/deck.js";
import {
  getSourceExploration,
  getTopicExploration,
  personalizeArticle,
} from "../../js/ranking/personalize.js";
import { transitionArticle } from "../../js/state/article-state.js";
import { createDefaultState } from "../../js/state/storage.js";
import { makeArticle, TIMES } from "./helpers.js";

test("personalized scoring preserves base and exposes the exact deterministic breakdown", () => {
  const article = makeArticle({ score: { base: 99, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 4 } });
  const preferences = {
    sources: { source_one: { weight: 4, interactions: 3 } },
    topics: {
      distributed_systems: { weight: 4, interactions: 3 },
      software_architecture: { weight: 4, interactions: 3 },
    },
  };
  const articleBefore = structuredClone(article);
  const preferencesBefore = structuredClone(preferences);
  assert.deepEqual(personalizeArticle(article, preferences), {
    total: 109,
    base: 99,
    sourcePreference: 4,
    topicPreference: 6,
    exploration: 0,
  });
  assert.deepEqual(article, articleBefore);
  assert.deepEqual(preferences, preferencesBefore);
});

test("topic preference sums unique tags then clamps at +6 and -6", () => {
  const article = makeArticle({ tags: [
    { id: "a", label: "A" },
    { id: "a", label: "A" },
    { id: "b", label: "B" },
  ] });
  const positive = { sources: {}, topics: { a: { weight: 5, interactions: 3 }, b: { weight: 4, interactions: 3 } } };
  const negative = { sources: {}, topics: { a: { weight: -5, interactions: 3 }, b: { weight: -3, interactions: 3 } } };
  assert.equal(personalizeArticle(article, positive).topicPreference, 6);
  assert.equal(personalizeArticle(article, negative).topicPreference, -6);
});

test("exploration follows every exact source/topic table entry and final +3 cap", () => {
  assert.deepEqual([0, 1, 2, 3, 8].map(getSourceExploration), [3, 2, 1, 0, 0]);
  assert.deepEqual([0, 1, 2, 3, 8].map(getTopicExploration), [2, 1, 0.5, 0, 0]);
  const noTags = personalizeArticle(makeArticle({ tags: [] }), { sources: { source_one: { weight: 0, interactions: 3 } }, topics: {} });
  assert.equal(noTags.exploration, 0);
  const capped = personalizeArticle(makeArticle(), { sources: {}, topics: {} });
  assert.equal(capped.exploration, 3);
});

test("candidate pre-sort uses total, base, date unknown-last, source, then Article ID", () => {
  const candidates = [
    { article: makeArticle({ id: "00000000000000000005", source: { id: "b" }, publishedAt: null }), score: { total: 90, base: 90 } },
    { article: makeArticle({ id: "00000000000000000006", source: { id: "a" }, publishedAt: null }), score: { total: 90, base: 90 } },
    { article: makeArticle({ id: "00000000000000000004", source: { id: "b" }, publishedAt: "2026-08-15T00:00:00Z" }), score: { total: 90, base: 90 } },
    { article: makeArticle({ id: "00000000000000000003", source: { id: "a" }, publishedAt: "2026-08-15T00:00:00Z" }), score: { total: 90, base: 90 } },
    { article: makeArticle({ id: "00000000000000000002", source: { id: "a" }, publishedAt: "2026-08-15T00:00:00Z" }), score: { total: 90, base: 90 } },
    { article: makeArticle({ id: "00000000000000000001", source: { id: "z" }, publishedAt: null, score: { base: 89 } }), score: { total: 90, base: 89 } },
  ];
  assert.deepEqual(candidates.toSorted(compareCandidates).map(({ article }) => article.id), [
    "00000000000000000002",
    "00000000000000000003",
    "00000000000000000004",
    "00000000000000000006",
    "00000000000000000005",
    "00000000000000000001",
  ]);
});

test("deck filters exact Discover eligibility and selected category", () => {
  const articles = [
    makeArticle({ id: "00000000000000000001", category: "technology" }),
    makeArticle({ id: "00000000000000000002", category: "science" }),
    makeArticle({ id: "00000000000000000003", category: "technology" }),
    makeArticle({ id: "00000000000000000004", category: "technology" }),
    makeArticle({ id: "00000000000000000005", category: "technology" }),
  ];
  let state = transitionArticle(createDefaultState(), articles[2], "open", { now: TIMES.first }).state;
  state = transitionArticle(state, articles[3], "save", { now: TIMES.first }).state;
  state = transitionArticle(state, articles[4], "dismiss", { now: TIMES.first }).state;
  assert.deepEqual(buildDeck({ articles, state, category: "technology" }).map((entry) => entry.article.id).sort(), [articles[0].id, articles[2].id].sort());
  assert.equal(buildDeck({ articles, state, category: "science" }).length, 1);
  assert.equal(buildDeck({ articles, state, category: "history" }).length, 0);
});

test("greedy sequencing applies same-source -8 but allows a stronger Article to win", () => {
  const articles = [
    makeArticle({ id: "00000000000000000001", source: { id: "a" }, score: { base: 100, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 5 } }),
    makeArticle({ id: "00000000000000000002", source: { id: "a" }, score: { base: 99, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 4 } }),
    makeArticle({ id: "00000000000000000003", source: { id: "b" }, score: { base: 95, sourceQuality: 48, contentType: 18, freshness: 15, topicSignal: 10, metadata: 4 } }),
  ];
  const deck = buildDeck({ articles, state: createDefaultState(), category: "technology" });
  assert.deepEqual(deck.map((entry) => entry.article.id), [articles[0].id, articles[2].id, articles[1].id]);
  assert.equal(deck[2].sequencing.sameSourcePenalty, 0);

  articles[1].score = { base: 100, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 5 };
  articles[2].score = { base: 90, sourceQuality: 48, contentType: 18, freshness: 10, topicSignal: 10, metadata: 4 };
  const strong = buildDeck({ articles, state: createDefaultState(), category: "technology" });
  assert.equal(strong[1].article.source.id, "a");
  assert.equal(strong[1].sequencing.sameSourcePenalty, -8);
});

test("All view applies third-consecutive-category -5 and category views disable it", () => {
  const articles = [
    makeArticle({ id: "00000000000000000001", source: { id: "a" }, category: "technology", score: { base: 100, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 5 } }),
    makeArticle({ id: "00000000000000000002", source: { id: "b" }, category: "technology", score: { base: 99, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 4 } }),
    makeArticle({ id: "00000000000000000003", source: { id: "c" }, category: "technology", score: { base: 96, sourceQuality: 48, contentType: 18, freshness: 15, topicSignal: 10, metadata: 5 } }),
    makeArticle({ id: "00000000000000000004", source: { id: "d" }, category: "science", score: { base: 94, sourceQuality: 48, contentType: 18, freshness: 13, topicSignal: 10, metadata: 5 } }),
  ];
  const all = buildDeck({ articles, state: createDefaultState(), category: "all" });
  assert.deepEqual(all.map((entry) => entry.article.id), [articles[0].id, articles[1].id, articles[3].id, articles[2].id]);
  const category = buildDeck({ articles, state: createDefaultState(), category: "technology" });
  assert.deepEqual(category.map((entry) => entry.article.id), [articles[0].id, articles[1].id, articles[2].id]);
  assert.equal(category.every((entry) => entry.sequencing.categoryPenalty === 0), true);

  const stronglyHigherThird = articles.map((article) => structuredClone(article));
  stronglyHigherThird[2].score = { base: 100, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 5 };
  stronglyHigherThird[0].score = { base: 99, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 4 };
  stronglyHigherThird[1].score = { base: 98, sourceQuality: 50, contentType: 20, freshness: 15, topicSignal: 10, metadata: 3 };
  stronglyHigherThird[3].score = { base: 90, sourceQuality: 48, contentType: 18, freshness: 10, topicSignal: 10, metadata: 4 };
  const strong = buildDeck({ articles: stronglyHigherThird, state: createDefaultState(), category: "all" });
  assert.equal(strong[2].article.category, "technology");
  assert.equal(strong[2].sequencing.categoryPenalty, -5);
});

test("diversity penalties are temporary and deck construction is deterministic and mutation-free", () => {
  const articles = [makeArticle(), makeArticle({ id: "00000000000000000002", source: { id: "source_two" } })];
  const state = createDefaultState();
  const beforeArticles = structuredClone(articles);
  const beforeState = structuredClone(state);
  const first = buildDeck({ articles, state, category: "all" });
  const second = buildDeck({ articles, state, category: "all" });
  assert.deepEqual(first, second);
  assert.deepEqual(articles, beforeArticles);
  assert.deepEqual(state, beforeState);
  assert.equal("sequencing" in articles[0], false);
});
