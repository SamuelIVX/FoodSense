/*
 * Swing UI and Open Food Facts client for FoodSense.
 * Builds the sidebar + product view, runs barcode search on a background thread,
 * and marshals UI updates onto the EDT via {@link SwingUtilities#invokeLater}.
 */

package com.foodsense;

import com.foodsense.security.SafeImageLoader;
import com.foodsense.security.SecurityValidator;
import com.foodsense.ui.AllergenBanner;
import com.foodsense.ui.CameraScanDialog;
import com.foodsense.ui.FoodSenseTheme;
import com.foodsense.ui.NutriScoreSpectrumView;
import com.foodsense.ui.NutritionLabelPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Main application window: manual barcode entry, in-app webcam scan handoff,
 * theme toggle, and modern nutrition display.
 */
public class FoodSenseGUI {

    private final ProductApiClient apiClient;
    private JFrame frame;
    private JPanel root;
    private JPanel contentArea;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton cameraBtn;
    private JButton themeToggleBtn;
    private JLabel bannerLabel;

    private Product currentProduct = null;
    private int imageRequestId = 0;

    // Sidebar product preview widgets
    private JPanel productCard;
    private JLabel imgLabel;
    private JLabel nameLabel;
    private JLabel brandLabel;
    private NutriScoreSpectrumView nutriScoreView;

    /**
     * Constructs the main frame and widget tree with a default API client.
     * Call {@link #start()} to show the window.
     */
    public FoodSenseGUI() {
        this(new ProductApiClient());
    }

    /**
     * Constructs the main frame and widget tree with a custom API client.
     *
     * @param apiClient API client instance
     */
    public FoodSenseGUI(ProductApiClient apiClient) {
        this.apiClient = apiClient != null ? apiClient : new ProductApiClient();
        FoodSenseTheme.initialize();
        build();
    }

    /**
     * Makes the main frame visible. Must be called after construction on the EDT.
     *
     * @example
     * <pre>{@code
     * new FoodSenseGUI().start();
     * }</pre>
     */
    public void start() {
        frame.setVisible(true);
    }

    /**
     * Returns the JFrame instance.
     *
     * @return top-level JFrame
     */
    public JFrame getFrame() {
        return frame;
    }

    // ── Frame Construction ────────────────────────────────────

    private void build() {
        frame = new JFrame("FoodSense");
        frame.setSize(1140, 780);
        frame.setMinimumSize(new Dimension(960, 640));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        root = new JPanel(new BorderLayout());
        root.setBackground(FoodSenseTheme.getBackground());

        root.add(buildSidebar(), BorderLayout.WEST);

        contentArea = buildEmptyState();
        root.add(contentArea, BorderLayout.CENTER);

        frame.setContentPane(root);
    }

    // ── Sidebar ───────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(FoodSenseTheme.getSidebarBackground());
        sidebar.setPreferredSize(new Dimension(300, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, FoodSenseTheme.getCardBorder()));

