package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Persists games in independent transactions so a constraint failure
 * (duplicate season/week/teams) cannot poison the caller's Hibernate session.
 */
@Service
public class GamePersistService {
    private static final Logger logger = LoggerFactory.getLogger(GamePersistService.class);

    private final GameRepository gameRepository;
    private final SeasonService seasonService;

    public GamePersistService(GameRepository gameRepository, SeasonService seasonService) {
        this.gameRepository = gameRepository;
        this.seasonService = seasonService;
    }

    /**
     * Insert a game if it does not already exist. Runs in a new transaction that
     * commits or rolls back independently of the caller.
     *
     * @return the existing or newly saved game, or empty if creation failed
     *         without a recoverable existing row
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Game> createIfAbsent(Integer seasonYear, Integer week,
                                         String homeTeam, String awayTeam,
                                         Instant kickoffTime) {
        int season = seasonYear != null
                ? seasonYear
                : seasonService.resolveSeasonYear(kickoffTime);

        Optional<Game> existing = findByTeamsEitherOrder(season, week, homeTeam, awayTeam);
        if (existing.isPresent()) {
            return existing;
        }

        Game game = new Game();
        game.setSeasonYear(season);
        game.setWeek(week);
        game.setHomeTeam(homeTeam);
        game.setAwayTeam(awayTeam);
        game.setKickoffTime(kickoffTime);
        game.setScored(false);
        game.setWinningTeam("");

        try {
            Game saved = gameRepository.saveAndFlush(game);
            logger.info("Created game: {} @ {} (season {} week {})",
                    awayTeam, homeTeam, season, week);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException e) {
            // This REQUIRES_NEW transaction will roll back; caller session stays clean.
            logger.info("Duplicate game on insert (constraint): {} @ {} season {} week {}",
                    awayTeam, homeTeam, season, week);
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Game> findExisting(Integer seasonYear, Integer week,
                                       String homeTeam, String awayTeam) {
        int season = seasonYear != null ? seasonYear : seasonService.getCurrentSeasonYear();
        return findByTeamsEitherOrder(season, week, homeTeam, awayTeam);
    }

    private Optional<Game> findByTeamsEitherOrder(int season, Integer week,
                                                 String homeTeam, String awayTeam) {
        Optional<Game> match = gameRepository.findBySeasonYearAndWeekAndHomeTeamAndAwayTeam(
                season, week, homeTeam, awayTeam);
        if (match.isPresent()) {
            return match;
        }
        return gameRepository.findBySeasonYearAndWeekAndHomeTeamAndAwayTeam(
                season, week, awayTeam, homeTeam);
    }
}
