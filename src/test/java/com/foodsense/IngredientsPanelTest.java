/*
 * Unit tests for IngredientsPanel and ingredient parsing contracts.
 */

package com.foodsense;

import com.foodsense.ui.IngredientsPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IngredientsPanel and Parsing Tests")
class IngredientsPanelTest {

    @Test
    @DisplayName("Should return empty list for null or blank input")
    void testEmptyAndNull() {
        assertTrue(IngredientsPanel.parseIngredients(null).isEmpty());
        assertTrue(IngredientsPanel.parseIngredients("").isEmpty());
        assertTrue(IngredientsPanel.parseIngredients("   \n\t ").isEmpty());
    }

    @Test
    @DisplayName("Should parse comma-separated ingredients into discrete tokens")
    void testBasicCommaSeparation() {
        String raw = "Carbonated Water, High Fructose Corn Syrup, Caramel Color, Phosphoric Acid, Natural Flavors, Caffeine.";
        List<String> items = IngredientsPanel.parseIngredients(raw);

        assertEquals(6, items.size());
        assertEquals("Carbonated Water", items.get(0));
        assertEquals("High Fructose Corn Syrup", items.get(1));
        assertEquals("Caramel Color", items.get(2));
        assertEquals("Phosphoric Acid", items.get(3));
        assertEquals("Natural Flavors", items.get(4));
        assertEquals("Caffeine", items.get(5));
    }

    @Test
    @DisplayName("Should preserve sub-ingredients inside parentheses and brackets")
    void testParenthesesAndBracketsPreservation() {
        String raw = "Enriched Flour (Wheat Flour, Niacin, Reduced Iron, Thiamin Mononitrate), Vegetable Oil [Palm, Canola], Salt.";
        List<String> items = IngredientsPanel.parseIngredients(raw);

        assertEquals(3, items.size());
        assertEquals("Enriched Flour (Wheat Flour, Niacin, Reduced Iron, Thiamin Mononitrate)", items.get(0));
        assertEquals("Vegetable Oil [Palm, Canola]", items.get(1));
        assertEquals("Salt", items.get(2));
    }

    @Test
    @DisplayName("Should split on semicolons and bullets as well")
    void testOtherDelimiters() {
        String raw = "Sugar; Cocoa Butter • Milk Powder; Emulsifier (Soy Lecithin)";
        List<String> items = IngredientsPanel.parseIngredients(raw);

        assertEquals(4, items.size());
        assertEquals("Sugar", items.get(0));
        assertEquals("Cocoa Butter", items.get(1));
        assertEquals("Milk Powder", items.get(2));
        assertEquals("Emulsifier (Soy Lecithin)", items.get(3));
    }

    @Test
    @DisplayName("Should instantiate IngredientsPanel safely with null and populated inputs")
    void testPanelInstantiation() {
        IngredientsPanel emptyPanel = new IngredientsPanel(null, null);
        assertNotNull(emptyPanel);

        IngredientsPanel fullPanel = new IngredientsPanel("Water, Sugar, Salt", "milk, sugar");
        assertNotNull(fullPanel);
    }
}
