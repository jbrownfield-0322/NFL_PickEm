package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
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
import java.util.stream.Collectors;

@Service
public class OddsService {
    
    private final BettingOddsRepository bettingOddsRepository;
    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    
    @Value("${ODDS_API_KEY:}")
    private String oddsApiKey;
    
    @Value("${ODDS_API_BASE_URL}")
    private String oddsApiBaseUrl;
    
    @Value("${ODDS_UPDATE_INTERVAL_HOURS}")
    private int updateIntervalHours;
    
    @Value("${NFL_SEASON_START_DATE}")
    private String nflSeasonStartDate;
    
    public OddsService(BettingOddsRepository bettingOddsRepository, GameRepository gameRepository) {
        this.bettingOddsRepository = bettingOddsRepository;
        this.gameRepository = gameRepository;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Fetch odds from The Odds API for a specific week
     */
    public List<BettingOdds> fetchOddsForWeek(Integer week) {
        if (oddsApiKey == null || oddsApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ODDS_API_KEY is not configured");
        }
        
        try {
            String url = String.format("%s/sports/americanfootball_nfl/odds/?apiKey=%s&regions=us&markets=spreads&bookmakers=fanduel&oddsFormat=american&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            System.out.println("Fetching FanDuel point spreads for week " + week + " from: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<OddsApiResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, OddsApiResponse[].class);
            
            if (response.getBody() != null) {
                return processOddsResponse(response.getBody(), week);
            }
            
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching odds from API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch odds from API: " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("Network error fetching odds: " + e.getMessage());
            throw new RuntimeException("Network error fetching odds: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching odds: " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching odds: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Fetch odds from The Odds API for all available games (creates/updates games as needed)
     */
    public List<BettingOdds> fetchAllAvailableOdds() {
        if (oddsApiKey == null || oddsApiKey.trim().isEmpty()) {
            throw new IllegalStateException("ODDS_API_KEY is not configured");
        }
        
        try {
            String url = String.format("%s/sports/americanfootball_nfl/odds/?apiKey=%s&regions=us&markets=spreads&bookmakers=fanduel&oddsFormat=american&dateFormat=iso", 
                oddsApiBaseUrl, oddsApiKey);
            
            System.out.println("Fetching all available FanDuel point spreads from: " + url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NFL-Pickem-App/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<OddsApiResponse[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, OddsApiResponse[].class);
            
            if (response.getBody() != null) {
                return processAllOddsResponse(response.getBody());
            }
            
        } catch (HttpClientErrorException e) {
            System.err.println("Error fetching odds from API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch odds from API: " + e.getMessage());
        } catch (ResourceAccessException e) {
            System.err.println("Network error fetching odds: " + e.getMessage());
            throw new RuntimeException("Network error fetching odds: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error fetching odds: " + e.getMessage());
            throw new RuntimeException("Unexpected error fetching odds: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Process the API response and match with existing games or create new ones
     */
    private List<BettingOdds> processOddsResponse(OddsApiResponse[] responses, Integer week) {
        List<BettingOdds> newOdds = new ArrayList<>();
        List<Game> weekGames = gameRepository.findByWeek(week);
        
        for (OddsApiResponse response : responses) {
            Game game = findOrCreateGame(response, weekGames, week);
            if (game != null) {
                BettingOdds odds = createBettingOddsFromResponse(response, game);
                if (odds != null) {
                    newOdds.add(odds);
                }
            }
        }
        
        return newOdds;
    }
    
    /**
     * Process all odds responses and create/update games as needed
     */
    private List<BettingOdds> processAllOddsResponse(OddsApiResponse[] responses) {
        List<BettingOdds> allOdds = new ArrayList<>();
        
        for (OddsApiResponse response : responses) {
            try {
                // Determine the week from the game time
                Integer week = determineWeekFromGameTime(response.commence_time);
                if (week == null) {
                    System.err.println("Could not determine week for game: " + response.getAwayTeam() + " @ " + response.getHomeTeam());
                    continue;
                }
                
                // Get all games for this week
                List<Game> weekGames = gameRepository.findByWeek(week);
                
                // Find or create the game
                Game game = findOrCreateGame(response, weekGames, week);
                if (game != null) {
                    BettingOdds odds = createBettingOddsFromResponse(response, game);
                    if (odds != null) {
                        allOdds.add(odds);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing odds response for " + response.getAwayTeam() + " @ " + response.getHomeTeam() + ": " + e.getMessage());
            }
        }
        
        return allOdds;
    }
    
    /**
     * Determine the NFL week from a game's commence time
     * This is a complex calculation because NFL weeks don't follow a simple pattern
     */
    private Integer determineWeekFromGameTime(String commenceTime) {
        try {
            Instant gameTime = Instant.parse(commenceTime);
            LocalDate gameDate = gameTime.atZone(java.time.ZoneId.of("America/New_York")).toLocalDate();
            
            // For NFL games, the season year is typically the year the season starts (September)
            // Even if games are in January of the next year, they belong to the previous season
            int seasonYear = gameDate.getYear();
            if (gameDate.getMonthValue() <= 2) { // January/February games belong to previous season
                seasonYear = gameDate.getYear() - 1;
            }
            
            // NFL season typically starts the first Thursday after Labor Day (first Monday of September)
            LocalDate nflSeasonStart = calculateNflSeasonStart(seasonYear);
            
            if (gameDate.isBefore(nflSeasonStart)) {
                // Before season start - could be preseason or previous season
                Integer week = determinePreseasonOrPreviousSeasonWeek(gameDate, seasonYear);
                System.out.println("Game date " + gameDate + " is before season start " + nflSeasonStart + ", determined week: " + week);
                return week;
            }
            
            // Calculate week based on NFL week structure
            Integer week = calculateNflWeek(gameDate, nflSeasonStart);
            System.out.println("Game date " + gameDate + " (season start: " + nflSeasonStart + ") determined week: " + week);
            return week;
            
        } catch (Exception e) {
            System.err.println("Error determining week from game time '" + commenceTime + "': " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate the NFL season start date for a given year
     * NFL season starts the first Thursday after Labor Day (first Monday of September)
     * Can be overridden by configuration
     */
    private LocalDate calculateNflSeasonStart(int year) {
        // Check if season start date is configured
        if (nflSeasonStartDate != null && !nflSeasonStartDate.trim().isEmpty()) {
            try {
                LocalDate configuredStart = LocalDate.parse(nflSeasonStartDate);
                if (configuredStart.getYear() == year) {
                    System.out.println("Using configured NFL season start date: " + configuredStart);
                    return configuredStart;
                }
            } catch (Exception e) {
                System.err.println("Invalid NFL_SEASON_START_DATE format, using calculated date: " + e.getMessage());
            }
        }
        
        // Default calculation: Labor Day is the first Monday of September
        LocalDate septemberFirst = LocalDate.of(year, 9, 1);
        LocalDate laborDay = septemberFirst.with(java.time.temporal.TemporalAdjusters.firstInMonth(java.time.DayOfWeek.MONDAY));
        
        // NFL season starts the first Thursday after Labor Day
        LocalDate nflSeasonStart = laborDay.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.THURSDAY));
        
        // Special case: if Labor Day is already Thursday, season starts that day
        if (laborDay.getDayOfWeek() == java.time.DayOfWeek.THURSDAY) {
            nflSeasonStart = laborDay;
        }
        
        System.out.println("Calculated NFL season start date for " + year + ": " + nflSeasonStart);
        return nflSeasonStart;
    }
    
    /**
     * Calculate NFL week number based on game date and season start
     */
    private Integer calculateNflWeek(LocalDate gameDate, LocalDate seasonStart) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(seasonStart, gameDate);
        
        if (daysBetween < 0) {
            return null; // Before season
        }
        
        // NFL regular season is 18 weeks
        // Calculate week based on days since season start
        // Each week is approximately 7 days, but we'll be more flexible
        
        int week = (int) (daysBetween / 7) + 1;
        
        // Cap at 18 weeks for regular season
        if (week > 18) {
            return 18; // Week 18 or playoffs
        }
        
        // Ensure we don't return week 0 or negative
        return Math.max(1, week);
    }
    
    /**
     * Handle games before season start (preseason or previous season)
     */
    private Integer determinePreseasonOrPreviousSeasonWeek(LocalDate gameDate, int year) {
        // Check if it's preseason of the current year
        LocalDate preseasonStart = LocalDate.of(year, 8, 1); // August 1st
        if (gameDate.isAfter(preseasonStart)) {
            return 0; // Preseason
        }
        
        // Check if it's from the previous season's playoffs
        LocalDate previousYearSeasonStart = calculateNflSeasonStart(year - 1);
        LocalDate previousYearSeasonEnd = previousYearSeasonStart.plusDays(18 * 7); // Rough estimate
        
        if (gameDate.isAfter(previousYearSeasonEnd)) {
            return 19; // Playoffs from previous season
        }
        
        return null; // Unknown/too old
    }
    
    /**
     * Find a game that matches the odds response or create a new one
     */
    private Game findOrCreateGame(OddsApiResponse response, List<Game> weekGames, Integer week) {
        String homeTeam = normalizeTeamName(response.getHomeTeam());
        String awayTeam = normalizeTeamName(response.getAwayTeam());
        
        // First, try to find an existing game using multiple matching strategies
        Game existingGame = findExistingGame(response, weekGames, homeTeam, awayTeam);
        
        if (existingGame != null) {
            // Update existing game with current time if it has changed (for flexed games)
            updateGameTimeIfChanged(existingGame, response);
            return existingGame;
        }
        
        // Create new game if no match found
        return createNewGameFromResponse(response, week);
    }
    
    /**
     * Find existing game using multiple matching strategies to prevent duplicates
     */
    private Game findExistingGame(OddsApiResponse response, List<Game> weekGames, String normalizedHomeTeam, String normalizedAwayTeam) {
        Integer week = determineWeekFromGameTime(response.commence_time);
        if (week == null) {
            return null;
        }
        
        // Strategy 1: Database-level exact match check
        Optional<Game> exactMatch = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getHomeTeam(), response.getAwayTeam());
        if (exactMatch.isPresent()) {
            System.out.println("Found exact database match for " + response.getAwayTeam() + " @ " + response.getHomeTeam());
            return exactMatch.get();
        }
        
        // Strategy 2: Database-level reverse match check
        Optional<Game> reverseMatch = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getAwayTeam(), response.getHomeTeam());
        if (reverseMatch.isPresent()) {
            System.out.println("Found reverse database match for " + response.getAwayTeam() + " @ " + response.getHomeTeam());
            return reverseMatch.get();
        }
        
        // Strategy 3: Time-based match using database query
        try {
            Instant apiGameTime = Instant.parse(response.commence_time);
            // Create a 2-hour window around the API game time
            Instant startTime = apiGameTime.minus(java.time.Duration.ofHours(2));
            Instant endTime = apiGameTime.plus(java.time.Duration.ofHours(2));
            
            List<Game> similarGames = gameRepository.findSimilarGames(week, response.getHomeTeam(), response.getAwayTeam(), startTime, endTime);
            if (!similarGames.isEmpty()) {
                Game timeMatch = similarGames.get(0);
                System.out.println("Found time-based database match for " + response.getAwayTeam() + " @ " + response.getHomeTeam() + 
                    " (time difference: " + Math.abs(java.time.Duration.between(timeMatch.getKickoffTime(), apiGameTime).toHours()) + " hours)");
                return timeMatch;
            }
        } catch (Exception e) {
            System.err.println("Error in time-based database matching: " + e.getMessage());
        }
        
        // Strategy 4: In-memory fuzzy team name matching for common variations
        Game fuzzyMatch = findFuzzyTeamMatch(weekGames, normalizedHomeTeam, normalizedAwayTeam);
        if (fuzzyMatch != null) {
            System.out.println("Found fuzzy team name match for " + response.getAwayTeam() + " @ " + response.getHomeTeam());
            return fuzzyMatch;
        }
        
        return null;
    }
    
    /**
     * Find games using fuzzy team name matching for common variations
     */
    private Game findFuzzyTeamMatch(List<Game> weekGames, String normalizedHomeTeam, String normalizedAwayTeam) {
        return weekGames.stream()
            .filter(game -> {
                String gameHome = normalizeTeamName(game.getHomeTeam());
                String gameAway = normalizeTeamName(game.getAwayTeam());
                
                // Check for common team name variations
                return (fuzzyTeamMatch(gameHome, normalizedHomeTeam) && fuzzyTeamMatch(gameAway, normalizedAwayTeam)) ||
                       (fuzzyTeamMatch(gameHome, normalizedAwayTeam) && fuzzyTeamMatch(gameAway, normalizedHomeTeam));
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if two team names match with fuzzy logic for common variations
     */
    private boolean fuzzyTeamMatch(String team1, String team2) {
        if (team1.equals(team2)) return true;
        
        // Remove common suffixes and prefixes
        String clean1 = team1.replaceAll("\\b(football|team|club|fc)\\b", "").trim();
        String clean2 = team2.replaceAll("\\b(football|team|club|fc)\\b", "").trim();
        
        if (clean1.equals(clean2)) return true;
        
        // Check for city-only matches (e.g., "New York Giants" vs "New York")
        String city1 = extractCity(team1);
        String city2 = extractCity(team2);
        if (!city1.isEmpty() && !city2.isEmpty() && city1.equals(city2)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Extract city name from team name
     */
    private String extractCity(String teamName) {
        // Common patterns for NFL team names
        String[] parts = teamName.split("\\s+");
        if (parts.length >= 2) {
            // Return first part if it looks like a city
            String firstPart = parts[0].toLowerCase();
            if (firstPart.matches("(new|los|san|las|kansas|green|tampa|atlanta|buffalo|carolina|chicago|cleveland|dallas|denver|detroit|houston|indianapolis|jacksonville|miami|minnesota|new|oakland|philadelphia|pittsburgh|seattle|tennessee|washington)")) {
                return parts[0] + (parts.length > 2 ? " " + parts[1] : "");
            }
        }
        return "";
    }
    
    /**
     * Update game time if it has changed (for flexed games)
     */
    private void updateGameTimeIfChanged(Game game, OddsApiResponse response) {
        try {
            Instant newKickoffTime = Instant.parse(response.commence_time);
            if (!newKickoffTime.equals(game.getKickoffTime())) {
                System.out.println("Updating game time for " + game.getAwayTeam() + " @ " + game.getHomeTeam() + 
                    " from " + game.getKickoffTime() + " to " + newKickoffTime);
                game.setKickoffTime(newKickoffTime);
                gameRepository.save(game);
            }
        } catch (Exception e) {
            System.err.println("Error updating game time: " + e.getMessage());
        }
    }
    
    /**
     * Create a new game from the odds API response
     */
    private Game createNewGameFromResponse(OddsApiResponse response, Integer week) {
        try {
            // Double-check for duplicates before creating
            Optional<Game> existingGame = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getHomeTeam(), response.getAwayTeam());
            if (existingGame.isPresent()) {
                System.out.println("Game already exists, skipping creation: " + response.getAwayTeam() + " @ " + response.getHomeTeam());
                return existingGame.get();
            }
            
            // Check reverse order too
            existingGame = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getAwayTeam(), response.getHomeTeam());
            if (existingGame.isPresent()) {
                System.out.println("Game already exists (reverse order), skipping creation: " + response.getAwayTeam() + " @ " + response.getHomeTeam());
                return existingGame.get();
            }
            
            Game newGame = new Game();
            newGame.setWeek(week);
            newGame.setHomeTeam(response.getHomeTeam());
            newGame.setAwayTeam(response.getAwayTeam());
            newGame.setKickoffTime(Instant.parse(response.commence_time));
            newGame.setScored(false);
            newGame.setWinningTeam("");
            
            Game savedGame = gameRepository.save(newGame);
            System.out.println("Created new game: " + savedGame.getAwayTeam() + " @ " + savedGame.getHomeTeam() + 
                " for Week " + week + " at " + savedGame.getKickoffTime());
            
            return savedGame;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            System.err.println("Duplicate game detected by database constraint: " + response.getAwayTeam() + " @ " + response.getHomeTeam() + 
                " for Week " + week + ". Attempting to find existing game.");
            
            // Try to find the existing game that caused the constraint violation
            Optional<Game> existingGame = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getHomeTeam(), response.getAwayTeam());
            if (existingGame.isPresent()) {
                return existingGame.get();
            }
            
            existingGame = gameRepository.findByWeekAndHomeTeamAndAwayTeam(week, response.getAwayTeam(), response.getHomeTeam());
            if (existingGame.isPresent()) {
                return existingGame.get();
            }
            
            System.err.println("Could not find existing game after constraint violation");
            return null;
        } catch (Exception e) {
            System.err.println("Error creating new game from odds response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Normalize team names for comparison
     */
    private String normalizeTeamName(String teamName) {
        if (teamName == null) return "";
        return teamName.toLowerCase()
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Create BettingOdds from API response
     */
    private BettingOdds createBettingOddsFromResponse(OddsApiResponse response, Game game) {
        if (response.bookmakers == null || response.bookmakers.isEmpty()) {
            return null;
        }
        
        // Use the first bookmaker's odds
        Bookmaker bookmaker = response.bookmakers.get(0);
        if (bookmaker.markets == null || bookmaker.markets.isEmpty()) {
            return null;
        }
        
        BettingOdds odds = new BettingOdds(game, bookmaker.title);
        
        // Process spreads only (FanDuel point spreads)
        Optional<Market> spreadMarket = bookmaker.markets.stream()
            .filter(m -> "spreads".equals(m.key))
            .findFirst();
        
        if (spreadMarket.isPresent() && !spreadMarket.get().outcomes.isEmpty()) {
            Market.Outcome spreadOutcome = spreadMarket.get().outcomes.get(0);
            odds.setSpread(spreadOutcome.point); // Keep the original sign (+ or -)
            odds.setSpreadTeam(spreadOutcome.name);
            System.out.println("Created FanDuel spread odds for " + game.getAwayTeam() + " @ " + game.getHomeTeam() + 
                ": " + spreadOutcome.name + " " + spreadOutcome.point);
        } else {
            // No spreads found, return null
            System.out.println("No FanDuel spreads found for " + game.getAwayTeam() + " @ " + game.getHomeTeam());
            return null;
        }
        
        odds.setOddsType("american");
        odds.setLastUpdated(Instant.now());
        
        return odds;
    }
    
    /**
     * Save or update odds for a week
     */
    public List<BettingOdds> updateOddsForWeek(Integer week) {
        List<BettingOdds> fetchedOdds = fetchOddsForWeek(week);
        List<BettingOdds> savedOdds = new ArrayList<>();
        
        for (BettingOdds odds : fetchedOdds) {
            // Check if odds already exist for this game and sportsbook
            Optional<BettingOdds> existingOdds = bettingOddsRepository
                .findByGameAndSportsbook(odds.getGame(), odds.getSportsbook());
            
            if (existingOdds.isPresent()) {
                // Update existing odds
                BettingOdds existing = existingOdds.get();
                existing.setSpread(odds.getSpread());
                existing.setSpreadTeam(odds.getSpreadTeam());
                existing.setTotal(odds.getTotal());
                existing.setHomeTeamOdds(odds.getHomeTeamOdds());
                existing.setAwayTeamOdds(odds.getAwayTeamOdds());
                existing.setLastUpdated(Instant.now());
                savedOdds.add(bettingOddsRepository.save(existing));
            } else {
                // Save new odds
                savedOdds.add(bettingOddsRepository.save(odds));
            }
        }
        
        return savedOdds;
    }
    
    /**
     * Update all available odds and create/update games as needed
     */
    public List<BettingOdds> updateAllAvailableOdds() {
        List<BettingOdds> fetchedOdds = fetchAllAvailableOdds();
        List<BettingOdds> savedOdds = new ArrayList<>();
        
        for (BettingOdds odds : fetchedOdds) {
            // Check if odds already exist for this game and sportsbook
            Optional<BettingOdds> existingOdds = bettingOddsRepository
                .findByGameAndSportsbook(odds.getGame(), odds.getSportsbook());
            
        if (existingOdds.isPresent()) {
                // Update existing odds
            BettingOdds existing = existingOdds.get();
            existing.setSpread(odds.getSpread());
            existing.setSpreadTeam(odds.getSpreadTeam());
            existing.setTotal(odds.getTotal());
            existing.setHomeTeamOdds(odds.getHomeTeamOdds());
            existing.setAwayTeamOdds(odds.getAwayTeamOdds());
            existing.setLastUpdated(Instant.now());
                savedOdds.add(bettingOddsRepository.save(existing));
        } else {
                // Save new odds
                savedOdds.add(bettingOddsRepository.save(odds));
            }
        }
        
        return savedOdds;
    }
    
    /**
     * Get odds for a specific game
     */
    public List<BettingOdds> getOddsForGame(Long gameId) {
        return bettingOddsRepository.findByGameId(gameId);
    }
    
    /**
     * Get odds for a specific week
     */
    public List<BettingOdds> getOddsForWeek(Integer week) {
        return bettingOddsRepository.findByWeek(week);
    }
    
    /**
     * Check if odds need updating
     */
    public boolean needsUpdate(Integer week) {
        List<BettingOdds> existingOdds = bettingOddsRepository.findByWeek(week);
        if (existingOdds.isEmpty()) {
            return true;
        }
        
        Instant cutoffTime = Instant.now().minusSeconds(updateIntervalHours * 3600);
        return existingOdds.stream()
            .anyMatch(odds -> odds.getLastUpdated().isBefore(cutoffTime));
    }
    
    /**
     * Clean up stale odds
     */
    public void cleanupStaleOdds() {
        Instant cutoffTime = Instant.now().minusSeconds(updateIntervalHours * 24 * 3600); // 24 hours older than update interval
        List<BettingOdds> staleOdds = bettingOddsRepository.findStaleOdds(cutoffTime);
        bettingOddsRepository.deleteAll(staleOdds);
    }
    
    /**
     * Check if the odds API is properly configured
     */
    public boolean isApiConfigured() {
        return oddsApiKey != null && !oddsApiKey.trim().isEmpty();
    }
    
    // Inner classes for API response mapping
    public static class OddsApiResponse {
        public String id;
        public String sport_key;
        public String sport_title;
        public String commence_time;
        public String home_team;
        public String away_team;
        public List<Bookmaker> bookmakers;
        
        // Getters for normalized names
        public String getHomeTeam() { return home_team; }
        public String getAwayTeam() { return away_team; }
    }
    
    public static class Bookmaker {
        public String key;
        public String title;
        public List<Market> markets;
    }
    
    public static class Market {
        public String key;
        public List<Outcome> outcomes;
        
        public static class Outcome {
            public String name;
            public Double point;
            public Double price;
        }
    }
}
