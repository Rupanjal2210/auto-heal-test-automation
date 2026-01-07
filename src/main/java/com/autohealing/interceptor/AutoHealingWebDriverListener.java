package com.autohealing.interceptor;

import com.autohealing.core.AutoHealingEngine;
import com.autohealing.adapters.WebPlatformAdapter;
import com.autohealing.reporting.HealingReporter;
import org.openqa.selenium.*;
import org.openqa.selenium.support.events.WebDriverEventListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * WebDriver Event Listener that automatically triggers healing when element finding fails
 * Works transparently with existing test scripts without requiring code changes
 */
@SuppressWarnings("deprecation")
public class AutoHealingWebDriverListener implements WebDriverEventListener {
    
    private static final Logger logger = Logger.getLogger(AutoHealingWebDriverListener.class.getName());
    
    private final AutoHealingEngine healingEngine;
    private final HealingReporter healingReporter;
    private final String sessionId;
    
    public AutoHealingWebDriverListener() {
        this.healingEngine = AutoHealingEngine.getInstance();
        this.healingReporter = new HealingReporter();
        this.sessionId = "AutoHealing_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Register web platform adapter if not already registered
        if (!healingEngine.isHealingEnabled()) {
            healingEngine.setHealingEnabled(true);
        }
        
        logger.info("AutoHealingWebDriverListener initialized with session: " + sessionId);
    }
    
    @Override
    public void beforeFindBy(By by, WebElement element, WebDriver driver) {
        // Called before any findElement or findElements operation
        logger.fine("Attempting to find element with locator: " + by.toString());
    }
    
    @Override
    public void afterFindBy(By by, WebElement element, WebDriver driver) {
        // Called after successful findElement operation
        logger.fine("Successfully found element with locator: " + by.toString());
    }
    
    @Override
    public void onException(Throwable throwable, WebDriver driver) {
        // This is called when any WebDriver operation throws an exception
        if (throwable instanceof NoSuchElementException) {
            handleElementNotFoundException((NoSuchElementException) throwable, driver);
        } else if (throwable instanceof StaleElementReferenceException) {
            handleStaleElementException((StaleElementReferenceException) throwable, driver);
        } else if (throwable instanceof ElementNotInteractableException) {
            handleElementNotInteractableException((ElementNotInteractableException) throwable, driver);
        }
    }
    
    /**
     * Handle NoSuchElementException by triggering auto-healing
     */
    private void handleElementNotFoundException(NoSuchElementException exception, WebDriver driver) {
        try {
            logger.info("NoSuchElementException detected - triggering auto-healing");
            
            // Extract locator information from exception message
            String originalLocator = extractLocatorFromException(exception);
            String elementId = generateElementId(originalLocator);
            
            if (originalLocator != null) {
                long startTime = System.currentTimeMillis();
                
                // Attempt auto-healing
                WebElement healedElement = attemptHealing(driver, elementId, originalLocator);
                
                long healingTime = System.currentTimeMillis() - startTime;
                
                if (healedElement != null) {
                    // Record successful healing
                    healingReporter.recordSuccess("WEB", "AutoHealingInterceptor", elementId, 
                                                originalLocator, "healed_" + originalLocator, healingTime);
                    
                    // Take screenshot of healed element
                    captureHealingScreenshot(driver, elementId, "SUCCESS");
                    
                    logger.info("Auto-healing successful for element: " + elementId + " in " + healingTime + "ms");
                    
                    // Store healed element for potential reuse
                    storeHealedElement(originalLocator, healedElement);
                    
                } else {
                    // Record healing failure
                    healingReporter.recordFailure("WEB", "AutoHealingInterceptor", elementId, 
                                                 originalLocator, "Could not heal element with any strategy");
                    
                    // Take screenshot of failure
                    captureHealingScreenshot(driver, elementId, "FAILURE");
                    
                    logger.warning("Auto-healing failed for element: " + elementId);
                }
                
                // Generate healing report
                generateHealingReport(elementId);
                
            } else {
                logger.warning("Could not extract locator information from exception: " + exception.getMessage());
            }
            
        } catch (Exception e) {
            logger.severe("Error during auto-healing process: " + e.getMessage());
        }
    }
    
    /**
     * Handle StaleElementReferenceException by attempting to re-find the element
     */
    private void handleStaleElementException(StaleElementReferenceException exception, WebDriver driver) {
        logger.info("StaleElementReferenceException detected - attempting element recovery");
        
        // For stale elements, we need to re-find them using cached locator information
        // This would require maintaining a cache of element-to-locator mappings
        String elementInfo = extractElementInfoFromException(exception);
        captureHealingScreenshot(driver, "stale_element", "STALE_REFERENCE");
    }
    
