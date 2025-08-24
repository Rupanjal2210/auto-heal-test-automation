package com.autohealing.integration;

import com.autohealing.AutoHealingFramework;
import com.autohealing.adapters.WebPlatformAdapter;

import java.util.logging.Logger;

/**
 * PRACTICAL INTEGRATION EXAMPLE
 * 
 * This shows exactly how to integrate auto-healing with your existing tests.
 * Copy these patterns into your test classes.
 */
public class PracticalIntegrationExample {
    
    private static final Logger logger = Logger.getLogger(PracticalIntegrationExample.class.getName());
    
    // ========================================
    // STEP 1: ADD THESE FIELDS TO YOUR TEST CLASS
    // ========================================
    private AutoHealingFramework healingFramework;
    
    // ========================================
    // STEP 2: ADD THIS SETUP METHOD
    // ========================================
    public void setupAutoHealing() {
        healingFramework = new AutoHealingFramework();
        
        // For Selenium WebDriver tests:
        // healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
        
        // For Windows app tests:
        // healingFramework.registerPlatformAdapter("WINDOWS", new WindowsPlatformAdapter());
        
        // Configure healing behavior
        healingFramework.getConfiguration().setHealingEnabled(true);
        healingFramework.getConfiguration().setMaxHealingAttempts(3);
        
        logger.info("Auto-healing framework initialized and ready");
    }
    
    // ========================================
    // STEP 3: ADD THIS WRAPPER METHOD
    // ========================================
    /**
     * USE THIS METHOD INSTEAD OF driver.findElement()
     * 
     * Replace:  WebElement button = driver.findElement(By.id("submit"));
     * With:     WebElement button = findElementWithHealing("submitButton", "id=submit");
     */
    private Object findElementWithHealing(String elementId, String locator) {
        try {
            // First attempt: try original locator (fast path)
            logger.info("Finding element '" + elementId + "' with locator: " + locator);
            
            // In real implementation, you'd use actual WebDriver here:
            // return driver.findElement(parseLocator(locator));
            
            // For this demo, we'll simulate occasional failures
            if (shouldSimulateFailure(elementId)) {
                throw new RuntimeException("Simulated locator failure for demo");
            }
            
            logger.info("Element found successfully with original locator");
            return new MockWebElement(elementId); // In real code: return actual WebElement
            
        } catch (Exception e) {
            logger.warning("Original locator failed for '" + elementId + "': " + e.getMessage());
            
            // Second attempt: use auto-healing
            Object healedElement = healingFramework.heal("WEB", elementId, locator, Object.class, null);
            
            if (healedElement != null) {
                logger.info("✅ Successfully healed element: " + elementId);
                return healedElement;
            } else {
                logger.severe("❌ Auto-healing failed for element: " + elementId);
                throw new RuntimeException("Element not found and healing failed: " + elementId);
            }
        }
    }
    
    // ========================================
    // STEP 4: EXAMPLE TEST TRANSFORMATIONS
    // ========================================
    
    /**
     * BEFORE: Original brittle test
     */
    public void originalBrittleTest() {
        logger.info("=== ORIGINAL BRITTLE TEST (BEFORE) ===");
        
        // Original approach - breaks when UI changes:
        // driver.get("https://example.com/login");
        // WebElement username = driver.findElement(By.id("username"));
        // WebElement password = driver.findElement(By.id("password"));
        // WebElement loginBtn = driver.findElement(By.xpath("//button[@class='login-btn']"));
        // 
        // username.sendKeys("testuser");
        // password.sendKeys("password123");
        // loginBtn.click();
        
        logger.info("Original test uses direct driver.findElement() calls");
        logger.info("❌ Breaks when developers change IDs, classes, or structure");
    }
    
    /**
     * AFTER: Enhanced test with auto-healing
     */
    public void enhancedTestWithHealing() {
        setupAutoHealing();
        
        logger.info("=== ENHANCED TEST WITH AUTO-HEALING (AFTER) ===");
        
        // Enhanced approach - automatically heals when UI changes:
        Object username = findElementWithHealing("usernameField", "id=username");
        Object password = findElementWithHealing("passwordField", "id=password");
        Object loginBtn = findElementWithHealing("loginButton", "xpath=//button[@class='login-btn']");
        
        // Same test logic, but now self-healing:
        // username.sendKeys("testuser");
        // password.sendKeys("password123");
        // loginBtn.click();
        
        logger.info("✅ Enhanced test automatically recovers from UI changes");
        logger.info("✅ Meaningful element IDs help with healing");
        logger.info("✅ Test continues to work even when locators break");
    }
    
    // ========================================
    // STEP 5: PAGE OBJECT PATTERN INTEGRATION
    // ========================================
    
    /**
     * Enhanced Page Object that uses auto-healing
     */
    public static class LoginPageWithHealing {
        private final AutoHealingFramework healingFramework;
        private final Object driver; // In real code: WebDriver driver
        
        // Keep your existing locator constants
        private static final String USERNAME_FIELD = "id=username";
        private static final String PASSWORD_FIELD = "id=password";
        private static final String LOGIN_BUTTON = "xpath=//button[@class='login-btn']";
        
