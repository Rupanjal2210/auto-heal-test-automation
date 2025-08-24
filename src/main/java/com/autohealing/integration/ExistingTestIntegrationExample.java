package com.autohealing.integration;

import com.autohealing.AutoHealingFramework;
import java.util.logging.Logger;

/**
 * Practical Integration Guide for Auto-Healing Framework
 * 
 * This example shows how to integrate auto-healing capabilities into existing test automation
 * projects with minimal code changes. Works with any testing framework (TestNG, JUnit, etc.)
 * 
 * Key Integration Patterns:
 * 1. Wrapper Method Pattern - Add healing to existing element finding
 * 2. Page Object Enhancement - Upgrade Page Objects with healing
 * 3. Conditional Healing - Enable healing based on environment
 * 4. Fallback Strategy - Multiple locator attempts with healing
 * 5. Gradual Migration - Phase-by-phase adoption approach
 */
public class ExistingTestIntegrationExample {
    
    private static final Logger logger = Logger.getLogger(ExistingTestIntegrationExample.class.getName());
    
    private AutoHealingFramework healingFramework;
    
    /**
     * STEP 1: Initialize Auto-Healing Framework
     * Add this to your existing test setup method (@BeforeMethod, @Before, setUp(), etc.)
     */
    public void initializeAutoHealing() {
        // Create healing framework instance
        healingFramework = new AutoHealingFramework();
        
        // Register platform adapters based on your test type
        // For Selenium Web tests:
        // healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
        
        // For Windows Desktop applications:
        // healingFramework.registerPlatformAdapter("WINDOWS", new WindowsPlatformAdapter());
        
        // For Java Applets:
        // healingFramework.registerPlatformAdapter("APPLET", new AppletPlatformAdapter());
        
        // Configure healing behavior
        healingFramework.getConfiguration().setHealingEnabled(true);
        healingFramework.getConfiguration().setMaxHealingAttempts(3);
        healingFramework.getConfiguration().setHealingTimeout(10000); // 10 seconds
        
        logger.info("Auto-healing framework initialized and ready");
    }
    
    /**
     * STEP 2: Generate Reports and Cleanup
     * Add this to your existing test teardown method (@AfterMethod, @After, tearDown(), etc.)
     */
    public void finalizeAutoHealing() {
        if (healingFramework != null) {
            // Generate comprehensive healing report
            String reportPath = "target/healing-reports/session-" + System.currentTimeMillis() + ".html";
            healingFramework.getReporter().generateReport(reportPath);
            
            // Log session statistics
            double successRate = healingFramework.getReporter().getSuccessRate();
            int totalAttempts = healingFramework.getReporter().getTotalAttempts();
            int successfulHeals = healingFramework.getReporter().getSuccessfulHeals();
            
            logger.info("=== AUTO-HEALING SESSION SUMMARY ===");
            logger.info("Total healing attempts: " + totalAttempts);
            logger.info("Successful heals: " + successfulHeals);
            logger.info("Success rate: " + String.format("%.1f%%", successRate));
            logger.info("Report generated: " + reportPath);
        }
    }
    
    /**
     * INTEGRATION PATTERN 1: Simple Wrapper Method
     * 
     * BEFORE: WebElement button = driver.findElement(By.id("submit"));
     * AFTER:  WebElement button = findWithHealing("submitButton", "id=submit");
     */
    public Object findWithHealing(String elementName, String locator) {
        try {
            // Try original locator first (fast path)
            return simulateOriginalFind(locator);
            
        } catch (Exception originalFailure) {
            logger.info("Original locator failed for '" + elementName + "', attempting healing...");
            
            // Attempt auto-healing
            Object healedElement = healingFramework.heal("WEB", elementName, locator, 
                                                        Object.class, null);
            
            if (healedElement != null) {
                logger.info("✓ Successfully healed element: " + elementName);
                return healedElement;
            } else {
                logger.warning("✗ Healing failed for element: " + elementName);
                throw new RuntimeException("Element '" + elementName + "' not found even after healing", 
                                         originalFailure);
            }
        }
    }
    
    /**
     * INTEGRATION PATTERN 2: Smart Fallback Strategy
     * 
     * Try multiple locators with healing for each attempt
     */
    public Object findWithSmartFallback(String elementName, String primaryLocator, 
                                       String... fallbackLocators) {
        // Try primary locator with healing
        try {
            return findWithHealing(elementName + "_primary", primaryLocator);
        } catch (Exception e) {
            logger.info("Primary locator failed for " + elementName + ", trying fallbacks...");
        }
        
        // Try fallback locators
        for (int i = 0; i < fallbackLocators.length; i++) {
            try {
                logger.info("Attempting fallback " + (i + 1) + " for " + elementName);
                return findWithHealing(elementName + "_fallback" + (i + 1), fallbackLocators[i]);
            } catch (Exception e) {
                logger.info("Fallback " + (i + 1) + " failed: " + e.getMessage());
            }
        }
        
        throw new RuntimeException("All locator strategies failed for: " + elementName);
    }
    
    /**
     * INTEGRATION PATTERN 3: Environment-Based Healing
     * 
     * Enable healing only in specific environments (staging, unstable, etc.)
     */
    public Object findWithConditionalHealing(String elementName, String locator) {
        // Check if healing should be enabled
        String environment = System.getProperty("test.environment", "dev");
        boolean healingEnabled = "staging".equals(environment) || 
                               "qa".equals(environment) ||
                               "true".equals(System.getProperty("auto.healing.enabled"));
        
        if (healingEnabled) {
            logger.info("Healing enabled for environment: " + environment);
            return findWithHealing(elementName, locator);
        } else {
            logger.info("Healing disabled for environment: " + environment);
            return simulateOriginalFind(locator);
        }
    }
    
