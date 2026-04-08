package com.rohsec.huntrboard.ui.charts;

import com.rohsec.huntrboard.service.TrackerService;
import com.rohsec.huntrboard.ui.ThemePalette;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

public class MonthlyBarChartPanel extends JPanel {
    private final ThemePalette palette;
    private List<Double> values = new ArrayList<>();

    public MonthlyBarChartPanel(ThemePalette palette) {
        this.palette = palette;
        setOpaque(true);
        setBackground(palette.cardBackground);
        setToolTipText("Monthly bounty progress");
    }

    public void setValues(List<Double> values) {
        this.values = values == null ? new ArrayList<>() : new ArrayList<>(values);
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
        int left = 18;
        int right = 14;
        int top = 20;
        int bottom = 36;
        int chartWidth = Math.max(10, width - left - right);
        int chartHeight = Math.max(10, height - top - bottom);

        g2.setColor(palette.textSecondary);
        g2.setFont(g2.getFont().deriveFont(13f));
        g2.drawString("Monthly progress", left, 14);

        double max = 0.0d;
        for (Double value : values) {
            if (value != null) {
                max = Math.max(max, value);
            }
        }
        if (max <= 0.0d) {
            max = 1.0d;
        }

        g2.setStroke(new BasicStroke(1f));
        for (int line = 0; line < 4; line++) {
            int y = top + (chartHeight * line / 3);
            g2.setColor(palette.chartGrid);
            g2.drawLine(left, y, left + chartWidth, y);
        }

        int barCount = TrackerService.MONTHS.length;
        int slotWidth = chartWidth / barCount;
        int barWidth = Math.max(10, slotWidth - 10);
        FontMetrics metrics = g2.getFontMetrics(g2.getFont().deriveFont(11f));

        for (int index = 0; index < barCount; index++) {
            double value = index < values.size() && values.get(index) != null ? Math.max(0.0d, values.get(index)) : 0.0d;
            int barHeight = (int) Math.round((value / max) * (chartHeight - 8));
            int x = left + (slotWidth * index) + Math.max(3, (slotWidth - barWidth) / 2);
            int y = top + chartHeight - barHeight;
            g2.setColor(index % 2 == 0 ? palette.accent : palette.accentMuted);
            g2.fillRoundRect(x, y, barWidth, barHeight, 10, 10);
            g2.setColor(new Color(255, 255, 255, 20));
            g2.drawRoundRect(x, y, barWidth, barHeight, 10, 10);

            g2.setColor(palette.textSecondary);
            String month = TrackerService.MONTHS[index];
            int labelX = x + ((barWidth - metrics.stringWidth(month)) / 2);
            g2.drawString(month, labelX, height - 14);
        }
        g2.dispose();
    }
}
