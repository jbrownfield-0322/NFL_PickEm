package com.nflpickem.pickem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.logging.Logger;

@Service
public class ScheduledOddsService {
    
    private static final Logger logger = Logger.getLogger(ScheduledOddsService.class.getName());
    
    @Autowired
    private OddsService oddsService;
    
    @Value("${odds.scheduling.enabled:true}")
    private boolean schedulingEnabled;
    
    @Value("${odds.scheduling.frequency:4}")
    private int updateFrequencyHours;
    
    @Value("${odds.scheduling.game-day-frequency:1}")
    private int gameDayUpdateFrequencyHours;
    
    @Value("${odds.scheduling.max-weeks-per-update:4}")
    private int maxWeeksPerUpdate;
    
    @Value("${odds.scheduling.delay-between-weeks:2000}")
    private long delayBetweenWeeksMs;
    
    /**
     * Update odds every N hours (configurable via odds.scheduling.frequency)
     * Cron format: every N hours at minute 0
     */
    @Scheduled(cron = "0 0 */${odds.scheduling.frequency} * * *")
    public void updateOddsEveryFourHours() {
        if (!schedulingEnabled) {
            logger.info("Scheduled odds updates are disabled");
            return;
        }
        
        try {
            logger.info("Starting scheduled odds update (" + updateFrequencyHours + "-hour interval)");
            updateOddsForCurrentWeeks();
            logger.info("Completed scheduled odds update");
        } catch (Exception e) {
            logger.severe("Error during scheduled odds update: " + e.getMessage());
        }
    }
    
    /**
     * Update odds more frequently on game days (Thursday, Sunday, Monday)
     * Cron format: every N hours on game days (configurable via odds.scheduling.game-day-frequency)
     */
    @Scheduled(cron = "0 0 */${odds.scheduling.game-day-frequency} * * THU,SUN,MON")
    public void updateOddsOnGameDays() {
        if (!schedulingEnabled) {
            return;
        }
        
        try {
            logger.info("Starting game day odds update (every " + gameDayUpdateFrequencyHours + " hours)");
            updateOddsForCurrentWeeks();
            logger.info("Completed game day odds update");
        } catch (Exception e) {
            logger.severe("Error during game day odds update: " + e.getMessage());
        }
    }
    
    /**
     * Update odds for current and upcoming weeks
     */
    private void updateOddsForCurrentWeeks() {
        int currentWeek = getCurrentNflWeek();
        
        // Update current week and next few weeks
        for (int week = currentWeek; week <= currentWeek + maxWeeksPerUpdate && week <= 18; week++) {
            try {
                logger.info("Updating odds for Week " + week);
                oddsService.fetchOddsForWeek(week);
                
                // Add delay between weeks to respect API rate limits
                if (week < currentWeek + maxWeeksPerUpdate && week < 18) {
                    Thread.sleep(delayBetweenWeeksMs);
                }
                
            } catch (Exception e) {
                logger.warning("Failed to update odds for Week " + week + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Get current NFL week
     */
    public int getCurrentNflWeek() {
        LocalDate today = LocalDate.now();
        LocalDate septemberFirst = LocalDate.of(today.getYear(), 9, 1);
        LocalDate nflSeasonStart = septemberFirst.with(TemporalAdjusters.firstInMonth(DayOfWeek.THURSDAY));
        
        if (septemberFirst.getDayOfWeek() == DayOfWeek.SUNDAY) {
            nflSeasonStart = septemberFirst;
        }
        
        if (today.isBefore(nflSeasonStart)) {
            return 1;
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(nflSeasonStart, today);
        return Math.min((int) (daysBetween / 7) + 1, 18);
    }
    
    /**
     * Manually trigger odds update for all current weeks
     */
    public void updateOddsNow() {
        logger.info("Manual odds update triggered");
        updateOddsForCurrentWeeks();
    }
    
    /**
     * Manually trigger odds update for a specific week
     */
    public void updateOddsForWeek(int week) {
        logger.info("Manual odds update triggered for Week " + week);
        try {
            oddsService.fetchOddsForWeek(week);
        } catch (Exception e) {
            logger.severe("Failed to update odds for Week " + week + ": " + e.getMessage());
            throw e;
        }
    }
    
    // Getters for monitoring
    public boolean isSchedulingEnabled() {
        return schedulingEnabled;
    }
    
    public int getUpdateFrequencyHours() {
        return updateFrequencyHours;
    }
    
    public int getGameDayUpdateFrequencyHours() {
        return gameDayUpdateFrequencyHours;
    }
    
    public int getMaxWeeksPerUpdate() {
        return maxWeeksPerUpdate;
    }
    
    public long getDelayBetweenWeeksMs() {
        return delayBetweenWeeksMs;
    }
}
