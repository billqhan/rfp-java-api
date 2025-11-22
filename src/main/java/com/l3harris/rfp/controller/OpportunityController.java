package com.l3harris.rfp.controller;

import com.l3harris.rfp.model.Opportunity;
import com.l3harris.rfp.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for opportunity management
 */
@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllOpportunities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String agency,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        List<Opportunity> opportunities;
        
        if (search != null || category != null || agency != null) {
            opportunities = opportunityService.searchOpportunities(search, category, agency);
        } else {
            opportunities = opportunityService.getAllOpportunities(page, pageSize);
        }
        
        int total = opportunityService.getTotalCount();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        Map<String, Object> response = Map.of(
            "total", total,
            "page", page,
            "pageSize", pageSize,
            "totalPages", totalPages,
            "items", opportunities
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Opportunity> getOpportunityById(@PathVariable String id) {
        return opportunityService.getOpportunityById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Opportunity>> searchOpportunities(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String agency) {
        
        List<Opportunity> opportunities = opportunityService.searchOpportunities(query, category, agency);
        return ResponseEntity.ok(opportunities);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = opportunityService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/agencies")
    public ResponseEntity<List<String>> getAgencies() {
        List<String> agencies = opportunityService.getAgencies();
        return ResponseEntity.ok(agencies);
    }
}