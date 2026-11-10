/*
 * Structured nutrition card for FoodSense.
 * Displays hero calorie metric, macronutrient summary cards, energy distribution ratio,
 * authentic subdivided nutrition facts table, and dietary benchmark indicators.
 */

package com.foodsense.ui;

import com.foodsense.Nutriments;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;

/**
 * Modern nutrition facts panel featuring rounded hero calories, macronutrient summary cards,
 * calorie distribution ratio bar, dietary health benchmarks, and a subdivided per-100g nutrient breakdown.
 */
public class NutritionLabelPanel extends JPanel {

    public NutritionLabelPanel(Nutriments nutriments) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        Nutriments n = nutriments != null ? nutriments : new Nutriments();

        add(buildHeader());
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        add(buildHeroCalories(n));
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        add(buildMacroCards(n));
        add(Box.createVerticalStrut(FoodSenseTheme.SPACE_12));

        JPanel ratioBar = buildMacroRatioBar(n);
        if (ratioBar != null) {
            add(ratioBar);
            add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        }

        JPanel dietaryProfile = buildDietaryBenchmarkCards(n);
        if (dietaryProfile != null) {
            add(dietaryProfile);
            add(Box.createVerticalStrut(FoodSenseTheme.SPACE_16));
        }

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

        JLabel subtitle = new JLabel("Reference amounts per 100g / 100ml");
        subtitle.setFont(FoodSenseTheme.getFont(Font.PLAIN, 12f));
        subtitle.setForeground(FoodSenseTheme.getTextMuted());
        p.add(subtitle);

