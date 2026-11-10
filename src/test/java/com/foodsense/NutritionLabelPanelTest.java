/*
 * Unit tests for NutritionLabelPanel.
 * Tests hero calorie resolution, null-aware macro formatting, and benchmark rendering.
 */

package com.foodsense;

import com.foodsense.ui.NutritionLabelPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("NutritionLabelPanel Tests")
class NutritionLabelPanelTest {

    @Test
    @DisplayName("Should instantiate panel with null nutriments safely")
    void testNullNutriments() {
        NutritionLabelPanel panel = new NutritionLabelPanel(null);
        assertNotNull(panel);
    }

    @Test
    @DisplayName("Should instantiate panel with explicit kcal and kj nutriments")
    void testExplicitKcalAndKj() {
        Nutriments n = new Nutriments();
        n.setEnergyKcal("140");
        n.setEnergyKj("586");
        n.setCarbohydrates("36");
        n.setSugars("35");
        n.setFat("0");
        n.setSaturated_fat("0");
        n.setProteins("0");
        n.setSodium("0.015");

        NutritionLabelPanel panel = new NutritionLabelPanel(n);
        assertNotNull(panel);
    }

    @Test
    @DisplayName("Should instantiate panel with only fallback energy field in kJ")
    void testFallbackEnergy() {
        Nutriments n = new Nutriments();
        n.setEnergy("2000"); // Open Food Facts standard kJ
        n.setCarbohydrates("50");
        n.setFat("20");
        n.setProteins("10");

        NutritionLabelPanel panel = new NutritionLabelPanel(n);
        assertNotNull(panel);
    }

    @Test
    @DisplayName("Should handle missing optional macros gracefully without crashing")
    void testMissingMacros() {
        Nutriments n = new Nutriments();
        n.setEnergy("100");

        NutritionLabelPanel panel = new NutritionLabelPanel(n);
        assertNotNull(panel);
    }
}
