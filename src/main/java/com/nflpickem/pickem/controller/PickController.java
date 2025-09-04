package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.PickRequest;
import com.nflpickem.pickem.dto.ErrorResponse;
import com.nflpickem.pickem.dto.PickComparisonDto;
import com.nflpickem.pickem.dto.BulkPickRequest;
import com.nflpickem.pickem.model.Pick;
import com.nflpickem.pickem.service.PickService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/picks")
public class PickController {
    private static final Logger logger = LoggerFactory.getLogger(PickController.class);
    private final PickService pickService;

    public PickController(PickService pickService) {
        this.pickService = pickService;
    }

    @PostMapping("/submit")
    public ResponseEntity<Object> submitPick(@RequestBody PickRequest pickRequest) {
        logger.info("Received pick submission request: userId={}, gameId={}, pickedTeam={}, leagueId={}", 
                   pickRequest.getUserId(), pickRequest.getGameId(), pickRequest.getPickedTeam(), pickRequest.getLeagueId());
        
        try {
            Pick submittedPick = pickService.submitPick(pickRequest.getUserId(), pickRequest.getGameId(), pickRequest.getPickedTeam(), pickRequest.getLeagueId());
            logger.info("Pick submitted successfully: {}", submittedPick.getId());
            return new ResponseEntity<>(submittedPick, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("Error submitting pick: {}", e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Pick>> getPicksByUser(@PathVariable Long userId, @RequestParam Long leagueId) {
        return ResponseEntity.ok(pickService.getPicksByUser(userId, leagueId));
    }

    @GetMapping("/comparison/{userId}/{week}")
    public ResponseEntity<List<PickComparisonDto>> getPickComparison(
            @PathVariable Long userId, 
            @PathVariable Integer week,
            @RequestParam Long leagueId) {
        try {
            List<PickComparisonDto> comparison = pickService.getPickComparison(userId, week, leagueId);
            return ResponseEntity.ok(comparison);
        } catch (RuntimeException e) {
            logger.error("Error getting pick comparison: {}", e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/submit-bulk")
    public ResponseEntity<Object> submitBulkPicks(@RequestBody BulkPickRequest bulkRequest) {
        logger.info("Received bulk pick submission request: userId={}, leagueId={}, picksCount={}", 
                   bulkRequest.getUserId(), bulkRequest.getLeagueId(), bulkRequest.getPicks().size());
        
        try {
            List<Pick> submittedPicks = pickService.submitBulkPicks(bulkRequest);
            logger.info("Bulk picks submitted successfully: {} picks", submittedPicks.size());
            return new ResponseEntity<>(submittedPicks, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("Error submitting bulk picks: {}", e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), System.currentTimeMillis());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("Test endpoint called");
        return ResponseEntity.ok("PickController is working!");
    }
} 