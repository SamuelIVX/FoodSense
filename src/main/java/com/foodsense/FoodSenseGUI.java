/*
 * Swing UI and Open Food Facts client for FoodSense.
 * Builds the sidebar + product view, runs barcode search on a background thread,
 * and marshals UI updates onto the EDT via {@link SwingUtilities#invokeLater}.
 * Hits the staging host {@code world.openfoodfacts.net} (not {@code .org}).
 */

package com.foodsense;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import com.google.gson.Gson;

/**
 * Main application window: manual barcode entry, webcam scan handoff, and nutrition display.
 */
public class FoodSenseGUI {

    // ── Palette ───────────────────────────────────────────────
    private static final Color SIDEBAR_BG     = new Color(18, 36, 20);
    private static final Color SIDEBAR_ACCENT = new Color(30, 56, 33);
    private static final Color PRIMARY        = new Color(72, 199, 116);
    private static final Color PRIMARY_DARK   = new Color(52, 168, 83);
    private static final Color BG             = new Color(246, 248, 250);
    private static final Color CARD           = Color.WHITE;
    private static final Color BORDER         = new Color(226, 232, 240);
    private static final Color TEXT           = new Color(17, 24, 39);
    private static final Color MUTED          = new Color(107, 114, 128);
    private static final Color DANGER         = new Color(220, 38, 38);
    private static final Color DANGER_BG      = new Color(254, 242, 242);
    private static final String F             = "Segoe UI";

    // ── State ─────────────────────────────────────────────────
    private JFrame     frame;
    private JPanel     root;
    private JPanel     contentArea;
    private JTextField searchField;
    private JButton    searchBtn;
    private JButton    cameraBtn;

    // Sidebar product widgets (shown after a successful search)
    private JLabel imgLabel;
    private JLabel nameLabel;
    private JLabel brandLabel;
    private JPanel scorePanel;

    /**
     * Constructs the main frame and widget tree (not yet visible).
     * Call {@link #start()} to show the window.
     *
     * @example
     * <pre>{@code
     * FoodSenseGUI gui = new FoodSenseGUI();
     * gui.start();
     * }</pre>
     */
    public FoodSenseGUI() {
        build();
    }

    /**
     * Makes the main frame visible. Must be called after construction on the EDT
     * (or before any other Swing work if this is the first Swing touch in the process).
     *
     * @example
     * <pre>{@code
     * new FoodSenseGUI().start();
     * }</pre>
     */
    public void start() {
        frame.setVisible(true);
    }

    // ── Frame ─────────────────────────────────────────────────

    /** Creates the JFrame, sidebar, and empty-state content area. */
    private void build() {
        frame = new JFrame("FoodSense");
        frame.setSize(1100, 760);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildSidebar(), BorderLayout.WEST);

        contentArea = buildEmptyState();
        root.add(contentArea, BorderLayout.CENTER);