    /**
     * Handle ElementNotInteractableException by trying alternative interaction methods
     */
    private void handleElementNotInteractableException(ElementNotInteractableException exception, WebDriver driver) {
        logger.info("ElementNotInteractableException detected - attempting interaction recovery");
        
        String elementInfo = extractElementInfoFromException(exception);
        captureHealingScreenshot(driver, "interaction_failed", "NOT_INTERACTABLE");
        
        // Could implement strategies like:
        // - Scroll element into view
        // - Wait for element to become interactable
        // - Use JavaScript to interact with element
        // - Find alternative similar elements
    }
    
    /**
     * Attempt to heal the element using various strategies
     */
    private WebElement attemptHealing(WebDriver driver, String elementId, String originalLocator) {
        try {
            // Register web platform adapter with current driver
            WebPlatformAdapter webAdapter = new WebPlatformAdapter(driver);
            healingEngine.registerPlatformAdapter("WEB", webAdapter);
            
            // Attempt healing using the engine
            WebElement healedElement = (WebElement) healingEngine.heal(
                "WEB", elementId, originalLocator, WebElement.class, driver
            );
            
            if (healedElement != null) {
                return healedElement;
            }
            
            // If engine healing fails, try manual healing strategies
            return attemptManualHealing(driver, originalLocator);
            
        } catch (Exception e) {
            logger.warning("Healing attempt failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Manual healing strategies when engine healing fails
     */
    private WebElement attemptManualHealing(WebDriver driver, String originalLocator) {
        // Generate alternative locators based on the original
        String[] alternativeLocators = generateAlternativeLocators(originalLocator);
        
        for (String locator : alternativeLocators) {
            try {
                By by = parseLocator(locator);
                WebElement element = driver.findElement(by);
                if (element != null && element.isDisplayed()) {
                    logger.info("Manual healing successful with locator: " + locator);
                    return element;
                }
            } catch (Exception e) {
                // Continue to next locator
            }
        }
        
        return null;
    }
    
    /**
     * Generate alternative locators based on the original failed locator
     */
    private String[] generateAlternativeLocators(String originalLocator) {
        // This method generates intelligent alternatives based on the original locator
        if (originalLocator.startsWith("id=")) {
            String id = originalLocator.substring(3);
            return new String[] {
                "css=[id='" + id + "']",
                "css=[id*='" + id + "']",
                "xpath=//*[@id='" + id + "']",
                "xpath=//*[contains(@id,'" + id + "')]",
                "name=" + id,
                "css=[name='" + id + "']"
            };
        } else if (originalLocator.startsWith("name=")) {
            String name = originalLocator.substring(5);
            return new String[] {
                "css=[name='" + name + "']",
                "css=[name*='" + name + "']",
                "xpath=//*[@name='" + name + "']",
                "xpath=//*[contains(@name,'" + name + "')]",
                "id=" + name,
                "css=[id='" + name + "']"
            };
        } else if (originalLocator.startsWith("css=") || originalLocator.startsWith("By.cssSelector:")) {
            String css = originalLocator.contains(":") ? 
                originalLocator.substring(originalLocator.indexOf(":") + 1).trim() :
                originalLocator.substring(4);
            return new String[] {
                "css=" + css.replace("'", "\""),
                "css=" + css.replace("#", "[id='") + "']",
                "css=" + css.replace(".", "[class*='") + "']",
                "xpath=//" + css.split("[#.]")[0] + "[contains(@class,'" + css.split("[#.]")[1] + "')]"
            };
        } else if (originalLocator.startsWith("xpath=") || originalLocator.startsWith("By.xpath:")) {
            // For XPath, generate CSS alternatives
            return new String[] {
                originalLocator.replace("contains", "starts-with"),
                originalLocator.replace("@id", "@name"),
                originalLocator.replace("@name", "@id"),
                originalLocator.replace("text()", "@value")
            };
        } else {
            // Generic alternatives
            return new String[] {
                "css=[id*='" + originalLocator + "']",
                "css=[name*='" + originalLocator + "']",
                "css=[class*='" + originalLocator + "']",
                "xpath=//*[contains(@id,'" + originalLocator + "')]",
                "xpath=//*[contains(@name,'" + originalLocator + "')]",
                "xpath=//*[contains(@class,'" + originalLocator + "')]"
            };
        }
    }
    
    /**
     * Parse locator string to Selenium By object
     */
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("//")) {
            return By.xpath(locator);
        } else if (locator.startsWith("#")) {
            return By.id(locator.substring(1));
        } else if (locator.startsWith(".")) {
            return By.className(locator.substring(1));
        } else {
            return By.cssSelector(locator);
        }
    }
    
    /**
     * Extract locator information from NoSuchElementException
     */
    private String extractLocatorFromException(NoSuchElementException exception) {
        String message = exception.getMessage();
        
        if (message.contains("By.id:")) {
            return "id=" + extractValueFromMessage(message, "By.id:");
        } else if (message.contains("By.name:")) {
            return "name=" + extractValueFromMessage(message, "By.name:");
        } else if (message.contains("By.cssSelector:")) {
            return "css=" + extractValueFromMessage(message, "By.cssSelector:");
        } else if (message.contains("By.xpath:")) {
            return "xpath=" + extractValueFromMessage(message, "By.xpath:");
        } else if (message.contains("By.className:")) {
            return "class=" + extractValueFromMessage(message, "By.className:");
        }
        
        return null;
    }
    
    /**
     * Extract value from exception message
     */
    private String extractValueFromMessage(String message, String prefix) {
        int startIndex = message.indexOf(prefix) + prefix.length();
        int endIndex = message.indexOf("}", startIndex);
        if (endIndex == -1) endIndex = message.length();
        
        return message.substring(startIndex, endIndex).trim();
    }
    
    /**
     * Generate element ID from locator
     */
    private String generateElementId(String locator) {
        return "element_" + Math.abs(locator.hashCode());
    }
    
    /**
     * Extract element information from other exceptions
     */
    private String extractElementInfoFromException(Exception exception) {
        return exception.getMessage();
    }
    
    /**
     * Capture screenshot during healing process
     */
    private void captureHealingScreenshot(WebDriver driver, String elementId, String status) {
        try {
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String filename = "healing-screenshots/AutoHealing_" + elementId + "_" + status + "_" + timestamp + ".png";
                
                java.nio.file.Path screenshotPath = java.nio.file.Paths.get(filename);
                java.nio.file.Files.createDirectories(screenshotPath.getParent());
                java.nio.file.Files.write(screenshotPath, screenshot);
                
                logger.info("Healing screenshot captured: " + filename);
            }
        } catch (Exception e) {
            logger.warning("Failed to capture healing screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Store healed element for potential reuse
     */
    private void storeHealedElement(String originalLocator, WebElement healedElement) {
        // Implementation could store in cache for future use
        logger.fine("Storing healed element mapping: " + originalLocator);
    }
    
    /**
     * Generate healing report
     */
    private void generateHealingReport(String elementId) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportPath = "healing-reports/AutoHealing_" + elementId + "_Report_" + timestamp + ".html";
            
            healingReporter.generateReport(reportPath);
            healingReporter.printSummary();
            
            logger.info("Healing report generated: " + reportPath);
            
        } catch (Exception e) {
            logger.warning("Failed to generate healing report: " + e.getMessage());
        }
    }
    
    public HealingReporter getHealingReporter() {
        return healingReporter;
    }
    
    // Other WebDriverEventListener methods (can be left empty for now)
    
    @Override
    public void beforeNavigateTo(String url, WebDriver driver) {}
    
    @Override
    public void afterNavigateTo(String url, WebDriver driver) {}
    
    @Override
    public void beforeNavigateBack(WebDriver driver) {}
    
    @Override
    public void afterNavigateBack(WebDriver driver) {}
    
    @Override
    public void beforeNavigateForward(WebDriver driver) {}
    
    @Override
    public void afterNavigateForward(WebDriver driver) {}
    
    @Override
    public void beforeNavigateRefresh(WebDriver driver) {}
    
    @Override
    public void afterNavigateRefresh(WebDriver driver) {}
    
    @Override
    public void beforeChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {}
    
    @Override
    public void afterChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {}
    
    @Override
    public void beforeClickOn(WebElement element, WebDriver driver) {}
    
    @Override
    public void afterClickOn(WebElement element, WebDriver driver) {}
    
    @Override
    public void beforeScript(String script, WebDriver driver) {}
    
    @Override
    public void afterScript(String script, WebDriver driver) {}
    
    @Override
    public void beforeSwitchToWindow(String windowName, WebDriver driver) {}
    
    @Override
    public void afterSwitchToWindow(String windowName, WebDriver driver) {}
}
