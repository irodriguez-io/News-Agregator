class TestEvent {
  constructor(type, options = {}) {
    this.type = type;
    this.bubbles = Boolean(options.bubbles);
    this.detail = options.detail;
    this.defaultPrevented = false;
    this.target = null;
    this.currentTarget = null;
  }

  preventDefault() {
    this.defaultPrevented = true;
  }
}

class TestNode {
  constructor() {
    this.parentNode = null;
    this.childNodes = [];
    this.ownText = "";
  }

  get textContent() {
    if (this.childNodes.length) return this.childNodes.map((child) => child.textContent).join("");
    return this.ownText;
  }

  set textContent(value) {
    this.childNodes = [];
    this.ownText = String(value ?? "");
  }

  get children() {
    return this.childNodes.filter((child) => child instanceof TestElement);
  }

  get childElementCount() {
    return this.children.length;
  }

  append(...children) {
    for (const child of children) {
      if (child === null || child === undefined) continue;
      const node = child instanceof TestNode ? child : new TestTextNode(String(child));
      node.parentNode = this;
      this.childNodes.push(node);
    }
  }

  replaceChildren(...children) {
    this.childNodes.forEach((child) => { child.parentNode = null; });
    this.childNodes = [];
    this.ownText = "";
    this.append(...children);
  }
}

class TestTextNode extends TestNode {
  constructor(text) {
    super();
    this.ownText = text;
  }
}

class TestElement extends TestNode {
  constructor(tagName) {
    super();
    this.tagName = String(tagName).toUpperCase();
    this.className = "";
    this.dataset = {};
    this.disabled = false;
    this.attributes = new Map();
    this.listeners = new Map();
  }

  setAttribute(name, value) {
    const normalized = String(value);
    this.attributes.set(name, normalized);
    if (name === "class") this.className = normalized;
    if (name === "disabled") this.disabled = true;
  }

  getAttribute(name) {
    return this.attributes.has(name) ? this.attributes.get(name) : null;
  }

  hasAttribute(name) {
    return this.attributes.has(name);
  }

  removeAttribute(name) {
    this.attributes.delete(name);
    if (name === "disabled") this.disabled = false;
  }

  addEventListener(type, listener) {
    const listeners = this.listeners.get(type) ?? [];
    listeners.push(listener);
    this.listeners.set(type, listeners);
  }

  removeEventListener(type, listener) {
    const listeners = this.listeners.get(type) ?? [];
    this.listeners.set(type, listeners.filter((candidate) => candidate !== listener));
  }

  dispatchEvent(event) {
    if (!event.target) event.target = this;
    event.currentTarget = this;
    for (const listener of this.listeners.get(event.type) ?? []) listener.call(this, event);
    if (event.bubbles && this.parentNode instanceof TestElement) this.parentNode.dispatchEvent(event);
    return !event.defaultPrevented;
  }

  click() {
    this.dispatchEvent(new TestEvent("click", { bubbles: true }));
  }

  closest(selector) {
    let candidate = this;
    while (candidate) {
      if (matches(candidate, selector)) return candidate;
      candidate = candidate.parentNode instanceof TestElement ? candidate.parentNode : null;
    }
    return null;
  }

  querySelector(selector) {
    return findAll(this, (node) => matches(node, selector))[0] ?? null;
  }

  hasPointerCapture() {
    return false;
  }

  releasePointerCapture() {}

  setPointerCapture() {}

  animate() {
    return { cancel() {} };
  }
}

class TestDocument extends TestElement {
  constructor() {
    super("document");
    this.activeElement = null;
  }

  createElement(tagName) {
    return new TestElement(tagName);
  }

  createTextNode(text) {
    return new TestTextNode(text);
  }

  getElementById(id) {
    return findAll(this, (node) => node.getAttribute("id") === id)[0] ?? null;
  }
}

function classNames(node) {
  return String(node.className || "").split(/\s+/).filter(Boolean);
}

function matches(node, selector) {
  if (!(node instanceof TestElement)) return false;
  if (selector.includes(",")) return selector.split(",").some((part) => matches(node, part.trim()));
  if (selector.startsWith("#")) return node.getAttribute("id") === selector.slice(1);
  if (selector.startsWith(".")) return classNames(node).includes(selector.slice(1));
  const attributeMatch = selector.match(/^([a-z]+)?\[([^=\]]+)(?:=['\"]?([^'\"]+)['\"]?)?\]$/i);
  if (attributeMatch) {
    const [, tagName, name, value] = attributeMatch;
    return (!tagName || node.tagName === tagName.toUpperCase())
      && node.hasAttribute(name)
      && (value === undefined || node.getAttribute(name) === value);
  }
  return node.tagName === selector.toUpperCase();
}

export function findAll(root, predicate) {
  const matchesFound = [];
  for (const child of root.childNodes ?? []) {
    if (child instanceof TestElement && predicate(child)) matchesFound.push(child);
    matchesFound.push(...findAll(child, predicate));
  }
  return matchesFound;
}

export function findByClass(root, className) {
  return findAll(root, (node) => classNames(node).includes(className))[0] ?? null;
}

export function findButton(root, ariaLabel) {
  return findAll(root, (node) => (
    node.tagName === "BUTTON" && node.getAttribute("aria-label") === ariaLabel
  ))[0] ?? null;
}

export function installDomFixture() {
  const prior = {
    CustomEvent: globalThis.CustomEvent,
    Element: globalThis.Element,
    HTMLElement: globalThis.HTMLElement,
    Node: globalThis.Node,
    document: globalThis.document,
    window: globalThis.window,
  };
  const document = new TestDocument();
  const appView = document.createElement("main");
  appView.setAttribute("id", "app-view");
  const status = document.createElement("div");
  status.setAttribute("id", "app-status");
  const toast = document.createElement("div");
  toast.setAttribute("id", "toast-region");
  document.append(appView, status, toast);

  globalThis.CustomEvent = TestEvent;
  globalThis.Element = TestElement;
  globalThis.HTMLElement = TestElement;
  globalThis.Node = TestNode;
  globalThis.document = document;
  globalThis.window = {
    innerWidth: 1024,
    matchMedia: () => ({ matches: true }),
    requestAnimationFrame: (callback) => { callback(); return 1; },
    setTimeout,
    clearTimeout,
  };

  return {
    appView,
    document,
    status,
    toast,
    restore() {
      Object.assign(globalThis, prior);
    },
  };
}
