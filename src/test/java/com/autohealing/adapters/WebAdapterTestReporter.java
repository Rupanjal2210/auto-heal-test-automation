package com.autohealing.adapters;

import com.autohealing.reporting.HealingReporter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Custom test report generator for WebAdapterLoginTest
 * Combines healing reports with test-specific information
 */
public class WebAdapterTestReporter {
    
    private static final Logger logger = Logger.getLogger(WebAdapterTestReporter.class.getName());
    
    private final HealingReporter healingReporter;
    private final String testSessionId;
    private final LocalDateTime testStartTime;
    private WebDriver driver;
    
    public WebAdapterTestReporter(HealingReporter healingReporter, String testSessionId, 
                                 LocalDateTime testStartTime, WebDriver driver) {
        this.healingReporter = healingReporter;
        this.testSessionId = testSessionId;
        this.testStartTime = testStartTime;
        this.driver = driver;
    }
    
    /**
     * Generate a comprehensive test report combining healing data and test results
     */
    public void generateTestReport(String testName, boolean testPassed, String testDetails) {
        try {
            String reportDir = "test-reports/healing-reports";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportPath = reportDir + "/WebAdapter_" + testName + "_Report_" + timestamp + ".html";
            
            // Ensure directory exists
            Path path = Paths.get(reportPath);
            Files.createDirectories(path.getParent());
            
            // Take screenshot if driver is available
            String screenshotPath = null;
            if (driver != null) {
                screenshotPath = captureScreenshot(testName, timestamp);
            }
            
            // Generate HTML report
            try (FileWriter writer = new FileWriter(reportPath)) {
                writer.write(generateTestHtmlReport(testName, testPassed, testDetails, screenshotPath));
            }
            
            logger.info("Test report generated: " + reportPath);
            
        } catch (IOException e) {
            logger.severe("Failed to generate test report: " + e.getMessage());
        }
    }
    
