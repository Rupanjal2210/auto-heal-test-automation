package com.autohealing.examples;

import com.autohealing.AutoHealingFramework;
import com.autohealing.adapters.WebPlatformAdapter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Example demonstrating web application auto-healing integration
 * Shows how to integrate with existing Selenium tests
 */
public class WebAutomationExample {
    
    private static final Logger logger = Logger.getLogger(WebAutomationExample.class.getName());
    
    private WebDriver driver;
    private AutoHealingFramework healingFramework;
    
    public void setup() {
        // Initialize WebDriver
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        
        // Initialize Auto-Healing Framework
        healingFramework = new AutoHealingFramework();
        
        // Register web platform adapter with the actual driver
        WebPlatformAdapter webAdapter = new WebPlatformAdapter(driver);
        healingFramework.registerPlatformAdapter("WEB", webAdapter);
        
        logger.info("Web automation with auto-healing initialized");
    }
    
    /**
     * Enhanced findElement method with auto-healing capability
     */
    public WebElement findElementWithHealing(String elementId, String locator) {
        try {
            // Try to find element normally first
            By by = parseLocator(locator);
            return driver.findElement(by);
            
        } catch (Exception e) {
            logger.warning("Normal element location failed for " + elementId);
            
            // Attempt auto-healing
            WebElement healedElement = healingFramework.heal("WEB", elementId, locator, WebElement.class, driver);
            
            if (healedElement != null) {
                logger.info("Successfully healed element: " + elementId);
                return healedElement;
            } else {
                logger.severe("Auto-healing failed for element: " + elementId);
                throw new RuntimeException("Element not found and healing failed: " + elementId);
            }
        }
    }
    
    /**
     * Example test method with auto-healing
     */
    public void testLoginWithHealing() {
        try {
            // Navigate to login page
            driver.get("https://example.com/login");
            
            // Find username field with auto-healing
            WebElement usernameField = findElementWithHealing("username-field", "id=username");
            usernameField.sendKeys("testuser");
            
            // Find password field with auto-healing
            WebElement passwordField = findElementWithHealing("password-field", "id=password");
            passwordField.sendKeys("testpass");
            
            // Find login button with auto-healing
            WebElement loginButton = findElementWithHealing("login-button", "id=login-btn");
            loginButton.click();
            
            // Verify successful login
            WebElement welcomeMessage = findElementWithHealing("welcome-msg", "css=.welcome-message");
            logger.info("Login successful: " + welcomeMessage.getText());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Test failed", e);
            throw e;
        }
    }
    
    /**
     * Example of storing element attributes for future healing
     */
    public void captureElementAttributesForHealing() {
        try {
            // Find an element that's currently working
            WebElement element = driver.findElement(By.id("stable-element"));
            
            // Get the web adapter and store attributes
            WebPlatformAdapter adapter = new WebPlatformAdapter(driver);
            var attributes = adapter.getElementAttributes(element);
            
            // Store for future healing (in real implementation, this would be automated)
            logger.info("Captured attributes for stable-element: " + attributes);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to capture element attributes", e);
        }
    }
    
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        
        // Generate healing report
        healingFramework.generateReport();
        
        logger.info("Web automation test completed");
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else {
            return By.xpath(locator);
        }
    }
    
    public static void main(String[] args) {
        WebAutomationExample example = new WebAutomationExample();
        
        try {
            example.setup();
            example.testLoginWithHealing();
            example.captureElementAttributesForHealing();
        } finally {
            example.tearDown();
        }
    }
}

