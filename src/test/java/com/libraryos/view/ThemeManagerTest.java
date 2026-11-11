package com.libraryos.view;

import com.libraryos.view.theme.ThemeManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeManagerTest {

    @Test
    void testThemeValuesAndDisplayNames() {
        assertNotNull(ThemeManager.Theme.LIGHT.getDisplayName());
        assertNotNull(ThemeManager.Theme.DARK.getDisplayName());

        assertEquals("/css/theme.css", ThemeManager.Theme.LIGHT.getBaseCss());
        assertEquals("/css/theme-dark.css", ThemeManager.Theme.DARK.getOverrideCss());
    }

    @Test
    void testSwitchTheme() {
        ThemeManager manager = ThemeManager.getInstance();
        manager.setTheme(ThemeManager.Theme.DARK);
        assertEquals(ThemeManager.Theme.DARK, manager.getCurrentTheme());
        assertTrue(manager.isDarkMode());

        manager.setTheme(ThemeManager.Theme.LIGHT);
        assertEquals(ThemeManager.Theme.LIGHT, manager.getCurrentTheme());
        assertFalse(manager.isDarkMode());
    }
}
