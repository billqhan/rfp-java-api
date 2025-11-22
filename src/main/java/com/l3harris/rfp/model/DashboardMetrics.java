package com.l3harris.rfp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard metrics model
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardMetrics {
    
    // Overview counts
    @JsonProperty("total_opportunities")
    private Integer totalOpportunities;
    
    @JsonProperty("total_matches")
    private Integer totalMatches;
    
    @JsonProperty("total_proposals")
    private Integer totalProposals;
    
    @JsonProperty("active_workflows")
    private Integer activeWorkflows;
    
    // Status breakdown
    @JsonProperty("opportunities_by_status")
    private StatusBreakdown opportunitiesByStatus;
    
    @JsonProperty("proposals_by_status")
    private StatusBreakdown proposalsByStatus;
    
    // Recent activity
    @JsonProperty("recent_opportunities")
    private List<Opportunity> recentOpportunities;
    
    @JsonProperty("recent_matches")
    private List<Match> recentMatches;
    
    // Performance metrics
    @JsonProperty("avg_match_score")
    private Double avgMatchScore;
    
    @JsonProperty("processing_time_avg")
    private Double processingTimeAvg;
    
    // Time series data
    @JsonProperty("opportunities_over_time")
    private List<TimeSeriesData> opportunitiesOverTime;
    
    @JsonProperty("matches_over_time")
    private List<TimeSeriesData> matchesOverTime;
    
    // Last update
    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusBreakdown {
        private Integer draft = 0;
        private Integer active = 0;
        private Integer submitted = 0;
        private Integer awarded = 0;
        private Integer closed = 0;
        private Integer cancelled = 0;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimeSeriesData {
        private LocalDateTime date;
        private Integer count;
        private Double value;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Match {
        @JsonProperty("match_id")
        private String matchId;
        
        @JsonProperty("opportunity_id")
        private String opportunityId;
        
        @JsonProperty("opportunity_title")
        private String opportunityTitle;
        
        @JsonProperty("match_score")
        private Double matchScore;
        
        @JsonProperty("confidence_level")
        private String confidenceLevel;
        
        @JsonProperty("match_date")
        private LocalDateTime matchDate;
        
        private List<String> reasons;
    }
}