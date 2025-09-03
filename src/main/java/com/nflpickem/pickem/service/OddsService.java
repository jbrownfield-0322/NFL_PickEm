package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
import com.nflpickem.pickem.repository.GameRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

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
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB max
            .build();
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
            
            // Use absolute URL to avoid baseUrl configuration issues
            String fullUrl = baseUrl + "/v4/sports/americanfootball_nfl/odds?apiKey=" + apiKey + "&regions=us&markets=spreads,totals&oddsFormat=american";
            System.out.println("Full request URL: " + fullUrl);
            
            Mono<Object[]> response = webClient.get()
                    .uri(fullUrl)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                System.err.println("API Error: " + clientResponse.statusCode() + " - " + clientResponse.statusCode());
                                System.err.println("Response Headers: " + clientResponse.headers());
                                // Log individual headers for better debugging
                                clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                                    System.err.println("Header: " + key + " = " + values);
                                });
                                
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
            // Make a simple test request to /v4/sports using absolute URL
            String fullUrl = baseUrl + "/v4/sports?apiKey=" + apiKey;
            System.out.println("Testing API connection with absolute URL: " + fullUrl);
            
            Mono<Object[]> response = webClient.get()
                    .uri(fullUrl)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                String errorMsg = "Test API Error: " + clientResponse.statusCode() + " - " + clientResponse.statusCode();
                                System.err.println(errorMsg);
                                System.err.println("Response Headers:");
                                clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                                    System.err.println("Header: " + key + " = " + values);
                                });
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            System.err.println("Error body: " + body);
                                            return Mono.error(new RuntimeException(errorMsg + " - " + body));
                                        });
                            })
                    .bodyToMono(Object[].class);
            
            System.out.println("Sending request...");
            Object[] result = response.block();
            System.out.println("Response received: " + (result != null ? "YES" : "NO"));
            
            if (result != null) {
                return "API connection successful! Available sports: " + result.length;
            } else {
                return "API connection failed - no response";
            }
            
        } catch (Exception e) {
            System.err.println("Exception during API test: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return "API connection test failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
    
    /**
     * Test the exact same request that works in PowerShell
     */
    public String testExactPowerShellRequest() {
        if (!isApiConfigured()) {
            return "API not configured";
        }
        
        try {
            // Test the exact same endpoint that works in PowerShell using absolute URL
            String fullUrl = baseUrl + "/v4/sports/americanfootball_nfl/odds?apiKey=" + apiKey + "&regions=us&markets=spreads,totals&oddsFormat=american";
            System.out.println("Testing exact PowerShell request with absolute URL: " + fullUrl);
            
            Mono<Object[]> response = webClient.get()
                    .uri(fullUrl)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                String errorMsg = "PowerShell Test Error: " + clientResponse.statusCode() + " - " + clientResponse.statusCode();
                                System.err.println(errorMsg);
                                System.err.println("Response Headers:");
                                clientResponse.headers().asHttpHeaders().forEach((key, values) -> {
                                    System.err.println("Header: " + key + " = " + values);
                                });
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            System.err.println("Error body: " + body);
                                            return Mono.error(new RuntimeException(errorMsg + " - " + body));
                                        });
                            })
                    .bodyToMono(Object[].class);
            
            Object[] result = response.block();
            if (result != null) {
                return "PowerShell request successful! Odds data received: " + result.length + " games";
            } else {
                return "PowerShell request failed - no response";
            }
            
        } catch (Exception e) {
            return "PowerShell request test failed: " + e.getMessage();
        }
    }
    
    /**
     * Test basic HTTP connectivity to The Odds API
     */
    public String testBasicConnectivity() {
        if (!isApiConfigured()) {
            return "API not configured";
        }
        
        try {
            System.out.println("=== BASIC CONNECTIVITY TEST ===");
            System.out.println("Testing connection to: " + baseUrl);
            System.out.println("API Key length: " + (apiKey != null ? apiKey.length() : "NULL"));
            System.out.println("API Key starts with: " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "NULL"));
            
            // Test with a simple ping-like request using absolute URL
            String fullUrl = baseUrl + "/v4/sports?apiKey=" + apiKey;
            System.out.println("Testing with absolute URL: " + fullUrl);
            
            Mono<Object[]> response = webClient.get()
                    .uri(fullUrl)
                    .header("User-Agent", "NFL-Pickem-App/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(Object[].class);
            
            System.out.println("Sending request...");
            Object[] result = response.block();
            System.out.println("Response received: " + (result != null ? "YES" : "NO"));
            
            if (result != null) {
                return "Basic connectivity successful! Response size: " + result.length;
            } else {
                return "Basic connectivity failed - no response";
            }
            
        } catch (Exception e) {
            System.err.println("Connectivity test exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return "Connectivity test failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
    
    /**
     * Get detailed configuration information for debugging
     */
    public Map<String, Object> getDetailedConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKeyConfigured", apiKey != null && !apiKey.isEmpty());
        config.put("apiKeyLength", apiKey != null ? apiKey.length() : 0);
        config.put("apiKeyStartsWith", apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "NULL");
        config.put("baseUrl", baseUrl);
        config.put("webClientConfigured", webClient != null);
        
        // Check environment variables directly
        try {
            String envApiKey = System.getenv("THEODDS_API_KEY");
            config.put("envApiKey", envApiKey != null ? envApiKey.substring(0, Math.min(8, envApiKey.length())) + "..." : "NULL");
            config.put("envApiKeyLength", envApiKey != null ? envApiKey.length() : 0);
        } catch (Exception e) {
            config.put("envApiKeyError", e.getMessage());
        }
        
        return config;
    }
    
    /**
     * Scheduled task to update odds every 4 hours for the current NFL week
     * Runs at: 00:00, 04:00, 08:00, 12:00, 16:00, 20:00 UTC
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledOddsUpdate() {
        try {
            System.out.println("=== SCHEDULED ODDS UPDATE STARTED ===");
            System.out.println("Timestamp: " + Instant.now());
            
            // Get the current NFL week (will always return a valid week now)
            Integer currentWeek = getCurrentNflWeek();
            System.out.println("Updating odds for NFL Week " + currentWeek);
            
            // Fetch odds for the current week
            fetchOddsForWeek(currentWeek);
            
            System.out.println("=== SCHEDULED ODDS UPDATE COMPLETED ===");
            System.out.println("Next update in 4 hours");
            
        } catch (Exception e) {
            System.err.println("Error during scheduled odds update: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Determine the current NFL week based on the current date
     * This is a simplified calculation - you may want to enhance this
     * based on your specific NFL schedule requirements
     */
    private Integer getCurrentNflWeek() {
        try {
            // Get current date
            java.time.LocalDate today = java.time.LocalDate.now();
            
            // NFL season typically starts in early September
            // This is a simplified calculation - you may need to adjust based on actual NFL calendar
            int currentYear = today.getYear();
            int currentMonth = today.getMonthValue();
            int currentDay = today.getDayOfMonth();
            
            // If we're in the offseason (before September), return Week 1 for preseason odds
            if (currentMonth < 9) {
                System.out.println("NFL season hasn't started yet (current month: " + currentMonth + ") - returning Week 1 for preseason odds");
                return 1;
            }
            
            // Calculate week based on days since September 1st
            // NFL Week 1 typically starts around September 5-10
            java.time.LocalDate seasonStart = java.time.LocalDate.of(currentYear, 9, 5);
            long daysSinceSeasonStart = java.time.temporal.ChronoUnit.DAYS.between(seasonStart, today);
            
            if (daysSinceSeasonStart < 0) {
                System.out.println("NFL season hasn't started yet - returning Week 1 for preseason odds");
                return 1;
            }
            
            // Calculate week (7 days per week, starting from week 1)
            int week = (int) (daysSinceSeasonStart / 7) + 1;
            
            // NFL regular season is typically 18 weeks
            if (week > 18) {
                System.out.println("NFL regular season is over (calculated week: " + week + ") - returning Week 1 for next season");
                return 1;
            }
            
            System.out.println("Current NFL Week calculated: " + week);
            return week;
            
        } catch (Exception e) {
            System.err.println("Error calculating current NFL week: " + e.getMessage());
            return 1; // Fallback to Week 1
        }
    }
    
    /**
     * Manual trigger for odds update (useful for testing)
     */
    public String triggerManualOddsUpdate() {
        try {
            System.out.println("=== MANUAL ODDS UPDATE TRIGGERED ===");
            scheduledOddsUpdate();
            return "Manual odds update completed successfully";
        } catch (Exception e) {
            return "Manual odds update failed: " + e.getMessage();
        }
    }
}
