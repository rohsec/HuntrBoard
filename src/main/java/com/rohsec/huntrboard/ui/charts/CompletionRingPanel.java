package com.rohsec.huntrboard.ui.charts;

import com.rohsec.huntrboard.ui.ThemePalette;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class CompletionRingPanel extends JPanel {
    private final ThemePalette palette;
    private double percent;
    private String centerLabel = "0%";
    private String footerLabel = "No target set";

    public CompletionRingPanel(ThemePalette palette) {
        this.palette = palette;
        setOpaque(true);
        setBackground(palette.cardBackground);
    }

    public void setProgress(double percent, String footerLabel) {
        this.percent = Math.max(0.0d, Math.min(100.0d, percent));
        this.centerLabel = Math.round(this.percent) + "%";
        this.footerLabel = footerLabel;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height) - 48;
        int x = (width - size) / 2;
        int y = 24;

        g2.setColor(palette.textSecondary);
        g2.setFont(g2.getFont().deriveFont(13f));
        g2.drawString("Yearly completion", 18, 18);

        g2.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(palette.chartGrid);
        g2.drawArc(x, y, size, size, 90, -360);

        g2.setColor(palette.success);
        g2.drawArc(x, y, size, size, 90, (int) Math.round((-360.0d * percent) / 100.0d));

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        int centerWidth = g2.getFontMetrics().stringWidth(centerLabel);
        g2.setColor(palette.textPrimary);
        g2.drawString(centerLabel, (width - centerWidth) / 2, y + size / 2 + 10);

        g2.setFont(g2.getFont().deriveFont(12f));
        int footerWidth = g2.getFontMetrics().stringWidth(footerLabel);
        g2.setColor(palette.textSecondary);
        g2.drawString(footerLabel, Math.max(18, (width - footerWidth) / 2), height - 18);
        g2.dispose();
    }
}
