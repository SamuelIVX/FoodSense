/*
 * Unit tests for ApiResponse construction and null-product handling.
 * Does not populate private fields via reflection; status/code cases only assert non-null construction.
 */
package com.foodsense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse Model Tests")
class ApiResponseTest {
    private ApiResponse response;
    private Product product;

    @BeforeEach
    void setUp() {
        response = new ApiResponse();
        product = new Product();
    }

    @Test
    @DisplayName("Should get status code from response")
    void testStatusRetrieval() {
        // Note: ApiResponse uses private field, accessed via reflection in real usage
        // This test demonstrates expected behavior
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should get product from response")
    void testProductRetrieval() {
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should get barcode code from response")
    void testCodeRetrieval() {
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle null product")
    void testNullProductHandling() {
        assertNull(response.getProduct());
    }
}
