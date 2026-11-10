/*
 * Unit tests for SafeImageLoader.
 * Verifies non-HTTPS URL rejection, null safety, and callback dispatching.
 */

package com.foodsense;

import com.foodsense.security.SafeImageLoader;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SafeImageLoaderTest {

    @Test
    void loadImageAsync_rejectsInvalidUrlAndCallsBackOnEdt() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean calledOnEdt = new AtomicBoolean(false);
        AtomicBoolean receivedNull = new AtomicBoolean(false);

        SafeImageLoader.loadImageAsync("http://insecure.example.com/test.jpg", 100, 100, icon -> {
            calledOnEdt.set(SwingUtilities.isEventDispatchThread());
            receivedNull.set(icon == null);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Callback should complete promptly");
        assertTrue(calledOnEdt.get(), "Callback must run on the Swing EDT");
        assertTrue(receivedNull.get(), "Icon must be null for insecure URL");
    }

    @Test
    void loadImageAsync_handlesNullUrl() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean receivedNull = new AtomicBoolean(false);

        SafeImageLoader.loadImageAsync(null, 100, 100, icon -> {
            receivedNull.set(icon == null);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(receivedNull.get());
    }
}
