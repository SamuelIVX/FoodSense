/*
 * Unit tests for FoodSenseTheme tokens and mode toggling.
 */

package com.foodsense;

import com.foodsense.ui.FoodSenseTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class FoodSenseThemeTest {

    @AfterEach
    void resetTheme() {
        FoodSenseTheme.setDarkMode(false, null);
    }

    @Test
    void toggleTheme_switchesModeAndPalette() {
        assertFalse(FoodSenseTheme.isDarkMode());
        Color lightBg = FoodSenseTheme.getBackground();

        FoodSenseTheme.toggleTheme(null);
        assertTrue(FoodSenseTheme.isDarkMode());
        Color darkBg = FoodSenseTheme.getBackground();

        assertNotEquals(lightBg, darkBg);

        FoodSenseTheme.toggleTheme(null);
        assertFalse(FoodSenseTheme.isDarkMode());
        assertEquals(lightBg, FoodSenseTheme.getBackground());
    }

    @Test
    void getNutriscoreColor_mapsOfficialGrades() {
        assertEquals(FoodSenseTheme.SCORE_A, FoodSenseTheme.getNutriscoreColor("a"));
        assertEquals(FoodSenseTheme.SCORE_A, FoodSenseTheme.getNutriscoreColor("A"));
        assertEquals(FoodSenseTheme.SCORE_B, FoodSenseTheme.getNutriscoreColor("B"));
        assertEquals(FoodSenseTheme.SCORE_C, FoodSenseTheme.getNutriscoreColor("C"));
        assertEquals(FoodSenseTheme.SCORE_D, FoodSenseTheme.getNutriscoreColor("D"));
        assertEquals(FoodSenseTheme.SCORE_E, FoodSenseTheme.getNutriscoreColor("E"));
        assertEquals(FoodSenseTheme.getTextMuted(), FoodSenseTheme.getNutriscoreColor(null));
        assertEquals(FoodSenseTheme.getTextMuted(), FoodSenseTheme.getNutriscoreColor("Z"));
    }
}