        // Top Section: Header, Theme Toggle, Search Input & Buttons
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));

        // Header row with logo and Theme Toggle
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel logo = new JLabel("FoodSense");
        logo.setFont(FoodSenseTheme.getFont(Font.BOLD, 20f));
        logo.setForeground(FoodSenseTheme.getPrimaryAccent());
        titleBlock.add(logo);

        JLabel tagline = new JLabel("Nutrition Intelligence");
        tagline.setFont(FoodSenseTheme.getFont(Font.PLAIN, 11f));
        tagline.setForeground(FoodSenseTheme.getTextMuted());
        titleBlock.add(tagline);

        headerRow.add(titleBlock, BorderLayout.WEST);

        // Icon-only Theme Toggle button
        themeToggleBtn = new JButton(FoodSenseTheme.isDarkMode() ? "☀" : "☾");
        themeToggleBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 14f));
        themeToggleBtn.setToolTipText("Toggle Light / Dark mode");
        themeToggleBtn.setFocusPainted(false);
        themeToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeToggleBtn.putClientProperty("JButton.buttonType", "roundRect");
        themeToggleBtn.addActionListener(e -> {
            FoodSenseTheme.toggleTheme(frame);
            themeToggleBtn.setText(FoodSenseTheme.isDarkMode() ? "☀" : "☾");
            updateComponentColors();
        });
        headerRow.add(themeToggleBtn, BorderLayout.EAST);
        top.add(headerRow);

        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_20));

        // Barcode label
        JLabel barcodeLabel = new JLabel("PRODUCT BARCODE");
        barcodeLabel.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
        barcodeLabel.setForeground(FoodSenseTheme.getTextMuted());
        barcodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(barcodeLabel);
        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));

        // Search text field
        searchField = new JTextField();
        searchField.setFont(FoodSenseTheme.getFont(Font.PLAIN, 13f));
        searchField.putClientProperty("JTextField.placeholderText", "Enter barcode (e.g. 0049000006346)");
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.addActionListener(e -> searchProduct());
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                clearBanner();
            }
        });
        top.add(searchField);
        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));

        // Action Buttons Row (Search & Scan)
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchBtn = new JButton("Search");
        searchBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> searchProduct());

        cameraBtn = new JButton("Scan with Camera");
        cameraBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
        cameraBtn.setFocusPainted(false);
        cameraBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cameraBtn.addActionListener(e -> openCameraModal());

        btnRow.add(searchBtn);
        btnRow.add(cameraBtn);
        top.add(btnRow);

        // Status banner
        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));
        bannerLabel = new JLabel("");
        bannerLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        bannerLabel.setForeground(FoodSenseTheme.getDanger());
        bannerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bannerLabel.setVisible(false);
        top.add(bannerLabel);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(buildSidebarProductCard(), BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel buildSidebarProductCard() {
        productCard = new JPanel();
        productCard.setLayout(new BoxLayout(productCard, BoxLayout.Y_AXIS));
        productCard.setOpaque(false);
        productCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        productCard.add(sep);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));

        // Product image container with smooth placeholder
        imgLabel = new JLabel();
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        imgLabel.setPreferredSize(new Dimension(180, 180));
        imgLabel.setMaximumSize(new Dimension(180, 180));
        imgLabel.setVisible(false);
        productCard.add(imgLabel);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_12));

        nameLabel = new JLabel();
        nameLabel.setFont(FoodSenseTheme.getFont(Font.BOLD, 14f));
        nameLabel.setForeground(FoodSenseTheme.getTextPrimary());
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setVisible(false);
        productCard.add(nameLabel);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));

        brandLabel = new JLabel();
        brandLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        brandLabel.setForeground(FoodSenseTheme.getTextMuted());
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandLabel.setVisible(false);
        productCard.add(brandLabel);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));

        nutriScoreView = new NutriScoreSpectrumView();
        nutriScoreView.setAlignmentX(Component.LEFT_ALIGNMENT);
        nutriScoreView.setVisible(false);
        productCard.add(nutriScoreView);

        return productCard;
    }

    private void updateComponentColors() {
        root.setBackground(FoodSenseTheme.getBackground());
        if (currentProduct != null) {
            updateSidebar(currentProduct);
            swapContent(buildProductView(currentProduct));
        } else {
            swapContent(buildEmptyState());
        }
        frame.revalidate();
        frame.repaint();
    }

    // ── Content Panes ─────────────────────────────────────────

    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(FoodSenseTheme.getBackground());

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel icon = new JLabel("⌕");
        icon.setFont(FoodSenseTheme.getFont(Font.PLAIN, 56f));
        icon.setForeground(FoodSenseTheme.getTextMuted());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("No Product Loaded");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 20f));
        title.setForeground(FoodSenseTheme.getTextPrimary());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Enter a barcode on the left or scan with your webcam to inspect nutrition facts.");
        hint.setFont(FoodSenseTheme.getFont(Font.PLAIN, 13f));
        hint.setForeground(FoodSenseTheme.getTextMuted());
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_12));
        inner.add(title);
        inner.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));
        inner.add(hint);

        p.add(inner);
        return p;
    }

    private JPanel buildProductView(Product p) {
        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBackground(FoodSenseTheme.getBackground());
        sections.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        // 1. Structured Nutrition Card
        sections.add(new NutritionLabelPanel(p.getNutriments()));
        sections.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_24));

        // 2. Ingredients Section
        sections.add(buildIngredientsSection(p));

        // 3. Allergens Section (if present)
        if (p.getAllergens_from_ingredients() != null && !p.getAllergens_from_ingredients().isBlank()) {
            sections.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_24));
            sections.add(new AllergenBanner(p.getAllergens_from_ingredients()));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(FoodSenseTheme.getBackground());
        wrapper.add(sections, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(FoodSenseTheme.getBackground());

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(FoodSenseTheme.getBackground());
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildIngredientsSection(Product p) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Ingredients");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 18f));
        title.setForeground(FoodSenseTheme.getTextPrimary());
        section.add(title);
        section.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_12));

        String text = (p.getIngredients_text() != null && !p.getIngredients_text().isBlank())
                ? p.getIngredients_text()
                : "No ingredients information available for this product.";

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FoodSenseTheme.getCardBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(FoodSenseTheme.getCardBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea area = new JTextArea(text);
        area.setFont(FoodSenseTheme.getFont(Font.PLAIN, 13f));
        area.setForeground(FoodSenseTheme.getTextPrimary());
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        card.add(area, BorderLayout.CENTER);

        section.add(card);
        return section;
    }

    // ── Logic & Handlers ──────────────────────────────────────

    private void searchProduct() {
        if (!searchBtn.isEnabled()) {
            return;
        }
        clearBanner();
        String rawBarcode = searchField.getText();

        if (rawBarcode == null || rawBarcode.trim().isEmpty()) {
            showBanner("Please enter a barcode number.");
            return;
        }

        String barcode = rawBarcode.trim();
        if (!SecurityValidator.isValidBarcode(barcode)) {
            showBanner("Invalid barcode format. Use numbers or alphanumeric codes (3-48 chars).");
            return;
        }

        searchBtn.setEnabled(false);
        cameraBtn.setEnabled(false);
        searchBtn.setText("Fetching...");

        new Thread(() -> {
            Product product = apiClient.fetchProduct(barcode);
            SwingUtilities.invokeLater(() -> {
                if (product != null) {
                    this.currentProduct = product;
                    updateSidebar(product);
                    swapContent(buildProductView(product));
                } else {
                    showBanner("Product not found. Verify the code and try again.");
                }
                searchBtn.setEnabled(true);
                cameraBtn.setEnabled(true);
                searchBtn.setText("Search");
            });
        }, "foodsense-api-search").start();
    }

    private void openCameraModal() {
        clearBanner();
        CameraScanDialog dialog = new CameraScanDialog(frame, scannedCode -> {
            searchField.setText(scannedCode);
            searchProduct();
        });
        dialog.open();
    }

    private void updateSidebar(Product p) {
        final int currentReq = ++imageRequestId;

        // Load image asynchronously to prevent EDT freezing
        imgLabel.setIcon(null);
        imgLabel.setText("Loading image...");
        imgLabel.setForeground(FoodSenseTheme.getTextMuted());
        imgLabel.setVisible(true);

        String imgUrl = p.getImage_front_url();
        if (imgUrl != null && !imgUrl.isBlank()) {
            SafeImageLoader.loadImageAsync(imgUrl, 180, 180, icon -> {
                if (currentReq != imageRequestId) {
                    return;
                }
                if (icon != null) {
                    imgLabel.setIcon(icon);
                    imgLabel.setText(null);
                } else {
                    imgLabel.setIcon(null);
                    imgLabel.setText("No image preview");
                }
            });
        } else {
            imgLabel.setIcon(null);
            imgLabel.setText("No image preview");
        }

        String safeName = SecurityValidator.escapeHtml(p.getProduct_name() != null ? p.getProduct_name() : "Unknown product");
        nameLabel.setText("<html><body style='width: 210px;'>" + safeName + "</body></html>");

        brandLabel.putClientProperty("html.disable", Boolean.TRUE);
        brandLabel.setText(p.getBrands() != null ? p.getBrands() : "");

        nutriScoreView.setGrade(p.getNutriscore_grade());

        nameLabel.setVisible(true);
        brandLabel.setVisible(true);
        nutriScoreView.setVisible(true);
        productCard.revalidate();
        productCard.repaint();
    }

    private void swapContent(JPanel next) {
        root.remove(contentArea);
        contentArea = next;
        root.add(contentArea, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    private void showBanner(String message) {
        bannerLabel.setText(message);
        bannerLabel.setVisible(true);
    }

    private void clearBanner() {
        bannerLabel.setText("");
        bannerLabel.setVisible(false);
    }
}
