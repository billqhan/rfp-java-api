package com.l3harris.rfp.service;

import com.l3harris.rfp.model.DashboardMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for dashboard metrics and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OpportunityService opportunityService;
    private final ProposalService proposalService;
    private final WorkflowService workflowService;

    public DashboardMetrics getDashboardMetrics() {
        log.info("Generating dashboard metrics");
        
        DashboardMetrics metrics = new DashboardMetrics();
        
        try {
            // Get basic counts
            metrics.setTotalOpportunities(opportunityService.getTotalCount());
            metrics.setTotalProposals(proposalService.getTotalCount());
            metrics.setActiveWorkflows(workflowService.getActiveWorkflowCount());
            
            // Get status breakdowns
            metrics.setOpportunitiesByStatus(getOpportunityStatusBreakdown());
            metrics.setProposalsByStatus(getProposalStatusBreakdown());
            
            // Get recent items (limited to 5 each)
            metrics.setRecentOpportunities(opportunityService.getRecentOpportunities(5));
            metrics.setRecentMatches(getRecentMatches(5));
            
            // Calculate averages
            metrics.setAvgMatchScore(opportunityService.getAverageMatchScore());
            metrics.setProcessingTimeAvg(workflowService.getAverageProcessingTime());
            
            // Time series data (last 30 days)
            metrics.setOpportunitiesOverTime(getOpportunitiesTimeSeries(30));
            metrics.setMatchesOverTime(getMatchesTimeSeries(30));
            
            metrics.setLastUpdated(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error generating dashboard metrics", e);
            // Return basic metrics with error indicators
            metrics = getEmptyMetrics();
        }
        
        return metrics;
    }
    
    private DashboardMetrics.StatusBreakdown getOpportunityStatusBreakdown() {
        // Implementation would aggregate from S3/DynamoDB
        DashboardMetrics.StatusBreakdown breakdown = new DashboardMetrics.StatusBreakdown();
        breakdown.setActive(15);
        breakdown.setDraft(8);
        breakdown.setSubmitted(12);
        breakdown.setAwarded(5);
        breakdown.setClosed(25);
        breakdown.setCancelled(3);
        return breakdown;
    }
    
    private DashboardMetrics.StatusBreakdown getProposalStatusBreakdown() {
        // Implementation would aggregate from DynamoDB
        DashboardMetrics.StatusBreakdown breakdown = new DashboardMetrics.StatusBreakdown();
        breakdown.setDraft(6);
        breakdown.setActive(4);
        breakdown.setSubmitted(8);
        breakdown.setAwarded(2);
        breakdown.setClosed(15);
        return breakdown;
    }
    
    private List<DashboardMetrics.Match> getRecentMatches(int limit) {
        // Implementation would fetch from S3/DynamoDB
        List<DashboardMetrics.Match> matches = new ArrayList<>();
        
        for (int i = 0; i < Math.min(limit, 3); i++) {
            DashboardMetrics.Match match = new DashboardMetrics.Match();
            match.setMatchId("match-" + (i + 1));
            match.setOpportunityId("opp-" + (i + 1));
            match.setOpportunityTitle("Sample Opportunity " + (i + 1));
            match.setMatchScore(0.85 + (i * 0.05));
            match.setConfidenceLevel("HIGH");
            match.setMatchDate(LocalDateTime.now().minusHours(i + 1));
            match.setReasons(List.of("NAICS match", "Keyword relevance"));
            matches.add(match);
        }
        
        return matches;
    }
    
    private List<DashboardMetrics.TimeSeriesData> getOpportunitiesTimeSeries(int days) {
        List<DashboardMetrics.TimeSeriesData> timeSeries = new ArrayList<>();
        
        for (int i = days; i >= 0; i--) {
            DashboardMetrics.TimeSeriesData data = new DashboardMetrics.TimeSeriesData();
            data.setDate(LocalDateTime.now().minusDays(i));
            data.setCount((int) (Math.random() * 10) + 1);
            timeSeries.add(data);
        }
        
        return timeSeries;
    }
    
    private List<DashboardMetrics.TimeSeriesData> getMatchesTimeSeries(int days) {
        List<DashboardMetrics.TimeSeriesData> timeSeries = new ArrayList<>();
        
        for (int i = days; i >= 0; i--) {
            DashboardMetrics.TimeSeriesData data = new DashboardMetrics.TimeSeriesData();
            data.setDate(LocalDateTime.now().minusDays(i));
            data.setCount((int) (Math.random() * 5) + 1);
            timeSeries.add(data);
        }
        
        return timeSeries;
    }
    
    private DashboardMetrics getEmptyMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();
        metrics.setTotalOpportunities(0);
        metrics.setTotalMatches(0);
        metrics.setTotalProposals(0);
        metrics.setActiveWorkflows(0);
        metrics.setLastUpdated(LocalDateTime.now());
        return metrics;
    }
}