package com.foodsense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Nutriments Model Tests")
class NutrimentsTest {
    private Nutriments nutriments;

    @BeforeEach
    void setUp() {
        nutriments = new Nutriments();
    }

    @Test
    @DisplayName("Should set and get energy (calories)")
    void testEnergyStorage() {
        nutriments.setEnergy("2000");
        assertEquals("2000", nutriments.getEnergy());
    }

    @Test
    @DisplayName("Should set and get sugars")
    void testSugarsStorage() {
        nutriments.setSugars("50");
        assertEquals("50", nutriments.getSugars());
    }

    @Test
    @DisplayName("Should set and get fat")
    void testFatStorage() {
        nutriments.setFat("65");
        assertEquals("65", nutriments.getFat());
    }

    @Test
    @DisplayName("Should set and get saturated fat")
    void testSaturatedFatStorage() {
        nutriments.setSaturated_fat("20");
        assertEquals("20", nutriments.getSaturated_fat());
    }

    @Test
    @DisplayName("Should set and get carbohydrates")
    void testCarbohydratesStorage() {
        nutriments.setCarbohydrates("300");
        assertEquals("300", nutriments.getCarbohydrates());
    }

    @Test
    @DisplayName("Should set and get proteins")
    void testProteinsStorage() {
        nutriments.setProteins("50");
        assertEquals("50", nutriments.getProteins());
    }

    @Test
    @DisplayName("Should set and get salt")
    void testSaltStorage() {
        nutriments.setSalt("2");
        assertEquals("2", nutriments.getSalt());
    }

    @Test
    @DisplayName("Should set and get sodium")
    void testSodiumStorage() {
        nutriments.setSodium("800");
        assertEquals("800", nutriments.getSodium());
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testNullValues() {
        nutriments.setEnergy(null);
        assertNull(nutriments.getEnergy());
    }

    @Test
    @DisplayName("Should format toString correctly")
    void testToStringFormat() {
        nutriments.setCarbohydrates("300");
        nutriments.setEnergy("2000");
        nutriments.setFat("65");
        nutriments.setProteins("50");
        nutriments.setSalt("2");
        nutriments.setSaturated_fat("20");
        nutriments.setSodium("800");
        nutriments.setSugars("50");

        String result = nutriments.toString();
        assertTrue(result.contains("Carbohydrates: 300"));
        assertTrue(result.contains("Energy: 2000"));
        assertTrue(result.contains("Fat: 65"));
        assertTrue(result.contains("Proteins: 50"));
    }
}
