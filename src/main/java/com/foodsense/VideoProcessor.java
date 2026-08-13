/**
 * Live webcam barcode scan pipeline: OpenCV grabber producer + ZXing decode consumer.
 * Uses a bounded {@link java.util.concurrent.BlockingQueue} of frames and notifies a
 * {@link BarcodeListener} when a code is found. Side effect: opens a JavaCV {@code CanvasFrame}.
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
 * Assumes a display and camera device {@code 0}; not safe to start twice without stopping first
 * (second {@link #start()} is a no-op while {@code running}).
 */
public class VideoProcessor {
    /**
     * Callback invoked on the consumer thread when a barcode is decoded.
     * Callers that touch Swing must marshal to the EDT themselves.
     */
    public interface BarcodeListener {
        /**
         * @param barcodeText decoded barcode payload (digits / text as returned by ZXing)
         */
        void onBarcodeDetected(String barcodeText);
    }

    private final BlockingQueue<Frame> frameQueue = new LinkedBlockingQueue<>(2);
    private final BarcodeListener listener;
    private volatile boolean running = false;

    /**
     * @param listener notified when a barcode is detected; may be {@code null} to skip callbacks
     */
    public VideoProcessor(BarcodeListener listener) {
        this.listener = listener;
    }

    /**
     * Starts producer (webcam grab) and consumer (decode + preview) threads if not already running.
     */
    public void start() {
        if (running)
            return;
        running = true;

        Thread producer = new Thread(new ProducerTask());
        Thread consumer = new Thread(new ConsumerTask(listener));
        producer.start();
        consumer.start();
    }

    /**
     * Signals both threads to exit and clears the frame queue.
     * Safe to call from the CanvasFrame close listener or after a successful decode.
     */
    public void stop() {
        running = false;
        frameQueue.clear();
    }

    /**
     * Grabs frames from camera index 0 and offers clones into {@link #frameQueue}.
     * Clones are required because {@code grab()} reuses its internal buffer.
     */
    private class ProducerTask implements Runnable {
        @Override
        public void run() {
            try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0)) {
                grabber.start();

                while (running) {
                    Frame frame = grabber.grab();
                    if (frame == null) {
                        running = false;
                        break;
                    }

                    // Clone the frame — grab() reuses the same internal buffer
                    Frame cloned = frame.clone();
                    if (!frameQueue.offer(cloned)) {
                        frameQueue.poll();
                        frameQueue.offer(cloned);
                    }
                }

                grabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Polls frames, runs ZXing decode, draws an optional green box, and shows the live preview.
     */
    private class ConsumerTask implements Runnable {
        private final BarcodeListener listener;

        /**
         * @param listener same listener passed to the outer {@link VideoProcessor}
         */
        public ConsumerTask(BarcodeListener listener) {
            this.listener = listener;
        }

        /**
         * Draws a padded green rectangle when ZXing returns exactly two result points
         * (typical for 1D barcodes). Leaves the frame unchanged otherwise.
         *
         * @param result ZXing decode result with geometry points
         * @param frame  current preview frame to annotate
         * @return frame with box drawn, or the original frame when points are unsuitable
         */
        private Frame drawBoxOverBarcode(Result result, Frame frame) {
            ResultPoint[] points = result.getResultPoints();

            if (points != null && points.length == 2) {
                OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
                Mat mat = matConverter.convert(frame);

                Point p1 = new Point((int) points[0].getX(), (int) points[0].getY());
                Point p2 = new Point((int) points[1].getX(), (int) points[1].getY());

                // Compute expanded bounding box (padding to make it larger & visible)
                int paddingX = 150;
                int paddingY = 120;

                // Compute the corners of the box
                int minX = Math.min(p1.x(), p2.x()) - paddingX;
                int minY = Math.min(p1.y(), p2.y()) - paddingY;
                int maxX = Math.max(p1.x(), p2.x()) + paddingX;
                int maxY = Math.max(p1.y(), p2.y()) + paddingY;

                // Make sure the box is within bounds (not outside the video frame)
                minX = Math.max(0, minX);
                minY = Math.max(0, minY);
                maxX = Math.min(mat.cols() - 1, maxX);
                maxY = Math.min(mat.rows() - 1, maxY);

                // Draw a bold box around the barcode
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
            CanvasFrame canvas = new CanvasFrame("Live Barcode Scanner");
            canvas.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            canvas.setCanvasSize(640, 480);

            // Stop the producer when the user closes the window
            canvas.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    stop();
                }
            });

            Java2DFrameConverter frameToImage = new Java2DFrameConverter();
            MultiFormatReader barcodeReader = new MultiFormatReader();

            while (running && canvas.isVisible()) {
                try {
                    Frame frame = frameQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (frame == null)
                        continue;

                    // Convert Frame → BufferedImage for ZXing
                    BufferedImage image = frameToImage.convert(frame);
                    if (image == null)
                        continue;
                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        Result result = barcodeReader.decode(bitmap);

                        // Draw box on frame
                        frame = drawBoxOverBarcode(result, frame);

                        System.out.println("Detected: " + result.getText() + " (" + result.getBarcodeFormat() + ")");

                        if (listener != null)
                            listener.onBarcodeDetected(result.getText());

                        // Pause for a short moment
                        canvas.showImage(frame);
                        try {
                            Thread.sleep(1000); // 1 second pause
                        } catch (InterruptedException ignored) {
                        }

                        stop();
                        canvas.dispose();
                        break;

                    } catch (NotFoundException e) {
                        // No barcode in this frame
                    }
                    canvas.showImage(frame);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            canvas.dispose();
        }
    }
}
