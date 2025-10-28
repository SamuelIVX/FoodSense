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
    // Color Scheme
    private static final Color PRIMARY_GREEN = new Color(46, 125, 50);
    private static final Color ACCENT_GREEN = new Color(56, 142, 60);
    private static final Color BACKGROUND_LIGHT = new Color(250, 250, 250);
    private static final Color CARD_BACKGROUND = new Color(255, 255, 255);
    private static final Color BORDER_COLOR = new Color(224, 224, 224);
    private static final Color TEXT_PRIMARY = new Color(33, 33, 33);
    private static final Color TEXT_SECONDARY = new Color(97, 97, 97);

    private JFrame frame;
    private JTextField barcodeField;
    private JButton searchButton;
    private JPanel resultPanel;
    private JPanel infoPanel;
    private JPanel ingredientsPanel;
    private JLabel productNameLabel;
    private JLabel brandsLabel;
    private JLabel nutriscoreLabel;
    private JLabel imageLabel;
    private JLabel ingredientsLabel;
    
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
        this.frame.setSize(800, 700);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLocationRelativeTo(null);
        this.frame.setLayout(new BorderLayout());
        this.frame.getContentPane().setBackground(BACKGROUND_LIGHT);

        // Top - Search Bar Panel
        JPanel searchPanel = new JPanel();
        searchPanel.setBackground(PRIMARY_GREEN);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel searchLabel = new JLabel("Search Barcode:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        searchPanel.add(searchLabel);

        barcodeField = createBarcodeField();
        searchPanel.add(barcodeField);

        searchButton = createStyledButton();
        searchPanel.add(searchButton);

        frame.add(searchPanel, BorderLayout.NORTH);

        // Center - Results Panel
        createResultsPanel();

        // Event Listeners
        assert barcodeField != null;
        barcodeField.addActionListener(e -> searchProduct());
        assert searchButton != null;
        searchButton.addActionListener(e -> searchProduct());
    }

    public void start(){
        this.frame.setVisible(true);
    }

    private JTextField createBarcodeField(){
        barcodeField = new JTextField(10);
        barcodeField.setFont(new Font("Arial", Font.PLAIN, 14));
        barcodeField.setMargin(new Insets(8, 12, 8, 12));
        barcodeField.setBackground(Color.WHITE);
        barcodeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        return barcodeField;
    }

    private JButton createStyledButton(){
        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setForeground(PRIMARY_GREEN);
        searchButton.setBackground(Color.WHITE);
        searchButton.setOpaque(true);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(true);
        searchButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchButton.setBackground(BACKGROUND_LIGHT);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchButton.setBackground(Color.WHITE);
            }
        });

        return searchButton;
    }

    private void createResultsPanel(){
        resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Product info at the top
        infoPanel = new JPanel();
        infoPanel.setBackground(CARD_BACKGROUND);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // Vertical stacking

        imageLabel = new JLabel("");
        imageLabel.setPreferredSize(new Dimension(100, 100));
        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        productNameLabel = new JLabel("");
        productNameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        productNameLabel.setForeground(TEXT_PRIMARY);
        productNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandsLabel = new JLabel("");
        brandsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        brandsLabel.setForeground(TEXT_SECONDARY);
        brandsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nutriscoreLabel = new JLabel("");
        nutriscoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nutriscoreLabel.setForeground(ACCENT_GREEN);
        nutriscoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(imageLabel);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(productNameLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(brandsLabel);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(nutriscoreLabel);

        resultPanel.add(infoPanel, BorderLayout.NORTH);

        // Product Ingredients at the bottom
        ingredientsPanel = new JPanel(new BorderLayout());
        ingredientsPanel.setBackground(BACKGROUND_LIGHT);
        ingredientsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JLabel ingredientsTitle = new JLabel("Ingredients");
        ingredientsTitle.setFont(new Font("Arial", Font.BOLD, 14));
        ingredientsTitle.setForeground(TEXT_PRIMARY);
        ingredientsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel ingredientCard = new JPanel(new BorderLayout());
        ingredientCard.setBackground(CARD_BACKGROUND);
        ingredientCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        ingredientsLabel = new JLabel();
        ingredientsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        ingredientsLabel.setForeground(TEXT_SECONDARY);

        ingredientCard.add(ingredientsLabel, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND_LIGHT);
        wrapper.add(ingredientsTitle, BorderLayout.NORTH);
        wrapper.add(ingredientCard, BorderLayout.CENTER);

        ingredientsPanel.add(wrapper, BorderLayout.CENTER);
        resultPanel.add(ingredientsPanel, BorderLayout.SOUTH);
        ingredientsPanel.setVisible(false);

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
            ApiResponse apiResponse;
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
                Image scaled = icon.getImage().getScaledInstance(110, 120, Image.SCALE_SMOOTH);
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
        brandsLabel.setText("Brand: " + product.getBrands());
        nutriscoreLabel.setText("Nutriscore: " + product.getNutriscore_grade().toUpperCase());

        // Update Product Nutriments
        Nutriments nutriments = product.getNutriments();
        JPanel nutrimentsGrid = createNutrimentsGrid(nutriments);

        // Update Product Ingredients
        ingredientsLabel.setText("<html><body style='width: 100%'>" + product.getIngredients_text() + "</body></html>");
        ingredientsPanel.setVisible(true);

        // Replace center content
        resultPanel.removeAll();
        resultPanel.add(infoPanel, BorderLayout.NORTH);
        resultPanel.add(nutrimentsGrid, BorderLayout.CENTER);
        resultPanel.add(ingredientsPanel, BorderLayout.SOUTH);

        // Refresh UI
        frame.revalidate();
        frame.repaint();
    }

    private JPanel createNutrimentsGrid(Nutriments nutriments){
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(BACKGROUND_LIGHT);
        outerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        JLabel nutrimentsTitle = new JLabel("Nutrition Facts");
        nutrimentsTitle.setFont(new Font("Arial", Font.BOLD, 14));
        nutrimentsTitle.setForeground(TEXT_PRIMARY);
        nutrimentsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        outerPanel.add(nutrimentsTitle, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        gridPanel.setBackground(BACKGROUND_LIGHT);

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

            JPanel card = createNutrimentCard(name, value);
            card.setPreferredSize(new Dimension(200, 80));
            gridPanel.add(card);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBackground(BACKGROUND_LIGHT);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        outerPanel.add(scrollPane, BorderLayout.CENTER);
        return outerPanel;
    }

    private JPanel createNutrimentCard(String name, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setForeground(TEXT_SECONDARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        valueLabel.setForeground(PRIMARY_GREEN);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(nameLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        return card;
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