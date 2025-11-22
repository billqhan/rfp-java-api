package com.l3harris.rfp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Opportunity model representing RFP/RFQ opportunities
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Opportunity {
    
    private String id;
    private String title;
    private String description;
    private String agency;
    private String department;
    private String category;
    private String type; // RFP, RFQ, etc.
    private String status;
    private String assignedTo;
    
    @JsonProperty("posted_date")
    private LocalDateTime postedDate;
    
    @JsonProperty("due_date")
    private LocalDateTime dueDate;
    
    @JsonProperty("response_date")
    private LocalDateTime responseDate;
    
    @JsonProperty("set_aside_code")
    private String setAsideCode;
    
    @JsonProperty("set_aside")
    private String setAside;
    
    @JsonProperty("naics_code")
    private String naicsCode;
    
    @JsonProperty("naics_description")
    private String naicsDescription;
    
    @JsonProperty("contract_value")
    private Double contractValue;
    
    @JsonProperty("place_of_performance")
    private String placeOfPerformance;
    
    @JsonProperty("procurement_method")
    private String procurementMethod;
    
    @JsonProperty("solicitation_number")
    private String solicitationNumber;
    
    @JsonProperty("award_number")
    private String awardNumber;
    
    @JsonProperty("notice_id")
    private String noticeId;
    
    @JsonProperty("source_url")
    private String sourceUrl;
    
    // Match-related fields
    @JsonProperty("match_score")
    private Double matchScore;
    
    @JsonProperty("match_reasons")
    private List<String> matchReasons;
    
    @JsonProperty("confidence_level")
    private String confidenceLevel;
    
    // Processing metadata
    @JsonProperty("processed_date")
    private LocalDateTime processedDate;
    
    @JsonProperty("last_updated")
    private LocalDateTime lastUpdated;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Contact information
    private Contact contact;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        private String name;
        private String email;
        private String phone;
        private String office;
    }
}