package com.l3harris.rfp.service;

import com.l3harris.rfp.model.Proposal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing proposals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProposalService {

    private final DynamoDbClient dynamoDbClient;

    public List<Proposal> getAllProposals(int page, int pageSize) {
        log.info("Fetching proposals page {} with size {}", page, pageSize);
        
        // TODO: Implement DynamoDB fetching
        
        return generateSampleProposals(pageSize);
    }

    public Optional<Proposal> getProposalById(String proposalId) {
        log.info("Fetching proposal by ID: {}", proposalId);
        
        // TODO: Implement DynamoDB single item fetch
        
        if ("sample-proposal-id".equals(proposalId)) {
            return Optional.of(generateSampleProposal(proposalId));
        }
        
        return Optional.empty();
    }

    public Proposal createProposal(Proposal proposal) {
        log.info("Creating new proposal for opportunity: {}", proposal.getOpportunityId());
        
        // Set metadata
        proposal.setCreatedDate(LocalDateTime.now());
        proposal.setLastModified(LocalDateTime.now());
        proposal.setStatus("DRAFT");
        proposal.setVersion(1);
        
        // TODO: Save to DynamoDB
        
        return proposal;
    }

    public Proposal updateProposal(String proposalId, Proposal proposal) {
        log.info("Updating proposal: {}", proposalId);
        
        // Update metadata
        proposal.setLastModified(LocalDateTime.now());
        proposal.setVersion(proposal.getVersion() + 1);
        
        // TODO: Update in DynamoDB
        
        return proposal;
    }

    public void deleteProposal(String proposalId) {
        log.info("Deleting proposal: {}", proposalId);
        
        // TODO: Delete from DynamoDB
    }

    public List<Proposal> getProposalsByOpportunity(String opportunityId) {
        log.info("Fetching proposals for opportunity: {}", opportunityId);
        
        // TODO: Query DynamoDB by GSI
        
        return generateSampleProposals(3);
    }

    public Integer getTotalCount() {
        // TODO: Implement actual count from DynamoDB
        return 35; // Sample count
    }

    private List<Proposal> generateSampleProposals(int count) {
        List<Proposal> proposals = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            proposals.add(generateSampleProposal("proposal-" + i));
        }
        
        return proposals;
    }

    private Proposal generateSampleProposal(String id) {
        Proposal proposal = new Proposal();
        proposal.setProposalId(id);
        proposal.setOpportunityId("opp-" + id.hashCode());
        proposal.setTitle("Proposal for Advanced Technology Solutions " + id);
        proposal.setDescription("Sample proposal description for " + id);
        proposal.setStatus("DRAFT");
        proposal.setAssignedTo("john.smith@l3harris.com");
        proposal.setCreatedDate(LocalDateTime.now().minusDays(3));
        proposal.setLastModified(LocalDateTime.now().minusHours(6));
        proposal.setDueDate(LocalDateTime.now().plusDays(25));
        proposal.setProposedPrice(800000.0 + (Math.random() * 2000000));
        proposal.setVersion(1);
        proposal.setStorageLocation("BOTH");
        proposal.setSyncStatus("SYNCED");
        
        return proposal;
    }
}