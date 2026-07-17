package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.repository.PickRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

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

    // OPTIMIZATION: Reduced frequency to every 2 hours instead of every hour to conserve API quota
    // On game days, this still provides timely updates while reducing API calls by 50%
    @Scheduled(fixedRate = 7200000) // Schedule to run every 2 hours (was 1 hour)
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

                int pickCount = gradePicksForGame(game);
                
                System.out.println("✅ Updated " + pickCount + " picks for " + 
                    game.getAwayTeam() + " " + result.getAwayScore() + " @ " + 
                    game.getHomeTeam() + " " + result.getHomeScore() + " - Winner: " + winningTeam);
            }
            
        } catch (Exception e) {
            System.err.println("Error scoring games with real data: " + e.getMessage());
            System.out.println("Skipping scoring due to API error - games will remain unscored until next attempt");
        }
    }

    /**
     * Grade all picks for a game based on its winningTeam.
     * Returns the number of picks updated.
     */
    public int gradePicksForGame(Game game) {
        if (game == null || game.getWinningTeam() == null) {
            return 0;
        }

        List<Pick> picksForGame = pickRepository.findByGame(game);
        String winningTeam = game.getWinningTeam();
        for (Pick pick : picksForGame) {
            boolean isCorrect = pick.getPickedTeam().equals(winningTeam);
            pick.setCorrect(isCorrect);
            pick.setScoredAt(LocalDateTime.now());
            pickRepository.save(pick);
        }
        return picksForGame.size();
    }

    /**
     * Re-grade picks for games that already have winners set.
     * Use this to repair weeks where games were marked scored without updating pick.correct
     * (e.g. admin UI / scrapers / Saturday games that never hit the Odds API path).
     *
     * @param week optional week filter; null regrades all scored games
     * @return number of games whose picks were regraded
     */
    public int regradePicksForScoredGames(Integer week) {
        List<Game> games = week != null
                ? gameRepository.findByWeek(week)
                : gameRepository.findAll();

        int gamesRegraded = 0;
        int picksUpdated = 0;

        for (Game game : games) {
            if (game.isScored() && game.getWinningTeam() != null) {
                picksUpdated += gradePicksForGame(game);
                gamesRegraded++;
                System.out.println("Regraded picks for week " + game.getWeek() + ": " +
                        game.getAwayTeam() + " @ " + game.getHomeTeam() + " -> " + game.getWinningTeam());
            }
        }

        System.out.println("Regraded " + picksUpdated + " picks across " + gamesRegraded +
                " scored games" + (week != null ? " for week " + week : ""));
        return gamesRegraded;
    }
}