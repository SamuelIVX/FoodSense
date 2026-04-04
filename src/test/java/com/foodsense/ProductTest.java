package com.foodsense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Model Tests")
class ProductTest {
    private Product product;
    private Nutriments nutriments;

    @BeforeEach
    void setUp() {
        product = new Product();
        nutriments = new Nutriments();
    }

    @Test
    @DisplayName("Should set and get product name")
    void testProductNameStorage() {
        product.setProduct_name("Apple");
        assertEquals("Apple", product.getProduct_name());
    }

    @Test
    @DisplayName("Should set and get brands")
    void testBrandsStorage() {
        product.setBrands("Organic Grove");
        assertEquals("Organic Grove", product.getBrands());
    }

    @Test
    @DisplayName("Should set and get nutriscore grade")
    void testNutriscoreGradeStorage() {
        product.setNutriscore_grade("A");
        assertEquals("A", product.getNutriscore_grade());
    }

    @Test
    @DisplayName("Should validate all nutriscore grades")
    void testAllNutriscoreGrades() {
        String[] grades = { "A", "B", "C", "D", "E" };
        for (String grade : grades) {
            product.setNutriscore_grade(grade);
            assertEquals(grade, product.getNutriscore_grade());
        }
    }

    @Test
    @DisplayName("Should set and get ingredients text")
    void testIngredientsTextStorage() {
        String ingredients = "Water, sugar, citric acid";
        product.setIngredients_text(ingredients);
        assertEquals(ingredients, product.getIngredients_text());
    }

    @Test
    @DisplayName("Should set and get allergens")
    void testAllergensStorage() {
        product.setAllergens_from_ingredients("Contains nuts, milk");
        assertEquals("Contains nuts, milk", product.getAllergens_from_ingredients());
    }

    @Test
    @DisplayName("Should set and get nutriments")
    void testNutrimentsStorage() {
        nutriments.setEnergy("100");
        product.setNutriments(nutriments);
        assertNotNull(product.getNutriments());
        assertEquals("100", product.getNutriments().getEnergy());
    }

    @Test
    @DisplayName("Should set and get front image URL")
    void testFrontImageUrlStorage() {
        String url = "https://example.com/image.jpg";
        product.setImage_front_url(url);
        assertEquals(url, product.getImage_front_url());
    }

    @Test
    @DisplayName("Should set and get ingredients image URL")
    void testIngredientsImageUrlStorage() {
        String url = "https://example.com/ingredients.jpg";
        product.setImage_ingredients_url(url);
        assertEquals(url, product.getImage_ingredients_url());
    }

    @Test
    @DisplayName("Should set and get nutrition image URL")
    void testNutritionImageUrlStorage() {
        String url = "https://example.com/nutrition.jpg";
        product.setImage_nutrition_url(url);
        assertEquals(url, product.getImage_nutrition_url());
    }

    @Test
    @DisplayName("Should handle null values for images")
    void testNullImageUrls() {
        product.setImage_front_url(null);
        product.setImage_ingredients_url(null);
        product.setImage_nutrition_url(null);

        assertNull(product.getImage_front_url());
        assertNull(product.getImage_ingredients_url());
        assertNull(product.getImage_nutrition_url());
    }

    @Test
    @DisplayName("Should handle complete product data")
    void testCompleteProductData() {
        product.setProduct_name("Organic Apple Juice");
        product.setBrands("FreshJuice Inc");
        product.setNutriscore_grade("B");
        product.setIngredients_text("100% apple juice");
        product.setAllergens_from_ingredients("None");
        nutriments.setEnergy("50");
        nutriments.setSugars("11");
        product.setNutriments(nutriments);
        product.setImage_front_url("https://example.com/apple.jpg");

        assertEquals("Organic Apple Juice", product.getProduct_name());
        assertEquals("FreshJuice Inc", product.getBrands());
        assertEquals("B", product.getNutriscore_grade());
        assertNotNull(product.getNutriments());
    }
}
