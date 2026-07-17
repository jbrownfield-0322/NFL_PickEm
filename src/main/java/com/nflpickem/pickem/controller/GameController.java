package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.GameWithOddsDto;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.service.GameService;
import com.nflpickem.pickem.service.ScoringService;
import com.nflpickem.pickem.service.GameScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final ScoringService scoringService;
    private final GameScoreService gameScoreService;

    public GameController(GameService gameService, ScoringService scoringService, GameScoreService gameScoreService) {
        this.gameService = gameService;
        this.scoringService = scoringService;
        this.gameScoreService = gameScoreService;
    }

    @GetMapping
    public ResponseEntity<List<GameWithOddsDto>> getAllGames(
            @RequestParam(required = false) Integer seasonYear) {
        List<Game> games = seasonYear != null
                ? gameService.getGamesBySeason(seasonYear)
                : gameService.getAllGames();
        List<GameWithOddsDto> gamesWithOdds = games.stream()
            .map(game -> {
                var odds = gameService.getOddsForGame(game.getId());
                return new GameWithOddsDto(game, odds);
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(gamesWithOdds);
    }

    @GetMapping("/seasons")
    public ResponseEntity<List<Integer>> getAvailableSeasons() {
        return ResponseEntity.ok(gameService.getAvailableSeasonYears());
    }

    @GetMapping("/currentSeason")
    public ResponseEntity<Integer> getCurrentSeason() {
        return ResponseEntity.ok(gameService.getCurrentSeasonYear());
    }

    @GetMapping("/week/{weekNum}")
    public ResponseEntity<List<GameWithOddsDto>> getGamesByWeek(
            @PathVariable Integer weekNum,
            @RequestParam(required = false) Integer seasonYear) {
        List<Game> games = gameService.getGamesByWeek(weekNum, seasonYear);
        List<GameWithOddsDto> gamesWithOdds = games.stream()
            .map(game -> {
                var odds = gameService.getOddsForGame(game.getId());
                return new GameWithOddsDto(game, odds);
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(gamesWithOdds);
    }

    @GetMapping("/currentWeek")
    public ResponseEntity<Integer> getCurrentWeek(@RequestParam(required = false) Integer seasonYear) {
        return ResponseEntity.ok(gameService.getCurrentWeek(seasonYear));
    }
    
    @GetMapping("/{gameId}/odds")
    public ResponseEntity<?> getOddsForGame(@PathVariable Long gameId) {
        try {
            var odds = gameService.getOddsForGame(gameId);
            return ResponseEntity.ok(odds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/week/{weekNum}/odds")
    public ResponseEntity<?> getOddsForWeek(
            @PathVariable Integer weekNum,
            @RequestParam(required = false) Integer seasonYear) {
        try {
            var odds = gameService.getOddsForWeek(weekNum, seasonYear);
            return ResponseEntity.ok(odds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest request) {
        try {
            Game game = new Game();
            game.setWeek(request.getWeek());
            game.setHomeTeam(request.getHomeTeam());
            game.setAwayTeam(request.getAwayTeam());
            
            if (request.getKickoffTime() != null && !request.getKickoffTime().isEmpty()) {
                Instant kickoffInstant = parseKickoffTime(request.getKickoffTime());
                game.setKickoffTime(kickoffInstant);
            }

            if (request.getSeasonYear() != null) {
                game.setSeasonYear(request.getSeasonYear());
            }
            
            game.setWinningTeam(request.getWinningTeam());
            game.setScored(request.getScored() != null ? request.getScored() : false);
            
            Game savedGame = gameService.saveGame(game);
            return ResponseEntity.ok(savedGame);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<Game> updateGameScore(@PathVariable Long id, @RequestBody UpdateScoreRequest request) {
        try {
            Game updatedGame = gameService.updateGameScore(id, request.getWinningTeam());
            if (updatedGame == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedGame);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGame(@PathVariable Long id) {
        try {
            boolean deleted = gameService.deleteGame(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok("Deleted game " + id);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error deleting game: " + e.getMessage());
        }
    }

    /**
     * Remove playoff games incorrectly stored as a regular-season week (e.g. week 18).
     */
    @PostMapping("/week/{weekNum}/cleanup-playoffs")
    public ResponseEntity<?> cleanupPlayoffGames(
            @PathVariable Integer weekNum,
            @RequestParam(required = false) Integer seasonYear) {
        try {
            List<String> deleted = gameService.removeGamesOutsideRegularSeasonWeek(weekNum, seasonYear);
            return ResponseEntity.ok(java.util.Map.of(
                "week", weekNum,
                "seasonYear", seasonYear != null ? seasonYear : gameService.getCurrentSeasonYear(),
                "deletedCount", deleted.size(),
                "deleted", deleted
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error cleaning up playoff games: " + e.getMessage());
        }
    }

    @PostMapping("/update-scores")
    public ResponseEntity<String> updateScoresFromApi() {
        try {
            scoringService.scoreGamesWithRealData();
            return ResponseEntity.ok("Scores updated successfully from API");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error updating scores: " + e.getMessage());
        }
    }

    /**
     * Re-grade picks for games that already have winners set.
     */
    @PostMapping("/regrade-picks")
    public ResponseEntity<String> regradePicks(
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) Integer seasonYear) {
        try {
            int gamesRegraded = scoringService.regradePicksForScoredGames(week, seasonYear);
            String scope = week != null ? "week " + week : "all weeks";
            if (seasonYear != null) {
                scope += " season " + seasonYear;
            }
            return ResponseEntity.ok("Regraded picks for " + gamesRegraded + " scored games (" + scope + ")");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error regrading picks: " + e.getMessage());
        }
    }

    @GetMapping("/test-api")
    public ResponseEntity<String> testApiConnection() {
        try {
            if (!gameScoreService.isApiConfigured()) {
                return ResponseEntity.ok("API not configured - check ODDS_API_KEY environment variable");
            }
            
            // Show database games first
            var allGames = gameService.getAllGames();
            var dbInfo = "Database has " + allGames.size() + " games. ";
            if (allGames.size() > 0) {
                dbInfo += "Sample: " + allGames.get(0).getAwayTeam() + " @ " + allGames.get(0).getHomeTeam();
            }
            
            var results = gameScoreService.fetchLiveScores();
            return ResponseEntity.ok(dbInfo + " API test successful. Found " + results.size() + " game results. Check server logs for details.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("API test failed: " + e.getMessage());
        }
    }

    @GetMapping("/debug-api")
    public ResponseEntity<String> debugApiResponse() {
        try {
            if (!gameScoreService.isApiConfigured()) {
                return ResponseEntity.ok("API not configured - check ODDS_API_KEY environment variable");
            }
            
            // Get raw API response for debugging
            var rawResponse = gameScoreService.getRawApiResponse();
            return ResponseEntity.ok("Raw API Response:\n" + rawResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Debug failed: " + e.getMessage());
        }
    }

    @GetMapping("/test-historical")
    public ResponseEntity<String> testHistoricalScores() {
        try {
            if (!gameScoreService.isApiConfigured()) {
                return ResponseEntity.ok("API not configured - check ODDS_API_KEY environment variable");
            }
            
            var historicalResponse = gameScoreService.getHistoricalScores();
            return ResponseEntity.ok("Historical Scores Test:\n" + historicalResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Historical test failed: " + e.getMessage());
        }
    }

    @PostMapping("/update-yesterday-scores")
    public ResponseEntity<String> updateYesterdayScores() {
        try {
            if (!gameScoreService.isApiConfigured()) {
                return ResponseEntity.badRequest()
                    .body("API not configured - check ODDS_API_KEY environment variable");
            }
            
            int updatedCount = gameScoreService.updateYesterdayScores();
            return ResponseEntity.ok("Updated " + updatedCount + " games from yesterday (Monday or Thursday night games)");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error updating yesterday's scores: " + e.getMessage());
        }
    }

    private Instant parseKickoffTime(String kickoffTimeStr) {
        try {
            // Try parsing as ISO format first
            return Instant.parse(kickoffTimeStr);
        } catch (Exception e) {
            try {
                // Try parsing as local date time and convert to instant
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDateTime = LocalDateTime.parse(kickoffTimeStr, formatter);
                return localDateTime.atZone(ZoneId.of("America/New_York")).toInstant();
            } catch (Exception e2) {
                throw new RuntimeException("Invalid kickoff time format. Use ISO format or yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    // Request DTOs
    public static class CreateGameRequest {
        private Integer seasonYear;
        private Integer week;
        private String homeTeam;
        private String awayTeam;
        private String kickoffTime;
        private String winningTeam;
        private Boolean scored;

        public Integer getSeasonYear() { return seasonYear; }
        public void setSeasonYear(Integer seasonYear) { this.seasonYear = seasonYear; }

        public Integer getWeek() { return week; }
        public void setWeek(Integer week) { this.week = week; }
        
        public String getHomeTeam() { return homeTeam; }
        public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
        
        public String getAwayTeam() { return awayTeam; }
        public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
        
        public String getKickoffTime() { return kickoffTime; }
        public void setKickoffTime(String kickoffTime) { this.kickoffTime = kickoffTime; }
        
        public String getWinningTeam() { return winningTeam; }
        public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }
        
        public Boolean getScored() { return scored; }
        public void setScored(Boolean scored) { this.scored = scored; }
    }

    public static class UpdateScoreRequest {
        private String winningTeam;

        public String getWinningTeam() { return winningTeam; }
        public void setWinningTeam(String winningTeam) { this.winningTeam = winningTeam; }
    }
} 