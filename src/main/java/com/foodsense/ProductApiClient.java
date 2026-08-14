/*
 * Open Food Facts API client for FoodSense.
 * Handles HTTP requests, host resolution (.org default with -Dfoodsense.host and FOODSENSE_HOST overrides),
 * and JSON deserialization into Product domain objects.
 */

package com.foodsense;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for fetching product nutrition data from the Open Food Facts API.
 */
public class ProductApiClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .build();

    /** Functional interface abstraction for sending HTTP requests. */
    @FunctionalInterface
    public interface HttpSender {
        HttpResponse<String> send(HttpRequest request) throws Exception;
    }

    public static final String DEFAULT_HOST = "world.openfoodfacts.org";

    private final String host;
    private final HttpSender httpSender;
    private final Gson gson;

    /**
     * Constructs a client using default host resolution and standard HttpClient.
     */
    public ProductApiClient() {
        this(resolveDefaultHost());
    }

    /**
     * Constructs a client with explicit host and default HttpClient.
     *
     * @param host API host domain
     */
    public ProductApiClient(String host) {
        this(host, DEFAULT_HTTP_CLIENT);
    }

    /**
     * Constructs a client with explicit host and configured HttpClient.
     *
     * @param host       API host domain
     * @param httpClient shared HttpClient instance
     */
    public ProductApiClient(String host, HttpClient httpClient) {
        this(host, request -> httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    /**
     * Constructs a client with explicit host and custom HTTP sender.
     *
     * @param host       API host domain (e.g. {@code "world.openfoodfacts.org"})
     * @param httpSender sender function for executing HTTP requests
     */
    public ProductApiClient(String host, HttpSender httpSender) {
        this.host = host != null && !host.isBlank() ? host : resolveDefaultHost();
        this.httpSender = httpSender != null ? httpSender : request -> DEFAULT_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        this.gson = new Gson();
    }

    /**
     * Resolves the target host. Checks system property {@code foodsense.host}, then
     * env var {@code FOODSENSE_HOST}, defaulting to {@link #DEFAULT_HOST}.
     *
     * @return host string
     */
    static String resolveDefaultHost() {
        return resolveHost(System.getProperty("foodsense.host"), System.getenv("FOODSENSE_HOST"));
    }

    /**
     * Resolves host from explicit system property and environment variable inputs.
     *
     * @param sysProp system property value (may be null)
     * @param envVar  environment variable value (may be null)
     * @return resolved host string
     */
    static String resolveHost(String sysProp, String envVar) {
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        if (envVar != null && !envVar.isBlank()) {
            return envVar;
        }
        return DEFAULT_HOST;
    }

    /**
     * Gets the configured host for this client.
     *
     * @return host string
     */
    public String getHost() {
        return host;
    }

    /**
     * Constructs the full request URI for a given barcode.
     *
     * @param barcode barcode string
     * @return request URI
     */
    public URI buildUri(String barcode) {
        return URI.create("https://" + host + "/api/v2/product/" + barcode);
    }

    /**
     * GETs product details from Open Food Facts API and deserializes with Gson.
     * Network I/O — call off the EDT.
     *
     * @param barcode product barcode
     * @return {@link Product} when found (status != 0); {@code null} on failure or status == 0
     */
    public Product fetchProduct(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(barcode.trim()))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> resp = httpSender.send(request);

            if (resp == null || resp.statusCode() != 200) {
                return null;
            }

            ApiResponse api = gson.fromJson(resp.body(), ApiResponse.class);
            return (api == null || api.getStatus() == 0) ? null : api.getProduct();
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
            return null;
        }
    }
}
