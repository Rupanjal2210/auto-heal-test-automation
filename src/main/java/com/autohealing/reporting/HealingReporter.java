package com.autohealing.reporting;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Comprehensive reporting system for auto-healing framework
 * Tracks healing attempts, success rates, and generates detailed reports
 */
public class HealingReporter {
    
    private static final Logger logger = Logger.getLogger(HealingReporter.class.getName());
    
    // Core metrics tracking
    private final AtomicInteger totalAttempts = new AtomicInteger(0);
    private final AtomicInteger successfulHeals = new AtomicInteger(0);
    private final AtomicInteger failedHeals = new AtomicInteger(0);
    
    // Detailed tracking maps
    private final Map<String, Integer> platformSuccessCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> platformFailureCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> strategySuccessCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> strategyFailureCount = new ConcurrentHashMap<>();
    private final Map<String, List<HealingAttempt>> elementHealingHistory = new ConcurrentHashMap<>();
    private final Map<String, Long> healingTimes = new ConcurrentHashMap<>();
    
    // Session information
    private final LocalDateTime sessionStart = LocalDateTime.now();
    private LocalDateTime sessionEnd;
    
    /**
     * Records a successful healing attempt
     */
    public void recordSuccess(String platform, String strategy, String elementId, 
                             String originalLocator, String healedLocator, long healingTimeMs) {
        totalAttempts.incrementAndGet();
        successfulHeals.incrementAndGet();
        
        // Track platform success
        platformSuccessCount.merge(platform, 1, Integer::sum);
        
        // Track strategy success
        strategySuccessCount.merge(strategy, 1, Integer::sum);
        
        // Record healing attempt details
        HealingAttempt attempt = new HealingAttempt(
            elementId, platform, strategy, originalLocator, 
            healedLocator, true, healingTimeMs, LocalDateTime.now(), null
        );
        
        elementHealingHistory.computeIfAbsent(elementId, k -> new ArrayList<>()).add(attempt);
        healingTimes.put(elementId + "_" + System.nanoTime(), healingTimeMs);
        
        logger.info("Healing success recorded: " + elementId + " via " + strategy + " (" + healingTimeMs + "ms)");
    }
    
    /**
     * Records a failed healing attempt
     */
    public void recordFailure(String platform, String strategy, String elementId, 
                             String originalLocator, String errorMessage) {
        totalAttempts.incrementAndGet();
        failedHeals.incrementAndGet();
        
        // Track platform failure
        platformFailureCount.merge(platform, 1, Integer::sum);
        
        // Track strategy failure
        strategyFailureCount.merge(strategy, 1, Integer::sum);
        
        // Record healing attempt details
        HealingAttempt attempt = new HealingAttempt(
            elementId, platform, strategy, originalLocator, 
            null, false, 0, LocalDateTime.now(), errorMessage
        );
        
        elementHealingHistory.computeIfAbsent(elementId, k -> new ArrayList<>()).add(attempt);
        
        logger.warning("Healing failure recorded: " + elementId + " via " + strategy + " - " + errorMessage);
    }
    
    /**
     * Gets the overall healing success rate as a percentage
     */
    public double getSuccessRate() {
        int total = totalAttempts.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulHeals.get() / total * 100.0;
    }
    
    /**
     * Gets the total number of healing attempts
     */
    public int getTotalAttempts() {
        return totalAttempts.get();
    }
    
    /**
     * Gets the number of successful healing attempts
     */
    public int getSuccessfulHeals() {
        return successfulHeals.get();
    }
    
    /**
     * Gets the number of failed healing attempts
     */
    public int getFailedHeals() {
        return failedHeals.get();
    }
    
    /**
     * Gets success rate for a specific platform
     */
    public double getPlatformSuccessRate(String platform) {
        int successes = platformSuccessCount.getOrDefault(platform, 0);
        int failures = platformFailureCount.getOrDefault(platform, 0);
        int total = successes + failures;
        
        if (total == 0) {
            return 0.0;
        }
        return (double) successes / total * 100.0;
    }
    
    /**
     * Gets success rate for a specific strategy
     */
    public double getStrategySuccessRate(String strategy) {
        int successes = strategySuccessCount.getOrDefault(strategy, 0);
        int failures = strategyFailureCount.getOrDefault(strategy, 0);
        int total = successes + failures;
        
        if (total == 0) {
            return 0.0;
        }
        return (double) successes / total * 100.0;
    }
    
    /**
     * Gets average healing time in milliseconds
     */
    public double getAverageHealingTime() {
        if (healingTimes.isEmpty()) {
            return 0.0;
        }
        
        long totalTime = healingTimes.values().stream().mapToLong(Long::longValue).sum();
        return (double) totalTime / healingTimes.size();
    }
    
