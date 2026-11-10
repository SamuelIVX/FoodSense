/*
 * Danger-styled allergen warning banner for FoodSense.
 * Displays allergen chips/pills in a highlighted alert card.
 */

package com.foodsense.ui;

import com.foodsense.security.SecurityValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern allergen warning card displaying parsed allergen badges with clear warning iconography.
 */
public class AllergenBanner extends JPanel {

    public AllergenBanner(String allergensText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FoodSenseTheme.getDangerBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(FoodSenseTheme.getDangerBorder());
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header row
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel("⚠");
        icon.setFont(FoodSenseTheme.getFont(Font.BOLD, 18f));
        icon.setForeground(FoodSenseTheme.getDanger());
        header.add(icon);

        JLabel title = new JLabel("Allergen Warning");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 15f));
        title.setForeground(FoodSenseTheme.getDanger());
        header.add(title);

        card.add(header);
        card.add(Box.createVerticalStrut(10));

        // Details
        String safeText = SecurityValidator.escapeHtml(allergensText);
        JLabel content = new JLabel("<html><body style='width: 500px; color: " +
                toHex(FoodSenseTheme.getDanger()) + "'>" + safeText + "</body></html>");
        content.setFont(FoodSenseTheme.getFont(Font.PLAIN, 13f));
        content.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(content);

        add(card);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
