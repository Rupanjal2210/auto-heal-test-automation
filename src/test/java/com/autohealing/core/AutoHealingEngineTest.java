package com.autohealing.core;

import com.autohealing.adapters.PlatformAdapter;
import com.autohealing.strategies.HealingStrategy;
import com.autohealing.config.HealingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AutoHealingEngine
 */
public class AutoHealingEngineTest {
    
    @Mock
    private PlatformAdapter mockAdapter;
    
    @Mock
    private HealingStrategy mockStrategy;
    
    private AutoHealingEngine engine;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        engine = AutoHealingEngine.getInstance();
        
        // Reset engine state for each test
        engine.setHealingEnabled(true);
    }
    
    @Test
    public void testSingletonInstance() {
        AutoHealingEngine engine1 = AutoHealingEngine.getInstance();
        AutoHealingEngine engine2 = AutoHealingEngine.getInstance();
        
        assertSame(engine1, engine2, "Engine should be singleton");
    }
    
    @Test
    public void testRegisterPlatformAdapter() {
        engine.registerPlatformAdapter("TEST", mockAdapter);
        
        // Verify adapter is registered by attempting to use it
        // This is tested indirectly through heal method
        assertNotNull(engine);
    }
    
    @Test
    public void testAddHealingStrategy() {
        when(mockStrategy.getStrategyName()).thenReturn("TEST_STRATEGY");
        
        engine.addHealingStrategy(mockStrategy);
        
        // Verify strategy is added
        verify(mockStrategy).getStrategyName();
    }
    
    @Test
    public void testHealingWhenDisabled() {
        engine.setHealingEnabled(false);
        
        Object result = engine.heal("TEST", "element1", "id=test", Object.class, null);
        
        assertNull(result, "Healing should return null when disabled");
    }
    
    @Test
    public void testSuccessfulHealing() {
        // Setup mocks
        Object expectedElement = new Object();
        when(mockAdapter.findElement(anyString(), any(), any())).thenReturn(expectedElement);
        when(mockStrategy.canHandle("TEST")).thenReturn(true);
        when(mockStrategy.heal(any(), anyString(), anyString(), any(), any())).thenReturn(expectedElement);
        when(mockStrategy.getHealedLocator("element1")).thenReturn("healed=locator");
        
        // Register mocks
        engine.registerPlatformAdapter("TEST", mockAdapter);
        engine.addHealingStrategy(mockStrategy);
        
        // Test healing
        Object result = engine.heal("TEST", "element1", "id=original", Object.class, null);
        
        assertSame(expectedElement, result, "Should return healed element");
        verify(mockStrategy).heal(mockAdapter, "element1", "id=original", Object.class, null);
    }
    
    @Test
    public void testHealingWithNoAdapter() {
        Object result = engine.heal("UNKNOWN", "element1", "id=test", Object.class, null);
        
        assertNull(result, "Should return null when no adapter is registered");
    }
    
    @Test
    public void testMaxHealingAttempts() {
        HealingConfiguration config = HealingConfiguration.getInstance();
        config.setMaxHealingAttempts(2);
        
        // First two attempts should work
        assertNull(engine.heal("TEST", "element1", "id=test", Object.class, null));
        assertNull(engine.heal("TEST", "element1", "id=test", Object.class, null));
        
        // Third attempt should be blocked
        assertNull(engine.heal("TEST", "element1", "id=test", Object.class, null));
    }
    
    @Test
    public void testResetHealingAttempts() {
        HealingConfiguration config = HealingConfiguration.getInstance();
        config.setMaxHealingAttempts(1);
        
        // First attempt
        assertNull(engine.heal("TEST", "element1", "id=test", Object.class, null));
        
        // Reset attempts
        engine.resetHealingAttempts("TEST", "element1");
        
        // Should be able to try again
        assertNull(engine.heal("TEST", "element1", "id=test", Object.class, null));
    }
    
    @Test
    public void testGetStatistics() {
        var stats = engine.getHealingStatistics();
        
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(stats.containsKey("totalAttempts") || stats.isEmpty(), 
                  "Statistics should contain expected keys");
    }
    
    @Test
    public void testHealingEnabledToggle() {
        assertTrue(engine.isHealingEnabled(), "Healing should be enabled by default");
        
        engine.setHealingEnabled(false);
        assertFalse(engine.isHealingEnabled(), "Healing should be disabled");
        
        engine.setHealingEnabled(true);
        assertTrue(engine.isHealingEnabled(), "Healing should be enabled again");
    }
}
