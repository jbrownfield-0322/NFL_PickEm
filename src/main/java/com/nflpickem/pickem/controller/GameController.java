package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.GameWithOddsDto;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.service.GameService;
import com.nflpickem.pickem.service.OddsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final OddsService oddsService;

    public GameController(GameService gameService, OddsService oddsService) {
        this.gameService = gameService;
        this.oddsService = oddsService;
    }

    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }
    
    @GetMapping("/with-odds")
    public ResponseEntity<List<GameWithOddsDto>> getAllGamesWithOdds() {
        return ResponseEntity.ok(gameService.getAllGamesWithOdds());
    }

    @GetMapping("/week/{weekNum}")
    public ResponseEntity<List<Game>> getGamesByWeek(@PathVariable Integer weekNum) {
        return ResponseEntity.ok(gameService.getGamesByWeek(weekNum));
    }
    
    @GetMapping("/week/{weekNum}/with-odds")
    public ResponseEntity<List<GameWithOddsDto>> getGamesByWeekWithOdds(@PathVariable Integer weekNum) {
        return ResponseEntity.ok(gameService.getGamesByWeekWithOdds(weekNum));
    }
    
    @PostMapping("/week/{weekNum}/fetch-odds")
    public ResponseEntity<String> fetchOddsForWeek(@PathVariable Integer weekNum) {
        try {
            oddsService.fetchOddsForWeek(weekNum);
            return ResponseEntity.ok("Odds fetching initiated for week " + weekNum);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching odds: " + e.getMessage());
        }
    }

    @GetMapping("/currentWeek")
    public ResponseEntity<Integer> getCurrentWeek() {
        return ResponseEntity.ok(gameService.getCurrentWeek());
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest request) {
        try {
            Game game = new Game();
            game.setWeek(request.getWeek());
            game.setHomeTeam(request.getHomeTeam());
            game.setAwayTeam(request.getAwayTeam());
            
            // Parse the kickoff time string to Instant
            if (request.getKickoffTime() != null && !request.getKickoffTime().isEmpty()) {
                Instant kickoffInstant = parseKickoffTime(request.getKickoffTime());
                game.setKickoffTime(kickoffInstant);
            }
            
            game.setWinningTeam(request.getWinningTeam());
            game.setScored(request.getScored() != null ? request.getScored() : false);
            
            // Save the game using the service
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
        private Integer week;
        private String homeTeam;
        private String awayTeam;
        private String kickoffTime;
        private String winningTeam;
        private Boolean scored;

        // Getters and setters
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