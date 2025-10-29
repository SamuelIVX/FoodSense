package com.foodsense;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.Mat;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SnapBarcode {
    public static void main(String[] args) throws Exception {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();

        Frame frame = grabber.grab();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        ImageIO.write(converter.convert(frame), "PNG", new File("test.png"));

        grabber.stop();
        System.out.println("Image captured successfully!");
    }
}