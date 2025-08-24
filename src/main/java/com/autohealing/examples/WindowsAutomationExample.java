package com.autohealing.examples;

import com.autohealing.AutoHealingFramework;
import com.autohealing.adapters.WindowsPlatformAdapter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Example demonstrating Windows desktop application auto-healing
 * Shows integration with Windows UI automation
 */
public class WindowsAutomationExample {
    
    private static final Logger logger = Logger.getLogger(WindowsAutomationExample.class.getName());
    
    private AutoHealingFramework healingFramework;
    private WindowsPlatformAdapter windowsAdapter;
    
    public void setup() {
        // Initialize Auto-Healing Framework
        healingFramework = new AutoHealingFramework();
        
        // Initialize Windows adapter
        windowsAdapter = new WindowsPlatformAdapter();
        healingFramework.registerPlatformAdapter("WINDOWS", windowsAdapter);
        
        logger.info("Windows automation with auto-healing initialized");
    }
    
    /**
     * Enhanced element finding with auto-healing for Windows applications
     */
    public WindowsPlatformAdapter.WindowsElement findWindowsElementWithHealing(String elementId, String locator) {
        try {
            // Try to find element normally first
            return windowsAdapter.findElement(locator, WindowsPlatformAdapter.WindowsElement.class, null);
            
        } catch (Exception e) {
            logger.warning("Normal element location failed for " + elementId);
            
            // Attempt auto-healing
            WindowsPlatformAdapter.WindowsElement healedElement = healingFramework.heal(
                "WINDOWS", elementId, locator, WindowsPlatformAdapter.WindowsElement.class, null);
            
            if (healedElement != null) {
                logger.info("Successfully healed Windows element: " + elementId);
                return healedElement;
            } else {
                logger.severe("Auto-healing failed for Windows element: " + elementId);
                throw new RuntimeException("Windows element not found and healing failed: " + elementId);
            }
        }
    }
    
    /**
     * Example test for Windows calculator application
     */
    public void testCalculatorWithHealing() {
        try {
            logger.info("Testing Windows Calculator with auto-healing...");
            
            // Find calculator elements with healing
            var numberOne = findWindowsElementWithHealing("btn-1", "automationId=num1Button");
            var plusButton = findWindowsElementWithHealing("btn-plus", "automationId=plusButton");
            var numberTwo = findWindowsElementWithHealing("btn-2", "automationId=num2Button");
            var equalsButton = findWindowsElementWithHealing("btn-equals", "automationId=equalButton");
            var resultDisplay = findWindowsElementWithHealing("result", "automationId=CalculatorResults");
            
            // Perform calculation: 1 + 2 = 3
            windowsAdapter.clickElement(numberOne);
            windowsAdapter.clickElement(plusButton);
            windowsAdapter.clickElement(numberTwo);
            windowsAdapter.clickElement(equalsButton);
            
            // Verify result
            String result = windowsAdapter.getElementText(resultDisplay);
            logger.info("Calculation result: " + result);
            
            if ("3".equals(result.trim())) {
                logger.info("Calculator test passed!");
            } else {
                logger.severe("Calculator test failed. Expected: 3, Got: " + result);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Calculator test failed", e);
            throw e;
        }
    }
    
    /**
     * Example test for a custom Windows application
     */
    public void testCustomApplicationWithHealing() {
        try {
            logger.info("Testing custom Windows application with auto-healing...");
            
            // Find application elements
            var menuFile = findWindowsElementWithHealing("menu-file", "name=File");
            var menuNew = findWindowsElementWithHealing("menu-new", "name=New");
            var textEditor = findWindowsElementWithHealing("text-editor", "controlType=Edit");
            var saveButton = findWindowsElementWithHealing("save-btn", "name=Save");
            
            // Perform actions
            windowsAdapter.clickElement(menuFile);
            windowsAdapter.clickElement(menuNew);
            
            windowsAdapter.typeText(textEditor, "This is a test document created with auto-healing!");
            
            windowsAdapter.clickElement(saveButton);
            
            logger.info("Custom application test completed successfully");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Custom application test failed", e);
            throw e;
        }
    }
    
    /**
     * Example of handling dynamic Windows dialogs
     */
    public void testDynamicDialogHandling() {
        try {
            logger.info("Testing dynamic dialog handling...");
            
            // Sometimes dialogs appear with different automation IDs or names
            // Auto-healing can help find them based on content or position
            
            var dialogOkButton = findWindowsElementWithHealing("dialog-ok", "automationId=Button_OK");
            //var dialogCancelButton = findWindowsElementWithHealing("dialog-cancel", "automationId=Button_Cancel");
            var dialogMessage = findWindowsElementWithHealing("dialog-msg", "controlType=Text");
            
            // Read dialog message
            String message = windowsAdapter.getElementText(dialogMessage);
            logger.info("Dialog message: " + message);
            
            // Click OK to proceed
            windowsAdapter.clickElement(dialogOkButton);
            
            logger.info("Dialog handling test completed");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Dialog handling test failed", e);
        }
    }
    
    public void tearDown() {
        // Generate healing report
        healingFramework.generateReport();
        
        // Cleanup
        windowsAdapter.cleanup();
        
        logger.info("Windows automation test completed");
    }
    
    public static void main(String[] args) {
        WindowsAutomationExample example = new WindowsAutomationExample();
        
        try {
            example.setup();
            example.testCalculatorWithHealing();
            example.testCustomApplicationWithHealing();
            example.testDynamicDialogHandling();
        } finally {
            example.tearDown();
        }
    }
}

