/*
 * Application entry point for FoodSense.
 * Instantiates {@link FoodSenseGUI} and shows the main window via {@link FoodSenseGUI#start()}.
 */

package com.foodsense;

/**
 * Launches the FoodSense Swing desktop app.
 */
public class FoodSense {

    /**
     * Starts the GUI. Requires a display (Swing); webcam is optional until Scan is used.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        FoodSenseGUI foodSenseGUI = new FoodSenseGUI();
        foodSenseGUI.start();
    }
}
