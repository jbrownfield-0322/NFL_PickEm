package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.CreateLeagueRequest;
import com.nflpickem.pickem.dto.JoinLeagueRequest;
import com.nflpickem.pickem.dto.LeagueResponse;
import com.nflpickem.pickem.model.League;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.service.LeagueService;
import com.nflpickem.pickem.util.AuthContext;
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
        try {
            User currentUser = AuthContext.getCurrentUser();
            if (currentUser == null) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
            
            League league = leagueService.createLeague(request.getLeagueName(), currentUser.getId());
            LeagueResponse leagueResponse = new LeagueResponse(league);
            return new ResponseEntity<>(leagueResponse, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/join")
    public ResponseEntity<LeagueResponse> joinLeague(@RequestBody JoinLeagueRequest request) {
        try {
            User currentUser = AuthContext.getCurrentUser();
            if (currentUser == null) {
                return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
            }
            
            League league = leagueService.joinLeague(request.getJoinCode(), currentUser.getId());
            LeagueResponse leagueResponse = new LeagueResponse(league);
            return new ResponseEntity<>(leagueResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeagueResponse>> getLeaguesByUserId(@PathVariable Long userId) {
        try {
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            List<League> leagues = leagueService.getLeaguesByUserId(userId);
            List<LeagueResponse> leagueResponses = leagues.stream()
                .map(LeagueResponse::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(leagueResponses);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeagueResponse> getLeagueById(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest().build();
            }
            
            Optional<League> league = leagueService.getLeagueById(id);
            return league.map(l -> ResponseEntity.ok(new LeagueResponse(l)))
                         .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 