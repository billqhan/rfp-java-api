package com.l3harris.rfp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check and system status controller
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
            "status", "healthy",
            "service", "RFP Response Agent Java API",
            "version", "1.0.0",
            "timestamp", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }
}