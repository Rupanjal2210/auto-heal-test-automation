package com.autohealing.integration;

import com.autohealing.AutoHealingFramework;

import java.util.logging.Logger;

/**
 * Simple Integration Example showing how to wrap existing test methods
 * This example shows the integration pattern without external dependencies
 */
public class SimpleIntegrationExample {
    
    private static final Logger logger = Logger.getLogger(SimpleIntegrationExample.class.getName());
    
    private AutoHealingFramework healingFramework;
    
    public void setup() {
        // Initialize Auto-Healing Framework
        healingFramework = new AutoHealingFramework();
        
        // Configure healing settings
        healingFramework.getConfiguration().setHealingEnabled(true);
        healingFramework.getConfiguration().setMaxHealingAttempts(3);
        
        logger.info("Auto-healing framework initialized");
    }
    
    /**
     * INTEGRATION PATTERN 1: Wrapper Method Approach
     * 
     * Replace this pattern in your existing tests:
     * WebElement element = driver.findElement(By.id("elementId"));
     * 
     * With this pattern:
     * WebElement element = findElementWithHealing("elementName", "id=elementId");
     */
    public void demonstrateWrapperPattern() {
        setup();
        
        logger.info("=== WRAPPER PATTERN DEMONSTRATION ===");
        
        // Original approach (brittle):
        // WebElement loginButton = driver.findElement(By.id("login-btn"));
        
        // Enhanced approach (with healing):
        // Object loginButton = findElementWithHealing("loginButton", "id=login-btn", driver);
        
        logger.info("Instead of direct driver.findElement(), use wrapper method");
        logger.info("This allows automatic healing when original locator fails");
    }
    
    /**
     * INTEGRATION PATTERN 2: Base Test Class
     * 
     * Create a base class that all your test classes extend
     */
    public static abstract class BaseHealingTest {
        protected AutoHealingFramework healingFramework;
        
        public void setupHealing() {
            healingFramework = new AutoHealingFramework();
            healingFramework.getConfiguration().setHealingEnabled(true);
            
            // Register platform adapters as needed
            // healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
        }
        
        // Utility method available to all test classes
        protected Object findElement(String elementId, String locator, Object context) {
            // Determine platform type based on context
            String platformType = determinePlatformType(context);
            
            return healingFramework.heal(platformType, elementId, locator, Object.class, context);
        }
        
        private String determinePlatformType(Object context) {
            if (context != null) {
                String className = context.getClass().getSimpleName();
                if (className.contains("Driver")) return "WEB";
                if (className.contains("Windows")) return "WINDOWS";
            }
            return "WEB"; // Default
        }
        
        public void teardownHealing() {
            if (healingFramework != null) {
                healingFramework.generateReport("target/healing-reports/session-report");
            }
        }
    }
    
    /**
     * INTEGRATION PATTERN 3: Factory Pattern
     * 
     * Create element factories that return healing-enabled elements
     */
    public static class HealingElementFactory {
        private final AutoHealingFramework healingFramework;
        private final Object context;
        
        public HealingElementFactory(AutoHealingFramework framework, Object context) {
            this.healingFramework = framework;
            this.context = context;
        }
        
        public Object createElement(String elementId, String locator) {
            return healingFramework.heal("WEB", elementId, locator, Object.class, context);
        }
        
        public Object createButton(String buttonId, String locator) {
            return createElement("button_" + buttonId, locator);
        }
        
        public Object createInputField(String fieldId, String locator) {
            return createElement("input_" + fieldId, locator);
        }
    }
    
    /**
     * INTEGRATION PATTERN 4: Configuration-Based Healing
     */
    public void demonstrateConfigurationBasedHealing() {
        logger.info("=== CONFIGURATION-BASED HEALING ===");
        
        // Configure programmatically
        healingFramework.getConfiguration().setHealingEnabled(true);
        healingFramework.getConfiguration().setMaxHealingAttempts(5);
        healingFramework.getConfiguration().setHealingTimeout(10000); // 10 seconds
        
        logger.info("Healing configured programmatically:");
        logger.info("  - Healing enabled: " + healingFramework.getConfiguration().isHealingEnabled());
        logger.info("  - Max attempts: " + healingFramework.getConfiguration().getMaxHealingAttempts());
        logger.info("  - Timeout: " + healingFramework.getConfiguration().getHealingTimeout() + "ms");
        
        logger.info("Healing configured based on application requirements");
    }
    
