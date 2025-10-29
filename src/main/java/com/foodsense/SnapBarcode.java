package com.foodsense;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Buffer;
import org.bytedeco.opencv.opencv_core.Mat;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import static org.bytedeco.opencv.global.opencv_core.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.stream.ImageInputStream;
import javax.swing.WindowConstants;

public class SnapBarcode {
    static void main(String[] args) throws Exception {
        BlockingQueue<BufferedImage> sharedQueue = new LinkedBlockingQueue<>(1);
        FrameGrabber grabber = new OpenCVFrameGrabber(0);

        CanvasFrame canvas = new CanvasFrame("Snap Barcode", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Java2DFrameConverter converterImg = new Java2DFrameConverter();

        // Camera Thread
        new Thread(() -> {
            try{
                grabber.start();
                Frame frame;

                while (canvas.isVisible() && (frame = grabber.grab()) != null) {
                    Mat mat = converter.convert(frame);
                    if(mat != null){
                        Mat flippedMat = new Mat();
                        flip(mat, flippedMat, 1);  // Flipping the JavaCV camera horizontally

                        Frame flippedFrame = converter.convert(flippedMat);
                        canvas.showImage(flippedFrame);

                        // Convert to a BufferedImage for barcode scanning
                        BufferedImage bufferedImage = converterImg.convert(flippedFrame);
                        sharedQueue.offer(bufferedImage); // Using 'offer' instead of 'put' to avoid blocking

                        flippedMat.release();
                        mat.release();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                try{
                    grabber.stop();
                    grabber.release();
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
                canvas.dispose();
            }
        }).start();

        // Barcode Scanning Thread
        new Thread(() -> {
            MultiFormatReader formatReader = new MultiFormatReader();

            while(true){
                try{
                    BufferedImage image = sharedQueue.take();
                    if (image == null) break;

                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try{
                        Result result = formatReader.decode(bitmap);
                        System.out.println("Barcode found: " + result.getText() + " (Format: " + result.getBarcodeFormat() + ")");
                    } catch(NotFoundException e){
                        // No barcode in this frame, continue silently
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
}