        public LoginPageWithHealing(Object driver, AutoHealingFramework healingFramework) {
            this.driver = driver;
            this.healingFramework = healingFramework;
        }
        
        public void login(String username, String password) {
            // Replace driver.findElement() with healing calls:
            Object usernameField = healingFramework.heal("WEB", "usernameField", USERNAME_FIELD, Object.class, driver);
            Object passwordField = healingFramework.heal("WEB", "passwordField", PASSWORD_FIELD, Object.class, driver);
            Object loginButton = healingFramework.heal("WEB", "loginButton", LOGIN_BUTTON, Object.class, driver);
            
            // Same interaction logic:
            // usernameField.sendKeys(username);
            // passwordField.sendKeys(password);
            // loginButton.click();
            
            Logger.getLogger(LoginPageWithHealing.class.getName()).info("Login performed with healing-enabled elements");
        }
        
        public boolean isLoginSuccessful() {
            try {
                Object successMessage = healingFramework.heal("WEB", "successMessage", "css=.success-message", Object.class, driver);
                return successMessage != null; // In real code: successMessage.isDisplayed()
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    // ========================================
    // STEP 6: BASE TEST CLASS PATTERN
    // ========================================
    
    /**
     * Create a base class like this for all your tests to extend
     */
    public static abstract class BaseTestWithHealing {
        protected AutoHealingFramework healingFramework;
        // protected WebDriver driver; // Your existing WebDriver
        
        // Call this in your @BeforeMethod
        protected void setupHealing() {
            healingFramework = new AutoHealingFramework();
            // healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
            
            healingFramework.getConfiguration().setHealingEnabled(true);
            healingFramework.getConfiguration().setMaxHealingAttempts(3);
        }
        
        // Use this method in all your tests instead of driver.findElement()
        protected Object findElement(String elementId, String locator) {
            return healingFramework.heal("WEB", elementId, locator, Object.class, null);
        }
        
        // Call this in your @AfterMethod
        protected void generateHealingReport() {
            healingFramework.getReporter().generateReport("healing-report-" + System.currentTimeMillis() + ".html");
            
            double successRate = healingFramework.getReporter().getSuccessRate();
            int totalAttempts = healingFramework.getReporter().getTotalAttempts();
            
            System.out.println("🔍 Healing Session Summary:");
            System.out.println("   Success Rate: " + successRate + "%");
            System.out.println("   Total Attempts: " + totalAttempts);
        }
    }
    
    // ========================================
    // STEP 7: CONDITIONAL HEALING
    // ========================================
    
    public void demonstrateConditionalHealing() {
        logger.info("=== CONDITIONAL HEALING EXAMPLE ===");
        
        // Enable healing only in certain environments
        String environment = System.getProperty("test.env", "local");
        boolean shouldUseHealing = environment.equals("ci") || environment.equals("staging");
        
        if (shouldUseHealing) {
            setupAutoHealing();
            logger.info("✅ Auto-healing ENABLED for environment: " + environment);
        } else {
            logger.info("⚠️ Auto-healing DISABLED for environment: " + environment);
            // Use original element finding in stable environments
        }
    }
    
    // ========================================
    // UTILITY METHODS FOR DEMO
    // ========================================
    
    private boolean shouldSimulateFailure(String elementId) {
        // Simulate failures for some elements to demonstrate healing
        return elementId.contains("username") || elementId.contains("login");
    }
    
    // Mock class for demonstration (in real code, you'd use actual WebElement)
    private static class MockWebElement {
        private final String elementId;
        
        public MockWebElement(String elementId) {
            this.elementId = elementId;
        }
        
        @Override
        public String toString() {
            return "MockWebElement{id='" + elementId + "'}";
        }
    }
    
    // ========================================
    // MAIN METHOD - DEMONSTRATES ALL PATTERNS
    // ========================================
    
    public static void main(String[] args) {
        PracticalIntegrationExample example = new PracticalIntegrationExample();
        
        System.out.println("🚀 PRACTICAL INTEGRATION EXAMPLES");
        System.out.println("==================================");
        
        // Show before vs after
        example.originalBrittleTest();
        System.out.println();
        example.enhancedTestWithHealing();
        System.out.println();
        
        // Demonstrate Page Object integration
        System.out.println("=== PAGE OBJECT INTEGRATION ===");
        example.setupAutoHealing();
        LoginPageWithHealing loginPage = new LoginPageWithHealing(null, example.healingFramework);
        loginPage.login("testuser", "password123");
        boolean success = loginPage.isLoginSuccessful();
        System.out.println("Login successful: " + success);
        System.out.println();
        
        // Show conditional healing
        example.demonstrateConditionalHealing();
        System.out.println();
        
        System.out.println("📋 INTEGRATION CHECKLIST:");
        System.out.println("✅ 1. Add AutoHealingFramework field to test class");
        System.out.println("✅ 2. Initialize framework in @BeforeMethod");
        System.out.println("✅ 3. Add findElementWithHealing() wrapper method");
        System.out.println("✅ 4. Replace driver.findElement() calls");
        System.out.println("✅ 5. Use meaningful element IDs");
        System.out.println("✅ 6. Generate healing reports");
        System.out.println();
        System.out.println("🎯 RESULT: Self-healing tests that recover from UI changes!");
    }
}
