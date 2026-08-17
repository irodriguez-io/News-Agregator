import { actionFailed } from "./dom.js";

export const SWIPE_THRESHOLD_PX = 90;

const INTERACTIVE_SELECTOR = [
  "a",
  "button",
  "input",
  "select",
  "textarea",
  "summary",
  "label",
  "[contenteditable='true']",
  "[role='button']",
  "[role='link']",
].join(",");

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function safeRelease(element, pointerId) {
  if (element.hasPointerCapture(pointerId)) element.releasePointerCapture(pointerId);
}

export function attachSwipe(element, {
  threshold = SWIPE_THRESHOLD_PX,
  onCommit,
  onFailure,
  onSuccess,
} = {}) {
  if (!(element instanceof HTMLElement)) throw new TypeError("Swipe requires an HTML element.");

  let pointerId = null;
  let startX = 0;
  let startY = 0;
  let deltaX = 0;
  let intent = "pending";
  let pending = false;
  let cardEffect = null;

  const setVisualPosition = (travel) => {
    const rotation = Math.max(-4.5, Math.min(4.5, travel / 34));
    if (cardEffect) cardEffect.cancel();
    cardEffect = prefersReducedMotion()
      ? null
      : element.animate(
        [{ transform: `translateX(${travel}px) rotate(${rotation}deg)` }],
        { duration: 1, fill: "forwards" },
      );
    element.dataset.swipeState = travel < 0 ? "left" : travel > 0 ? "right" : "";
  };

  const restore = () => {
    pointerId = null;
    deltaX = 0;
    intent = "pending";
    pending = false;
    element.dataset.dragging = "false";
    element.dataset.pending = "false";
    element.dataset.swipeState = "";
    if (cardEffect) cardEffect.cancel();
    cardEffect = null;
    element.removeAttribute("aria-busy");
  };

  const onPointerDown = (event) => {
    if (pending || event.button !== 0 || event.target.closest(INTERACTIVE_SELECTOR)) return;
    pointerId = event.pointerId;
    startX = event.clientX;
    startY = event.clientY;
    deltaX = 0;
    intent = "pending";
    element.setPointerCapture(pointerId);
  };

  const onPointerMove = (event) => {
    if (event.pointerId !== pointerId || pending) return;
    const x = event.clientX - startX;
    const y = event.clientY - startY;

    if (intent === "pending" && (Math.abs(x) > 8 || Math.abs(y) > 8)) {
      intent = Math.abs(x) > Math.abs(y) * 1.15 ? "horizontal" : "vertical";
    }
    if (intent !== "horizontal") return;

    event.preventDefault();
    deltaX = x;
    element.dataset.dragging = "true";
    setVisualPosition(deltaX);
  };

  const finishPointer = async (event, cancelled = false) => {
    if (event.pointerId !== pointerId || pending) return;
    safeRelease(element, pointerId);
    element.dataset.dragging = "false";

    if (cancelled || intent !== "horizontal" || Math.abs(deltaX) < threshold) {
      restore();
      return;
    }

    const action = deltaX < 0 ? "dismiss" : "save";
    const direction = deltaX < 0 ? -1 : 1;
    pending = true;
    element.dataset.pending = "true";
    element.setAttribute("aria-busy", "true");
    const exitDistance = Math.max(window.innerWidth * 0.82, 620) * direction;
    if (!prefersReducedMotion()) {
      if (cardEffect) cardEffect.cancel();
      cardEffect = element.animate(
        [
          {},
          { transform: `translateX(${exitDistance}px) rotate(${direction * 4.5}deg)`, opacity: 0 },
        ],
        { duration: 280, easing: "cubic-bezier(0.2, 0.8, 0.2, 1)", fill: "forwards" },
      );
    }

    let result;
    try {
      result = typeof onCommit === "function" ? await onCommit({ action }) : undefined;
    } catch (error) {
      result = { ok: false, message: error instanceof Error ? error.message : "The action could not be completed." };
    }

    if (actionFailed(result)) {
      restore();
      if (typeof onFailure === "function") onFailure(result, action);
      return;
    }

    if (typeof onSuccess === "function") onSuccess(result, action);
  };

  const onPointerUp = (event) => finishPointer(event, false);
  const onPointerCancel = (event) => finishPointer(event, true);

  element.addEventListener("pointerdown", onPointerDown);
  element.addEventListener("pointermove", onPointerMove);
  element.addEventListener("pointerup", onPointerUp);
  element.addEventListener("pointercancel", onPointerCancel);

  return {
    restore,
    destroy() {
      element.removeEventListener("pointerdown", onPointerDown);
      element.removeEventListener("pointermove", onPointerMove);
      element.removeEventListener("pointerup", onPointerUp);
      element.removeEventListener("pointercancel", onPointerCancel);
      restore();
    },
  };
}

function unsafeShortcutContext(event) {
  const target = event.target instanceof Element ? event.target : document.activeElement;
  if (document.querySelector("dialog[open]")) return true;
  if (!(target instanceof Element)) return false;
  return Boolean(target.closest(INTERACTIVE_SELECTOR) || target.closest("[role='dialog'], .category-selector"));
}

export function installDiscoverShortcuts({ onDismiss, onSave, onUndo, canUndo = false } = {}) {
  const listener = (event) => {
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) return;
    if (unsafeShortcutContext(event)) return;

    if (event.key === "ArrowLeft" && typeof onDismiss === "function") {
      event.preventDefault();
      onDismiss();
    } else if (event.key === "ArrowRight" && typeof onSave === "function") {
      event.preventDefault();
      onSave();
    } else if (
      event.key.toLowerCase() === "z"
      && (typeof canUndo === "function" ? canUndo() : canUndo)
      && typeof onUndo === "function"
    ) {
      event.preventDefault();
      onUndo();
    }
  };
  document.addEventListener("keydown", listener);
  return () => document.removeEventListener("keydown", listener);
}
