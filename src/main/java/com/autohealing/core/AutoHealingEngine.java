package com.autohealing.core;

import com.autohealing.strategies.HealingStrategy;
import com.autohealing.adapters.PlatformAdapter;
import com.autohealing.config.HealingConfiguration;
import com.autohealing.reporting.HealingReporter;
import java.util.logging.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Auto-Healing Engine that coordinates healing across different platforms
 * Supports runtime healing for Web, Windows Desktop, Java Applets, and Mainframe applications
 */
public class AutoHealingEngine {
    
    private static final Logger logger = Logger.getLogger(AutoHealingEngine.class.getName());
    private static AutoHealingEngine instance;
    
    private final Map<String, PlatformAdapter> platformAdapters;
    private final List<HealingStrategy> healingStrategies;
    private final HealingConfiguration configuration;
    private final HealingReporter reporter;
    private final Map<String, Integer> healingAttempts;
    
    private AutoHealingEngine() {
        this.platformAdapters = new ConcurrentHashMap<>();
        this.healingStrategies = new ArrayList<>();
        this.configuration = HealingConfiguration.getInstance();
        this.reporter = new HealingReporter();
        this.healingAttempts = new ConcurrentHashMap<>();
    }
    
    public static synchronized AutoHealingEngine getInstance() {
        if (instance == null) {
            instance = new AutoHealingEngine();
        }
        return instance;
    }
    
    /**
     * Register a platform adapter for specific application type
     */
    public void registerPlatformAdapter(String platformType, PlatformAdapter adapter) {
        platformAdapters.put(platformType, adapter);
        logger.info("Registered platform adapter for: " + platformType);
    }
    
    /**
     * Add a healing strategy to the engine
     */
    public void addHealingStrategy(HealingStrategy strategy) {
        healingStrategies.add(strategy);
        logger.info("Added healing strategy: " + strategy.getClass().getSimpleName());
    }
    
    /**
     * Main healing method - attempts to heal a failed element/action
     */
    public <T> T heal(String platformType, String elementId, String originalLocator, 
                     Class<T> expectedType, Object context) {
        
        String healingKey = platformType + ":" + elementId;
        int attempts = healingAttempts.getOrDefault(healingKey, 0);
        
        if (attempts >= configuration.getMaxHealingAttempts()) {
            logger.warning("Max healing attempts reached for " + healingKey);
            reporter.recordFailedHealing(platformType, elementId, originalLocator, "Max attempts exceeded");
            return null;
        }
        
        PlatformAdapter adapter = platformAdapters.get(platformType);
        if (adapter == null) {
            logger.severe("No adapter found for platform: " + platformType);
            return null;
        }
        
        healingAttempts.put(healingKey, attempts + 1);
        
        // Try each healing strategy
        for (HealingStrategy strategy : healingStrategies) {
            if (strategy.canHandle(platformType)) {
                try {
                    T healedElement = strategy.heal(adapter, elementId, originalLocator, expectedType, context);
                    if (healedElement != null) {
                        logger.info("Successfully healed " + healingKey + " using strategy: " + 
                                  strategy.getClass().getSimpleName());
                        reporter.recordSuccessfulHealing(platformType, elementId, originalLocator, 
                                                       strategy.getClass().getSimpleName());
                        
                        // Update configuration with successful locator
                        configuration.updateSuccessfulLocator(elementId, 
                                                             strategy.getHealedLocator(elementId));
                        
                        // Reset attempt counter on success
                        healingAttempts.remove(healingKey);
                        return healedElement;
                    }
                } catch (Exception e) {
                    logger.warning("Healing strategy " + strategy.getClass().getSimpleName() + 
                              " failed for " + healingKey + ": " + e.getMessage());
                }
            }
        }
        
        logger.severe("All healing strategies failed for: " + healingKey);
        reporter.recordFailedHealing(platformType, elementId, originalLocator, "All strategies failed");
        return null;
    }
    
    /**
     * Get healing statistics
     */
    public Map<String, Object> getHealingStatistics() {
        return reporter.getStatistics();
    }
    
    /**
     * Reset healing attempts for a specific element
     */
    public void resetHealingAttempts(String platformType, String elementId) {
        String healingKey = platformType + ":" + elementId;
        healingAttempts.remove(healingKey);
        logger.info("Reset healing attempts for: " + healingKey);
    }
    
    /**
     * Enable/disable auto-healing at runtime
     */
    public void setHealingEnabled(boolean enabled) {
        configuration.setHealingEnabled(enabled);
        logger.info("Auto-healing " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if healing is enabled
     */
    public boolean isHealingEnabled() {
        return configuration.isHealingEnabled();
    }
}

