package com.foodsense;

// Java Swing Imports
import javax.swing.*;
import java.awt.*;

// Gson Imports
import com.google.gson.Gson;

// API Request Imports
import java.lang.reflect.Field;
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
    private JPanel resultPanel;
    private JPanel infoPanel;
    private JLabel productNameLabel;
    private JLabel brandsLabel;
    private JLabel nutriscoreLabel;
    private JLabel imageLabel;
    private JLabel nutrimentsLabel;

    private JLabel carbohydratesValue;

    public FoodSenseGUI() {
        initialize();
    }

    // JFrameObject -> JPanelObject -> JButton/JLabel/JTextField...
    // JPanelObject.add(specific_component);
    // JFrameObject.add(JPanelObject, BorderLayout.SPECIFIED_DIRECTION);

    // Initialize UI
    private void initialize(){
        frame = new JFrame();
        this.frame.setTitle("FoodSense - Barcode Scanner");
        this.frame.setSize(600, 500);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.setLayout(new BorderLayout());

        // Top - Search Bar Panel
        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search Barcode:"));
        barcodeField = createBarcodeField();
        searchPanel.add(barcodeField);
        searchButton = new JButton("Search");
        searchPanel.add(searchButton);
        frame.add(searchPanel, BorderLayout.NORTH);

        // Center - Results Panel
        createResultsPanel();

        // Event Listeners
        barcodeField.addActionListener(e -> searchProduct());
        searchButton.addActionListener(e -> searchProduct());

        this.frame.setVisible(true);
    }

    private JTextField createBarcodeField(){
        barcodeField = new JTextField(10);
        barcodeField.setFont(new Font("", Font.BOLD, 14));
        barcodeField.setMargin(new Insets(5,10,5, 10));

        return barcodeField;
    }

    private void createResultsPanel(){
        resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Product info at the top
        infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        imageLabel = new JLabel("No Image", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(100, 100));
        productNameLabel = new JLabel("Product: ");
        productNameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        brandsLabel = new JLabel("Brand: ");
        nutriscoreLabel = new JLabel("Nutriscore: ");

        infoPanel.add(imageLabel);
        infoPanel.add(productNameLabel);
        infoPanel.add(brandsLabel);
        infoPanel.add(nutriscoreLabel);

        resultPanel.add(infoPanel, BorderLayout.NORTH);

        // Product Information at the center

        // Product Ingredients at the bottom

        frame.add(resultPanel, BorderLayout.CENTER);
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
        try {
            if (product.getImage_front_url() != null && !product.getImage_front_url().isEmpty()) {
                URI uri = new URI(product.getImage_front_url());
                URL url = uri.toURL();
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
                imageLabel.setText(null);
            } else {
                imageLabel.setIcon(null);
                imageLabel.setText("No image available");
            }
        } catch (URISyntaxException | MalformedURLException e) {
            imageLabel.setText("Invalid image URL");
        }

        // Update Product Labels
        productNameLabel.setText(product.getProduct_name());
        brandsLabel.setText(product.getBrands());
        nutriscoreLabel.setText(product.getNutriscore_grade());

        // Update Product Nutriments
        Nutriments nutriments = product.getNutriments();
        JPanel nutrimentsGrid = createNutrimentsGrid(nutriments);

        // Update Product Ingredients

        // Replace center content
        resultPanel.removeAll();
        resultPanel.add(infoPanel, BorderLayout.NORTH);
        resultPanel.add(nutrimentsGrid, BorderLayout.CENTER);

        // Refresh UI
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createNutrimentsGrid(Nutriments nutriments){
        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(BorderFactory.createTitledBorder("Nutriments"));

        Field[] fields = Nutriments.class.getDeclaredFields();
        String name, value;

        for(Field field : fields){
            field.setAccessible(true);
            name = formatFieldName(field.getName());
            try {
                value = (String) field.get(nutriments);
                if (value == null || value.isBlank()) value = "N/A";
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(new Color(245, 247, 250));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
            nameLabel.setForeground(new Color(60, 60, 60));

            JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
            valueLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            valueLabel.setForeground(new Color(80, 80, 80));

            card.add(nameLabel, BorderLayout.NORTH);
            card.add(valueLabel, BorderLayout.CENTER);

            gridPanel.add(card);
        }
        return gridPanel;
    }

    // Helper Function
    private String formatFieldName(String raw) {
        String[] parts = raw.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            formatted.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }
        return formatted.toString().trim();
    }

}