/*
 * Live webcam barcode scan pipeline: OpenCV grabber producer + ZXing decode consumer.
 * Uses a bounded {@link java.util.concurrent.BlockingQueue} of frames and notifies a
 * {@link BarcodeListener} when a code is found. Supports in-app frame rendering or fallback CanvasFrame.
 */

package com.foodsense;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import javax.swing.WindowConstants;

/**
 * Producer/consumer webcam scanner. Call {@link #start()} once; {@link #stop()} ends both threads.
 * Assumes a display and camera device {@code 0}; safe to call {@link #stop()} multiple times.
 */
public class VideoProcessor {

    /**
     * Callback invoked on the consumer thread when a barcode is decoded.
     * Callers that touch Swing must marshal to the EDT themselves.
     */
    public interface BarcodeListener {
        /**
         * Called on the consumer thread after a successful ZXing decode.
         *
         * @param barcodeText decoded barcode payload (digits / text as returned by ZXing)
         */
        void onBarcodeDetected(String barcodeText);
    }

    /**
     * Callback invoked on the consumer thread with each live preview image.
     */
    @FunctionalInterface
    public interface FrameListener {
        /**
         * Called when a new processed frame is available for rendering.
         *
         * @param image decoded video frame as a BufferedImage
         */
        void onFrame(BufferedImage image);
    }

    /**
     * Callback invoked when a camera hardware or permission error occurs.
     */
    @FunctionalInterface
    public interface ErrorListener {
        /**
         * Called when camera acquisition or streaming encounters a fatal error.
         *
         * @param message diagnostic error message
         */
        void onError(String message);
    }

    private final BlockingQueue<Frame> frameQueue = new LinkedBlockingQueue<>(2);
    private final BarcodeListener listener;
    private final FrameListener frameListener;
    private final ErrorListener errorListener;
    private volatile boolean running = false;
    private CanvasFrame canvas = null;

    /**
     * Creates a scanner that will notify {@code listener} when a code is found.
     * Uses fallback {@link CanvasFrame} for video preview.
     *
     * @param listener notified when a barcode is detected; may be {@code null} to skip callbacks
     */
    public VideoProcessor(BarcodeListener listener) {
        this(listener, null, null);
    }

    /**
     * Creates a scanner with in-app frame streaming and error notification callbacks.
     *
     * @param listener      notified when a barcode is detected
     * @param frameListener stream listener for in-app frame painting (if null, CanvasFrame is used)
     * @param errorListener notified if camera cannot be accessed or throws an exception
     */
    public VideoProcessor(BarcodeListener listener, FrameListener frameListener, ErrorListener errorListener) {
        this.listener = listener;
        this.frameListener = frameListener;
        this.errorListener = errorListener;
    }

    /**
     * Starts producer (webcam grab) and consumer (decode + preview) threads if not already running.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;

        Thread producer = new Thread(new ProducerTask(), "foodsense-camera-producer");
        Thread consumer = new Thread(new ConsumerTask(listener), "foodsense-camera-consumer");
        producer.setDaemon(true);
        consumer.setDaemon(true);
        producer.start();
        consumer.start();
    }

    /**
     * Signals both threads to exit, disposes canvas if active, and clears the frame queue.
     */
    public synchronized void stop() {
        running = false;
        frameQueue.clear();
        if (canvas != null) {
            try {
                canvas.dispose();
            } catch (Exception ignored) {
            }
            canvas = null;
        }
    }

    /**
     * Returns whether the scanner is actively running.
     *
     * @return {@code true} if running; {@code false} otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Grabs frames from camera index 0 and offers clones into {@link #frameQueue}.
     * Clones are required because {@code grab()} reuses its internal buffer.
     */
    private class ProducerTask implements Runnable {
        @Override
        public void run() {
            OpenCVFrameGrabber grabber = null;
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame == null) {
                        running = false;
                        break;
                    }

                    Frame cloned = frame.clone();
                    if (!frameQueue.offer(cloned)) {
                        frameQueue.poll();
                        frameQueue.offer(cloned);
                    }
                }
            } catch (Exception e) {
                running = false;
                if (errorListener != null) {
                    errorListener.onError("Camera error: " + (e.getMessage() != null ? e.getMessage() : "Unable to access camera device"));
                }
            } finally {
                if (grabber != null) {
                    try {
                        grabber.stop();
                        grabber.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Polls frames, runs ZXing decode, draws an optional green box, and streams preview.
     */
    private class ConsumerTask implements Runnable {
        private final BarcodeListener listener;

        public ConsumerTask(BarcodeListener listener) {
            this.listener = listener;
        }

        private Frame drawBoxOverBarcode(Result result, Frame frame) {
            ResultPoint[] points = result.getResultPoints();
            if (points != null && points.length == 2) {
                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                Mat mat = matConverter.convert(frame);

                Point p1 = new Point((int) points[0].getX(), (int) points[0].getY());
                Point p2 = new Point((int) points[1].getX(), (int) points[1].getY());

                int paddingX = 150;
                int paddingY = 120;

                int minX = Math.max(0, Math.min(p1.x(), p2.x()) - paddingX);
                int minY = Math.max(0, Math.min(p1.y(), p2.y()) - paddingY);
                int maxX = Math.min(mat.cols() - 1, Math.max(p1.x(), p2.x()) + paddingX);
                int maxY = Math.min(mat.rows() - 1, Math.max(p1.y(), p2.y()) + paddingY);

                rectangle(
                        mat,
                        new Point(minX, minY),
                        new Point(maxX, maxY),
                        new Scalar(0, 255, 0, 0),
                        4,
                        LINE_AA,
                        0);

                frame = matConverter.convert(mat);
            }
            return frame;
        }

        @Override
        public void run() {
            if (frameListener == null) {
                canvas = new CanvasFrame("Live Barcode Scanner");
                canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                canvas.setCanvasSize(640, 480);
                canvas.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        stop();
                    }
                });
            }

            Java2DFrameConverter frameToImage = new Java2DFrameConverter();
            MultiFormatReader barcodeReader = new MultiFormatReader();

            while (running && (frameListener != null || (canvas != null && canvas.isVisible()))) {
                try {
                    Frame frame = frameQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (frame == null) {
                        continue;
                    }

                    BufferedImage image = frameToImage.convert(frame);
                    if (image == null) {
                        continue;
                    }

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        Result result = barcodeReader.decode(bitmap);
                        frame = drawBoxOverBarcode(result, frame);
                        image = frameToImage.convert(frame);

                        if (listener != null) {
                            listener.onBarcodeDetected(result.getText());
                        }

                        if (frameListener != null) {
                            frameListener.onFrame(image);
                        } else if (canvas != null) {
                            canvas.showImage(frame);
                        }

                        try {
                            Thread.sleep(600);
                        } catch (InterruptedException ignored) {
                        }

                        stop();
                        break;
                    } catch (NotFoundException ignored) {
                        // Frame did not contain a valid barcode
                    }

                    if (frameListener != null) {
                        frameListener.onFrame(image);
                    } else if (canvas != null) {
                        canvas.showImage(frame);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {
                }
            }

            if (canvas != null) {
                try {
                    canvas.dispose();
                } catch (Exception ignored) {
                }
                canvas = null;
            }
        }
    }
}
