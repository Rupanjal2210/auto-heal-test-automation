# Integration Guide: Adding Auto-Healing to Existing Test Scripts

## Overview
This guide shows how to integrate the Multi-Platform Auto-Healing Framework with your existing test automation scripts with minimal code changes.

## Integration Strategies

### 1. Wrapper Method Approach (Recommended)
Replace your existing element finding logic with healing-enabled wrappers.

### 2. Factory Pattern
Create a factory that returns healing-enabled elements.

### 3. Page Object Enhancement
Enhance existing Page Object Models with auto-healing capabilities.

### 4. TestNG/JUnit Integration
Add healing as a test retry mechanism.

---

## Web Tests (Selenium) Integration

### Before (Original Test)
```java
public class LoginTest {
    WebDriver driver;
    
    @Test
    public void testLogin() {
        driver.get("https://example.com/login");
        
        // Original brittle locators
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));
        WebElement loginButton = driver.findElement(By.xpath("//button[@class='login-btn']"));
        
        usernameField.sendKeys("testuser");
        passwordField.sendKeys("password123");
        loginButton.click();
    }
}
```

### After (With Auto-Healing)
```java
public class LoginTest {
    WebDriver driver;
    AutoHealingFramework healingFramework;
    
    @BeforeMethod
    public void setup() {
        driver = new ChromeDriver();
        healingFramework = new AutoHealingFramework();
        healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
    }
    
    @Test
    public void testLogin() {
        driver.get("https://example.com/login");
        
        // Healing-enabled element finding
        WebElement usernameField = findElementWithHealing("username", "id=username");
        WebElement passwordField = findElementWithHealing("password", "id=password");
        WebElement loginButton = findElementWithHealing("loginBtn", "xpath=//button[@class='login-btn']");
        
        usernameField.sendKeys("testuser");
        passwordField.sendKeys("password123");
        loginButton.click();
    }
    
    // Wrapper method for healing
    private WebElement findElementWithHealing(String elementId, String locator) {
        try {
            // Try original locator first
            return parseAndFindElement(locator);
        } catch (Exception e) {
            // Attempt auto-healing
            WebElement healedElement = healingFramework.heal("WEB", elementId, locator, WebElement.class, driver);
            if (healedElement != null) {
                System.out.println("Successfully healed element: " + elementId);
                return healedElement;
            }
            throw new RuntimeException("Element not found and healing failed: " + elementId);
        }
    }
    
    private WebElement parseAndFindElement(String locator) {
        if (locator.startsWith("id=")) {
            return driver.findElement(By.id(locator.substring(3)));
        } else if (locator.startsWith("xpath=")) {
            return driver.findElement(By.xpath(locator.substring(6)));
        } else if (locator.startsWith("css=")) {
            return driver.findElement(By.cssSelector(locator.substring(4)));
        }
        throw new IllegalArgumentException("Unsupported locator: " + locator);
    }
}
```

---

## Page Object Model Integration

### Enhanced Page Object with Auto-Healing
```java
public class LoginPage {
    private WebDriver driver;
    private AutoHealingFramework healingFramework;
    
    public LoginPage(WebDriver driver, AutoHealingFramework healingFramework) {
        this.driver = driver;
        this.healingFramework = healingFramework;
    }
    
    // Original locators as fallback
    private static final String USERNAME_LOCATOR = "id=username";
    private static final String PASSWORD_LOCATOR = "id=password";
    private static final String LOGIN_BUTTON_LOCATOR = "xpath=//button[@class='login-btn']";
    
    public void enterUsername(String username) {
        WebElement field = findElementWithHealing("usernameField", USERNAME_LOCATOR);
        field.clear();
        field.sendKeys(username);
    }
    
    public void enterPassword(String password) {
        WebElement field = findElementWithHealing("passwordField", PASSWORD_LOCATOR);
        field.clear();
        field.sendKeys(password);
    }
    
    public void clickLogin() {
        WebElement button = findElementWithHealing("loginButton", LOGIN_BUTTON_LOCATOR);
        button.click();
    }
    
    private WebElement findElementWithHealing(String elementId, String locator) {
        return healingFramework.heal("WEB", elementId, locator, WebElement.class, driver);
    }
}
```

