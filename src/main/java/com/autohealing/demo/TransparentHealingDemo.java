package com.autohealing.demo;

import com.autohealing.interceptor.AutoHealingWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.logging.Logger;

/**
 * Simple demonstration of transparent auto-healing
 * 
 * This demo shows how existing Selenium code can work with auto-healing
 * by simply wrapping the WebDriver - no other code changes needed!
 */
public class TransparentHealingDemo {
    
    private static final Logger logger = Logger.getLogger(TransparentHealingDemo.class.getName());
    
    public static void main(String[] args) {
        logger.info("Starting Transparent Auto-Healing Demo...");
        
        WebDriver driver = null;
        AutoHealingWebDriver healingDriver = null;
        
        try {
            // Standard Chrome setup
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Run headless for demo
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            
            // Regular WebDriver
            WebDriver baseDriver = new ChromeDriver(options);
            
            // Wrap with auto-healing - THIS IS THE ONLY CHANGE NEEDED!
            healingDriver = new AutoHealingWebDriver(baseDriver);
            driver = healingDriver; // Use as normal WebDriver
            
            logger.info("Auto-healing WebDriver initialized successfully");
            
            // From here on, use exactly like normal WebDriver
            demonstrateNormalUsage(driver);
            demonstrateHealingOnFailure(driver);
            
        } catch (Exception e) {
            logger.severe("Demo encountered error: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            if (healingDriver != null) {
                logger.info("\n=== HEALING SUMMARY ===");
                healingDriver.getHealingReporter().printSummary();
            }
            
            if (driver != null) {
                driver.quit();
                logger.info("Demo completed - driver closed");
            }
        }
    }
    
    /**
     * Demonstrate normal WebDriver usage - works exactly the same
     */
    private static void demonstrateNormalUsage(WebDriver driver) {
        logger.info("\n=== DEMONSTRATING NORMAL USAGE ===");
        
        try {
            // Navigate to a simple page
            driver.get("https://www.example.com");
            logger.info("✓ Successfully navigated to example.com");
            
            // Get page title - normal WebDriver operation
            String title = driver.getTitle();
            logger.info("✓ Page title: " + title);
            
            // Find an element that exists - normal operation
            WebElement bodyElement = driver.findElement(By.tagName("body"));
            logger.info("✓ Found body element: " + bodyElement.getTagName());
            
            // Find heading - should work normally
            WebElement heading = driver.findElement(By.tagName("h1"));
            logger.info("✓ Found heading: " + heading.getText());
            
        } catch (Exception e) {
            logger.warning("Normal usage test encountered issue: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrate healing when locators fail
     */
    private static void demonstrateHealingOnFailure(WebDriver driver) {
        logger.info("\n=== DEMONSTRATING AUTO-HEALING ON FAILURES ===");
        
        // Test 1: Non-existent ID
        try {
            logger.info("Attempting to find element with non-existent ID...");
            WebElement element = driver.findElement(By.id("this-id-definitely-does-not-exist"));
            logger.info("✓ HEALING SUCCESS! Found alternative element: " + element.getTagName());
            
        } catch (Exception e) {
            logger.info("✗ Healing could not find alternative for non-existent ID (expected)");
        }
        
        // Test 2: Wrong class name
        try {
            logger.info("Attempting to find element with wrong class name...");
            WebElement element = driver.findElement(By.className("non-existent-class-name"));
            logger.info("✓ HEALING SUCCESS! Found alternative element: " + element.getTagName());
            
        } catch (Exception e) {
            logger.info("✗ Healing could not find alternative for wrong class (expected)");
        }
        
        // Test 3: Wrong CSS selector
        try {
            logger.info("Attempting to find element with wrong CSS selector...");
            WebElement element = driver.findElement(By.cssSelector("#wrong-selector"));
            logger.info("✓ HEALING SUCCESS! Found alternative element: " + element.getTagName());
            
        } catch (Exception e) {
            logger.info("✗ Healing could not find alternative for wrong CSS (expected)");
        }
        
        // Test 4: Try to find something that might work
        try {
            logger.info("Attempting more realistic failing locator...");
            // This might actually find something through healing
            WebElement element = driver.findElement(By.cssSelector("div.content"));
            logger.info("✓ HEALING SUCCESS! Found element: " + element.getTagName());
            
        } catch (Exception e) {
            logger.info("✗ No suitable alternative found for div.content");
        }
    }
}
