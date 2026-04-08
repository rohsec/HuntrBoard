package com.rohsec.huntrboard.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
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

    public static JPanel createTitleLabel(String icon, String title, ThemePalette palette, float fontSize) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setForeground(palette.accent);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(fontSize)));

        JLabel textLabel = new JLabel(title);
        textLabel.setForeground(palette.textPrimary);
        textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD, fontSize));

        panel.add(iconLabel);
        panel.add(textLabel);
        return panel;
    }

    public static JPanel createSignalBadge(ThemePalette palette) {
        JPanel badge = new JPanel();
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setOpaque(true);
        badge.setBackground(palette.cardBackground);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.cardBorder, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel dot = new JLabel("●");
        dot.setForeground(palette.danger);
        dot.setFont(dot.getFont().deriveFont(Font.BOLD, 12f));
        JLabel text = new JLabel("ROHSEC");
        text.setForeground(palette.textPrimary);
        text.setFont(text.getFont().deriveFont(Font.BOLD, 12f));
        topRow.add(dot);
        topRow.add(text);

        JPanel linksRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        linksRow.setOpaque(false);
        linksRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton siteButton = createLinkButton("🌐 rohsec.com", palette);
        siteButton.setFont(siteButton.getFont().deriveFont(Font.PLAIN, 11f));
        siteButton.addActionListener(event -> openLink("https://rohsec.com"));
        JButton xButton = createLinkButton("𝕏 @rohsec", palette);
        xButton.setFont(xButton.getFont().deriveFont(Font.PLAIN, 11f));
        xButton.addActionListener(event -> openLink("https://x.com/rohsec"));
        linksRow.add(siteButton);
        linksRow.add(xButton);

        badge.add(topRow);
        badge.add(Box.createVerticalStrut(2));
        badge.add(linksRow);
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
