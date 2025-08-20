package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.model.Game;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.dto.PlayerScore;
import com.nflpickem.pickem.repository.PickRepository;
import com.nflpickem.pickem.repository.GameRepository;
import com.nflpickem.pickem.repository.LeagueRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {
    private final PickRepository pickRepository;
    private final GameRepository gameRepository;
    private final LeagueRepository leagueRepository;

    public LeaderboardService(PickRepository pickRepository, GameRepository gameRepository, LeagueRepository leagueRepository) {
        this.pickRepository = pickRepository;
        this.gameRepository = gameRepository;
        this.leagueRepository = leagueRepository;
    }

    public List<PlayerScore> getWeeklyLeaderboard(Integer week, Long leagueId) {
        List<Game> gamesInWeek = gameRepository.findByWeek(week);
        List<Pick> picksToScore;

        if (leagueId != null) {
            League league = leagueRepository.findById(leagueId)
                    .orElseThrow(() -> new RuntimeException("League not found"));
            picksToScore = pickRepository.findByLeague(league).stream()
                    .filter(pick -> gamesInWeek.contains(pick.getGame()))
                    .collect(Collectors.toList());
        } else {
            picksToScore = pickRepository.findAll().stream()
                    .filter(pick -> gamesInWeek.contains(pick.getGame()))
                    .collect(Collectors.toList());
        }
        return calculateLeaderboard(picksToScore);
    }

    public List<PlayerScore> getSeasonLeaderboard(Long leagueId) {
        List<Pick> allPicks;
        if (leagueId != null) {
            League league = leagueRepository.findById(leagueId)
                    .orElseThrow(() -> new RuntimeException("League not found"));
            allPicks = pickRepository.findByLeague(league);
        } else {
            allPicks = pickRepository.findAll();
        }
        return calculateLeaderboard(allPicks);
    }

    private List<PlayerScore> calculateLeaderboard(List<Pick> picks) {
        Map<String, Long> userScores = picks.stream()
                .filter(Pick::isCorrect)
                .collect(Collectors.groupingBy(pick -> pick.getUser().getUsername(), Collectors.counting()));

        return userScores.entrySet().stream()
                .map(entry -> new PlayerScore(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PlayerScore::getScore).reversed())
                .collect(Collectors.toList());
    }
} 