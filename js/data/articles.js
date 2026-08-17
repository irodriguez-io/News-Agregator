import { ValidationError, validateArticleDataset } from "./validation.js";

export class DatasetError extends Error {
  constructor(code, message, options = {}) {
    super(message, options);
    this.name = "DatasetError";
    this.code = code;
  }
}

export async function loadArticleDataset(
  url = "./data/articles.json",
  { fetchImpl = globalThis.fetch } = {},
) {
  if (typeof fetchImpl !== "function") {
    throw new DatasetError("FETCH_FAILED", "The article dataset could not be loaded");
  }

  let response;
  try {
    response = await fetchImpl(url);
  } catch {
    throw new DatasetError("FETCH_FAILED", "The article dataset could not be loaded");
  }
  if (!response || response.ok !== true) {
    throw new DatasetError("FETCH_FAILED", "The article dataset could not be loaded");
  }

  let candidate;
  try {
    candidate = await response.json();
  } catch {
    throw new DatasetError("INVALID_JSON", "The article dataset is not valid JSON");
  }

  try {
    return validateArticleDataset(candidate);
  } catch (error) {
    if (error instanceof ValidationError && error.code === "UNSUPPORTED_SCHEMA") {
      throw new DatasetError("UNSUPPORTED_SCHEMA", "The article dataset schema is unsupported");
    }
    throw new DatasetError("INVALID_DATASET", "The article dataset is structurally invalid");
  }
}
