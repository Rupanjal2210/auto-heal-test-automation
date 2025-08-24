package com.autohealing.adapters;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;
import java.util.logging.Logger;
import java.util.*;

/**
 * Web platform adapter for Selenium WebDriver
 * Supports healing for web applications using various locator strategies
 */
public class WebPlatformAdapter implements PlatformAdapter {
    
    private static final Logger logger = Logger.getLogger(WebPlatformAdapter.class.getName());
    private WebDriver driver;
    
    public WebPlatformAdapter(WebDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public String getPlatformType() {
        return "WEB";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T findElement(String locator, Class<T> expectedType, Object context) {
        try {
            By by = parseLocator(locator);
            WebElement element = driver.findElement(by);
            return (T) element;
        } catch (Exception e) {
            logger.fine("Failed to find element with locator: " + locator);
            return null;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> findElements(String locator, Class<T> expectedType, Object context) {
        try {
            By by = parseLocator(locator);
            List<WebElement> elements = driver.findElements(by);
            return (List<T>) elements;
        } catch (Exception e) {
            logger.fine("Failed to find elements with locator: " + locator);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<String> getAllPossibleLocators(Object element, Object context) {
        if (!(element instanceof WebElement)) {
            return new ArrayList<>();
        }
        
        WebElement webElement = (WebElement) element;
        List<String> locators = new ArrayList<>();
        
        try {
            // ID locator
            String id = webElement.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                locators.add("id=" + id);
            }
            
            // Name locator
            String name = webElement.getAttribute("name");
            if (name != null && !name.isEmpty()) {
                locators.add("name=" + name);
            }
            
            // Class name locator
            String className = webElement.getAttribute("class");
            if (className != null && !className.isEmpty()) {
                locators.add("className=" + className);
            }
            
            // Tag name locator
            String tagName = webElement.getTagName();
            if (tagName != null && !tagName.isEmpty()) {
                locators.add("tagName=" + tagName);
            }
            
            // Link text for anchor tags
            if ("a".equals(tagName)) {
                String linkText = webElement.getText();
                if (linkText != null && !linkText.isEmpty()) {
                    locators.add("linkText=" + linkText);
                    locators.add("partialLinkText=" + linkText.substring(0, Math.min(linkText.length(), 10)));
                }
            }
            
            // CSS selectors
            String cssSelector = generateCssSelector(webElement);
            if (cssSelector != null) {
                locators.add("css=" + cssSelector);
            }
            
            // XPath
            String xpath = generateXPath(webElement);
            if (xpath != null) {
                locators.add("xpath=" + xpath);
            }
            
        } catch (Exception e) {
            logger.warning("Error generating locators: " + e.getMessage());
        }
        
        return locators;
    }
    
    @Override
    public Map<String, String> getElementAttributes(Object element) {
        if (!(element instanceof WebElement)) {
            return new HashMap<>();
        }
        
        WebElement webElement = (WebElement) element;
        Map<String, String> attributes = new HashMap<>();
        
        try {
            // Get common attributes
            String[] commonAttrs = {"id", "name", "class", "type", "value", "href", "src", "alt", "title"};
            for (String attr : commonAttrs) {
                String value = webElement.getAttribute(attr);
                if (value != null) {
                    attributes.put(attr, value);
                }
            }
            
            // Get all attributes using JavaScript
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                "var attrs = {}; " +
                "for (var i = 0; i < arguments[0].attributes.length; i++) { " +
                "    var attr = arguments[0].attributes[i]; " +
                "    attrs[attr.name] = attr.value; " +
                "} " +
                "return attrs;", webElement);
            
            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> jsAttrs = (Map<String, String>) result;
                attributes.putAll(jsAttrs);
            }
            
        } catch (Exception e) {
            logger.warning("Error getting element attributes: " + e.getMessage());
        }
        
        return attributes;
    }
    
    @Override
    public String getElementText(Object element) {
        if (!(element instanceof WebElement)) {
            return "";
        }
        
        try {
            return ((WebElement) element).getText();
        } catch (Exception e) {
            logger.warning("Error getting element text: " + e.getMessage());
            return "";
        }
    }
    
    @Override
    public boolean isElementDisplayed(Object element) {
        if (!(element instanceof WebElement)) {
            return false;
        }
        
        try {
            return ((WebElement) element).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isElementEnabled(Object element) {
        if (!(element instanceof WebElement)) {
            return false;
        }
        
        try {
            return ((WebElement) element).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getPageSource(Object context) {
        try {
            return driver.getPageSource();
        } catch (Exception e) {
            logger.warning("Error getting page source: " + e.getMessage());
            return "";
        }
    }
    
    @Override
    public byte[] takeScreenshot(Object context) {
        try {
            if (driver instanceof TakesScreenshot) {
                return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            }
        } catch (Exception e) {
            logger.warning("Error taking screenshot: " + e.getMessage());
        }
        return new byte[0];
    }
    
    @Override
    public void clickElement(Object element) {
        if (element instanceof WebElement) {
            try {
                ((WebElement) element).click();
            } catch (Exception e) {
                logger.warning("Error clicking element: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void typeText(Object element, String text) {
        if (element instanceof WebElement) {
            try {
                WebElement webElement = (WebElement) element;
                webElement.clear();
                webElement.sendKeys(text);
            } catch (Exception e) {
                logger.warning("Error typing text: " + e.getMessage());
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getAllSimilarElements(String baseLocator, Class<T> expectedType, Object context) {
        try {
            By by = parseLocator(baseLocator);
            List<WebElement> elements = driver.findElements(by);
            return (List<T>) elements;
        } catch (Exception e) {
            logger.fine("Error finding similar elements: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public void initialize(Map<String, Object> config) {
        logger.info("Initialized Web Platform Adapter");
    }
    
    @Override
    public void cleanup() {
        logger.info("Cleaned up Web Platform Adapter");
    }
    
    // Helper methods
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else if (locator.startsWith("className=")) {
            return By.className(locator.substring(10));
        } else if (locator.startsWith("tagName=")) {
            return By.tagName(locator.substring(8));
        } else if (locator.startsWith("linkText=")) {
            return By.linkText(locator.substring(9));
        } else if (locator.startsWith("partialLinkText=")) {
            return By.partialLinkText(locator.substring(16));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else {
            // Default to XPath
            return By.xpath(locator);
        }
    }
    
    private String generateCssSelector(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (String) js.executeScript(
                "function getPath(element) {" +
                "    if (element.id !== '') return '#' + element.id;" +
                "    if (element === document.body) return 'body';" +
                "    var ix = 0;" +
                "    var siblings = element.parentNode.childNodes;" +
                "    for (var i = 0; i < siblings.length; i++) {" +
                "        var sibling = siblings[i];" +
                "        if (sibling === element) return getPath(element.parentNode) + ' > ' + element.tagName.toLowerCase() + ':nth-child(' + (ix + 1) + ')';" +
                "        if (sibling.nodeType === 1 && sibling.tagName === element.tagName) ix++;" +
                "    }" +
                "}" +
                "return getPath(arguments[0]);", element);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String generateXPath(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (String) js.executeScript(
                "function getXPath(element) {" +
                "    if (element.id !== '') return '//*[@id=\"' + element.id + '\"]';" +
                "    if (element === document.body) return '/html/body';" +
                "    var ix = 0;" +
                "    var siblings = element.parentNode.childNodes;" +
                "    for (var i = 0; i < siblings.length; i++) {" +
                "        var sibling = siblings[i];" +
                "        if (sibling === element) return getXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';" +
                "        if (sibling.nodeType === 1 && sibling.tagName === element.tagName) ix++;" +
                "    }" +
                "}" +
                "return getXPath(arguments[0]);", element);
        } catch (Exception e) {
            return null;
        }
    }
}

