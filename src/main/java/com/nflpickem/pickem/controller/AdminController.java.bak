package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.service.ScheduledOddsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    
    private final ScheduledOddsService scheduledOddsService;
    
    @Autowired
    public AdminController(ScheduledOddsService scheduledOddsService) {
        this.scheduledOddsService = scheduledOddsService;
    }
    
    /**
     * Manually trigger an immediate odds update
     */
    @PostMapping("/odds/update-now")
    public ResponseEntity<Map<String, Object>> triggerOddsUpdate() {
        try {
            log.info("Admin triggered immediate odds update at {}", Instant.now());
            scheduledOddsService.triggerImmediateUpdate();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Odds update triggered successfully");
            response.put("timestamp", Instant.now());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error triggering odds update: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error triggering odds update: " + e.getMessage());
            response.put("timestamp", Instant.now());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get information about the odds update schedule
     */
    @GetMapping("/odds/schedule")
    public ResponseEntity<Map<String, Object>> getOddsSchedule() {
        Map<String, Object> schedule = new HashMap<>();
        
        // Next 4-hour update
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextUpdate = now.withMinute(0).withSecond(0).withNano(0);
        
        // Find next 4-hour mark (0, 4, 8, 12, 16, 20)
        int currentHour = now.getHour();
        int nextHour = ((currentHour / 4) + 1) * 4;
        if (nextHour >= 24) {
            nextHour = 0;
            nextUpdate = nextUpdate.plusDays(1);
        }
        nextUpdate = nextUpdate.withHour(nextHour);
        
        schedule.put("currentTime", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        schedule.put("nextScheduledUpdate", nextUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        schedule.put("updateFrequency", "Every 4 hours (00:00, 04:00, 08:00, 12:00, 16:00, 20:00)");
        schedule.put("gameDayUpdates", "Every hour on Thursday, Sunday, Monday");
        schedule.put("timezone", ZoneId.systemDefault().getId());
        
        return ResponseEntity.ok(schedule);
    }
    
    /**
     * Get the current odds update status
     */
    @GetMapping("/odds/status")
    public ResponseEntity<Map<String, Object>> getOddsStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("schedulingEnabled", scheduledOddsService.isSchedulingEnabled());
        status.put("lastUpdate", scheduledOddsService.getLastUpdateTime());
        status.put("nextUpdate", getNextUpdateTime());
        status.put("autoUpdate", true);
        status.put("manualUpdateAvailable", true);
        status.put("totalUpdates", scheduledOddsService.getTotalUpdates());
        status.put("schedulingInfo", scheduledOddsService.getSchedulingInfo());
        status.put("updateStats", scheduledOddsService.getUpdateStats());
        
        return ResponseEntity.ok(status);
    }
    
    private String getNextUpdateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextUpdate = now.withMinute(0).withSecond(0).withNano(0);
        
        int currentHour = now.getHour();
        int nextHour = ((currentHour / 4) + 1) * 4;
        if (nextHour >= 24) {
            nextHour = 0;
            nextUpdate = nextUpdate.plusDays(1);
        }
        nextUpdate = nextUpdate.withHour(nextHour);
        
        return nextUpdate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
