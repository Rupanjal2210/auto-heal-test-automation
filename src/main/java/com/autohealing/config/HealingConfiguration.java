package com.autohealing.config;

import com.autohealing.core.AutoHealingEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.logging.Logger;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration management for the auto-healing framework
 * Supports runtime configuration updates and persistent storage
 */
public class HealingConfiguration {
    
    private static final Logger logger = Logger.getLogger(AutoHealingEngine.class.getName());
    private static HealingConfiguration instance;
    
    private final Map<String, Object> configuration;
    private final Map<String, String> successfulLocators;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    
    private String configFilePath = "healing-config.yml";
    private String locatorsFilePath = "successful-locators.json";
    
    private HealingConfiguration() {
        this.configuration = new ConcurrentHashMap<>();
        this.successfulLocators = new ConcurrentHashMap<>();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
        
        loadDefaultConfiguration();
        loadConfigurationFromFile();
        loadSuccessfulLocators();
    }
    
    public static synchronized HealingConfiguration getInstance() {
        if (instance == null) {
            instance = new HealingConfiguration();
        }
        return instance;
    }
    
    // Configuration getters
    
    public boolean isHealingEnabled() {
        return (Boolean) configuration.getOrDefault("healing.enabled", true);
    }
    
    public void setHealingEnabled(boolean enabled) {
        configuration.put("healing.enabled", enabled);
        saveConfiguration();
    }
    
    public int getMaxHealingAttempts() {
        return (Integer) configuration.getOrDefault("healing.maxAttempts", 3);
    }
    
    public void setMaxHealingAttempts(int maxAttempts) {
        configuration.put("healing.maxAttempts", maxAttempts);
        saveConfiguration();
    }
    
    public long getHealingTimeout() {
        return (Long) configuration.getOrDefault("healing.timeout", 30000L); // 30 seconds
    }
    
    public void setHealingTimeout(long timeout) {
        configuration.put("healing.timeout", timeout);
        saveConfiguration();
    }
    
    public boolean isScreenshotOnFailureEnabled() {
        return (Boolean) configuration.getOrDefault("healing.screenshotOnFailure", true);
    }
    
    public void setScreenshotOnFailureEnabled(boolean enabled) {
        configuration.put("healing.screenshotOnFailure", enabled);
        saveConfiguration();
    }
    
    public String getScreenshotsDirectory() {
        return (String) configuration.getOrDefault("healing.screenshotsDir", "screenshots");
    }
    
    public void setScreenshotsDirectory(String directory) {
        configuration.put("healing.screenshotsDir", directory);
        saveConfiguration();
    }
    
    public double getImageMatchThreshold() {
        return (Double) configuration.getOrDefault("healing.imageMatchThreshold", 0.8);
    }
    
    public void setImageMatchThreshold(double threshold) {
        configuration.put("healing.imageMatchThreshold", threshold);
        saveConfiguration();
    }
    
    public List<String> getEnabledStrategies() {
        @SuppressWarnings("unchecked")
        List<String> strategies = (List<String>) configuration.get("healing.enabledStrategies");
        if (strategies == null) {
            strategies = Arrays.asList("DOM_ANALYSIS", "ATTRIBUTE_MATCHING", "IMAGE_RECOGNITION", "TEXT_MATCHING");
        }
        return strategies;
    }
    
    public void setEnabledStrategies(List<String> strategies) {
        configuration.put("healing.enabledStrategies", strategies);
        saveConfiguration();
    }
    
