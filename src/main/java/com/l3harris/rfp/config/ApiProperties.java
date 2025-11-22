package com.l3harris.rfp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rfp.api")
public class ApiProperties {
    
    private Aws aws = new Aws();
    private Storage storage = new Storage();
    private Processing processing = new Processing();
    
    @Data
    public static class Aws {
        private String region = "us-east-1";
        private String environment = "dev";
        private String projectPrefix = "l3harris-qhan";
        private Lambda lambda = new Lambda();
        private Dynamodb dynamodb = new Dynamodb();
        private S3 s3 = new S3();
        
        @Data
        public static class Lambda {
            private String downloadFunction;
            private String processFunction;
            private String matchFunction;
            private String reportFunction;
            private String notifyFunction;
        }
        
        @Data
        public static class Dynamodb {
            private String proposalsTable;
        }
        
        @Data
        public static class S3 {
            private String bucketPrefix;
        }
    }
    
    @Data
    public static class Storage {
        private boolean enableLocalStorage = true;
        private boolean enableCloudStorage = true;
        private String fallbackStrategy = "local";
    }
    
    @Data
    public static class Processing {
        private double matchThreshold = 0.7;
        private int maxResults = 100;
        private boolean enableKnowledgeBase = false;
        private String companyName = "L3Harris Technologies";
        private int timeout = 30000; // 30 seconds
    }
}