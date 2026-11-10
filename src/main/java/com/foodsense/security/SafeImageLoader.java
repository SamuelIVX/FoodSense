/*
 * Asynchronous, bounded image loader for FoodSense.
 * Protects against EDT blocking, SSRF, non-HTTPS URLs, and memory exhaustion (payload caps).
 */

package com.foodsense.security;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Handles safe asynchronous image loading from remote HTTPS URLs.
 * Limits image size to 5MB, enforces strict connect and read timeouts, and dispatches
 * the scaled {@link ImageIcon} back to the Swing EDT.
 */
public final class SafeImageLoader {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB maximum
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "foodsense-image-loader");
        t.setDaemon(true);
        return t;
    });

    private SafeImageLoader() {
        // Prevent instantiation
    }

    /**
     * Asynchronously downloads an image from a verified HTTPS URL, scales it smoothly,
     * and delivers the resulting {@link ImageIcon} to {@code callback} on the EDT.
     * Passes {@code null} to the callback if the URL is invalid, download fails, or data is corrupt.
     *
     * @param urlString target image URL (must be HTTPS)
     * @param targetWidth desired scaled width in pixels
     * @param targetHeight desired scaled height in pixels
     * @param callback consumer invoked on the Swing EDT with the loaded icon, or {@code null} on failure
     * @example
     * <pre>{@code
     * SafeImageLoader.loadImageAsync("https://images.openfoodfacts.org/img.jpg", 180, 200, icon -> {
     *     if (icon != null) {
     *         label.setIcon(icon);
     *     }
     * });
     * }</pre>
     */
    public static void loadImageAsync(String urlString, int targetWidth, int targetHeight, Consumer<ImageIcon> callback) {
        if (!SecurityValidator.isSafeHttpsUrl(urlString)) {
            SwingUtilities.invokeLater(() -> callback.accept(null));
            return;
        }

        EXECUTOR.submit(() -> {
            ImageIcon icon = null;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlString.trim()))
                        .timeout(TIMEOUT)
                        .GET()
                        .build();

                HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 200) {
                    try (InputStream in = response.body()) {
                        byte[] bytes = in.readNBytes(MAX_IMAGE_BYTES + 1);
                        if (bytes.length <= MAX_IMAGE_BYTES) {
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                                double scale = Math.min((double) targetWidth / img.getWidth(),
                                        (double) targetHeight / img.getHeight());
                                int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
                                int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
                                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                                icon = new ImageIcon(scaled);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Return null icon on network, parse, or timeout error
            }

            final ImageIcon result = icon;
            SwingUtilities.invokeLater(() -> callback.accept(result));
        });
    }
}
