package com.nflpickem.pickem.repository;

import com.nflpickem.pickem.model.AlertMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertMilestoneRepository extends JpaRepository<AlertMilestone, Long> {
    
    /**
     * Check if a milestone alert has already been sent for a specific endpoint
     */
    boolean existsByMilestoneAndApiEndpoint(Integer milestone, String apiEndpoint);
    
    /**
     * Get all sent milestones for an endpoint (for debugging)
     */
    List<AlertMilestone> findByApiEndpointOrderBySentAtDesc(String apiEndpoint);
    
    /**
     * Get recent alerts (for admin dashboard)
     */
    @Query("SELECT a FROM AlertMilestone a ORDER BY a.sentAt DESC")
    List<AlertMilestone> findRecentAlerts();
    
    /**
     * Get the most recent alert
     */
    @Query("SELECT a FROM AlertMilestone a ORDER BY a.sentAt DESC")
    List<AlertMilestone> findMostRecentAlert();
}
