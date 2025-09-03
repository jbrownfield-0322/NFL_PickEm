package com.nflpickem.pickem.service;

import com.nflpickem.pickem.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class ScheduledOddsService {
    
    private final OddsService oddsService;
    private final GameRepository gameRepository;
    
    @Value("${odds.scheduling.enabled:true}")
    private boolean schedulingEnabled;
    
    @Value("${odds.scheduling.max-weeks-per-update:2}")
    private int maxWeeksPerUpdate;
    
    @Value("${odds.scheduling.delay-between-weeks:2000}")
    private long delayBetweenWeeks;
    
    @Autowired
    public ScheduledOddsService(OddsService oddsService, GameRepository gameRepository) {
        this.oddsService = oddsService;
        this.gameRepository = gameRepository;
    }
    
    /**
     * Update odds every 4 hours for all active weeks
     * Cron: Every 4 hours (0, 4, 8, 12, 16, 20)
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void updateOddsEveryFourHours() {
        if (!schedulingEnabled) {
            log.debug("Scheduled odds update skipped - scheduling disabled");
            return;
        }
        
        log.info("Starting scheduled odds update at {}", Instant.now());
        
        try {
            // Get current NFL week
            Integer currentWeek = getCurrentNflWeek();
            
            // Update odds for current week and next few weeks (configurable)
            for (int i = 0; i < maxWeeksPerUpdate; i++) {
                Integer weekToUpdate = currentWeek + i;
                if (weekToUpdate <= 18) { // NFL regular season is 18 weeks
                    log.info("Updating odds for Week {}", weekToUpdate);
                    oddsService.fetchOddsForWeek(weekToUpdate);
                    
                    // Add delay between weeks to respect API rate limits
                    if (i < maxWeeksPerUpdate - 1) {
                        Thread.sleep(delayBetweenWeeks);
                    }
                }
            }
            
            log.info("Completed scheduled odds update at {}", Instant.now());
            
        } catch (Exception e) {
            log.error("Error during scheduled odds update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update odds every hour during game days (Thursday, Sunday, Monday)
     * Cron: Every hour on game days
     */
    @Scheduled(cron = "0 0 * * * THU,SUN,MON")
    public void updateOddsOnGameDays() {
        if (!schedulingEnabled) {
            log.debug("Game day odds update skipped - scheduling disabled");
            return;
        }
        
        log.info("Starting game day odds update at {}", Instant.now());
        
        try {
            Integer currentWeek = getCurrentNflWeek();
            log.info("Updating odds for Week {} on game day", currentWeek);
            oddsService.fetchOddsForWeek(currentWeek);
            
            log.info("Completed game day odds update at {}", Instant.now());
            
        } catch (Exception e) {
            log.error("Error during game day odds update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update odds 2 hours before each game starts
     * This is more complex and would require additional logic
     * For now, we'll stick with the 4-hour intervals
     */
    
    /**
     * Get current NFL week based on date
     */
    private Integer getCurrentNflWeek() {
        LocalDate today = LocalDate.now();
        LocalDate septemberFirst = LocalDate.of(today.getYear(), 9, 1);
        
        // Find first Thursday in September
        LocalDate nflSeasonStart = septemberFirst;
        while (nflSeasonStart.getDayOfWeek().getValue() != 4) { // 4 = Thursday
            nflSeasonStart = nflSeasonStart.plusDays(1);
        }
        
        if (today.isBefore(nflSeasonStart)) {
            return 1; // Pre-season
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(nflSeasonStart, today);
        return (int) (daysBetween / 7) + 1;
    }
    
    /**
     * Manual trigger for immediate odds update (can be called from admin endpoints)
     */
    public void triggerImmediateUpdate() {
        log.info("Manual odds update triggered at {}", Instant.now());
        updateOddsEveryFourHours();
    }
    
    /**
     * Check if scheduling is enabled
     */
    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }
    
    /**
     * Get scheduling configuration
     */
    public String getSchedulingInfo() {
        return String.format("Scheduling: %s, Max weeks per update: %d, Delay: %dms", 
                schedulingEnabled ? "Enabled" : "Disabled", 
                maxWeeksPerUpdate, 
                delayBetweenWeeks);
    }
}
