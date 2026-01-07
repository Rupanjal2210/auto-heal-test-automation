package com.autohealing.interceptor;

import com.autohealing.core.AutoHealingEngine;
import com.autohealing.adapters.WebPlatformAdapter;
import com.autohealing.reporting.HealingReporter;
import org.openqa.selenium.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * WebDriver wrapper that automatically triggers healing when element finding fails
 * Works transparently with existing test scripts without requiring code changes
 * 
 * Usage:
 * WebDriver driver = new ChromeDriver();
 * WebDriver healingDriver = new AutoHealingWebDriver(driver);
 * // Use healingDriver exactly like normal WebDriver - healing happens automatically
 */
public class AutoHealingWebDriver implements WebDriver, TakesScreenshot {
    
    private static final Logger logger = Logger.getLogger(AutoHealingWebDriver.class.getName());
    
    private final WebDriver originalDriver;
    private final AutoHealingEngine healingEngine;
    private final HealingReporter healingReporter;
    private final String sessionId;
    
    public AutoHealingWebDriver(WebDriver originalDriver) {
        this.originalDriver = originalDriver;
        this.healingEngine = AutoHealingEngine.getInstance();
        this.healingReporter = new HealingReporter();
        this.sessionId = "AutoHealing_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Register web platform adapter
        WebPlatformAdapter webAdapter = new WebPlatformAdapter(originalDriver);
        healingEngine.registerPlatformAdapter("WEB", webAdapter);
        healingEngine.setHealingEnabled(true);
        
        logger.info("AutoHealingWebDriver initialized with session: " + sessionId);
    }
    
    @Override
    public WebElement findElement(By by) {
        try {
            // First try the original driver
            return originalDriver.findElement(by);
        } catch (NoSuchElementException e) {
            // Trigger auto-healing
            return handleElementNotFoundException(by, e);
        }
    }
    
