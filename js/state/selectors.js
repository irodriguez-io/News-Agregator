import { cloneState } from "./storage.js";

export function isDiscoverEligible(record) {
  return record == null || record.status === "opened";
}

function timestampDescending(field) {
  return (left, right) => Date.parse(right[field]) - Date.parse(left[field]);
}

export function selectReadLater(state) {
  return Object.values(state.articles)
    .filter((record) => record.status === "saved")
    .toSorted(timestampDescending("savedAt"))
    .map(cloneState);
}

export function selectHistory(state) {
  return Object.values(state.articles)
    .filter((record) => record.status === "read")
    .toSorted(timestampDescending("readAt"))
    .map(cloneState);
}

export function getNavigationCounts(state) {
  let readLater = 0;
  let history = 0;
  for (const record of Object.values(state.articles)) {
    if (record.status === "saved") readLater += 1;
    if (record.status === "read") history += 1;
  }
  return { readLater, history };
}

function aggregate(records, topicProperty) {
  let knownReadingTimeMinutes = 0;
  let unknownReadingTimeCount = 0;
  for (const { article } of records) {
    if (article.readingTimeMinutes === null) unknownReadingTimeCount += 1;
    else knownReadingTimeMinutes += article.readingTimeMinutes;
  }
  const firstTopic = records.find(({ article }) => article.tags.length > 0)?.article.tags[0]?.id ?? null;
  return {
    count: records.length,
    knownReadingTimeMinutes,
    unknownReadingTimeCount,
    [topicProperty]: firstTopic,
  };
}

export function getReadLaterAggregate(state) {
  return aggregate(selectReadLater(state), "firstAvailableTopic");
}

export function getHistoryAggregate(state) {
  return aggregate(selectHistory(state), "newestAvailableTopic");
}
