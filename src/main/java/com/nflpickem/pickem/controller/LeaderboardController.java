package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.PlayerScore;
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
    public ResponseEntity<List<PlayerScore>> getWeeklyLeaderboard(@PathVariable Integer weekNum, @RequestParam(required = false) Long leagueId) {
        return ResponseEntity.ok(leaderboardService.getWeeklyLeaderboard(weekNum, leagueId));
    }

    @GetMapping("/season")
    public ResponseEntity<List<PlayerScore>> getSeasonLeaderboard(@RequestParam(required = false) Long leagueId) {
        return ResponseEntity.ok(leaderboardService.getSeasonLeaderboard(leagueId));
    }
} 