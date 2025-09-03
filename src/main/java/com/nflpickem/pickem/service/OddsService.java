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
            
            String url = String.format("/v4/sports/americanfootball_nfl/odds?apiKey=%s&regions=us&markets=spreads,totals&oddsFormat=american", apiKey);
            
            Mono<Object[]> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object[].class);
            
            Object[] oddsData = response.block();
            if (oddsData != null) {
                processOddsResponse(oddsData, games);
                updateStats();
            }
            
            // Rate limiting - The Odds API allows 500 requests per month
            Thread.sleep(1000); // 1 second delay between requests
            
        } catch (Exception e) {
            System.err.println("Error fetching odds for week " + week + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch odds", e);
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
}
