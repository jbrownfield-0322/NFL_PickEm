package com.nflpickem.pickem.service;

import com.nflpickem.pickem.model.AlertMilestone;
import com.nflpickem.pickem.repository.AlertMilestoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Value("${ALERT_MILESTONES:100,50,25,10}")
    private String milestonesConfig;
    
    private final DiscordService discordService;
    private final AlertMilestoneRepository alertRepository;
    private List<Integer> milestones;
    
    public AlertService(DiscordService discordService, AlertMilestoneRepository alertRepository) {
        this.discordService = discordService;
        this.alertRepository = alertRepository;
    }
    
    @PostConstruct
    public void init() {
        this.milestones = parseMilestones();
    }
    
    /**
     * Check if we've hit a milestone and send alert if needed
     */
    public void checkApiLimits(HttpHeaders responseHeaders, String apiEndpoint) {
        String requestsRemainingHeader = responseHeaders.getFirst("X-Requests-Remaining");
        if (requestsRemainingHeader == null) {
            logger.debug("No X-Requests-Remaining header found");
            return;
        }
        
        try {
            int requestsRemaining = Integer.parseInt(requestsRemainingHeader);
            Integer milestone = findTriggeredMilestone(requestsRemaining);
            
            if (milestone != null) {
                sendMilestoneAlert(milestone, apiEndpoint, requestsRemaining);
            }
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid X-Requests-Remaining header: {}", requestsRemainingHeader);
        }
    }
    
    /**
     * Find which milestone was triggered (if any)
     */
    private Integer findTriggeredMilestone(int requestsRemaining) {
        // Check milestones in ascending order to find the smallest crossed milestone
        for (int i = milestones.size() - 1; i >= 0; i--) {
            Integer milestone = milestones.get(i);
            if (requestsRemaining <= milestone) {
                return milestone;
            }
        }
        return null;
    }
    
    /**
     * Send milestone alert if not already sent
     */
    private void sendMilestoneAlert(Integer milestone, String apiEndpoint, int requestsRemaining) {
        // Check if we've already sent this milestone alert for this endpoint
        if (alertRepository.existsByMilestoneAndApiEndpoint(milestone, apiEndpoint)) {
            logger.debug("Milestone {} alert already sent for endpoint {}", milestone, apiEndpoint);
            return;
        }
        
        try {
            // Send Discord alert
            discordService.sendMilestoneAlert(milestone, apiEndpoint, requestsRemaining);
            
            // Record the alert in database
            AlertMilestone alertRecord = new AlertMilestone();
            alertRecord.setMilestone(milestone);
            alertRecord.setApiEndpoint(apiEndpoint);
            alertRecord.setRequestsRemaining(requestsRemaining);
            alertRecord.setSentAt(LocalDateTime.now());
            alertRecord.setEmailRecipients("Discord Only"); // Simplified since we're not using email
            alertRecord.setSuccess(true);
            
            alertRepository.save(alertRecord);
            
            logger.info("✅ Discord milestone alert sent: {} requests remaining (milestone: {}) for endpoint: {}", 
                       requestsRemaining, milestone, apiEndpoint);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send Discord milestone alert: {}", e.getMessage());
            
            // Record the failed attempt
            AlertMilestone alertRecord = new AlertMilestone();
            alertRecord.setMilestone(milestone);
            alertRecord.setApiEndpoint(apiEndpoint);
            alertRecord.setRequestsRemaining(requestsRemaining);
            alertRecord.setSentAt(LocalDateTime.now());
            alertRecord.setEmailRecipients("Discord Only");
            alertRecord.setSuccess(false);
            alertRecord.setErrorMessage(e.getMessage());
            
            alertRepository.save(alertRecord);
        }
    }
    
    /**
     * Parse milestones from configuration string
     */
    private List<Integer> parseMilestones() {
        if (milestonesConfig == null || milestonesConfig.trim().isEmpty()) {
            return Arrays.asList(100, 50, 25, 10);
        }
        
        try {
            return Arrays.stream(milestonesConfig.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .sorted(Collections.reverseOrder()) // Sort descending: 100, 50, 25, 10
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            logger.warn("Invalid milestone configuration: {}, using defaults", milestonesConfig);
            return Arrays.asList(100, 50, 25, 10);
        }
    }
    
    
    /**
     * Send Discord alert for API quota exceeded
     */
    public void sendQuotaExceededAlert(String apiEndpoint, String errorResponse) {
        try {
            // Send Discord alert
            discordService.sendQuotaExceededAlert(apiEndpoint, errorResponse);
            
            logger.info("✅ Discord quota exceeded alert sent for endpoint: {}", apiEndpoint);
            
        } catch (Exception e) {
            logger.error("❌ Failed to send Discord quota exceeded alert: {}", e.getMessage());
        }
    }
    
    /**
     * Get configured milestones (for admin endpoints)
     */
    public List<Integer> getMilestones() {
        if (milestones == null) {
            return Arrays.asList(100, 50, 25, 10); // Default fallback
        }
        return milestones;
    }
}
