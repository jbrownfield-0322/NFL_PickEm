package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
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
import java.util.Optional;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final BettingOddsRepository bettingOddsRepository;
    private final NflScheduleScraper nflScheduleScraper;
    
    @Value("${ENABLE_NFL_SCRAPING:false}")
    private boolean enableNflScraping;
    
    @Value("${NFL_SEASON_WEEKS:18}")
    private int nflSeasonWeeks;

    public GameService(GameRepository gameRepository, BettingOddsRepository bettingOddsRepository, NflScheduleScraper nflScheduleScraper) {
        this.gameRepository = gameRepository;
        this.bettingOddsRepository = bettingOddsRepository;
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
     * Check if a game has odds
     */
    public boolean gameHasOdds(Long gameId) {
        return !bettingOddsRepository.findByGameId(gameId).isEmpty();
    }
    
    /**
     * Get the primary odds for a game (first available odds)
     */
    public Optional<BettingOdds> getPrimaryOddsForGame(Long gameId) {
        List<BettingOdds> odds = bettingOddsRepository.findByGameId(gameId);
        return odds.isEmpty() ? Optional.empty() : Optional.of(odds.get(0));
    }
} 