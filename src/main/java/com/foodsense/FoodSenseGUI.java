/*
 * Swing UI and Open Food Facts desktop client for FoodSense.
 * Builds the slim sidebar and responsive dashboard product view.
 */

package com.foodsense;

import com.foodsense.security.SafeImageLoader;
import com.foodsense.security.SecurityValidator;
import com.foodsense.ui.AllergenBanner;
import com.foodsense.ui.CameraScanDialog;
import com.foodsense.ui.FoodSenseTheme;
import com.foodsense.ui.IngredientsPanel;
import com.foodsense.ui.NutriScoreSpectrumView;
import com.foodsense.ui.NutritionLabelPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Main application window: manual barcode entry, in-app webcam scan handoff,
 * theme toggle, and responsive dashboard nutrition display.
 */
public class FoodSenseGUI {

    private final ProductApiClient apiClient;
    private JFrame frame;
    private JPanel root;
    private JPanel contentArea;

    // Sidebar references for instant in-place theme recoloring
    private JPanel sidebar;
    private JLabel logo;
    private JLabel tagline;
    private JLabel barcodeLabel;
    private JTextField searchField;
    private JButton searchBtn;
    private JButton cameraBtn;
    private JButton themeToggleBtn;
    private JLabel bannerLabel;

    // Product card widgets in sidebar
    private JPanel productCard;
    private JPanel imgCard;
    private JLabel imgLabel;
    private NutriScoreSpectrumView nutriScoreView;

    private Product currentProduct = null;
    private int imageRequestId = 0;

    /**
     * Constructs the main frame with a default API client.
     */
    public FoodSenseGUI() {
        this(new ProductApiClient());
    }

    /**
     * Constructs the main frame with a custom API client.
     *
     * @param apiClient API client instance
     */
    public FoodSenseGUI(ProductApiClient apiClient) {
        this.apiClient = apiClient != null ? apiClient : new ProductApiClient();
        FoodSenseTheme.initialize();
        build();
    }

