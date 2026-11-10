/*
 * Official 5-pill Nutri-Score spectrum view (A, B, C, D, E) for FoodSense.
 * Renders the detected grade enlarged and highlighted while dimming non-active grades.
 */

package com.foodsense.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom Swing component that renders the standard 5-pill Nutri-Score spectrum.
 * Compact and proportioned to fit sidebars without clipping.
 */
public class NutriScoreSpectrumView extends JPanel {

    private static final String[] GRADES = { "A", "B", "C", "D", "E" };
    private static final Color[] COLORS = {
        FoodSenseTheme.SCORE_A,
        FoodSenseTheme.SCORE_B,
        FoodSenseTheme.SCORE_C,
        FoodSenseTheme.SCORE_D,
        FoodSenseTheme.SCORE_E
    };

    private String activeGrade = null;

    /**
     * Constructs a spectrum view with no initial grade selected.
     */
    public NutriScoreSpectrumView() {
        this(null);
    }

    /**
     * Constructs a spectrum view displaying the specified active grade.
     *
     * @param grade Nutri-Score grade letter (A-E, case-insensitive)
     */
    public NutriScoreSpectrumView(String grade) {
        setOpaque(false);
        setGrade(grade);
        setPreferredSize(new Dimension(160, 36));
        setMaximumSize(new Dimension(180, 40));
    }

    /**
     * Updates the active grade and repaints the spectrum.
     *
     * @param grade letter grade (A-E), or null for neutral spectrum
     */
    public void setGrade(String grade) {
        String normalized = grade != null ? grade.trim().toUpperCase(java.util.Locale.ROOT) : "";
        this.activeGrade = normalized.matches("[A-E]") ? normalized : null;
        setToolTipText(this.activeGrade != null ? "Nutri-Score: Grade " + this.activeGrade : "Nutri-Score not available");
        repaint();
    }

    /**
     * Returns the currently displayed Nutri-Score grade.
     *
     * @return uppercase grade string, or {@code null}
     */
    public String getGrade() {
        return activeGrade;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int totalWidth = getWidth();
        int totalHeight = getHeight();
        int pillCount = GRADES.length;

        int standardW = 24;
        int standardH = 24;
        int activeW = 30;
        int activeH = 32;
        int gap = 3;

        int activeIndex = -1;
        if (activeGrade != null) {
            for (int i = 0; i < GRADES.length; i++) {
                if (GRADES[i].equalsIgnoreCase(activeGrade)) {
                    activeIndex = i;
                    break;
                }
            }
        }

        int requiredWidth = (pillCount - (activeIndex >= 0 ? 1 : 0)) * standardW
                + (activeIndex >= 0 ? activeW : 0)
                + (pillCount - 1) * gap;

        int startX = Math.max(0, (totalWidth - requiredWidth) / 2);
        int currentX = startX;

        for (int i = 0; i < pillCount; i++) {
            boolean isActive = (i == activeIndex);
            int w = isActive ? activeW : standardW;
            int h = isActive ? activeH : standardH;
            int y = (totalHeight - h) / 2;

            Color baseColor = COLORS[i];

            if (isActive) {
                // Elevated active pill
                g2.setColor(baseColor);
                g2.fill(new RoundRectangle2D.Float(currentX, y, w, h, 8, 8));

                // Clean highlight border
                g2.setColor(new Color(255, 255, 255, 220));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(currentX, y, w, h, 8, 8));

                g2.setFont(FoodSenseTheme.getFont(Font.BOLD, 14f));
                g2.setColor(Color.WHITE);
            } else {
                // Dimmed inactive pill
                int alpha = (activeIndex >= 0) ? 90 : 180;
                Color dimmed = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
                g2.setColor(dimmed);
                g2.fill(new RoundRectangle2D.Float(currentX, y, w, h, 6, 6));

                g2.setFont(FoodSenseTheme.getFont(Font.BOLD, 11f));
                g2.setColor(new Color(255, 255, 255, (activeIndex >= 0) ? 140 : 220));
            }

            // Draw centered letter
            FontMetrics fm = g2.getFontMetrics();
            int textX = currentX + (w - fm.stringWidth(GRADES[i])) / 2;
            int textY = y + ((h - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(GRADES[i], textX, textY);

            currentX += w + gap;
        }

        g2.dispose();
    }
}
