import {
  actionFailed,
  actionMessage,
  button,
  element,
  emitSemanticAction,
  requireMount,
  replaceChildren,
} from "./dom.js";
import { announceStatus } from "./toast.js";
import { applyAppearance } from "./theme.js";

const MAX_IMPORT_BYTES = 5 * 1024 * 1024;
const APPEARANCES = Object.freeze([
  ["light", "Light"],
  ["dark", "Dark"],
  ["system", "System"],
]);

let settingsAbort = null;
let returnFocus = null;

async function runSettingsAction(handlers, detail, source, fallback) {
  let result;
  try {
    result = await emitSemanticAction(handlers, detail, source);
  } catch (error) {
    result = { ok: false, message: error instanceof Error ? error.message : fallback };
  }
  if (actionFailed(result)) announceStatus(actionMessage(result, fallback));
  return result;
}

function focusableElements(dialog) {
  return [...dialog.querySelectorAll(
    "button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), summary, [href], [tabindex]:not([tabindex='-1'])",
  )].filter((node) => !node.hidden && node.getClientRects().length > 0);
}

function trapTabKey(dialog, event) {
  if (event.key !== "Tab") return;
  const focusable = focusableElements(dialog);
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

function appearanceSection(viewModel, handlers) {
  const section = element("section", { className: "settings-section", attributes: { "aria-labelledby": "appearance-title" } });
  section.append(
    element("h3", { text: "Appearance", attributes: { id: "appearance-title" } }),
    element("p", { className: "settings-copy", text: "System follows your device or browser color preference." }),
  );
  const options = element("div", { className: "appearance-options", attributes: { role: "radiogroup", "aria-label": "Appearance" } });
  const selected = APPEARANCES.some(([value]) => value === viewModel.appearance) ? viewModel.appearance : "system";
  applyAppearance(selected);

  for (const [value, label] of APPEARANCES) {
    const input = element("input", {
      attributes: {
        type: "radio",
        name: "appearance",
        value,
        checked: value === selected,
      },
    });
    const option = element("label", { className: "appearance-option" }, [input, element("span", { text: label })]);
    input.addEventListener("change", async () => {
      if (!input.checked) return;
      const result = await runSettingsAction(
        handlers,
        { action: "appearance_change", appearance: value },
        input,
        "Appearance could not be changed.",
      );
      if (actionFailed(result)) {
        const prior = options.querySelector(`input[value="${selected}"]`);
        if (prior) prior.checked = true;
        applyAppearance(selected);
      } else {
        applyAppearance(value);
        announceStatus(`${label} appearance selected.`);
      }
    });
    options.append(option);
  }
  section.append(options);
  return section;
}

function importSection(handlers) {
  const section = element("section", { className: "settings-section", attributes: { "aria-labelledby": "import-title" } });
  const inputId = "settings-import-file";
  const input = element("input", {
    className: "file-input",
    attributes: { id: inputId, type: "file", accept: "application/json,.json" },
  });
  const confirmationHost = element("div");
  section.append(
    element("h3", { text: "Import local data", attributes: { id: "import-title" } }),
    element("p", {
      className: "settings-copy",
      text: "Choose a V1 JSON backup no larger than 5 MiB. A valid import replaces current local data rather than merging it.",
    }),
    element("label", { className: "visually-hidden", text: "Choose a local data backup", attributes: { for: inputId } }),
    input,
    confirmationHost,
  );

  input.addEventListener("change", () => {
    confirmationHost.replaceChildren();
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > MAX_IMPORT_BYTES) {
      input.value = "";
      announceStatus("Import rejected. The selected file is larger than 5 MiB.");
      confirmationHost.append(element("p", {
        className: "inline-notice",
        text: "This file is larger than the 5 MiB V1 import limit and was not read.",
      }));
      return;
    }

    const panel = element("div", { className: "confirmation-panel" });
    panel.append(element("p", {
      text: `Replace current preferences, Read Later, History, dismissals, and local settings with “${file.name}” after validation?`,
    }));
    const actions = element("div", { className: "confirmation-actions" });
    const cancel = button("Cancel", { className: "button button-quiet" });
    const confirm = button("Replace local data", { className: "button button-destructive" });
    cancel.addEventListener("click", () => {
      input.value = "";
      confirmationHost.replaceChildren();
      input.focus();
    });
    confirm.addEventListener("click", async () => {
      confirm.disabled = true;
      cancel.disabled = true;
      let serialized;
      try {
        serialized = await file.text();
      } catch {
        announceStatus("The selected backup could not be read. Current local data was not changed.");
        confirm.disabled = false;
        cancel.disabled = false;
        return;
      }
      const result = await runSettingsAction(
        handlers,
        { action: "import_data", serialized, fileName: file.name },
        confirm,
        "Import was not completed. Current local data was not changed.",
      );
      if (actionFailed(result)) {
        confirm.disabled = false;
        cancel.disabled = false;
      } else announceStatus("Local data imported.");
    });
    actions.append(cancel, confirm);
    panel.append(actions);
    replaceChildren(confirmationHost, panel);
    cancel.focus();
  });
  return section;
}

