package com.foodsense;

// Java Swing Imports
import javax.swing.*;
import java.awt.*;

// Gson Imports
import com.google.gson.Gson;

// API Request Imports
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FoodSenseGUI{
    private JFrame frame;
    private JTextField barcodeField;
    private JButton searchButton;
    private JTextArea resultArea;
    private JPanel resultPanel;
    private JLabel productNameLabel;
    private JLabel brandsLabel;
    private JLabel nutriscoreLabel;
    private JLabel imageLabel;
    private JTextArea infoArea;

    public FoodSenseGUI() {
        initialize();
    }

    // JFrameObject -> JPanelObject -> JButton/JLabel/JTextField...
    // JPanelObject.add(specific_component);
    // JFrameObject.add(JPanelObject, BorderLayout.SPECIFIED_DIRECTION);

    // Creating the entire layout of my frame
    private void initialize(){
        frame = new JFrame();
        this.frame.setTitle("FoodSense - Barcode Scanner");
        this.frame.setSize(600, 500);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.setLayout(new BorderLayout());

        // Top - Search Bar
        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search Barcode:"));
        barcodeField = createBarcodeField();
        searchPanel.add(barcodeField);
        searchButton = new JButton("Search");
        searchPanel.add(searchButton);
        frame.add(searchPanel, BorderLayout.NORTH);

        // Center - Results
        resultPanel = createResultsPanel();
        frame.add(resultPanel, BorderLayout.CENTER);

        // Event Listeners
        barcodeField.addActionListener(e -> searchProduct());
        searchButton.addActionListener(e -> searchProduct());

        this.frame.setVisible(true);
    }

    private JTextField createBarcodeField(){
        barcodeField = new JTextField(10);
        barcodeField.setFont(new Font("", Font.BOLD, 14));
        barcodeField.setMargin(new Insets(5,10,5, 10));

        barcodeField.setBackground(Color.GRAY);

        return barcodeField;
    }

    private JPanel createResultsPanel(){
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Product info at the top
        JPanel infoPanel = new JPanel();
        imageLabel = new JLabel("No Image", SwingConstants.CENTER);
        productNameLabel = new JLabel("Product: ");
        productNameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        brandsLabel = new JLabel("Brand: ");
        brandsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        nutriscoreLabel = new JLabel("Nutriscore: ");
        nutriscoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoPanel.add(imageLabel);
        infoPanel.add(productNameLabel);
        infoPanel.add(brandsLabel);
        infoPanel.add(nutriscoreLabel);
        resultsPanel.add(infoPanel, BorderLayout.NORTH);

        // Product Information at the center
        infoArea = new JTextArea(5, 20);
        infoArea.setLineWrap(true);
        infoArea.setFont(new Font("Arial", Font.PLAIN, 12));
        infoArea.setEditable(false);
        JScrollPane ingredientsScrollPane = new JScrollPane(infoArea);
        ingredientsScrollPane.setBorder(BorderFactory.createTitledBorder("Product Information"));
        resultsPanel.add(ingredientsScrollPane, BorderLayout.CENTER);

        return resultsPanel;
    }

    private void searchProduct(){
        String barcode = barcodeField.getText();

        if(barcode.isEmpty()){
            JOptionPane.showMessageDialog(frame, "Please enter a barcode");
            return;
        }

        // Show loading state
        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        // Fetch product from API
        Product product = fetchProductFromAPI(barcode);

        try {
            if (product != null) {
                displayProduct(product);
            } else {
                JOptionPane.showMessageDialog(frame, "Product not found", "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally{
            searchButton.setEnabled(true);
            searchButton.setText("Search");
        }
    }

    private Product fetchProductFromAPI(String barcode){
        try {
            ApiResponse apiResponse = new ApiResponse();
            Gson gson = new Gson();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://world.openfoodfacts.net/api/v2/product/" + barcode))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            apiResponse = gson.fromJson(response.body(), ApiResponse.class);

            if(apiResponse.getStatus() == 0){
                return null; // Product not found
            }

            return apiResponse.getProduct();

        } catch (Exception e) {
            return null;
        }
    }

    private void displayProduct(Product product){
        // Update Product Image
        if(product.getImage_front_url() != null && !product.getImage_front_url().isEmpty()){
            URI uri = null;
            try {
                uri = new URI(product.getImage_front_url());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            URL url = null;
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            ImageIcon imageIcon = new ImageIcon(url);

            // Scale Image
            Image scaledImage = imageIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
            imageLabel.setText(null);
        }  else {
            imageLabel.setIcon(null);
            imageLabel.setText("No image available");
        }

        // Update Product Labels
        productNameLabel.setText(product.getProduct_name());
        brandsLabel.setText(product.getBrands());
        nutriscoreLabel.setText(product.getNutriscore_grade());

        // Update Product Information
        Nutriments nutriments = product.getNutriments();
        infoArea.setText(nutriments.toString());

        // Update Product Ingredients

        // Refresh UI
        frame.revalidate();
        frame.repaint();
    }

}