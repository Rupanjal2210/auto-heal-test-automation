# MakeMyTrip Login Test with Auto-Healing

This test demonstrates the auto-healing capabilities of the framework by performing a login test on the MakeMyTrip website using the phone number `1234567890`.

## Test Features

### Auto-Healing Capabilities Tested
- **Multiple Locator Strategies**: Tests various CSS selectors, XPath expressions, and element attributes
- **Element Recovery**: Automatically tries alternative locators when primary ones fail
- **Robust Element Finding**: Handles dynamic web elements and page changes
- **Error Recovery**: Gracefully handles website protection mechanisms and popup overlays

### Test Scenarios
1. **Page Load Test**: Verifies the MakeMyTrip login page loads correctly
2. **Element Discovery**: Tests finding login form elements using multiple locator strategies  
3. **Login Flow Test**: Attempts to enter phone number and proceed with login
4. **Broken Locator Healing**: Tests auto-healing with intentionally broken locators

## Prerequisites

### Software Requirements
- **Java 11+**: Required for running the framework
- **Maven 3.6+**: For dependency management and test execution
- **Google Chrome**: Latest version recommended
- **Internet Connection**: Required to access MakeMyTrip website

### Dependencies (Automatically Managed)
- Selenium WebDriver 4.15.0
- WebDriverManager 5.6.2 (automatic ChromeDriver management)
- JUnit 5.10.0
- Auto-Healing Framework components

## Running the Test

### Option 1: Using the Batch Script (Windows)
```cmd
run-web-test.bat
```

### Option 2: Using PowerShell Script  
```powershell
.\run-web-test.ps1
```

### Option 3: Using Maven Directly
```bash
# Compile the project
mvn clean compile

# Run the specific test
mvn test -Dtest=WebAdapterLoginTest
```

### Option 4: Running Individual Test Methods
```bash
# Run only the page load test
mvn test -Dtest=WebAdapterLoginTest#testPageLoad

# Run only the element finding test  
mvn test -Dtest=WebAdapterLoginTest#testFindLoginElements

# Run only the login flow test
mvn test -Dtest=WebAdapterLoginTest#testLoginFlow

# Run only the auto-healing test
mvn test -Dtest=WebAdapterLoginTest#testAutoHealingWithBrokenLocators
```

## 📊 **Healing Reports**

The test framework generates comprehensive healing reports that provide detailed insights into the auto-healing process:

### **Report Types Generated:**

#### **1. HTML Healing Reports**
- **Location**: `test-reports/healing-reports/WebAdapter_*_Report_*.html`
- **Content**: 
  - Test execution summary with pass/fail status
  - Auto-healing metrics (attempts, success rate, timing)
  - Screenshots of test execution
  - Test environment details
  - Platform and strategy effectiveness analysis

#### **2. JSON Healing Reports**  
- **Location**: `test-reports/healing-reports/WebAdapter_HealingReport_*.json`
- **Content**: Machine-readable healing data for programmatic analysis
- **Use Case**: Integration with CI/CD pipelines and automated analysis

#### **3. Screenshots**
- **Location**: `test-reports/screenshots/WebAdapter_*_*.png`
- **Content**: Screenshots captured during test execution
- **Purpose**: Visual validation and debugging

### **Viewing Reports:**

#### **Automatic Report Opening:**
```bash
# These scripts automatically open the latest generated report
run-web-test-with-reports.bat
.\run-web-test-with-reports.ps1
```

#### **Manual Report Access:**
```bash
# Navigate to reports directory
cd test-reports/healing-reports/

# Open the latest HTML report in browser
start WebAdapter_*_Report_*.html
```

### **Report Metrics:**

The healing reports include the following metrics:

#### **Healing Effectiveness:**
- **Total Healing Attempts**: Number of times auto-healing was triggered
- **Successful Heals**: Number of successful element recoveries  
- **Failed Heals**: Number of healing attempts that couldn't recover elements
- **Success Rate**: Percentage of successful healing attempts
- **Average Healing Time**: Time taken for successful healing operations

