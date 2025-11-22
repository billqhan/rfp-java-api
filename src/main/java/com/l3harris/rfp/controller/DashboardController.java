package com.l3harris.rfp.controller;

import com.l3harris.rfp.model.DashboardMetrics;
import com.l3harris.rfp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard metrics and overview controller
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetrics> getMetrics() {
        DashboardMetrics metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
}