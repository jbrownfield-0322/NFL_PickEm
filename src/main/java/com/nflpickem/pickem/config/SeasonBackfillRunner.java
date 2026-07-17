package com.nflpickem.pickem.config;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.service.SeasonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backfills Game.seasonYear for existing rows after multi-season support is deployed.
 */
@Component
public class SeasonBackfillRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SeasonBackfillRunner.class);

    private final GameRepository gameRepository;
    private final SeasonService seasonService;

    public SeasonBackfillRunner(GameRepository gameRepository, SeasonService seasonService) {
        this.gameRepository = gameRepository;
        this.seasonService = seasonService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Game> missing = gameRepository.findBySeasonYearIsNull();
        if (missing.isEmpty()) {
            return;
        }

        int updated = 0;
        for (Game game : missing) {
            int seasonYear = seasonService.resolveSeasonYear(game.getKickoffTime());
            game.setSeasonYear(seasonYear);
            gameRepository.save(game);
            updated++;
        }
        logger.info("Backfilled seasonYear on {} games", updated);
    }
}
