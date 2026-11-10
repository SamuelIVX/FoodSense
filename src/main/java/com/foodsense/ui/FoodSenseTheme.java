/*
 * Design token definitions, color palettes, and theme management for FoodSense.
 * Features an authentic forest green / botanical aesthetic for both Light and Dark modes.
 */

package com.foodsense.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Central design tokens, color scales, and theme configuration for FoodSense.
 * Adheres to a botanical forest green aesthetic for both light and dark modes.
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

    // ── Forest Theme Semantic Colors ──────────────────────────

    public static Color getBackground() {
        // Light: Gentle morning mist botanical green; Dark: Deep obsidian forest
        return darkMode ? new Color(11, 22, 14) : new Color(244, 248, 244);
    }

    public static Color getSidebarBackground() {
        // Light: Soft light sage rail; Dark: Deep midnight evergreen rail
        return darkMode ? new Color(8, 18, 11) : new Color(235, 244, 237);
    }

    public static Color getSidebarText() {
        // Light: Deep pine green; Dark: Crisp mint white
        return darkMode ? new Color(240, 253, 244) : new Color(18, 48, 26);
    }

    public static Color getSidebarMuted() {
        // Light: Medium fern green; Dark: Radiant mint
        return darkMode ? new Color(110, 231, 183) : new Color(68, 108, 80);
    }

    public static Color getCardBackground() {
        // Light: Pure white; Dark: Elevated dark pine surface
        return darkMode ? new Color(18, 35, 22) : Color.WHITE;
    }

    public static Color getCardBorder() {
        return darkMode ? new Color(31, 58, 37) : new Color(212, 228, 215);
    }

    public static Color getTextPrimary() {
        // Light: Deep pine black; Dark: Crisp mint white
        return darkMode ? new Color(240, 253, 244) : new Color(16, 44, 24);
    }

    public static Color getTextSecondary() {
        return darkMode ? new Color(167, 243, 208) : new Color(38, 72, 48);
    }

    public static Color getTextMuted() {
        return darkMode ? new Color(110, 231, 183) : new Color(78, 114, 88);
    }

    public static Color getPrimaryAccent() {
        // Light: Rich botanical emerald green; Dark: Radiant emerald mint
        return darkMode ? new Color(52, 211, 153) : new Color(22, 102, 53);
    }

    public static Color getPrimaryAccentHover() {
        return darkMode ? new Color(16, 185, 129) : new Color(16, 82, 42);
    }

    public static Color getPrimaryLight() {
        return darkMode ? new Color(12, 54, 30) : new Color(230, 246, 233);
    }

    public static Color getChipBackground() {
        return darkMode ? new Color(16, 36, 24) : new Color(236, 247, 238);
    }

    public static Color getChipBorder() {
        return darkMode ? new Color(30, 64, 40) : new Color(200, 226, 204);
    }

    public static Color getChipText() {
        return darkMode ? new Color(236, 253, 245) : new Color(20, 52, 30);
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
