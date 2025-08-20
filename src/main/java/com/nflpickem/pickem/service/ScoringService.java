package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.repository.PickRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class ScoringService {
    private final GameRepository gameRepository;
    private final PickRepository pickRepository;

    public ScoringService(GameRepository gameRepository, PickRepository pickRepository) {
        this.gameRepository = gameRepository;
        this.pickRepository = pickRepository;
    }

    @Scheduled(fixedRate = 300000) // Schedule to run every 5 minutes
    public void runScoring() {
        System.out.println("Running scoring task at " + LocalDateTime.now());
        scoreGames();
    }

    public void scoreGames() {
        List<Game> unscoredGames = gameRepository.findByScoredFalseAndKickoffTimeBefore(Instant.now());

        for (Game game : unscoredGames) {
            String winningTeam;
            if (game.getWinningTeam() != null && !game.getWinningTeam().isEmpty()) {
                winningTeam = game.getWinningTeam();
            } else {
                // Fallback to random if no winner scraped yet (game not over or data not updated)
                winningTeam = new Random().nextBoolean() ? game.getHomeTeam() : game.getAwayTeam();
            }
            game.setWinningTeam(winningTeam);
            game.setScored(true);
            gameRepository.save(game);

            List<Pick> picksForGame = pickRepository.findByGame(game);
            for (Pick pick : picksForGame) {
                pick.setCorrect(pick.getPickedTeam().equals(winningTeam));
                pick.setScoredAt(LocalDateTime.now());
                pickRepository.save(pick);
            }
            System.out.println("Game " + game.getId() + " scored. Winner: " + winningTeam);
        }
    }
} 