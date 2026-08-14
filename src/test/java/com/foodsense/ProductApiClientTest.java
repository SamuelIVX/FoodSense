package com.foodsense;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ProductApiClientTest {

    private String originalSysProp;

    @BeforeEach
    void setUp() {
        originalSysProp = System.getProperty("foodsense.host");
        System.clearProperty("foodsense.host");
    }

    @AfterEach
    void tearDown() {
        if (originalSysProp != null) {
            System.setProperty("foodsense.host", originalSysProp);
        } else {
            System.clearProperty("foodsense.host");
        }
    }

    @Test
    void testResolveDefaultHostWithNoOverrides() {
        assertEquals(ProductApiClient.DEFAULT_HOST, ProductApiClient.resolveHost(null, null));
    }

    @Test
    void testResolveHostPrefersSystemProperty() {
        assertEquals("staging.openfoodfacts.org", ProductApiClient.resolveHost("staging.openfoodfacts.org", null));
    }

    @Test
    void testResolveHostFallsBackToEnvVar() {
        assertEquals("staging.openfoodfacts.org", ProductApiClient.resolveHost(null, "staging.openfoodfacts.org"));
    }

    @Test
    void testResolveHostSystemPropertyTakesPrecedenceOverEnvVar() {
        assertEquals("staging.openfoodfacts.org", ProductApiClient.resolveHost("staging.openfoodfacts.org", "env.openfoodfacts.org"));
    }

    @Test
    void testDefaultHostResolution() {
        ProductApiClient client = new ProductApiClient();
        assertEquals(ProductApiClient.DEFAULT_HOST, client.getHost());
        assertEquals("world.openfoodfacts.org", client.getHost());
    }

    @Test
    void testSystemPropertyHostOverride() {
        System.setProperty("foodsense.host", "staging.openfoodfacts.org");
        ProductApiClient client = new ProductApiClient();
        assertEquals("staging.openfoodfacts.org", client.getHost());
    }

    @Test
    void testExplicitHostConstructor() {
        ProductApiClient client = new ProductApiClient("custom.openfoodfacts.net", req -> null);
        assertEquals("custom.openfoodfacts.net", client.getHost());
    }

    @Test
    void testBuildUri() {
        ProductApiClient client = new ProductApiClient("world.openfoodfacts.org", req -> null);
        URI uri = client.buildUri("0049000006346");
        assertEquals("https://world.openfoodfacts.org/api/v2/product/0049000006346", uri.toString());
    }

    @Test
    void testFetchProductSuccess() {
        String jsonPayload = """
                {
                  "status": 1,
                  "product": {
                    "product_name": "Coca-Cola",
                    "brands": "Coca-Cola",
                    "nutriscore_grade": "e"
                  }
                }
                """;

        ProductApiClient client = new ProductApiClient("world.openfoodfacts.org", req ->
                new SimpleHttpResponse(200, jsonPayload));

        Product product = client.fetchProduct("0049000006346");

        assertNotNull(product);
        assertEquals("Coca-Cola", product.getProduct_name());
        assertEquals("Coca-Cola", product.getBrands());
        assertEquals("e", product.getNutriscore_grade());
    }

    @Test
    void testFetchProductNotFoundStatusZero() {
        String jsonPayload = """
                {
                  "status": 0,
                  "product": null
                }
                """;

        ProductApiClient client = new ProductApiClient("world.openfoodfacts.org", req ->
                new SimpleHttpResponse(200, jsonPayload));

        Product product = client.fetchProduct("9999999999999");

        assertNull(product);
    }

    @Test
    void testFetchProductHttp404() {
        ProductApiClient client = new ProductApiClient("world.openfoodfacts.org", req ->
                new SimpleHttpResponse(404, "Not Found"));

        Product product = client.fetchProduct("invalid");

        assertNull(product);
    }

    @Test
    void testFetchProductNullOrBlankBarcode() {
        ProductApiClient client = new ProductApiClient();
        assertNull(client.fetchProduct(null));
        assertNull(client.fetchProduct("  "));
    }

    /** Helper stub for HttpResponse<String> without bytecode instrumentation. */
    private record SimpleHttpResponse(int statusCode, String body) implements HttpResponse<String> {
        @Override
        public HttpRequest request() { return null; }
        @Override
        public java.util.Optional<HttpResponse<String>> previousResponse() { return java.util.Optional.empty(); }
        @Override
        public HttpHeaders headers() { return null; }
        @Override
        public URI uri() { return null; }
        @Override
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
        @Override
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