    /**
     * Gets the most problematic elements (highest failure rate)
     */
    public List<String> getProblematicElements(int limit) {
        Map<String, Double> elementFailureRates = new HashMap<>();
        
        for (Map.Entry<String, List<HealingAttempt>> entry : elementHealingHistory.entrySet()) {
            String elementId = entry.getKey();
            List<HealingAttempt> attempts = entry.getValue();
            
            if (attempts.size() >= 2) { // Only consider elements with multiple attempts
                long failures = attempts.stream().mapToLong(a -> a.successful ? 0 : 1).sum();
                double failureRate = (double) failures / attempts.size() * 100.0;
                elementFailureRates.put(elementId, failureRate);
            }
        }
        
        return elementFailureRates.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Gets the most effective healing strategies
     */
    public List<String> getMostEffectiveStrategies() {
        return strategySuccessCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Generates a comprehensive HTML report
     */
    public void generateReport(String filePath) {
        sessionEnd = LocalDateTime.now();
        
        try {
            // Ensure directory exists
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(generateHtmlReport());
            }
            
            logger.info("Healing report generated successfully: " + filePath);
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to generate healing report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates a JSON report for programmatic consumption
     */
    public void generateJsonReport(String filePath) {
        sessionEnd = LocalDateTime.now();
        
        try {
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(generateJsonReport());
            }
            
            logger.info("JSON healing report generated: " + filePath);
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to generate JSON report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Prints a summary report to the console
     */
    public void printSummary() {
        logger.info("\n" + "=".repeat(60));
        logger.info("AUTO-HEALING SESSION SUMMARY");
        logger.info("=".repeat(60));
        logger.info("Session Duration: " + sessionStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + 
                   " to " + (sessionEnd != null ? sessionEnd.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "ongoing"));
        logger.info("Total Attempts: " + getTotalAttempts());
        logger.info("Successful Heals: " + getSuccessfulHeals());
        logger.info("Failed Heals: " + getFailedHeals());
        logger.info("Success Rate: " + String.format("%.1f%%", getSuccessRate()));
        logger.info("Average Healing Time: " + String.format("%.1fms", getAverageHealingTime()));
        
        if (!platformSuccessCount.isEmpty()) {
            logger.info("\nPlatform Performance:");
            platformSuccessCount.keySet().forEach(platform -> 
                logger.info("  " + platform + ": " + String.format("%.1f%%", getPlatformSuccessRate(platform)))
            );
        }
        
        if (!strategySuccessCount.isEmpty()) {
            logger.info("\nStrategy Effectiveness:");
            getMostEffectiveStrategies().forEach(strategy -> 
                logger.info("  " + strategy + ": " + String.format("%.1f%%", getStrategySuccessRate(strategy)))
            );
        }
        
        List<String> problematic = getProblematicElements(5);
        if (!problematic.isEmpty()) {
            logger.info("\nMost Problematic Elements:");
            problematic.forEach(element -> logger.info("  " + element));
        }
        
        logger.info("=".repeat(60));
    }
    
    /**
     * Generates HTML report content
     */
    private String generateHtmlReport() {
        StringBuilder html = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        html.append("<!DOCTYPE html><html><head>");
        html.append("<title>Auto-Healing Framework Report</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; }");
        html.append(".success { color: green; } .failure { color: red; }");
        html.append(".metric { background-color: #e8f4fd; padding: 10px; margin: 10px 0; border-radius: 5px; }");
        html.append("</style></head><body>");
        
        // Header
        html.append("<h1>Auto-Healing Framework Report</h1>");
        html.append("<div class='metric'>");
        html.append("<h2>Session Overview</h2>");
        html.append("<p><strong>Start Time:</strong> ").append(sessionStart.format(formatter)).append("</p>");
        if (sessionEnd != null) {
            html.append("<p><strong>End Time:</strong> ").append(sessionEnd.format(formatter)).append("</p>");
        }
        html.append("<p><strong>Total Attempts:</strong> ").append(getTotalAttempts()).append("</p>");
        html.append("<p><strong>Successful Heals:</strong> <span class='success'>").append(getSuccessfulHeals()).append("</span></p>");
        html.append("<p><strong>Failed Heals:</strong> <span class='failure'>").append(getFailedHeals()).append("</span></p>");
        html.append("<p><strong>Success Rate:</strong> ").append(String.format("%.1f%%", getSuccessRate())).append("</p>");
        html.append("<p><strong>Average Healing Time:</strong> ").append(String.format("%.1fms", getAverageHealingTime())).append("</p>");
        html.append("</div>");
        
        // Platform performance
        if (!platformSuccessCount.isEmpty()) {
            html.append("<h2>Platform Performance</h2>");
            html.append("<table><tr><th>Platform</th><th>Success Rate</th><th>Attempts</th></tr>");
            platformSuccessCount.keySet().forEach(platform -> {
                int successes = platformSuccessCount.getOrDefault(platform, 0);
                int failures = platformFailureCount.getOrDefault(platform, 0);
                html.append("<tr><td>").append(platform).append("</td>");
                html.append("<td>").append(String.format("%.1f%%", getPlatformSuccessRate(platform))).append("</td>");
                html.append("<td>").append(successes + failures).append("</td></tr>");
            });
            html.append("</table>");
        }
        
        // Strategy effectiveness
        if (!strategySuccessCount.isEmpty()) {
            html.append("<h2>Strategy Effectiveness</h2>");
            html.append("<table><tr><th>Strategy</th><th>Success Rate</th><th>Attempts</th></tr>");
            getMostEffectiveStrategies().forEach(strategy -> {
                int successes = strategySuccessCount.getOrDefault(strategy, 0);
                int failures = strategyFailureCount.getOrDefault(strategy, 0);
                html.append("<tr><td>").append(strategy).append("</td>");
                html.append("<td>").append(String.format("%.1f%%", getStrategySuccessRate(strategy))).append("</td>");
                html.append("<td>").append(successes + failures).append("</td></tr>");
            });
            html.append("</table>");
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    /**
     * Generates JSON report content
     */
    private String generateJsonReport() {
        StringBuilder json = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        json.append("{\n");
        json.append("  \"sessionInfo\": {\n");
        json.append("    \"startTime\": \"").append(sessionStart.format(formatter)).append("\",\n");
        if (sessionEnd != null) {
            json.append("    \"endTime\": \"").append(sessionEnd.format(formatter)).append("\",\n");
        }
        json.append("    \"totalAttempts\": ").append(getTotalAttempts()).append(",\n");
        json.append("    \"successfulHeals\": ").append(getSuccessfulHeals()).append(",\n");
        json.append("    \"failedHeals\": ").append(getFailedHeals()).append(",\n");
        json.append("    \"successRate\": ").append(String.format("%.1f", getSuccessRate())).append(",\n");
        json.append("    \"averageHealingTime\": ").append(String.format("%.1f", getAverageHealingTime())).append("\n");
        json.append("  }");
        
        if (!platformSuccessCount.isEmpty()) {
            json.append(",\n  \"platformPerformance\": {\n");
            boolean first = true;
            for (String platform : platformSuccessCount.keySet()) {
                if (!first) json.append(",\n");
                json.append("    \"").append(platform).append("\": {");
                json.append("\"successRate\": ").append(String.format("%.1f", getPlatformSuccessRate(platform)));
                json.append(", \"attempts\": ").append(platformSuccessCount.get(platform) + platformFailureCount.getOrDefault(platform, 0));
                json.append("}");
                first = false;
            }
            json.append("\n  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    /**
     * Convenience method for recording successful healing - used by AutoHealingEngine
     */
    public void recordSuccessfulHealing(String platform, String elementId, 
                                       String originalLocator, String strategy) {
        recordSuccess(platform, strategy, elementId, originalLocator, "healed", 0);
    }
    
    /**
     * Convenience method for recording failed healing - used by AutoHealingEngine
     */
    public void recordFailedHealing(String platform, String elementId, 
                                   String originalLocator, String errorMessage) {
        recordFailure(platform, "unknown", elementId, originalLocator, errorMessage);
    }
    
    /**
     * Gets comprehensive statistics as a map - used by AutoHealingEngine
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttempts", getTotalAttempts());
        stats.put("successfulHeals", getSuccessfulHeals());
        stats.put("failedHeals", getFailedHeals());
        stats.put("successRate", getSuccessRate());
        stats.put("averageHealingTime", getAverageHealingTime());
        stats.put("platformStats", new HashMap<>(platformSuccessCount));
        stats.put("strategyStats", new HashMap<>(strategySuccessCount));
        stats.put("problematicElements", getProblematicElements(10));
        stats.put("effectiveStrategies", getMostEffectiveStrategies());
        return stats;
    }

    /**
     * Inner class to represent a healing attempt
     */
    private static class HealingAttempt {
        final String elementId;
        final String platform;
        final String strategy;
        final String originalLocator;
        final String healedLocator;
        final boolean successful;
        final long healingTimeMs;
        final LocalDateTime timestamp;
        final String errorMessage;
        
        HealingAttempt(String elementId, String platform, String strategy, String originalLocator,
                      String healedLocator, boolean successful, long healingTimeMs, 
                      LocalDateTime timestamp, String errorMessage) {
            this.elementId = elementId;
            this.platform = platform;
            this.strategy = strategy;
            this.originalLocator = originalLocator;
            this.healedLocator = healedLocator;
            this.successful = successful;
            this.healingTimeMs = healingTimeMs;
            this.timestamp = timestamp;
            this.errorMessage = errorMessage;
        }
    }
}

