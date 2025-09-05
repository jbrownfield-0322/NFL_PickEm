package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.model.BettingOdds;
import com.nflpickem.pickem.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/odds")
@CrossOrigin(origins = "*")
public class OddsController {
    
    @Autowired
    private OddsService oddsService;
    
    /**
     * Get odds for a specific week
     */
    @GetMapping("/week/{week}")
    public ResponseEntity<List<BettingOdds>> getOddsForWeek(@PathVariable Integer week) {
        try {
            List<BettingOdds> odds = oddsService.getOddsForWeek(week);
            return ResponseEntity.ok(odds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get odds for a specific game
     */
    @GetMapping("/game/{gameId}")
    public ResponseEntity<List<BettingOdds>> getOddsForGame(@PathVariable Long gameId) {
        try {
            List<BettingOdds> odds = oddsService.getOddsForGame(gameId);
            return ResponseEntity.ok(odds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update odds for a specific week
     */
    @PostMapping("/update/week/{week}")
    public ResponseEntity<Map<String, Object>> updateOddsForWeek(@PathVariable Integer week) {
        try {
            List<BettingOdds> updatedOdds = oddsService.updateOddsForWeek(week);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully updated odds for week " + week);
            response.put("oddsCount", updatedOdds.size());
            response.put("odds", updatedOdds);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update odds: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Update all available odds and create/update games as needed
     */
    @PostMapping("/update/all")
    public ResponseEntity<Map<String, Object>> updateAllAvailableOdds() {
        try {
            List<BettingOdds> updatedOdds = oddsService.updateAllAvailableOdds();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully updated all available odds and games");
            response.put("oddsCount", updatedOdds.size());
            response.put("odds", updatedOdds);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update all odds: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Check if odds need updating for a week
     */
    @GetMapping("/needs-update/{week}")
    public ResponseEntity<Map<String, Object>> checkIfOddsNeedUpdate(@PathVariable Integer week) {
        try {
            boolean needsUpdate = oddsService.needsUpdate(week);
            
            Map<String, Object> response = new HashMap<>();
            response.put("week", week);
            response.put("needsUpdate", needsUpdate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check odds status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Clean up stale odds
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupStaleOdds() {
        try {
            oddsService.cleanupStaleOdds();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully cleaned up stale odds");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cleanup stale odds: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
