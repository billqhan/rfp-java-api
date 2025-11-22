package com.l3harris.rfp.controller;

import com.l3harris.rfp.model.WorkflowExecution;
import com.l3harris.rfp.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for workflow management
 */
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/{step}")
    public ResponseEntity<WorkflowExecution> triggerWorkflow(
            @PathVariable String step,
            @RequestBody(required = false) Map<String, Object> parameters) {
        
        if (parameters == null) {
            parameters = Map.of();
        }
        
        WorkflowExecution execution = workflowService.triggerWorkflow(step, parameters);
        return ResponseEntity.ok(execution);
    }

    @PostMapping("/download")
    public ResponseEntity<WorkflowExecution> triggerDownload(@RequestBody(required = false) Map<String, Object> parameters) {
        return triggerWorkflow("download", parameters);
    }

    @PostMapping("/process")
    public ResponseEntity<WorkflowExecution> triggerProcess(@RequestBody(required = false) Map<String, Object> parameters) {
        return triggerWorkflow("process", parameters);
    }

    @PostMapping("/match")
    public ResponseEntity<WorkflowExecution> triggerMatch(@RequestBody(required = false) Map<String, Object> parameters) {
        return triggerWorkflow("match", parameters);
    }

    @PostMapping("/reports")
    public ResponseEntity<WorkflowExecution> triggerReports(@RequestBody(required = false) Map<String, Object> parameters) {
        return triggerWorkflow("reports", parameters);
    }

    @PostMapping("/notify")
    public ResponseEntity<WorkflowExecution> triggerNotify(@RequestBody(required = false) Map<String, Object> parameters) {
        return triggerWorkflow("notify", parameters);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus() {
        Map<String, Object> status = workflowService.getWorkflowStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/history")
    public ResponseEntity<List<WorkflowExecution>> getWorkflowHistory(
            @RequestParam(defaultValue = "50") int limit) {
        
        List<WorkflowExecution> history = workflowService.getWorkflowHistory(limit);
        return ResponseEntity.ok(history);
    }
}