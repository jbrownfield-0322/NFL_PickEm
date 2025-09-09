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

    public LeaderboardService(PickRepository pickRepository, GameRepository gameRepository, LeagueRepository leagueRepository) {
        this.pickRepository = pickRepository;
        this.gameRepository = gameRepository;
        this.leagueRepository = leagueRepository;
    }

    public List<PlayerScore> getWeeklyLeaderboard(Integer week, Long leagueId) {
        logger.info("Getting weekly leaderboard for week: {}, leagueId: {}", week, leagueId);
        
        try {
            List<Game> gamesInWeek = gameRepository.findByWeek(week);
            logger.info("Found {} games for week {}", gamesInWeek.size(), week);
            
            Set<Long> gameIdsInWeek = gamesInWeek.stream()
                    .map(Game::getId)
                    .collect(Collectors.toSet());
            
            List<Pick> picksToScore;

            if (leagueId != null) {
                logger.info("Fetching picks for league: {}", leagueId);
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
                picksToScore = pickRepository.findByLeague(league).stream()
                        .filter(pick -> gameIdsInWeek.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
                logger.info("Found {} picks for league {} in week {}", picksToScore.size(), leagueId, week);
            } else {
                logger.info("Fetching all picks for week {}", week);
                picksToScore = pickRepository.findAll().stream()
                        .filter(pick -> gameIdsInWeek.contains(pick.getGame().getId()))
                        .collect(Collectors.toList());
                logger.info("Found {} picks for week {}", picksToScore.size(), week);
            }
            
            List<PlayerScore> result = calculateLeaderboard(picksToScore);
            logger.info("Calculated leaderboard with {} players", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error getting weekly leaderboard for week: {}, leagueId: {}", week, leagueId, e);
            throw e;
        }
    }

    public List<PlayerScore> getSeasonLeaderboard(Long leagueId) {
        logger.info("Getting season leaderboard for leagueId: {}", leagueId);
        
        try {
            List<Pick> allPicks;
            if (leagueId != null) {
                logger.info("Fetching picks for league: {}", leagueId);
                League league = leagueRepository.findById(leagueId)
                        .orElseThrow(() -> new RuntimeException("League not found with id: " + leagueId));
                allPicks = pickRepository.findByLeague(league);
                logger.info("Found {} picks for league {}", allPicks.size(), leagueId);
            } else {
                logger.info("Fetching all picks for season");
                allPicks = pickRepository.findAll();
                logger.info("Found {} picks for season", allPicks.size());
            }
            
            List<PlayerScore> result = calculateLeaderboard(allPicks);
            logger.info("Calculated season leaderboard with {} players", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error getting season leaderboard for leagueId: {}", leagueId, e);
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

    public List<WeeklyWinsDto> getWeeklyWins(Long leagueId) {
        logger.info("Getting weekly wins for leagueId: {}", leagueId);
        
        try {
            Map<String, Long> weeklyWinsMap = new HashMap<>();
            
            // Get all weeks that have games
            List<Integer> weeks = gameRepository.findAll().stream()
                    .map(Game::getWeek)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
            
            logger.info("Found weeks: {}", weeks);
            
            for (Integer week : weeks) {
                // Get weekly leaderboard for this week
                List<PlayerScore> weeklyLeaderboard = getWeeklyLeaderboard(week, leagueId);
                
                if (!weeklyLeaderboard.isEmpty()) {
                    // Find the highest score for this week
                    Long highestScore = weeklyLeaderboard.get(0).getScore();
                    
                    // Count all players who tied for the highest score
                    List<PlayerScore> winners = weeklyLeaderboard.stream()
                            .filter(player -> player.getScore().equals(highestScore))
                            .collect(Collectors.toList());
                    
                    logger.info("Week {}: {} players tied with score {}", week, winners.size(), highestScore);
                    
                    // Award a win to each player who tied for first
                    for (PlayerScore winner : winners) {
                        weeklyWinsMap.merge(winner.getUsername(), 1L, Long::sum);
                    }
                }
            }
            
            // Convert to WeeklyWinsDto list
            List<WeeklyWinsDto> result = weeklyWinsMap.entrySet().stream()
                    .map(entry -> {
                        String username = entry.getKey();
                        Long wins = entry.getValue();
                        
                        // Get the name from any pick by this user
                        String name = username; // Default to username
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
            
            logger.info("Calculated weekly wins for {} players", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error getting weekly wins for leagueId: {}", leagueId, e);
            throw e;
        }
    }
} 