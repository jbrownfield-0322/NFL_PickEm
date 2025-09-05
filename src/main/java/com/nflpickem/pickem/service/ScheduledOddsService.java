package com.nflpickem.pickem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;

@Service
public class ScheduledOddsService {
    
    @Autowired
    private OddsService oddsService;
    
    @Autowired
    private GameService gameService;
    
    /**
     * Update all available odds and games every 6 hours during NFL season
     * Runs at 6 AM, 12 PM, 6 PM, and 12 AM UTC
     */
    @Scheduled(cron = "0 0 6,12,18,0 * * *")
    public void scheduledOddsUpdate() {
        try {
            // Only run during NFL season (September to January)
            LocalDate now = LocalDate.now();
            if (isNflSeason(now)) {
                System.out.println("Scheduled comprehensive odds and games update");
                var updatedOdds = oddsService.updateAllAvailableOdds();
                System.out.println("Updated " + updatedOdds.size() + " odds entries and created/updated games as needed");
            }
        } catch (Exception e) {
            System.err.println("Error in scheduled odds update: " + e.getMessage());
        }
    }
    
    /**
     * Clean up stale odds daily at 2 AM UTC
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledOddsCleanup() {
        try {
            System.out.println("Running scheduled odds cleanup");
            oddsService.cleanupStaleOdds();
            System.out.println("Odds cleanup completed");
        } catch (Exception e) {
            System.err.println("Error in scheduled odds cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Check if we're in NFL season
     */
    private boolean isNflSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.SEPTEMBER || 
               month == Month.OCTOBER || 
               month == Month.NOVEMBER || 
               month == Month.DECEMBER || 
               (month == Month.JANUARY && date.getDayOfMonth() <= 7); // First week of January for playoffs
    }
}
