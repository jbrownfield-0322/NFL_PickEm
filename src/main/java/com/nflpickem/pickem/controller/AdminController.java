package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.model.AlertMilestone;
import com.nflpickem.pickem.repository.AlertMilestoneRepository;
import com.nflpickem.pickem.service.AlertService;
import com.nflpickem.pickem.service.OddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private OddsService oddsService;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private AlertMilestoneRepository alertMilestoneRepository;
    
    /**
     * Trigger odds update for current week
     */
    @PostMapping("/odds/trigger-update")
    public ResponseEntity<Map<String, Object>> triggerOddsUpdate() {
        try {
            // Get current week (you might want to inject GameService for this)
            int currentWeek = 1; // Default to week 1, should be injected from GameService
            
            var updatedOdds = oddsService.updateOddsForWeek(currentWeek);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully updated odds for week " + currentWeek);
            response.put("oddsCount", updatedOdds.size());
            
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
     * Trigger odds update for specific week
     */
    @PostMapping("/odds/trigger-update/{week}")
    public ResponseEntity<Map<String, Object>> triggerOddsUpdateForWeek(@PathVariable Integer week) {
        try {
            var updatedOdds = oddsService.updateOddsForWeek(week);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully updated odds for week " + week);
            response.put("oddsCount", updatedOdds.size());
            
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
     * Trigger update for all available odds and games
     */
    @PostMapping("/odds/trigger-update-all")
    public ResponseEntity<Map<String, Object>> triggerUpdateAllOddsAndGames() {
        try {
            var updatedOdds = oddsService.updateAllAvailableOdds();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully updated all available odds and games");
            response.put("oddsCount", updatedOdds.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update all odds and games: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get odds API status
     */
    @GetMapping("/odds/status")
    public ResponseEntity<Map<String, Object>> getOddsStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("apiConfigured", oddsService.isApiConfigured());
            response.put("message", oddsService.isApiConfigured() ? 
                "Odds API is configured and ready" : 
                "Odds API key is not configured");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get odds status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get alert milestone history
     */
    @GetMapping("/alerts/milestones")
    public ResponseEntity<List<AlertMilestone>> getMilestoneHistory() {
        try {
            List<AlertMilestone> alerts = alertMilestoneRepository.findRecentAlerts();
            // Limit to 10 most recent
            if (alerts.size() > 10) {
                alerts = alerts.subList(0, 10);
            }
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get alert status and configuration
     */
    @GetMapping("/alerts/status")
    public ResponseEntity<Map<String, Object>> getAlertStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("milestones", alertService.getMilestones());
            status.put("totalAlertsSent", alertMilestoneRepository.count());
            AlertMilestone lastAlert = alertMilestoneRepository.findMostRecentAlert();
            status.put("lastAlert", lastAlert);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get alert status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
