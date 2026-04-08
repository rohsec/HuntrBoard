package com.rohsec.huntrboard.ui.components;

import com.rohsec.huntrboard.ui.ThemePalette;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;

public class StatCardPanel extends CardPanel {
    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();

    public StatCardPanel(ThemePalette palette, String title) {
        super(palette);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(160, 116));

        titleLabel.setText(title);
        titleLabel.setForeground(palette.textSecondary);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));

        valueLabel.setForeground(palette.textPrimary);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));

        subtitleLabel.setForeground(palette.textSecondary);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(Font.PLAIN, 12f));

        add(titleLabel);
        add(javax.swing.Box.createVerticalStrut(10));
        add(valueLabel);
        add(javax.swing.Box.createVerticalGlue());
        add(subtitleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }
}
