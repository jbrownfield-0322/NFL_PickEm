package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.repository.BettingOddsRepository;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.repository.PickRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nflpickem.pickem.util.NflScheduleScraper;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final BettingOddsRepository bettingOddsRepository;
    private final PickRepository pickRepository;
    private final NflScheduleScraper nflScheduleScraper;
    private final ScoringService scoringService;
    private final SeasonService seasonService;
    
    @Value("${ENABLE_NFL_SCRAPING:false}")
    private boolean enableNflScraping;
    
    @Value("${NFL_SEASON_WEEKS:18}")
    private int nflSeasonWeeks;

    public GameService(GameRepository gameRepository, BettingOddsRepository bettingOddsRepository,
                       PickRepository pickRepository, NflScheduleScraper nflScheduleScraper,
                       @Lazy ScoringService scoringService, SeasonService seasonService) {
        this.gameRepository = gameRepository;
        this.bettingOddsRepository = bettingOddsRepository;
        this.pickRepository = pickRepository;
        this.nflScheduleScraper = nflScheduleScraper;
        this.scoringService = scoringService;
        this.seasonService = seasonService;
    }

    @PostConstruct
    public void init() {
        if (gameRepository.count() == 0) {
            if (enableNflScraping) {
                try {
                    int seasonYear = seasonService.getCurrentSeasonYear();
                    for (int week = 1; week <= nflSeasonWeeks; week++) {
                        List<Game> games = nflScheduleScraper.scrapeGames(seasonYear, week);
                        gameRepository.saveAll(games);
                        System.out.println("Scraped and saved " + games.size() + " games for Week " + week + " of " + seasonYear);
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

    public List<Game> getGamesBySeason(Integer seasonYear) {
        return gameRepository.findBySeasonYear(seasonService.normalizeSeasonYear(seasonYear));
    }

    public List<Game> getGamesByWeek(Integer week) {
        return getGamesByWeek(week, null);
    }

    public List<Game> getGamesByWeek(Integer week, Integer seasonYear) {
        return gameRepository.findBySeasonYearAndWeek(seasonService.normalizeSeasonYear(seasonYear), week);
    }

    public List<Integer> getAvailableSeasonYears() {
        return seasonService.getAvailableSeasonYears();
    }

    public int getCurrentSeasonYear() {
        return seasonService.getCurrentSeasonYear();
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id).orElse(null);
    }

    public Game saveGame(Game game) {
        if (game.getSeasonYear() == null) {
            game.setSeasonYear(seasonService.resolveSeasonYear(game.getKickoffTime()));
        }
        return gameRepository.save(game);
    }

    public Game updateGameScore(Long gameId, String winningTeam) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game != null) {
            game.setWinningTeam(winningTeam);
            game.setScored(true);
            Game saved = gameRepository.save(game);
            scoringService.gradePicksForGame(saved);
            return saved;
        }
        return null;
    }

    @Transactional
    public boolean deleteGame(Long gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            return false;
        }
        pickRepository.deleteByGame(game);
        bettingOddsRepository.deleteByGame(game);
        gameRepository.delete(game);
        return true;
    }

    @Transactional
    public List<String> removeGamesOutsideRegularSeasonWeek(Integer week, Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        List<Game> weekGames = gameRepository.findBySeasonYearAndWeek(season, week);
        if (weekGames.isEmpty()) {
            return List.of();
        }

        Instant earliestKickoff = weekGames.stream()
                .map(Game::getKickoffTime)
                .filter(t -> t != null)
                .min(Instant::compareTo)
                .orElse(null);
        if (earliestKickoff == null) {
            return List.of();
        }

        LocalDate earliestDate = earliestKickoff.atZone(ZoneId.of("America/New_York")).toLocalDate();
        LocalDate weekStart = earliestDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Instant cutoff = weekEnd.plusDays(1).atStartOfDay(ZoneId.of("America/New_York")).toInstant();

        List<String> deleted = new ArrayList<>();
        for (Game game : weekGames) {
            if (game.getKickoffTime() != null && !game.getKickoffTime().isBefore(cutoff)) {
                String label = game.getId() + ": " + game.getAwayTeam() + " @ " + game.getHomeTeam()
                        + " (" + game.getKickoffTime() + ")";
                pickRepository.deleteByGame(game);
                bettingOddsRepository.deleteByGame(game);
                gameRepository.delete(game);
                deleted.add(label);
            }
        }
        return deleted;
    }

    public Integer getCurrentWeek() {
        return getCurrentWeek(null);
    }

    public Integer getCurrentWeek(Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        return calculateCurrentNflWeek(season);
    }

    private int calculateCurrentNflWeek(int seasonYear) {
        LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
        LocalDate septemberFirst = LocalDate.of(seasonYear, 9, 1);
        LocalDate nflSeasonStart = septemberFirst.with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY));
        if (septemberFirst.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nflSeasonStart = septemberFirst;
        }
        if (today.isBefore(nflSeasonStart)) {
            return 1;
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(nflSeasonStart, today);
        int week = (int) (daysBetween / 7) + 1;
        return Math.min(week, nflSeasonWeeks);
    }
    
    public List<BettingOdds> getOddsForGame(Long gameId) {
        return bettingOddsRepository.findByGameId(gameId);
    }
    
    public List<BettingOdds> getOddsForWeek(Integer week) {
        return getOddsForWeek(week, null);
    }

    public List<BettingOdds> getOddsForWeek(Integer week, Integer seasonYear) {
        return bettingOddsRepository.findBySeasonYearAndWeek(seasonService.normalizeSeasonYear(seasonYear), week);
    }
    
    public boolean gameHasOdds(Long gameId) {
        return !bettingOddsRepository.findByGameId(gameId).isEmpty();
    }
    
    public Optional<BettingOdds> getPrimaryOddsForGame(Long gameId) {
        List<BettingOdds> odds = bettingOddsRepository.findByGameId(gameId);
        return odds.isEmpty() ? Optional.empty() : Optional.of(odds.get(0));
    }
} 