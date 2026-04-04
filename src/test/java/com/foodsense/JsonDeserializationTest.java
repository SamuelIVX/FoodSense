package com.foodsense;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JSON Deserialization Tests")
class JsonDeserializationTest {
    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new Gson();
    }

    @Test
    @DisplayName("Should deserialize Nutriments with @SerializedName mapping")
    void testNutrimentsDeserialization() {
        String json = "{" +
                "\"energy\": \"2000\"," +
                "\"sugars\": \"50\"," +
                "\"fat\": \"65\"," +
                "\"saturated-fat\": \"20\"," +
                "\"carbohydrates\": \"300\"," +
                "\"proteins\": \"50\"," +
                "\"salt\": \"2\"," +
                "\"sodium\": \"800\"" +
                "}";

        Nutriments nutriments = gson.fromJson(json, Nutriments.class);
        assertNotNull(nutriments);
        assertEquals("2000", nutriments.getEnergy());
        assertEquals("50", nutriments.getSugars());
        assertEquals("65", nutriments.getFat());
        assertEquals("20", nutriments.getSaturated_fat());
    }

    @Test
    @DisplayName("Should deserialize Product with nested Nutriments")
    void testProductDeserializationWithNutriments() {
        String json = "{" +
                "\"product_name\": \"Apple Juice\"," +
                "\"brands\": \"FreshJuice\"," +
                "\"nutriscore_grade\": \"B\"," +
                "\"ingredients_text\": \"100% juice\"," +
                "\"nutriments\": {" +
                "\"energy\": \"50\"," +
                "\"sugars\": \"11\"" +
                "}," +
                "\"image_front_url\": \"https://example.com/apple.jpg\"" +
                "}";

        Product product = gson.fromJson(json, Product.class);
        assertNotNull(product);
        assertEquals("Apple Juice", product.getProduct_name());
        assertEquals("FreshJuice", product.getBrands());
        assertEquals("B", product.getNutriscore_grade());
        assertNotNull(product.getNutriments());
        assertEquals("50", product.getNutriments().getEnergy());
    }

    @Test
    @DisplayName("Should handle missing optional fields in Nutriments")
    void testNutrimentsWithMissingFields() {
        String json = "{\"energy\": \"100\"}";
        Nutriments nutriments = gson.fromJson(json, Nutriments.class);

        assertNotNull(nutriments);
        assertEquals("100", nutriments.getEnergy());
        assertNull(nutriments.getSugars());
        assertNull(nutriments.getFat());
    }

    @Test
    @DisplayName("Should deserialize minimal Product data")
    void testMinimalProductDeserialization() {
        String json = "{\"product_name\": \"Product\"}";
        Product product = gson.fromJson(json, Product.class);

        assertNotNull(product);
        assertEquals("Product", product.getProduct_name());
    }

    @Test
    @DisplayName("Should handle empty JSON objects")
    void testEmptyProductDeserialization() {
        String json = "{}";
        Product product = gson.fromJson(json, Product.class);

        assertNotNull(product);
        assertNull(product.getProduct_name());
    }
}
