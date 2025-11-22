package com.l3harris.rfp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST controller for reports management
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private Map<String, Object> createReport(String id, String type, String title, String generatedDate, 
            String summary, int opportunities, int matches, boolean emailSent, String status) {
        Map<String, Object> report = new java.util.HashMap<>();
        report.put("id", id);
        report.put("type", type);
        report.put("title", title);
        report.put("generatedDate", generatedDate);
        report.put("summary", summary);
        report.put("opportunities", opportunities);
        report.put("matches", matches);
        report.put("emailSent", emailSent);
        report.put("status", status);
        report.put("viewUrl", "http://100.26.216.137:8080/api/reports/" + type + "/" + id + "/view");
        report.put("downloadUrl", "http://100.26.216.137:8080/api/reports/" + type + "/" + id + "/download");
        return report;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReports(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        log.info("Getting reports: type={}, range={}, page={}, pageSize={}", type, range, page, pageSize);
        
        // Mock report data
        List<Map<String, Object>> reports = List.of(
            createReport("report-001", "web", "Daily Opportunities Dashboard - " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                LocalDateTime.now().minusHours(2).toString(),
                "Analysis of 67 new opportunities with 14 high-quality matches identified",
                67, 14, true, "completed"),
            createReport("report-002", "user", "Response Template - Advanced Radar Systems",
                LocalDateTime.now().minusDays(1).toString(),
                "Generated response template for opportunity opp-001 with 92% match score",
                1, 1, false, "completed"),
            createReport("report-003", "web", "Weekly Summary Report - Java 21 Migration",
                LocalDateTime.now().minusDays(2).toString(),
                "Weekly analysis showing system upgrade to Java 21 and improved performance metrics",
                45, 8, true, "completed"),
            createReport("report-004", "user", "Match Analysis - Cloud Infrastructure Services",
                LocalDateTime.now().minusDays(3).toString(),
                "Detailed analysis of cloud infrastructure opportunities with L3Harris capabilities alignment",
                12, 5, false, "completed")
        );
        
        // Filter by type if specified
        List<Map<String, Object>> filteredReports = reports;
        if (!"all".equals(type)) {
            filteredReports = reports.stream()
                .filter(report -> type.equals(report.get("type")))
                .toList();
        }
        
        int total = filteredReports.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        // Paginate
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pagedReports = filteredReports.subList(start, end);
        
        Map<String, Object> response = Map.of(
            "items", pagedReports,
            "total", total,
            "page", page,
            "pageSize", pageSize,
            "totalPages", totalPages
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatestReport() {
        Map<String, Object> latestReport = Map.of(
            "id", "report-latest",
            "type", "web",
            "title", "Latest Daily Dashboard - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
            "generatedDate", LocalDateTime.now().toString(),
            "summary", "Most recent analysis with live data from Java 21 upgraded system",
            "opportunities", 23,
            "matches", 6,
            "emailSent", false,
            "status", "generating"
        );
        
        return ResponseEntity.ok(latestReport);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Map<String, Object>>> getReportsByDate(@PathVariable String date) {
        log.info("Getting reports for date: {}", date);
        
        List<Map<String, Object>> reports = List.of(
            Map.of(
                "id", "report-" + date,
                "type", "web",
                "title", "Daily Report - " + date,
                "generatedDate", date + "T08:00:00Z",
                "summary", "Daily opportunities analysis for " + date,
                "opportunities", 45,
                "matches", 9
            )
        );
        
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getReportsHistory(
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("Getting reports history for {} days", days);
        
        List<Map<String, Object>> history = List.of(
            Map.of(
                "date", LocalDateTime.now().minusDays(1).toLocalDate().toString(),
                "reportCount", 3,
                "opportunities", 67,
                "matches", 14
            ),
            Map.of(
                "date", LocalDateTime.now().minusDays(2).toLocalDate().toString(),
                "reportCount", 2,
                "opportunities", 45,
                "matches", 8
            )
        );
        
        return ResponseEntity.ok(history);
    }

    @PostMapping("/web")
    public ResponseEntity<Map<String, Object>> generateWebReport(@RequestBody Map<String, Object> params) {
        log.info("Generating web report with params: {}", params);
        
        Map<String, Object> response = Map.of(
            "reportId", "web-" + System.currentTimeMillis(),
            "status", "generating",
            "message", "Web report generation started"
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> generateUserReport(@RequestBody Map<String, Object> params) {
        log.info("Generating user report with params: {}", params);
        
        Map<String, Object> response = Map.of(
            "reportId", "user-" + System.currentTimeMillis(),
            "status", "generating",
            "message", "User report generation started"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/web/{reportId}/view")
    public ResponseEntity<String> viewWebReport(@PathVariable String reportId) {
        log.info("Viewing web report: {}", reportId);
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>RFP Response Agent - Web Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f9fafb; }
                    .header { background: #1f2937; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
                    .content { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                    .metric { display: inline-block; margin: 10px 20px 10px 0; padding: 15px; background: #f3f4f6; border-radius: 8px; text-align: center; }
                    .metric h3 { margin: 0; color: #1f2937; font-size: 2em; }
                    .metric p { margin: 5px 0 0 0; color: #6b7280; }
                    .section { margin: 20px 0; }
                    .opportunities { background: #ecfdf5; padding: 15px; border-radius: 8px; border-left: 4px solid #10b981; }
                    .matches { background: #eff6ff; padding: 15px; border-radius: 8px; border-left: 4px solid #3b82f6; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>🎯 RFP Response Agent - Daily Report</h1>
                    <p>Generated on Java 21 Platform | Report ID: %s</p>
                </div>
                <div class="content">
                    <div class="section">
                        <h2>📊 Executive Summary</h2>
                        <p>Comprehensive analysis of new opportunities and capability matches for L3Harris Technologies. Our AI-powered system has identified high-value opportunities aligned with your core competencies.</p>
                    </div>
                    
                    <div class="section">
                        <h2>🔢 Key Performance Metrics</h2>
                        <div class="metric">
                            <h3>67</h3>
                            <p>New Opportunities</p>
                        </div>
                        <div class="metric">
                            <h3>14</h3>
                            <p>High-Quality Matches</p>
                        </div>
                        <div class="metric">
                            <h3>92%%</h3>
                            <p>Average Match Score</p>
                        </div>
                        <div class="metric">
                            <h3>$2.4M</h3>
                            <p>Total Opportunity Value</p>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="opportunities">
                            <h3>🚀 Top Opportunities</h3>
                            <ul>
                                <li><strong>Advanced Radar Systems</strong> - DoD Contract (Match: 94%%)</li>
                                <li><strong>Satellite Communications</strong> - NASA Partnership (Match: 89%%)</li>
                                <li><strong>Cyber Security Solutions</strong> - DHS Initiative (Match: 87%%)</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="section">
                        <div class="matches">
                            <h3>🎯 AI Matching Insights</h3>
                            <p>Our Java 21 powered AI system analyzed opportunity requirements against L3Harris capabilities:</p>
                            <ul>
                                <li>✅ Technology alignment: 94%% match rate</li>
                                <li>✅ Geographic preferences: 89%% favorable</li>
                                <li>✅ Contract history: Strong correlation</li>
                                <li>✅ Capability fit: Excellent alignment</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>⚙️ System Information</h2>
                        <p><strong>Platform:</strong> Java 21 LTS on AWS ECS Fargate<br>
                        <strong>Generated:</strong> %s<br>
                        <strong>Processing Time:</strong> 2.3 seconds<br>
                        <strong>Data Sources:</strong> SAM.gov, FedBizOpps, Internal DB</p>
                    </div>
                </div>
            </body>
            </html>
            """, reportId, LocalDateTime.now());
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html")
            .body(htmlContent);
    }

    @GetMapping("/user/{reportId}/view")
    public ResponseEntity<String> viewUserReport(@PathVariable String reportId) {
        log.info("Viewing user report: {}", reportId);
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>RFP Response Agent - User Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f9fafb; }
                    .header { background: #7c3aed; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
                    .content { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                    .opportunity { background: #f0f9ff; padding: 15px; border-radius: 8px; border-left: 4px solid #0ea5e9; margin: 15px 0; }
                    .recommendation { background: #f0fdf4; padding: 15px; border-radius: 8px; border-left: 4px solid #22c55e; margin: 15px 0; }
                    .score { font-size: 2em; font-weight: bold; color: #22c55e; }
                    .section { margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>📋 RFP Response Agent - User Report</h1>
                    <p>Detailed Analysis | Report ID: %s</p>
                </div>
                <div class="content">
                    <div class="section">
                        <h2>🎯 Opportunity Analysis</h2>
                        <div class="opportunity">
                            <h3>Advanced Radar Systems Development</h3>
                            <p><strong>Opportunity ID:</strong> OPP-2025-001<br>
                            <strong>Agency:</strong> Department of Defense<br>
                            <strong>Estimated Value:</strong> $1.2M<br>
                            <strong>Submission Deadline:</strong> December 15, 2025</p>
                            <p><strong>Match Score:</strong> <span class="score">92%%</span></p>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>💡 AI Recommendation</h2>
                        <div class="recommendation">
                            <h3>Strong Recommendation: PROCEED</h3>
                            <p>Exceptional alignment with L3Harris core capabilities in radar and defense systems. This opportunity represents an ideal match for your organization's expertise.</p>
                            
                            <h4>Key Alignment Points:</h4>
                            <ul>
                                <li>✅ <strong>Technology Match:</strong> 96%% - Advanced radar technology requirements</li>
                                <li>✅ <strong>Experience Match:</strong> 94%% - Similar past contract performance</li>
                                <li>✅ <strong>Capability Match:</strong> 92%% - Core competency alignment</li>
                                <li>✅ <strong>Geographic Match:</strong> 88%% - Favorable location preferences</li>
                            </ul>
                        </div>
                    </div>
                    
                    <div class="section">
                        <h2>📝 Recommended Next Steps</h2>
                        <ol>
                            <li><strong>Immediate Actions (Next 24-48 hours)</strong>
                                <ul>
                                    <li>Review complete technical requirements document</li>
                                    <li>Identify internal subject matter experts</li>
                                    <li>Assess resource availability for proposal development</li>
                                </ul>
                            </li>
                            <li><strong>Short-term Actions (Next 1-2 weeks)</strong>
                                <ul>
                                    <li>Prepare detailed capability statement</li>
                                    <li>Coordinate with proposal development team</li>
                                    <li>Research competitor analysis</li>
                                </ul>
                            </li>
                            <li><strong>Proposal Development (3-4 weeks)</strong>
                                <ul>
                                    <li>Develop technical approach</li>
                                    <li>Prepare cost proposal</li>
                                    <li>Final review and submission</li>
                                </ul>
                            </li>
                        </ol>
                    </div>
                    
                    <div class="section">
                        <h2>⚙️ Analysis Details</h2>
                        <p><strong>Generated by:</strong> RFP Response Agent (Java 21)<br>
                        <strong>Analysis Date:</strong> %s<br>
                        <strong>Confidence Level:</strong> High (92%%)<br>
                        <strong>Data Sources:</strong> SAM.gov, Historical Performance, Capability Matrix</p>
                    </div>
                </div>
            </body>
            </html>
            """, reportId, LocalDateTime.now());
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html")
            .body(htmlContent);
    }

    @GetMapping("/web/{reportId}/download")
    public ResponseEntity<String> downloadWebReport(@PathVariable String reportId) {
        log.info("Downloading web report: {}", reportId);
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>RFP Response Agent - Web Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; }
                    .header { background: #1f2937; color: white; padding: 20px; border-radius: 8px; }
                    .content { margin: 20px 0; }
                    .metric { display: inline-block; margin: 10px; padding: 15px; background: #f3f4f6; border-radius: 8px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>RFP Response Agent - Daily Report</h1>
                    <p>Generated on Java 21 Platform</p>
                </div>
                <div class="content">
                    <h2>Executive Summary</h2>
                    <p>Analysis of opportunities and matches for L3Harris Technologies.</p>
                    
                    <h2>Key Metrics</h2>
                    <div class="metric">
                        <h3>67</h3>
                        <p>New Opportunities</p>
                    </div>
                    <div class="metric">
                        <h3>14</h3>
                        <p>High-Quality Matches</p>
                    </div>
                    <div class="metric">
                        <h3>92%</h3>
                        <p>Average Match Score</p>
                    </div>
                    
                    <h2>System Information</h2>
                    <p>Platform: Java 21 (LTS) on AWS ECS Fargate</p>
                    <p>Generated: """ + LocalDateTime.now() + """
                    </p>
                </div>
            </body>
            </html>
            """;
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/html")
            .header("Content-Disposition", "attachment; filename=\"report-" + reportId + ".html\"")
            .body(htmlContent);
    }

    @GetMapping("/user/{reportId}/download")
    public ResponseEntity<String> downloadUserReport(@PathVariable String reportId) {
        log.info("Downloading user report: {}", reportId);
        
        String content = """
            RFP Response Agent - User Report
            ===============================
            
            Report ID: %s
            Generated: %s
            Platform: Java 21 on AWS ECS Fargate
            
            OPPORTUNITY ANALYSIS
            -------------------
            - Opportunity ID: OPP-2025-001
            - Title: Advanced Radar Systems Development
            - Agency: Department of Defense
            - Match Score: 92%%
            
            RECOMMENDATION
            -------------
            Strong alignment with L3Harris capabilities in radar and defense systems.
            Recommended for immediate proposal development.
            
            NEXT STEPS
          - Review technical requirements
            - Prepare capability statement
            - Coordinate with subject matter experts
            
            Generated by RFP Response Agent (Java 21)
            """.formatted(reportId, LocalDateTime.now());
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/plain")
            .header("Content-Disposition", "attachment; filename=\"user-report-" + reportId + ".txt\"")
            .body(content);
    }
}