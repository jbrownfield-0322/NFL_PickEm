// Shared season helpers for Games / Leaderboard / League Details

export async function fetchSeasonOptions(apiBase) {
  const [seasonsRes, currentRes] = await Promise.all([
    fetch(`${apiBase}/games/seasons`),
    fetch(`${apiBase}/games/currentSeason`),
  ]);

  let seasons = [];
  let currentSeason = new Date().getFullYear();

  if (seasonsRes.ok) {
    seasons = await seasonsRes.json();
  }
  if (currentRes.ok) {
    const text = await currentRes.text();
    currentSeason = parseInt(text, 10);
  }

  if (!seasons.includes(currentSeason)) {
    seasons = [currentSeason, ...seasons];
  }

  // Newest first, unique
  seasons = [...new Set(seasons)].sort((a, b) => b - a);
  return { seasons, currentSeason };
}

export function seasonQuery(seasonYear) {
  return seasonYear ? `seasonYear=${seasonYear}` : '';
}

export function appendSeasonParam(url, seasonYear) {
  if (!seasonYear) return url;
  const sep = url.includes('?') ? '&' : '?';
  return `${url}${sep}seasonYear=${seasonYear}`;
}