        return p;
    }

    private JPanel buildHeroCalories(Nutriments n) {
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
        hero.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel label = new JLabel("ENERGY / CALORIES");
        label.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
        label.setForeground(FoodSenseTheme.getTextMuted());
        left.add(label);

        // Resolve calories using explicit Open Food Facts fields
        String kcalText = "— kcal";
        String kjText = "";

        if (n.getEnergyKcal() != null && !n.getEnergyKcal().isBlank()) {
            try {
                double val = Double.parseDouble(n.getEnergyKcal().trim());
                int kcal = (int) Math.round(val);
                kcalText = kcal + " kcal";
                if (n.getEnergyKj() != null && !n.getEnergyKj().isBlank()) {
                    int kj = (int) Math.round(Double.parseDouble(n.getEnergyKj().trim()));
                    kjText = kj + " kJ";
                } else {
                    int kj = (int) Math.round(val * 4.184);
                    kjText = kj + " kJ";
                }
            } catch (NumberFormatException ignored) {
                kcalText = n.getEnergyKcal() + " kcal";
            }
        } else if (n.getEnergyKj() != null && !n.getEnergyKj().isBlank()) {
            try {
                double val = Double.parseDouble(n.getEnergyKj().trim());
                int kj = (int) Math.round(val);
                int kcal = (int) Math.round(val / 4.184);
                kcalText = kcal + " kcal";
                kjText = kj + " kJ";
            } catch (NumberFormatException ignored) {
                kjText = n.getEnergyKj() + " kJ";
            }
        } else if (n.getEnergy() != null && !n.getEnergy().isBlank()) {
            try {
                double val = Double.parseDouble(n.getEnergy().trim());
                // Open Food Facts API spec: standard "energy" without suffix is reported in kJ
                int kj = (int) Math.round(val);
                int kcal = (int) Math.round(val / 4.184);
                kcalText = kcal + " kcal";
                kjText = kj + " kJ";
            } catch (NumberFormatException ignored) {
                kcalText = n.getEnergy() + " kcal";
            }
        }

        JLabel num = new JLabel(kcalText);
        num.putClientProperty("html.disable", Boolean.TRUE);
        num.setFont(FoodSenseTheme.getFont(Font.BOLD, 24f));
        num.setForeground(FoodSenseTheme.getPrimaryAccent());
        left.add(num);

        hero.add(left, BorderLayout.WEST);

        if (!kjText.isEmpty()) {
            JLabel rightKj = new JLabel(kjText);
            rightKj.putClientProperty("html.disable", Boolean.TRUE);
            rightKj.setFont(FoodSenseTheme.getFont(Font.BOLD, 13f));
            rightKj.setForeground(FoodSenseTheme.getTextMuted());
            hero.add(rightKj, BorderLayout.EAST);
        }

        return hero;
    }

    private JPanel buildMacroCards(Nutriments n) {
        JPanel grid = new JPanel(new GridLayout(1, 3, 10, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        String carbVal = formatRounded(n.getCarbohydrates());
        String sugarVal = formatRounded(n.getSugars());
        String fatVal = formatRounded(n.getFat());
        String satFatVal = formatRounded(n.getSaturated_fat());
        String proteinVal = formatRounded(n.getProteins());

        String carbMain = carbVal != null ? carbVal + "g" : "—";
        String sugarSub = sugarVal != null ? "Sugars " + sugarVal + "g" : "Sugars —";
        String fatMain = fatVal != null ? fatVal + "g" : "—";
        String satFatSub = satFatVal != null ? "Saturates " + satFatVal + "g" : "Saturates —";
        String proteinMain = proteinVal != null ? proteinVal + "g" : "—";

        grid.add(mkMacroChip("Carbohydrates", carbMain, sugarSub, new Color(217, 119, 6)));
        grid.add(mkMacroChip("Fats", fatMain, satFatSub, new Color(225, 29, 72)));
        grid.add(mkMacroChip("Proteins", proteinMain, "Building blocks", new Color(5, 150, 105)));

        return grid;
    }

    private JPanel mkMacroChip(String title, String mainValue, String subValue, Color accentColor) {
        JPanel chip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FoodSenseTheme.getCardBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(FoodSenseTheme.getCardBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                // Left color accent bar
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, 4, getHeight(), 4, 4));
                g2.dispose();
            }
        };
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 12));

        JLabel t = new JLabel(title);
        t.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
        t.setForeground(FoodSenseTheme.getTextMuted());
        chip.add(t);

        JLabel v = new JLabel(mainValue);
        v.putClientProperty("html.disable", Boolean.TRUE);
        v.setFont(FoodSenseTheme.getFont(Font.BOLD, 15f));
        v.setForeground(FoodSenseTheme.getTextPrimary());
        chip.add(v);

        JLabel sub = new JLabel(subValue);
        sub.setFont(FoodSenseTheme.getFont(Font.PLAIN, 10f));
        sub.setForeground(FoodSenseTheme.getTextMuted());
        chip.add(sub);

        return chip;
    }

    /**
     * Builds a segmented calorie contribution ratio bar (% Carbs, % Fat, % Protein).
     */
    private JPanel buildMacroRatioBar(Nutriments n) {
        double carbs = parseDouble(n.getCarbohydrates());
        double fat = parseDouble(n.getFat());
        double protein = parseDouble(n.getProteins());

        double carbKcal = carbs * 4.0;
        double fatKcal = fat * 9.0;
        double protKcal = protein * 4.0;
        double totalKcal = carbKcal + fatKcal + protKcal;

        if (totalKcal <= 0) {
            return null;
        }

        final int carbPct = (int) Math.round((carbKcal / totalKcal) * 100);
        final int fatPct = (int) Math.round((fatKcal / totalKcal) * 100);
        final int protPct = Math.max(0, 100 - carbPct - fatPct);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Segmented bar
        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int r = 8;

                // Clip to rounded rectangle
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);

                int carbW = (int) Math.round(w * (carbPct / 100.0));
                int fatW = (int) Math.round(w * (fatPct / 100.0));
                int protW = w - carbW - fatW;

                g2.setColor(new Color(217, 119, 6)); // Carbs amber
                g2.fillRect(0, 0, carbW, h);

                g2.setColor(new Color(225, 29, 72)); // Fat rose
                g2.fillRect(carbW, 0, fatW, h);

                g2.setColor(new Color(5, 150, 105)); // Protein emerald
                g2.fillRect(carbW + fatW, 0, protW, h);

                g2.setClip(null);
                g2.setColor(FoodSenseTheme.getCardBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, r, r));

                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, 10));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        bar.setOpaque(false);
        panel.add(bar);

        panel.add(Box.createVerticalStrut(FoodSenseTheme.SPACE_4));

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        legend.setOpaque(false);

        legend.add(mkLegendDot("Carbs " + carbPct + "%", new Color(217, 119, 6)));
        legend.add(mkLegendDot("Fat " + fatPct + "%", new Color(225, 29, 72)));
        legend.add(mkLegendDot("Protein " + protPct + "%", new Color(5, 150, 105)));

        panel.add(legend);
        return panel;
    }

    private JLabel mkLegendDot(String text, Color dotColor) {
        JLabel l = new JLabel("● " + text);
        l.setFont(FoodSenseTheme.getFont(Font.BOLD, 10f));
        l.setForeground(dotColor);
        return l;
    }

    /**
     * Builds dietary health benchmark indicators based on WHO / FDA nutritional guidelines.
     */
    private JPanel buildDietaryBenchmarkCards(Nutriments n) {
        double sugars = parseDouble(n.getSugars());
        double satFat = parseDouble(n.getSaturated_fat());
        double sodium = parseSodiumMg(n.getSodium());

        boolean hasData = n.getSugars() != null || n.getSaturated_fat() != null || n.getSodium() != null;
        if (!hasData) {
            return null;
        }

        JPanel wrapper = new JPanel(new GridLayout(1, 3, 10, 0));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Sugar Benchmark: Low <= 5g, Moderate 5-15g, High > 15g
        if (n.getSugars() != null) {
            if (sugars <= 5.0) {
                wrapper.add(mkBenchmarkPill("Sugar", "Low (" + fmt(sugars) + "g)", FoodSenseTheme.SCORE_A, "≤ 5.0g / 100g"));
            } else if (sugars <= 15.0) {
                wrapper.add(mkBenchmarkPill("Sugar", "Moderate (" + fmt(sugars) + "g)", FoodSenseTheme.SCORE_C, "5-15g / 100g"));
            } else {
                wrapper.add(mkBenchmarkPill("Sugar", "High (" + fmt(sugars) + "g)", FoodSenseTheme.SCORE_E, "> 15g / 100g"));
            }
        }

        // Saturated Fat Benchmark: Low <= 1.5g, Moderate 1.5-5.0g, High > 5.0g
        if (n.getSaturated_fat() != null) {
            if (satFat <= 1.5) {
                wrapper.add(mkBenchmarkPill("Sat. Fat", "Low (" + fmt(satFat) + "g)", FoodSenseTheme.SCORE_A, "≤ 1.5g / 100g"));
            } else if (satFat <= 5.0) {
                wrapper.add(mkBenchmarkPill("Sat. Fat", "Moderate (" + fmt(satFat) + "g)", FoodSenseTheme.SCORE_C, "1.5-5.0g / 100g"));
            } else {
                wrapper.add(mkBenchmarkPill("Sat. Fat", "High (" + fmt(satFat) + "g)", FoodSenseTheme.SCORE_E, "> 5.0g / 100g"));
            }
        }

        // Sodium Benchmark: Low <= 120mg, Moderate 120-600mg, High > 600mg
        if (n.getSodium() != null) {
            if (sodium <= 120.0) {
                wrapper.add(mkBenchmarkPill("Sodium", "Low (" + Math.round(sodium) + "mg)", FoodSenseTheme.SCORE_A, "≤ 120mg / 100g"));
            } else if (sodium <= 600.0) {
                wrapper.add(mkBenchmarkPill("Sodium", "Moderate (" + Math.round(sodium) + "mg)", FoodSenseTheme.SCORE_C, "120-600mg / 100g"));
            } else {
                wrapper.add(mkBenchmarkPill("Sodium", "High (" + Math.round(sodium) + "mg)", FoodSenseTheme.SCORE_E, "> 600mg / 100g"));
            }
        }

        return wrapper;
    }

    private JPanel mkBenchmarkPill(String metric, String status, Color statusColor, String hint) {
        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FoodSenseTheme.getCardBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(FoodSenseTheme.getCardBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 8, 8));

                // Left status indicator dot
                g2.setColor(statusColor);
                g2.fillOval(10, (getHeight() - 8) / 2, 8, 8);
                g2.dispose();
            }
        };
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createEmptyBorder(6, 24, 6, 8));
        pill.setToolTipText(metric + " reference: " + hint);

        JLabel m = new JLabel(metric.toUpperCase(Locale.ROOT));
        m.setFont(FoodSenseTheme.getFont(Font.BOLD, 9f));
        m.setForeground(FoodSenseTheme.getTextMuted());
        pill.add(m);

        JLabel s = new JLabel(status);
        s.setFont(FoodSenseTheme.getFont(Font.BOLD, 11f));
        s.setForeground(statusColor);
        pill.add(s);

        return pill;
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

        // Format values with proper rounding and units
        String carbVal = formatRounded(n.getCarbohydrates());
        String sugarVal = formatRounded(n.getSugars());
        String fatVal = formatRounded(n.getFat());
        String satFatVal = formatRounded(n.getSaturated_fat());
        String proteinVal = formatRounded(n.getProteins());
        String saltVal = formatRounded(n.getSalt());
        String sodiumVal = formatSodium(n.getSodium());

        Object[][] rows = {
            { "Total Carbohydrates", carbVal,    "g",  false },
            { "↳ of which Sugars",   sugarVal,   "g",  true  },
            { "Total Fat",            fatVal,     "g",  false },
            { "↳ of which Saturates", satFatVal,  "g",  true  },
            { "Proteins",             proteinVal, "g",  false },
            { "Salt",                 saltVal,    "g",  false },
            { "Sodium",               sodiumVal,  "mg", false },
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
        row.setBorder(BorderFactory.createEmptyBorder(9, sub ? 24 : 16, 9, 16));

        JLabel left = new JLabel(name);
        left.setFont(FoodSenseTheme.getFont(sub ? Font.ITALIC : Font.BOLD, sub ? 12f : 13f));
        left.setForeground(sub ? FoodSenseTheme.getTextMuted() : FoodSenseTheme.getTextPrimary());

        String v = (value != null && !value.isBlank()) ? value : "—";
        String displayRight = v.equals("—") ? "—" : v + " " + unit;
        JLabel right = new JLabel(displayRight);
        right.putClientProperty("html.disable", Boolean.TRUE);
        right.setFont(FoodSenseTheme.getFont(Font.BOLD, 13f));
        right.setForeground(sub ? FoodSenseTheme.getTextMuted() : FoodSenseTheme.getTextSecondary());

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private static double parseDouble(String str) {
        if (str == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String fmt(double d) {
        return String.format(Locale.US, "%.1f", d);
    }

    private static String formatRounded(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double d = Double.parseDouble(raw.trim());
            return String.format(Locale.US, "%.1f", d);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static double parseSodiumMg(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        try {
            double d = Double.parseDouble(raw.trim());
            return d * 1000.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String formatSodium(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double d = Double.parseDouble(raw.trim());
            // Open Food Facts API spec: sodium is stored in grams; convert to milligrams
            return String.format(Locale.US, "%.1f", d * 1000.0);
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