    /**
     * Capture screenshot for the report
     */
    private String captureScreenshot(String testName, String timestamp) {
        try {
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String screenshotDir = "test-reports/screenshots";
                String screenshotPath = screenshotDir + "/WebAdapter_" + testName + "_" + timestamp + ".png";
                
                Path path = Paths.get(screenshotPath);
                Files.createDirectories(path.getParent());
                Files.write(path, screenshot);
                
                logger.info("Screenshot captured: " + screenshotPath);
                return screenshotPath;
            }
        } catch (Exception e) {
            logger.warning("Failed to capture screenshot: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Generate HTML report content
     */
    private String generateTestHtmlReport(String testName, boolean testPassed, 
                                        String testDetails, String screenshotPath) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\\n<html>\\n<head>\\n");
        html.append("<title>WebAdapter Test Report - ").append(testName).append("</title>\\n");
        html.append("<style>\\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\\n");
        html.append("h1 { color: #333; border-bottom: 2px solid #007acc; }\\n");
        html.append("h2 { color: #555; margin-top: 30px; }\\n");
        html.append(".header { background: linear-gradient(135deg, #007acc, #0056b3); color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\\n");
        html.append(".test-info { background: white; padding: 15px; border-radius: 8px; margin: 10px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\\n");
        html.append(".status-pass { color: #28a745; font-weight: bold; }\\n");
        html.append(".status-fail { color: #dc3545; font-weight: bold; }\\n");
        html.append(".metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }\\n");
        html.append(".metric-card { background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }\\n");
        html.append(".metric-value { font-size: 24px; font-weight: bold; color: #007acc; }\\n");
        html.append(".metric-label { font-size: 12px; color: #666; margin-top: 5px; }\\n");
        html.append(".screenshot { max-width: 100%; border: 1px solid #ddd; border-radius: 8px; margin: 10px 0; }\\n");
        html.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\\n");
        html.append("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }\\n");
        html.append("th { background-color: #007acc; color: white; }\\n");
        html.append("tr:hover { background-color: #f8f9fa; }\\n");
        html.append("</style>\\n");
        html.append("</head>\\n<body>\\n");
        
        // Header
        html.append("<div class='header'>\\n");
        html.append("<h1>WebAdapter Auto-Healing Test Report</h1>\\n");
        html.append("<p>Test: ").append(testName).append(" | Session: ").append(testSessionId).append("</p>\\n");
        html.append("<p>Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\\n");
        html.append("</div>\\n");
        
        // Test Information
        html.append("<div class='test-info'>\\n");
        html.append("<h2>Test Summary</h2>\\n");
        html.append("<p><strong>Test Name:</strong> ").append(testName).append("</p>\\n");
        html.append("<p><strong>Status:</strong> <span class='").append(testPassed ? "status-pass'>PASSED" : "status-fail'>FAILED").append("</span></p>\\n");
        html.append("<p><strong>Start Time:</strong> ").append(testStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\\n");
        html.append("<p><strong>Test Details:</strong> ").append(testDetails != null ? testDetails : "No additional details").append("</p>\\n");
        html.append("</div>\\n");
        
        // Healing Metrics
        html.append("<h2>Auto-Healing Metrics</h2>\\n");
        html.append("<div class='metrics'>\\n");
        html.append("<div class='metric-card'>\\n");
        html.append("<div class='metric-value'>").append(healingReporter.getTotalAttempts()).append("</div>\\n");
        html.append("<div class='metric-label'>Total Healing Attempts</div>\\n");
        html.append("</div>\\n");
        html.append("<div class='metric-card'>\\n");
        html.append("<div class='metric-value'>").append(healingReporter.getSuccessfulHeals()).append("</div>\\n");
        html.append("<div class='metric-label'>Successful Heals</div>\\n");
        html.append("</div>\\n");
        html.append("<div class='metric-card'>\\n");
        html.append("<div class='metric-value'>").append(healingReporter.getFailedHeals()).append("</div>\\n");
        html.append("<div class='metric-label'>Failed Heals</div>\\n");
        html.append("</div>\\n");
        html.append("<div class='metric-card'>\\n");
        html.append("<div class='metric-value'>").append(String.format("%.1f%%", healingReporter.getSuccessRate())).append("</div>\\n");
        html.append("<div class='metric-label'>Success Rate</div>\\n");
        html.append("</div>\\n");
        html.append("<div class='metric-card'>\\n");
        html.append("<div class='metric-value'>").append(String.format("%.0fms", healingReporter.getAverageHealingTime())).append("</div>\\n");
        html.append("<div class='metric-label'>Avg Healing Time</div>\\n");
        html.append("</div>\\n");
        html.append("</div>\\n");
        
        // Screenshot
        if (screenshotPath != null) {
            html.append("<h2>Test Screenshot</h2>\\n");
            html.append("<div class='test-info'>\\n");
            html.append("<img src='../screenshots/").append(Paths.get(screenshotPath).getFileName()).append("' class='screenshot' alt='Test Screenshot'>\\n");
            html.append("</div>\\n");
        }
        
        // Test Environment Info
        html.append("<h2>Test Environment</h2>\\n");
        html.append("<div class='test-info'>\\n");
        html.append("<table>\\n");
        html.append("<tr><th>Property</th><th>Value</th></tr>\\n");
        html.append("<tr><td>URL Tested</td><td>https://www.makemytrip.com/login/</td></tr>\\n");
        html.append("<tr><td>Browser</td><td>Chrome (WebDriver managed)</td></tr>\\n");
        html.append("<tr><td>Platform</td><td>WEB</td></tr>\\n");
        html.append("<tr><td>Healing Enabled</td><td>Yes</td></tr>\\n");
        html.append("<tr><td>Max Healing Attempts</td><td>3</td></tr>\\n");
        html.append("<tr><td>Java Version</td><td>").append(System.getProperty("java.version")).append("</td></tr>\\n");
        html.append("<tr><td>OS</td><td>").append(System.getProperty("os.name")).append("</td></tr>\\n");
        html.append("</table>\\n");
        html.append("</div>\\n");
        
        // Footer
        html.append("<div style='margin-top: 40px; text-align: center; color: #666; font-size: 12px;'>\\n");
        html.append("<p>Generated by Auto-Healing Framework Test Reporter</p>\\n");
        html.append("</div>\\n");
        
        html.append("</body>\\n</html>");
        
        return html.toString();
    }
}