        frame.setContentPane(root);
    }

    // ── Sidebar ───────────────────────────────────────────────

    /**
     * Builds the dark-green left rail: logo, barcode field, Search/Scan buttons, product card.
     *
     * @return sidebar panel for {@link BorderLayout#WEST}
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(270, 0));

        // Top: logo + search controls
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(SIDEBAR_BG);
        top.setBorder(BorderFactory.createEmptyBorder(28, 20, 20, 20));

        JLabel logo = new JLabel("FoodSense");
        logo.setFont(new Font(F, Font.BOLD, 22));
        logo.setForeground(PRIMARY);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Nutrition at a glance");
        tagline.setFont(new Font(F, Font.PLAIN, 12));
        tagline.setForeground(new Color(100, 150, 100));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(logo);
        top.add(Box.createVerticalStrut(3));
        top.add(tagline);
        top.add(Box.createVerticalStrut(24));
        top.add(mkSeparator());
        top.add(Box.createVerticalStrut(20));

        JLabel barcodeLabel = new JLabel("BARCODE");
        barcodeLabel.setFont(new Font(F, Font.BOLD, 10));
        barcodeLabel.setForeground(new Color(90, 130, 90));
        barcodeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(barcodeLabel);
        top.add(Box.createVerticalStrut(7));

        searchField = new JTextField();
        searchField.setFont(new Font(F, Font.PLAIN, 14));
        searchField.setBackground(SIDEBAR_ACCENT);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 85, 55), 1),
                BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(searchField);
        top.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchBtn = mkBtn("Search", true);
        cameraBtn = mkBtn("Scan", false);
        btnRow.add(searchBtn);
        btnRow.add(cameraBtn);
        top.add(btnRow);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(buildSidebarProductCard(), BorderLayout.CENTER);

        searchField.addActionListener(e -> searchProduct());
        searchBtn.addActionListener(e -> searchProduct());
        cameraBtn.addActionListener(e -> startBarcodeScanner());

        return sidebar;
    }

    /**
     * Sidebar widgets for product image, name, brand, and Nutri-Score (hidden until a lookup succeeds).
     *
     * @return center section of the sidebar
     */
    private JPanel buildSidebarProductCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SIDEBAR_BG);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        card.add(mkSeparator());
        card.add(Box.createVerticalStrut(20));

        imgLabel = new JLabel();
        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        imgLabel.setVisible(false);
        card.add(imgLabel);
        card.add(Box.createVerticalStrut(14));

        nameLabel = new JLabel();
        nameLabel.setFont(new Font(F, Font.BOLD, 13));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setVisible(false);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));

        brandLabel = new JLabel();
        brandLabel.setFont(new Font(F, Font.PLAIN, 12));
        brandLabel.setForeground(new Color(110, 160, 110));
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandLabel.setVisible(false);
        card.add(brandLabel);
        card.add(Box.createVerticalStrut(10));

        scorePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        scorePanel.setOpaque(false);
        scorePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scorePanel.setVisible(false);
        card.add(scorePanel);

        return card;
    }

    /**
     * @param text    button label
     * @param primary if true, filled primary green; otherwise muted accent style
     * @return styled {@link JButton} with hover background swap
     */
    private JButton mkBtn(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(new Font(F, Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color base  = primary ? PRIMARY : SIDEBAR_ACCENT;
        Color hover = primary ? PRIMARY_DARK : new Color(42, 72, 45);
        b.setBackground(base);
        b.setForeground(primary ? Color.WHITE : new Color(170, 210, 170));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { b.setBackground(base); }
        });
        return b;
    }

    /** @return thin separator tinted to {@link #SIDEBAR_ACCENT} */
    private JSeparator mkSeparator() {
        JSeparator s = new JSeparator();
        s.setForeground(SIDEBAR_ACCENT);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    // ── Sidebar product update ────────────────────────────────

    /**
     * Fills sidebar image/name/brand/Nutri-Score from a looked-up product.
     * Loads {@link Product#getImage_front_url()} over the network on the calling thread.
     * Must run on the EDT.
     *
     * @param p product to display; image URL may be missing
     */
    private void updateSidebar(Product p) {
        try {
            if (p.getImage_front_url() != null && !p.getImage_front_url().isEmpty()) {
                ImageIcon raw = new ImageIcon(new URI(p.getImage_front_url()).toURL());
                Image scaled  = raw.getImage().getScaledInstance(170, 190, Image.SCALE_SMOOTH);
                imgLabel.setIcon(new ImageIcon(scaled));
                imgLabel.setText(null);
            } else {
                imgLabel.setIcon(null);
                imgLabel.setText("No image");
                imgLabel.setForeground(MUTED);
            }
        } catch (Exception ex) {
            imgLabel.setIcon(null);
            imgLabel.setText("No image");
            imgLabel.setForeground(MUTED);
        }

        String name = p.getProduct_name() != null ? p.getProduct_name() : "Unknown product";
        nameLabel.setText("<html><div style='width:210px'>" + name + "</div></html>");
        brandLabel.setText(p.getBrands() != null ? p.getBrands() : "");

        scorePanel.removeAll();
        if (p.getNutriscore_grade() != null && !p.getNutriscore_grade().isBlank()) {
            JLabel lbl = new JLabel("Nutri-Score");
            lbl.setFont(new Font(F, Font.PLAIN, 11));
            lbl.setForeground(new Color(110, 160, 110));
            scorePanel.add(lbl);
            scorePanel.add(mkNutriscoreBadge(p.getNutriscore_grade(), 12));
        }

        imgLabel.setVisible(true);
        nameLabel.setVisible(true);
        brandLabel.setVisible(true);
        scorePanel.setVisible(true);
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    // ── Empty state ───────────────────────────────────────────

    /**
     * Placeholder shown before any successful product lookup.
     *
     * @return center content panel
     */
    private JPanel buildEmptyState() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel icon = new JLabel("🔍");
        icon.setFont(new Font(F, Font.PLAIN, 52));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("No product loaded");
        title.setFont(new Font(F, Font.BOLD, 20));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("<html><div align='center'>Enter a barcode on the left or use your camera to scan.</div></html>");
        hint.setFont(new Font(F, Font.PLAIN, 14));
        hint.setForeground(MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(title);
        inner.add(Box.createVerticalStrut(8));
        inner.add(hint);

        p.add(inner);
        return p;
    }

    // ── Product view ──────────────────────────────────────────

    /**
     * Scrollable nutrition / ingredients / allergens view for a product.
     *
     * @param p product with optional nutriments and allergen text
     * @return content panel to swap into the center region
     */
    private JPanel buildProductView(Product p) {
        JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBackground(BG);
        sections.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        sections.add(buildNutritionSection(p.getNutriments()));
        sections.add(Box.createVerticalStrut(24));
        sections.add(buildIngredientsSection(p));

        if (p.getAllergens_from_ingredients() != null && !p.getAllergens_from_ingredients().isBlank()) {
            sections.add(Box.createVerticalStrut(24));
            sections.add(buildAllergensSection(p));
        }

        // Wrap in BorderLayout.NORTH so BoxLayout uses preferred height, not stretched height
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.add(sections, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(BG);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    // ── Sections ──────────────────────────────────────────────

    /**
     * @param n nutriments DTO (may contain null fields)
     * @return "Nutrition Facts" section panel
     */
    private JPanel buildNutritionSection(Nutriments n) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(mkSectionTitle("Nutrition Facts", "per 100g"));
        section.add(Box.createVerticalStrut(12));
        section.add(buildNutritionLabel(n));
        return section;
    }

    /**
     * Builds the bordered per-100g nutrition grid rows.
     *
     * @param n nutriments source; null field values render as an em dash
     * @return card panel of nutrient rows
     */
    private JPanel buildNutritionLabel(Nutriments n) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Object[][] rows = {
            { "Energy",              n.getEnergy(),        "kcal", false },
            { "Carbohydrates",       n.getCarbohydrates(), "g",    false },
            { "of which Sugars",     n.getSugars(),        "g",    true  },
            { "Fat",                 n.getFat(),           "g",    false },
            { "of which Saturates",  n.getSaturated_fat(), "g",    true  },
            { "Proteins",            n.getProteins(),      "g",    false },
            { "Salt",                n.getSalt(),          "g",    false },
            { "Sodium",              n.getSodium(),        "mg",   false },
        };

        boolean first = true;
        for (Object[] r : rows) {
            if (!first) {
                JSeparator sep = new JSeparator();
                sep.setForeground(BORDER);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                card.add(sep);
            }
            card.add(mkNutritionRow((String) r[0], (String) r[1], (String) r[2], (boolean) r[3]));
            first = false;
        }
        return card;
    }

    /**
     * @param name  left-side nutrient label
     * @param value amount string, or blank/{@code null} for "—"
     * @param unit  unit suffix (e.g. {@code g}, {@code kcal})
     * @param sub   if true, indented muted "of which …" style row
     * @return single nutrition row panel
     */
    private JPanel mkNutritionRow(String name, String value, String unit, boolean sub) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(sub ? new Color(250, 252, 250) : CARD);
        row.setBorder(BorderFactory.createEmptyBorder(11, sub ? 28 : 16, 11, 16));

        JLabel left = new JLabel(name);
        left.setFont(new Font(F, sub ? Font.PLAIN : Font.BOLD, sub ? 13 : 14));
        left.setForeground(sub ? MUTED : TEXT);

        String v = (value != null && !value.isBlank()) ? value : "—";
        JLabel right = new JLabel(v + " " + unit);
        right.setFont(new Font(F, Font.BOLD, 14));
        right.setForeground(sub ? MUTED : PRIMARY_DARK);

        row.add(left,  BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /**
     * @param p product whose {@code ingredients_text} is shown (fallback copy if blank)
     * @return ingredients section panel
     */
    private JPanel buildIngredientsSection(Product p) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(mkSectionTitle("Ingredients", null));
        section.add(Box.createVerticalStrut(12));

        String text = (p.getIngredients_text() != null && !p.getIngredients_text().isBlank())
                ? p.getIngredients_text()
                : "No ingredients information available.";

        JTextArea area = new JTextArea(text);
        area.setFont(new Font(F, Font.PLAIN, 13));
        area.setForeground(TEXT);
        area.setBackground(CARD);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(area);
        return section;
    }

    /**
     * Danger-styled allergens panel. Caller should only invoke when allergen text is present.
     *
     * @param p product with non-blank {@link Product#getAllergens_from_ingredients()}
     * @return allergens section panel
     */
    private JPanel buildAllergensSection(Product p) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("⚠ Allergens");
        header.setFont(new Font(F, Font.BOLD, 18));
        header.setForeground(DANGER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(header);

        JTextArea area = new JTextArea(p.getAllergens_from_ingredients());
        area.setFont(new Font(F, Font.PLAIN, 13));
        area.setForeground(DANGER);
        area.setBackground(DANGER_BG);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(252, 165, 165), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(area);
        return section;
    }

    // ── Shared helpers ────────────────────────────────────────

    /**
     * @param title    section heading
     * @param subtitle optional muted line under the title; {@code null} to omit
     * @return title block panel
     */
    private JPanel mkSectionTitle(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel t = new JLabel(title);
        t.setFont(new Font(F, Font.BOLD, 18));
        t.setForeground(TEXT);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);

        if (subtitle != null) {
            JLabel s = new JLabel(subtitle);
            s.setFont(new Font(F, Font.PLAIN, 12));
            s.setForeground(MUTED);
            s.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(s);
        }
        return p;
    }

    /**
     * @param grade    Nutri-Score letter (case-insensitive)
     * @param fontSize badge font size in points
     * @return opaque colored badge label
     */
    private JLabel mkNutriscoreBadge(String grade, int fontSize) {
        JLabel badge = new JLabel(grade.toUpperCase());
        badge.setFont(new Font(F, Font.BOLD, fontSize));
        badge.setForeground(Color.WHITE);
        badge.setBackground(getNutriscoreColor(grade));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return badge;
    }

    /**
     * Maps Nutri-Score A–E to badge background colors; unknown grades use {@link #MUTED}.
     *
     * @param grade Nutri-Score letter
     * @return background color for the badge
     */
    private Color getNutriscoreColor(String grade) {
        return switch (grade.toUpperCase()) {
            case "A" -> new Color(0, 150, 57);
            case "B" -> new Color(133, 192, 66);
            case "C" -> new Color(255, 210, 0);
            case "D" -> new Color(255, 130, 0);
            case "E" -> new Color(220, 38, 38);
            default  -> MUTED;
        };
    }

    // ── Logic ─────────────────────────────────────────────────

    /**
     * Opens the webcam scan pipeline; on detect, fills the search field and runs {@link #searchProduct()}
     * on the EDT.
     */
    private void startBarcodeScanner() {
        VideoProcessor vp = new VideoProcessor(barcode -> SwingUtilities.invokeLater(() -> {
            searchField.setText(barcode);
            searchProduct();
        }));
        vp.start();
    }

    /**
     * Reads the barcode field, disables controls, fetches on a background thread, then updates the UI
     * on the EDT. Shows a warning dialog for empty input or a failed/not-found lookup.
     */
    private void searchProduct() {
        String barcode = searchField.getText().trim();
        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a barcode.", "Empty Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        searchBtn.setEnabled(false);
        cameraBtn.setEnabled(false);
        searchBtn.setText("...");

        new Thread(() -> {
            Product product = fetchProductFromAPI(barcode);
            SwingUtilities.invokeLater(() -> {
                if (product != null) {
                    updateSidebar(product);
                    swapContent(buildProductView(product));
                } else {
                    JOptionPane.showMessageDialog(frame,
                            "Product not found. Check the barcode and try again.",
                            "Not Found", JOptionPane.WARNING_MESSAGE);
                }
                searchBtn.setEnabled(true);
                cameraBtn.setEnabled(true);
                searchBtn.setText("Search");
            });
        }).start();
    }

    /**
     * Replaces the center content panel. Must run on the EDT.
     *
     * @param next panel to show (empty state or product view)
     */
    private void swapContent(JPanel next) {
        root.remove(contentArea);
        contentArea = next;
        root.add(contentArea, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }

    /**
     * GETs {@code https://world.openfoodfacts.net/api/v2/product/{barcode}} and deserializes with Gson.
     * Network I/O — call off the EDT. Failures are swallowed (logged to stderr) and return {@code null}.
     *
     * @param barcode product barcode (path segment; not URL-encoded here)
     * @return product when {@code status != 0}; {@code null} when not found or on any failure
     */
    private Product fetchProductFromAPI(String barcode) {
        try {
            Gson gson = new Gson();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://world.openfoodfacts.net/api/v2/product/" + barcode))
                    .GET()
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            ApiResponse api = gson.fromJson(resp.body(), ApiResponse.class);
            return api.getStatus() == 0 ? null : api.getProduct();
        } catch (Exception e) {
            System.err.println("API Error: " + e.getMessage());
            return null;
        }
    }
}
