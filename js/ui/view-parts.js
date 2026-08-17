import { actionFailed, actionMessage, button, element, emitSemanticAction, numericValue } from "./dom.js";
import { announceStatus } from "./toast.js";

export function editorialHeader({ eyebrow, title, description, id = "view-title", actionLabel, onAction }) {
  const header = element("header", { className: "view-header" });
  header.append(
    element("p", { className: "eyebrow", text: eyebrow }),
    element("h1", { className: "view-title", text: title, attributes: { id } }),
    element("p", { className: "view-description", text: description }),
  );
  if (actionLabel && onAction) header.append(button(actionLabel, { className: "button button-quiet", onClick: onAction }));
  return header;
}

export function overviewBand(items) {
  const available = items.filter((item) => item && item.value !== undefined && item.value !== null && item.value !== "");
  if (!available.length) return null;
  const band = element("section", { className: "overview-band", attributes: { "aria-label": "Overview" } });
  for (const item of available) {
    band.append(element("div", { className: "overview-item" }, [
      element("span", { className: "overview-label", text: item.label }),
      element("span", { className: "overview-value", text: item.value }),
    ]));
  }
  return band;
}

export function countValue(value) {
  const numeric = numericValue(value);
  return numeric === null ? null : String(Math.max(0, Math.trunc(numeric)));
}

export async function runRowAction(handlers, detail, source, failureCopy, successCopy) {
  let result;
  try {
    result = await emitSemanticAction(handlers, detail, source);
  } catch (error) {
    result = { ok: false, message: error instanceof Error ? error.message : failureCopy };
  }
  if (actionFailed(result)) announceStatus(actionMessage(result, failureCopy));
  else if (successCopy) announceStatus(successCopy);
  return result;
}
