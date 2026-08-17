import { CATEGORY_FILTER_IDS } from "../data/validation.js";
import { isDiscoverEligible } from "../state/selectors.js";
import { personalizeArticle } from "./personalize.js";

function compareAscending(left, right) {
  if (left < right) return -1;
  if (left > right) return 1;
  return 0;
}

function publicationValue(article) {
  return article.publishedAt === null ? Number.NEGATIVE_INFINITY : Date.parse(article.publishedAt);
}

export function compareCandidates(left, right) {
  if (left.score.total !== right.score.total) return right.score.total - left.score.total;
  if (left.score.base !== right.score.base) return right.score.base - left.score.base;
  const leftPublication = publicationValue(left.article);
  const rightPublication = publicationValue(right.article);
  if (leftPublication !== rightPublication) return rightPublication - leftPublication;
  const sourceDifference = compareAscending(left.article.source.id, right.article.source.id);
  if (sourceDifference !== 0) return sourceDifference;
  return compareAscending(left.article.id, right.article.id);
}

function penaltiesFor(candidate, selected, category) {
  const previous = selected.at(-1);
  const sameSourcePenalty = previous?.article.source.id === candidate.article.source.id ? -8 : 0;
  let categoryPenalty = 0;
  if (category === "all" && selected.length >= 2) {
    const penultimate = selected.at(-2);
    if (previous.article.category === candidate.article.category
      && penultimate.article.category === candidate.article.category) {
      categoryPenalty = -5;
    }
  }
  return { sameSourcePenalty, categoryPenalty };
}

function compareSequenced(left, right) {
  if (left.sequencing.score !== right.sequencing.score) {
    return right.sequencing.score - left.sequencing.score;
  }
  return compareCandidates(left, right);
}

export function buildDeck({ articles, state, category }) {
  if (!CATEGORY_FILTER_IDS.includes(category)) throw new TypeError(`Unsupported category filter: ${category}`);

  const remaining = articles
    .filter((article) => isDiscoverEligible(state.articles[article.id]))
    .filter((article) => category === "all" || article.category === category)
    .map((article) => ({ article, score: personalizeArticle(article, state.preferences) }))
    .toSorted(compareCandidates);

  const selected = [];
  while (remaining.length > 0) {
    const evaluated = remaining.map((candidate) => {
      const { sameSourcePenalty, categoryPenalty } = penaltiesFor(candidate, selected, category);
      return {
        ...candidate,
        sequencing: {
          score: candidate.score.total + sameSourcePenalty + categoryPenalty,
          sameSourcePenalty,
          categoryPenalty,
        },
      };
    }).toSorted(compareSequenced);
    const winner = evaluated[0];
    selected.push(winner);
    const winnerIndex = remaining.findIndex((candidate) => candidate.article.id === winner.article.id);
    remaining.splice(winnerIndex, 1);
  }
  return selected;
}
