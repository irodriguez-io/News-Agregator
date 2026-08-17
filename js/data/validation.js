export const ARTICLE_ID_PATTERN = /^[0-9a-f]{20}$/;
export const IDENTIFIER_PATTERN = /^[a-z0-9][a-z0-9_]{0,99}$/;
export const CATEGORY_IDS = Object.freeze([
  "science",
  "technology",
  "literature",
  "history",
  "weightlifting",
  "iam",
  "identity_automation",
]);
export const CATEGORY_FILTER_IDS = Object.freeze(["all", ...CATEGORY_IDS]);
export const APPEARANCE_IDS = Object.freeze(["light", "dark", "system"]);
export const ARTICLE_STATUSES = Object.freeze(["opened", "saved", "dismissed", "read"]);
export const CONTENT_TYPE_IDS = Object.freeze([
  "standards_update",
  "official_release_notes",
  "research_reporting",
  "reported_science",
  "engineering_deep_dive",
  "evidence_based_training",
  "historical_essay",
  "engineering_journalism",
  "literary_essay",
  "reported_journalism",
]);

const DANGEROUS_KEYS = new Set(["__proto__", "prototype", "constructor"]);
const UTC_TIMESTAMP_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?Z$/;

export class ValidationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = "ValidationError";
    this.code = code;
  }
}

export function isPlainObject(value) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) return false;
  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
}

function fail(path, message) {
  throw new ValidationError("INVALID_STRUCTURE", `${path} ${message}`);
}

export function assertNoDangerousKeys(value, path = "value") {
  if (value === null || typeof value !== "object") return;
  for (const key of Object.keys(value)) {
    if (DANGEROUS_KEYS.has(key)) fail(`${path}.${key}`, "uses a dangerous property name");
    assertNoDangerousKeys(value[key], `${path}.${key}`);
  }
}

function expectObject(value, path) {
  if (!isPlainObject(value)) fail(path, "must be an object");
}

function expectExactKeys(value, keys, path) {
  expectObject(value, path);
  const actual = Object.keys(value).sort();
  const expected = [...keys].sort();
  if (actual.length !== expected.length || actual.some((key, index) => key !== expected[index])) {
    fail(path, "has an invalid structure");
  }
}

function expectString(value, path, { min = 0, max = Infinity, pattern } = {}) {
  if (typeof value !== "string" || value.length < min || value.length > max || (pattern && !pattern.test(value))) {
    fail(path, "is invalid");
  }
  return value;
}

function expectInteger(value, path, minimum, maximum = Number.MAX_SAFE_INTEGER) {
  if (!Number.isInteger(value) || value < minimum || value > maximum) fail(path, "is invalid");
  return value;
}

export function isUtcTimestamp(value) {
  if (typeof value !== "string") return false;
  const match = UTC_TIMESTAMP_PATTERN.exec(value);
  if (!match) return false;
  const milliseconds = Date.parse(value);
  if (!Number.isFinite(milliseconds)) return false;
  const parsed = new Date(milliseconds);
  return parsed.getUTCFullYear() === Number(match[1])
    && parsed.getUTCMonth() + 1 === Number(match[2])
    && parsed.getUTCDate() === Number(match[3])
    && parsed.getUTCHours() === Number(match[4])
    && parsed.getUTCMinutes() === Number(match[5])
    && parsed.getUTCSeconds() === Number(match[6]);
}

function expectIdentifier(value, path) {
  const identifier = expectString(value, path, { pattern: IDENTIFIER_PATTERN });
  if (DANGEROUS_KEYS.has(identifier)) fail(path, "uses a dangerous identifier");
  return identifier;
}

function expectTimestamp(value, path, { nullable = false } = {}) {
  if (nullable && value === null) return null;
  if (!isUtcTimestamp(value)) fail(path, "must be a UTC ISO-8601 timestamp");
  return value;
}

export function isSafeHttpUrl(value) {
  if (typeof value !== "string") return false;
  try {
    const parsed = new URL(value);
    return (parsed.protocol === "http:" || parsed.protocol === "https:") && parsed.hostname.length > 0;
  } catch {
    return false;
  }
}

