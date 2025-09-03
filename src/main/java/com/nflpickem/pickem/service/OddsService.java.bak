package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
import com.nflpickem.pickem.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OddsService {
    
    @Value("${theodds.api.key}")
    private String apiKey;
    
    @Value("${theodds.api.base-url:https://api.the-odds-api.com}")
    private String baseUrl;
    
    private final WebClient webClient;
    private final BettingOddsRepository oddsRepository;
    private final GameRepository gameRepository;
    
    // Track last update time for monitoring
    private Instant lastUpdateTime = Instant.now();
    private int totalUpdates = 0;
    
    public OddsService(BettingOddsRepository oddsRepository, GameRepository gameRepository) {
        this.oddsRepository = oddsRepository;
        this.gameRepository = gameRepository;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    
    /**
     * Fetch odds for all games in a specific week
     */
    public void fetchOddsForWeek(Integer week) {
        try {
            List<Game> games = gameRepository.findByWeek(week);
            for (Game game : games) {
                fetchOddsForGame(game);
                // Add delay to respect API rate limits
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("Error fetching odds for week {}: {}", week, e.getMessage());
        }
    }
    
    /**
     * Fetch odds for a specific game
     */
    public void fetchOddsForGame(Game game) {
        try {
            String url = String.format("/v4/sports/americanfootball_nfl/odds?apiKey=%s&regions=us&markets=spreads,totals&oddsFormat=american", apiKey);
            
            Mono<Object[]> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object[].class);
            
            response.subscribe(
                oddsData -> processOddsResponse(game, oddsData),
                error -> log.error("Error fetching odds for game {}: {}", game.getId(), error.getMessage())
            );
            
        } catch (Exception e) {
            log.error("Error fetching odds for game {}: {}", game.getId(), e.getMessage());
        }
    }
    
    /**
     * Process the odds response from The Odds API
     */
    @SuppressWarnings("unchecked")
    private void processOddsResponse(Game game, Object[] oddsData) {
        try {
            for (Object oddsObj : oddsData) {
                if (oddsObj instanceof Map) {
                    Map<String, Object> odds = (Map<String, Object>) oddsObj;
                    
                    // Check if this is the right game by comparing team names
                    String homeTeam = (String) odds.get("home_team");
                    String awayTeam = (String) odds.get("away_team");
                    
                    if (matchesGame(game, homeTeam, awayTeam)) {
                        List<Map<String, Object>> bookmakers = (List<Map<String, Object>>) odds.get("bookmakers");
                        if (bookmakers != null && !bookmakers.isEmpty()) {
                            // Use the first bookmaker (usually the most popular)
                            Map<String, Object> bookmaker = bookmakers.get(0);
                            String sportsbook = (String) bookmaker.get("title");
                            
                            List<Map<String, Object>> markets = (List<Map<String, Object>>) bookmaker.get("markets");
                            if (markets != null) {
                                processMarkets(game, sportsbook, markets);
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing odds response for game {}: {}", game.getId(), e.getMessage());
        }
    }
    
    /**
     * Process the markets (spreads, totals) from the odds response
     */
    @SuppressWarnings("unchecked")
    private void processMarkets(Game game, String sportsbook, List<Map<String, Object>> markets) {
        Double spread = null;
        String spreadTeam = null;
        Double total = null;
        Double homeTeamOdds = null;
        Double awayTeamOdds = null;
        
        for (Map<String, Object> market : markets) {
            String marketKey = (String) market.get("key");
            List<Map<String, Object>> outcomes = (List<Map<String, Object>>) market.get("outcomes");
            
            if ("spreads".equals(marketKey) && outcomes != null && outcomes.size() >= 2) {
                // Process spread
                Map<String, Object> homeOutcome = outcomes.get(0);
                Map<String, Object> awayOutcome = outcomes.get(1);
                
                Double homeSpread = parseDouble(homeOutcome.get("point"));
                Double awaySpread = parseDouble(awayOutcome.get("point"));
                
                if (homeSpread != null && awaySpread != null) {
                    // The negative spread indicates the favorite
                    if (homeSpread < 0) {
                        spread = Math.abs(homeSpread);
                        spreadTeam = game.getHomeTeam();
                    } else {
                        spread = Math.abs(awaySpread);
                        spreadTeam = game.getAwayTeam();
                    }
                }
                
                homeTeamOdds = parseDouble(homeOutcome.get("price"));
                awayTeamOdds = parseDouble(awayOutcome.get("price"));
                
            } else if ("totals".equals(marketKey) && outcomes != null && outcomes.size() >= 2) {
                // Process total
                Map<String, Object> overOutcome = outcomes.get(0);
                total = parseDouble(overOutcome.get("point"));
            }
        }
        
        // Save or update the odds
        if (spread != null || total != null) {
            saveOrUpdateOdds(game, sportsbook, homeTeamOdds, awayTeamOdds, spread, spreadTeam, total);
        }
    }
    
    /**
     * Check if the odds data matches the game
     */
    private boolean matchesGame(Game game, String homeTeam, String awayTeam) {
        return game.getHomeTeam().equalsIgnoreCase(homeTeam) && 
               game.getAwayTeam().equalsIgnoreCase(awayTeam);
    }
    
    /**
     * Parse double value safely
     */
    private Double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Save or update betting odds for a game
     */
    private void saveOrUpdateOdds(Game game, String sportsbook, Double homeTeamOdds, 
                                 Double awayTeamOdds, Double spread, String spreadTeam, Double total) {
        Optional<BettingOdds> existingOdds = oddsRepository.findByGame(game);
        
        BettingOdds odds;
        if (existingOdds.isPresent()) {
            odds = existingOdds.get();
        } else {
            odds = new BettingOdds();
            odds.setGame(game);
        }
        
        odds.setSportsbook(sportsbook);
        odds.setHomeTeamOdds(homeTeamOdds);
        odds.setAwayTeamOdds(awayTeamOdds);
        odds.setSpread(spread);
        odds.setSpreadTeam(spreadTeam);
        odds.setTotal(total);
        odds.setOddsType("american");
        odds.setLastUpdated(Instant.now());
        
        oddsRepository.save(odds);
        lastUpdateTime = Instant.now();
        totalUpdates++;
        
        log.info("Saved odds for game {}: {} @ {} (Spread: {} {}, Total: {})", 
                game.getId(), game.getAwayTeam(), game.getHomeTeam(), spread, spreadTeam, total);
    }
    
    /**
     * Get odds for a specific game
     */
    public Optional<BettingOdds> getOddsForGame(Game game) {
        return oddsRepository.findByGame(game);
    }
    
    /**
     * Get odds for all games in a week
     */
    public List<BettingOdds> getOddsForWeek(Integer week) {
        return oddsRepository.findByGameWeek(week);
    }
    
    /**
     * Get last update time for monitoring
     */
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * Get total number of updates performed
     */
    public int getTotalUpdates() {
        return totalUpdates;
    }
    
    /**
     * Get odds update statistics
     */
    public String getUpdateStats() {
        return String.format("Last update: %s, Total updates: %d", 
                lastUpdateTime.toString(), totalUpdates);
    }
}
