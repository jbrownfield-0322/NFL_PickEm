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
    private final GameScoreService gameScoreService;

    public ScoringService(GameRepository gameRepository, PickRepository pickRepository, GameScoreService gameScoreService) {
        this.gameRepository = gameRepository;
        this.pickRepository = pickRepository;
        this.gameScoreService = gameScoreService;
    }

    @Scheduled(fixedRate = 3600000) // Schedule to run every hour
    public void runScoring() {
        System.out.println("Running scoring task at " + LocalDateTime.now());
        
        // Only run on game days and after the first game starts
        if (gameScoreService.isGameDay() && gameScoreService.shouldStartFetchingScores()) {
            scoreGamesWithRealData();
        } else {
            System.out.println("Not a game day or too early to fetch scores, skipping scoring task");
        }
    }

    /**
     * Score games using real data from The Odds API
     */
    public void scoreGamesWithRealData() {
        try {
            if (!gameScoreService.isApiConfigured()) {
                System.out.println("Odds API not configured, falling back to legacy scoring");
                scoreGames();
                return;
            }
            
            // Fetch live scores from The Odds API
            List<GameScoreService.GameScoreResult> scoreResults = gameScoreService.fetchLiveScores();
            System.out.println("Fetched " + scoreResults.size() + " game results from API");
            
            if (scoreResults.isEmpty()) {
                System.out.println("No game results returned from API - nothing to update");
                return;
            }
            
            for (GameScoreService.GameScoreResult result : scoreResults) {
                Game game = result.getGame();
                System.out.println("Processing result for game: " + game.getAwayTeam() + " @ " + game.getHomeTeam() + 
                    " (ID: " + game.getId() + ", Already scored: " + game.isScored() + ")");
                
                // Only score games that haven't been scored yet
                if (!game.isScored()) {
                    String winningTeam = result.getWinningTeam();
                    System.out.println("Updating game " + game.getId() + " with winner: " + winningTeam);
                    
                    game.setWinningTeam(winningTeam);
                    game.setScored(true);
                    gameRepository.save(game);

                    // Score all picks for this game
                    List<Pick> picksForGame = pickRepository.findByGame(game);
                    System.out.println("Found " + picksForGame.size() + " picks for this game");
                    
                    for (Pick pick : picksForGame) {
                        boolean isCorrect = pick.getPickedTeam().equals(winningTeam);
                        pick.setCorrect(isCorrect);
                        pick.setScoredAt(LocalDateTime.now());
                        pickRepository.save(pick);
                        System.out.println("Updated pick " + pick.getId() + " - Picked: " + pick.getPickedTeam() + 
                            ", Winner: " + winningTeam + ", Correct: " + isCorrect);
                    }
                    
                    System.out.println("✅ Game " + game.getId() + " scored with real data. " + 
                        game.getAwayTeam() + " " + result.getAwayScore() + " @ " + 
                        game.getHomeTeam() + " " + result.getHomeScore() + " - Winner: " + winningTeam);
                } else {
                    System.out.println("Game " + game.getId() + " already scored, skipping");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error scoring games with real data: " + e.getMessage());
            System.out.println("Falling back to legacy scoring method");
            scoreGames();
        }
    }
    
    /**
     * Legacy scoring method (fallback with random selection)
     */
    public void scoreGames() {
        List<Game> unscoredGames = gameRepository.findByScoredFalseAndKickoffTimeBefore(Instant.now());

        for (Game game : unscoredGames) {
            String winningTeam;
            if (game.getWinningTeam() != null && !game.getWinningTeam().isEmpty()) {
                winningTeam = game.getWinningTeam();
            } else {
                // Fallback to random if no winner scraped yet (game not over or data not updated)
                winningTeam = new Random().nextBoolean() ? game.getHomeTeam() : game.getAwayTeam();
                System.out.println("WARNING: Using random selection for game " + game.getId() + 
                    " (" + game.getAwayTeam() + " @ " + game.getHomeTeam() + ") - Winner: " + winningTeam);
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