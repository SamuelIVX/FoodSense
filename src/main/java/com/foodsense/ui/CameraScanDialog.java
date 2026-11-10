/*
 * In-app camera scanner modal dialog for FoodSense.
 * Embeds live camera stream with an illuminated viewfinder reticle and animated scan guide.
 */

package com.foodsense.ui;

import com.foodsense.VideoProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Modern modal scanner dialog displaying live webcam feed with viewfinder reticles
 * and animated scan line.
 */
public class CameraScanDialog extends JDialog {

    private final Consumer<String> onBarcodeScanned;
    private VideoProcessor videoProcessor;
    private BufferedImage currentFrame = null;
    private final ViewfinderPanel viewfinder;
    private final JLabel statusLabel;
    private final Timer scanAnimTimer;
    private float scanLinePos = 0.0f;
    private boolean scanLineDown = true;

    /**
     * Constructs and opens the camera scanner modal.
     *
     * @param parent           parent JFrame
     * @param onBarcodeScanned callback invoked on EDT with detected barcode string
     */
    public CameraScanDialog(JFrame parent, Consumer<String> onBarcodeScanned) {
        super(parent, "Live Barcode Scanner", true);
        this.onBarcodeScanned = onBarcodeScanned;

        setSize(680, 560);
        setResizable(false);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(15, 23, 42));

        viewfinder = new ViewfinderPanel();
        add(viewfinder, BorderLayout.CENTER);

        // Bottom control bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(11, 19, 32));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        statusLabel = new JLabel("● Align barcode within the viewfinder");
        statusLabel.setFont(FoodSenseTheme.getFont(Font.PLAIN, 13f));
        statusLabel.setForeground(new Color(148, 163, 184));
        bottomBar.add(statusLabel, BorderLayout.WEST);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(FoodSenseTheme.getFont(Font.BOLD, 12f));
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> closeScanner());
        bottomBar.add(cancelBtn, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // Esc key listener
        getRootPane().registerKeyboardAction(
                e -> closeScanner(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeScanner();
            }
        });

        // Scan line animation timer (30 FPS)
        scanAnimTimer = new Timer(33, e -> {
            if (scanLineDown) {
                scanLinePos += 0.03f;
                if (scanLinePos >= 1.0f) {
                    scanLinePos = 1.0f;
                    scanLineDown = false;
                }
            } else {
                scanLinePos -= 0.03f;
                if (scanLinePos <= 0.0f) {
                    scanLinePos = 0.0f;
                    scanLineDown = true;
                }
            }
            viewfinder.repaint();
        });
    }

    /**
     * Initializes camera grabber and makes modal visible.
     */
    public void open() {
        scanAnimTimer.start();

        videoProcessor = new VideoProcessor(
                barcode -> SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("✔ Barcode detected!");
                    statusLabel.setForeground(new Color(52, 211, 153));
                    if (onBarcodeScanned != null) {
                        onBarcodeScanned.accept(barcode);
                    }
                    closeScanner();
                }),
                image -> SwingUtilities.invokeLater(() -> {
                    this.currentFrame = image;
                    viewfinder.repaint();
                }),
                errorMsg -> SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("⚠ " + errorMsg);
                    statusLabel.setForeground(new Color(248, 113, 113));
                })
        );

        videoProcessor.start();
        setVisible(true);
    }

    private void closeScanner() {
        scanAnimTimer.stop();
        if (videoProcessor != null) {
            videoProcessor.stop();
        }
        dispose();
    }

    /**
     * Custom surface rendering camera feed with semi-transparent vignette and viewfinder reticle.
     */
    private class ViewfinderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Draw camera video frame if available
            if (currentFrame != null) {
                g2.drawImage(currentFrame, 0, 0, w, h, null);
            } else {
                g2.setColor(new Color(15, 23, 42));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(FoodSenseTheme.getFont(Font.PLAIN, 14f));
                String msg = "Connecting to camera...";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            }

            // 2. Viewfinder dimensions
            int boxW = 380;
            int boxH = 200;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;

            // Semi-transparent dark overlay around the viewfinder
            g2.setColor(new Color(0, 0, 0, 140));
            // Top
            g2.fillRect(0, 0, w, boxY);
            // Bottom
            g2.fillRect(0, boxY + boxH, w, h - (boxY + boxH));
            // Left
            g2.fillRect(0, boxY, boxX, boxH);
            // Right
            g2.fillRect(boxX + boxW, boxY, w - (boxX + boxW), boxH);

            // 3. Viewfinder border & corner reticles
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(boxX, boxY, boxW, boxH, 12, 12));

            // Corner brackets (bold mint accent)
            g2.setColor(new Color(52, 211, 153));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int arm = 24;
            // Top-left
            g2.drawLine(boxX, boxY + arm, boxX, boxY);
            g2.drawLine(boxX, boxY, boxX + arm, boxY);
            // Top-right
            g2.drawLine(boxX + boxW - arm, boxY, boxX + boxW, boxY);
            g2.drawLine(boxX + boxW, boxY, boxX + boxW, boxY + arm);
            // Bottom-left
            g2.drawLine(boxX, boxY + boxH - arm, boxX, boxY + boxH);
            g2.drawLine(boxX, boxY + boxH, boxX + arm, boxY + boxH);
            // Bottom-right
            g2.drawLine(boxX + boxW - arm, boxY + boxH, boxX + boxW, boxY + boxH);
            g2.drawLine(boxX + boxW, boxY + boxH - arm, boxX + boxW, boxY + boxH);

            // 4. Animated scan line
            int lineY = (int) (boxY + (boxH * scanLinePos));
            GradientPaint gp = new GradientPaint(
                    boxX, lineY, new Color(52, 211, 153, 0),
                    boxX + boxW / 2f, lineY, new Color(52, 211, 153, 220),
                    true
            );
            g2.setPaint(gp);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(boxX + 10, lineY, boxX + boxW - 10, lineY);

            g2.dispose();
        }
    }
}
