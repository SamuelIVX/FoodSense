/*
 * Structured ingredients panel for FoodSense.
 * Parses raw ingredient paragraphs into discrete, readable chips with allergen highlights.
 */

package com.foodsense.ui;

import com.foodsense.security.SecurityValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Modern ingredients card with discrete tags/chips, allergen highlighting,
 * item count badge, and a toggle between chip flow and original text.
 */
public class IngredientsPanel extends JPanel {

    private final String rawText;
    private final String allergensText;
    private final List<String> parsedIngredients;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel bodyContainer = new JPanel(cardLayout);
    private boolean showingChips = true;
    private JButton toggleBtn;

    /**
     * Constructs the ingredients panel for a given ingredients string and allergen list.
     *
     * @param rawText       raw ingredients text from Open Food Facts API
     * @param allergensText allergens text from API used to highlight suspect ingredients
     */
    public IngredientsPanel(String rawText, String allergensText) {
        this.rawText = rawText != null ? rawText.trim() : "";
        this.allergensText = allergensText != null ? allergensText.toLowerCase(Locale.ROOT) : "";
        this.parsedIngredients = parseIngredients(this.rawText);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(buildCard());
    }

    private JPanel buildCard() {
        JPanel card = new JPanel() {
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
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header Row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("Ingredients");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 16f));
        title.setForeground(FoodSenseTheme.getTextPrimary());
        titleBlock.add(title);

        if (!parsedIngredients.isEmpty()) {
            JLabel countBadge = new JLabel(parsedIngredients.size() + " items");
            countBadge.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
            countBadge.setForeground(FoodSenseTheme.getPrimaryAccent());
            countBadge.setBackground(FoodSenseTheme.getPrimaryLight());
            countBadge.setOpaque(true);
            countBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            titleBlock.add(countBadge);
        }
        headerRow.add(titleBlock, BorderLayout.WEST);

        if (!rawText.isEmpty()) {
            toggleBtn = new JButton("View Text");
            toggleBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 11f));
            toggleBtn.setFocusPainted(false);
            toggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggleBtn.putClientProperty("JButton.buttonType", "roundRect");
            toggleBtn.addActionListener(e -> {
                showingChips = !showingChips;
                cardLayout.show(bodyContainer, showingChips ? "CHIPS" : "TEXT");
                toggleBtn.setText(showingChips ? "View Text" : "View Chips");
            });
            headerRow.add(toggleBtn, BorderLayout.EAST);
        }

        card.add(headerRow);
        card.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_12));

        // Body Panes
        bodyContainer.setOpaque(false);

        if (rawText.isEmpty() || parsedIngredients.isEmpty()) {
            JLabel emptyLabel = new JLabel("No ingredients listed for this product.");
            emptyLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
            emptyLabel.setForeground(FoodSenseTheme.getTextMuted());
            bodyContainer.add(emptyLabel, "EMPTY");
        } else {
            bodyContainer.add(buildChipsView(), "CHIPS");
            bodyContainer.add(buildTextView(), "TEXT");
        }

        card.add(bodyContainer);
        return card;
    }

    private JPanel buildChipsView() {
        JPanel chipsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        chipsPanel.setOpaque(false);

        for (String ingredient : parsedIngredients) {
            chipsPanel.add(createIngredientChip(ingredient));
        }

        return chipsPanel;
    }

    private JPanel buildTextView() {
        JTextArea area = new JTextArea(rawText) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        area.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        area.setForeground(FoodSenseTheme.getTextSecondary());
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(area, BorderLayout.CENTER);
        return p;
    }

    private JComponent createIngredientChip(String text) {
        boolean isAllergen = isSuspectAllergen(text);

        JPanel chip = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isAllergen ? FoodSenseTheme.getDangerBackground() : FoodSenseTheme.getChipBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(isAllergen ? FoodSenseTheme.getDangerBorder() : FoodSenseTheme.getChipBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        chip.setOpaque(false);

        String displayText = (isAllergen ? "⚠️ " : "") + text;
        JLabel lbl = new JLabel(displayText);
        lbl.putClientProperty("html.disable", Boolean.TRUE);
        lbl.setFont(FoodSenseTheme.getFont(isAllergen ? Font.BOLD : Font.PLAIN, 11.5f));
        lbl.setForeground(isAllergen ? FoodSenseTheme.getDanger() : FoodSenseTheme.getChipText());
        chip.add(lbl);

        if (text.length() > 30) {
            chip.setToolTipText(text);
        }

        return chip;
    }

    private boolean isSuspectAllergen(String ingredient) {
        if (allergensText.isEmpty() || ingredient == null) {
            return false;
        }
        String lowerIng = ingredient.toLowerCase(Locale.ROOT);
        String[] allergens = allergensText.split("[,;]");
        for (String a : allergens) {
            String trimmed = a.trim();
            if (!trimmed.isEmpty() && lowerIng.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses an ingredients string into individual ingredient tokens, preserving
     * sub-ingredients inside parentheses or brackets.
     *
     * @param raw raw ingredients string
     * @return list of parsed discrete ingredients
     */
    public static List<String> parseIngredients(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        List<String> items = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth = Math.max(0, parenDepth - 1);
            } else if (c == '[') {
                bracketDepth++;
            } else if (c == ']') {
                bracketDepth = Math.max(0, bracketDepth - 1);
            }

            // Split when outside parentheses/brackets on comma, semicolon, bullet, or newline
            if ((c == ',' || c == ';' || c == '•' || c == '\n') && parenDepth == 0 && bracketDepth == 0) {
                String token = cleanIngredientToken(current.toString());
                if (!token.isEmpty()) {
                    items.add(token);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String last = cleanIngredientToken(current.toString());
        if (!last.isEmpty()) {
            items.add(last);
        }

        return items;
    }

    private static String cleanIngredientToken(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        while (trimmed.endsWith(".") || trimmed.endsWith(";") || trimmed.endsWith(",")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        while (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed;
    }
}
