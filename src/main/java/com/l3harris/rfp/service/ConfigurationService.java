package com.l3harris.rfp.service;

import com.l3harris.rfp.config.ApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Configuration service for AWS resource naming and management
 */
@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final ApiProperties apiProperties;

    public String getDownloadFunctionName() {
        String configured = apiProperties.getAws().getLambda().getDownloadFunction();
        return configured != null ? configured :
            String.format("%s-sam-gov-daily-download-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getProcessFunctionName() {
        String configured = apiProperties.getAws().getLambda().getProcessFunction();
        return configured != null ? configured :
            String.format("%s-sam-json-processor-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getMatchFunctionName() {
        String configured = apiProperties.getAws().getLambda().getMatchFunction();
        return configured != null ? configured :
            String.format("%s-sam-sqs-generate-match-reports-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getReportFunctionName() {
        String configured = apiProperties.getAws().getLambda().getReportFunction();
        return configured != null ? configured :
            String.format("%s-sam-produce-web-reports-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getNotifyFunctionName() {
        String configured = apiProperties.getAws().getLambda().getNotifyFunction();
        return configured != null ? configured :
            String.format("%s-sam-daily-email-notification-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getProposalsTableName() {
        String configured = apiProperties.getAws().getDynamodb().getProposalsTable();
        return configured != null ? configured :
            String.format("%s-sam-proposals-%s",
                apiProperties.getAws().getProjectPrefix(),
                apiProperties.getAws().getEnvironment());
    }

    public String getBucketPrefix() {
        String configured = apiProperties.getAws().getS3().getBucketPrefix();
        return configured != null ? configured : apiProperties.getAws().getProjectPrefix();
    }

    public String getOpportunitiesBucket() {
        return String.format("%s-sam-opportunities", getBucketPrefix());
    }

    public String getMatchesBucket() {
        return String.format("%s-sam-matches", getBucketPrefix());
    }

    public String getReportsBucket() {
        return String.format("%s-sam-reports", getBucketPrefix());
    }

    public String getFunctionNameForStep(String step) {
        return switch (step.toLowerCase()) {
            case "download" -> getDownloadFunctionName();
            case "process" -> getProcessFunctionName();
            case "match" -> getMatchFunctionName();
            case "reports" -> getReportFunctionName();
            case "notify" -> getNotifyFunctionName();
            default -> throw new IllegalArgumentException("Unknown workflow step: " + step);
        };
    }
}