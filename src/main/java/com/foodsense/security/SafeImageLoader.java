/*
 * Asynchronous, bounded image loader for FoodSense.
 * Protects against EDT blocking, SSRF, non-HTTPS URLs, memory exhaustion (payload & pixel caps),
 * and hung response body streams.
 */

package com.foodsense.security;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Handles safe asynchronous image loading from verified HTTPS Open Food Facts URLs.
 * Limits image size to 5MB, caps pixel dimensions to 16 megapixels, enforces a watchdog
 * deadline on body reading, and dispatches the scaled {@link ImageIcon} back to the Swing EDT.
 */
public final class SafeImageLoader {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5 MB maximum
    private static final long MAX_IMAGE_PIXELS = 16_000_000L;   // 16 Megapixels
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long BODY_DEADLINE_SECONDS = 10;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "foodsense-image-loader");
        t.setDaemon(true);
        return t;
    });

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "foodsense-image-watchdog");
        t.setDaemon(true);
        return t;
    });

    private SafeImageLoader() {
        // Prevent instantiation
    }

    /**
     * Asynchronously downloads an image from a verified HTTPS URL, scales it smoothly preserving aspect ratio,
     * and delivers the resulting {@link ImageIcon} to {@code callback} on the EDT.
     * Passes {@code null} to the callback if the URL is invalid, download fails, or data is corrupt.
     *
     * @param urlString target image URL (must be HTTPS)
     * @param targetWidth desired scaled width in pixels
     * @param targetHeight desired scaled height in pixels
     * @param callback consumer invoked on the Swing EDT with the loaded icon, or {@code null} on failure
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
                        ScheduledFuture<?> guard = WATCHDOG.schedule(() -> {
                            try {
                                in.close();
                            } catch (IOException ignored) {
                            }
                        }, BODY_DEADLINE_SECONDS, TimeUnit.SECONDS);

                        byte[] bytes;
                        try {
                            bytes = in.readNBytes(MAX_IMAGE_BYTES + 1);
                        } finally {
                            guard.cancel(false);
                        }

                        if (bytes.length <= MAX_IMAGE_BYTES) {
                            BufferedImage img = readBounded(bytes);
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
                } else if (response.body() != null) {
                    response.body().close();
                }
            } catch (Exception ignored) {
                // Return null icon on network, parse, or timeout error
            }

            final ImageIcon result = icon;
            SwingUtilities.invokeLater(() -> callback.accept(result));
        });
    }

    /**
     * Reads image bytes safely by inspecting image dimensions through ImageReader
     * before allocating memory for the full raster, mitigating decompression bombs.
     */
    private static BufferedImage readBounded(byte[] bytes) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                return pixels <= MAX_IMAGE_PIXELS ? reader.read(0) : null;
            } finally {
                reader.dispose();
            }
        }
    }
}
