/*
 * Unit tests for SecurityValidator.
 * Tests barcode allowlist matching, Swing HTML escaping, and safe HTTPS URL validation.
 */

package com.foodsense;

import com.foodsense.security.SecurityValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "0049000006346",
        "737628064502",
        "123456",
        "ABC-123_456",
        "3017620422003"
    })
    void isValidBarcode_permitsValidBarcodes(String barcode) {
        assertTrue(SecurityValidator.isValidBarcode(barcode));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "  ",
        "12",                       // Too short (< 3)
        "123/../../etc/passwd",      // Path traversal
        "123 456",                  // Space
        "123?query=1",              // Query delimiter
        "123#fragment",             // Fragment
        "123<script>",              // Injected script
        "12345678901234567890123456789012345678901234567890" // > 48 chars
    })
    void isValidBarcode_rejectsInvalidBarcodes(String barcode) {
        assertFalse(SecurityValidator.isValidBarcode(barcode));
    }

    @Test
    void isValidBarcode_rejectsNull() {
        assertFalse(SecurityValidator.isValidBarcode(null));
    }

    @Test
    void escapeHtml_sanitizesSpecialCharacters() {
        assertEquals("&lt;b&gt;Test &amp; &quot;Value&quot;&#x27;s&lt;/b&gt;",
                SecurityValidator.escapeHtml("<b>Test & \"Value\"'s</b>"));
    }

    @Test
    void escapeHtml_handlesNullAndEmpty() {
        assertEquals("", SecurityValidator.escapeHtml(null));
        assertEquals("", SecurityValidator.escapeHtml(""));
        assertEquals("Clean Text", SecurityValidator.escapeHtml("Clean Text"));
    }

    @Test
    void isSafeHttpsUrl_validatesHttps() {
        assertTrue(SecurityValidator.isSafeHttpsUrl("https://images.openfoodfacts.org/images/1.jpg"));
        assertTrue(SecurityValidator.isSafeHttpsUrl("https://world.openfoodfacts.org/api/v2/product/123"));
        assertTrue(SecurityValidator.isSafeHttpsUrl("https://static.openfoodfacts.org/images/products/1.jpg"));
        assertTrue(SecurityValidator.isSafeHttpsUrl("https://openfoodfacts.org/logo.png"));

        // Insecure scheme
        assertFalse(SecurityValidator.isSafeHttpsUrl("http://images.openfoodfacts.org/images/1.jpg"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("ftp://example.com/img.jpg"));

        // Untrusted external or private addresses
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://example.com/img.jpg"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://evilopenfoodfacts.org/img.jpg"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://localhost/secret"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://127.0.0.1:8080"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://127.0.0.2/"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://0.0.0.0/"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://[::1]/"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://172.16.0.1/"));
        assertFalse(SecurityValidator.isSafeHttpsUrl("https://169.254.169.254/latest/meta-data/"));
        assertFalse(SecurityValidator.isSafeHttpsUrl(null));
        assertFalse(SecurityValidator.isSafeHttpsUrl("not a url"));
    }
}
