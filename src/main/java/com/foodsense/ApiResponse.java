/*
 * Gson DTO for an Open Food Facts product-lookup response envelope.
 * Field names match the API JSON ({@code status}, {@code product}, {@code code}).
 */

package com.foodsense;

/**
 * Top-level API payload: status flag, optional product body, and barcode code.
 */
public class ApiResponse {
    private int status;
    private Product product;
    private String code;

    /**
     * Open Food Facts lookup status flag.
     *
     * @return API status (typically {@code 1} when found, {@code 0} when not)
     * @example
     * <pre>{@code
     * if (response.getStatus() == 0) {
     *     // product not found
     * }
     * }</pre>
     */
    public int getStatus() {
        return status;
    }

    /**
     * Product body when the lookup succeeded.
     *
     * @return deserialized product, or {@code null} when not found / absent
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Barcode echoed by the API envelope.
     *
     * @return barcode string echoed by the API, if present
     */
    public String getCode() {
        return code;
    }
}
