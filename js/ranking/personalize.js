function interactionCount(entry) {
  return entry?.interactions ?? 0;
}

export function getSourceExploration(interactions) {
  if (interactions <= 0) return 3;
  if (interactions === 1) return 2;
  if (interactions === 2) return 1;
  return 0;
}

export function getTopicExploration(interactions) {
  if (interactions <= 0) return 2;
  if (interactions === 1) return 1;
  if (interactions === 2) return 0.5;
  return 0;
}

export function personalizeArticle(article, preferences) {
  const sourceEntry = preferences.sources?.[article.source.id];
  const sourcePreference = sourceEntry?.weight ?? 0;
  const uniqueTopicIds = [...new Set(article.tags.map((tag) => tag.id))];
  const rawTopicPreference = uniqueTopicIds.reduce(
    (total, topicId) => total + (preferences.topics?.[topicId]?.weight ?? 0),
    0,
  );
  const topicPreference = Math.max(-6, Math.min(6, rawTopicPreference));
  const sourceExploration = getSourceExploration(interactionCount(sourceEntry));
  const topicExploration = uniqueTopicIds.length === 0
    ? 0
    : getTopicExploration(Math.min(...uniqueTopicIds.map((topicId) => interactionCount(preferences.topics?.[topicId]))));
  const exploration = Math.min(3, sourceExploration + topicExploration);
  const base = article.score.base;
  return {
    total: base + sourcePreference + topicPreference + exploration,
    base,
    sourcePreference,
    topicPreference,
    exploration,
  };
}
