/*
 * Design token definitions, color palettes, and theme management for FoodSense.
 * Adheres to 4px spacing scale, WCAG 2.2 AA contrast rules, and provides runtime light/dark switching.
 */

package com.foodsense.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Central design tokens, color scales, and theme configuration for FoodSense.
 */
public final class FoodSenseTheme {

    // ── Spacing Scale (4px grid) ──────────────────────────────
    public static final int SPACE_2  = 2;
    public static final int SPACE_4  = 4;
    public static final int SPACE_8  = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_20 = 20;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;

    // ── Nutri-Score Official Palette ──────────────────────────
    public static final Color SCORE_A = new Color(3, 129, 65);
    public static final Color SCORE_B = new Color(133, 187, 47);
    public static final Color SCORE_C = new Color(254, 203, 2);
    public static final Color SCORE_D = new Color(238, 129, 0);
    public static final Color SCORE_E = new Color(230, 62, 17);

    // ── State ─────────────────────────────────────────────────
    private static boolean darkMode = false;

    private FoodSenseTheme() {
        // Prevent instantiation
    }

    /**
     * Initializes FlatLaf Look and Feel with anti-aliasing rendering hints.
     */
    public static void initialize() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            if (darkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception e) {
            System.err.println("Notice: FlatLaf setup fallback: " + e.getMessage());
        }
    }

    /**
     * Returns whether the active theme is dark mode.
     *
     * @return {@code true} if dark mode; {@code false} if light mode
     */
    public static boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Toggles between Light and Dark mode, updating FlatLaf and repainting the component tree.
     *
     * @param root component tree to update (typically top-level JFrame)
     */
    public static void toggleTheme(Component root) {
        setDarkMode(!darkMode, root);
    }

    /**
     * Sets the active theme mode and updates Swing component UI defaults.
     *
     * @param dark true for dark mode; false for light mode
     * @param root component tree to update
     */
    public static void setDarkMode(boolean dark, Component root) {
        darkMode = dark;
        try {
            if (darkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            if (root != null) {
                SwingUtilities.updateComponentTreeUI(root);
                root.repaint();
            }
        } catch (Exception e) {
            System.err.println("Theme update failed: " + e.getMessage());
        }
    }

    // ── Semantic Color Getters ────────────────────────────────

    public static Color getBackground() {
        return darkMode ? new Color(15, 23, 42) : new Color(248, 250, 252);
    }

    public static Color getSidebarBackground() {
        return darkMode ? new Color(11, 19, 32) : new Color(241, 245, 249);
    }

    public static Color getCardBackground() {
        return darkMode ? new Color(30, 41, 59) : Color.WHITE;
    }

    public static Color getCardBorder() {
        return darkMode ? new Color(51, 65, 85) : new Color(226, 232, 240);
    }

    public static Color getTextPrimary() {
        return darkMode ? new Color(248, 250, 252) : new Color(15, 23, 42);
    }

    public static Color getTextSecondary() {
        return darkMode ? new Color(203, 213, 225) : new Color(71, 85, 105);
    }

    public static Color getTextMuted() {
        return darkMode ? new Color(148, 163, 184) : new Color(100, 116, 139);
    }

    public static Color getPrimaryAccent() {
        return darkMode ? new Color(52, 211, 153) : new Color(27, 67, 50);
    }

    public static Color getPrimaryAccentHover() {
        return darkMode ? new Color(16, 185, 129) : new Color(45, 106, 79);
    }

    public static Color getPrimaryLight() {
        return darkMode ? new Color(6, 78, 59) : new Color(232, 245, 233);
    }

    public static Color getDanger() {
        return darkMode ? new Color(248, 113, 113) : new Color(220, 38, 38);
    }

    public static Color getDangerBackground() {
        return darkMode ? new Color(69, 10, 10) : new Color(254, 242, 242);
    }

    public static Color getDangerBorder() {
        return darkMode ? new Color(127, 29, 29) : new Color(254, 202, 202);
    }

    /**
     * Resolves the official background color for a Nutri-Score grade letter.
     *
     * @param grade letter grade (A-E)
     * @return matching Nutri-Score color, or muted grey if unknown
     */
    public static Color getNutriscoreColor(String grade) {
        if (grade == null) {
            return getTextMuted();
        }
        return switch (grade.trim().toUpperCase()) {
            case "A" -> SCORE_A;
            case "B" -> SCORE_B;
            case "C" -> SCORE_C;
            case "D" -> SCORE_D;
            case "E" -> SCORE_E;
            default  -> getTextMuted();
        };
    }

    // ── Typography ────────────────────────────────────────────

    /**
     * Returns a styled modern font for the given weight and size.
     *
     * @param weight Font.PLAIN or Font.BOLD
     * @param size   font size in points
     * @return Font instance
     */
    public static Font getFont(int weight, float size) {
        return new Font("SansSerif", weight, (int) size);
    }
}
