export const INTERACTION_DELTAS = Object.freeze({
  open: Object.freeze({ source: 0.1, topic: 0.05 }),
  save: Object.freeze({ source: 0.45, topic: 0.3 }),
  dismiss: Object.freeze({ source: -0.35, topic: -0.2 }),
  read: Object.freeze({ source: 0.25, topic: 0.2 }),
});

function clampWeight(value) {
  return Math.max(-5, Math.min(5, Math.round(value * 1e10) / 1e10));
}

function clonePreferences(preferences) {
  return {
    sources: Object.fromEntries(Object.entries(preferences?.sources ?? {}).map(([key, entry]) => [key, { ...entry }])),
    topics: Object.fromEntries(Object.entries(preferences?.topics ?? {}).map(([key, entry]) => [key, { ...entry }])),
  };
}

function getDelta(event) {
  const delta = INTERACTION_DELTAS[event];
  if (!delta) throw new TypeError(`Unsupported interaction event: ${event}`);
  return delta;
}

function uniqueTopicIds(article) {
  return [...new Set((article.tags ?? []).map((tag) => tag.id))];
}

function applyToEntry(map, key, delta) {
  const current = map[key] ?? { weight: 0, interactions: 0 };
  map[key] = {
    weight: clampWeight(current.weight + delta),
    interactions: current.interactions + 1,
  };
}

function reverseFromEntry(map, key, delta) {
  const current = map[key];
  if (!current || current.interactions <= 0) return;
  const next = {
    weight: clampWeight(current.weight - delta),
    interactions: Math.max(0, current.interactions - 1),
  };
  if (next.interactions === 0 && next.weight === 0) delete map[key];
  else map[key] = next;
}

export function applyInteraction(preferences, article, event) {
  const delta = getDelta(event);
  const next = clonePreferences(preferences);
  applyToEntry(next.sources, article.source.id, delta.source);
  for (const topicId of uniqueTopicIds(article)) applyToEntry(next.topics, topicId, delta.topic);
  return next;
}

export function reverseInteraction(preferences, article, event) {
  const delta = getDelta(event);
  const next = clonePreferences(preferences);
  reverseFromEntry(next.sources, article.source.id, delta.source);
  for (const topicId of uniqueTopicIds(article)) reverseFromEntry(next.topics, topicId, delta.topic);
  return next;
}
