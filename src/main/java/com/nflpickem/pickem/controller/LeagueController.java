package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.CreateLeagueRequest;
import com.nflpickem.pickem.dto.JoinLeagueRequest;
import com.nflpickem.pickem.dto.LeagueResponse;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.service.LeagueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {
    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping("/create")
    public ResponseEntity<LeagueResponse> createLeague(@RequestBody CreateLeagueRequest request) {
        Long adminId = 1L; // Hardcoded for now
        try {
            League league = leagueService.createLeague(request.getLeagueName(), adminId);
            LeagueResponse leagueResponse = new LeagueResponse(league);
            return new ResponseEntity<>(leagueResponse, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/join")
    public ResponseEntity<LeagueResponse> joinLeague(@RequestBody JoinLeagueRequest request) {
        Long userId = 1L; // Hardcoded for now
        try {
            League league = leagueService.joinLeague(request.getJoinCode(), userId);
            LeagueResponse leagueResponse = new LeagueResponse(league);
            return new ResponseEntity<>(leagueResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeagueResponse>> getLeaguesByUserId(@PathVariable Long userId) {
        Long currentUserId = 1L; // Hardcoded for now
        try {
            List<League> leagues = leagueService.getLeaguesByUserId(currentUserId);
            List<LeagueResponse> leagueResponses = leagues.stream()
                .map(LeagueResponse::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(leagueResponses);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeagueResponse> getLeagueById(@PathVariable Long id) {
        Optional<League> league = leagueService.getLeagueById(id);
        return league.map(l -> ResponseEntity.ok(new LeagueResponse(l)))
                     .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
} 