    /**
     * INTEGRATION PATTERN 5: Gradual Migration Strategy
     */
    public void demonstrateGradualMigration() {
        logger.info("=== GRADUAL MIGRATION STRATEGY ===");
        
        // Phase 1: Identify problematic elements
        String[] problematicElements = {
            "loginButton", "searchBox", "submitForm", "navigationMenu"
        };
        
        logger.info("Phase 1: Identified " + problematicElements.length + " problematic elements");
        
        // Phase 2: Replace specific elements with healing versions
        for (String elementId : problematicElements) {
            logger.info("Migrating element: " + elementId + " to use auto-healing");
            // Replace driver.findElement() calls for these elements only
        }
        
        // Phase 3: Monitor healing effectiveness
        logger.info("Phase 3: Monitor healing reports to assess effectiveness");
        
        // Phase 4: Expand to all elements
        logger.info("Phase 4: Gradually expand to all element finding operations");
    }
    
    /**
     * Utility method showing how to add healing to existing Page Object Model
     */
    public static class LoginPageExample {
        private static final Logger logger = Logger.getLogger(LoginPageExample.class.getName());
        private final AutoHealingFramework healingFramework;
        private final Object driver;
        
        // Define locators as constants (existing pattern)
        private static final String USERNAME_FIELD = "id=username";
        private static final String PASSWORD_FIELD = "id=password";
        private static final String LOGIN_BUTTON = "xpath=//button[@type='submit']";
        
        public LoginPageExample(Object driver, AutoHealingFramework healingFramework) {
            this.driver = driver;
            this.healingFramework = healingFramework;
        }
        
        public void enterUsername(String username) {
            // Original: WebElement field = driver.findElement(By.id("username"));
            // Enhanced:
            Object field = healingFramework.heal("WEB", "usernameField", USERNAME_FIELD, Object.class, driver);
            
            // field.sendKeys(username); // Your existing interaction code
            logger.info("Username entered using healing-enabled element for: " + username);
        }
        
        public void enterPassword(String password) {
            Object field = healingFramework.heal("WEB", "passwordField", PASSWORD_FIELD, Object.class, driver);
            // field.sendKeys(password);
            logger.info("Password entered using healing-enabled element");
        }
        
        public void clickLogin() {
            Object button = healingFramework.heal("WEB", "loginButton", LOGIN_BUTTON, Object.class, driver);
            // button.click();
            logger.info("Login button clicked using healing-enabled element");
        }
    }
    
    /**
     * Example showing conditional healing based on environment
     */
    public void demonstrateConditionalHealing() {
        logger.info("=== CONDITIONAL HEALING ===");
        
        // Enable healing only in specific environments
        String environment = System.getProperty("test.environment", "dev");
        boolean enableHealing = environment.equals("staging") || environment.equals("production");
        
        healingFramework.getConfiguration().setHealingEnabled(enableHealing);
        
        if (enableHealing) {
            logger.info("Healing enabled for environment: " + environment);
        } else {
            logger.info("Healing disabled for environment: " + environment);
        }
    }
    
    /**
     * Main method to demonstrate integration patterns
     */
    public static void main(String[] args) {
        SimpleIntegrationExample example = new SimpleIntegrationExample();
        
        example.demonstrateWrapperPattern();
        example.demonstrateConfigurationBasedHealing();
        example.demonstrateGradualMigration();
        example.demonstrateConditionalHealing();
        
        // Demonstrate factory pattern
        example.setup();
        HealingElementFactory factory = new HealingElementFactory(example.healingFramework, null);
        factory.createButton("submitBtn", "id=submit");
        
        // Demonstrate Page Object integration
        LoginPageExample loginPage = new LoginPageExample(null, example.healingFramework);
        loginPage.enterUsername("testuser");
        loginPage.enterPassword("password");
        loginPage.clickLogin();
        
        System.out.println("\n=== INTEGRATION PATTERNS DEMONSTRATED ===");
        System.out.println("1. Wrapper Method Approach");
        System.out.println("2. Base Test Class Pattern");
        System.out.println("3. Element Factory Pattern");
        System.out.println("4. Configuration-Based Healing");
        System.out.println("5. Gradual Migration Strategy");
        System.out.println("6. Page Object Model Integration");
        System.out.println("7. Conditional Healing");
        
        System.out.println("\nChoose the pattern that best fits your existing test architecture!");
    }
}