export function validateArticle(candidate, path = "article") {
  assertNoDangerousKeys(candidate, path);
  expectExactKeys(candidate, [
    "id", "title", "url", "source", "category", "publishedAt", "author", "excerpt",
    "readingTimeMinutes", "tags", "contentType", "score",
  ], path);

  const id = expectString(candidate.id, `${path}.id`, { pattern: ARTICLE_ID_PATTERN });
  const title = expectString(candidate.title, `${path}.title`, { min: 1, max: 500 });
  if (!/[^\s\p{P}\p{S}]/u.test(title)) fail(`${path}.title`, "must contain readable text");
  if (!isSafeHttpUrl(candidate.url)) fail(`${path}.url`, "must be an external HTTP/HTTPS URL");

  expectExactKeys(candidate.source, ["id", "name"], `${path}.source`);
  const source = {
    id: expectIdentifier(candidate.source.id, `${path}.source.id`),
    name: expectString(candidate.source.name, `${path}.source.name`, { min: 1, max: 200 }),
  };

  if (!CATEGORY_IDS.includes(candidate.category)) fail(`${path}.category`, "is invalid");
  const publishedAt = expectTimestamp(candidate.publishedAt, `${path}.publishedAt`, { nullable: true });
  const author = candidate.author === null
    ? null
    : expectString(candidate.author, `${path}.author`, { min: 1, max: 200 });
  const excerpt = expectString(candidate.excerpt, `${path}.excerpt`, { max: 800 });
  const readingTimeMinutes = candidate.readingTimeMinutes === null
    ? null
    : expectInteger(candidate.readingTimeMinutes, `${path}.readingTimeMinutes`, 1);

  if (!Array.isArray(candidate.tags)) fail(`${path}.tags`, "must be an array");
  const seenTags = new Set();
  const tags = candidate.tags.map((tag, index) => {
    const tagPath = `${path}.tags[${index}]`;
    expectExactKeys(tag, ["id", "label"], tagPath);
    const tagId = expectIdentifier(tag.id, `${tagPath}.id`);
    if (seenTags.has(tagId)) fail(`${tagPath}.id`, "must be unique");
    seenTags.add(tagId);
    return {
      id: tagId,
      label: expectString(tag.label, `${tagPath}.label`, { min: 1, max: 200 }),
    };
  });

  expectExactKeys(candidate.contentType, ["id", "label"], `${path}.contentType`);
  if (!CONTENT_TYPE_IDS.includes(candidate.contentType.id)) fail(`${path}.contentType.id`, "is invalid");
  const contentType = {
    id: candidate.contentType.id,
    label: expectString(candidate.contentType.label, `${path}.contentType.label`, { min: 1, max: 200 }),
  };

  expectExactKeys(candidate.score, ["base", "sourceQuality", "contentType", "freshness", "topicSignal", "metadata"], `${path}.score`);
  const score = {
    base: expectInteger(candidate.score.base, `${path}.score.base`, 0, 100),
    sourceQuality: expectInteger(candidate.score.sourceQuality, `${path}.score.sourceQuality`, 0, 50),
    contentType: expectInteger(candidate.score.contentType, `${path}.score.contentType`, 0, 20),
    freshness: expectInteger(candidate.score.freshness, `${path}.score.freshness`, 0, 15),
    topicSignal: expectInteger(candidate.score.topicSignal, `${path}.score.topicSignal`, 0, 10),
    metadata: expectInteger(candidate.score.metadata, `${path}.score.metadata`, 0, 5),
  };
  if (score.base !== score.sourceQuality + score.contentType + score.freshness + score.topicSignal + score.metadata) {
    fail(`${path}.score.base`, "must equal its component sum");
  }

  return {
    id,
    title,
    url: candidate.url,
    source,
    category: candidate.category,
    publishedAt,
    author,
    excerpt,
    readingTimeMinutes,
    tags,
    contentType,
    score,
  };
}

export function validateArticleDataset(candidate) {
  assertNoDangerousKeys(candidate, "dataset");
  expectExactKeys(candidate, ["schemaVersion", "generatedAt", "pipeline", "articles"], "dataset");
  if (candidate.schemaVersion !== 1) {
    throw new ValidationError("UNSUPPORTED_SCHEMA", "Unsupported ArticleDataset schema version");
  }
  const generatedAt = expectTimestamp(candidate.generatedAt, "dataset.generatedAt");
  expectExactKeys(candidate.pipeline, ["enabledSourceCount", "successfulSourceCount", "failedSourceCount", "articleCount"], "dataset.pipeline");
  const pipeline = {
    enabledSourceCount: expectInteger(candidate.pipeline.enabledSourceCount, "dataset.pipeline.enabledSourceCount", 0),
    successfulSourceCount: expectInteger(candidate.pipeline.successfulSourceCount, "dataset.pipeline.successfulSourceCount", 0),
    failedSourceCount: expectInteger(candidate.pipeline.failedSourceCount, "dataset.pipeline.failedSourceCount", 0),
    articleCount: expectInteger(candidate.pipeline.articleCount, "dataset.pipeline.articleCount", 0),
  };
  if (pipeline.successfulSourceCount + pipeline.failedSourceCount !== pipeline.enabledSourceCount) {
    fail("dataset.pipeline", "source counts are inconsistent");
  }
  if (!Array.isArray(candidate.articles) || pipeline.articleCount !== candidate.articles.length) {
    fail("dataset.articles", "does not match pipeline.articleCount");
  }
  const ids = new Set();
  const articles = candidate.articles.map((article, index) => {
    const validated = validateArticle(article, `dataset.articles[${index}]`);
    if (ids.has(validated.id)) fail(`dataset.articles[${index}].id`, "must be unique");
    ids.add(validated.id);
    return validated;
  });
  return { schemaVersion: 1, generatedAt, pipeline, articles };
}
