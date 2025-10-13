package com.nflpickem.pickem.service;

import com.nflpickem.pickem.dto.DiscordEmbed;
import com.nflpickem.pickem.dto.DiscordMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DiscordService {
    
    private static final Logger logger = LoggerFactory.getLogger(DiscordService.class);
    
    @Value("${DISCORD_WEBHOOK_URL:}")
    private String webhookUrl;
    
    @Value("${DISCORD_BOT_NAME:NFL Pick'em Alerts}")
    private String botName;
    
    private final RestTemplate restTemplate;
    
    public DiscordService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public void sendMilestoneAlert(Integer milestone, String apiEndpoint, int requestsRemaining) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("Discord webhook URL not configured, skipping Discord alert");
            return;
        }
        
        try {
            DiscordMessage message = buildDiscordMessage(milestone, apiEndpoint, requestsRemaining);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<DiscordMessage> entity = new HttpEntity<>(message, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Discord alert sent successfully for milestone: {}", milestone);
            } else {
                logger.error("❌ Discord alert failed: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to send Discord alert: {}", e.getMessage());
        }
    }
    
    private DiscordMessage buildDiscordMessage(Integer milestone, String apiEndpoint, int requestsRemaining) {
        DiscordMessage message = new DiscordMessage();
        message.setUsername(botName);
        message.setAvatarUrl("https://cdn-icons-png.flaticon.com/512/25/25694.png"); // NFL icon
        
        // Build embed for rich formatting
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("🚨 NFL Pick'em API Limit Alert");
        embed.setColor(getUrgencyColor(milestone));
        embed.setTimestamp(Instant.now().toString());
        
        // Add fields
        embed.addField("Milestone", milestone.toString(), true);
        embed.addField("Requests Remaining", String.valueOf(requestsRemaining), true);
        embed.addField("API Endpoint", apiEndpoint, false);
        embed.addField("Alert Level", getUrgencyLevel(milestone), false);
        embed.addField("Time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false);
        
        // Add recommendations
        embed.addField("Recommended Actions", getRecommendations(milestone), false);
        
        // Add footer
        DiscordEmbed.Footer footer = new DiscordEmbed.Footer();
        footer.setText("NFL Pick'em System • Each milestone alert is sent only once");
        embed.setFooter(footer);
        
        message.addEmbed(embed);
        
        return message;
    }
    
    private int getUrgencyColor(Integer milestone) {
        return switch (milestone) {
            case 100 -> 0xFFC107; // Yellow
            case 50 -> 0xFD7E14;  // Orange
            case 25 -> 0xDC3545;  // Red
            case 10 -> 0x6F42C1;  // Purple
            default -> 0x6C757D;  // Gray
        };
    }
    
    private String getUrgencyLevel(Integer milestone) {
        return switch (milestone) {
            case 100 -> "🟡 LOW - Monitor usage";
            case 50 -> "🟠 MEDIUM - Consider reducing frequency";
            case 25 -> "🔴 HIGH - Reduce update frequency";
            case 10 -> "🚨 CRITICAL - Immediate action required";
            default -> "⚠️ UNKNOWN";
        };
    }
    
    private String getRecommendations(Integer milestone) {
        return switch (milestone) {
            case 100 -> "• Monitor API usage patterns\n• Review scheduled update frequency";
            case 50 -> "• Reduce update frequency to 12 hours\n• Check for unnecessary API calls";
            case 25 -> "• Reduce to once daily\n• Consider upgrading API plan";
            case 10 -> "• **URGENT:** Stop non-essential calls\n• Upgrade API plan immediately";
            default -> "• No specific recommendations";
        };
    }
    
    /**
     * Send Discord alert for API quota exceeded
     */
    public void sendQuotaExceededAlert(String apiEndpoint, String errorResponse) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("Discord webhook URL not configured, skipping Discord quota exceeded alert");
            return;
        }
        
        try {
            DiscordMessage message = buildQuotaExceededMessage(apiEndpoint, errorResponse);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<DiscordMessage> entity = new HttpEntity<>(message, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Discord quota exceeded alert sent successfully for endpoint: {}", apiEndpoint);
            } else {
                logger.error("❌ Discord quota exceeded alert failed: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to send Discord quota exceeded alert: {}", e.getMessage());
        }
    }
    
    private DiscordMessage buildQuotaExceededMessage(String apiEndpoint, String errorResponse) {
        DiscordMessage message = new DiscordMessage();
        message.setUsername(botName);
        message.setAvatarUrl("https://cdn-icons-png.flaticon.com/512/25/25694.png"); // NFL icon
        
        // Build embed for rich formatting
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("🚨 CRITICAL: API Quota Exceeded");
        embed.setColor(0xDC3545); // Red color for critical alerts
        embed.setTimestamp(Instant.now().toString());
        
        // Add fields
        embed.addField("Status", "❌ QUOTA EXCEEDED", true);
        embed.addField("API Endpoint", apiEndpoint, true);
        embed.addField("Error Code", "401 UNAUTHORIZED", true);
        embed.addField("Time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false);
        
        // Add error details
        if (errorResponse != null && !errorResponse.trim().isEmpty()) {
            String truncatedError = errorResponse.length() > 1000 ? 
                errorResponse.substring(0, 1000) + "..." : errorResponse;
            embed.addField("Error Details", "```json\n" + truncatedError + "\n```", false);
        }
        
        // Add urgent recommendations
        embed.addField("🚨 IMMEDIATE ACTIONS REQUIRED", 
            "• **STOP** all non-essential API calls immediately\n" +
            "• **UPGRADE** your API plan at https://the-odds-api.com\n" +
            "• **REVIEW** your API usage patterns\n" +
            "• **CONTACT** support if this is unexpected", false);
        
        // Add footer
        DiscordEmbed.Footer footer = new DiscordEmbed.Footer();
        footer.setText("NFL Pick'em System • Critical Alert");
        embed.setFooter(footer);
        
        message.addEmbed(embed);
        
        return message;
    }
}
