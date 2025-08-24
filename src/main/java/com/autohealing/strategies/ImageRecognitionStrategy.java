package com.autohealing.strategies;

import com.autohealing.adapters.PlatformAdapter;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * Image Recognition Strategy for visual element healing
 * Uses computer vision to identify elements by their appearance
 * Particularly useful for Windows applications, Java Applets, and Mainframe apps
 */
public class ImageRecognitionStrategy implements HealingStrategy {
    
    private static final Logger logger = Logger.getLogger(ImageRecognitionStrategy.class.getName());
    
    private final Map<String, String> healedLocators = new ConcurrentHashMap<>();
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> elementTemplates = new ConcurrentHashMap<>();
    private final Set<String> supportedPlatforms = Set.of("WINDOWS", "APPLET", "MAINFRAME");
    
    private double matchThreshold = 0.8; // 80% similarity threshold
    
    @Override
    public boolean canHandle(String platformType) {
        return supportedPlatforms.contains(platformType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T heal(PlatformAdapter adapter, String elementId, String originalLocator, 
                     Class<T> expectedType, Object context) {
        
        try {
            logger.info("Attempting image recognition healing for element: " + elementId);
            
            // Get current screenshot
            byte[] screenshot = adapter.takeScreenshot(context);
            if (screenshot == null || screenshot.length == 0) {
                logger.warning("Could not take screenshot for image recognition");
                return null;
            }
            
            BufferedImage currentScreen = ImageIO.read(new ByteArrayInputStream(screenshot));
            
            // Get stored template for this element
            BufferedImage template = elementTemplates.get(elementId);
            if (template == null) {
                // Try to extract template from original locator if it contains coordinates
                template = extractTemplateFromLocator(originalLocator, adapter, context);
                if (template != null) {
                    elementTemplates.put(elementId, template);
                }
            }
            
            if (template == null) {
                logger.warning("No template available for element: " + elementId);
                incrementMetric("no_template_failures");
                return null;
            }
            
            // Perform template matching
            MatchResult matchResult = performTemplateMatching(currentScreen, template);
            
            if (matchResult != null && matchResult.confidence >= matchThreshold) {
                // Create new locator with coordinates
                String newLocator = String.format("bounds=%d,%d,%d,%d", 
                    matchResult.x, matchResult.y, 
                    matchResult.x + template.getWidth(), 
                    matchResult.y + template.getHeight());
                
                // Try to find element using the new coordinates
                T element = adapter.findElement(newLocator, expectedType, context);
                if (element != null) {
                    healedLocators.put(elementId, newLocator);
                    incrementMetric("successful_healings");
                    logger.info("Successfully healed element " + elementId + " using image recognition at (" + 
                              matchResult.x + ", " + matchResult.y + ")");
                    return element;
                }
            }
            
            // Try multi-scale template matching for different zoom levels
            T element = tryMultiScaleMatching(currentScreen, template, elementId, adapter, expectedType, context);
            if (element != null) {
                return element;
            }
            
            incrementMetric("failed_healings");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in image recognition healing", e);
            incrementMetric("strategy_errors");
            return null;
        }
    }
    
    @Override
    public int getPriority() {
        return 3; // Medium priority - useful when other strategies fail
    }
    
    @Override
    public String getStrategyName() {
        return "IMAGE_RECOGNITION";
    }
    
    @Override
    public String getHealedLocator(String elementId) {
        return healedLocators.get(elementId);
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        logger.info("Initialized Image Recognition Strategy");
        
        // Configure threshold from config
        if (config.containsKey("matchThreshold")) {
            matchThreshold = (Double) config.get("matchThreshold");
        }
        
        // Initialize metrics
        metrics.put("successful_healings", 0);
        metrics.put("failed_healings", 0);
        metrics.put("strategy_errors", 0);
        metrics.put("no_template_failures", 0);
        metrics.put("template_matches", 0);
    }
    
    @Override
    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }
    
    // Public method to store element templates
    public void storeElementTemplate(String elementId, BufferedImage template) {
        elementTemplates.put(elementId, template);
        logger.info("Stored template for element: " + elementId);
    }
    
    public void storeElementTemplate(String elementId, byte[] templateBytes) {
        try {
            BufferedImage template = ImageIO.read(new ByteArrayInputStream(templateBytes));
            storeElementTemplate(elementId, template);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error storing template for element: " + elementId, e);
        }
    }
    
    // Helper methods
    
    private BufferedImage extractTemplateFromLocator(String locator, PlatformAdapter adapter, Object context) {
        try {
            if (locator.startsWith("bounds=")) {
                String boundsStr = locator.substring(7);
                String[] parts = boundsStr.split(",");
                if (parts.length == 4) {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int width = Integer.parseInt(parts[2]) - x;
                    int height = Integer.parseInt(parts[3]) - y;
                    
                    byte[] screenshot = adapter.takeScreenshot(context);
                    BufferedImage fullScreen = ImageIO.read(new ByteArrayInputStream(screenshot));
                    
                    if (x >= 0 && y >= 0 && x + width <= fullScreen.getWidth() && y + height <= fullScreen.getHeight()) {
                        return fullScreen.getSubimage(x, y, width, height);
                    }
                }
            }
        } catch (Exception e) {
            logger.fine("Could not extract template from locator: " + locator);
        }
        return null;
    }
    
    private MatchResult performTemplateMatching(BufferedImage screen, BufferedImage template) {
        if (screen == null || template == null) {
            return null;
        }
        
        int screenWidth = screen.getWidth();
        int screenHeight = screen.getHeight();
        int templateWidth = template.getWidth();
        int templateHeight = template.getHeight();
        
        if (templateWidth > screenWidth || templateHeight > screenHeight) {
            return null;
        }
        
        double bestMatch = 0.0;
        int bestX = 0;
        int bestY = 0;
        
        // Slide template across the screen
        for (int y = 0; y <= screenHeight - templateHeight; y += 5) { // Step by 5 pixels for performance
            for (int x = 0; x <= screenWidth - templateWidth; x += 5) {
                double similarity = calculateSimilarity(screen, template, x, y);
                if (similarity > bestMatch) {
                    bestMatch = similarity;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        
        if (bestMatch >= matchThreshold) {
            incrementMetric("template_matches");
            return new MatchResult(bestX, bestY, bestMatch);
        }
        
        return null;
    }
    
    private double calculateSimilarity(BufferedImage screen, BufferedImage template, int offsetX, int offsetY) {
        int templateWidth = template.getWidth();
        int templateHeight = template.getHeight();
        
        long totalDifference = 0;
        long maxDifference = (long) templateWidth * templateHeight * 255 * 3; // RGB channels
        
        for (int y = 0; y < templateHeight; y++) {
            for (int x = 0; x < templateWidth; x++) {
                int screenRGB = screen.getRGB(offsetX + x, offsetY + y);
                int templateRGB = template.getRGB(x, y);
                
                int screenR = (screenRGB >> 16) & 0xFF;
                int screenG = (screenRGB >> 8) & 0xFF;
                int screenB = screenRGB & 0xFF;
                
                int templateR = (templateRGB >> 16) & 0xFF;
                int templateG = (templateRGB >> 8) & 0xFF;
                int templateB = templateRGB & 0xFF;
                
                totalDifference += Math.abs(screenR - templateR);
                totalDifference += Math.abs(screenG - templateG);
                totalDifference += Math.abs(screenB - templateB);
            }
        }
        
        return 1.0 - ((double) totalDifference / maxDifference);
    }
    
    @SuppressWarnings("unchecked")
    private <T> T tryMultiScaleMatching(BufferedImage screen, BufferedImage template, String elementId,
                                       PlatformAdapter adapter, Class<T> expectedType, Object context) {
        
        double[] scales = {0.8, 0.9, 1.1, 1.2}; // Try different scales
        
        for (double scale : scales) {
            try {
                int newWidth = (int) (template.getWidth() * scale);
                int newHeight = (int) (template.getHeight() * scale);
                
                BufferedImage scaledTemplate = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = scaledTemplate.createGraphics();
                g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                   java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(template, 0, 0, newWidth, newHeight, null);
                g2d.dispose();
                
                MatchResult matchResult = performTemplateMatching(screen, scaledTemplate);
                
                if (matchResult != null && matchResult.confidence >= matchThreshold * 0.9) { // Slightly lower threshold for scaled
                    String newLocator = String.format("bounds=%d,%d,%d,%d", 
                        matchResult.x, matchResult.y, 
                        matchResult.x + newWidth, 
                        matchResult.y + newHeight);
                    
                    T element = adapter.findElement(newLocator, expectedType, context);
                    if (element != null) {
                        healedLocators.put(elementId, newLocator);
                        incrementMetric("successful_healings");
                        logger.info("Successfully healed element " + elementId + " using scaled template (scale: " + scale + ")");
                        return element;
                    }
                }
                
            } catch (Exception e) {
                logger.fine("Error in multi-scale matching with scale " + scale + ": " + e.getMessage());
            }
        }
        
        return null;
    }
    
    private void incrementMetric(String metricName) {
        metrics.put(metricName, (Integer) metrics.getOrDefault(metricName, 0) + 1);
    }
    
    // Helper class for match results
    private static class MatchResult {
        final int x;
        final int y;
        final double confidence;
        
        MatchResult(int x, int y, double confidence) {
            this.x = x;
            this.y = y;
            this.confidence = confidence;
        }
    }
}

