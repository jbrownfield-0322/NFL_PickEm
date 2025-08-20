package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.PickRequest;
import com.nflpickem.pickem.dto.ErrorResponse;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.service.PickService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/picks")
public class PickController {
    private final PickService pickService;

    public PickController(PickService pickService) {
        this.pickService = pickService;
    }

    @PostMapping
    public ResponseEntity<Object> submitPick(@RequestBody PickRequest pickRequest) {
        Long userId = 1L; // For now, hardcode userId
        try {
            Pick submittedPick = pickService.submitPick(userId, pickRequest.getGameId(), pickRequest.getPickedTeam(), pickRequest.getLeagueId());
            return new ResponseEntity<>(submittedPick, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pick>> getPicksByUser(@PathVariable Long userId, @RequestParam(required = false) Long leagueId) {
        return ResponseEntity.ok(pickService.getPicksByUser(userId, leagueId));
    }
} 