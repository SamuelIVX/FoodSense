/*
 * Application entry point for FoodSense.
 * Supports both CLI barcode lookup mode (--barcode <code> / -b <code>) and GUI launch mode.
 */

package com.foodsense;

/**
 * Main springboard for FoodSense: CLI barcode search or GUI launch.
 */
public class FoodSense {

    /**
     * Entry point. If arguments specify a barcode lookup ({@code --barcode <code>} or {@code -b <code>}),
     * executes a headless CLI lookup and prints nutrition data to standard output. Otherwise, launches
     * the Swing desktop GUI.
     *
     * @param args command-line arguments
     * @example
     * <pre>{@code
     * // Headless CLI search:
     * FoodSense.main(new String[]{"--barcode", "0049000006346"});
     * // GUI launch:
     * FoodSense.main(new String[0]);
     * }</pre>
     */
    public static void main(String[] args) {
        if (args != null && args.length >= 2 && ("--barcode".equals(args[0]) || "-b".equals(args[0]))) {
            String barcode = args[1];
            ProductApiClient client = new ProductApiClient();
            Product product = client.fetchProduct(barcode);
            if (product != null) {
                System.out.println("Product: " + (product.getProduct_name() != null ? product.getProduct_name() : "N/A"));
                System.out.println("Brand: " + (product.getBrands() != null ? product.getBrands() : "N/A"));
                System.out.println("Nutri-Score: " + (product.getNutriscore_grade() != null ? product.getNutriscore_grade().toUpperCase() : "N/A"));
                if (product.getNutriments() != null) {
                    System.out.println("Calories (energy): " + product.getNutriments().getEnergy());
                }
            } else {
                System.out.println("Product not found for barcode: " + barcode);
            }
            return;
        }

        FoodSenseGUI foodSenseGUI = new FoodSenseGUI();
        foodSenseGUI.start();
    }
}
