package com.l3harris.rfp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workflow execution model
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExecution {
    
    @JsonProperty("execution_id")
    private String executionId;
    
    private String step; // download, process, match, reports, notify
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    
    @JsonProperty("start_time")
    private LocalDateTime startTime;
    
    @JsonProperty("end_time")
    private LocalDateTime endTime;
    
    @JsonProperty("duration_ms")
    private Long durationMs;
    
    // Input/Output
    private Map<String, Object> input;
    private Map<String, Object> output;
    
    // Error handling
    @JsonProperty("error_message")
    private String errorMessage;
    
    @JsonProperty("error_details")
    private Map<String, Object> errorDetails;
    
    // Metrics
    @JsonProperty("items_processed")
    private Integer itemsProcessed;
    
    @JsonProperty("items_success")
    private Integer itemsSuccess;
    
    @JsonProperty("items_failed")
    private Integer itemsFailed;
    
    // Metadata
    @JsonProperty("lambda_function")
    private String lambdaFunction;
    
    @JsonProperty("request_id")
    private String requestId;
    
    private Map<String, Object> metadata;
}