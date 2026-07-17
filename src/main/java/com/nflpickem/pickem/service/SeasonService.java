package com.nflpickem.pickem.service;

import com.nflpickem.pickem.repository.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves NFL season years (the calendar year the regular season starts, e.g. 2025 for Sep 2025–Feb 2026).
 */
@Service
public class SeasonService {

    private final GameRepository gameRepository;

    @Value("${NFL_SEASON_START_DATE:}")
    private String configuredSeasonStartDate;

    public SeasonService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Current / upcoming season year for UI defaults and new game creation.
     * Sep–Feb → season that started in that September (Jan/Feb use prior calendar year).
     * Mar–Aug → upcoming season (same as calendar year), so offseason setup targets next year.
     */
    public int getCurrentSeasonYear() {
        LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
        int month = today.getMonthValue();
        if (month >= 3 && month <= 8) {
            return today.getYear();
        }
        if (month >= 9) {
            return today.getYear();
        }
        return today.getYear() - 1;
    }

    /**
     * Kickoff / Week 1 start date for a season year.
     * Uses NFL_SEASON_START_DATE when it matches the requested year; otherwise
     * falls back to the first Thursday after Labor Day.
     */
    public LocalDate getSeasonStartDate(int seasonYear) {
        if (configuredSeasonStartDate != null && !configuredSeasonStartDate.trim().isEmpty()) {
            try {
                String cleanDate = configuredSeasonStartDate.trim().replaceAll("^\"|\"$", "");
                LocalDate configured = LocalDate.parse(cleanDate);
                if (configured.getYear() == seasonYear) {
                    return configured;
                }
            } catch (Exception ignored) {
                // fall through to calculated start
            }
        }

        LocalDate septemberFirst = LocalDate.of(seasonYear, 9, 1);
        LocalDate laborDay = septemberFirst.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        if (laborDay.getDayOfWeek() == DayOfWeek.THURSDAY) {
            return laborDay;
        }
        return laborDay.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
    }

    /**
     * Season year for a kickoff instant (Jan/Feb games belong to the prior calendar year's season).
     */
    public int resolveSeasonYear(Instant kickoffTime) {
        if (kickoffTime == null) {
            return getCurrentSeasonYear();
        }
        LocalDate gameDate = kickoffTime.atZone(ZoneId.of("America/New_York")).toLocalDate();
        return resolveSeasonYear(gameDate);
    }

    public int resolveSeasonYear(LocalDate gameDate) {
        if (gameDate == null) {
            return getCurrentSeasonYear();
        }
        int year = gameDate.getYear();
        int month = gameDate.getMonthValue();
        if (month == 1 || month == 2) {
            return year - 1;
        }
        return year;
    }

    /**
     * Seasons available in the DB, plus the current/upcoming season so next year can be selected early.
     * Newest first.
     */
    public List<Integer> getAvailableSeasonYears() {
        Set<Integer> years = new LinkedHashSet<>();
        years.add(getCurrentSeasonYear());
        for (Integer year : gameRepository.findDistinctSeasonYears()) {
            if (year != null) {
                years.add(year);
            }
        }
        List<Integer> sorted = new ArrayList<>(years);
        sorted.sort(Comparator.reverseOrder());
        return sorted;
    }

    public int normalizeSeasonYear(Integer seasonYear) {
        return seasonYear != null ? seasonYear : getCurrentSeasonYear();
    }
}