    /**
     * Makes the main frame visible.
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

        sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        contentArea = buildEmptyState();
        root.add(contentArea, BorderLayout.CENTER);

        frame.setContentPane(root);
    }

    // ── Sidebar (Slim 240px) ──────────────────────────────────

    private JPanel buildSidebar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(FoodSenseTheme.getSidebarBackground());
        bar.setPreferredSize(new Dimension(240, 0));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, FoodSenseTheme.getCardBorder()));

        // Top controls block
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));

        // Header: Logo & Theme Toggle
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        logo = new JLabel("FoodSense");
        logo.setFont(FoodSenseTheme.getFont(Font.BOLD, 19f));
        logo.setForeground(FoodSenseTheme.getPrimaryAccent());
        titleBlock.add(logo);

        tagline = new JLabel("Nutrition Intelligence");
        tagline.setFont(FoodSenseTheme.getFont(Font.PLAIN, 11f));
        tagline.setForeground(FoodSenseTheme.getSidebarMuted());
        titleBlock.add(tagline);

        headerRow.add(titleBlock, BorderLayout.WEST);

        themeToggleBtn = new JButton(FoodSenseTheme.isDarkMode() ? "☀" : "☾");
        themeToggleBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 13f));
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

        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));

        // Input Label
        barcodeLabel = new JLabel("PRODUCT BARCODE");
        barcodeLabel.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
        barcodeLabel.setForeground(FoodSenseTheme.getSidebarMuted());
        barcodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(barcodeLabel);
        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));

        // Text Field
        searchField = new JTextField();
        searchField.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        searchField.putClientProperty("JTextField.placeholderText", "Enter barcode number");
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
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

        // Stacked Buttons
        searchBtn = new JButton("Search Barcode");
        searchBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        searchBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchBtn.addActionListener(e -> searchProduct());
        top.add(searchBtn);

        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));

        cameraBtn = new JButton("Scan with Camera");
        cameraBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
        cameraBtn.setFocusPainted(false);
        cameraBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cameraBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        cameraBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        cameraBtn.addActionListener(e -> openCameraModal());
        top.add(cameraBtn);

        // Status banner
        top.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));
        bannerLabel = new JLabel("");
        bannerLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 11f));
        bannerLabel.setForeground(FoodSenseTheme.getDanger());
        bannerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bannerLabel.setVisible(false);
        top.add(bannerLabel);

        bar.add(top, BorderLayout.NORTH);
        bar.add(buildSidebarProductCard(), BorderLayout.CENTER);

        return bar;
    }

    private JPanel buildSidebarProductCard() {
        productCard = new JPanel();
        productCard.setLayout(new BoxLayout(productCard, BoxLayout.Y_AXIS));
        productCard.setOpaque(false);
        productCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 16));

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        productCard.add(sep);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));

        // Styled image frame card
        imgCard = new JPanel(new BorderLayout()) {
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
        imgCard.setOpaque(false);
        imgCard.setPreferredSize(new Dimension(150, 150));
        imgCard.setMaximumSize(new Dimension(150, 150));
        imgCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        imgLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 11f));
        imgLabel.setForeground(FoodSenseTheme.getTextMuted());
        imgCard.add(imgLabel, BorderLayout.CENTER);
        imgCard.setVisible(false);

        productCard.add(imgCard);
        productCard.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));

        nutriScoreView = new NutriScoreSpectrumView();
        nutriScoreView.setAlignmentX(Component.CENTER_ALIGNMENT);
        nutriScoreView.setVisible(false);
        productCard.add(nutriScoreView);

        return productCard;
    }

    /**
     * Reapplies theme colors to sidebar components in-place without restarting image fetch.
     */
    private void applySidebarTheme() {
        sidebar.setBackground(FoodSenseTheme.getSidebarBackground());
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, FoodSenseTheme.getCardBorder()));
        logo.setForeground(FoodSenseTheme.getPrimaryAccent());
        tagline.setForeground(FoodSenseTheme.getSidebarMuted());
        barcodeLabel.setForeground(FoodSenseTheme.getSidebarMuted());
        bannerLabel.setForeground(FoodSenseTheme.getDanger());
        imgLabel.setForeground(FoodSenseTheme.getTextMuted());
        nutriScoreView.repaint();
        sidebar.revalidate();
        sidebar.repaint();
    }

    private void updateComponentColors() {
        root.setBackground(FoodSenseTheme.getBackground());
        applySidebarTheme();
        if (currentProduct != null) {
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
        icon.setFont(FoodSenseTheme.getFont(Font.PLAIN, 48f));
        icon.setForeground(FoodSenseTheme.getTextMuted());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Ready to Scan");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 18f));
        title.setForeground(FoodSenseTheme.getTextPrimary());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Enter a barcode on the left or scan live with your camera.");
        hint.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        hint.setForeground(FoodSenseTheme.getTextMuted());
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_8));
        inner.add(title);
        inner.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));
        inner.add(hint);

        p.add(inner);
        return p;
    }

    /**
     * Builds a balanced, two-column responsive dashboard that fills the window
     * and eliminates blank spaces and horizontal scrollbars.
     */
    private JPanel buildProductView(Product p) {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(FoodSenseTheme.getBackground());
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Top Header Banner inside Main Content
        JPanel headerCard = new JPanel(new BorderLayout()) {
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
        headerCard.setOpaque(false);
        headerCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setOpaque(false);

        String titleText = p.getProduct_name() != null ? p.getProduct_name() : "Unknown Product";
        JLabel prodTitle = new JLabel(titleText);
        prodTitle.putClientProperty("html.disable", Boolean.TRUE);
        prodTitle.setFont(FoodSenseTheme.getFont(Font.BOLD, 22f));
        prodTitle.setForeground(FoodSenseTheme.getTextPrimary());
        headerLeft.add(prodTitle);
        headerLeft.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));

        String brandText = p.getBrands() != null && !p.getBrands().isBlank() ? p.getBrands() : "Generic / Unbranded";
        JLabel prodBrand = new JLabel("<html><span style='color:" + toHex(FoodSenseTheme.getTextMuted()) + ";'>Brand: </span><b><i>"
                + SecurityValidator.escapeHtml(brandText) + "</i></b></html>");
        prodBrand.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12.5f));
        prodBrand.setForeground(FoodSenseTheme.getTextSecondary());
        headerLeft.add(prodBrand);

        headerCard.add(headerLeft, BorderLayout.WEST);

        // Header right Nutri-Score badge
        if (p.getNutriscore_grade() != null && p.getNutriscore_grade().trim().matches("(?i)[a-e]")) {
            String g = p.getNutriscore_grade().trim().toUpperCase();
            JLabel gradeBadge = new JLabel(" Nutri-Score Grade " + g + " ");
            gradeBadge.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
            gradeBadge.setForeground(Color.WHITE);
            gradeBadge.setBackground(FoodSenseTheme.getNutriscoreColor(g));
            gradeBadge.setOpaque(true);
            gradeBadge.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
            badgeWrapper.setOpaque(false);
            badgeWrapper.add(gradeBadge);
            headerCard.add(badgeWrapper, BorderLayout.EAST);
        }

        main.add(headerCard, BorderLayout.NORTH);

        // 2-Column Dashboard Center
        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Column 1: Nutrition Profile
        columns.add(new NutritionLabelPanel(p.getNutriments()));

        // Column 2: Ingredients & Allergens
        JPanel col2 = new JPanel();
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        col2.setOpaque(false);

        col2.add(new IngredientsPanel(p.getIngredients_text(), p.getAllergens_from_ingredients()));

        if (p.getAllergens_from_ingredients() != null && !p.getAllergens_from_ingredients().isBlank()) {
            col2.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
            col2.add(new AllergenBanner(p.getAllergens_from_ingredients()));
        }

        columns.add(col2);
        main.add(columns, BorderLayout.CENTER);

        // ScrollPane with horizontal scrollbar disabled
        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(FoodSenseTheme.getBackground());

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(FoodSenseTheme.getBackground());
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    // ── Search & Logic ────────────────────────────────────────

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
            showBanner("Invalid barcode (3-48 alphanumeric characters, hyphens, or underscores).");
            return;
        }

        searchBtn.setEnabled(false);
        cameraBtn.setEnabled(false);
        searchBtn.setText("Searching...");

        new Thread(() -> {
            Product product = apiClient.fetchProduct(barcode);
            SwingUtilities.invokeLater(() -> {
                try {
                    if (product != null) {
                        this.currentProduct = product;
                        updateSidebar(product);
                        swapContent(buildProductView(product));
                    } else {
                        showBanner("Product not found. Verify code.");
                    }
                } finally {
                    searchBtn.setEnabled(true);
                    cameraBtn.setEnabled(true);
                    searchBtn.setText("Search Barcode");
                }
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

        imgCard.setVisible(true);
        imgLabel.setIcon(null);
        imgLabel.setText("Loading...");

        String imgUrl = p.getImage_front_url();
        if (imgUrl != null && !imgUrl.isBlank()) {
            SafeImageLoader.loadImageAsync(imgUrl, 140, 140, icon -> {
                if (currentReq != imageRequestId) {
                    return;
                }
                if (icon != null) {
                    imgLabel.setIcon(icon);
                    imgLabel.setText(null);
                } else {
                    imgLabel.setIcon(null);
                    imgLabel.setText("No photo");
                }
            });
        } else {
            imgLabel.setIcon(null);
            imgLabel.setText("No photo");
        }

        nutriScoreView.setGrade(p.getNutriscore_grade());
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
        bannerLabel.setText("<html><div style='width: 190px;'>"
                + SecurityValidator.escapeHtml(message) + "</div></html>");
        bannerLabel.setVisible(true);
    }

    private void clearBanner() {
        bannerLabel.setText("");
        bannerLabel.setVisible(false);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
