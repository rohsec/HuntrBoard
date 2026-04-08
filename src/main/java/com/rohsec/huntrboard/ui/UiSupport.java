package com.rohsec.huntrboard.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;

public final class UiSupport {
    private UiSupport() {
    }

    public static JButton createIconButton(String text, String tooltip, ThemePalette palette) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.fieldBorder, 1, true),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)
        ));
        button.setBackground(palette.fieldBackground);
        button.setForeground(palette.textPrimary);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JButton createAccentButton(String text, ThemePalette palette) {
        JButton button = createIconButton(text, text, palette);
        button.setBackground(palette.accent);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        return button;
    }

    public static JPanel createSignalBadge(ThemePalette palette) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        badge.setOpaque(true);
        badge.setBackground(palette.cardBackground);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.cardBorder, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel dot = new JLabel("●");
        dot.setForeground(palette.danger);
        dot.setFont(dot.getFont().deriveFont(Font.BOLD, 12f));

        JLabel text = new JLabel("Live Signal");
        text.setForeground(palette.textSecondary);
        text.setFont(text.getFont().deriveFont(Font.BOLD, 12f));

        badge.add(dot);
        badge.add(text);
        return badge;
    }

    public static JButton createLinkButton(String text, ThemePalette palette) {
        JButton button = new JButton(text);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(palette.accent);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        return button;
    }

    public static void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
            // If Burp runs in a restricted environment, failing silently is preferable to breaking the UI.
        }
    }
}