    // Platform-specific configurations
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWebConfiguration() {
        return (Map<String, Object>) configuration.getOrDefault("platforms.web", new HashMap<>());
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWindowsConfiguration() {
        return (Map<String, Object>) configuration.getOrDefault("platforms.windows", new HashMap<>());
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppletConfiguration() {
        return (Map<String, Object>) configuration.getOrDefault("platforms.applet", new HashMap<>());
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMainframeConfiguration() {
        return (Map<String, Object>) configuration.getOrDefault("platforms.mainframe", new HashMap<>());
    }
    
    // Successful locators management
    
    public String getSuccessfulLocator(String elementId) {
        return successfulLocators.get(elementId);
    }
    
    public void updateSuccessfulLocator(String elementId, String locator) {
        successfulLocators.put(elementId, locator);
        saveSuccessfulLocators();
        logger.info("Updated successful locator for " + elementId + ": " + locator);
    }
    
    public Map<String, String> getAllSuccessfulLocators() {
        return new HashMap<>(successfulLocators);
    }
    
    public void removeSuccessfulLocator(String elementId) {
        successfulLocators.remove(elementId);
        saveSuccessfulLocators();
    }
    
    // Configuration management
    
    public void setConfigFilePath(String path) {
        this.configFilePath = path;
        loadConfigurationFromFile();
    }
    
    public void setLocatorsFilePath(String path) {
        this.locatorsFilePath = path;
        loadSuccessfulLocators();
    }
    
    public void updateConfiguration(String key, Object value) {
        configuration.put(key, value);
        saveConfiguration();
    }
    
    public Object getConfiguration(String key) {
        return configuration.get(key);
    }
    
    public Object getConfiguration(String key, Object defaultValue) {
        return configuration.getOrDefault(key, defaultValue);
    }
    
    public Map<String, Object> getAllConfiguration() {
        return new HashMap<>(configuration);
    }
    
    public void reloadConfiguration() {
        loadConfigurationFromFile();
        loadSuccessfulLocators();
        logger.info("Reloaded configuration from files");
    }
    
    // Private helper methods
    
    private void loadDefaultConfiguration() {
        // General healing settings
        configuration.put("healing.enabled", true);
        configuration.put("healing.maxAttempts", 3);
        configuration.put("healing.timeout", 30000L);
        configuration.put("healing.screenshotOnFailure", true);
        configuration.put("healing.screenshotsDir", "screenshots");
        configuration.put("healing.imageMatchThreshold", 0.8);
        configuration.put("healing.enabledStrategies", 
            Arrays.asList("DOM_ANALYSIS", "ATTRIBUTE_MATCHING", "IMAGE_RECOGNITION", "TEXT_MATCHING"));
        
        // Platform-specific settings
        Map<String, Object> webConfig = new HashMap<>();
        webConfig.put("waitTimeout", 10000);
        webConfig.put("implicitWait", 5000);
        webConfig.put("pageLoadTimeout", 30000);
        webConfig.put("enableJavaScript", true);
        configuration.put("platforms.web", webConfig);
        
        Map<String, Object> windowsConfig = new HashMap<>();
        windowsConfig.put("waitTimeout", 15000);
        windowsConfig.put("enableAccessibility", true);
        windowsConfig.put("captureMethod", "SCREENSHOT");
        configuration.put("platforms.windows", windowsConfig);
        
        Map<String, Object> appletConfig = new HashMap<>();
        appletConfig.put("waitTimeout", 20000);
        appletConfig.put("useImageRecognition", true);
        appletConfig.put("javaVersion", "11");
        configuration.put("platforms.applet", appletConfig);
        
        Map<String, Object> mainframeConfig = new HashMap<>();
        mainframeConfig.put("waitTimeout", 25000);
        mainframeConfig.put("terminalType", "3270");
        mainframeConfig.put("enableScreenScraping", true);
        configuration.put("platforms.mainframe", mainframeConfig);
        
        // Strategy-specific settings
        Map<String, Object> strategyConfig = new HashMap<>();
        strategyConfig.put("dom.maxDepth", 5);
        strategyConfig.put("dom.attributeWeights", Map.of("id", 10, "name", 8, "class", 6, "tag", 4));
        strategyConfig.put("image.matchThreshold", 0.8);
        strategyConfig.put("image.maxScale", 1.5);
        strategyConfig.put("text.similarity", 0.7);
        configuration.put("strategies", strategyConfig);
    }
    
    private void loadConfigurationFromFile() {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> fileConfig = yamlMapper.readValue(configFile, Map.class);
                mergeConfiguration(fileConfig);
                logger.info("Loaded configuration from: " + configFilePath);
            } catch (Exception e) {
                logger.warning("Error loading configuration from " + configFilePath + ": " + e.getMessage());
            }
        } else {
            logger.info("Configuration file not found: " + configFilePath + ", using defaults");
        }
    }
    
    private void saveConfiguration() {
        try {
            File configFile = new File(configFilePath);
            configFile.getParentFile().mkdirs();
            yamlMapper.writeValue(configFile, configuration);
            logger.fine("Saved configuration to: " + configFilePath);
        } catch (Exception e) {
            logger.severe("Error saving configuration to " + configFilePath + ": " + e.getMessage());
        }
    }
    
    private void loadSuccessfulLocators() {
        File locatorsFile = new File(locatorsFilePath);
        if (locatorsFile.exists()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> fileLocators = jsonMapper.readValue(locatorsFile, Map.class);
                successfulLocators.putAll(fileLocators);
                logger.info("Loaded " + fileLocators.size() + " successful locators from: " + locatorsFilePath);
            } catch (Exception e) {
                logger.warning("Error loading successful locators from " + locatorsFilePath + ": " + e.getMessage());
            }
        } else {
            logger.info("Successful locators file not found: " + locatorsFilePath);
        }
    }
    
    private void saveSuccessfulLocators() {
        try {
            File locatorsFile = new File(locatorsFilePath);
            locatorsFile.getParentFile().mkdirs();
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(locatorsFile, successfulLocators);
            logger.fine("Saved " + successfulLocators.size() + " successful locators to: " + locatorsFilePath);
        } catch (Exception e) {
            logger.severe("Error saving successful locators to " + locatorsFilePath + ": " + e.getMessage());
        }
    }
    
    private void mergeConfiguration(Map<String, Object> newConfig) {
        for (Map.Entry<String, Object> entry : newConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && configuration.get(key) instanceof Map) {
                // Merge nested maps
                @SuppressWarnings("unchecked")
                Map<String, Object> existingMap = (Map<String, Object>) configuration.get(key);
                @SuppressWarnings("unchecked")
                Map<String, Object> newMap = (Map<String, Object>) value;
                existingMap.putAll(newMap);
            } else {
                configuration.put(key, value);
            }
        }
    }
}

