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
        if (!gameScoreService.isApiConfigured()) {
            System.out.println("Odds API not configured, skipping scoring");
            return;
        }
        
        try {
            // Fetch live scores from The Odds API
            List<GameScoreService.GameScoreResult> scoreResults = gameScoreService.fetchLiveScores();
            System.out.println("Fetched " + scoreResults.size() + " game results from API");
            
            if (scoreResults.isEmpty()) {
                System.out.println("No game results returned from API - nothing to update");
                return;
            }
            
            for (GameScoreService.GameScoreResult result : scoreResults) {
                Game game = result.getGame();
                String winningTeam = result.getWinningTeam();
                String previousWinner = game.getWinningTeam();
                
                if (!game.isScored()) {
                    System.out.println("Scoring game: " + game.getAwayTeam() + " @ " + game.getHomeTeam() + " - Winner: " + winningTeam);
                } else {
                    System.out.println("Re-scoring game: " + game.getAwayTeam() + " @ " + game.getHomeTeam() + 
                        " - Previous: " + previousWinner + ", New: " + winningTeam);
                }
                
                game.setWinningTeam(winningTeam);
                game.setScored(true);
                gameRepository.save(game);

                // Score all picks for this game
                List<Pick> picksForGame = pickRepository.findByGame(game);
                for (Pick pick : picksForGame) {
                    boolean isCorrect = pick.getPickedTeam().equals(winningTeam);
                    pick.setCorrect(isCorrect);
                    pick.setScoredAt(LocalDateTime.now());
                    pickRepository.save(pick);
                }
                
                System.out.println("✅ Updated " + picksForGame.size() + " picks for " + 
                    game.getAwayTeam() + " " + result.getAwayScore() + " @ " + 
                    game.getHomeTeam() + " " + result.getHomeScore() + " - Winner: " + winningTeam);
            }
            
        } catch (Exception e) {
            System.err.println("Error scoring games with real data: " + e.getMessage());
            System.out.println("Skipping scoring due to API error - games will remain unscored until next attempt");
        }
    }
} 