function resetSection(handlers) {
  const section = element("section", { className: "settings-section", attributes: { "aria-labelledby": "reset-title" } });
  const confirmationHost = element("div");
  const reveal = button("Reset all data", { className: "button button-destructive" });
  section.append(
    element("h3", { text: "Reset local data", attributes: { id: "reset-title" } }),
    element("p", { className: "settings-copy", text: "Clear the personal reading state stored on this device." }),
    reveal,
    confirmationHost,
  );

  reveal.addEventListener("click", () => {
    reveal.hidden = true;
    const panel = element("div", { className: "confirmation-panel" });
    panel.append(element("p", {
      text: "Reset clears preferences, Read Later, History, dismissals, and local settings on this device. This cannot be undone.",
    }));
    const actions = element("div", { className: "confirmation-actions" });
    const cancel = button("Cancel", { className: "button button-quiet" });
    const confirm = button("Reset all data", { className: "button button-destructive" });
    cancel.addEventListener("click", () => {
      confirmationHost.replaceChildren();
      reveal.hidden = false;
      reveal.focus();
    });
    confirm.addEventListener("click", async () => {
      confirm.disabled = true;
      cancel.disabled = true;
      const result = await runSettingsAction(
        handlers,
        { action: "reset_data" },
        confirm,
        "Local data could not be reset.",
      );
      if (actionFailed(result)) {
        confirm.disabled = false;
        cancel.disabled = false;
      } else announceStatus("Local data reset.");
    });
    actions.append(cancel, confirm);
    panel.append(actions);
    replaceChildren(confirmationHost, panel);
    cancel.focus();
  });
  return section;
}

export function renderSettings(viewModel = {}, handlers = {}, target = "#settings-dialog") {
  if (settingsAbort) settingsAbort.abort();
  settingsAbort = new AbortController();
  const { signal } = settingsAbort;
  const dialog = requireMount(target, "settings-dialog");
  if (!(dialog instanceof HTMLDialogElement)) throw new TypeError("Settings mount must be a dialog element.");

  const content = element("div", { className: "settings-dialog-content" });
  const header = element("header", { className: "dialog-header" });
  const close = button("Close Settings", { className: "icon-button", ariaLabel: "Close Settings", text: false });
  close.append(element("span", { text: "×", attributes: { "aria-hidden": "true" } }));
  header.append(
    element("div", {}, [
      element("p", { className: "eyebrow", text: "Local preferences" }),
      element("h2", { className: "dialog-title", text: "Settings", attributes: { id: "settings-title" } }),
    ]),
    close,
  );
  content.append(header, appearanceSection(viewModel, handlers));

  const dataSection = element("section", { className: "settings-section", attributes: { "aria-labelledby": "data-title" } });
  dataSection.append(
    element("h3", { text: "Local data", attributes: { id: "data-title" } }),
    element("p", { className: "settings-copy", text: "Back up or restore the reading state stored only on this device." }),
  );
  const exportButton = button("Export local data", { className: "button button-quiet" });
  exportButton.addEventListener("click", () => {
    runSettingsAction(
      handlers,
      { action: "export_data" },
      exportButton,
      "Local data could not be exported.",
    ).then((result) => {
      if (!actionFailed(result)) announceStatus("Local data export prepared.");
    });
  });
  dataSection.append(element("div", { className: "settings-actions" }, [exportButton]));
  content.append(dataSection, importSection(handlers), resetSection(handlers));
  replaceChildren(dialog, content);

  const toggle = document.getElementById("settings-toggle");
  const closeDialog = () => {
    if (dialog.open) dialog.close();
  };
  close.addEventListener("click", closeDialog, { signal });
  dialog.addEventListener("keydown", (event) => trapTabKey(dialog, event), { signal });
  dialog.addEventListener("close", () => {
    if (returnFocus instanceof HTMLElement) returnFocus.focus();
    returnFocus = null;
  }, { signal });
  if (toggle) {
    toggle.addEventListener("click", () => openSettings(viewModel, handlers, target), { signal });
  }
  return dialog;
}

export function openSettings(viewModel = {}, handlers = {}, target = "#settings-dialog") {
  const dialog = renderSettings(viewModel, handlers, target);
  returnFocus = document.activeElement instanceof HTMLElement
    ? document.activeElement
    : document.getElementById("settings-toggle");
  if (!dialog.open) dialog.showModal();
  const initial = dialog.querySelector("input:checked") || dialog.querySelector("button");
  if (initial instanceof HTMLElement) initial.focus();
  return dialog;
}

export function closeSettings(target = "#settings-dialog") {
  const dialog = document.querySelector(target);
  if (dialog instanceof HTMLDialogElement && dialog.open) dialog.close();
}

export { MAX_IMPORT_BYTES };
