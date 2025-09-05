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
        
        // Try different daysFrom values (3, 2, 1) in case of API restrictions
        int[] daysToTry = {3, 2, 1};
        
        for (int daysFrom : daysToTry) {
            try {
                String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&daysFrom=%d&dateFormat=iso", 
                    oddsApiBaseUrl, oddsApiKey, daysFrom);
                
                System.out.println("Fetching live NFL scores from: " + url);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "NFL-Pickem-App/1.0");
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<ScoreApiResponse[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ScoreApiResponse[].class);
                
                if (response.getBody() != null) {
                    System.out.println("API returned " + response.getBody().length + " games (daysFrom=" + daysFrom + ")");
                    
                    if (response.getBody().length == 0) {
                        System.out.println("No games returned from API with daysFrom=" + daysFrom);
                        continue; // Try next daysFrom value
                    }
                    
                    // Log ALL games for debugging (not just first 3)
                    System.out.println("=== ALL GAMES FROM API ===");
                                    for (int i = 0; i < response.getBody().length; i++) {
                    ScoreApiResponse game = response.getBody()[i];
                    System.out.println("Game " + (i+1) + ": " + game.away_team + " @ " + game.home_team + 
                        " (Status: " + game.completed + ", Scores: " + game.getAwayScore() + "-" + game.getHomeScore() + 
                        ", Time: " + game.commence_time + ")");
                }
                    System.out.println("=== END API GAMES ===");
                    
                    return processScoreResponse(response.getBody());
                }
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 422) {
                    System.err.println("daysFrom=" + daysFrom + " not allowed, trying next value...");
                    continue; // Try next daysFrom value
                } else {
                    System.err.println("Error fetching scores from API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to fetch scores from API: " + e.getMessage());
                }
            } catch (ResourceAccessException e) {
                System.err.println("Network error fetching scores: " + e.getMessage());
                throw new RuntimeException("Network error fetching scores: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Unexpected error fetching scores: " + e.getMessage());
                throw new RuntimeException("Unexpected error fetching scores: " + e.getMessage());
            }
        }
        
        // If all daysFrom values failed, try without the parameter (live/upcoming only)
        try {
            String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            System.out.println("Trying without daysFrom parameter: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ScoreApiResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, ScoreApiResponse[].class);
            
            if (response.getBody() != null) {
                System.out.println("API returned " + response.getBody().length + " games (live/upcoming only)");
                return processScoreResponse(response.getBody());
            }
            
        } catch (Exception e) {
            System.err.println("Final attempt failed: " + e.getMessage());
            throw new RuntimeException("Failed to fetch scores from API after all attempts: " + e.getMessage());
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
                Integer awayScore = response.getAwayScore();
                Integer homeScore = response.getHomeScore();
                
                System.out.println("Processing game: " + response.away_team + " @ " + response.home_team + 
                    " (Status: " + response.completed + ", Scores: " + awayScore + "-" + homeScore + ")");
                
                // Process games that are completed AND have scores
                if (!"true".equals(response.completed) && !Boolean.TRUE.equals(response.completed)) {
                    System.out.println("Skipping game - not completed");
                    continue;
                }
                
                // Skip if no scores available
                if (awayScore == null || homeScore == null) {
                    System.out.println("Skipping game - no scores available (awayScore=" + awayScore + ", homeScore=" + homeScore + ")");
                    continue;
                }
                
                System.out.println("✅ Processing completed game with scores: " + response.away_team + " @ " + response.home_team + " (" + awayScore + "-" + homeScore + ")");
                
                // Find matching game in our database
                Game game = findMatchingGame(response);
                if (game != null) {
                    String winningTeam = determineWinner(response);
                    results.add(new GameScoreResult(game, winningTeam, response.away_score, response.home_score));
                    System.out.println("Added game result: " + game.getAwayTeam() + " @ " + game.getHomeTeam() + " - Winner: " + winningTeam);
                } else {
                    System.out.println("No matching game found in database for: " + response.away_team + " @ " + response.home_team);
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
        System.out.println("Looking for match: " + response.away_team + " @ " + response.home_team);
        
        // Try exact match first
        Optional<Game> exactMatch = gameRepository.findByHomeTeamAndAwayTeam(response.home_team, response.away_team);
        if (exactMatch.isPresent()) {
            System.out.println("Found exact match: " + exactMatch.get().getAwayTeam() + " @ " + exactMatch.get().getHomeTeam());
            return exactMatch.get();
        }
        
        // Try reverse match
        Optional<Game> reverseMatch = gameRepository.findByHomeTeamAndAwayTeam(response.away_team, response.home_team);
        if (reverseMatch.isPresent()) {
            System.out.println("Found reverse match: " + reverseMatch.get().getAwayTeam() + " @ " + reverseMatch.get().getHomeTeam());
            return reverseMatch.get();
        }
        
        // Try fuzzy matching for team name variations
        List<Game> allGames = gameRepository.findAll();
        System.out.println("Checking " + allGames.size() + " games in database for fuzzy match...");
        
        for (Game game : allGames) {
            if (fuzzyTeamMatch(game.getHomeTeam(), response.home_team) && 
                fuzzyTeamMatch(game.getAwayTeam(), response.away_team)) {
                System.out.println("Found fuzzy match: " + game.getAwayTeam() + " @ " + game.getHomeTeam());
                return game;
            }
            if (fuzzyTeamMatch(game.getHomeTeam(), response.away_team) && 
                fuzzyTeamMatch(game.getAwayTeam(), response.home_team)) {
                System.out.println("Found reverse fuzzy match: " + game.getAwayTeam() + " @ " + game.getHomeTeam());
                return game;
            }
        }
        
        System.out.println("No match found for: " + response.away_team + " @ " + response.home_team);
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
        Integer awayScore = response.getAwayScore();
        Integer homeScore = response.getHomeScore();
        
        if (awayScore > homeScore) {
            return response.away_team;
        } else if (homeScore > awayScore) {
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
     * Try to get historical scores from a different endpoint
     */
    public String getHistoricalScores() {
        if (oddsApiKey == null || oddsApiKey.trim().isEmpty()) {
            return "API not configured";
        }
        
        StringBuilder response = new StringBuilder();
        
        // Try the historical scores endpoint
        try {
            String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&daysFrom=1&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            response.append("=== TRYING HISTORICAL SCORES ===\n");
            response.append("URL: ").append(url).append("\n");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ScoreApiResponse[]> apiResponse = restTemplate.exchange(
                url, HttpMethod.GET, entity, ScoreApiResponse[].class);
            
            if (apiResponse.getBody() != null) {
                response.append("Found ").append(apiResponse.getBody().length).append(" games\n");
                
                // Look for games with actual scores
                int gamesWithScores = 0;
                for (ScoreApiResponse game : apiResponse.getBody()) {
                    Integer awayScore = game.getAwayScore();
                    Integer homeScore = game.getHomeScore();
                    if (awayScore != null && homeScore != null) {
                        gamesWithScores++;
                        response.append("Game with scores: ").append(game.away_team).append(" @ ").append(game.home_team)
                            .append(" (Status: ").append(game.completed)
                            .append(", Scores: ").append(awayScore).append("-").append(homeScore)
                            .append(", Time: ").append(game.commence_time).append(")\n");
                    }
                }
                
                response.append("Total games with scores: ").append(gamesWithScores).append("\n");
                
                if (gamesWithScores == 0) {
                    response.append("No games with scores found. This might be because:\n");
                    response.append("1. It's off-season and no recent games\n");
                    response.append("2. The API doesn't have historical scores for this period\n");
                    response.append("3. Games haven't been marked as final yet\n");
                }
            }
            
        } catch (Exception e) {
            response.append("Error: ").append(e.getMessage()).append("\n");
        }
        
        return response.toString();
    }

    /**
     * Get raw API response for debugging
     */
    public String getRawApiResponse() {
        if (oddsApiKey == null || oddsApiKey.trim().isEmpty()) {
            return "API not configured";
        }
        
        StringBuilder response = new StringBuilder();
        
        // Try different daysFrom values
        int[] daysToTry = {3, 2, 1};
        
        for (int daysFrom : daysToTry) {
            try {
                String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&daysFrom=%d&dateFormat=iso", 
                    oddsApiBaseUrl, oddsApiKey, daysFrom);
                
                response.append("=== TRYING daysFrom=").append(daysFrom).append(" ===\n");
                response.append("URL: ").append(url).append("\n");
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "NFL-Pickem-App/1.0");
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                ResponseEntity<ScoreApiResponse[]> apiResponse = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ScoreApiResponse[].class);
                
                if (apiResponse.getBody() != null) {
                    response.append("SUCCESS! Found ").append(apiResponse.getBody().length).append(" games\n");
                    
                    for (int i = 0; i < apiResponse.getBody().length; i++) {
                        ScoreApiResponse game = apiResponse.getBody()[i];
                        response.append("Game ").append(i + 1).append(": ")
                            .append(game.away_team).append(" @ ").append(game.home_team)
                            .append(" (Status: ").append(game.completed)
                            .append(", Scores: ").append(game.away_score).append("-").append(game.home_score)
                            .append(", Time: ").append(game.commence_time).append(")\n");
                    }
                    
                    // Also show database games for comparison
                    response.append("\n=== DATABASE GAMES ===\n");
                    List<Game> dbGames = gameRepository.findAll();
                    response.append("Database has ").append(dbGames.size()).append(" games:\n");
                    for (int i = 0; i < Math.min(10, dbGames.size()); i++) {
                        Game game = dbGames.get(i);
                        response.append("DB Game ").append(i + 1).append(": ")
                            .append(game.getAwayTeam()).append(" @ ").append(game.getHomeTeam())
                            .append(" (Week: ").append(game.getWeek())
                            .append(", Scored: ").append(game.isScored()).append(")\n");
                    }
                    
                    return response.toString();
                } else {
                    response.append("No response body\n");
                }
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 422) {
                    response.append("daysFrom=").append(daysFrom).append(" not allowed\n");
                } else {
                    response.append("Error: ").append(e.getStatusCode()).append(" - ").append(e.getResponseBodyAsString()).append("\n");
                }
            } catch (Exception e) {
                response.append("Exception: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Try without daysFrom parameter
        try {
            String url = String.format("%s/sports/americanfootball_nfl/scores/?apiKey=%s&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            response.append("=== TRYING WITHOUT daysFrom ===\n");
            response.append("URL: ").append(url).append("\n");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ScoreApiResponse[]> apiResponse = restTemplate.exchange(
                url, HttpMethod.GET, entity, ScoreApiResponse[].class);
            
            if (apiResponse.getBody() != null) {
                response.append("SUCCESS! Found ").append(apiResponse.getBody().length).append(" games (live/upcoming)\n");
                
                for (int i = 0; i < apiResponse.getBody().length; i++) {
                    ScoreApiResponse game = apiResponse.getBody()[i];
                    response.append("Game ").append(i + 1).append(": ")
                        .append(game.away_team).append(" @ ").append(game.home_team)
                        .append(" (Status: ").append(game.completed)
                        .append(", Scores: ").append(game.getAwayScore()).append("-").append(game.getHomeScore())
                        .append(", Time: ").append(game.commence_time).append(")\n");
                }
            }
        } catch (Exception e) {
            response.append("Final attempt failed: ").append(e.getMessage()).append("\n");
        }
        
        return response.toString();
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
        public List<ScoreEntry> scores;
        public String last_update;
        
        // Helper method to get scores from the scores array
        public Integer getAwayScore() {
            if (scores != null) {
                System.out.println("DEBUG: Looking for away team '" + away_team + "' in scores array:");
                for (ScoreEntry score : scores) {
                    System.out.println("  - Found score entry: name='" + score.name + "', score='" + score.score + "'");
                    if (score.name.equals(away_team)) {
                        try {
                            Integer parsedScore = Integer.parseInt(score.score);
                            System.out.println("  - MATCH! Parsed away score: " + parsedScore);
                            return parsedScore;
                        } catch (NumberFormatException e) {
                            System.out.println("  - MATCH but failed to parse score: " + score.score);
                            return null;
                        }
                    }
                }
                System.out.println("  - No match found for away team '" + away_team + "'");
            } else {
                System.out.println("DEBUG: scores array is null, using fallback away_score: " + away_score);
            }
            return away_score; // Fallback to old format
        }
        
        public Integer getHomeScore() {
            if (scores != null) {
                System.out.println("DEBUG: Looking for home team '" + home_team + "' in scores array:");
                for (ScoreEntry score : scores) {
                    System.out.println("  - Found score entry: name='" + score.name + "', score='" + score.score + "'");
                    if (score.name.equals(home_team)) {
                        try {
                            Integer parsedScore = Integer.parseInt(score.score);
                            System.out.println("  - MATCH! Parsed home score: " + parsedScore);
                            return parsedScore;
                        } catch (NumberFormatException e) {
                            System.out.println("  - MATCH but failed to parse score: " + score.score);
                            return null;
                        }
                    }
                }
                System.out.println("  - No match found for home team '" + home_team + "'");
            } else {
                System.out.println("DEBUG: scores array is null, using fallback home_score: " + home_score);
            }
            return home_score; // Fallback to old format
        }
    }
    
    public static class ScoreEntry {
        public String name;
        public String score;
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
