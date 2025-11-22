package com.l3harris.rfp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Proposal model for RFP responses
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Proposal {
    
    @JsonProperty("proposal_id")
    private String proposalId;
    
    @JsonProperty("opportunity_id")
    private String opportunityId;
    
    private String title;
    private String description;
    private String status; // DRAFT, SUBMITTED, ACCEPTED, REJECTED
    
    @JsonProperty("assigned_to")
    private String assignedTo;
    
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
    
    @JsonProperty("last_modified")
    private LocalDateTime lastModified;
    
    @JsonProperty("due_date")
    private LocalDateTime dueDate;
    
    @JsonProperty("submitted_date")
    private LocalDateTime submittedDate;
    
    // Content
    private String content;
    
    @JsonProperty("executive_summary")
    private String executiveSummary;
    
    @JsonProperty("technical_approach")
    private String technicalApproach;
    
    @JsonProperty("management_approach")
    private String managementApproach;
    
    @JsonProperty("past_performance")
    private String pastPerformance;
    
    // Pricing
    @JsonProperty("proposed_price")
    private Double proposedPrice;
    
    @JsonProperty("cost_breakdown")
    private Map<String, Object> costBreakdown;
    
    // Metadata
    private Map<String, Object> metadata;
    
    // Versioning
    private Integer version = 1;
    
    @JsonProperty("parent_version")
    private String parentVersion;
    
    // Storage metadata
    @JsonProperty("storage_location")
    private String storageLocation; // LOCAL, CLOUD, BOTH
    
    @JsonProperty("sync_status")
    private String syncStatus; // SYNCED, PENDING, FAILED
}