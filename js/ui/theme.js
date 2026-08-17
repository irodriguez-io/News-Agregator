const ALLOWED_APPEARANCES = new Set(["light", "dark", "system"]);
const systemQuery = window.matchMedia("(prefers-color-scheme: dark)");
let activeAppearance = "system";
let listenerAttached = false;

function resolvedTheme(appearance) {
  return appearance === "system" ? (systemQuery.matches ? "dark" : "light") : appearance;
}

function applyResolvedTheme() {
  const root = document.documentElement;
  root.dataset.appearance = activeAppearance;
  root.dataset.theme = resolvedTheme(activeAppearance);
}

function onSystemThemeChange() {
  if (activeAppearance === "system") applyResolvedTheme();
}

export function applyAppearance(appearance = "system") {
  activeAppearance = ALLOWED_APPEARANCES.has(appearance) ? appearance : "system";
  applyResolvedTheme();
  if (!listenerAttached) {
    systemQuery.addEventListener("change", onSystemThemeChange);
    listenerAttached = true;
  }
  return activeAppearance;
}

export function currentAppearance() {
  return activeAppearance;
}
