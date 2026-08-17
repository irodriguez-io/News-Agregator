import {
  actionFailed,
  actionMessage,
  button,
  element,
  emitSemanticAction,
  numericValue,
  requireMount,
  replaceChildren,
} from "./dom.js";
import { CATEGORY_OPTIONS, categoryLabel, readingTime, relativeDate } from "./format.js";
import { attachSwipe, installDiscoverShortcuts } from "./swipe.js";
import { announceStatus, showUndoToast } from "./toast.js";

let destroySwipe = null;
let destroyShortcuts = null;

function cleanupInteractions() {
  if (destroySwipe) destroySwipe();
  if (destroyShortcuts) destroyShortcuts();
  destroySwipe = null;
  destroyShortcuts = null;
}

function viewHeader(viewModel, handlers) {
  const header = element("header", { className: "view-header" });
  header.append(
    element("p", { className: "eyebrow", text: "A finite reading queue" }),
    element("h1", { className: "view-title", text: "Discover", attributes: { id: "view-title" } }),
    element("p", {
      className: "view-description",
      text: "Choose what deserves your attention. Save what matters, dismiss what does not, and leave when you are done.",
    }),
  );

  const remaining = numericValue(viewModel.remainingCount);
  if (remaining !== null) {
    const selectedLabel = categoryLabel(viewModel.category || "all") || "All";
    header.append(element("p", {
      className: "view-context",
      text: `${Math.max(0, Math.trunc(remaining))} available in ${selectedLabel}`,
    }));
  }

  const scroller = element("div", { className: "category-scroll" });
  const selector = element("div", {
    className: "category-selector",
    attributes: { role: "group", "aria-label": "Discover category" },
  });
  const selected = viewModel.category || "all";
  for (const [id, label] of CATEGORY_OPTIONS) {
    const control = button(label, {
      className: "category-button",
      ariaPressed: id === selected ? "true" : "false",
      onClick: () => emitSemanticAction(handlers, { action: "category_change", category: id }, control),
    });
    selector.append(control);
  }
  scroller.append(selector);
  header.append(scroller);
  return header;
}

function statePanel({ title, copy, actionLabel, onAction, loading = false }) {
  const panel = element("section", { className: "state-panel", attributes: { "aria-labelledby": "discover-state-title" } });
  if (loading) {
    panel.setAttribute("aria-busy", "true");
    panel.append(element("p", { className: "loading-indicator", text: copy }));
    return panel;
  }
  panel.append(
    element("h2", { text: title, attributes: { id: "discover-state-title" } }),
    element("p", { text: copy }),
  );
  if (actionLabel && onAction) panel.append(button(actionLabel, { className: "button button-quiet", onClick: onAction }));
  return panel;
}

function contentTypeBadge(article) {
  const label = article?.contentType?.label;
  return label ? element("span", { className: "badge", text: label }) : null;
}

function topicList(article, limit = 5) {
  const tags = Array.isArray(article?.tags) ? article.tags.slice(0, limit) : [];
  if (!tags.length) return null;
  const list = element("ul", { className: "topic-tags", attributes: { "aria-label": "Article topics" } });
  for (const tag of tags) {
    if (tag && tag.label) list.append(element("li", { className: "topic-tag", text: tag.label }));
  }
  return list.childElementCount ? list : null;
}

function debugDetails(debug) {
  if (!debug || typeof debug !== "object") return null;
  const fields = [
    ["base", "Base score"],
    ["sourcePreference", "Source preference"],
    ["topicPreference", "Topic preference"],
    ["exploration", "Exploration boost"],
    ["total", "Final score"],
    ["detectedTagCount", "Detected tag count"],
  ].filter(([key]) => numericValue(debug[key]) !== null);
  if (!fields.length) return null;

  const details = element("details", { className: "debug-details" });
  details.append(element("summary", { text: "Ranking details" }));
  const grid = element("dl", { className: "debug-grid" });
  for (const [key, label] of fields) {
    grid.append(
      element("dt", { text: label }),
      element("dd", { text: debug[key] }),
    );
  }
  details.append(grid);
  return details;
}

async function actionResult(handlers, detail, source) {
  try {
    return await emitSemanticAction(handlers, detail, source);
  } catch (error) {
    return { ok: false, message: error instanceof Error ? error.message : "The action could not be completed." };
  }
}

