package com.nflpickem.pickem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_milestones")
public class AlertMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer milestone;
    
    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;
    
    @Column(name = "requests_remaining", nullable = false)
    private Integer requestsRemaining;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "email_recipients")
    private String emailRecipients; // JSON string
    
    @Column(nullable = false)
    private Boolean success = true;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    public AlertMilestone() {}
    
    public AlertMilestone(Integer milestone, String apiEndpoint, Integer requestsRemaining) {
        this.milestone = milestone;
        this.apiEndpoint = apiEndpoint;
        this.requestsRemaining = requestsRemaining;
        this.sentAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getMilestone() {
        return milestone;
    }
    
    public void setMilestone(Integer milestone) {
        this.milestone = milestone;
    }
    
    public String getApiEndpoint() {
        return apiEndpoint;
    }
    
    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }
    
    public Integer getRequestsRemaining() {
        return requestsRemaining;
    }
    
    public void setRequestsRemaining(Integer requestsRemaining) {
        this.requestsRemaining = requestsRemaining;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getEmailRecipients() {
        return emailRecipients;
    }
    
    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