---

## Windows Desktop Application Integration

### Before (Original Test)
```java
public class CalculatorTest {
    @Test
    public void testCalculation() {
        // Original brittle approach
        WindowsDriver<WindowsElement> driver = new WindowsDriver<>(serverUrl, capabilities);
        
        WindowsElement button1 = driver.findElementByAccessibilityId("num1Button");
        WindowsElement buttonPlus = driver.findElementByName("Plus");
        WindowsElement button2 = driver.findElementByAccessibilityId("num2Button");
        WindowsElement buttonEquals = driver.findElementByName("Equals");
        
        button1.click();
        buttonPlus.click();
        button2.click();
        buttonEquals.click();
    }
}
```

### After (With Auto-Healing)
```java
public class CalculatorTest {
    private AutoHealingFramework healingFramework;
    
    @BeforeMethod
    public void setup() {
        healingFramework = new AutoHealingFramework();
        healingFramework.registerPlatformAdapter("WINDOWS", new WindowsPlatformAdapter());
    }
    
    @Test
    public void testCalculation() {
        WindowsPlatformAdapter.WindowsElement button1 = findWindowsElement("num1", "automationId=num1Button");
        WindowsPlatformAdapter.WindowsElement buttonPlus = findWindowsElement("plus", "name=Plus");
        WindowsPlatformAdapter.WindowsElement button2 = findWindowsElement("num2", "automationId=num2Button");
        WindowsPlatformAdapter.WindowsElement buttonEquals = findWindowsElement("equals", "name=Equals");
        
        button1.click();
        buttonPlus.click();
        button2.click();
        buttonEquals.click();
    }
    
    private WindowsPlatformAdapter.WindowsElement findWindowsElement(String elementId, String locator) {
        return healingFramework.heal("WINDOWS", elementId, locator, WindowsPlatformAdapter.WindowsElement.class, null);
    }
}
```

---

## TestNG Integration with Retry Mechanism

```java
public class AutoHealingTestListener implements IRetryAnalyzer {
    private AutoHealingFramework healingFramework;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;
    
    public AutoHealingTestListener() {
        this.healingFramework = new AutoHealingFramework();
    }
    
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            
            // Enable aggressive healing for retries
            healingFramework.getConfiguration().setMaxHealingAttempts(5);
            healingFramework.getConfiguration().setHealingEnabled(true);
            
            System.out.println("Retrying test with enhanced auto-healing: " + result.getName());
            return true;
        }
        return false;
    }
}

// Usage in test class
@Listeners(AutoHealingTestListener.class)
public class MyTestClass {
    // Your tests here
}
```

---

## Configuration-Based Integration

### Create healing.yml configuration
```yaml
autohealing:
  enabled: true
  maxAttempts: 3
  strategies:
    - DOM_ANALYSIS
    - ATTRIBUTE_MATCHING
    - IMAGE_RECOGNITION
  platforms:
    web:
      enabled: true
      timeoutMs: 5000
    windows:
      enabled: true
      timeoutMs: 3000
```

