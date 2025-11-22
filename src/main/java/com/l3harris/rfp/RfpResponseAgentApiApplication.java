package com.l3harris.rfp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main Spring Boot application for RFP Response Agent API
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RfpResponseAgentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RfpResponseAgentApiApplication.class, args);
    }
}