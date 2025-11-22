package com.l3harris.rfp.controller;

import com.l3harris.rfp.model.Proposal;
import com.l3harris.rfp.service.ProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for proposal management
 */
@RestController
@RequestMapping("/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProposals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo) {
        
        List<Proposal> proposals = proposalService.getAllProposals(page, pageSize);
        int total = proposalService.getTotalCount();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        Map<String, Object> response = Map.of(
            "total", total,
            "page", page,
            "pageSize", pageSize,
            "totalPages", totalPages,
            "items", proposals
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{proposalId}")
    public ResponseEntity<Proposal> getProposalById(@PathVariable String proposalId) {
        return proposalService.getProposalById(proposalId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Proposal> createProposal(@RequestBody Proposal proposal) {
        Proposal created = proposalService.createProposal(proposal);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{proposalId}")
    public ResponseEntity<Proposal> updateProposal(
            @PathVariable String proposalId,
            @RequestBody Proposal proposal) {
        
        proposal.setProposalId(proposalId);
        Proposal updated = proposalService.updateProposal(proposalId, proposal);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{proposalId}")
    public ResponseEntity<Map<String, String>> deleteProposal(@PathVariable String proposalId) {
        proposalService.deleteProposal(proposalId);
        
        Map<String, String> response = Map.of(
            "success", "true",
            "message", "Proposal deleted successfully"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-opportunity/{opportunityId}")
    public ResponseEntity<List<Proposal>> getProposalsByOpportunity(@PathVariable String opportunityId) {
        List<Proposal> proposals = proposalService.getProposalsByOpportunity(opportunityId);
        return ResponseEntity.ok(proposals);
    }
}