package com.autohealing.strategies;

import com.autohealing.adapters.PlatformAdapter;

/**
 * Interface for different healing strategies
 * Each strategy implements a specific approach to healing broken locators
 */
public interface HealingStrategy {
    
    /**
     * Check if this strategy can handle the given platform
     */
    boolean canHandle(String platformType);
    
    /**
     * Attempt to heal a broken element
     */
    <T> T heal(PlatformAdapter adapter, String elementId, String originalLocator, 
               Class<T> expectedType, Object context);
    
    /**
     * Get the priority of this strategy (lower number = higher priority)
     */
    int getPriority();
    
    /**
     * Get the name of this strategy
     */
    String getStrategyName();
    
    /**
     * Get the healed locator for an element (if successful)
     */
    String getHealedLocator(String elementId);
    
    /**
     * Initialize the strategy with configuration
     */
    void initialize(java.util.Map<String, Object> config);
    
    /**
     * Get strategy-specific metrics
     */
    java.util.Map<String, Object> getMetrics();
}

