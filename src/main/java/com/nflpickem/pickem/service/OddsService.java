package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
import com.nflpickem.pickem.repository.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OddsService {
    
    private final WebClient webClient;
    private final GameRepository gameRepository;
    private final BettingOddsRepository oddsRepository;
    
    @Value("${theodds.api.key}")
    private String apiKey;
    
    @Value("${theodds.api.base-url:https://api.the-odds-api.com}")
    private String baseUrl;
    
    private Instant lastUpdateTime;
    private int totalUpdates = 0;
    
    public OddsService(WebClient.Builder webClientBuilder, 
                      GameRepository gameRepository, 
                      BettingOddsRepository oddsRepository) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.gameRepository = gameRepository;
        this.oddsRepository = oddsRepository;
    }
    
    public void fetchOddsForWeek(Integer week) {
        try {
            List<Game> games = gameRepository.findByWeek(week);
            if (games.isEmpty()) {
                System.out.println("No games found for week " + week);
                return;
            }
            
            // Validate API key
            if (apiKey == null || apiKey.isEmpty() || "your_api_key_here".equals(apiKey)) {
                System.err.println("Cannot fetch odds: Invalid or missing API key. Please set THEODDS_API_KEY environment variable.");
                System.err.println("Visit https://the-odds-api.com/ to get a free API key.");
                throw new RuntimeException("Invalid or missing API key. Please set THEODDS_API_KEY environment variable.");
            }
            
            // Enhanced debugging for API key issues
            System.out.println("=== ODDS API DEBUG INFO ===");
            System.out.println("API Key length: " + (apiKey != null ? apiKey.length() : "NULL"));
            System.out.println("API Key starts with: " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "NULL"));
            System.out.println("Base URL: " + baseUrl);
            System.out.println("Full request URL: " + String.format("/v4/sports/americanfootball_nfl/odds?apiKey=%s&regions=us&markets=spreads,totals&oddsFormat=american", apiKey.substring(0, Math.min(8, apiKey.length())) + "..."));
            System.out.println("================================");
            
            System.out.println("Fetching odds for week " + week + " using API key: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
            
            String url = String.format("/v4/sports/americanfootball_nfl/odds?apiKey=%s&regions=us&markets=spreads,totals&oddsFormat=american", apiKey);
            
            Mono<Object[]> response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                System.err.println("API Error: " + clientResponse.statusCode() + " - " + clientResponse.statusCode());
                                System.err.println("Response Headers: " + clientResponse.headers());
                                
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            String errorMsg = "API Error: " + clientResponse.statusCode() + " - " + body;
                                            System.err.println(errorMsg);
                                            
                                            // Provide helpful error messages for common issues
                                            if (clientResponse.statusCode().value() == 403) {
                                                errorMsg += "\nThis usually means:";
                                                errorMsg += "\n1. Invalid API key";
                                                errorMsg += "\n2. API key has expired";
                                                errorMsg += "\n3. API key has reached its request limit";
                                                errorMsg += "\n4. Request headers are being rejected";
                                                errorMsg += "\n5. IP address restrictions";
                                                errorMsg += "\nPlease check your API key at https://the-odds-api.com/";
                                                
                                                // Log additional debugging info
                                                System.err.println("=== 403 ERROR DEBUG INFO ===");
                                                System.err.println("API Key used: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
                                                System.err.println("Request URL: " + url);
                                                System.err.println("User-Agent: NFL-Pickem-App/1.0");
                                                System.err.println("=====================================");
                                            }
                                            
                                            return Mono.error(new RuntimeException(errorMsg));
                                        });
                            })
                    .bodyToMono(Object[].class);
            
            Object[] oddsData = response.block();
            if (oddsData != null) {
                processOddsResponse(oddsData, games);
                updateStats();
                System.out.println("Successfully fetched odds for week " + week);
            }
            
            // Rate limiting - The Odds API allows 500 requests per month
            // Add a longer delay to avoid triggering rate limiting
            Thread.sleep(2000); // 2 second delay between requests
            
        } catch (Exception e) {
            System.err.println("Error fetching odds for week " + week + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch odds: " + e.getMessage(), e);
        }
    }
    
    public void fetchOddsForGame(Long gameId) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isPresent()) {
            fetchOddsForWeek(gameOpt.get().getWeek());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void processOddsResponse(Object[] oddsData, List<Game> games) {
        for (Object oddsObj : oddsData) {
            if (oddsObj instanceof Map) {
                Map<String, Object> oddsMap = (Map<String, Object>) oddsObj;
                
                String homeTeam = (String) oddsMap.get("home_team");
                String awayTeam = (String) oddsMap.get("away_team");
                
                // Find matching game
                Optional<Game> matchingGame = games.stream()
                        .filter(game -> matchesTeamNames(game, homeTeam, awayTeam))
                        .findFirst();
                
                if (matchingGame.isPresent()) {
                    Game game = matchingGame.get();
                    processGameOdds(game, oddsMap);
                }
            }
        }
    }
    
    private boolean matchesTeamNames(Game game, String homeTeam, String awayTeam) {
        return (game.getHomeTeam().equalsIgnoreCase(homeTeam) && 
                game.getAwayTeam().equalsIgnoreCase(awayTeam)) ||
               (game.getHomeTeam().equalsIgnoreCase(awayTeam) && 
                game.getAwayTeam().equalsIgnoreCase(homeTeam));
    }
    
    @SuppressWarnings("unchecked")
    private void processGameOdds(Game game, Map<String, Object> oddsMap) {
        try {
            List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) oddsMap.get("bookmakers");
            if (bookmakers != null && !bookmakers.isEmpty()) {
                // Use the first bookmaker for now
                Map<String, Object> bookmaker = bookmakers.get(0);
                String sportsbook = (String) bookmaker.get("title");
                
                List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmaker.get("markets");
                if (markets != null) {
                    Double spread = null;
                    String spreadTeam = null;
                    Double total = null;
                    Double homeOdds = null;
                    Double awayOdds = null;
                    
                    for (Map<String, Object> market : markets) {
                        String marketKey = (String) market.get("key");
                        List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
                        
                        if ("spreads".equals(marketKey) && outcomes != null && outcomes.size() >= 2) {
                            // Process spread
                            Map<String, Object> homeOutcome = outcomes.get(0);
                            Map<String, Object> awayOutcome = outcomes.get(1);
                            
                            if (homeOutcome.get("name").equals(game.getHomeTeam())) {
                                spread = Math.abs(((Number) homeOutcome.get("point")).doubleValue());
                                spreadTeam = ((Number) homeOutcome.get("point")).doubleValue() > 0 ? game.getHomeTeam() : game.getAwayTeam();
                            } else {
                                spread = Math.abs(((Number) awayOutcome.get("point")).doubleValue());
                                spreadTeam = ((Number) awayOutcome.get("point")).doubleValue() > 0 ? game.getAwayTeam() : game.getHomeTeam();
                            }
                        } else if ("totals".equals(marketKey) && outcomes != null && outcomes.size() >= 2) {
                            // Process total
                            total = ((Number) outcomes.get(0).get("point")).doubleValue();
                        }
                    }
                    
                    // Create and save odds
                    BettingOdds odds = new BettingOdds(game, sportsbook, homeOdds, awayOdds, spread, spreadTeam, total, "american");
                    saveOrUpdateOdds(odds);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing odds for game " + game.getId() + ": " + e.getMessage());
        }
    }
    
    private void saveOrUpdateOdds(BettingOdds odds) {
        Optional<BettingOdds> existingOdds = oddsRepository.findByGame(odds.getGame());
        if (existingOdds.isPresent()) {
            BettingOdds existing = existingOdds.get();
            existing.setSpread(odds.getSpread());
            existing.setSpreadTeam(odds.getSpreadTeam());
            existing.setTotal(odds.getTotal());
            existing.setHomeTeamOdds(odds.getHomeTeamOdds());
            existing.setAwayTeamOdds(odds.getAwayTeamOdds());
            existing.setLastUpdated(Instant.now());
            oddsRepository.save(existing);
        } else {
            oddsRepository.save(odds);
        }
    }
    
    private void updateStats() {
        this.lastUpdateTime = Instant.now();
        this.totalUpdates++;
    }
    
    // Getters for monitoring
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public int getTotalUpdates() {
        return totalUpdates;
    }
    
    /**
     * Check if the odds API is properly configured
     */
    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !"your_api_key_here".equals(apiKey);
    }
    
    /**
     * Get API configuration status for debugging
     */
    public String getApiStatus() {
        if (!isApiConfigured()) {
            return "NOT_CONFIGURED - Please set THEODDS_API_KEY environment variable";
        }
        return "CONFIGURED - API key: " + apiKey.substring(0, Math.min(8, apiKey.length())) + "...";
    }
    
    /**
     * Get API key for testing purposes (first 8 characters)
     */
    public String getApiKeyForTesting() {
        if (!isApiConfigured()) {
            return "NOT_CONFIGURED";
        }
        return apiKey.substring(0, Math.min(8, apiKey.length())) + "...";
    }
    
    /**
     * Test the API connection with a simple request
     */
    public String testApiConnection() {
        if (!isApiConfigured()) {
            return "API not configured";
        }
        
        try {
            // Make a simple test request to /v4/sports (which should work with any valid key)
            String testUrl = "/v4/sports?apiKey=" + apiKey;
            System.out.println("Testing API connection with URL: " + testUrl);
            
            Mono<Object[]> response = webClient.get()
                    .uri(testUrl)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                String errorMsg = "Test API Error: " + clientResponse.statusCode() + " - " + clientResponse.statusCode();
                                System.err.println(errorMsg);
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            System.err.println("Error body: " + body);
                                            return Mono.error(new RuntimeException(errorMsg + " - " + body));
                                        });
                            })
                    .bodyToMono(Object[].class);
            
            Object[] result = response.block();
            if (result != null) {
                return "API connection successful! Available sports: " + result.length;
            } else {
                return "API connection failed - no response";
            }
            
        } catch (Exception e) {
            return "API connection test failed: " + e.getMessage();
        }
    }
}