    /**
     * INTEGRATION PATTERN 4: Enhanced Page Object Model
     * 
     * Upgrade your existing Page Objects to include auto-healing
     */
    public static class HealingEnabledPageObject {
        private final AutoHealingFramework framework;
        
        // Define your page elements (same as before, but with healing)
        private static final String LOGIN_USERNAME = "id=username";
        private static final String LOGIN_PASSWORD = "id=password";
        private static final String LOGIN_SUBMIT = "css=button[type='submit']";
        private static final String SUCCESS_MESSAGE = "xpath=//div[contains(@class,'success')]";
        
        public HealingEnabledPageObject(AutoHealingFramework framework) {
            this.framework = framework;
        }
        
        public void performLogin(String username, String password) {
            // Find elements with healing capability
            Object usernameField = framework.heal("WEB", "loginUsername", 
                                                 LOGIN_USERNAME, Object.class, null);
            Object passwordField = framework.heal("WEB", "loginPassword", 
                                                 LOGIN_PASSWORD, Object.class, null);
            Object submitButton = framework.heal("WEB", "loginSubmit", 
                                                LOGIN_SUBMIT, Object.class, null);
            
            // Perform actions (adapt to your WebDriver implementation)
            logger.info("Performing login with credentials: " + username);
            // usernameField.sendKeys(username);
            // passwordField.sendKeys(password);
            // submitButton.click();
        }
        
        public boolean isLoginSuccessful() {
            try {
                Object successMsg = framework.heal("WEB", "loginSuccess", 
                                                  SUCCESS_MESSAGE, Object.class, null);
                return successMsg != null;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * INTEGRATION PATTERN 5: Gradual Migration Approach
     * 
     * Start by healing only known problematic elements
     */
    public Object findWithGradualMigration(String elementName, String locator, 
                                          boolean isKnownProblematic) {
        if (isKnownProblematic) {
            // Use healing for elements that frequently break
            logger.info("Using healing for known problematic element: " + elementName);
            return findWithHealing(elementName, locator);
        } else {
            // Use original approach for stable elements
            logger.info("Using original approach for stable element: " + elementName);
            return simulateOriginalFind(locator);
        }
    }
    
    /**
     * COMPLETE INTEGRATION EXAMPLE
     * 
     * Shows how a typical test method would look before and after integration
     */
    public void demonstrateCompleteIntegration() {
        logger.info("=== INTEGRATION DEMONSTRATION ===");
        
        // Initialize healing (add to your @BeforeMethod)
        initializeAutoHealing();
        
        try {
            logger.info("\n--- SIMULATING TYPICAL TEST SCENARIO ---");
            
            // Example: Login test with healing
            Object usernameField = findWithHealing("usernameInput", "id=username");
            Object passwordField = findWithHealing("passwordInput", "id=password");
            
            // Example: Button with fallback locators
            Object loginButton = findWithSmartFallback("loginButton",
                "id=login-btn",                    // Primary
                "css=.login-button",               // Fallback 1
                "xpath=//button[text()='Login']",  // Fallback 2
                "css=button[type='submit']"        // Fallback 3
            );
            
            // Example: Conditional healing for unstable element
            Object dynamicContent = findWithConditionalHealing("dynamicElement", 
                                                              "css=.dynamic-content");
            
            logger.info("✓ All elements found successfully");
            logger.info("✓ Test execution completed with auto-healing support");
            
        } catch (Exception e) {
            logger.severe("✗ Test failed: " + e.getMessage());
        } finally {
            // Generate reports (add to your @AfterMethod)
            finalizeAutoHealing();
        }
    }
    
    /**
     * MIGRATION CHECKLIST
     * 
     * Step-by-step guide for integrating into existing projects
     */
    public void printMigrationChecklist() {
        logger.info("\n=== AUTO-HEALING INTEGRATION CHECKLIST ===");
        logger.info("□ 1. Add AutoHealingFramework dependency to project");
        logger.info("□ 2. Initialize framework in test setup method");
        logger.info("□ 3. Identify 5-10 most problematic elements in tests");
        logger.info("□ 4. Replace problematic findElement() calls with findWithHealing()");
        logger.info("□ 5. Add report generation to test teardown");
        logger.info("□ 6. Run tests and monitor healing success rates");
        logger.info("□ 7. Gradually expand healing to more elements");
        logger.info("□ 8. Configure environment-based healing rules");
        logger.info("□ 9. Integrate with CI/CD pipeline");
        logger.info("□ 10. Train team on healing patterns and best practices");
        logger.info("\n✓ Framework integration complete!");
    }
    
    /**
     * Utility method to simulate original element finding
     * Replace this with your actual WebDriver/automation logic
     */
    private Object simulateOriginalFind(String locator) {
        logger.fine("Simulating original element finding: " + locator);
        
        // Simulate occasional failures for demonstration
        if (locator.contains("unstable") || Math.random() < 0.1) {
            throw new RuntimeException("Simulated element not found: " + locator);
        }
        
        return new Object(); // Simulated element
    }
    
    /**
     * Main demonstration method
     */
    public static void main(String[] args) {
        ExistingTestIntegrationExample integration = new ExistingTestIntegrationExample();
        
        logger.info("🚀 MULTI-PLATFORM AUTO-HEALING FRAMEWORK");
        logger.info("    Integration Guide and Examples");
        logger.info("=====================================");
        
        // Show migration checklist
        integration.printMigrationChecklist();
        
        // Demonstrate complete integration
        integration.demonstrateCompleteIntegration();
        
        logger.info("\n🎉 Integration demonstration completed!");
        logger.info("   Framework is ready for production use.");
    }
}