function articleCard(viewModel, handlers) {
  const article = viewModel.article;
  const card = element("article", {
    className: "article-card",
    attributes: { "aria-labelledby": "discover-article-title" },
  });
  const content = element("div", { className: "article-card-content" });
  const meta = element("div", { className: "card-meta" });
  if (article?.source?.name) meta.append(element("span", { className: "source-name", text: article.source.name }));

  const age = relativeDate(article?.publishedAt, viewModel.now);
  if (age) meta.append(element("span", { className: "meta-line meta-separator", text: age }));
  const badge = contentTypeBadge(article);
  if (badge) meta.append(badge);
  const category = categoryLabel(article?.category);
  if (category) meta.append(element("span", { className: "meta-line", text: category }));
  const duration = readingTime(article?.readingTimeMinutes);
  if (duration) meta.append(element("span", { className: "meta-line", text: duration }));

  content.append(meta);
  content.append(element("h2", {
    className: "card-title",
    text: article?.title || "",
    attributes: { id: "discover-article-title" },
  }));
  if (article?.excerpt) content.append(element("p", { className: "card-excerpt", text: article.excerpt }));
  const tags = topicList(article);
  if (tags) content.append(tags);

  const actions = element("div", { className: "card-actions", attributes: { "aria-label": "Article actions" } });
  const dismissButton = button("Not interested", {
    className: "triage-button",
    ariaLabel: "Not interested",
    text: false,
  });
  dismissButton.append(
    element("span", { className: "triage-icon", text: "←", attributes: { "aria-hidden": "true" } }),
    element("span", { className: "triage-label", text: "Not interested" }),
  );
  const openButton = button("Read article", {
    className: "button button-primary",
    ariaLabel: "Read article in a new tab",
    text: false,
  });
  openButton.append(
    element("span", { text: "Read article" }),
    element("span", { text: "↗", attributes: { "aria-hidden": "true" } }),
  );
  const saveButton = button("Save for later", {
    className: "triage-button",
    ariaLabel: "Save for later",
    text: false,
  });
  saveButton.append(
    element("span", { className: "triage-icon", text: "→", attributes: { "aria-hidden": "true" } }),
    element("span", { className: "triage-label", text: "Save for later" }),
  );
  actions.append(dismissButton, openButton, saveButton);

  const perform = async (action, { undoEligible = false, source = card } = {}) => {
    const controls = [dismissButton, openButton, saveButton];
    controls.forEach((control) => { control.disabled = true; });
    const result = await actionResult(handlers, {
      action,
      articleId: article.id,
      undoable: undoEligible,
    }, source);
    if (actionFailed(result)) {
      controls.forEach((control) => { control.disabled = false; });
      announceStatus(actionMessage(result, "Your change could not be saved. The article has been restored."));
      return result;
    }

    if (undoEligible && (action === "save" || action === "dismiss")) {
      showUndoToast({
        message: action === "save" ? "Saved to Read Later" : "Not interested",
        onUndo: async () => {
          const undoResult = await actionResult(handlers, { action: "undo", articleId: article.id }, card);
          if (actionFailed(undoResult)) announceStatus(actionMessage(undoResult, "Undo could not be completed."));
        },
      });
    } else {
      const message = {
        dismiss: "Marked not interested.",
        save: "Saved to Read Later.",
        open: "Opening the publisher in a new tab.",
      }[action];
      if (message) announceStatus(message);
    }
    if (action === "open") controls.forEach((control) => { control.disabled = false; });
    return result;
  };

  dismissButton.addEventListener("click", () => perform("dismiss", { source: dismissButton }));
  openButton.addEventListener("click", () => perform("open", { source: openButton }));
  saveButton.addEventListener("click", () => perform("save", { source: saveButton }));

  content.append(actions);
  const debug = debugDetails(viewModel.debug);
  if (debug) content.append(debug);
  card.append(
    element("span", { className: "swipe-cue swipe-cue-left", text: "← Not interested", attributes: { "aria-hidden": "true" } }),
    element("span", { className: "swipe-cue swipe-cue-right", text: "Save for later →", attributes: { "aria-hidden": "true" } }),
    content,
  );

  const swipeController = attachSwipe(card, {
    onCommit: ({ action }) => perform(action, { undoEligible: true }),
  });
  destroySwipe = () => swipeController.destroy();

  destroyShortcuts = installDiscoverShortcuts({
    onDismiss: () => perform("dismiss", { undoEligible: true }),
    onSave: () => perform("save", { undoEligible: true }),
    onUndo: () => actionResult(handlers, { action: "undo", articleId: article.id }, card),
    canUndo: Boolean(viewModel.undoAvailable),
  });
  return card;
}

export function renderDiscover(viewModel = {}, handlers = {}, target = "#app-view") {
  cleanupInteractions();
  const mount = requireMount(target);
  const layout = element("section", { className: "view-layout", attributes: { "aria-labelledby": "view-title" } });
  layout.append(viewHeader(viewModel, handlers));

  if (viewModel.loading || viewModel.state === "loading") {
    layout.append(statePanel({ loading: true, copy: "Gathering a thoughtful queue…" }));
    replaceChildren(mount, layout);
    announceStatus("Gathering a thoughtful queue.");
    return mount;
  }

  if (viewModel.error || viewModel.state === "error") {
    layout.append(statePanel({
      title: "Discover is unavailable right now",
      copy: "Your saved reading and History remain on this device. Try loading the current reading queue again when you are ready.",
      actionLabel: typeof handlers.onRetry === "function" ? "Try again" : null,
      onAction: typeof handlers.onRetry === "function" ? handlers.onRetry : null,
    }));
    replaceChildren(mount, layout);
    announceStatus("Discover is unavailable. Read Later and History remain available.");
    return mount;
  }

  if (!viewModel.article) {
    layout.append(statePanel({
      title: "Nothing needs your attention right now",
      copy: "You are caught up for this category. Leave without missing anything, or return to your saved reading.",
      actionLabel: "View Read Later",
      onAction: () => emitSemanticAction(handlers, { action: "navigate", destination: "read_later" }, mount),
    }));
    replaceChildren(mount, layout);
    return mount;
  }

  const content = element("div", { className: "discover-content" });
  if (viewModel.degraded) {
    content.append(element("p", {
      className: "degraded-notice",
      text: "Some sources were unavailable during the latest refresh.",
    }));
  }
  const stage = element("div", {
    className: "card-stage",
    dataset: { hasNext: numericValue(viewModel.remainingCount) === null || viewModel.remainingCount > 1 },
  });
  stage.append(articleCard(viewModel, handlers));
  content.append(stage);

  const remaining = numericValue(viewModel.remainingCount);
  if (remaining !== null && remaining > 1) {
    content.append(element("p", {
      className: "discover-side-note",
      text: `${Math.trunc(remaining - 1)} more ${remaining - 1 === 1 ? "choice" : "choices"} wait quietly behind this one.`,
    }));
  }
  layout.append(content);
  replaceChildren(mount, layout);
  return mount;
}

export function destroyDiscover() {
  cleanupInteractions();
}
