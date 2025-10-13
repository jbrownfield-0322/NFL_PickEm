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
    List<AlertMilestone> findTop10ByOrderBySentAtDesc();
    
    /**
     * Get the most recent alert
     */
    AlertMilestone findTop1ByOrderBySentAtDesc();
}