    @Override
    public java.util.List<WebElement> findElements(By by) {
        try {
            // First try the original driver
            java.util.List<WebElement> elements = originalDriver.findElements(by);
            if (!elements.isEmpty()) {
                return elements;
            }
            // If empty list, try healing
            WebElement healedElement = handleElementNotFoundException(by, new NoSuchElementException("No elements found"));
            if (healedElement != null) {
                return java.util.Arrays.asList(healedElement);
            }
            return elements;
        } catch (Exception e) {
            logger.warning("Error in findElements: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Handle NoSuchElementException by triggering auto-healing
     */
    private WebElement handleElementNotFoundException(By by, NoSuchElementException exception) {
        try {
            logger.info("NoSuchElementException detected for locator: " + by + " - triggering auto-healing");
            
            String originalLocator = by.toString();
            String elementId = generateElementId(originalLocator);
            
            long startTime = System.currentTimeMillis();
            
            // Attempt auto-healing using the engine
            WebElement healedElement = attemptHealing(elementId, originalLocator);
            
            long healingTime = System.currentTimeMillis() - startTime;
            
            if (healedElement != null) {
                // Record successful healing
                healingReporter.recordSuccess("WEB", "AutoHealingWrapper", elementId, 
                                            originalLocator, "healed_" + originalLocator, healingTime);
                
                // Take screenshot of healed element
                captureHealingScreenshot(elementId, "SUCCESS");
                
                logger.info("Auto-healing successful for element: " + elementId + " in " + healingTime + "ms");
                
                // Generate healing report
                generateHealingReport(elementId);
                
                return healedElement;
                
            } else {
                // Record healing failure
                healingReporter.recordFailure("WEB", "AutoHealingWrapper", elementId, 
                                             originalLocator, "Could not heal element with any strategy");
                
                // Take screenshot of failure
                captureHealingScreenshot(elementId, "FAILURE");
                
                logger.warning("Auto-healing failed for element: " + elementId);
                
                // Generate healing report
                generateHealingReport(elementId);
                
                // Re-throw the original exception
                throw exception;
            }
            
        } catch (Exception e) {
            logger.severe("Error during auto-healing process: " + e.getMessage());
            throw exception;
        }
    }
    
    /**
     * Attempt to heal the element using various strategies
     */
    private WebElement attemptHealing(String elementId, String originalLocator) {
        try {
            // Attempt healing using the engine
            WebElement healedElement = (WebElement) healingEngine.heal(
                "WEB", elementId, originalLocator, WebElement.class, originalDriver
            );
            
            if (healedElement != null) {
                return healedElement;
            }
            
            // If engine healing fails, try manual healing strategies
            return attemptManualHealing(originalLocator);
            
        } catch (Exception e) {
            logger.warning("Healing attempt failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Manual healing strategies when engine healing fails
     */
    private WebElement attemptManualHealing(String originalLocator) {
        // Generate alternative locators based on the original
        String[] alternativeLocators = generateAlternativeLocators(originalLocator);
        
        for (String locator : alternativeLocators) {
            try {
                By by = parseLocator(locator);
                WebElement element = originalDriver.findElement(by);
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
        // Extract the selector part from the By.toString() format
        String selector = extractSelectorFromByString(originalLocator);
        
        if (originalLocator.contains("By.id:")) {
            return new String[] {
                "[id='" + selector + "']",
                "[id*='" + selector + "']",
                "//*[@id='" + selector + "']",
                "//*[contains(@id,'" + selector + "')]",
                "[name='" + selector + "']"
            };
        } else if (originalLocator.contains("By.name:")) {
            return new String[] {
                "[name='" + selector + "']",
                "[name*='" + selector + "']",
                "//*[@name='" + selector + "']",
                "//*[contains(@name,'" + selector + "')]",
                "[id='" + selector + "']"
            };
        } else if (originalLocator.contains("By.cssSelector:")) {
            return new String[] {
                selector.replace("'", "\""),
                selector.replace("#", "[id='") + "']",
                selector.replace(".", "[class*='") + "']",
                "//*[contains(@class,'" + selector.replaceAll("[#.]", "") + "')]"
            };
        } else if (originalLocator.contains("By.xpath:")) {
            return new String[] {
                selector.replace("contains", "starts-with"),
                selector.replace("@id", "@name"),
                selector.replace("@name", "@id"),
                selector.replace("text()", "@value")
            };
        } else if (originalLocator.contains("By.className:")) {
            return new String[] {
                "[class*='" + selector + "']",
                "//*[contains(@class,'" + selector + "')]",
                "." + selector,
                "[class='" + selector + "']"
            };
        } else {
            // Generic alternatives
            return new String[] {
                "[id*='" + selector + "']",
                "[name*='" + selector + "']",
                "[class*='" + selector + "']",
                "//*[contains(@id,'" + selector + "')]",
                "//*[contains(@name,'" + selector + "')]"
            };
        }
    }
    
    /**
     * Extract selector value from By.toString() format
     */
    private String extractSelectorFromByString(String byString) {
        int startIndex = byString.indexOf(":") + 1;
        return byString.substring(startIndex).trim();
    }
    
    /**
     * Parse locator string to Selenium By object
     */
    private By parseLocator(String locator) {
        if (locator.startsWith("//") || locator.startsWith(".//")) {
            return By.xpath(locator);
        } else if (locator.startsWith("#")) {
            return By.id(locator.substring(1));
        } else if (locator.startsWith(".")) {
            return By.className(locator.substring(1));
        } else if (locator.contains("[") || locator.contains(":")) {
            return By.cssSelector(locator);
        } else {
            return By.id(locator);
        }
    }
    
    /**
     * Generate element ID from locator
     */
    private String generateElementId(String locator) {
        return "element_" + Math.abs(locator.hashCode());
    }
    
    /**
     * Capture screenshot during healing process
     */
    private void captureHealingScreenshot(String elementId, String status) {
        try {
            if (originalDriver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) originalDriver).getScreenshotAs(OutputType.BYTES);
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
    
    // Delegate all other WebDriver methods to the original driver
    
    @Override
    public void get(String url) {
        originalDriver.get(url);
    }
    
    @Override
    public String getCurrentUrl() {
        return originalDriver.getCurrentUrl();
    }
    
    @Override
    public String getTitle() {
        return originalDriver.getTitle();
    }
    
    @Override
    public String getPageSource() {
        return originalDriver.getPageSource();
    }
    
    @Override
    public void close() {
        originalDriver.close();
    }
    
    @Override
    public void quit() {
        // Generate final report before quitting
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String finalReportPath = "healing-reports/Final_AutoHealing_Session_Report_" + timestamp + ".html";
            healingReporter.generateReport(finalReportPath);
            logger.info("Final healing session report generated: " + finalReportPath);
        } catch (Exception e) {
            logger.warning("Failed to generate final report: " + e.getMessage());
        }
        
        originalDriver.quit();
    }
    
    @Override
    public java.util.Set<String> getWindowHandles() {
        return originalDriver.getWindowHandles();
    }
    
    @Override
    public String getWindowHandle() {
        return originalDriver.getWindowHandle();
    }
    
    @Override
    public TargetLocator switchTo() {
        return originalDriver.switchTo();
    }
    
    @Override
    public Navigation navigate() {
        return originalDriver.navigate();
    }
    
    @Override
    public Options manage() {
        return originalDriver.manage();
    }
    
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        if (originalDriver instanceof TakesScreenshot) {
            return ((TakesScreenshot) originalDriver).getScreenshotAs(target);
        }
        throw new UnsupportedOperationException("Driver does not support taking screenshots");
    }
    
    // Getter for the healing reporter
    public HealingReporter getHealingReporter() {
        return healingReporter;
    }
    
    // Getter for the original driver
    public WebDriver getOriginalDriver() {
        return originalDriver;
    }
}