### Base Test Class
```java
public abstract class BaseHealingTest {
    protected AutoHealingFramework healingFramework;
    protected WebDriver driver;
    
    @BeforeMethod
    public void setupHealing() {
        healingFramework = new AutoHealingFramework();
        
        // Load configuration
        healingFramework.getConfiguration().loadFromFile("healing.yml");
        
        // Setup driver and register adapter
        if (driver != null) {
            healingFramework.registerPlatformAdapter("WEB", new WebPlatformAdapter(driver));
        }
    }
    
    @AfterMethod
    public void teardownHealing() {
        // Generate healing report (auto-generates filename)
        healingFramework.generateReport();
        
        // Or specify custom path
        // healingFramework.generateReport("target/reports/healing-session");
        
        // Log session statistics
        HealingReporter reporter = healingFramework.getReporter();
        System.out.println("Healing Success Rate: " + reporter.getSuccessRate() + "%");
        System.out.println("Total Attempts: " + reporter.getTotalAttempts());
    }
    
    // Utility method for all test classes
    protected WebElement findElement(String elementId, String locator) {
        return healingFramework.heal("WEB", elementId, locator, WebElement.class, driver);
    }
}
```

### Your Test Class
```java
public class ProductSearchTest extends BaseHealingTest {
    
    @BeforeMethod
    public void setup() {
        driver = new ChromeDriver();
        super.setupHealing(); // Initialize healing framework
    }
    
    @Test
    public void testProductSearch() {
        driver.get("https://shop.example.com");
        
        // Use healing-enabled element finding
        WebElement searchBox = findElement("searchBox", "id=search");
        WebElement searchButton = findElement("searchBtn", "css=.search-button");
        
        searchBox.sendKeys("laptop");
        searchButton.click();
        
        WebElement results = findElement("results", "xpath=//div[@class='results']");
        Assert.assertTrue(results.isDisplayed());
    }
}
```

---

## Gradual Migration Strategy

### Phase 1: Add Framework (No Changes to Tests)
1. Add healing framework dependency
2. Initialize framework in @BeforeMethod
3. Tests run normally, healing is available but not used

### Phase 2: Identify Brittle Elements
1. Run existing tests
2. Note which elements fail frequently
3. Replace those specific findElement calls with healing-enabled versions

### Phase 3: Full Integration
1. Create base test class with healing utilities
2. Migrate all element finding to use healing
3. Configure healing strategies per application

### Phase 4: Optimization
1. Analyze healing reports
2. Tune healing strategies
3. Add custom healing rules for specific applications

---

## Best Practices

### 1. Element Naming Convention
```java
// Use descriptive, stable element IDs
findElement("loginUsernameField", locator);  // Good
findElement("field1", locator);               // Bad
```

### 2. Locator Fallback Strategy
```java
private WebElement findElementWithFallback(String elementId, String... locators) {
    for (String locator : locators) {
        try {
            return findElement(elementId, locator);
        } catch (Exception e) {
            // Try next locator
        }
    }
    throw new NoSuchElementException("All locators failed for: " + elementId);
}

// Usage
WebElement button = findElementWithFallback("submitButton", 
    "id=submit", 
    "css=.submit-btn", 
    "xpath=//button[contains(text(),'Submit')]");
```

### 3. Conditional Healing
```java
private WebElement findElement(String elementId, String locator, boolean enableHealing) {
    if (enableHealing) {
        return healingFramework.heal("WEB", elementId, locator, WebElement.class, driver);
    } else {
        return driver.findElement(parseLocator(locator));
    }
}
```

### 4. Healing Reports Integration
```java
@AfterSuite
public void generateHealingReport() {
    // Generate comprehensive healing report
    healingFramework.generateReport();
    
    // Or specify custom path
    // healingFramework.generateReport("target/reports/healing-session");
    
    // Access detailed reporter for custom logic
    HealingReporter reporter = healingFramework.getReporter();
    
    // Log summary
    System.out.println("Healing Success Rate: " + reporter.getSuccessRate() + "%");
    System.out.println("Total Healing Attempts: " + reporter.getTotalAttempts());
    System.out.println("Successful Heals: " + reporter.getSuccessfulHeals());
}
```

This integration approach allows you to:
- **Gradually migrate** existing tests without breaking them
- **Maintain existing test structure** while adding healing capabilities
- **Get detailed reports** on healing effectiveness
- **Tune healing strategies** based on your specific application needs
