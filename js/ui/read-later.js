import { articleFromEntry, button, element, emitSemanticAction, numericValue, requireMount, replaceChildren } from "./dom.js";
import { categoryLabel, readingTime, relativeDate } from "./format.js";
import { countValue, editorialHeader, overviewBand, runRowAction } from "./view-parts.js";

function emptyQueue(handlers) {
  const panel = element("section", { className: "state-panel", attributes: { "aria-labelledby": "empty-queue-title" } });
  panel.append(
    element("h2", { text: "Your reading queue is open", attributes: { id: "empty-queue-title" } }),
    element("p", { text: "Save worthwhile articles from Discover to build your reading queue." }),
    button("Return to Discover", {
      className: "button button-quiet",
      onClick: () => emitSemanticAction(handlers, { action: "navigate", destination: "discover" }, panel),
    }),
  );
  return panel;
}

function topicTags(article) {
  const tags = Array.isArray(article?.tags) ? article.tags.slice(0, 3) : [];
  if (!tags.length) return null;
  const list = element("ul", { className: "topic-tags", attributes: { "aria-label": "Article topics" } });
  for (const tag of tags) {
    if (tag?.label) list.append(element("li", { className: "topic-tag", text: tag.label }));
  }
  return list.childElementCount ? list : null;
}

function queueRow(entry, index, viewModel, handlers) {
  const article = articleFromEntry(entry) || {};
  const row = element("li", { className: "reading-row" });
  const savedAt = entry?.savedAt || entry?.record?.savedAt;
  const age = relativeDate(savedAt, viewModel.now);
  const position = element("div", { className: "row-position" });
  position.append(
    element("span", { text: `Queue ${String(index + 1).padStart(2, "0")}` }),
    age ? element("div", { text: `Saved ${age}` }) : null,
  );

  const content = element("article", { className: "row-content", attributes: { "aria-labelledby": `queue-title-${index}` } });
  const kicker = element("div", { className: "row-kicker" });
  if (article.source?.name) kicker.append(element("strong", { text: article.source.name }));
  const category = categoryLabel(article.category);
  if (category) kicker.append(element("span", { text: category }));
  const duration = readingTime(article.readingTimeMinutes);
  if (duration) kicker.append(element("span", { text: duration }));
  if (article.contentType?.label) kicker.append(element("span", { text: article.contentType.label }));
  content.append(
    kicker,
    element("h2", { className: "row-title", text: article.title || "", attributes: { id: `queue-title-${index}` } }),
  );
  const tags = topicTags(article);
  if (tags) content.append(tags);

  const actions = element("div", { className: "row-actions", attributes: { "aria-label": article.title ? `Actions for ${article.title}` : "Article actions" } });
  const specs = [
    ["Read ↗", "open", "Could not record this open locally. The publisher may still be opened by integration.", "Opening the publisher in a new tab."],
    ["Mark read", "mark_read", "The article could not be moved to History.", "Moved to History."],
    ["Remove", "remove", "The article could not be removed from Read Later.", "Removed from Read Later."],
  ];
  for (const [label, action, failureCopy, successCopy] of specs) {
    const control = button(label, { className: "text-action" });
    control.addEventListener("click", () => runRowAction(handlers, { action, articleId: article.id }, control, failureCopy, successCopy));
    actions.append(control);
  }
  row.append(position, content, actions);
  return row;
}

export function renderReadLater(viewModel = {}, handlers = {}, target = "#app-view") {
  const mount = requireMount(target);
  const items = Array.isArray(viewModel.items) ? viewModel.items : [];
  const layout = element("section", { className: "view-layout", attributes: { "aria-labelledby": "view-title" } });
  layout.append(editorialHeader({
    eyebrow: "Your deliberate queue",
    title: "Read Later",
    description: "Articles you already decided are worth your time, kept in the order supplied by your local reading state.",
    actionLabel: "Discover something new",
    onAction: () => emitSemanticAction(handlers, { action: "navigate", destination: "discover" }, layout),
  }));

  const aggregate = numericValue(viewModel.aggregateReadingTimeMinutes);
  const nextTopic = typeof viewModel.nextTopic === "string" ? viewModel.nextTopic : viewModel.nextTopic?.label;
  const overview = overviewBand([
    { label: "In queue", value: countValue(viewModel.readLaterCount) },
    { label: "Known reading time", value: aggregate !== null && aggregate > 0 ? `~${Math.trunc(aggregate)} min` : "Unavailable" },
    { label: "Next topic", value: nextTopic || "Unavailable" },
  ]);
  if (overview && items.length) layout.append(overview);

  if (!items.length) {
    layout.append(emptyQueue(handlers));
  } else {
    const list = element("ol", { className: "reading-list", attributes: { "aria-label": "Read Later articles" } });
    items.forEach((entry, index) => list.append(queueRow(entry, index, viewModel, handlers)));
    layout.append(list);
  }
  replaceChildren(mount, layout);
  return mount;
}
