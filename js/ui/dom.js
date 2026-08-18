export function element(tagName, options = {}, children = []) {
  const node = document.createElement(tagName);

  if (options.className) node.className = options.className;
  if (options.text !== undefined && options.text !== null) {
    node.textContent = String(options.text);
  }

  if (options.attributes) {
    for (const [name, value] of Object.entries(options.attributes)) {
      if (value !== undefined && value !== null && value !== false) {
        node.setAttribute(name, value === true ? "" : String(value));
      }
    }
  }

  if (options.dataset) {
    for (const [name, value] of Object.entries(options.dataset)) {
      if (value !== undefined && value !== null) node.dataset[name] = String(value);
    }
  }

  const childList = Array.isArray(children) ? children : [children];
  for (const child of childList) {
    if (child instanceof Node) node.append(child);
    else if (child !== undefined && child !== null) node.append(document.createTextNode(String(child)));
  }

  return node;
}

export function button(label, options = {}) {
  const node = element("button", {
    className: options.className,
    attributes: {
      type: "button",
      "aria-label": options.ariaLabel,
      "aria-pressed": options.ariaPressed,
      disabled: options.disabled,
      ...options.attributes,
    },
    dataset: options.dataset,
  });
  if (options.text !== false) node.textContent = label;
  if (typeof options.onClick === "function") node.addEventListener("click", options.onClick);
  return node;
}

export function requireMount(target, fallbackId = "app-view") {
  if (target instanceof Element) return target;
  const mount = document.querySelector(target || `#${fallbackId}`);
  if (!mount) throw new Error(`Required UI mount not found: ${target || `#${fallbackId}`}`);
  return mount;
}

export function replaceChildren(target, ...children) {
  target.replaceChildren(...children.filter((child) => child !== undefined && child !== null));
  return target;
}

export function emitSemanticAction(handlers = {}, detail, source) {
  const eventTarget = source || document;
  eventTarget.dispatchEvent(new CustomEvent("intentional-reading:action", {
    bubbles: true,
    detail: { ...detail },
  }));

  if (typeof handlers.onAction === "function") return handlers.onAction({ ...detail });
  return undefined;
}

export function actionFailed(result) {
  return result === false || (result && typeof result === "object" && result.ok === false);
}

export function actionMessage(result, fallback) {
  if (result && typeof result === "object" && typeof result.message === "string" && result.message.trim()) {
    return result.message;
  }
  return fallback;
}

export function articleFromEntry(entry) {
  return entry && typeof entry === "object" && entry.article ? entry.article : entry;
}

export function numericValue(value) {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}
