package com.autohealing.examples;

import com.autohealing.interceptor.AutoHealingWebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Example test that demonstrates transparent auto-healing for existing test scripts
 * 
 * This test shows how existing Selenium tests can use auto-healing without changing
 * any of their existing code - just wrap the WebDriver with AutoHealingWebDriver
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransparentHealingExampleTest {
    
    private static final Logger logger = Logger.getLogger(TransparentHealingExampleTest.class.getName());
    
    private WebDriver driver;
    private WebDriverWait wait;
    private AutoHealingWebDriver healingDriver;
    
    @BeforeEach
    void setUp() {
        logger.info("Setting up test with transparent auto-healing...");
        
        // Standard WebDriver setup
        WebDriver baseDriver = new ChromeDriver();
        
        // Wrap with auto-healing - this is the ONLY change needed!
        healingDriver = new AutoHealingWebDriver(baseDriver);
        driver = healingDriver; // Use as normal WebDriver
        
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        logger.info("Auto-healing test setup complete");
    }
    
    @AfterEach
    void tearDown() {
        if (driver != null) {
            logger.info("Generating final healing report...");
            // Get healing statistics
            if (healingDriver != null) {
                healingDriver.getHealingReporter().printSummary();
            }
            
            driver.quit();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Google Search with Transparent Healing")
    void testGoogleSearchWithHealing() {
        logger.info("Starting Google search test with transparent healing...");
        
        // Navigate to Google - normal WebDriver code
        driver.get("https://www.google.com");
        
        // This might fail on some systems due to cookie consent, etc.
        // But healing will automatically try alternative locators
        try {
            WebElement searchBox = driver.findElement(By.name("q"));
            searchBox.sendKeys("Selenium WebDriver auto-healing");
            
            // Submit search - might fail with normal locator
            WebElement searchButton = driver.findElement(By.name("btnK"));
            searchButton.click();
            
            // Wait for results - healing will handle if elements change
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search")));
            
            logger.info("Google search test completed successfully");
            
        } catch (Exception e) {
            logger.warning("Test encountered issues, but healing should have attempted recovery: " + e.getMessage());
            // In a real test, you might want to handle this differently
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Form Interaction with Healing")
    void testFormWithHealing() {
        logger.info("Starting form interaction test...");
        
        // Navigate to a test form page
        driver.get("https://demoqa.com/text-box");
        
        try {
            // These selectors might break over time - healing will fix them
            WebElement fullNameField = driver.findElement(By.id("userName"));
            fullNameField.sendKeys("Test User");
            
            WebElement emailField = driver.findElement(By.id("userEmail"));
            emailField.sendKeys("test@example.com");
            
            WebElement addressField = driver.findElement(By.id("currentAddress"));
            addressField.sendKeys("123 Test Street");
            
            // Submit button might have changing attributes
            WebElement submitButton = driver.findElement(By.id("submit"));
            submitButton.click();
            
            // Verify output appeared
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("output")));
            
            logger.info("Form interaction test completed successfully");
            
        } catch (Exception e) {
            logger.warning("Form test encountered issues: " + e.getMessage());
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Dynamic Content with Healing")
    void testDynamicContentWithHealing() {
        logger.info("Starting dynamic content test...");
        
        // Navigate to page with dynamic content
        driver.get("https://the-internet.herokuapp.com/dynamic_loading/1");
        
        try {
            // Click start button
            WebElement startButton = driver.findElement(By.cssSelector("#start button"));
            startButton.click();
            
            // Wait for dynamic content to appear
            // The healing wrapper will help if the locator changes
            WebElement finishText = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("finish"))
            );
            
            // Verify the text
            String text = finishText.getText();
            logger.info("Dynamic content loaded: " + text);
            
            Assertions.assertTrue(text.contains("Hello World!"), 
                "Expected 'Hello World!' text to appear");
            
            logger.info("Dynamic content test completed successfully");
            
        } catch (Exception e) {
            logger.warning("Dynamic content test encountered issues: " + e.getMessage());
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Failing Locators with Healing Recovery")
    void testFailingLocatorsWithHealing() {
        logger.info("Testing intentionally failing locators to demonstrate healing...");
        
        driver.get("https://www.example.com");
        
        try {
            // Use intentionally wrong locators to trigger healing
            logger.info("Attempting to find element with incorrect ID...");
            WebElement nonExistentElement = driver.findElement(By.id("this-id-does-not-exist"));
            
            // If we get here, healing found an alternative
            logger.info("Healing successfully found alternative element: " + nonExistentElement.getTagName());
            
        } catch (Exception e) {
            logger.info("Element not found even with healing - this is expected for this test");
        }
        
        try {
            // Try another failing locator
            logger.info("Attempting to find element with incorrect class...");
            WebElement anotherElement = driver.findElement(By.className("non-existent-class"));
            
            logger.info("Healing found alternative for class locator: " + anotherElement.getTagName());
            
        } catch (Exception e) {
            logger.info("Class locator also failed - demonstrating healing attempt");
        }
        
        // Now try something that should exist and work
        try {
            WebElement workingElement = driver.findElement(By.tagName("h1"));
            logger.info("Found working element: " + workingElement.getText());
            
        } catch (Exception e) {
            logger.warning("Even basic tag locator failed: " + e.getMessage());
        }
    }
}
