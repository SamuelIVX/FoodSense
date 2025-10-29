package com.foodsense;

import org.bytedeco.javacv.*;
import java.awt.image.BufferedImage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.WindowConstants;

public class VideoProcessor {
    private static final BlockingQueue<BufferedImage> imageQueue = new LinkedBlockingQueue<>(1);

    static void main(String[] args) {
        // Start the producer and consumer threads.
        Thread producerThread = new Thread(new ProducerTask());
        Thread consumerThread = new Thread(new ConsumerTask());

        producerThread.start();
        consumerThread.start();
    }

    static class ProducerTask implements Runnable{
        @Override
        public void run(){
            FrameGrabber grabber = new  OpenCVFrameGrabber(0);

            CanvasFrame canvas = new CanvasFrame("Snap Barcode", CanvasFrame.getDefaultGamma() / grabber.getGamma());
            canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            Java2DFrameConverter converterImg = new Java2DFrameConverter();

            try{
                grabber.start();
                Frame frame;

                while(canvas.isVisible() && (frame = grabber.grab()) != null){
                    canvas.showImage(frame);

                     // Convert to a BufferedImage for Barcode Scanning
                    BufferedImage bufferedImage = converterImg.convert(frame);
                    imageQueue.offer(bufferedImage); // Using 'offer' to avoid blocking
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                try {
                    grabber.stop();
                    grabber.release();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
                canvas.dispose();
                VideoProcessor.imageQueue.offer(null);
            }
        }
    }

    static class ConsumerTask implements Runnable{
        @Override
        public void run(){
            System.out.println("Waiting for frames...");

            MultiFormatReader formatReader = new MultiFormatReader();

            while(true){
                try{
                    BufferedImage image = imageQueue.take();
                    if (image == null) break;

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try{
                        Result result = formatReader.decode(bitmap);
                        System.out.println("Barcode found: " + result.getText() + " (Format: " + result.getBarcodeFormat() + ")");
                    } catch (NotFoundException e){
                        // No barcode in this frame... skip silently
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}