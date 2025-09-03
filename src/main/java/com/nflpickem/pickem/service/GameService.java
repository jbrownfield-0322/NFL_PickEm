package com.nflpickem.pickem.service;

import com.nflpickem.pickem.dto.GameWithOddsDto;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
import com.nflpickem.pickem.repository.GameRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nflpickem.pickem.util.NflScheduleScraper;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final BettingOddsRepository oddsRepository;
    private final NflScheduleScraper nflScheduleScraper;
    
    @Value("${ENABLE_NFL_SCRAPING:false}")
    private boolean enableNflScraping;
    
    @Value("${NFL_SEASON_WEEKS:18}")
    private int nflSeasonWeeks;

    public GameService(GameRepository gameRepository, BettingOddsRepository oddsRepository, NflScheduleScraper nflScheduleScraper) {
        this.gameRepository = gameRepository;
        this.oddsRepository = oddsRepository;
        this.nflScheduleScraper = nflScheduleScraper;
    }

    @PostConstruct
    public void init() {
        if (gameRepository.count() == 0) {
            if (enableNflScraping) {
                try {
                    int currentYear = LocalDate.now().getYear();
                    // Scrape all regular season weeks (configurable via NFL_SEASON_WEEKS)
                    for (int week = 1; week <= nflSeasonWeeks; week++) {
                        List<Game> games = nflScheduleScraper.scrapeGames(currentYear, week);
                        gameRepository.saveAll(games);
                        System.out.println("Scraped and saved " + games.size() + " games for Week " + week + " of " + currentYear + " from Pro-Football-Reference.com");
                    }
                } catch (IOException e) {
                    System.err.println("Error scraping NFL schedule: " + e.getMessage());
                    System.err.println("This is expected in Railway deployment. You can manually add games via API or set ENABLE_NFL_SCRAPING=true");
                }
            } else {
                System.out.println("NFL schedule scraping is disabled. Set ENABLE_NFL_SCRAPING=true to enable automatic scraping.");
                System.out.println("You can manually add games via the API endpoints.");
            }
        }
    }

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public List<Game> getGamesByWeek(Integer week) {
        return gameRepository.findByWeek(week);
    }
    
    /**
     * Get all games with odds information
     */
    public List<GameWithOddsDto> getAllGamesWithOdds() {
        List<Game> games = gameRepository.findAll();
        return games.stream()
                .map(this::convertToGameWithOdds)
                .collect(Collectors.toList());
    }
    
    /**
     * Get games for a specific week with odds information
     */
    public List<GameWithOddsDto> getGamesByWeekWithOdds(Integer week) {
        List<Game> games = gameRepository.findByWeek(week);
        return games.stream()
                .map(this::convertToGameWithOdds)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert Game entity to GameWithOddsDto
     */
    private GameWithOddsDto convertToGameWithOdds(Game game) {
        GameWithOddsDto dto = new GameWithOddsDto(game);
        oddsRepository.findByGame(game).ifPresent(dto::setOdds);
        return dto;
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id).orElse(null);
    }

    public Game saveGame(Game game) {
        return gameRepository.save(game);
    }

    public Game updateGameScore(Long gameId, String winningTeam) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game != null) {
            game.setWinningTeam(winningTeam);
            game.setScored(true);
            return gameRepository.save(game);
        }
        return null;
    }

    public boolean deleteGame(Long gameId) {
        try {
            if (gameRepository.existsById(gameId)) {
                gameRepository.deleteById(gameId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error deleting game " + gameId + ": " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> cleanupDuplicateGames() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Game> allGames = gameRepository.findAll();
            Map<String, List<Game>> gameGroups = new HashMap<>();
            
            // Group games by week and team combination
            for (Game game : allGames) {
                String key = game.getWeek() + "-" + game.getAwayTeam() + "-" + game.getHomeTeam();
                gameGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(game);
            }
            
            List<Game> duplicatesToDelete = new ArrayList<>();
            
            // Find duplicates (keep first, mark others for deletion)
            for (List<Game> games : gameGroups.values()) {
                if (games.size() > 1) {
                    // Keep the first game, mark the rest as duplicates
                    duplicatesToDelete.addAll(games.subList(1, games.size()));
                }
            }
            
            if (duplicatesToDelete.isEmpty()) {
                result.put("success", true);
                result.put("message", "No duplicate games found");
                result.put("deletedCount", 0);
                result.put("totalGames", allGames.size());
                return result;
            }
            
            // Delete duplicates
            int deletedCount = 0;
            for (Game duplicate : duplicatesToDelete) {
                try {
                    gameRepository.deleteById(duplicate.getId());
                    deletedCount++;
                } catch (Exception e) {
                    System.err.println("Error deleting duplicate game " + duplicate.getId() + ": " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("message", "Successfully cleaned up duplicate games");
            result.put("deletedCount", deletedCount);
            result.put("totalDuplicatesFound", duplicatesToDelete.size());
            result.put("remainingGames", allGames.size() - deletedCount);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Error during cleanup: " + e.getMessage());
        }
        
        return result;
    }

    public Integer getCurrentWeek() {
        return calculateCurrentNflWeek(LocalDate.now().getYear());
    }

    private int calculateCurrentNflWeek(int year) {
        LocalDate today = LocalDate.now();
        LocalDate septemberFirst = LocalDate.of(year, 9, 1);
        LocalDate nflSeasonStart = septemberFirst.with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY));
        if (septemberFirst.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nflSeasonStart = septemberFirst;
        }
        if (today.isBefore(nflSeasonStart)) {
            return 1;
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(nflSeasonStart, today);
        return (int) (daysBetween / 7) + 1;
    }
} 