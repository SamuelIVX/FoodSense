/*
 * Structured nutrition card for FoodSense.
 * Displays hero calorie metric, macronutrient breakdown bar, and tabular nutrient details.
 */

package com.foodsense.ui;

import com.foodsense.Nutriments;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern nutrition panel featuring hero calories, macro visual proportion bar, and per-100g table.
 */
public class NutritionLabelPanel extends JPanel {

    public NutritionLabelPanel(Nutriments nutriments) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        Nutriments n = nutriments != null ? nutriments : new Nutriments();

        add(buildHeader());
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        add(buildHeroCalories(n.getEnergy()));
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        add(buildMacroBar(n));
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        add(buildNutrientTable(n));
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Nutrition Facts");
        title.setFont(FoodSenseTheme.getFont(Font.BOLD, 18f));
        title.setForeground(FoodSenseTheme.getTextPrimary());
        p.add(title);

        JLabel subtitle = new JLabel("Per 100g reference amount");
        subtitle.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        subtitle.setForeground(FoodSenseTheme.getTextMuted());
        p.add(subtitle);

        return p;
    }

    private JPanel buildHeroCalories(String calories) {
        JPanel hero = new JPanel(new BorderLayout()) {
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
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel label = new JLabel("ENERGY / CALORIES");
        label.setFont(FoodSenseTheme.getFont(Font.BOLD, 11f));
        label.setForeground(FoodSenseTheme.getTextMuted());
        left.add(label);

        String val = (calories != null && !calories.isBlank()) ? calories : "—";
        JLabel num = new JLabel(val + " kcal");
        num.putClientProperty("html.disable", Boolean.TRUE);
        num.setFont(FoodSenseTheme.getFont(Font.BOLD, 26f));
        num.setForeground(FoodSenseTheme.getPrimaryAccent());
        left.add(num);

        hero.add(left, BorderLayout.WEST);
        return hero;
    }

    private JPanel buildMacroBar(Nutriments n) {
        double carbs = parseDouble(n.getCarbohydrates());
        double fat = parseDouble(n.getFat());
        double protein = parseDouble(n.getProteins());
        double total = carbs + fat + protein;

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Segmented bar
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                g2.setColor(FoodSenseTheme.getCardBorder());
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));

                if (total > 0) {
                    int carbW = (int) Math.round((carbs / total) * w);
                    int fatW = (int) Math.round((fat / total) * w);
                    int proteinW = w - carbW - fatW;

                    int curX = 0;
                    // Carbs (Amber)
                    g2.setColor(new Color(245, 158, 11));
                    g2.fill(new RoundRectangle2D.Float(curX, 0, carbW, h, 8, 8));
                    curX += carbW;

                    // Fat (Rose)
                    g2.setColor(new Color(244, 63, 94));
                    g2.fillRect(curX, 0, fatW, h);
                    curX += fatW;

                    // Protein (Emerald)
                    g2.setColor(new Color(16, 185, 129));
                    g2.fill(new RoundRectangle2D.Float(curX, 0, proteinW, h, 8, 8));
                }
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 10));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        bar.setOpaque(false);
        container.add(bar);
        container.add(Box.createVerticalStrut(6));

        // Legend row
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        legend.add(mkLegendItem("Carbs: " + fmt(carbs) + "g", new Color(245, 158, 11)));
        legend.add(mkLegendItem("Fat: " + fmt(fat) + "g", new Color(244, 63, 94)));
        legend.add(mkLegendItem("Protein: " + fmt(protein) + "g", new Color(16, 185, 129)));
        container.add(legend);

        return container;
    }

    private JLabel mkLegendItem(String text, Color color) {
        JLabel l = new JLabel("● " + text);
        l.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        l.setForeground(color);
        return l;
    }

    private JPanel buildNutrientTable(Nutriments n) {
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
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        Object[][] rows = {
            { "Carbohydrates",       n.getCarbohydrates(), "g",  false },
            { "of which Sugars",     n.getSugars(),        "g",  true  },
            { "Fat",                 n.getFat(),           "g",  false },
            { "of which Saturates",  n.getSaturated_fat(), "g",  true  },
            { "Proteins",            n.getProteins(),      "g",  false },
            { "Salt",                n.getSalt(),          "g",  false },
            { "Sodium",              n.getSodium(),        "mg", false },
        };

        boolean first = true;
        for (Object[] r : rows) {
            if (!first) {
                JSeparator sep = new JSeparator();
                sep.setForeground(FoodSenseTheme.getCardBorder());
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                card.add(sep);
            }
            card.add(mkRow((String) r[0], (String) r[1], (String) r[2], (boolean) r[3]));
            first = false;
        }
        return card;
    }

    private JPanel mkRow(String name, String value, String unit, boolean sub) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, sub ? 28 : 16, 10, 16));

        JLabel left = new JLabel(name);
        left.setFont(FoodSenseTheme.getFont(sub ? Font.PLAIN : Font.BOLD, sub ? 12f : 13f));
        left.setForeground(sub ? FoodSenseTheme.getTextMuted() : FoodSenseTheme.getTextPrimary());

        String v = (value != null && !value.isBlank()) ? value : "—";
        JLabel right = new JLabel(v + " " + unit);
        right.putClientProperty("html.disable", Boolean.TRUE);
        right.setFont(FoodSenseTheme.getFont(Font.BOLD, 13f));
        right.setForeground(sub ? FoodSenseTheme.getTextMuted() : FoodSenseTheme.getTextSecondary());

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private double parseDouble(String str) {
        if (str == null) return 0.0;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String fmt(double d) {
        return String.format("%.1f", d);
    }
}
