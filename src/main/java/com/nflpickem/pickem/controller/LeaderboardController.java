package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.PlayerScore;
import com.nflpickem.pickem.dto.WeeklyWinsDto;
import com.nflpickem.pickem.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/weekly/{weekNum}")
    public ResponseEntity<List<PlayerScore>> getWeeklyLeaderboard(@PathVariable Integer weekNum, @RequestParam Long leagueId) {
        try {
            if (weekNum == null || weekNum < 1) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(leaderboardService.getWeeklyLeaderboard(weekNum, leagueId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/season")
    public ResponseEntity<List<PlayerScore>> getSeasonLeaderboard(@RequestParam Long leagueId) {
        try {
            return ResponseEntity.ok(leaderboardService.getSeasonLeaderboard(leagueId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/weekly-wins")
    public ResponseEntity<List<WeeklyWinsDto>> getWeeklyWins(@RequestParam Long leagueId) {
        try {
            return ResponseEntity.ok(leaderboardService.getWeeklyWins(leagueId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 