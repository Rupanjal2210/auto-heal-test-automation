package com.autohealing.strategies;

import com.autohealing.adapters.PlatformAdapter;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DOM Tree Analysis Strategy for Web Applications
 * Analyzes DOM structure to find elements when original locators fail
 * Similar to Healenium's approach
 */
public class DOMAnalysisStrategy implements HealingStrategy {
    
    private static final Logger logger = Logger.getLogger(DOMAnalysisStrategy.class.getName());
    
    private final Map<String, String> healedLocators = new ConcurrentHashMap<>();
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private final Set<String> supportedPlatforms = Set.of("WEB");
    
    @Override
    public boolean canHandle(String platformType) {
        return supportedPlatforms.contains(platformType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T heal(PlatformAdapter adapter, String elementId, String originalLocator, 
                     Class<T> expectedType, Object context) {
        
        try {
            logger.info("Attempting DOM analysis healing for element: " + elementId);
            
            // Get page source for analysis
            String pageSource = adapter.getPageSource(context);
            if (pageSource == null || pageSource.isEmpty()) {
                return null;
            }
            
            // Parse original locator to understand the search strategy
            LocatorInfo locatorInfo = parseLocator(originalLocator);
            
            // Try different healing approaches
            List<String> candidateLocators = generateCandidateLocators(locatorInfo, pageSource);
            
            for (String candidateLocator : candidateLocators) {
                try {
                    T element = adapter.findElement(candidateLocator, expectedType, context);
                    if (element != null && adapter.isElementDisplayed(element)) {
                        // Validate the element by checking its properties
                        if (validateElement(element, locatorInfo, adapter)) {
                            healedLocators.put(elementId, candidateLocator);
                            incrementMetric("successful_healings");
                            logger.info("Successfully healed element " + elementId + " with locator: " + candidateLocator);
                            return element;
                        }
                    }
                } catch (Exception e) {
                    logger.fine("Candidate locator failed: " + candidateLocator);
                }
            }
            
            incrementMetric("failed_healings");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in DOM analysis healing", e);
            incrementMetric("strategy_errors");
            return null;
        }
    }
    
    @Override
    public int getPriority() {
        return 1; // High priority for web applications
    }
    
    @Override
    public String getStrategyName() {
        return "DOM_ANALYSIS";
    }
    
    @Override
    public String getHealedLocator(String elementId) {
        return healedLocators.get(elementId);
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        logger.info("Initialized DOM Analysis Strategy");
        metrics.put("successful_healings", 0);
        metrics.put("failed_healings", 0);
        metrics.put("strategy_errors", 0);
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }
    
    // Helper methods
    
    private LocatorInfo parseLocator(String locator) {
        LocatorInfo info = new LocatorInfo();
        
        if (locator.startsWith("id=")) {
            info.type = "id";
            info.value = locator.substring(3);
        } else if (locator.startsWith("name=")) {
            info.type = "name";
            info.value = locator.substring(5);
        } else if (locator.startsWith("className=")) {
            info.type = "className";
            info.value = locator.substring(10);
        } else if (locator.startsWith("css=")) {
            info.type = "css";
            info.value = locator.substring(4);
        } else if (locator.startsWith("xpath=")) {
            info.type = "xpath";
            info.value = locator.substring(6);
        } else {
            info.type = "xpath";
            info.value = locator;
        }
        
        return info;
    }
    
    private List<String> generateCandidateLocators(LocatorInfo originalLocator, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        switch (originalLocator.type) {
            case "id":
                candidates.addAll(generateIdBasedCandidates(originalLocator.value, pageSource));
                break;
            case "name":
                candidates.addAll(generateNameBasedCandidates(originalLocator.value, pageSource));
                break;
            case "className":
                candidates.addAll(generateClassBasedCandidates(originalLocator.value, pageSource));
                break;
            case "css":
                candidates.addAll(generateCssBasedCandidates(originalLocator.value, pageSource));
                break;
            case "xpath":
                candidates.addAll(generateXPathBasedCandidates(originalLocator.value, pageSource));
                break;
        }
        
        // Add generic fallback candidates
        candidates.addAll(generateGenericCandidates(originalLocator, pageSource));
        
        return candidates;
    }
    
    private List<String> generateIdBasedCandidates(String originalId, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Try partial ID matches
        if (originalId.length() > 3) {
            String partialId = originalId.substring(0, originalId.length() - 1);
            candidates.add("css=[id*='" + partialId + "']");
            candidates.add("xpath=//*[contains(@id, '" + partialId + "')]");
        }
        
        // Try ID with different patterns (common dynamic ID patterns)
        candidates.add("css=[id^='" + extractIdPrefix(originalId) + "']");
        candidates.add("xpath=//*[starts-with(@id, '" + extractIdPrefix(originalId) + "')]");
        
        return candidates;
    }
    
    private List<String> generateNameBasedCandidates(String originalName, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Try partial name matches
        candidates.add("css=[name*='" + originalName + "']");
        candidates.add("xpath=//*[contains(@name, '" + originalName + "')]");
        
        return candidates;
    }
    
    private List<String> generateClassBasedCandidates(String originalClass, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Handle multiple classes
        String[] classes = originalClass.split("\\s+");
        for (String cls : classes) {
            candidates.add("css=." + cls);
            candidates.add("xpath=//*[contains(@class, '" + cls + "')]");
        }
        
        return candidates;
    }
    
    private List<String> generateCssBasedCandidates(String originalCss, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Try making CSS selectors more flexible
        // Remove specific nth-child selectors
        String flexibleCss = originalCss.replaceAll(":nth-child\\(\\d+\\)", "");
        if (!flexibleCss.equals(originalCss)) {
            candidates.add("css=" + flexibleCss);
        }
        
        // Try without specific IDs but keep classes
        String classOnlyCss = originalCss.replaceAll("#[\\w-]+", "");
        if (!classOnlyCss.equals(originalCss)) {
            candidates.add("css=" + classOnlyCss);
        }
        
        return candidates;
    }
    
    private List<String> generateXPathBasedCandidates(String originalXPath, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Try making XPath more flexible
        // Remove specific position predicates
        String flexibleXPath = originalXPath.replaceAll("\\[\\d+\\]", "");
        if (!flexibleXPath.equals(originalXPath)) {
            candidates.add("xpath=" + flexibleXPath);
        }
        
        // Try relative XPath
        if (originalXPath.startsWith("//")) {
            String relativeXPath = originalXPath.substring(2);
            candidates.add("xpath=.//" + relativeXPath);
        }
        
        // Try text-based XPath if original had text
        if (originalXPath.contains("text()")) {
            String textContent = extractTextFromXPath(originalXPath);
            if (textContent != null) {
                candidates.add("xpath=//*[contains(text(), '" + textContent + "')]");
            }
        }
        
        return candidates;
    }
    
    private List<String> generateGenericCandidates(LocatorInfo originalLocator, String pageSource) {
        List<String> candidates = new ArrayList<>();
        
        // Try common tag names with attributes
        candidates.add("css=input[type='submit']");
        candidates.add("css=button");
        candidates.add("css=a");
        candidates.add("css=div");
        candidates.add("css=span");
        
        // Try by common attributes
        candidates.add("xpath=//*[@role='button']");
        candidates.add("xpath=//*[@type='submit']");
        candidates.add("xpath=//*[@value]");
        
        return candidates;
    }
    
    private boolean validateElement(Object element, LocatorInfo originalLocator, PlatformAdapter adapter) {
        try {
            // Basic validation - element should be displayed
            if (!adapter.isElementDisplayed(element)) {
                return false;
            }
            
            // Get element attributes for validation
            Map<String, String> attributes = adapter.getElementAttributes(element);
            String elementText = adapter.getElementText(element);
            
            // Validate based on original locator type
            switch (originalLocator.type) {
                case "id":
                    return validateById(originalLocator.value, attributes);
                case "name":
                    return validateByName(originalLocator.value, attributes);
                case "className":
                    return validateByClass(originalLocator.value, attributes);
                default:
                    return true; // Basic validation passed
            }
            
        } catch (Exception e) {
            logger.log(Level.FINE, "Error validating element", e);
            return false;
        }
    }
    
    private boolean validateById(String originalId, Map<String, String> attributes) {
        String elementId = attributes.get("id");
        return elementId != null && (elementId.equals(originalId) || elementId.contains(originalId));
    }
    
    private boolean validateByName(String originalName, Map<String, String> attributes) {
        String elementName = attributes.get("name");
        return elementName != null && elementName.contains(originalName);
    }
    
    private boolean validateByClass(String originalClass, Map<String, String> attributes) {
        String elementClass = attributes.get("class");
        return elementClass != null && elementClass.contains(originalClass);
    }
    
    private String extractIdPrefix(String id) {
        // Extract prefix from dynamic IDs (e.g., "button_123" -> "button_")
        return id.replaceAll("\\d+$", "");
    }
    
    private String extractTextFromXPath(String xpath) {
        // Extract text content from XPath expressions
        if (xpath.contains("text()='")) {
            int start = xpath.indexOf("text()='") + 8;
            int end = xpath.indexOf("'", start);
            if (end > start) {
                return xpath.substring(start, end);
            }
        }
        return null;
    }
    
    private void incrementMetric(String metricName) {
        metrics.put(metricName, (Integer) metrics.getOrDefault(metricName, 0) + 1);
    }
    
    // Helper class to store locator information
    private static class LocatorInfo {
        String type;
        String value;
    }
}

