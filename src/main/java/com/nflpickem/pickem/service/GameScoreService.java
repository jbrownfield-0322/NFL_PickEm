package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class GameScoreService {
    
    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    
    @Value("${ODDS_API_KEY:}")
    private String oddsApiKey;
    
    @Value("${ODDS_API_BASE_URL}")
    private String oddsApiBaseUrl;
    
    public GameScoreService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Fetch live scores from The Odds API for completed games
     */
    public List<GameScoreResult> fetchLiveScores() {
        if (oddsApiKey == null || oddsApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ODDS_API_KEY is not configured");
        }
        
        try {
            // Fetch scores from The Odds API
            String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&daysFrom=1&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            System.out.println("Fetching live NFL scores from: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ScoreApiResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ScoreApiResponse[].class);
            
            if (response.getBody() != null) {
                return processScoreResponse(response.getBody());
            }
            
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching scores from API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch scores from API: " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("Network error fetching scores: " + e.getMessage());
            throw new RuntimeException("Network error fetching scores: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching scores: " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching scores: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Process the score API response and match with existing games
     */
    private List<GameScoreResult> processScoreResponse(ScoreApiResponse[] responses) {
        List<GameScoreResult> results = new ArrayList<>();
        
        for (ScoreApiResponse response : responses) {
            try {
                // Only process completed games
                if (!"final".equals(response.completed)) {
                    continue;
                }
                
                // Find matching game in our database
                Game game = findMatchingGame(response);
                if (game != null) {
                    String winningTeam = determineWinner(response);
                    results.add(new GameScoreResult(game, winningTeam, response.away_score, response.home_score));
                }
                
            } catch (Exception e) {
                System.err.println("Error processing score response for " + response.away_team + " @ " + response.home_team + ": " + e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * Find matching game in database
     */
    private Game findMatchingGame(ScoreApiResponse response) {
        // Try exact match first
        Optional<Game> exactMatch = gameRepository.findByHomeTeamAndAwayTeam(response.home_team, response.away_team);
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        
        // Try reverse match
        Optional<Game> reverseMatch = gameRepository.findByHomeTeamAndAwayTeam(response.away_team, response.home_team);
        if (reverseMatch.isPresent()) {
            return reverseMatch.get();
        }
        
        // Try fuzzy matching for team name variations
        List<Game> allGames = gameRepository.findAll();
        for (Game game : allGames) {
            if (fuzzyTeamMatch(game.getHomeTeam(), response.home_team) && 
                fuzzyTeamMatch(game.getAwayTeam(), response.away_team)) {
                return game;
            }
            if (fuzzyTeamMatch(game.getHomeTeam(), response.away_team) && 
                fuzzyTeamMatch(game.getAwayTeam(), response.home_team)) {
                return game;
            }
        }
        
        return null;
    }
    
    /**
     * Check if two team names match with fuzzy logic
     */
    private boolean fuzzyTeamMatch(String team1, String team2) {
        if (team1 == null || team2 == null) return false;
        
        String normalized1 = team1.toLowerCase().trim();
        String normalized2 = team2.toLowerCase().trim();
        
        if (normalized1.equals(normalized2)) return true;
        
        // Remove common suffixes
        String clean1 = normalized1.replaceAll("\\b(football|team|club|fc)\\b", "").trim();
        String clean2 = normalized2.replaceAll("\\b(football|team|club|fc)\\b", "").trim();
        
        if (clean1.equals(clean2)) return true;
        
        // Check for city-only matches
        String city1 = extractCity(normalized1);
        String city2 = extractCity(normalized2);
        if (!city1.isEmpty() && !city2.isEmpty() && city1.equals(city2)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract city name from team name
     */
    private String extractCity(String teamName) {
        String[] parts = teamName.split("\\s+");
        if (parts.length >= 2) {
            String firstPart = parts[0].toLowerCase();
            if (firstPart.matches("(new|los|san|las|kansas|green|tampa|atlanta|buffalo|carolina|chicago|cleveland|dallas|denver|detroit|houston|indianapolis|jacksonville|miami|minnesota|oakland|philadelphia|pittsburgh|seattle|tennessee|washington)")) {
                return parts[0] + (parts.length > 2 ? " " + parts[1] : "");
            }
        }
        return "";
    }
    
    /**
     * Determine winner from score response
     */
    private String determineWinner(ScoreApiResponse response) {
        if (response.away_score > response.home_score) {
            return response.away_team;
        } else if (response.home_score > response.away_score) {
            return response.home_team;
        } else {
            return "TIE";
        }
    }
    
    /**
     * Check if the API is properly configured
     */
    public boolean isApiConfigured() {
        return oddsApiKey != null && !oddsApiKey.trim().isEmpty();
    }
    
    /**
     * Check if it's a game day (Thursday, Sunday, or Monday during NFL season)
     */
    public boolean isGameDay() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        
        // NFL games are typically on Thursday (4), Sunday (7), and Monday (1)
        return dayOfWeek == 1 || dayOfWeek == 4 || dayOfWeek == 7;
    }
    
    /**
     * Check if we should start fetching scores (after first game of the day)
     */
    public boolean shouldStartFetchingScores() {
        if (!isGameDay()) {
            return false;
        }
        
        // Get the earliest game time for today
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.of("America/New_York")).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.of("America/New_York")).toInstant();
        
        List<Game> todaysGames = gameRepository.findByKickoffTimeBetween(startOfDay, endOfDay);
        
        if (todaysGames.isEmpty()) {
            return false;
        }
        
        // Find the earliest game
        Instant earliestGame = todaysGames.stream()
            .map(Game::getKickoffTime)
            .min(Instant::compareTo)
            .orElse(null);
        
        if (earliestGame == null) {
            return false;
        }
        
        // Start fetching 1 hour before the first game
        Instant fetchStartTime = earliestGame.minus(java.time.Duration.ofHours(1));
        return Instant.now().isAfter(fetchStartTime);
    }
    
    // Inner classes for API response mapping
    public static class ScoreApiResponse {
        public String id;
        public String sport_key;
        public String sport_title;
        public String commence_time;
        public String home_team;
        public String away_team;
        public Integer home_score;
        public Integer away_score;
        public String completed;
    }
    
    public static class GameScoreResult {
        private final Game game;
        private final String winningTeam;
        private final Integer awayScore;
        private final Integer homeScore;
        
        public GameScoreResult(Game game, String winningTeam, Integer awayScore, Integer homeScore) {
            this.game = game;
            this.winningTeam = winningTeam;
            this.awayScore = awayScore;
            this.homeScore = homeScore;
        }
        
        public Game getGame() { return game; }
        public String getWinningTeam() { return winningTeam; }
        public Integer getAwayScore() { return awayScore; }
        public Integer getHomeScore() { return homeScore; }
    }
}