#### **Strategy Performance:**
- **DirectFind Success**: Elements found without healing
- **AutoHealing Success**: Elements recovered through healing strategies
- **Platform Effectiveness**: Success rates by platform (WEB, Windows, etc.)
- **Locator Strategy Effectiveness**: Which locator types work best

#### **Element Analysis:**
- **Most Problematic Elements**: Elements with highest failure rates
- **Element Healing History**: Timeline of healing attempts per element
- **Locator Evolution**: How locators changed through healing

## Test Configuration

### Chrome Browser Options
The test is configured with these Chrome options for better compatibility:
- Disabled automation detection
- Custom user agent
- Disabled web security (for testing purposes)
- Window maximized
- 10-second implicit wait

### Auto-Healing Settings
- **Max Healing Attempts**: 3
- **Healing Enabled**: true
- **Platform**: WEB
- **Timeout**: 15 seconds

### Element Locator Strategies
The test uses multiple locator strategies in order of preference:
1. `data-cy` attributes (Cypress testing attributes)
2. Placeholder text matching
3. Name attributes  
4. ID attributes containing keywords
5. Input type attributes
6. CSS class selectors

## Expected Behavior

### Successful Test Run
- Page loads successfully
- Login elements are found using auto-healing
- Phone number is entered in the input field
- Continue button is located and clicked
- Test completes without critical failures

### Common Scenarios
- **Website Protection**: MakeMyTriip may have bot protection; this is expected
- **Element Not Found**: Auto-healing will try alternative locators
- **Popup Overlays**: Test includes logic to handle common popup/modal closures
- **Network Issues**: Test will timeout gracefully after 15 seconds

## Test Output

### Success Indicators
- ✅ Page loads with correct title
- ✅ Login elements found using primary or healed locators  
- ✅ Phone number successfully entered
- ✅ Continue button located and clickable
- ✅ No critical exceptions during healing attempts

### Log Information
The test provides detailed logging:
- Element discovery attempts
- Auto-healing strategy results
- Locator success/failure information
- Page interaction details

## Troubleshooting

### Common Issues

#### ChromeDriver Issues
```bash
# WebDriverManager should handle this automatically, but if needed:
# Download ChromeDriver manually and add to PATH
```

#### Website Changes
```bash
# If MakeMyTrip changes their page structure:
# 1. Update locator arrays in the test
# 2. Add new selector strategies
# 3. Adjust wait times if needed
```

#### Network/Firewall Issues
```bash
# If corporate firewall blocks access:
# 1. Use VPN or different network
# 2. Update Chrome proxy settings
# 3. Add necessary firewall exceptions
```

#### Headless Mode
To run in headless mode (no browser window), uncomment this line in the test:
```java
options.addArguments("--headless");
```

## Understanding Auto-Healing

### How It Works
1. **Primary Locator**: Test tries the main locator strategy
2. **Healing Trigger**: If element not found, auto-healing activates
3. **Alternative Strategies**: Framework tries backup locators
4. **Success Recording**: Successful locators are cached for future use
5. **Failure Handling**: If all strategies fail, test continues gracefully

### Healing Strategies Used
- **DOM Analysis**: Analyzes page structure changes
- **Attribute Matching**: Finds elements with similar attributes
- **CSS Selector Variations**: Tries different CSS approaches
- **XPath Alternatives**: Uses XPath when CSS selectors fail

## Integration with CI/CD

To integrate with automated pipelines:

```yaml
# Example GitHub Actions step
- name: Run Web Auto-Healing Tests
  run: |
    mvn clean test -Dtest=WebAdapterLoginTest
    # Optional: Add headless mode for CI
    # mvn clean test -Dtest=WebAdapterLoginTest -Dheadless=true
```

## Notes

- This test uses a dummy phone number (1234567890) for demonstration
- The test validates the auto-healing mechanism rather than actual login success
- Website protection mechanisms are expected and handled gracefully
- All test data and interactions are read-only and safe
