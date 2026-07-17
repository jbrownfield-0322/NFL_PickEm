package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.dto.PlayerScore;
import com.nflpickem.pickem.dto.WeeklyWinsDto;
import com.nflpickem.pickem.repository.PickRepository;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.repository.LeagueRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class LeaderboardService {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardService.class);
    
    private final PickRepository pickRepository;
    private final GameRepository gameRepository;
    private final LeagueRepository leagueRepository;
    private final SeasonService seasonService;

    public LeaderboardService(PickRepository pickRepository, GameRepository gameRepository,
                              LeagueRepository leagueRepository, SeasonService seasonService) {
        this.pickRepository = pickRepository;
        this.gameRepository = gameRepository;
        this.leagueRepository = leagueRepository;
        this.seasonService = seasonService;
    }

    public List<PlayerScore> getWeeklyLeaderboard(Integer week, Long leagueId, Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        logger.info("Getting weekly leaderboard for season: {}, week: {}, leagueId: {}", season, week, leagueId);
        
        try {
            List<Game> gamesInWeek = gameRepository.findBySeasonYearAndWeek(season, week);
            logger.info("Found {} games for season {} week {}", gamesInWeek.size(), season, week);
            
            Set<Long> gameIdsInWeek = gamesInWeek.stream()
                    .map(Game::getId)
                    .collect(Collectors.toSet());
            
            List<Pick> picksToScore;

            if (leagueId != null) {
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
                picksToScore = pickRepository.findByLeague(league).stream()
                        .filter(pick -> gameIdsInWeek.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
            } else {
                picksToScore = pickRepository.findAll().stream()
                        .filter(pick -> gameIdsInWeek.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
            }
            
            return calculateLeaderboard(picksToScore);
        } catch (Exception e) {
            logger.error("Error getting weekly leaderboard for season: {}, week: {}, leagueId: {}", season, week, leagueId, e);
            throw e;
        }
    }

    public List<PlayerScore> getSeasonLeaderboard(Long leagueId, Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        logger.info("Getting season leaderboard for season: {}, leagueId: {}", season, leagueId);
        
        try {
            Set<Long> seasonGameIds = gameRepository.findBySeasonYear(season).stream()
                    .map(Game::getId)
                    .collect(Collectors.toSet());

            List<Pick> allPicks;
            if (leagueId != null) {
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
                allPicks = pickRepository.findByLeague(league).stream()
                        .filter(pick -> seasonGameIds.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
            } else {
                allPicks = pickRepository.findAll().stream()
                        .filter(pick -> seasonGameIds.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
            }
            
            return calculateLeaderboard(allPicks);
        } catch (Exception e) {
            logger.error("Error getting season leaderboard for season: {}, leagueId: {}", season, leagueId, e);
            throw e;
        }
    }

    private List<PlayerScore> calculateLeaderboard(List<Pick> picks) {
        Map<String, PlayerScore> userScores = new HashMap<>();
        
        for (Pick pick : picks) {
            if (pick.isCorrect()) {
                String username = pick.getUser().getUsername();
                String name = pick.getUser().getName();
                
                if (userScores.containsKey(username)) {
                    userScores.get(username).setScore(userScores.get(username).getScore() + 1);
                } else {
                    userScores.put(username, new PlayerScore(username, name, 1L));
                }
            }
        }

        return userScores.values().stream()
                .sorted(Comparator.comparing(PlayerScore::getScore).reversed())
                .collect(Collectors.toList());
    }

    public List<WeeklyWinsDto> getWeeklyWins(Long leagueId, Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        logger.info("Getting weekly wins for season: {}, leagueId: {}", season, leagueId);
        
        try {
            Map<String, Long> weeklyWinsMap = new HashMap<>();
            
            List<Integer> weeks = gameRepository.findBySeasonYear(season).stream()
                    .map(Game::getWeek)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            
            for (Integer week : weeks) {
                List<Game> weekGames = gameRepository.findBySeasonYearAndWeek(season, week);
                boolean allGamesScored = !weekGames.isEmpty() && weekGames.stream().allMatch(Game::isScored);
                
                if (!allGamesScored) {
                    continue;
                }
                
                List<PlayerScore> weeklyLeaderboard = getWeeklyLeaderboard(week, leagueId, season);
                
                if (!weeklyLeaderboard.isEmpty()) {
                    Long highestScore = weeklyLeaderboard.get(0).getScore();
                    List<PlayerScore> winners = weeklyLeaderboard.stream()
                            .filter(player -> player.getScore().equals(highestScore))
                            .collect(Collectors.toList());
                    
                    for (PlayerScore winner : winners) {
                        weeklyWinsMap.merge(winner.getUsername(), 1L, Long::sum);
                    }
                }
            }
            
            List<WeeklyWinsDto> result = weeklyWinsMap.entrySet().stream()
                    .map(entry -> {
                        String username = entry.getKey();
                        Long wins = entry.getValue();
                        
                        String name = username;
                        if (leagueId != null) {
                            League league = leagueRepository.findById(leagueId)
                                    .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
                            List<Pick> userPicks = pickRepository.findByLeague(league).stream()
                                    .filter(pick -> pick.getUser().getUsername().equals(username))
                                    .collect(Collectors.toList());
                            if (!userPicks.isEmpty()) {
                                name = userPicks.get(0).getUser().getName();
                            }
                        }
                        
                        return new WeeklyWinsDto(username, name, wins);
                    })
                    .sorted(Comparator.comparing(WeeklyWinsDto::getWeeklyWins).reversed()
                            .thenComparing(WeeklyWinsDto::getUsername))
                    .collect(Collectors.toList());
            
            return result;
        } catch (Exception e) {
            logger.error("Error getting weekly wins for season: {}, leagueId: {}", season, leagueId, e);
            throw e;
        }
    }

    public boolean isWeekComplete(Integer week, Integer seasonYear) {
        int season = seasonService.normalizeSeasonYear(seasonYear);
        List<Game> weekGames = gameRepository.findBySeasonYearAndWeek(season, week);
        if (weekGames.isEmpty()) {
            return false;
        }
        return weekGames.stream().allMatch(Game::isScored);
    }
}
