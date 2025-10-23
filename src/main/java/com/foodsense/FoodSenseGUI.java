package com.foodsense;

// Java Swing Imports
import javax.swing.*;
import java.awt.*;

// Gson Imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// API Request Imports
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FoodSenseGUI{
    private JFrame frame;
    private JTextField barcodeField;
    private JButton searchButton;
    private JTextArea resultArea;

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
        barcodeField = new JTextField(10);
        searchPanel.add(barcodeField);
        searchButton = new JButton("Search");
        searchPanel.add(searchButton);
        frame.add(searchPanel, BorderLayout.NORTH);

        // Center - Results
        JPanel resultPanel = createResultsPanel();
        frame.add(resultPanel, BorderLayout.CENTER);

        // Event Listeners
        barcodeField.addActionListener(e -> searchProduct());
        searchButton.addActionListener(e -> searchProduct());

        private JPanel createResultsPanel(){
            JPanel resultsPanel = new JPanel();

            return resultsPanel;
        }

        private void searchProduct(){}

        this.frame.setVisible(true);
    }
}