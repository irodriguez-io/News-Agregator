export const CATEGORY_OPTIONS = Object.freeze([
  ["all", "All"],
  ["science", "Science"],
  ["technology", "Technology"],
  ["literature", "Literature"],
  ["history", "History"],
  ["weightlifting", "Weightlifting"],
  ["iam", "IAM"],
  ["identity_automation", "Identity Automation"],
]);

const CATEGORY_LABELS = new Map(CATEGORY_OPTIONS);

export function categoryLabel(category) {
  return CATEGORY_LABELS.get(category) || "";
}

export function validDate(value) {
  if (typeof value !== "string" || !value.trim()) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function relativeDate(value, nowValue = Date.now()) {
  const date = validDate(value);
  const now = nowValue instanceof Date ? nowValue : new Date(nowValue);
  if (!date || Number.isNaN(now.getTime())) return "";

  const delta = Math.max(0, now.getTime() - date.getTime());
  const hours = Math.floor(delta / 3_600_000);
  if (hours < 1) return "Now";
  if (hours < 24) return `${hours}h`;

  const days = Math.floor(hours / 24);
  if (days < 31) return `${days}d`;
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", year: "numeric" }).format(date);
}

export function localDateTime(value) {
  const date = validDate(value);
  if (!date) return "";
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

export function historyGroup(value, nowValue = Date.now()) {
  const date = validDate(value);
  const now = nowValue instanceof Date ? nowValue : new Date(nowValue);
  if (!date || Number.isNaN(now.getTime())) return "Earlier";

  const startToday = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  const startValue = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
  const days = Math.round((startToday - startValue) / 86_400_000);
  if (days === 0) return "Today";
  if (days === 1) return "Yesterday";
  return "Earlier";
}

export function readingTime(value) {
  return Number.isInteger(value) && value > 0 ? `~${value} min` : "";
}
