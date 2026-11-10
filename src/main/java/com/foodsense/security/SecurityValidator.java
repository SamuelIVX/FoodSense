/*
 * Input validation, sanitization, and security defenses for FoodSense.
 * Protects against barcode path manipulation, Swing HTML injection, and SSRF.
 */

package com.foodsense.security;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Utility class providing defense-in-depth security validations:
 * strict barcode formatting, HTML escaping for Swing labels, and HTTPS URL verification.
 */
public final class SecurityValidator {

    private static final java.util.List<String> ALLOWED_IMAGE_HOST_SUFFIXES =
            java.util.List.of("openfoodfacts.org", "openfoodfacts.net");

    /**
     * Barcodes must be 3-48 characters containing only alphanumeric characters, dashes, or underscores.
     * Rejects control characters, path separators ('/' or '\\'), spaces, and query characters ('?', '#').
     */
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,48}$");

    private SecurityValidator() {
        // Prevent instantiation of utility class
    }

    /**
     * Validates whether a barcode string matches the permitted allowlist pattern.
     *
     * @param barcode candidate barcode string
     * @return {@code true} if non-null and matching {@code ^[A-Za-z0-9_-]{3,48}$}; {@code false} otherwise
     * @example
     * <pre>{@code
     * boolean valid = SecurityValidator.isValidBarcode("0049000006346"); // true
     * boolean invalid = SecurityValidator.isValidBarcode("../evil");     // false
     * }</pre>
     */
    public static boolean isValidBarcode(String barcode) {
        if (barcode == null) {
            return false;
        }
        String trimmed = barcode.trim();
        return BARCODE_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Sanitizes untrusted text for safe interpolation inside Swing {@code <html>} components.
     * Converts {@code &}, {@code <}, {@code >}, {@code "}, and {@code '} to HTML entities.
     *
     * @param text raw input text (may be {@code null})
     * @return escaped HTML-safe string, or empty string if input is {@code null}
     * @example
     * <pre>{@code
     * String safe = SecurityValidator.escapeHtml("<script>alert(1)</script>");
     * // returns "&lt;script&gt;alert(1)&lt;/script&gt;"
     * }</pre>
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&'  -> sb.append("&amp;");
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&#x27;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Validates whether a given URL string is an external HTTPS URL and does not target
     * loopback or cloud metadata IP addresses.
     *
     * @param urlString URL candidate to validate
     * @return {@code true} if scheme is strictly {@code https} and host is not a private/loopback address
     * @example
     * <pre>{@code
     * SecurityValidator.isSafeHttpsUrl("https://images.openfoodfacts.org/img.jpg"); // true
     * SecurityValidator.isSafeHttpsUrl("http://169.254.169.254/latest");          // false
     * }</pre>
     */
    public static boolean isSafeHttpsUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(urlString.trim());
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            String lowerHost = host.toLowerCase(java.util.Locale.ROOT);
            for (String suffix : ALLOWED_IMAGE_HOST_SUFFIXES) {
                if (lowerHost.equals(suffix) || lowerHost.endsWith("." + suffix)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
