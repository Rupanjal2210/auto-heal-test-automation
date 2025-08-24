package com.autohealing.adapters;

import java.util.List;
import java.util.Map;

/**
 * Platform adapter interface for different application types
 * Provides abstraction layer for Web, Windows, Mainframe, and Applet platforms
 */
public interface PlatformAdapter {
    
    /**
     * Get platform type identifier
     */
    String getPlatformType();
    
    /**
     * Find element using locator strategy
     */
    <T> T findElement(String locator, Class<T> expectedType, Object context);
    
    /**
     * Find multiple elements using locator strategy
     */
    <T> List<T> findElements(String locator, Class<T> expectedType, Object context);
    
    /**
     * Get all possible locators for an element
     */
    List<String> getAllPossibleLocators(Object element, Object context);
    
    /**
     * Get element attributes
     */
    Map<String, String> getElementAttributes(Object element);
    
    /**
     * Get element text content
     */
    String getElementText(Object element);
    
    /**
     * Check if element is displayed/visible
     */
    boolean isElementDisplayed(Object element);
    
    /**
     * Check if element is enabled/interactive
     */
    boolean isElementEnabled(Object element);
    
    /**
     * Get page/screen source
     */
    String getPageSource(Object context);
    
    /**
     * Take screenshot for analysis
     */
    byte[] takeScreenshot(Object context);
    
    /**
     * Perform click action on element
     */
    void clickElement(Object element);
    
    /**
     * Perform type action on element
     */
    void typeText(Object element, String text);
    
    /**
     * Get all elements of a similar type
     */
    <T> List<T> getAllSimilarElements(String baseLocator, Class<T> expectedType, Object context);
    
    /**
     * Platform-specific initialization
     */
    void initialize(Map<String, Object> config);
    
    /**
     * Platform-specific cleanup
     */
    void cleanup();
}

