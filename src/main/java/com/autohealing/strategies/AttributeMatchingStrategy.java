package com.autohealing.strategies;

import com.autohealing.adapters.PlatformAdapter;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Attribute Matching Strategy
 * Heals elements by finding similar attributes when original locators fail
 */
public class AttributeMatchingStrategy implements HealingStrategy {
    
    private static final Logger logger = Logger.getLogger(AttributeMatchingStrategy.class.getName());
    
    private final Map<String, String> healedLocators = new ConcurrentHashMap<>();
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private final Set<String> supportedPlatforms = Set.of("WEB", "WINDOWS", "APPLET", "MAINFRAME");
    
    private double similarityThreshold = 0.7; // 70% similarity threshold
    
    @Override
    public boolean canHandle(String platformType) {
        return supportedPlatforms.contains(platformType);
    }
    
    @Override
    public <T> T heal(PlatformAdapter adapter, String elementId, String originalLocator, 
                     Class<T> expectedType, Object context) {
        
        try {
            logger.info("Attempting attribute matching healing for element: " + elementId);
            
            // Get stored attributes for this element if available
            Map<String, String> expectedAttributes = getStoredAttributes(elementId);
            if (expectedAttributes.isEmpty()) {
                logger.warning("No stored attributes found for element: " + elementId);
                incrementMetric("no_attributes_failures");
                return null;
            }
            
            // Find all elements of similar type
            List<T> similarElements = findSimilarElements(adapter, originalLocator, expectedType, context);
            
            // Score each element based on attribute similarity
            T bestMatch = null;
            double bestScore = 0.0;
            
            for (T element : similarElements) {
                if (adapter.isElementDisplayed(element)) {
                    Map<String, String> elementAttributes = adapter.getElementAttributes(element);
                    double similarity = calculateAttributeSimilarity(expectedAttributes, elementAttributes);
                    
                    if (similarity > bestScore && similarity >= similarityThreshold) {
                        bestScore = similarity;
                        bestMatch = element;
                    }
                }
            }
            
            if (bestMatch != null) {
                // Generate new locator for the best match
                String newLocator = generateLocatorFromElement(bestMatch, adapter);
                if (newLocator != null) {
                    healedLocators.put(elementId, newLocator);
                    incrementMetric("successful_healings");
                    logger.info("Successfully healed element " + elementId + " with similarity score: " + 
                              String.format("%.2f", bestScore));
                    return bestMatch;
                }
            }
            
            incrementMetric("failed_healings");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in attribute matching healing", e);
            incrementMetric("strategy_errors");
            return null;
        }
    }
    
    @Override
    public int getPriority() {
        return 2; // Medium-high priority
    }
    
    @Override
    public String getStrategyName() {
        return "ATTRIBUTE_MATCHING";
    }
    
    @Override
    public String getHealedLocator(String elementId) {
        return healedLocators.get(elementId);
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        logger.info("Initialized Attribute Matching Strategy");
        
        // Configure threshold from config
        if (config.containsKey("attributeSimilarityThreshold")) {
            similarityThreshold = (Double) config.get("attributeSimilarityThreshold");
        }
        
        // Initialize metrics
        metrics.put("successful_healings", 0);
        metrics.put("failed_healings", 0);
        metrics.put("strategy_errors", 0);
        metrics.put("no_attributes_failures", 0);
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }
    
    // Public method to store element attributes for future healing
    public void storeElementAttributes(String elementId, Map<String, String> attributes) {
        // In a real implementation, this would be persisted to a database or file
        // For now, we'll use a simple in-memory storage
        attributeStore.put(elementId, new HashMap<>(attributes));
        logger.info("Stored attributes for element: " + elementId);
    }
    
    // Simple in-memory attribute storage
    private final Map<String, Map<String, String>> attributeStore = new ConcurrentHashMap<>();
    
    // Helper methods
    
    private Map<String, String> getStoredAttributes(String elementId) {
        return attributeStore.getOrDefault(elementId, new HashMap<>());
    }
    
