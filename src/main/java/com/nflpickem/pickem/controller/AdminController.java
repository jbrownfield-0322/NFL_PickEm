package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.service.OddsService;
import com.nflpickem.pickem.service.ScheduledOddsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/odds")
public class AdminController {
    
    @Autowired
    private ScheduledOddsService scheduledOddsService;
    
    @Autowired
    private OddsService oddsService;
    
    /**
     * Manually trigger odds update for all current weeks
     */
    @PostMapping("/update-now")
    public ResponseEntity<Map<String, String>> updateOddsNow() {
        try {
            scheduledOddsService.updateOddsNow();
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Odds update initiated successfully");
            response.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update odds: " + e.getMessage());
            response.put("timestamp", Instant.now().toString());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Manually trigger odds update for a specific week
     */
    @PostMapping("/update-week/{weekNum}")
    public ResponseEntity<Map<String, String>> updateOddsForWeek(@PathVariable Integer weekNum) {
        try {
            scheduledOddsService.updateOddsForWeek(weekNum);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Odds update initiated for Week " + weekNum);
            response.put("timestamp", Instant.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to update odds for Week " + weekNum + ": " + e.getMessage());
            response.put("timestamp", Instant.now().toString());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get current scheduling configuration and status
     */
    @GetMapping("/schedule")
    public ResponseEntity<Map<String, Object>> getScheduleInfo() {
        Map<String, Object> scheduleInfo = new HashMap<>();
        
        scheduleInfo.put("schedulingEnabled", scheduledOddsService.isSchedulingEnabled());
        scheduleInfo.put("updateFrequencyHours", scheduledOddsService.getUpdateFrequencyHours());
        scheduleInfo.put("gameDayUpdateFrequencyHours", scheduledOddsService.getGameDayUpdateFrequencyHours());
        scheduleInfo.put("maxWeeksPerUpdate", scheduledOddsService.getMaxWeeksPerUpdate());
        scheduleInfo.put("delayBetweenWeeksMs", scheduledOddsService.getDelayBetweenWeeksMs());
        scheduleInfo.put("currentNflWeek", scheduledOddsService.getCurrentNflWeek());
        scheduleInfo.put("nextUpdateTime", getNextUpdateTime());
        
        return ResponseEntity.ok(scheduleInfo);
    }
    
    /**
     * Get comprehensive status and statistics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Scheduling info
        status.put("schedulingEnabled", scheduledOddsService.isSchedulingEnabled());
        status.put("currentNflWeek", scheduledOddsService.getCurrentNflWeek());
        
        // Odds service stats
        status.put("lastUpdateTime", oddsService.getLastUpdateTime());
        status.put("totalUpdates", oddsService.getTotalUpdates());
        
        // API configuration status
        status.put("oddsApiConfigured", oddsService.isApiConfigured());
        status.put("oddsApiStatus", oddsService.getApiStatus());
        
        // Next scheduled updates
        status.put("nextFourHourUpdate", getNextUpdateTime());
        status.put("nextGameDayUpdate", getNextGameDayUpdateTime());
        
        // Configuration
        status.put("updateFrequencyHours", scheduledOddsService.getUpdateFrequencyHours());
        status.put("gameDayUpdateFrequencyHours", scheduledOddsService.getGameDayUpdateFrequencyHours());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Calculate next 4-hour update time
     */
    private String getNextUpdateTime() {
        Instant now = Instant.now();
        // Simple calculation - next 4-hour boundary
        long nextUpdate = now.plusSeconds(4 * 60 * 60).getEpochSecond();
        nextUpdate = (nextUpdate / (4 * 60 * 60)) * (4 * 60 * 60);
        return Instant.ofEpochSecond(nextUpdate).toString();
    }
    
    /**
     * Calculate next game day update time
     */
    private String getNextGameDayUpdateTime() {
        Instant now = Instant.now();
        // Next hour on game days
        return now.plusSeconds(60 * 60).toString();
    }
    
    /**
     * Check odds API configuration status
     */
    @GetMapping("/odds-api-status")
    public ResponseEntity<Map<String, Object>> getOddsApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", oddsService.isApiConfigured());
        status.put("status", oddsService.getApiStatus());
        status.put("lastUpdateTime", oddsService.getLastUpdateTime());
        status.put("totalUpdates", oddsService.getTotalUpdates());
        return ResponseEntity.ok(status);
    }
    
    /**
     * Test the odds API connection (without fetching odds)
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testOddsApiConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!oddsService.isApiConfigured()) {
                result.put("success", false);
                result.put("error", "API not configured");
                result.put("message", oddsService.getApiStatus());
                return ResponseEntity.badRequest().body(result);
            }
            
            // Test the actual API connection
            String testResult = oddsService.testApiConnection();
            result.put("testResult", testResult);
            result.put("success", testResult.contains("successful"));
            result.put("message", testResult);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Test the exact same request that works in PowerShell
     */
    @GetMapping("/test-powershell-request")
    public ResponseEntity<Map<String, Object>> testPowershellRequest() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!oddsService.isApiConfigured()) {
                result.put("success", false);
                result.put("error", "API not configured");
                result.put("message", oddsService.getApiStatus());
                return ResponseEntity.badRequest().body(result);
            }
            
            // Test the exact same request that works in PowerShell
            String testResult = oddsService.testExactPowerShellRequest();
            result.put("testResult", testResult);
            result.put("success", testResult.contains("successful"));
            result.put("message", testResult);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Debug endpoint to show exact values being used by OddsService
     */
    @GetMapping("/debug-odds-service")
    public ResponseEntity<Map<String, Object>> debugOddsService() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            debug.put("apiConfigured", oddsService.isApiConfigured());
            debug.put("apiStatus", oddsService.getApiStatus());
            debug.put("apiKeyForTesting", oddsService.getApiKeyForTesting());
            debug.put("lastUpdateTime", oddsService.getLastUpdateTime());
            debug.put("totalUpdates", oddsService.getTotalUpdates());
            
            // Add detailed configuration
            Map<String, Object> detailedConfig = oddsService.getDetailedConfiguration();
            debug.put("detailedConfiguration", detailedConfig);
            
            // Test basic connectivity
            String connectivityTest = oddsService.testBasicConnectivity();
            debug.put("basicConnectivityTest", connectivityTest);
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(debug);
    }
    
    /**
     * Direct environment variable check for debugging
     */
    @GetMapping("/debug-environment")
    public ResponseEntity<Map<String, Object>> debugEnvironment() {
        Map<String, Object> env = new HashMap<>();
        
        try {
            // Check environment variables directly
            env.put("THEODDS_API_KEY", System.getenv("THEODDS_API_KEY") != null ? 
                System.getenv("THEODDS_API_KEY").substring(0, Math.min(8, System.getenv("THEODDS_API_KEY").length())) + "..." : "NULL");
            env.put("SPRING_PROFILES_ACTIVE", System.getenv("SPRING_PROFILES_ACTIVE"));
            env.put("DATABASE_URL", System.getenv("DATABASE_URL") != null ? "SET" : "NULL");
            env.put("PORT", System.getenv("PORT"));
            
            // Check if we're in the right profile
            env.put("currentProfile", "heroku"); // This should match your active profile
            
        } catch (Exception e) {
            env.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(env);
    }
    
    /**
     * Test basic HTTP connectivity to The Odds API
     */
    @GetMapping("/test-connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!oddsService.isApiConfigured()) {
                result.put("success", false);
                result.put("error", "API not configured");
                result.put("message", oddsService.getApiStatus());
                return ResponseEntity.badRequest().body(result);
            }
            
            // Test basic connectivity
            String testResult = oddsService.testBasicConnectivity();
            result.put("testResult", testResult);
            result.put("success", testResult.contains("successful"));
            result.put("message", testResult);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
