import { button, element, requireMount, replaceChildren } from "./dom.js";

let toastTimer = null;
let toastToken = 0;

export function announceStatus(message) {
  const region = document.getElementById("app-status");
  if (!region) return;
  region.textContent = "";
  window.requestAnimationFrame(() => {
    region.textContent = String(message || "");
  });
}

export function clearToast(target = "#toast-region") {
  toastToken += 1;
  if (toastTimer !== null) window.clearTimeout(toastTimer);
  toastTimer = null;
  const region = document.querySelector(target);
  if (region) region.replaceChildren();
}

export function showToast({
  message,
  actionLabel,
  onAction,
  duration = 4500,
  target = "#toast-region",
} = {}) {
  const region = requireMount(target, "toast-region");
  clearToast(target);
  const token = ++toastToken;
  const toast = element("div", {
    className: "toast",
    attributes: { role: "status", "aria-live": "polite", "aria-atomic": "true" },
  });
  toast.append(element("span", { className: "toast-message", text: message || "Status updated." }));

  if (actionLabel && typeof onAction === "function") {
    toast.append(button(actionLabel, {
      className: "toast-action",
      onClick: async () => {
        await onAction();
        clearToast(target);
      },
    }));
  }

  replaceChildren(region, toast);
  window.requestAnimationFrame(() => {
    if (token === toastToken) toast.dataset.visible = "true";
  });
  announceStatus(actionLabel ? `${message}. ${actionLabel} available.` : message);

  toastTimer = window.setTimeout(() => {
    if (token !== toastToken) return;
    toast.dataset.visible = "false";
    window.setTimeout(() => {
      if (token === toastToken) clearToast(target);
    }, 180);
  }, Math.max(0, duration));

  return () => clearToast(target);
}

export function showUndoToast({ message, onUndo, duration = 4500 } = {}) {
  return showToast({ message, actionLabel: "Undo", onAction: onUndo, duration });
}