    private <T> List<T> findSimilarElements(PlatformAdapter adapter, String originalLocator, 
                                          Class<T> expectedType, Object context) {
        
        List<T> elements = new ArrayList<>();
        
        // Try to find elements using more generic locators
        List<String> genericLocators = generateGenericLocators(originalLocator);
        
        for (String locator : genericLocators) {
            try {
                List<T> found = adapter.findElements(locator, expectedType, context);
                elements.addAll(found);
            } catch (Exception e) {
                logger.fine("Failed to find elements with locator: " + locator);
            }
        }
        
        return elements;
    }
    
    private List<String> generateGenericLocators(String originalLocator) {
        List<String> locators = new ArrayList<>();
        
        // Extract the type from original locator and create broader searches
        if (originalLocator.startsWith("id=")) {
            locators.add("css=*"); // Find all elements
            locators.add("xpath=//*[@id]"); // All elements with id
        } else if (originalLocator.startsWith("name=")) {
            locators.add("css=*"); 
            locators.add("xpath=//*[@name]"); // All elements with name
        } else if (originalLocator.startsWith("className=")) {
            locators.add("css=*");
            locators.add("xpath=//*[@class]"); // All elements with class
        } else if (originalLocator.startsWith("css=")) {
            locators.add("css=*");
        } else if (originalLocator.startsWith("xpath=")) {
            locators.add("xpath=//*");
        } else {
            // Default fallbacks
            locators.add("css=*");
            locators.add("xpath=//*");
        }
        
        return locators;
    }
    
    private double calculateAttributeSimilarity(Map<String, String> expected, Map<String, String> actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return 0.0;
        }
        
        int totalWeight = 0;
        int matchWeight = 0;
        
        // Attribute weights for different attributes
        Map<String, Integer> weights = getAttributeWeights();
        
        for (Map.Entry<String, String> expectedEntry : expected.entrySet()) {
            String key = expectedEntry.getKey();
            String expectedValue = expectedEntry.getValue();
            String actualValue = actual.get(key);
            
            int weight = weights.getOrDefault(key, 1);
            totalWeight += weight;
            
            if (actualValue != null) {
                double similarity = calculateStringSimilarity(expectedValue, actualValue);
                matchWeight += (int) (similarity * weight);
            }
        }
        
        return totalWeight > 0 ? (double) matchWeight / totalWeight : 0.0;
    }
    
    private Map<String, Integer> getAttributeWeights() {
        Map<String, Integer> weights = new HashMap<>();
        
        // Web attributes
        weights.put("id", 10);
        weights.put("name", 8);
        weights.put("class", 6);
        weights.put("type", 7);
        weights.put("value", 5);
        weights.put("href", 6);
        weights.put("src", 6);
        weights.put("alt", 4);
        weights.put("title", 4);
        
        // Windows attributes
        weights.put("automationId", 10);
        weights.put("controlType", 8);
        weights.put("helpText", 5);
        weights.put("accessKey", 6);
        
        // Applet attributes
        weights.put("componentClass", 8);
        weights.put("tooltip", 5);
        
        // Mainframe attributes
        weights.put("fieldName", 9);
        weights.put("screenId", 7);
        weights.put("label", 6);
        weights.put("color", 3);
        
        // Common attributes
        weights.put("text", 7);
        weights.put("enabled", 2);
        weights.put("visible", 2);
        
        return weights;
    }
    
    private double calculateStringSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        // Use Levenshtein distance for string similarity
        int distance = levenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        return maxLength > 0 ? 1.0 - ((double) distance / maxLength) : 0.0;
    }
    
    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        
        return dp[str1.length()][str2.length()];
    }
    
    private String generateLocatorFromElement(Object element, PlatformAdapter adapter) {
        try {
            // Get all possible locators for the element
            List<String> locators = adapter.getAllPossibleLocators(element, null);
            
            // Return the most specific locator (usually ID-based is best)
            for (String locator : locators) {
                if (locator.startsWith("id=")) {
                    return locator;
                }
            }
            
            // Fallback to name-based
            for (String locator : locators) {
                if (locator.startsWith("name=")) {
                    return locator;
                }
            }
            
            // Fallback to any available locator
            if (!locators.isEmpty()) {
                return locators.get(0);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error generating locator from element", e);
        }
        
        return null;
    }
    
    private void incrementMetric(String metricName) {
        metrics.put(metricName, (Integer) metrics.getOrDefault(metricName, 0) + 1);
    }
}

