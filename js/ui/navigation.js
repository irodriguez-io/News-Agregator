import { button, element, emitSemanticAction, numericValue, requireMount, replaceChildren } from "./dom.js";

const DESTINATIONS = Object.freeze([
  { id: "read_later", label: "Read Later", icon: "▱", countKey: "readLaterCount" },
  { id: "discover", label: "Discover", icon: "◉" },
  { id: "history", label: "History", icon: "▤", countKey: "historyCount" },
]);

export function renderNavigation(viewModel = {}, handlers = {}, target = "#primary-navigation") {
  const mount = requireMount(target, "primary-navigation");
  const activeDestination = DESTINATIONS.some(({ id }) => id === viewModel.activeDestination)
    ? viewModel.activeDestination
    : "discover";

  const controls = DESTINATIONS.map((destination) => {
    const control = button(destination.label, {
      className: "nav-button",
      ariaLabel: destination.label,
      attributes: { "aria-current": destination.id === activeDestination ? "page" : null },
      dataset: { destination: destination.id },
      text: false,
      onClick: () => emitSemanticAction(handlers, {
        action: "navigate",
        destination: destination.id,
      }, control),
    });
    control.append(
      element("span", { className: "nav-icon", text: destination.icon, attributes: { "aria-hidden": "true" } }),
      element("span", { className: "nav-label", text: destination.label }),
    );

    if (destination.countKey) {
      const count = numericValue(viewModel[destination.countKey]);
      if (count !== null) {
        control.append(element("span", {
          className: "nav-count",
          text: Math.max(0, Math.trunc(count)),
          attributes: { "aria-label": `${Math.max(0, Math.trunc(count))} ${destination.label}` },
        }));
      }
    }
    return control;
  });

  replaceChildren(mount, ...controls);
  return mount;
}
