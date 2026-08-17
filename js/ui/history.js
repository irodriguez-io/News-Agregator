import { articleFromEntry, button, element, emitSemanticAction, numericValue, requireMount, replaceChildren } from "./dom.js";
import { categoryLabel, historyGroup, localDateTime, readingTime } from "./format.js";
import { countValue, editorialHeader, overviewBand, runRowAction } from "./view-parts.js";

const GROUPS = Object.freeze(["Today", "Yesterday", "Earlier"]);

function emptyHistory(handlers) {
  const panel = element("section", { className: "state-panel", attributes: { "aria-labelledby": "empty-history-title" } });
  panel.append(
    element("h2", { text: "No reading history yet", attributes: { id: "empty-history-title" } }),
    element("p", { text: "Articles appear here after you explicitly mark them read. There is no target to keep up with." }),
    button("Go to Discover", {
      className: "button button-quiet",
      onClick: () => emitSemanticAction(handlers, { action: "navigate", destination: "discover" }, panel),
    }),
  );
  return panel;
}

function historyRow(entry, index, handlers) {
  const article = articleFromEntry(entry) || {};
  const readAt = entry?.readAt || entry?.record?.readAt;
  const row = element("li", { className: "history-row" });
  row.append(element("div", { className: "row-position", text: localDateTime(readAt) || "Read date unavailable" }));

  const content = element("article", { className: "row-content", attributes: { "aria-labelledby": `history-title-${index}` } });
  const kicker = element("div", { className: "row-kicker" });
  const category = categoryLabel(article.category);
  if (category) kicker.append(element("span", { text: category }));
  if (article.source?.name) kicker.append(element("strong", { text: article.source.name }));
  if (article.contentType?.label) kicker.append(element("span", { text: article.contentType.label }));
  const duration = readingTime(article.readingTimeMinutes);
  if (duration) kicker.append(element("span", { text: duration }));
  content.append(
    kicker,
    element("h3", { className: "row-title", text: article.title || "", attributes: { id: `history-title-${index}` } }),
  );

  const actions = element("div", { className: "row-actions", attributes: { "aria-label": article.title ? `Actions for ${article.title}` : "Article actions" } });
  const reopen = button("Reopen ↗", { className: "text-action" });
  reopen.addEventListener("click", () => runRowAction(
    handlers,
    { action: "open", articleId: article.id },
    reopen,
    "Could not record this open locally. The publisher may still be opened by integration.",
    "Opening the publisher in a new tab.",
  ));
  const unread = button("Mark unread", { className: "text-action" });
  unread.addEventListener("click", () => runRowAction(
    handlers,
    { action: "mark_unread", articleId: article.id },
    unread,
    "The article could not be returned to Read Later.",
    "Moved back to Read Later.",
  ));
  actions.append(reopen, unread);
  row.append(content, actions);
  return row;
}

export function renderHistory(viewModel = {}, handlers = {}, target = "#app-view") {
  const mount = requireMount(target);
  const items = Array.isArray(viewModel.items) ? viewModel.items : [];
  const layout = element("section", { className: "view-layout", attributes: { "aria-labelledby": "view-title" } });
  layout.append(editorialHeader({
    eyebrow: "Completed reading",
    title: "History",
    description: "A chronological record of articles you explicitly marked read—not merely opened.",
    actionLabel: "Return to Read Later",
    onAction: () => emitSemanticAction(handlers, { action: "navigate", destination: "read_later" }, layout),
  }));

  const aggregate = numericValue(viewModel.aggregateReadingTimeMinutes);
  const latestTopic = typeof viewModel.latestTopic === "string" ? viewModel.latestTopic : viewModel.latestTopic?.label;
  const overview = overviewBand([
    { label: "Articles read", value: countValue(viewModel.historyCount) },
    { label: "Known reading time", value: aggregate !== null && aggregate > 0 ? `~${Math.trunc(aggregate)} min` : "Unavailable" },
    { label: "Latest topic", value: latestTopic || "Unavailable" },
  ]);
  if (overview && items.length) layout.append(overview);

  if (!items.length) {
    layout.append(emptyHistory(handlers));
  } else {
    const grouped = new Map(GROUPS.map((group) => [group, []]));
    items.forEach((entry, index) => {
      const readAt = entry?.readAt || entry?.record?.readAt;
      grouped.get(historyGroup(readAt, viewModel.now)).push([entry, index]);
    });

    const groups = element("div", { className: "history-groups" });
    for (const groupName of GROUPS) {
      const entries = grouped.get(groupName);
      if (!entries.length) continue;
      const section = element("section", { className: "history-group", attributes: { "aria-labelledby": `history-${groupName.toLowerCase()}` } });
      section.append(element("h2", { className: "history-group-heading", attributes: { id: `history-${groupName.toLowerCase()}` } }, [
        element("span", { text: groupName }),
        element("span", { className: "history-group-count", text: `${entries.length} ${entries.length === 1 ? "article" : "articles"}` }),
      ]));
      const list = element("ol", { className: "history-list" });
      entries.forEach(([entry, index]) => list.append(historyRow(entry, index, handlers)));
      section.append(list);
      groups.append(section);
    }
    layout.append(groups);
  }

  replaceChildren(mount, layout);
  return mount;
}
